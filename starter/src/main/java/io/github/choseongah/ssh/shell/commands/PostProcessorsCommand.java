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
import io.github.choseongah.ssh.shell.postprocess.PostProcessor;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.annotation.Command;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Command to list available post processors
 */
@SshShellComponent("sshPostProcessorsCommand")
@ConditionalOnProperty(
        name = SshShellProperties.SSH_SHELL_PREFIX + ".commands." + PostProcessorsCommand.GROUP + ".enabled",
        havingValue = "true"
)
public class PostProcessorsCommand extends AbstractCommand {

    public static final String GROUP = "postprocessors";
    public static final String COMMAND_POST_PROCESSORS = "postprocessors";
    public static final String AVAILABILITY_PROVIDER = "postProcessorsAvailabilityProvider";

    private final List<PostProcessor<?, ?>> postProcessors;

    public PostProcessorsCommand(SshShellHelper helper, SshShellProperties properties,
                                 List<PostProcessor<?, ?>> postProcessors) {
        super(helper, properties, properties.getCommands().getPostprocessors());
        this.postProcessors = new ArrayList<>(postProcessors);
        this.postProcessors.sort(Comparator.comparing(PostProcessor::getName));
    }

    @Command(name = COMMAND_POST_PROCESSORS, group = "Built-In Commands",
            description = "Display the available post processors", availabilityProvider = AVAILABILITY_PROVIDER)
    public CharSequence postprocessors() {
        AttributedStringBuilder result = new AttributedStringBuilder();
        result.append("Available Post-Processors\n\n", AttributedStyle.BOLD);
        for (PostProcessor<?, ?> postProcessor : postProcessors) {
            result.append("\t" + postProcessor.getName() + ":\n", AttributedStyle.BOLD);
            Class<?> input = (Class<?>) ((ParameterizedType) postProcessor.getClass().getGenericInterfaces()[0])
                    .getActualTypeArguments()[0];
            Class<?> output = (Class<?>) ((ParameterizedType) postProcessor.getClass().getGenericInterfaces()[0])
                    .getActualTypeArguments()[1];
            result.append("\t\thelp   : " + postProcessor.getDescription() + "\n", AttributedStyle.DEFAULT);
            result.append("\t\tinput  : " + input.getName() + "\n", AttributedStyle.DEFAULT);
            result.append("\t\toutput : " + output.getName() + "\n", AttributedStyle.DEFAULT);
        }
        return result;
    }

    public Availability postprocessorsAvailability() {
        return availability(GROUP, COMMAND_POST_PROCESSORS);
    }
}
