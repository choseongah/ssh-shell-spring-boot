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

import com.github.choseongah.ssh.shell.SshContext;
import com.github.choseongah.ssh.shell.SshShellCommandFactory;
import com.github.choseongah.ssh.shell.SshShellHelper;
import com.github.choseongah.ssh.shell.SshShellProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StacktraceCommandTest {

    @Test
    void stacktraceEmpty() {
        StacktraceCommand cmd = new StacktraceCommand(new SshShellHelper(null), new SshShellProperties());
        SshShellCommandFactory.SSH_THREAD_CONTEXT.set(null);
        try {
            assertEquals("", cmd.stacktrace());
        } finally {
            SshShellCommandFactory.SSH_THREAD_CONTEXT.remove();
        }
    }

    @Test
    void stacktraceWithLastError() {
        StacktraceCommand cmd = new StacktraceCommand(new SshShellHelper(null), new SshShellProperties());
        SshContext sshContext = new SshContext(null, null, null, null, null);
        sshContext.setLastError(new IllegalArgumentException("[TEST]"));
        SshShellCommandFactory.SSH_THREAD_CONTEXT.set(sshContext);
        try {
            String stacktrace = cmd.stacktrace();
            assertTrue(stacktrace.contains("IllegalArgumentException"));
            assertTrue(stacktrace.contains("[TEST]"));
        } finally {
            SshShellCommandFactory.SSH_THREAD_CONTEXT.remove();
        }
    }
}
