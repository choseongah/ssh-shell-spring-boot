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

package com.github.choseongah.ssh.shell;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

class SshHelperTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshHelperTest.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static void call(SshShellProperties properties, Executor executor) {
        call(properties.getUser(), properties.getPassword(), properties.getHost(), properties.getPort(), executor);
    }

    static void call(String user, String pass, SshShellProperties properties, Executor executor) {
        call(user, pass, properties.getHost(), properties.getPort(), executor);
    }

    static void call(String user, String pass, String host, int port, Executor executor) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, port);
            session.setPassword(pass);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            ChannelShell channel = (ChannelShell) session.openChannel("shell");
            // Tests run on platforms that may not provide `infocmp`, so request a
            // built-in dumb terminal instead of JSch's default vt100 PTY.
            channel.setPtyType("dumb");
            try (PipedInputStream pis = new PipedInputStream();
                 PipedOutputStream pos = new PipedOutputStream()) {
                channel.setInputStream(new PipedInputStream(pos));
                channel.setOutputStream(new PipedOutputStream(pis));
                channel.connect();
                executor.execute(pis, pos);
            } catch (Exception e) {
                fail(e.toString());
            } finally {
                channel.disconnect();
                session.disconnect();
            }
        } catch (JSchException ex) {
            fail(ex.toString());
        }
    }

    static void verifyResponse(InputStream pis, String response) {
        verifyResponse(pis, new String[]{response});
    }

    static void verifyResponse(InputStream pis, String... responses) {
        StringBuilder sb = new StringBuilder();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("Got interrupted exception while waiting");
        }
        try {
            await().atMost(Duration.ofSeconds(5)).until(() -> {
                while (true) {
                    sb.append((char) pis.read());
                    String s = sb.toString();
                    if (Arrays.stream(responses).anyMatch(s::contains)) {
                        break;
                    }
                }
                return true;
            });
        } finally {
            LOGGER.info("--------------- received::start ---------------");
            LOGGER.info(sb.toString());
            LOGGER.info("--------------- received::end   ---------------");
        }
    }

    static void verifyJsonResponse(InputStream pis, Object response) {
        verifyResponse(pis,
                OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(response),
                OBJECT_MAPPER.writeValueAsString(response));
    }

    static void write(OutputStream os, String... input) throws IOException {
        for (String s : input) {
            os.write((s + "\r").getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    @FunctionalInterface
    interface Executor {

        void execute(InputStream is, OutputStream os) throws Exception;
    }
}
