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

import io.github.choseongah.ssh.shell.auth.SshShellAuthenticationProvider;
import io.github.choseongah.ssh.shell.auth.SshShellPasswordAuthenticationProvider;
import io.github.choseongah.ssh.shell.auth.SshShellSecurityAuthenticationProvider;
import io.github.choseongah.ssh.shell.listeners.SshShellListener;
import io.github.choseongah.ssh.shell.listeners.SshShellListenerService;
import io.github.choseongah.ssh.shell.postprocess.provided.GrepPostProcessor;
import io.github.choseongah.ssh.shell.postprocess.provided.HighlightPostProcessor;
import io.github.choseongah.ssh.shell.postprocess.provided.SavePostProcessor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.server.SshServer;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.shell.core.autoconfigure.JLineShellAutoConfiguration;
import org.springframework.shell.core.autoconfigure.SpringShellAutoConfiguration;
import org.springframework.shell.core.autoconfigure.SpringShellProperties;

import jakarta.annotation.PostConstruct;
import java.util.List;

import static io.github.choseongah.ssh.shell.SshShellProperties.SSH_SHELL_ENABLE;
import static io.github.choseongah.ssh.shell.SshShellProperties.SSH_SHELL_PREFIX;

/**
 * <p>Ssh shell auto configuration</p>
 * <p>Can be disabled by property <b>ssh.shell.enable=false</b></p>
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(SshServer.class)
@ConditionalOnProperty(name = SSH_SHELL_ENABLE, havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({SshShellProperties.class})
@AutoConfigureAfter({
        SpringShellAutoConfiguration.class,
        JLineShellAutoConfiguration.class
})
@ComponentScan(basePackages = {"io.github.choseongah.ssh.shell"})
@AllArgsConstructor
public class SshShellAutoConfiguration {

    private final ApplicationContext context;
    private final SshShellProperties properties;
    private final SpringShellProperties springShellProperties;

    /**
     * Initialize ssh shell auto config
     */
    @PostConstruct
    public void init() {
        springShellProperties.getHistory().setName(properties.getHistoryFile().getAbsolutePath());
        springShellProperties.getCommand().getHistory().setEnabled(false);
        springShellProperties.getCommand().getScript().setEnabled(false);
    }

    @Bean
    @ConditionalOnProperty(value = "spring.main.lazy-initialization", havingValue = "true")
    public ApplicationListener<ContextRefreshedEvent> lazyInitApplicationListener() {
        return event -> {
            LOGGER.info("Lazy initialization enabled, calling configuration beans explicitly to start ssh server and initialize shell correctly");
            context.getBean(SshShellConfiguration.SshServerLifecycle.class);
            context.getBeansOfType(Terminal.class);
            context.getBeansOfType(LineReader.class);
        };
    }

    @Bean
    public SavePostProcessor savePostProcessor() {
        return new SavePostProcessor();
    }

    @Bean
    public GrepPostProcessor grepPostProcessor() {
        return new GrepPostProcessor();
    }

    @Bean
    public HighlightPostProcessor highlightPostProcessor() {
        return new HighlightPostProcessor();
    }

    @Bean
    public SshShellHelper sshShellHelper(ObjectProvider<Terminal> terminalProvider,
                                         ObjectProvider<LineReader> lineReaderProvider) {
        SshShellHelper helper = new SshShellHelper(properties.getConfirmationWords());
        helper.setDefaultTerminal(terminalProvider.getIfAvailable());
        helper.setDefaultLineReader(lineReaderProvider.getIfAvailable());
        return helper;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.security.authentication.AuthenticationManager")
    @ConditionalOnProperty(value = SSH_SHELL_PREFIX + ".authentication", havingValue = "security")
    public SshShellAuthenticationProvider sshShellSecurityAuthenticationProvider() {
        return new SshShellSecurityAuthenticationProvider(context, properties.getAuthProviderBeanName());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = SSH_SHELL_PREFIX + ".authentication", havingValue = "simple", matchIfMissing = true)
    public SshShellAuthenticationProvider sshShellSimpleAuthenticationProvider() {
        return new SshShellPasswordAuthenticationProvider(properties.getUser(), properties.getPassword());
    }

    /**
     * Creates ssh listener service
     *
     * @param listeners found listeners in context
     * @return listener service
     */
    @Bean
    public SshShellListenerService sshShellListenerService(@Autowired(required = false) List<SshShellListener> listeners) {
        return new SshShellListenerService(listeners);
    }
}
