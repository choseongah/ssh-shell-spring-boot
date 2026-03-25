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

package io.github.choseongah.ssh.shell;

import jakarta.validation.Validator;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.shell.core.command.availability.AvailabilityProvider;
import org.springframework.shell.core.command.completion.CompletionProvider;
import org.springframework.shell.core.command.completion.DefaultCompletionProvider;
import org.springframework.shell.core.command.exit.ExitStatusExceptionMapper;
import org.springframework.shell.core.utils.Utils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Factory helping to build instances of {@link Command}.
 */
public class SshCommandFactoryBean {

    private final ApplicationContext applicationContext;

    private final SshPostProcessorService postProcessorService;

    public SshCommandFactoryBean(ApplicationContext applicationContext, SshPostProcessorService postProcessorService) {
        this.applicationContext = applicationContext;
        this.postProcessorService = postProcessorService;
    }

    public Command getObject(Method method) {
        org.springframework.shell.core.command.annotation.Command command = MergedAnnotations.from(method)
                .get(org.springframework.shell.core.command.annotation.Command.class)
                .synthesize();

        String name = String.join(" ", command.name());
        name = name.isEmpty() ? Utils.unCamelify(method.getName()) : name;
        String description = command.description();
        description = description.isEmpty() ? "N/A" : description;
        String help = command.help();
        String group = command.group();
        if (group.isEmpty()) {
            String simpleName = Utils.splitCamelCase(method.getDeclaringClass().getSimpleName());
            group = simpleName.endsWith(" Commands") ? simpleName : simpleName + " Commands";
        }
        boolean hidden = command.hidden();
        String[] aliases = command.alias();
        AvailabilityProvider availabilityProvider = getAvailabilityProvider(command.availabilityProvider());
        ExitStatusExceptionMapper exitStatusExceptionMapper = getExitStatusExceptionMapper(
                command.exitStatusExceptionMapper());
        CompletionProvider completionProvider = getCompletionProvider(command.completionProvider());
        ConfigurableConversionService conversionService = getConversionService();
        Validator validator = getValidator();
        List<CommandOption> commandOptions = getCommandOptions(method);

        SshMethodInvokerCommandAdapter adapter = new SshMethodInvokerCommandAdapter(name, description, group, help,
                hidden, method, applicationContext, conversionService, validator, postProcessorService);
        adapter.setAliases(Arrays.stream(aliases).toList());
        adapter.setOptions(commandOptions);
        adapter.setAvailabilityProvider(availabilityProvider);
        adapter.setCompletionProvider(completionProvider);
        if (exitStatusExceptionMapper != null) {
            adapter.setExitStatusExceptionMapper(exitStatusExceptionMapper);
        }
        return adapter;
    }

    private List<CommandOption> getCommandOptions(Method method) {
        List<CommandOption> commandOptions = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            Option option = parameter.getAnnotation(Option.class);
            if (option != null) {
                String longName = option.longName().isEmpty() ? parameter.getName() : option.longName();
                commandOptions.add(CommandOption.with()
                        .shortName(option.shortName())
                        .longName(longName)
                        .description(option.description())
                        .required(option.required())
                        .defaultValue(option.defaultValue())
                        .type(parameter.getType())
                        .build());
            }
        }
        return commandOptions;
    }

    private AvailabilityProvider getAvailabilityProvider(String beanName) {
        if (!StringUtils.hasText(beanName)) {
            return AvailabilityProvider.alwaysAvailable();
        }
        return () -> {
            try {
                return this.applicationContext.getBean(beanName, AvailabilityProvider.class).get();
            } catch (BeansException e) {
                return AvailabilityProvider.alwaysAvailable().get();
            }
        };
    }

    private ExitStatusExceptionMapper getExitStatusExceptionMapper(String beanName) {
        try {
            return this.applicationContext.getBean(beanName, ExitStatusExceptionMapper.class);
        } catch (BeansException e) {
            return null;
        }
    }

    private CompletionProvider getCompletionProvider(String beanName) {
        try {
            return this.applicationContext.getBean(beanName, CompletionProvider.class);
        } catch (BeansException e) {
            return new DefaultCompletionProvider();
        }
    }

    private ConfigurableConversionService getConversionService() {
        try {
            return this.applicationContext.getBean(ConfigurableConversionService.class);
        } catch (BeansException e) {
            return new DefaultConversionService();
        }
    }

    private Validator getValidator() {
        try {
            return this.applicationContext.getBean(Validator.class);
        } catch (BeansException e) {
            return Utils.defaultValidator();
        }
    }
}
