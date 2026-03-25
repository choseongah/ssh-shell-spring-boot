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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.shell.core.ParameterValidationException;
import org.springframework.shell.core.command.AbstractCommand;
import org.springframework.shell.core.command.CommandArgument;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.ExitStatus;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Arguments;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.shell.core.utils.Utils;
import org.springframework.util.MethodInvoker;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Method adapter applying ssh post processors before writing command result.
 */
public class SshMethodInvokerCommandAdapter extends AbstractCommand {

    private final Method method;

    private final ApplicationContext applicationContext;

    private final ConfigurableConversionService conversionService;

    private final Validator validator;

    private final SshPostProcessorService postProcessorService;

    public SshMethodInvokerCommandAdapter(String name, String description, String group, String help, boolean hidden,
                                          Method method, ApplicationContext applicationContext,
                                          ConfigurableConversionService conversionService, Validator validator,
                                          SshPostProcessorService postProcessorService) {
        super(name, description, group, help, hidden);
        this.method = method;
        this.applicationContext = applicationContext;
        this.conversionService = conversionService;
        this.validator = validator;
        this.postProcessorService = postProcessorService;
    }

    @Override
    public ExitStatus execute(CommandContext commandContext) throws Exception {
        Availability availability = getAvailabilityProvider().get();
        if (!availability.isAvailable()) {
            printError(commandContext, "Command '" + getName() + "' exists but is not currently available because "
                    + availability.reason());
            return ExitStatus.AVAILABILITY_ERROR;
        }

        List<CommandOption> options = commandContext.parsedInput().options();
        if (options.stream().anyMatch(this::isHelp)) {
            println(SshHelpCommand.getHelpMessageForCommand(this), commandContext);
            return ExitStatus.OK;
        }

        try {
            return doExecute(commandContext);
        } catch (MissingOptionException e) {
            printError(commandContext, missingOptionMessage(e.optionName, e.description));
            return ExitStatus.USAGE_ERROR;
        } catch (MissingArgumentException e) {
            printError(commandContext, missingArgumentMessage(e.argumentIndex, e.argumentName, e.argumentType,
                    e.description));
            return ExitStatus.USAGE_ERROR;
        } catch (ParameterValidationException parameterValidationException) {
            printError(commandContext, "The following constraints were not met:");
            parameterValidationException.getConstraintViolations().forEach(violation -> {
                String propertyPath = violation.getPropertyPath().toString();
                int lastIndexOfDot = propertyPath.lastIndexOf(".");
                String propertyName = lastIndexOfDot == -1 ? propertyPath : propertyPath.substring(lastIndexOfDot + 1);
                String errorMessage = String.format("\t--%s: %s", propertyName, violation.getMessage());
                printError(commandContext, errorMessage);
            });
            return ExitStatus.USAGE_ERROR;
        } catch (Exception e) {
            if (getExitStatusExceptionMapper() != null) {
                return getExitStatusExceptionMapper().apply(e);
            }
            throw e;
        }
    }

    @Override
    public ExitStatus doExecute(CommandContext commandContext) throws Exception {
        Object targetObject = this.applicationContext.getBean(this.method.getDeclaringClass());
        Method invocableMethod = AopUtils.selectInvocableMethod(this.method, targetObject.getClass());
        MethodInvoker methodInvoker = new MethodInvoker();
        methodInvoker.setTargetObject(targetObject);
        methodInvoker.setTargetMethod(invocableMethod.getName());

        List<Object> arguments = prepareArguments(commandContext);
        methodInvoker.setArguments(arguments.toArray());
        methodInvoker.prepare();

        Set<ConstraintViolation<Object>> constraintViolations = validator.forExecutables()
                .validateParameters(targetObject, invocableMethod, arguments.toArray());
        if (!constraintViolations.isEmpty()) {
            throw new ParameterValidationException(constraintViolations);
        }

        Object result = methodInvoker.invoke();
        if (result != null) {
            Object processed = postProcessorService.postProcess(result, commandContext.outputWriter());
            if (processed != null) {
                commandContext.outputWriter().println(render(processed));
                commandContext.outputWriter().flush();
            }
        }
        return ExitStatus.OK;
    }

