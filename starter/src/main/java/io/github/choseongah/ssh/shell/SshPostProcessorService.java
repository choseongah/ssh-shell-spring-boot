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

import io.github.choseongah.ssh.shell.postprocess.PostProcessor;
import io.github.choseongah.ssh.shell.postprocess.PostProcessorObject;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.choseongah.ssh.shell.SshShellCommandFactory.SSH_THREAD_CONTEXT;

/**
 * Service applying configured post processors on command results.
 */
@Component
public class SshPostProcessorService {

    private final Map<String, PostProcessor<?, ?>> postProcessorMap = new HashMap<>();

    public SshPostProcessorService(List<PostProcessor<?, ?>> postProcessors) {
        if (postProcessors != null) {
            for (PostProcessor<?, ?> postProcessor : postProcessors) {
                this.postProcessorMap.put(postProcessor.getName(), postProcessor);
            }
        }
    }

    public Object postProcess(Object result, PrintWriter outputWriter) {
        SshContext context = SSH_THREAD_CONTEXT.get();
        if (context == null || context.getPostProcessorsList().isEmpty()) {
            return result;
        }
        Object current = result;
        for (PostProcessorObject postProcessorObject : context.getPostProcessorsList()) {
            PostProcessor<?, ?> postProcessor = this.postProcessorMap.get(postProcessorObject.getName());
            if (postProcessor == null) {
                outputWriter.println(SshShellHelper.getColoredMessage(
                        "Unknown post processor [" + postProcessorObject.getName() + "]", PromptColor.YELLOW));
                outputWriter.flush();
                continue;
            }
            Class<?> inputClass = (Class<?>) ((ParameterizedType) postProcessor.getClass().getGenericInterfaces()[0])
                    .getActualTypeArguments()[0];
            if (!inputClass.isAssignableFrom(current.getClass())) {
                outputWriter.println(SshShellHelper.getColoredMessage(
                        "Post processor [" + postProcessorObject.getName() + "] can only apply to class ["
                                + inputClass.getName() + "] (current object class is "
                                + current.getClass().getName() + ")",
                        PromptColor.YELLOW));
                outputWriter.flush();
                continue;
            }
            try {
                current = apply(postProcessor, current, postProcessorObject.getParameters());
            } catch (Exception e) {
                context.setLastError(e);
                outputWriter.println(SshShellHelper.getColoredMessage(e.getMessage(), PromptColor.RED));
                outputWriter.flush();
                return null;
            }
        }
        return current;
    }

    public List<String> getPostProcessorNames() {
        return this.postProcessorMap.keySet().stream().sorted().toList();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object apply(PostProcessor postProcessor, Object result, List<String> parameters) throws Exception {
        return postProcessor.process(result, parameters);
    }
}
