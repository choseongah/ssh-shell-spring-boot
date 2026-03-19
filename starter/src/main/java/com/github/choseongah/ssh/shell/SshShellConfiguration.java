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

package com.github.choseongah.ssh.shell;

import com.github.choseongah.ssh.shell.auth.SshShellPublicKeyAuthenticationProvider;
import com.github.choseongah.ssh.shell.listeners.SshShellListenerService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.util.io.IoUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.RejectAllPublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.jline.reader.LineReader;
import org.springframework.boot.Banner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.jline.PromptProvider;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

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
     * @param promptProvider       prompt provider
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
                                                         PromptProvider promptProvider,
                                                         SshPostProcessorService postProcessorService) {
        return new SshShellCommandFactory(properties, shellListenerService, shellBanner, environment, commandRegistry,
                commandParser, lineReader, promptProvider, postProcessorService);
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
