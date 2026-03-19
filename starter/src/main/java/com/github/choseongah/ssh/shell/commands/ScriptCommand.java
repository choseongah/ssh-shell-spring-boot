/*
 * Copyright (c) 2020 François Onimus
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

package com.github.choseongah.ssh.shell.commands;

import com.github.choseongah.ssh.shell.SshShellHelper;
import com.github.choseongah.ssh.shell.SshShellProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.shell.core.FileInputProvider;
import org.springframework.shell.core.NonInteractiveShellRunner;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.annotation.Option;

import java.io.File;

/**
 * Script command
 */
@SshShellComponent("sshScriptCommand")
@ConditionalOnProperty(
        name = SshShellProperties.SSH_SHELL_PREFIX + ".commands." + ScriptCommand.GROUP + ".create",
        havingValue = "true", matchIfMissing = true
)
public class ScriptCommand extends AbstractCommand {

    public static final String GROUP = "script";
    public static final String COMMAND_SCRIPT = GROUP;
    public static final String AVAILABILITY_PROVIDER = "scriptAvailabilityProvider";
    public static final String COMPLETION_PROVIDER = "scriptCompletionProvider";

    private final CommandParser commandParser;

    private final CommandRegistry commandRegistry;

    public ScriptCommand(CommandParser commandParser, CommandRegistry commandRegistry,
                         SshShellHelper helper, SshShellProperties properties) {
        super(helper, properties, properties.getCommands().getScript());
        this.commandParser = commandParser;
        this.commandRegistry = commandRegistry;
    }

    @Command(
            name = COMMAND_SCRIPT,
            group = "Built-In Commands",
            description = "Execute commands from a script file",
            availabilityProvider = AVAILABILITY_PROVIDER,
            completionProvider = COMPLETION_PROVIDER
    )
    public void script(
            @Option(shortName = 'f', longName = "file", required = true,
                    description = "The absolute path to the script file to execute")
            File file,
            CommandContext commandContext
    ) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("File is mandatory");
        }
        try (FileInputProvider inputProvider = new FileInputProvider(file)) {
            String input;
            while ((input = inputProvider.readInput()) != null) {
                executeCommand(commandContext, input);
            }
        }
    }

    private void executeCommand(CommandContext commandContext, String input) throws Exception {
        String[] commandTokens = input.split(" ");
        NonInteractiveShellRunner shellRunner = new NonInteractiveShellRunner(this.commandParser, this.commandRegistry,
                commandContext.outputWriter());
        shellRunner.run(commandTokens);
    }

    public Availability scriptAvailability() {
        return availability(GROUP, COMMAND_SCRIPT);
    }
}
