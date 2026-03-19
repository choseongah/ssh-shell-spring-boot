/*
 * Copyright (c) 2026 OpenAI Codex with Cho Seong-ah (choseongah)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.choseongah.ssh.shell;

import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.util.ClassUtils;
import org.springframework.shell.core.utils.Utils;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.github.choseongah.ssh.shell.SshShellProperties.SSH_SHELL_ENABLE;

/**
 * Register programmatic and annotated commands with a custom adapter supporting ssh features.
 */
@AutoConfiguration
@AutoConfigureBefore(name = "org.springframework.shell.core.autoconfigure.CommandRegistryAutoConfiguration")
@ConditionalOnProperty(name = SSH_SHELL_ENABLE, havingValue = "true", matchIfMissing = true)
public class SshCommandRegistryAutoConfiguration {

    @Bean
    @Primary
    public CommandRegistry sshCommandRegistry(ApplicationContext applicationContext,
                                              SshPostProcessorService postProcessorService) {
        CommandRegistry commandRegistry = new SshCommandRegistry();
        registerProgrammaticCommands(applicationContext, commandRegistry);
        registerAnnotatedCommands(applicationContext, commandRegistry, postProcessorService);
        commandRegistry.registerCommand(Utils.QUIT_COMMAND);
        return commandRegistry;
    }

    private void registerProgrammaticCommands(ApplicationContext applicationContext, CommandRegistry commandRegistry) {
        Map<String, Command> commandBeans = applicationContext.getBeansOfType(Command.class);
        commandBeans.forEach((beanName, command) -> {
            if (("help".equals(command.getName()) && !"sshHelpCommand".equals(beanName))
                    || "history".equals(command.getName())
                    || "script".equals(command.getName())) {
                return;
            }
            commandRegistry.registerCommand(command);
        });
    }

    private void registerAnnotatedCommands(ApplicationContext applicationContext, CommandRegistry commandRegistry,
                                           SshPostProcessorService postProcessorService) {
        SshCommandFactoryBean factoryBean = new SshCommandFactoryBean(applicationContext, postProcessorService);
        Object application = applicationContext.getBeansWithAnnotation(SpringBootApplication.class)
                .values().stream().findFirst().orElse(null);
        if (application == null) {
            return;
        }

        Class<?> applicationClass = AopUtils.getTargetClass(application);
        Set<String> basePackages = new LinkedHashSet<>();
        basePackages.add(ClassUtils.getPackageName(applicationClass));
        basePackages.add(ClassUtils.getPackageName(SshShellAutoConfiguration.class));
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(true);
        Set<String> registeredMethods = new LinkedHashSet<>();
        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                String beanClassName = candidate.getBeanClassName();
                if (beanClassName == null) {
                    continue;
                }
                try {
                    Class<?> beanClass = ClassUtils.forName(beanClassName, applicationContext.getClassLoader());
                    Map<Method, Boolean> methods = MethodIntrospector.selectMethods(beanClass,
                            (MethodIntrospector.MetadataLookup<Boolean>) method -> AnnotatedElementUtils.hasAnnotation(
                                    method, org.springframework.shell.core.command.annotation.Command.class)
                                    ? Boolean.TRUE : null);
                    for (Method method : methods.keySet()) {
                        String methodId = method.getDeclaringClass().getName() + "#" + method.toGenericString();
                        if (registeredMethods.add(methodId)) {
                            commandRegistry.registerCommand(factoryBean.getObject(method));
                        }
                    }
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Unable to load command class " + beanClassName, e);
                }
            }
        }
    }
}
