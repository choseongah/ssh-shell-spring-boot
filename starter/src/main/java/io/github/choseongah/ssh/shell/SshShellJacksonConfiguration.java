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

import tools.jackson.databind.ObjectMapper;
import io.github.choseongah.ssh.shell.postprocess.provided.JsonPointerPostProcessor;
import io.github.choseongah.ssh.shell.postprocess.provided.PrettyJsonPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Additional configuration if ObjectMapper is on classpath
 */
@Configuration
@ConditionalOnClass(ObjectMapper.class)
public class SshShellJacksonConfiguration {

    @Bean
    public JsonPointerPostProcessor jsonPointerPostProcessor(ObjectMapper mapper) {
        return new JsonPointerPostProcessor(mapper);
    }

    @Bean
    public PrettyJsonPostProcessor prettyJsonPostProcessor(ObjectMapper mapper) {
        return new PrettyJsonPostProcessor(mapper);
    }
}
