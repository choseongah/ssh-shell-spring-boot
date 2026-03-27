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

package io.github.choseongah.ssh.shell;

import io.github.choseongah.ssh.shell.auth.SshAuthentication;
import io.github.choseongah.ssh.shell.auth.SshShellAuthenticationProvider;
import io.github.choseongah.ssh.shell.listeners.SshShellListenerService;
import io.github.choseongah.ssh.shell.postprocess.PostProcessorObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.channel.exception.SshChannelClosedException;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.session.ServerSession;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.ExternalTerminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.Banner;
import org.springframework.core.env.Environment;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandExecutionException;
import org.springframework.shell.core.command.CommandExecutor;
import org.springframework.shell.core.command.CommandNotFoundException;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.ExitStatus;
import org.springframework.shell.core.command.ParsedInput;
import org.springframework.shell.jline.JLineInputReader;
import org.springframework.shell.jline.PromptProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.github.choseongah.ssh.shell.SshShellCommandFactory.SSH_THREAD_CONTEXT;

/**
 * Runnable for an SSH shell session.
 */
@SuppressWarnings("resource")
@Slf4j
@AllArgsConstructor
public class SshShellRunnable implements Runnable {

    private static final String SSH_ENV_COLUMNS = "COLUMNS";
    private static final String SSH_ENV_LINES = "LINES";
    private static final String SSH_ENV_TERM = "TERM";
    private static final String PIPE = "|";
    private static final String ARROW = ">";

    private final SshShellProperties properties;
    private final SshShellListenerService shellListenerService;
    private final Banner shellBanner;
    private final CommandRegistry commandRegistry;
    private final CommandParser commandParser;
    private final LineReader lineReaderTemplate;
    private final PromptProvider promptProvider;
    private final Environment environment;
    private final SshPostProcessorService postProcessorService;
    private final ChannelSession session;
    private final InputStream is;
    private final OutputStream os;
    private final ExitCallback ec;

    @Getter
    private final org.apache.sshd.server.Environment sshEnv;

