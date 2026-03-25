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

import io.github.choseongah.ssh.shell.SshShellCommandFactory;
import io.github.choseongah.ssh.shell.SshShellHelper;
import io.github.choseongah.ssh.shell.SshShellProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.annotation.Command;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Override stacktrace command to get error per thread
 */
@SshShellComponent("sshStacktraceCommand")
@ConditionalOnProperty(
        name = SshShellProperties.SSH_SHELL_PREFIX + ".commands." + StacktraceCommand.GROUP + ".enabled",
        havingValue = "true"
)
public class StacktraceCommand extends AbstractCommand {

    public static final String GROUP = "stacktrace";
    public static final String COMMAND_STACKTRACE = GROUP;
    public static final String AVAILABILITY_PROVIDER = "stacktraceAvailabilityProvider";

    public StacktraceCommand(SshShellHelper helper, SshShellProperties properties) {
        super(helper, properties, properties.getCommands().getStacktrace());
    }

    @Command(name = COMMAND_STACKTRACE, group = "Built-In Commands",
            description = "Display the full stacktrace of the last error.",
            availabilityProvider = AVAILABILITY_PROVIDER)
    public String stacktrace() {
        Throwable lastError = SshShellCommandFactory.SSH_THREAD_CONTEXT.get() != null
                ? SshShellCommandFactory.SSH_THREAD_CONTEXT.get().getLastError()
                : null;
        if (lastError == null) {
            return "";
        }
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        lastError.printStackTrace(printWriter);
        printWriter.flush();
        return stringWriter.toString();
    }

    public Availability stacktraceAvailability() {
        return availability(GROUP, COMMAND_STACKTRACE);
    }
}
