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

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.shell.core.ShellRunner;
import org.springframework.shell.core.autoconfigure.TerminalCustomizer;

import static io.github.choseongah.ssh.shell.SshShellProperties.SSH_SHELL_ENABLE;

/**
 * Prevent spring shell non interactive runner from logging an error in ssh only mode.
 */
@AutoConfiguration
@ConditionalOnClass(ShellRunner.class)
@ConditionalOnProperty(name = SSH_SHELL_ENABLE, havingValue = "true", matchIfMissing = true)
public class SshShellRunnerAutoConfiguration {

    @Bean
    public static BeanFactoryPostProcessor sshShellRunnerSuppressor(Environment environment) {
        return beanFactory -> {
            boolean interactiveEnabled = environment.getProperty(
                    "spring.shell.interactive.enabled", Boolean.class, true
            );
            if (!interactiveEnabled) {
                // Spring Shell 4 still initializes local JLine infrastructure in SSH-only mode.
                // Enable ParsedLine support to avoid ExtendedDefaultParser compatibility warnings.
                setSystemPropertyIfAbsent(LineReader.PROP_SUPPORT_PARSEDLINE, Boolean.TRUE.toString());
                // Disable JNA terminal probing to avoid slow startup and reflective-access noise
                // when Spring Shell tries to resolve a local system terminal we do not use.
                setSystemPropertyIfAbsent(TerminalBuilder.PROP_JNA, Boolean.FALSE.toString());
            }
            if (beanFactory instanceof BeanDefinitionRegistry registry) {
                if (registry.containsBeanDefinition("historyCommand")) {
                    registry.removeBeanDefinition("historyCommand");
                }
                if (registry.containsBeanDefinition("scriptCommand")) {
                    registry.removeBeanDefinition("scriptCommand");
                }
                if (registry.containsBeanDefinition("commandRegistry")
                        && registry.containsBeanDefinition("sshCommandRegistry")) {
                    registry.removeBeanDefinition("commandRegistry");
                }
                if (registry.containsBeanDefinition("springShellApplicationRunner")) {
                    registry.removeBeanDefinition("springShellApplicationRunner");
                }
            }
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.shell.interactive", name = "enabled", havingValue = "false")
    public TerminalCustomizer sshOnlyTerminalCustomizer() {
        // In SSH-only mode, force Spring Shell's local fallback terminal to a dumb, non-system
        // terminal so startup does not log "Unable to create a system terminal".
        // SSH sessions still create their own TERM-aware terminal in SshShellRunnable.
        return builder -> builder
                .system(false)
                .streams(System.in, System.out)
                .dumb(true)
                .type(Terminal.TYPE_DUMB)
                .jna(false);
    }

    @Bean
    public ApplicationRunner sshShellApplicationRunner(ShellRunner shellRunner, Environment environment) {
        return args -> {
            boolean interactiveEnabled = environment.getProperty(
                    "spring.shell.interactive.enabled", Boolean.class, true);
            if (!interactiveEnabled && args.getNonOptionArgs().isEmpty()) {
                return;
            }
            shellRunner.run(args.getSourceArgs());
        };
    }

    private static void setSystemPropertyIfAbsent(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
