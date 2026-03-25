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
import io.github.choseongah.ssh.shell.postprocess.PostProcessorObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.session.ServerSession;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import java.util.ArrayList;
import java.util.List;

/**
 * Ssh context to hold terminal, history and thread specific objects
 */
@Getter
public class SshContext {

    private final SshShellRunnable sshShellRunnable;

    private final Terminal terminal;

    private final LineReader lineReader;

    private final History history;

    private final SshAuthentication authentication;

    private final List<PostProcessorObject> postProcessorsList = new ArrayList<>();

    @Setter
    private Throwable lastError;

    /**
     * Constructor
     *
     * @param sshShellRunnable ssh runnable
     * @param terminal         ssh terminal
     * @param lineReader       ssh line reader
     * @param history          ssh history
     * @param authentication   (optional) spring authentication objects
     */
    public SshContext(SshShellRunnable sshShellRunnable, Terminal terminal, LineReader lineReader, History history,
                      SshAuthentication authentication) {
        this.sshShellRunnable = sshShellRunnable;
        this.terminal = terminal;
        this.lineReader = lineReader;
        this.history = history;
        this.authentication = authentication;
    }

    /**
     * Check if current prompt is the one started with application
     *
     * @return if local prompt or not
     */
    public boolean isLocalPrompt() {
        return sshShellRunnable == null;
    }

    /**
     * Return current ssh session
     *
     * @return ssh session, or null if is local prompt
     */
    public ServerSession getSshSession() {
        return isLocalPrompt() ? null : sshShellRunnable.getSshSession();
    }

    /**
     * Return current ssh env
     *
     * @return ssh env, or null if is local prompt
     */
    public Environment getSshEnv() {
        return isLocalPrompt() ? null : sshShellRunnable.getSshEnv();
    }
}
