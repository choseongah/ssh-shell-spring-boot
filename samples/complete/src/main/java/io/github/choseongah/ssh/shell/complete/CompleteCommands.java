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

package io.github.choseongah.ssh.shell.complete;

import io.github.choseongah.ssh.shell.PromptColor;
import io.github.choseongah.ssh.shell.SimpleTable;
import io.github.choseongah.ssh.shell.SshShellHelper;
import io.github.choseongah.ssh.shell.auth.SshAuthentication;
import io.github.choseongah.ssh.shell.commands.SshShellComponent;
import io.github.choseongah.ssh.shell.interactive.Interactive;
import io.github.choseongah.ssh.shell.interactive.KeyBinding;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.session.ServerSession;
import org.jline.terminal.Size;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.completion.CompletionContext;
import org.springframework.shell.core.command.completion.CompletionProposal;
import org.springframework.shell.core.command.completion.CompletionProvider;
import org.springframework.shell.jline.tui.table.BorderStyle;
import org.springframework.shell.jline.tui.table.SimpleHorizontalAligner;
import org.springframework.shell.jline.tui.table.SimpleVerticalAligner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Demo command for example
 */
@SshShellComponent
@AllArgsConstructor
public class CompleteCommands {

    public static final String ADMIN_AVAILABILITY_PROVIDER = "completeAdminAvailabilityProvider";
    public static final String CUSTOM_VALUES_PROVIDER = "customValuesProvider";