    private List<Object> prepareArguments(CommandContext commandContext) {
        List<Object> args = new ArrayList<>();
        Parameter[] parameters = this.method.getParameters();
        Class<?>[] parameterTypes = this.method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (parameterTypes[i].equals(CommandContext.class)) {
                args.add(commandContext);
                continue;
            }

            Option option = parameters[i].getAnnotation(Option.class);
            if (option != null) {
                String longName = option.longName().isEmpty() ? parameters[i].getName() : option.longName();
                CommandOption commandOption = commandContext.getOptionByLongName(longName);
                if (commandOption == null && option.shortName() != ' ') {
                    commandOption = commandContext.getOptionByShortName(option.shortName());
                }
                if (commandOption != null) {
                    args.add(this.conversionService.convert(commandOption.value(), parameterTypes[i]));
                } else if (option.required()) {
                    throw new MissingOptionException(longName, option.description());
                } else if (!parameterTypes[i].isPrimitive()) {
                    if (!option.defaultValue().isEmpty()) {
                        args.add(this.conversionService.convert(option.defaultValue(), parameterTypes[i]));
                    } else {
                        args.add(null);
                    }
                } else {
                    args.add(Utils.getDefaultValueForPrimitiveType(parameterTypes[i]));
                }
                continue;
            }

            Argument argument = parameters[i].getAnnotation(Argument.class);
            if (argument != null) {
                String rawValue = null;
                try {
                    rawValue = commandContext.getArgumentByIndex(argument.index()).value();
                } catch (Exception e) {
                    if (!argument.defaultValue().isEmpty()) {
                        rawValue = argument.defaultValue();
                    }
                }
                if (rawValue == null) {
                    if (argument.defaultValue().isEmpty()) {
                        throw new MissingArgumentException(argument.index(), parameters[i].getName(),
                                parameterTypes[i].getSimpleName(), argument.description());
                    }
                    if (parameterTypes[i].isPrimitive()) {
                        args.add(Utils.getDefaultValueForPrimitiveType(parameterTypes[i]));
                    } else {
                        args.add(null);
                    }
                } else {
                    args.add(this.conversionService.convert(rawValue, parameterTypes[i]));
                }
                continue;
            }

            Arguments argumentsAnnotation = parameters[i].getAnnotation(Arguments.class);
            if (argumentsAnnotation != null) {
                List<String> rawValues = commandContext.parsedInput().arguments().stream().map(CommandArgument::value)
                        .toList();
                args.add(this.conversionService.convert(rawValues, parameterTypes[i]));
            }
        }
        return args;
    }

    private String missingOptionMessage(String optionName, String description) {
        StringBuilder message = new StringBuilder("Missing mandatory option '--").append(optionName).append("'");
        if (description != null && !description.isBlank()) {
            message.append(", ").append(description);
        }
        return message.toString();
    }

    private String missingArgumentMessage(int argumentIndex, String argumentName, String argumentType,
                                          String description) {
        StringBuilder message = new StringBuilder("Missing mandatory argument at index ")
                .append(argumentIndex)
                .append(" [")
                .append(argumentName)
                .append(" ")
                .append(argumentType)
                .append("]");
        if (description != null && !description.isBlank()) {
            message.append(", ").append(description);
        }
        return message.toString();
    }

    private void printError(CommandContext commandContext, String message) {
        commandContext.outputWriter().println(SshShellHelper.getColoredMessage(message, PromptColor.RED));
        commandContext.outputWriter().flush();
    }

    private Object render(Object result) {
        if (result == null || isSimpleResult(result) || !shouldRenderAsJson(result)) {
            return result;
        }
        try {
            ObjectMapper mapper = this.applicationContext.getBean(ObjectMapper.class);
            return mapper.writeValueAsString(result);
        } catch (BeansException | JacksonException e) {
            return result;
        }
    }

    private boolean isSimpleResult(Object result) {
        return result instanceof CharSequence
                || result instanceof Number
                || result instanceof Boolean
                || result instanceof Character
                || result instanceof Enum<?>;
    }

    private boolean shouldRenderAsJson(Object result) {
        if (result instanceof Map<?, ?> || result instanceof Collection<?> || result.getClass().isArray()) {
            return true;
        }
        Package resultPackage = result.getClass().getPackage();
        String packageName = resultPackage != null ? resultPackage.getName() : "";
        return packageName.startsWith("org.springframework.boot.") && packageName.contains(".actuate.");
    }

    Method getMethod() {
        return this.method;
    }

    private static class MissingOptionException extends IllegalArgumentException {

        private final String optionName;

        private final String description;

        private MissingOptionException(String optionName, String description) {
            this.optionName = optionName;
            this.description = description;
        }
    }

    private static class MissingArgumentException extends IllegalArgumentException {

        private final int argumentIndex;

        private final String argumentName;

        private final String argumentType;

        private final String description;

        private MissingArgumentException(int argumentIndex, String argumentName, String argumentType,
                                         String description) {
            this.argumentIndex = argumentIndex;
            this.argumentName = argumentName;
            this.argumentType = argumentType;
            this.description = description;
        }
    }
}
