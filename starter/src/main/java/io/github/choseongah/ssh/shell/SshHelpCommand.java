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

import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandArgument;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.ExitStatus;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Arguments;
import org.springframework.shell.core.utils.Utils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

/**
 * Help command with ssh specific formatting
 */
public class SshHelpCommand implements Command {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Display help about available commands";
    }

    @Override
    public String getGroup() {
        return "Built-In Commands";
    }

    @Override
    public ExitStatus execute(CommandContext commandContext) {
        PrintWriter outputWriter = commandContext.outputWriter();
        CommandRegistry commandRegistry = commandContext.commandRegistry();
        String helpMessage = formatAvailableCommands(commandRegistry);
        List<CommandArgument> arguments = commandContext.parsedInput().arguments();
        String commandName = String.join(" ", arguments.stream().map(CommandArgument::value).toList());
        Command command = commandRegistry.getCommandByName(commandName);
        if (command != null) {
            helpMessage = getHelpMessageForCommand(command);
        } else {
            Command aliasCommand = commandRegistry.getCommandByAlias(commandName);
            if (aliasCommand != null) {
                helpMessage = getHelpMessageForCommand(aliasCommand);
            }
        }
        outputWriter.println(helpMessage);
        outputWriter.flush();
        return ExitStatus.OK;
    }

    private String formatAvailableCommands(CommandRegistry commandRegistry) {
        StringBuilder stringBuilder = new StringBuilder("AVAILABLE COMMANDS");
        stringBuilder.append(System.lineSeparator()).append(System.lineSeparator());
        Set<Command> commands = commandRegistry.getCommands();
        List<String> groups = commands.stream()
                .filter(command -> !command.isHidden())
                .map(Command::getGroup)
                .distinct()
                .sorted()
                .toList();
        for (String group : groups) {
            stringBuilder.append(group).append(System.lineSeparator());
            for (Command command : commands.stream()
                    .filter(c -> !c.isHidden())
                    .filter(c -> c.getGroup().equals(group))
                    .toList()) {
                stringBuilder.append("\t")
                        .append(command.getName())
                        .append(command.getAliases().isEmpty() ? "" : ", " + String.join(", ", command.getAliases()))
                        .append(": ")
                        .append(command.getDescription())
                        .append(System.lineSeparator());
            }
            stringBuilder.append(System.lineSeparator());
        }
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("Commands marked with (*) are currently unavailable.")
                .append(System.lineSeparator());
        stringBuilder.append("Type `help <command>` to learn more.")
                .append(System.lineSeparator());
        return stringBuilder.toString();
    }

    static String getHelpMessageForCommand(Command command) {
        StringBuilder helpMessageBuilder = new StringBuilder();
        List<HelpArgument> arguments = getArguments(command);
        appendName(command, helpMessageBuilder);
        appendSynopsis(command, arguments, helpMessageBuilder);
        appendArguments(arguments, helpMessageBuilder);
        appendOptions(command, helpMessageBuilder);
        appendAliases(command, helpMessageBuilder);
        return helpMessageBuilder.toString();
    }

    private static void appendName(Command command, StringBuilder helpMessageBuilder) {
        helpMessageBuilder.append("NAME")
                .append(System.lineSeparator())
                .append("\t")
                .append(command.getName())
                .append(" - ")
                .append(command.getDescription())
                .append(System.lineSeparator())
                .append(System.lineSeparator());
    }

    private static void appendSynopsis(Command command, List<HelpArgument> arguments, StringBuilder helpMessageBuilder) {
        List<CommandOption> options = command.getOptions();
        helpMessageBuilder.append("SYNOPSIS")
                .append(System.lineSeparator())
                .append("\t")
                .append(command.getName())
                .append(" ");

        for (HelpArgument argument : arguments) {
            if (argument.required()) {
                helpMessageBuilder.append("[");
            }
            helpMessageBuilder.append(argument.name()).append(" ").append(argument.type().getSimpleName());
            if (argument.required()) {
                helpMessageBuilder.append("]");
            }
            helpMessageBuilder.append(" ");
        }

        for (CommandOption option : options) {
            if (Boolean.TRUE.equals(option.required())) {
                helpMessageBuilder.append("[");
            }
            if (option.longName() != null) {
                helpMessageBuilder.append("--").append(option.longName());
            } else {
                helpMessageBuilder.append("-").append(option.shortName());
            }
            helpMessageBuilder.append(" ").append(option.type().getSimpleName());
            if (Boolean.TRUE.equals(option.required())) {
                helpMessageBuilder.append("]");
            }
            helpMessageBuilder.append(" ");
        }

        helpMessageBuilder.append("--help")
                .append(System.lineSeparator())
                .append(System.lineSeparator());
    }

    private static void appendArguments(List<HelpArgument> arguments, StringBuilder helpMessageBuilder) {
        if (arguments.isEmpty()) {
            return;
        }

        helpMessageBuilder.append("ARGUMENTS").append(System.lineSeparator());
        for (HelpArgument argument : arguments) {
            helpMessageBuilder.append("\t")
                    .append(argument.name())
                    .append(" ")
                    .append(argument.type().getSimpleName())
                    .append(System.lineSeparator());

            if (!argument.description().isBlank()) {
                helpMessageBuilder.append("\t")
                        .append(argument.description())
                        .append(System.lineSeparator());
            }

            if (argument.required()) {
                helpMessageBuilder.append("\t")
                        .append("[Mandatory]")
                        .append(System.lineSeparator())
                        .append(System.lineSeparator());
            } else {
                helpMessageBuilder.append("\t")
                        .append("[Optional, default = ")
                        .append(argument.defaultValue())
                        .append("]")
                        .append(System.lineSeparator())
                        .append(System.lineSeparator());
            }
        }
    }

    private static void appendOptions(Command command, StringBuilder helpMessageBuilder) {
        List<CommandOption> options = command.getOptions();
        helpMessageBuilder.append("OPTIONS").append(System.lineSeparator());
        if (!options.isEmpty()) {
            for (CommandOption option : options) {
                helpMessageBuilder.append("\t");
                if (option.longName() != null) {
                    helpMessageBuilder.append("--").append(option.longName());
                }
                if (option.shortName() != ' ') {
                    helpMessageBuilder.append(" or -").append(option.shortName());
                }
                helpMessageBuilder.append(" ").append(option.type().getSimpleName()).append(System.lineSeparator());
                helpMessageBuilder.append("\t").append(option.description()).append(System.lineSeparator());
                if (Boolean.TRUE.equals(option.required())) {
                    helpMessageBuilder.append("\t").append("[Mandatory]").append(System.lineSeparator())
                            .append(System.lineSeparator());
                } else {
                    helpMessageBuilder.append("\t").append("[Optional, default = ");
                    String defaultValue = option.defaultValue();
                    Class<?> optionType = option.type();
                    if (defaultValue == null && optionType.isPrimitive()) {
                        defaultValue = Utils.getDefaultValueForPrimitiveType(optionType).toString();
                    }
                    helpMessageBuilder.append(defaultValue).append("]").append(System.lineSeparator())
                            .append(System.lineSeparator());
                }
            }
        }
        helpMessageBuilder.append("\t--help or -h").append(System.lineSeparator());
        helpMessageBuilder.append("\thelp for ").append(command.getName()).append(System.lineSeparator());
        helpMessageBuilder.append("\t[Optional]").append(System.lineSeparator()).append(System.lineSeparator());
    }

    private static void appendAliases(Command command, StringBuilder helpMessageBuilder) {
        List<String> aliases = command.getAliases();
        if (!aliases.isEmpty()) {
            helpMessageBuilder.append("ALIASES").append(System.lineSeparator());
            helpMessageBuilder.append("\t").append(String.join(", ", aliases)).append(System.lineSeparator());
        }
    }

    private static List<HelpArgument> getArguments(Command command) {
        if (!(command instanceof SshMethodInvokerCommandAdapter adapter)) {
            return List.of();
        }

        Method method = adapter.getMethod();
        List<HelpArgument> arguments = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            Argument argument = parameter.getAnnotation(Argument.class);
            if (argument != null) {
                arguments.add(new HelpArgument(parameter.getName(), argument.description(), argument.defaultValue(),
                        parameter.getType(), argument.defaultValue().isEmpty()));
                continue;
            }

            Arguments argumentsAnnotation = parameter.getAnnotation(Arguments.class);
            if (argumentsAnnotation != null) {
                arguments.add(new HelpArgument(parameter.getName(), "", "", parameter.getType(), false));
            }
        }
        return arguments;
    }

    private record HelpArgument(String name, String description, String defaultValue, Class<?> type, boolean required) {
    }
}
