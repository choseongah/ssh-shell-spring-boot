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

import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.ExitStatus;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.availability.AvailabilityProvider;
import org.springframework.shell.core.command.completion.CompletionProvider;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ssh command registry with deterministic command replacement by name
 */
public class SshCommandRegistry extends CommandRegistry {

    private static final String UNAVAILABLE_PREFIX = "* ";

    @Override
    public void registerCommand(Command command) {
        super.getCommands().stream()
                .filter(existing -> existing.getName().equals(command.getName()))
                .findFirst()
                .ifPresent(this::unregisterCommand);
        super.registerCommand(command);
    }

    @Override
    public Set<Command> getCommands() {
        SshContext sshContext = SshShellCommandFactory.SSH_THREAD_CONTEXT.get();
        if (sshContext == null || sshContext.isLocalPrompt()) {
            return super.getCommands();
        }
        Set<Command> commands = new LinkedHashSet<>();
        for (Command command : super.getCommands()) {
            commands.add(markUnavailable(command));
        }
        return Set.copyOf(commands);
    }

    @Override
    public void afterSingletonsInstantiated() {
        // command registration is handled explicitly in auto configuration
    }

    private Command markUnavailable(Command command) {
        Availability availability = command.getAvailabilityProvider().get();
        if (availability == null || availability.isAvailable() || command.getName().startsWith(UNAVAILABLE_PREFIX)) {
            return command;
        }
        return new UnavailableCommand(command);
    }

    private static class UnavailableCommand implements Command {

        private final Command delegate;

        private UnavailableCommand(Command delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getName() {
            return UNAVAILABLE_PREFIX + delegate.getName();
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public String getHelp() {
            return delegate.getHelp();
        }

        @Override
        public boolean isHidden() {
            return delegate.isHidden();
        }

        @Override
        public String getGroup() {
            return delegate.getGroup();
        }

        @Override
        public List<String> getAliases() {
            return delegate.getAliases();
        }

        @Override
        public List<CommandOption> getOptions() {
            return delegate.getOptions();
        }

        @Override
        public AvailabilityProvider getAvailabilityProvider() {
            return delegate.getAvailabilityProvider();
        }

        @Override
        public CompletionProvider getCompletionProvider() {
            return delegate.getCompletionProvider();
        }

        @Override
        public ExitStatus execute(CommandContext commandContext) throws Exception {
            return delegate.execute(commandContext);
        }
    }
}
