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
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.shell.core.autoconfigure.SpringShellAutoConfiguration;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SshShellApplicationRunnerOverrideTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestApplication.class)
            .withConfiguration(AutoConfigurations.of(
                    SpringShellAutoConfiguration.class,
                    SshShellRunnerAutoConfiguration.class
            ))
            .withPropertyValues(
                    "ssh.shell.enable=true",
                    "ssh.shell.port=0",
                    "spring.shell.interactive.enabled=false"
            );

    @Test
    void removesSpringShellDefaultApplicationRunnerBeanDefinition() {
        this.contextRunner.run(context -> {
            Map<String, ApplicationRunner> applicationRunners = context.getBeansOfType(ApplicationRunner.class);

            assertTrue(context.getBeanFactory().containsBeanDefinition("sshShellApplicationRunner"));
            assertFalse(context.getBeanFactory().containsBeanDefinition("springShellApplicationRunner"));
            assertFalse(applicationRunners.containsKey("springShellApplicationRunner"));
        });
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
