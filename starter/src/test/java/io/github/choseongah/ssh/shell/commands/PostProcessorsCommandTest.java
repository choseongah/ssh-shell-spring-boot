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

package io.github.choseongah.ssh.shell.commands;

import io.github.choseongah.ssh.shell.SshShellHelper;
import io.github.choseongah.ssh.shell.SshShellProperties;
import io.github.choseongah.ssh.shell.postprocess.provided.GrepPostProcessor;
import io.github.choseongah.ssh.shell.postprocess.provided.JsonPointerPostProcessor;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PostProcessorsCommandTest {

    @Test
    void postprocessors() {
        GrepPostProcessor grep = new GrepPostProcessor();
        JsonPointerPostProcessor json = new JsonPointerPostProcessor(new ObjectMapper());
        String result = new PostProcessorsCommand(new SshShellHelper(null),
                new SshShellProperties(), Arrays.asList(grep, json)).postprocessors().toString();

        assertTrue(result.startsWith("Available Post-Processors"));
        assertTrue(result.contains(grep.getName()));
        assertTrue(result.contains(json.getName()));
    }
}
