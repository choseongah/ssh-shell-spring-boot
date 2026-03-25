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

package io.github.choseongah.ssh.shell.commands;

import io.github.choseongah.ssh.shell.SshShellHelper;
import io.github.choseongah.ssh.shell.SshShellProperties;
import org.jline.reader.History;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Override history command to get history per user if not shared
 */
@SshShellComponent("sshHistoryCommand")
@ConditionalOnProperty(
        name = SshShellProperties.SSH_SHELL_PREFIX + ".commands." + HistoryCommand.GROUP + ".enabled",
        havingValue = "true"
)
public class HistoryCommand extends AbstractCommand {

    public static final String GROUP = "history";
    public static final String COMMAND_HISTORY = GROUP;
    public static final String AVAILABILITY_PROVIDER = "historyAvailabilityProvider";
    public static final String COMPLETION_PROVIDER = "historyCompletionProvider";

    public HistoryCommand(SshShellProperties properties, SshShellHelper helper) {
        super(helper, properties, properties.getCommands().getHistory());
    }

    @Command(name = COMMAND_HISTORY, group = "Built-In Commands",
            description = "Display or save the history of previously run commands",
            availabilityProvider = AVAILABILITY_PROVIDER, completionProvider = COMPLETION_PROVIDER)
    public Object history(
            @Option(longName = "file", description = "A file to save history to.", defaultValue = "") File file,
            @Option(longName = "display-array",
                    description = "To display standard spring shell way (array.tostring). Default value: false",
                    defaultValue = "false") boolean displayArray
    ) throws IOException {
        List<String> result = new java.util.ArrayList<>();
        for (History.Entry entry : helper.getHistory()) {
            result.add(entry.line());
        }
        if (file != null) {
            Files.write(file.toPath(), result);
            return file.getAbsolutePath();
        } else if (displayArray) {
            return result;
        }
        StringBuilder sb = new StringBuilder();
        if (!CollectionUtils.isEmpty(result)) {
            result.forEach(h -> sb.append(h).append(System.lineSeparator()));
        }
        return sb.toString();
    }

    public Availability historyAvailability() {
        return availability(GROUP, COMMAND_HISTORY);
    }
}
