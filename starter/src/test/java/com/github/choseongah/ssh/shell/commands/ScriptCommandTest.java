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
import org.junit.jupiter.api.Test;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScriptCommandTest {

    @Test
    void testFileNull() {
        ScriptCommand cmd = new ScriptCommand(mock(CommandParser.class), mock(CommandRegistry.class),
                new SshShellHelper(null), new SshShellProperties());

        assertThrows(IllegalArgumentException.class, () -> cmd.script(null, mock(CommandContext.class)));
    }

    @Test
    void testEmptyScript() throws Exception {
        ScriptCommand cmd = new ScriptCommand(mock(CommandParser.class), mock(CommandRegistry.class),
                new SshShellHelper(null), new SshShellProperties());
        CommandContext commandContext = mock(CommandContext.class);
        when(commandContext.outputWriter()).thenReturn(new PrintWriter(System.out));

        Path emptyFile = Files.createTempFile("ssh-shell-script", ".txt");
        try {
            cmd.script(emptyFile.toFile(), commandContext);
        } finally {
            Files.deleteIfExists(emptyFile);
        }
    }
}
