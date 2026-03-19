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

package com.github.choseongah.ssh.shell;

import org.jline.reader.LineReader;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class SshShellTerminalAutoConfigurationTest {

    @Test
    void suppressesSystemTerminalFallbackLogsInSshOnlyMode(CapturedOutput output) {
        String previousSupportParsedLine = System.getProperty(LineReader.PROP_SUPPORT_PARSEDLINE);
        String previousJna = System.getProperty(TerminalBuilder.PROP_JNA);

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "ssh.shell.port=0",
                        "ssh.shell.password=pass",
                        "spring.shell.interactive.enabled=false")
                .run()) {
            assertTrue(context.containsBean("sshOnlyTerminalCustomizer"));
            assertEquals("true", System.getProperty(LineReader.PROP_SUPPORT_PARSEDLINE));
            assertEquals("false", System.getProperty(TerminalBuilder.PROP_JNA));
        } finally {
            restoreSystemProperty(LineReader.PROP_SUPPORT_PARSEDLINE, previousSupportParsedLine);
            restoreSystemProperty(TerminalBuilder.PROP_JNA, previousJna);
        }

        assertFalse(output.getOut().contains("Unable to create a system terminal"));
        assertFalse(output.getErr().contains("Unable to create a system terminal"));
    }

    private static void restoreSystemProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
