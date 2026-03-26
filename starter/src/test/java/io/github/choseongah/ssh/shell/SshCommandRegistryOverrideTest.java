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

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.shell.core.command.CommandRegistry;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SshCommandRegistryOverrideTest {

    @Test
    void removesSpringShellDefaultCommandRegistryBeanDefinition() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "ssh.shell.port=0",
                        "ssh.shell.password=pass",
                        "spring.shell.interactive.enabled=false")
                .run()) {
            Map<String, CommandRegistry> commandRegistries = context.getBeansOfType(CommandRegistry.class);

            assertTrue(context.getBeanFactory().containsBeanDefinition("sshCommandRegistry"));
            assertFalse(context.getBeanFactory().containsBeanDefinition("commandRegistry"));
            assertEquals(1, commandRegistries.size());
            assertInstanceOf(SshCommandRegistry.class, commandRegistries.get("sshCommandRegistry"));
        }
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
