/*
 * Copyright (c) 2021 François Onimus
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

import io.github.choseongah.ssh.shell.auth.SshShellPublicKeyAuthenticationProvider;
import io.github.choseongah.ssh.shell.listeners.SshShellListenerService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.util.io.IoUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.RejectAllPublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.Banner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.autoconfigure.JLineShellAutoConfiguration;
import org.springframework.shell.jline.PromptProvider;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;

/**
 * Ssh shell configuration
 */

@Slf4j
@Configuration
@AllArgsConstructor
public class SshShellConfiguration {

    private final SshShellProperties properties;

    private final PasswordAuthenticator passwordAuthenticator;

    /**
     * Creates the ssh shell factory used as shell and command factory by sshd
     *
     * @param shellListenerService ssh shell listener service
     * @param shellBanner          optional spring banner
     * @param environment          spring environment
     * @param commandRegistry      spring shell command registry
     * @param commandParser        spring shell command parser
     * @param lineReader           default line reader
     * @param promptProviders      available prompt providers
     * @param postProcessorService ssh post processor service
     * @return ssh shell command factory
     */
    @Bean
    public SshShellCommandFactory sshShellCommandFactory(SshShellListenerService shellListenerService,
                                                         java.util.Optional<Banner> shellBanner,
                                                         Environment environment,
                                                         CommandRegistry commandRegistry,
                                                         CommandParser commandParser,
                                                         LineReader lineReader,
                                                         ConfigurableListableBeanFactory beanFactory,
                                                         Map<String, PromptProvider> promptProviders,
                                                         SshPostProcessorService postProcessorService) {
        PromptProvider promptProvider = resolveSshPromptProvider(beanFactory, promptProviders);
        return new SshShellCommandFactory(properties, shellListenerService, shellBanner, environment, commandRegistry,
                commandParser, lineReader, promptProvider, postProcessorService);
    }

    /**
     * Choose the SSH prompt provider once at bean creation time.
     * Prefer an application-defined prompt provider and otherwise fall back to SSH properties.
     *
     * @param beanFactory bean factory used to inspect prompt provider definitions
     * @param promptProviders available prompt providers keyed by bean name
     * @return prompt provider used by SSH sessions
     */
    private PromptProvider resolveSshPromptProvider(ConfigurableListableBeanFactory beanFactory,
                                                    Map<String, PromptProvider> promptProviders) {
        Map<String, PromptProvider> customPromptProviders = promptProviders.entrySet().stream()
                .filter(entry -> !isSpringShellDefaultPromptProvider(beanFactory, entry.getKey()))
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));

        if (customPromptProviders.size() > 1) {
            throw new IllegalStateException("Multiple custom PromptProvider beans found: " + customPromptProviders.keySet());
        }
        if (customPromptProviders.size() == 1) {
            return customPromptProviders.values().iterator().next();
        }
        return createPropertiesPromptProvider();
    }

    private PromptProvider createPropertiesPromptProvider() {
        AttributedString prompt = new AttributedString(properties.getPrompt().getText(),
                AttributedStyle.DEFAULT.foreground(properties.getPrompt().getColor().toJlineAttributedStyle()));
        return () -> prompt;
    }

    /**
     * Detect Spring Shell's fallback prompt provider so it can be replaced by the SSH properties prompt.
     *
     * @param beanFactory bean factory used to inspect bean metadata
     * @param beanName prompt provider bean name to inspect
     * @return true if the bean is Spring Shell's default prompt provider
     */
    private boolean isSpringShellDefaultPromptProvider(ConfigurableListableBeanFactory beanFactory, String beanName) {
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        return "promptProvider".equals(beanDefinition.getFactoryMethodName())
                && JLineShellAutoConfiguration.class.getName().equals(beanDefinition.getFactoryBeanName());
    }

    /**
     * Create the bean responsible for starting and stopping the SSH server
     *
     * @param sshServer the ssh server to manage
     * @return ssh server lifecycle
     */
    @Bean
    public SshServerLifecycle sshServerLifecycle(SshServer sshServer) {
        return new SshServerLifecycle(sshServer, this.properties);
    }

    /**
     * Construct ssh server thanks to ssh shell properties
     *
     * @return ssh server
     */
    @Bean
    public SshServer sshServer(SshShellCommandFactory shellCommandFactory) throws IOException {
        SshServer server = SshServer.setUpDefaultServer();
        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(properties.getHostKeyFile().toPath()));
        server.setHost(properties.getHost());
        server.setPasswordAuthenticator(passwordAuthenticator);
        server.setPublickeyAuthenticator(RejectAllPublickeyAuthenticator.INSTANCE);
        if (properties.getAuthorizedPublicKeys() != null) {
            if (properties.getAuthorizedPublicKeys().exists()) {
                server.setPublickeyAuthenticator(
                        new SshShellPublicKeyAuthenticationProvider(getFile(properties.getAuthorizedPublicKeys()))
                );
                LOGGER.info("Using authorized public keys from : {}",
                        properties.getAuthorizedPublicKeys().getDescription());
            } else {
                LOGGER.warn("Could not read authorized public keys from : {}, public key authentication is disabled.",
                        properties.getAuthorizedPublicKeys().getDescription());
            }
        }
        server.setPort(properties.getPort());
        server.setShellFactory(channelSession -> shellCommandFactory);
        server.setCommandFactory((channelSession, s) -> shellCommandFactory);
        return server;
    }

    private File getFile(Resource authorizedPublicKeys) throws IOException {
        if ("file".equals(authorizedPublicKeys.getURL().getProtocol())) {
            return authorizedPublicKeys.getFile();
        } else {
            File tmp = Files.createTempFile("sshShellPubKeys-", ".tmp").toFile();
            try (InputStream is = authorizedPublicKeys.getInputStream();
                 OutputStream os = Files.newOutputStream(tmp.toPath())) {
                IoUtils.copy(is, os);
            }
            tmp.deleteOnExit();
            LOGGER.info("Copying {} to following temporary file : {}", authorizedPublicKeys, tmp.getAbsolutePath());
            return tmp;
        }
    }

    /**
     * Ssh server lifecycle class used to start and stop ssh server
     */
    @RequiredArgsConstructor
    public static class SshServerLifecycle {

        private final SshServer sshServer;

        private final SshShellProperties properties;

        /**
         * Start ssh server
         *
         * @throws IOException in case of error
         */
        @PostConstruct
        public void startServer() throws IOException {
            sshServer.start();
            LOGGER.info("Ssh server started [{}:{}]", properties.getHost(), properties.getPort());
        }

        /**
         * Stop ssh server
         *
         * @throws IOException in case of error
         */
        @PreDestroy
        public void stopServer() throws IOException {
            sshServer.stop();
        }
    }
}