    private static final Logger LOGGER = LoggerFactory.getLogger(CompleteCommands.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SshShellHelper helper;

    /**
     * Echo command
     *
     * @param message message to print
     * @param color   color for the message
     * @return message
     */
    @Command(name = "echo", description = "Echo command", completionProvider = CUSTOM_VALUES_PROVIDER)
    public String echo(
            @Argument(index = 0) String message,
            @Option(longName = "color", description = "Color for the message", defaultValue = "") PromptColor color
    ) {
        if (color != null) {
            return new AttributedStringBuilder().append(message,
                    AttributedStyle.DEFAULT.foreground(color.toJlineAttributedStyle())).toAnsi();
        }
        return message;
    }

    /**
     * Wait for some time
     *
     * @param waitInMillis wait time
     * @return message
     */
    @Command(name = "wait", description = "Wait command")
    public String waitCmd(@Argument(index = 0) long waitInMillis) {
        try {
            Thread.sleep(waitInMillis);
        } catch (InterruptedException e) {
            LOGGER.warn("Got interrupted");
        }
        return "Waited " + waitInMillis + " milliseconds";
    }

    /**
     * Pojo command
     * <p>Try the post processors like pretty, grep with it</p>
     *
     * @return pojo
     */
    @Command(name = "pojo", description = "Pojo command")
    public Pojo pojo() {
        return new Pojo("value1", "value2");
    }

    /**
     * Confirmation example command
     *
     * @return welcome message
     */
    @Command(name = "conf", description = "Confirmation command")
    public String conf() {
        return helper.confirm("Are you sure ?") ? "Great ! Let's do it !" : "Such a shame ...";
    }

    /**
     * Terminal size command example
     *
     * @return size
     */
    @Command(name = "size", description = "Terminal size command")
    public Size size() {
        return helper.terminalSize();
    }

    /**
     * Simple table example command
     *
     * @return principal
     */
    @Command(name = "table-simple", description = "Simple table command")
    public String tableSimple() {
        return helper.renderTable(SimpleTable.builder()
                .column("col1")
                .column("col2")
                .column("col3")
                .column("col4")
                .line(Arrays.asList("line1 col1", "line1 col2", "line1 col3", "line1 col4"))
                .line(Arrays.asList("line2 col1", "line2 col2", "line2 col3", "line2 col4"))
                .line(Arrays.asList("line3 col1", "line3 col2", "line3 col3", "line3 col4"))
                .line(Arrays.asList("line4 col1", "line4 col2", "line4 col3", "line4 col4"))
                .line(Arrays.asList("line5 col1", "line5 col2", "line5 col3", "line5 col4"))
                .line(Arrays.asList("line6 col1", "line6 col2", "line6 col3", "line6 col4"))
                .build());
    }

    /**
     * Complex table example command
     *
     * @return principal
     */
    @Command(name = "table-complex", description = "Complex table command")
    public String tableComplex() {
        return helper.renderTable(SimpleTable.builder()
                .column("col1")
                .column("col2")
                .column("col3")
                .column("col4")
                .line(Arrays.asList("line1 col1", "line1 col2", "line1 col3", "line1 col4"))
                .line(Arrays.asList("line2 col1", "line2 col2", "line2 col3", "line2 col4"))
                .line(Arrays.asList("line3 col1", "line3 col2", "line3 col3", "line3 col4"))
                .line(Arrays.asList("line4 col1", "line4 col2", "line4 col3", "line4 col4"))
                .line(Arrays.asList("line5 col1", "line5 col2", "line5 col3", "line5 col4"))
                .line(Arrays.asList("line6 col1", "line6 col2", "line6 col3", "line6 col4"))
                .headerAligner(SimpleHorizontalAligner.right)
                .lineAligner(SimpleHorizontalAligner.left)
                .lineAligner(SimpleVerticalAligner.bottom)
                .useFullBorder(false)
                .borderStyle(BorderStyle.fancy_heavy_double_dash)
                .tableBuilderListener(tableBuilder -> {
                    tableBuilder.addInnerBorder(BorderStyle.fancy_light_double_dash);
                    tableBuilder.addOutlineBorder(BorderStyle.fancy_double);
                }).build());
    }

    /**
     * Progress displays command example
     *
     * @param progress current percentage
     */
    @Command(name = "progress", description = "Progress command")
    public void progress(@Argument(index = 0) int progress) {
        helper.printSuccess(progress + "%");
        helper.print(helper.progress(progress));
    }

    /**
     * File provider command example
     *
     * @param file     file to get info from
     * @param extended extended provider file to get info from
     */
    @Command(name = "file", description = "File command", completionProvider = "extendedFileCompletionProvider")
    public void file(
            @Argument(index = 0, defaultValue = "") File file,
            @Argument(index = 1, defaultValue = "") File extended
    ) {
        info(file);
        info(extended);
    }

    private void info(File file) {
        if (file != null) {
            if (file.exists()) {
                helper.printSuccess("File exists: " + file.getAbsolutePath());
                helper.print("\nType: " + (file.isDirectory() ? "directory" : "file"));
                helper.print("Size: " + file.length());
            } else {
                helper.printError("File does not exist: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Interactive command example
     *
     * @param fullscreen fullscreen mode
     * @param delay      delay in ms
     */
    @Command(name = "interactive", description = "Interactive command")
    public void interactive(
            @Option(longName = "fullscreen", description = "Fullscreen mode", defaultValue = "false")
            boolean fullscreen,
            @Option(longName = "delay", description = "Delay in ms", defaultValue = "3000") long delay
    ) {

        KeyBinding binding = KeyBinding.builder()
                .description("K binding example")
                .key("k").input(() -> LOGGER.info("In specific action triggered by key 'k' !")).build();

        Interactive interactive = Interactive.builder().input((size, currentDelay) -> {
            LOGGER.info("In interactive command for input...");
            List<AttributedString> lines = new ArrayList<>();
            AttributedStringBuilder sb = new AttributedStringBuilder(size.getColumns());

            sb.append("\nCurrent time", AttributedStyle.BOLD).append(" : ");
            sb.append(String.format("%8tT", new Date()));

            lines.add(sb.toAttributedString());

            lines.add(new AttributedStringBuilder().append(helper.progress(RANDOM.nextInt(100)),
                    AttributedStyle.DEFAULT.foreground(RANDOM.nextInt(6) + 1)).toAttributedString());
            lines.add(AttributedString.fromAnsi(SshShellHelper.INTERACTIVE_LONG_MESSAGE + "\n"));

            return lines;
        }).binding(binding).fullScreen(fullscreen).refreshDelay(delay).build();

        helper.interactive(interactive);
    }

    /**
     * Ex command
     *
     * @throws IllegalStateException for example
     */
    @Command(name = "ex", description = "Ex command")
    public void ex() {
        throw new IllegalStateException("Test exception message");
    }

    /**
     * Interaction example command
     *
     * @return welcome message
     */
    @Command(name = "welcome", description = "Welcome command")
    public String welcome() {
        helper.printInfo("You are now in the welcome command");
        String name = helper.read("What's your name ?");
        return "Hello, '" + name + "' !";
    }

    /**
     * Admin only example command
     *
     * @return welcome message
     */
    @Command(name = "admin", description = "Admin command",
            availabilityProvider = ADMIN_AVAILABILITY_PROVIDER)
    public String admin() {
        return "Finally an administrator !!";
    }

    /**
     * Check admin availability
     *
     * @return is admin
     */
    public Availability adminAvailability() {
        if (!helper.checkAuthorities(Collections.singletonList("ADMIN"))) {
            return Availability.unavailable("admin command is only for an admin users !");
        }
        return Availability.available();
    }

    /**
     * Authentication example command
     *
     * @return principal
     */
    @Command(name = "authentication", description = "Authentication command")
    public SshAuthentication authentication() {
        return helper.getAuthentication();
    }

    /**
     * Sleep command
     */
    @Command(name = "sleep", description = "Sleep command")
    public void sleep(@Argument(index = 0) long seconds) throws InterruptedException {
        Thread.sleep(seconds * 1000);
    }

    /**
     * Displays ssh env information
     *
     * @return table with ssh env information
     */
    @Command(name = "display-ssh-env", description = "Displays ssh env information")
    public String displaySshEnv() {
        Environment env = helper.getSshEnvironment();
        if (env == null) {
            return helper.getWarning("Not in a ssh session");
        }

        SimpleTable.SimpleTableBuilder builder = SimpleTable.builder().column("Property").column("Value");

        for (Map.Entry<String, String> e : env.getEnv().entrySet()) {
            builder.line(Arrays.asList(e.getKey(), e.getValue()));
        }

        return helper.renderTable(builder.build());
    }

    /**
     * Displays ssh session information
     *
     * @return table with ssh session information
     */
    @Command(name = "display-ssh-session", description = "Displays ssh session information")
    public String displaySshSession() {
        ServerSession session = helper.getSshSession();
        if (session == null) {
            return helper.getWarning("Not in a ssh session");
        }

        return helper.renderTable(SimpleTable.builder()
                .column("Property").column("Value")
                .line(Arrays.asList("Session id", session.getIoSession().getId()))
                .line(Arrays.asList("Local address", session.getIoSession().getLocalAddress()))
                .line(Arrays.asList("Remote address", session.getIoSession().getRemoteAddress()))
                .line(Arrays.asList("Acceptance address", session.getIoSession().getAcceptanceAddress()))
                .line(Arrays.asList("Server version", session.getServerVersion()))
                .line(Arrays.asList("Client version", session.getClientVersion()))
                .build());
    }

    /**
     * For scheduled command example
     */
    @Scheduled(initialDelay = 0, fixedDelay = 60000)
    public void logWithDelay() {
        LOGGER.info("In 'fixed-delay' scheduled task..");
    }

    /**
     * For scheduled command example
     */
    @Scheduled(initialDelay = 0, fixedRate = 60000)
    public void logWithRate() {
        LOGGER.info("In 'fixed-rate' scheduled task..");
    }

    /**
     * For scheduled command example
     */
    @Scheduled(cron = "0/59 * * * * *")
    public void logWithCron() {
        LOGGER.info("In 'cron' scheduled task..");
    }

    /**
     * For scheduled command example
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void logWithLongDuration() throws InterruptedException {
        LOGGER.info("In 'cron' scheduled task for a while..");
        Thread.sleep(100000);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Pojo {

        private String key1;

        private String key2;
    }
}

@Component(CompleteCommands.CUSTOM_VALUES_PROVIDER)
class CustomValuesProvider implements CompletionProvider {

    private final static String[] VALUES = new String[]{
            "message1", "message2", "message3"
    };

    @Override
    public List<CompletionProposal> apply(CompletionContext completionContext) {
        return Arrays.stream(VALUES).map(CompletionProposal::new).collect(Collectors.toList());
    }
}