    @Override
    public void run() {
        LOGGER.debug("{}: running...", session);
        Size terminalSize = resolveTerminalSize();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8);
             Terminal terminal = createTerminal()) {
            History history = new DefaultHistory();
            boolean sessionStarted = false;
            SshContext sshContext = null;
            try {
                configureTerminal(terminal, terminalSize);
                printBanner(baos, ps, terminal);

                LineReader reader = createLineReader(terminal, history);
                SshAuthentication authentication = resolveAuthentication();
                configureHistory(reader, history, authentication);

                sshContext = new SshContext(this, terminal, reader, history, authentication);
                SSH_THREAD_CONTEXT.set(sshContext);
                shellListenerService.onSessionStarted(session);
                sessionStarted = true;
                runShell(reader, sshContext);
                shellListenerService.onSessionStopped(session);
                LOGGER.debug("{}: closing", session);
                quit(0);
            } catch (Throwable e) {
                if (sshContext != null) {
                    sshContext.setLastError(e);
                }
                if (sessionStarted) {
                    shellListenerService.onSessionError(session);
                }
                LOGGER.error("{}: unexpected exception", session, e);
                quit(1);
            } finally {
                saveHistory(history);
                SSH_THREAD_CONTEXT.remove();
            }
        } catch (IOException e) {
            LOGGER.error("Unable to open terminal", e);
            quit(1);
        }
    }

    private void runShell(LineReader reader, SshContext sshContext) {
        CommandExecutor commandExecutor = new CommandExecutor(commandRegistry);
        boolean debugMode = environment.getProperty("spring.shell.debug.enabled", Boolean.class, false);
        while (true) {
            String input;
            try {
                input = reader.readLine(promptProvider.getPrompt().toAnsi(reader.getTerminal()));
                if (input == null || input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                    reader.getTerminal().writer().println("Exiting the shell");
                    break;
                }
                if (input.isEmpty()) {
                    continue;
                }
            } catch (Exception e) {
                sshContext.setLastError(e);
                if (debugMode) {
                    e.printStackTrace(reader.getTerminal().writer());
                }
                break;
            } finally {
                reader.getTerminal().flush();
            }

            String commandInput = applyPostProcessors(input, reader, sshContext);
            if (commandInput.isBlank()) {
                continue;
            }

            ParsedInput parsedInput;
            try {
                parsedInput = commandParser.parse(commandInput);
            } catch (Exception exception) {
                sshContext.setLastError(exception);
                reader.getTerminal().writer().println(errorMessage("Error while parsing command: " + exception.getMessage()));
                if (debugMode) {
                    exception.printStackTrace(reader.getTerminal().writer());
                }
                continue;
            }

            try {
                CommandContext commandContext = new CommandContext(parsedInput, commandRegistry,
                        reader.getTerminal().writer(), new JLineInputReader(reader));
                ExitStatus exitStatus = commandExecutor.execute(commandContext);
                if (ExitStatus.USAGE_ERROR.code() == exitStatus.code()
                        || ExitStatus.AVAILABILITY_ERROR.code() == exitStatus.code()) {
                    continue;
                }
                if (ExitStatus.OK.code() != exitStatus.code()) {
                    reader.getTerminal().writer().println(errorMessage(
                            "Error while executing command " + parsedInput.commandName() + ": "
                                    + exitStatus.description()));
                }
            } catch (CommandExecutionException executionException) {
                Throwable cause = rootCause(executionException);
                sshContext.setLastError(cause != null ? cause : executionException);
                printError(reader, executionMessage(cause != null ? cause : executionException));
                reader.getTerminal().writer().println(omittedDetailsMessage());
                reader.getTerminal().writer().flush();
                if (debugMode) {
                    executionException.printStackTrace(reader.getTerminal().writer());
                }
            } catch (CommandNotFoundException exception) {
                printError(reader, "No command found for '" + exception.getCommandName() + "'");
            } finally {
                reader.getTerminal().flush();
            }
        }
    }

    private String applyPostProcessors(String input, LineReader reader, SshContext sshContext) {
        sshContext.getPostProcessorsList().clear();
        int index = firstIndexOfKeyChar(input);
        if (index < 0) {
            return input;
        }
        String commandInput = input.substring(0, index).trim();
        String postProcessorInput = input.substring(index);
        ParsedLine parsedLine = reader.getParser().parse(
                postProcessorInput.replace(PIPE, " " + PIPE + " ").replace(ARROW, " " + ARROW + " "),
                postProcessorInput.length());
        List<String> words = parsedLine.words();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (PIPE.equals(word) && i + 1 < words.size()) {
                String postProcessorName = words.get(i + 1);
                List<String> parameters = new ArrayList<>();
                int currentIndex = i + 2;
                while (currentIndex < words.size()
                        && !PIPE.equals(words.get(currentIndex))
                        && !ARROW.equals(words.get(currentIndex))) {
                    parameters.add(words.get(currentIndex));
                    currentIndex++;
                }
                sshContext.getPostProcessorsList().add(new PostProcessorObject(postProcessorName, parameters));
            } else if (ARROW.equals(word) && i + 1 < words.size()) {
                sshContext.getPostProcessorsList().add(new PostProcessorObject("save",
                        Collections.singletonList(words.get(i + 1))));
            }
        }
        return commandInput;
    }

    private int firstIndexOfKeyChar(String input) {
        int pipe = input.indexOf(PIPE);
        int arrow = input.indexOf(ARROW);
        if (pipe < 0) {
            return arrow;
        }
        if (arrow < 0) {
            return pipe;
        }
        return Math.min(pipe, arrow);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private String errorMessage(String message) {
        return SshShellHelper.getColoredMessage(message, PromptColor.RED);
    }

    private void printError(LineReader reader, String message) {
        reader.getTerminal().writer().println(errorMessage(message));
        reader.getTerminal().writer().flush();
    }

    private String executionMessage(Throwable throwable) {
        if (throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
            return throwable.getMessage();
        }
        return throwable.toString();
    }

    private String omittedDetailsMessage() {
        AttributedStyle red = AttributedStyle.DEFAULT.foreground(PromptColor.RED.toJlineAttributedStyle());
        return new AttributedStringBuilder()
                .append("Details of the error have been omitted. You can use the ", red)
                .append("stacktrace", red.bold())
                .append(" command to print the full stacktrace.", red)
                .toAnsi();
    }

    private Terminal createTerminal() throws IOException {
        return new ExternalTerminal(
                "JLine terminal",
                sshEnv.getEnv().get(SSH_ENV_TERM),
                is,
                new SafeTerminalOutputStream(os),
                null
        );
    }

    private Size resolveTerminalSize() {
        if (!sshEnv.getEnv().containsKey(SSH_ENV_COLUMNS) || !sshEnv.getEnv().containsKey(SSH_ENV_LINES)) {
            return null;
        }
        try {
            return new Size(
                    Integer.parseInt(sshEnv.getEnv().get(SSH_ENV_COLUMNS)),
                    Integer.parseInt(sshEnv.getEnv().get(SSH_ENV_LINES))
            );
        } catch (NumberFormatException e) {
            LOGGER.debug("Unable to get terminal size : {}:{}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    private void configureTerminal(Terminal terminal, Size terminalSize) {
        Attributes attr = terminal.getAttributes();
        SshShellUtils.fill(attr, sshEnv.getPtyModes());
        terminal.setAttributes(attr);

        if (terminalSize != null) {
            terminal.setSize(terminalSize);
            sshEnv.addSignalListener((channel, signal) -> {
                Size updatedSize = resolveTerminalSize();
                if (updatedSize == null) {
                    return;
                }
                terminal.setSize(updatedSize);
                terminal.raise(Terminal.Signal.WINCH);
            }, Signal.WINCH);
        }
    }

    private void printBanner(ByteArrayOutputStream baos, PrintStream ps, Terminal terminal) {
        if (properties.isDisplayBanner() && shellBanner != null) {
            shellBanner.printBanner(environment, getClass(), ps);
        }
        String banner = baos.toString(StandardCharsets.UTF_8);
        if (!banner.isBlank()) {
            terminal.writer().print(banner);
        }
        terminal.writer().println("Please type `help` to see available commands");
        terminal.flush();
    }

    private LineReader createLineReader(Terminal terminal, History history) {
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .appName("Spring SSH Shell")
                .completer(new SshCommandCompleter(commandRegistry, postProcessorService))
                .history(history)
                .highlighter(lineReaderTemplate.getHighlighter())
                .parser(lineReaderTemplate.getParser())
                .build();
        reader.unsetOpt(LineReader.Option.INSERT_TAB);
        return reader;
    }

    private SshAuthentication resolveAuthentication() {
        Object authenticationObject = session.getSession().getIoSession()
                .getAttribute(SshShellAuthenticationProvider.AUTHENTICATION_ATTRIBUTE);
        if (authenticationObject == null) {
            return null;
        }
        if (!(authenticationObject instanceof SshAuthentication authentication)) {
            throw new IllegalStateException(
                    "Unknown authentication object class: " + authenticationObject.getClass().getName());
        }
        return authentication;
    }

    private void configureHistory(LineReader reader, History history, SshAuthentication authentication) throws IOException {
        File historyFile = resolveHistoryFile(authentication);
        Path historyPath = historyFile.toPath().toAbsolutePath();
        Path parent = historyPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        reader.setVariable(LineReader.HISTORY_FILE, historyPath);
        history.attach(reader);
        try {
            history.load();
        } catch (NoSuchFileException e) {
            LOGGER.debug("History file does not exist yet: {}", historyPath);
        }
    }

    private File resolveHistoryFile(SshAuthentication authentication) {
        if (properties.isSharedHistory()) {
            return properties.getHistoryFile();
        }
        String user = authentication != null ? authentication.getName() : "unknown";
        return new File(properties.getHistoryDirectory(), "sshShellHistory-" + user + ".log");
    }

    private void saveHistory(History history) {
        try {
            history.save();
        } catch (IOException e) {
            LOGGER.warn("Unable to save SSH command history", e);
        }
    }

    private void quit(int exitCode) {
        if (ec != null) {
            ec.onExit(exitCode);
        }
    }

    public ServerSession getSshSession() {
        return session.getSession();
    }

    @FunctionalInterface
    private interface IoOperation {

        void execute() throws IOException;
    }

    private final class SafeTerminalOutputStream extends OutputStream {

        private final OutputStream delegate;
        private volatile boolean closedChannel;

        private SafeTerminalOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            execute(() -> delegate.write(b));
        }

        @Override
        public void write(byte @NonNull [] b, int off, int len) throws IOException {
            execute(() -> delegate.write(b, off, len));
        }

        @Override
        public void flush() throws IOException {
            execute(delegate::flush);
        }

        @Override
        public void close() throws IOException {
            execute(delegate::close);
        }

        private void execute(IoOperation operation) throws IOException {
            if (closedChannel) {
                return;
            }
            try {
                operation.execute();
            } catch (IOException e) {
                handleWriteFailure(e);
            }
        }

        private void handleWriteFailure(IOException exception) throws IOException {
            if (isClosedChannelException(exception)) {
                closedChannel = true;
                LOGGER.debug("{}: terminal output ignored because SSH channel is already closed", session);
                return;
            }
            throw exception;
        }

        private boolean isClosedChannelException(Throwable throwable) {
            Throwable current = throwable;
            while (current != null) {
                if (current instanceof SshChannelClosedException) {
                    return true;
                }
                current = current.getCause();
            }
            return false;
        }
    }

}
