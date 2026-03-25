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

import io.github.choseongah.ssh.shell.conf.SshShellSecurityConfigurationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static io.github.choseongah.ssh.shell.SshHelperTest.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {SshShellApplicationSecurityTest.TestApplication.class, SshShellSecurityConfigurationTest.class},
        properties = {
                "ssh.shell.port=2346",
                "ssh.shell.password=pass",
                "ssh.shell.authentication=security",
                "ssh.shell.authProviderBeanName=authManager",
                "management.endpoints.web.exposure.include=*",
                "spring.session.store-type=none",
                "spring.autoconfigure.exclude=org.springframework.boot.session.autoconfigure.SessionAutoConfiguration,"
                        + "org.springframework.boot.session.autoconfigure.SessionsEndpointAutoConfiguration",
                "spring.shell.interactive.enabled=false"
        }
)
@DirtiesContext
class SshShellApplicationSecurityTest
        extends AbstractTest {

    @Test
    void testSshCallInfoCommandAdmin() {
        Map<String, Object> result = info.info();
        call("admin", "admin", properties, (is, os) -> {
            write(os, "info");
            verifyJsonResponse(is, result);
        });
    }

    @Test
    void testSshCallInfoCommandUser() {
        call("user", "password", properties, (is, os) -> {
            write(os, "health");
            verifyResponse(is, "forbidden for current user");
        });
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
