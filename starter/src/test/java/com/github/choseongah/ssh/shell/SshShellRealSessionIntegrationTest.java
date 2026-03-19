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

import com.github.choseongah.ssh.shell.auth.SshAuthentication;
import com.github.choseongah.ssh.shell.commands.SshShellComponent;
import com.github.choseongah.ssh.shell.listeners.SshShellEvent;
import com.github.choseongah.ssh.shell.listeners.SshShellEventType;
import com.github.choseongah.ssh.shell.listeners.SshShellListener;
import com.github.choseongah.ssh.shell.listeners.SshShellListenerService;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.server.channel.ChannelSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                SshShellRealSessionIntegrationTest.TestApplication.class,
                SshShellRealSessionIntegrationTest.RealSessionTestConfiguration.class
        },
        properties = {
                "ssh.shell.host=127.0.0.1",
                "ssh.shell.port=2355",
                "ssh.shell.authentication=security",
                "ssh.shell.authProviderBeanName=sshAuthenticationManager",
                "spring.shell.interactive.enabled=false",
                "logging.level.com.github.choseongah.ssh.shell.auth.SshShellSecurityAuthenticationProvider=OFF",
                "logging.level.com.github.choseongah.ssh.shell.auth.SshShellPublicKeyAuthenticationProvider=OFF",
                "logging.level.org.apache.sshd.server.session.ServerSessionImpl=OFF"
        })
class SshShellRealSessionIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final KeyPair AUTHORIZED_KEY_PAIR = generateRsaKeyPair();
    private static final Path AUTHORIZED_KEYS_FILE = writeAuthorizedKeys(AUTHORIZED_KEY_PAIR);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("ssh.shell.authorized-public-keys", () -> AUTHORIZED_KEYS_FILE.toUri().toString());
    }

    @Autowired
    private SshShellProperties properties;

    @Autowired
    private SessionEventCollector collector;

    @Autowired
    private SshShellListenerService listenerService;

    @BeforeEach
    void setUp() {
        collector.clear();
    }

    @Test
    void helperAndSecurityAuthenticationUseRealSession() throws Exception {
        SessionInfo info = SessionInfo.parse(executePasswordCommand("user", "pass", "test-session-info"));

        assertTrue(info.sessionId() > 0);
        assertEquals("user", info.name());
        assertEquals("principal-user", info.principal());
        assertEquals("details-user", info.details());
        assertEquals("credentials-user", info.credentials());
        assertEquals("ROLE_USER", info.authorities());
        assertEquals("dumb", info.term());
        assertEquals("true", info.historyPresent());
        assertEquals("false", info.localPrompt());
    }

    @Test
    void listenerServiceUsesRealSession() throws Exception {
        SessionInfo info = SessionInfo.parse(executePasswordCommand("actuator", "actuator", "test-session-info"));

        SessionEvent started = collector.awaitEvent(SshShellEventType.SESSION_STARTED);
        SessionEvent stopped = collector.awaitEvent(SshShellEventType.SESSION_STOPPED);

        assertEquals(info.sessionId(), started.sessionId());
        assertEquals(info.sessionId(), stopped.sessionId());
        assertEquals("actuator", started.username());
        assertNotNull(started.session());

        collector.clear();
        listenerService.onSessionError(started.session());

        SessionEvent error = collector.awaitEvent(SshShellEventType.SESSION_STOPPED_UNEXPECTEDLY);
        assertEquals(info.sessionId(), error.sessionId());
        assertEquals("actuator", error.username());
    }

    @Test
    void securityAuthenticationRejectsWrongPasswordOnRealSession() {
        assertThrows(IOException.class, () -> executePasswordCommand("user", "wrong-pass", "test-session-info"));
    }

    @Test
    void publicKeyAuthenticationUsesRealSession() throws Exception {
        SessionInfo info = SessionInfo.parse(executePublicKeyCommand("pub-user", AUTHORIZED_KEY_PAIR, "test-session-info"));

        assertTrue(info.sessionId() > 0);
        assertEquals("pub-user", info.name());
        assertEquals("pub-user", info.principal());
        assertEquals("", info.details());
        assertEquals("", info.credentials());
        assertEquals("", info.authorities());
        assertEquals("dumb", info.term());
        assertEquals("true", info.historyPresent());
        assertEquals("false", info.localPrompt());
    }

    @Test
    void publicKeyAuthenticationRejectsWrongKeyOnRealSession() {
        assertThrows(IOException.class, () -> executePublicKeyCommand("pub-user", generateRsaKeyPair(), "test-session-info"));
    }

    private String executePasswordCommand(String user, String password, String command) throws Exception {
        return executeCommand(user, session -> session.addPasswordIdentity(password), command);
    }

    private String executePublicKeyCommand(String user, KeyPair keyPair, String command) throws Exception {
        return executeCommand(user, session -> session.addPublicKeyIdentity(keyPair), command);
    }

    private String executeCommand(String user, SessionConfigurer configurer, String command) throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.setServerKeyVerifier((clientSession, remoteAddress, serverKey) -> true);
            client.start();
            try (ClientSession session = client.connect(user, properties.getHost(), properties.getPort())
                    .verify(TIMEOUT)
                    .getSession()) {
                configurer.configure(session);
                session.auth().verify(TIMEOUT);
                return executeShellCommand(session, command);
            } finally {
                client.stop();
            }
        }
    }

    private String executeShellCommand(ClientSession session, String command) throws Exception {
        try (ChannelShell channel = session.createShellChannel();
             PipedInputStream commandInput = new PipedInputStream();
             PipedOutputStream commandWriter = new PipedOutputStream(commandInput);
             PipedInputStream responseReader = new PipedInputStream();
             PipedOutputStream responseOutput = new PipedOutputStream(responseReader)) {
            channel.setUsePty(true);
            channel.setPtyType("dumb");
            channel.setIn(commandInput);
            channel.setOut(responseOutput);
            channel.open().verify(TIMEOUT);

            commandWriter.write((command + "\r").getBytes(StandardCharsets.UTF_8));
            commandWriter.flush();

            String output = readUntil(responseReader, "|END");
            channel.close(false).await(TIMEOUT);
            return output;
        }
    }

    private String readUntil(InputStream inputStream, String marker) {
        StringBuilder buffer = new StringBuilder();
        await().atMost(TIMEOUT).until(() -> {
            while (true) {
                int read = inputStream.read();
                if (read < 0) {
                    return buffer.toString().contains(marker);
                }
                buffer.append((char) read);
                if (buffer.toString().contains(marker)) {
                    return true;
                }
            }
        });
        return buffer.toString();
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate RSA key pair", e);
        }
    }

    private static Path writeAuthorizedKeys(KeyPair keyPair) {
        try {
            Path file = Files.createTempFile("ssh-shell-authorized-", ".keys");
            Files.writeString(file, PublicKeyEntry.toString(keyPair.getPublic()) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            file.toFile().deleteOnExit();
            return file;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to write authorized keys file", e);
        }
    }

    @SpringBootApplication
    static class TestApplication {
    }

    @TestConfiguration
    static class RealSessionTestConfiguration {

        @Bean
        AuthenticationManager sshAuthenticationManager() {
            return authentication -> {
                String username = authentication.getName();
                String password = String.valueOf(authentication.getCredentials());
                if ("user".equals(username) && "pass".equals(password)) {
                    UsernamePasswordAuthenticationToken token =
                            new UsernamePasswordAuthenticationToken("principal-user", "credentials-user",
                                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    token.setDetails("details-user");
                    return token;
                }
                if ("actuator".equals(username) && "actuator".equals(password)) {
                    UsernamePasswordAuthenticationToken token =
                            new UsernamePasswordAuthenticationToken("principal-actuator", "credentials-actuator",
                                    List.of(new SimpleGrantedAuthority("ROLE_ACTUATOR")));
                    token.setDetails("details-actuator");
                    return token;
                }
                throw new BadCredentialsException("[MOCK]");
            };
        }

        @Bean
        SessionEventCollector sessionEventCollector() {
            return new SessionEventCollector();
        }

        @Bean
        SshShellListener sessionEventListener(SessionEventCollector collector) {
            return collector::record;
        }

        @Bean
        TestSessionCommand testSessionCommand(SshShellHelper helper) {
            return new TestSessionCommand(helper);
        }
    }

    @SshShellComponent("testSessionCommand")
    static class TestSessionCommand {

        private final SshShellHelper helper;

        TestSessionCommand(SshShellHelper helper) {
            this.helper = helper;
        }

        @Command(name = "test-session-info", group = "Test Commands",
                description = "Expose current SSH session info for integration tests.")
        public String sessionInfo() {
            SshAuthentication authentication = helper.getAuthentication();
            String details = authentication != null && authentication.getDetails() != null
                    ? authentication.getDetails().toString() : "";
            String credentials = authentication != null && authentication.getCredentials() != null
                    ? authentication.getCredentials().toString() : "";
            String authorities = authentication != null && authentication.getAuthorities() != null
                    ? String.join(",", authentication.getAuthorities()) : "";
            String term = helper.getSshEnvironment() != null
                    ? String.valueOf(helper.getSshEnvironment().getEnv().getOrDefault("TERM", "")) : "";
            long sessionId = helper.getSshSession() != null
                    ? helper.getSshSession().getIoSession().getId() : -1L;
            String name = authentication != null ? authentication.getName() : "";
            String principal = authentication != null && authentication.getPrincipal() != null
                    ? authentication.getPrincipal().toString() : "";
            return "SESSION_INFO|" + sessionId + "|" + name + "|" + principal + "|" + details + "|" + credentials
                    + "|" + authorities + "|" + term + "|" + (helper.getHistory() != null) + "|"
                    + helper.isLocalPrompt() + "|END";
        }
    }

    static class SessionEventCollector {

        private final List<SessionEvent> events = new CopyOnWriteArrayList<>();

        void record(SshShellEvent event) {
            String username = event.getSession().getServerSession() != null
                    ? event.getSession().getServerSession().getUsername() : "";
            events.add(new SessionEvent(event.getType(), event.getSessionId(), username, event.getSession()));
        }

        void clear() {
            events.clear();
        }

        SessionEvent awaitEvent(SshShellEventType type) {
            await().atMost(TIMEOUT).until(() -> events.stream().anyMatch(event -> event.type() == type));
            return events.stream()
                    .filter(event -> event.type() == type)
                    .reduce((first, second) -> second)
                    .orElseThrow(() -> new IllegalStateException("Unable to find event of type " + type));
        }
    }

    record SessionEvent(SshShellEventType type, long sessionId, String username, ChannelSession session) {
    }

    record SessionInfo(long sessionId, String name, String principal, String details, String credentials,
                       String authorities, String term, String historyPresent, String localPrompt) {

        static SessionInfo parse(String output) {
            int start = output.indexOf("SESSION_INFO|");
            int end = output.indexOf("|END", start);
            if (start < 0 || end < 0) {
                throw new IllegalArgumentException("Unable to extract session info from output: " + output);
            }
            String[] values = output.substring(start, end).split("\\|", -1);
            return new SessionInfo(Long.parseLong(values[1]), values[2], values[3], values[4], values[5], values[6],
                    values[7], values[8], values[9]);
        }
    }

    @FunctionalInterface
    interface SessionConfigurer {

        void configure(ClientSession session);
    }
}
