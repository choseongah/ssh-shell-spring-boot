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

import io.github.choseongah.ssh.shell.conf.SshShellSessionConfigurationTest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {SshShellApplicationLazyInitTest.TestApplication.class, SshShellSessionConfigurationTest.class},
        properties = {
                "ssh.shell.port=2347",
                "ssh.shell.password=pass",
                "ssh.shell.shared-history=false",
                "ssh.shell.commands.manage-sessions.enabled=true",
                "management.endpoints.web.exposure.include=*",
                "spring.main.lazy-initialization=true",
                "spring.shell.interactive.enabled=false"
        }
)
@DirtiesContext
class SshShellApplicationLazyInitTest
        extends SshShellApplicationWebTest {

        // exactly same tests as extended test,
        // the aim is to test if application starts with spring.main.lazy-initialization=true

    @SpringBootApplication
    static class TestApplication {
    }
}
