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

import io.github.choseongah.ssh.shell.listeners.SshShellListenerService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.jline.reader.LineReader;
import org.springframework.boot.Banner;
import org.springframework.core.env.Environment;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.jline.PromptProvider;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SSH shell command implementation that starts per-session {@link SshShellRunnable}s.
 */
@Slf4j
@RequiredArgsConstructor
public class SshShellCommandFactory implements Command {

    public static final ThreadLocal<SshContext> SSH_THREAD_CONTEXT = ThreadLocal.withInitial(() -> null);

    public static final ThreadLocal<SshIO> SSH_IO_CONTEXT = ThreadLocal.withInitial(SshIO::new);

    @NonNull
    private final SshShellProperties properties;
    @NonNull
    private final SshShellListenerService shellListenerService;
    @NonNull
    private final Optional<Banner> shellBanner;
    @NonNull
    private final Environment environment;
    @NonNull
    private final CommandRegistry commandRegistry;
    @NonNull
    private final CommandParser commandParser;
    @NonNull
    private final LineReader lineReader;
    @NonNull
    private final PromptProvider promptProvider;
    @NonNull
    private final SshPostProcessorService postProcessorService;

    private final Map<ChannelSession, Thread> threads = new ConcurrentHashMap<>();

    @Override
    public void start(ChannelSession channelSession, org.apache.sshd.server.Environment sshEnv) {
        SshIO sshIO = SSH_IO_CONTEXT.get();
        Thread sshThread = new Thread(
                new ThreadGroup("ssh-shell"),
                new SshShellRunnable(
                        properties,
                        shellListenerService,
                        shellBanner.orElse(null),
                        commandRegistry,
                        commandParser,
                        lineReader,
                        promptProvider,
                        environment,
                        postProcessorService,
                        channelSession,
                        sshIO.getIs(),
                        sshIO.getOs(),
                        sshIO.getEc(),
                        sshEnv
                ), "ssh-session-" + channelSession.getServerSession().getIoSession().getId());
        sshThread.start();
        threads.put(channelSession, sshThread);
        SSH_IO_CONTEXT.remove();
        LOGGER.debug("{}: started [{} session(s) currently active]", channelSession, threads.size());
    }

    @Override
    public void destroy(ChannelSession channelSession) {
        Thread sshThread = threads.remove(channelSession);
        if (sshThread != null && sshThread != Thread.currentThread()) {
            sshThread.interrupt();
        }
        LOGGER.debug("{}: destroyed [{} session(s) currently active]", channelSession, threads.size());
    }

    @Override
    public void setErrorStream(OutputStream errOS) {
        // not used
    }

    @Override
    public void setExitCallback(ExitCallback ec) {
        SSH_IO_CONTEXT.get().setEc(ec);
    }

    @Override
    public void setInputStream(InputStream is) {
        SSH_IO_CONTEXT.get().setIs(is);
    }

    @Override
    public void setOutputStream(OutputStream os) {
        SSH_IO_CONTEXT.get().setOs(os);
    }

    public Map<Long, ChannelSession> listSessions() {
        return threads.keySet().stream()
                .collect(Collectors.toMap(s -> s.getServerSession().getIoSession().getId(), Function.identity()));
    }
}
