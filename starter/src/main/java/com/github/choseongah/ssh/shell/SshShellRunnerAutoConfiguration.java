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

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.shell.core.ShellRunner;

import static com.github.choseongah.ssh.shell.SshShellProperties.SSH_SHELL_ENABLE;

/**
 * Prevent spring shell non interactive runner from logging an error in ssh only mode.
 */
@AutoConfiguration
@AutoConfigureBefore(name = "org.springframework.shell.core.autoconfigure.ShellRunnerAutoConfiguration")
@ConditionalOnClass(ShellRunner.class)
@ConditionalOnProperty(name = SSH_SHELL_ENABLE, havingValue = "true", matchIfMissing = true)
public class SshShellRunnerAutoConfiguration {

    @Bean
    public static BeanFactoryPostProcessor sshShellRunnerSuppressor(Environment environment) {
        return beanFactory -> {
            boolean interactiveEnabled = environment.getProperty(
                    "spring.shell.interactive.enabled", Boolean.class, true);
            if (beanFactory instanceof BeanDefinitionRegistry registry) {
                if (registry.containsBeanDefinition("historyCommand")) {
                    registry.removeBeanDefinition("historyCommand");
                }
                if (registry.containsBeanDefinition("scriptCommand")) {
                    registry.removeBeanDefinition("scriptCommand");
                }
                if (!interactiveEnabled && registry.containsBeanDefinition("springShellApplicationRunner")) {
                    registry.removeBeanDefinition("springShellApplicationRunner");
                }
            }
        };
    }

    @Bean
    public ApplicationRunner sshShellApplicationRunner(ShellRunner shellRunner, Environment environment) {
        return args -> {
            boolean interactiveEnabled = environment.getProperty(
                    "spring.shell.interactive.enabled", Boolean.class, true);
            if (!interactiveEnabled && args.getNonOptionArgs().isEmpty()) {
                return;
            }
            shellRunner.run(args.getSourceArgs());
        };
    }
}
