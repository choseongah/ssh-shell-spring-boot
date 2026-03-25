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

import io.github.choseongah.ssh.shell.commands.actuator.ActuatorCommand;
import io.github.choseongah.ssh.shell.commands.DatasourceCommand;
import io.github.choseongah.ssh.shell.commands.HistoryCommand;
import io.github.choseongah.ssh.shell.commands.JmxCommand;
import io.github.choseongah.ssh.shell.commands.ManageSessionsCommand;
import io.github.choseongah.ssh.shell.commands.PostProcessorsCommand;
import io.github.choseongah.ssh.shell.commands.ScriptCommand;
import io.github.choseongah.ssh.shell.commands.StacktraceCommand;
import io.github.choseongah.ssh.shell.commands.TasksCommand;
import io.github.choseongah.ssh.shell.commands.system.SystemCommand;
import io.github.choseongah.ssh.shell.completion.ExtendedFileCompletionProvider;
import io.github.choseongah.ssh.shell.manage.SshShellSessionManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.availability.AvailabilityProvider;
import org.springframework.shell.core.command.completion.CompletionContext;
import org.springframework.shell.core.command.completion.CompletionProposal;
import org.springframework.shell.core.command.completion.CompletionProvider;

import java.util.List;

/**
 * Additional ssh command configuration
 */
@Configuration
public class SshShellCommandsConfiguration {

    @Bean
    public SshShellSessionManager sshShellSessionManager(SshShellCommandFactory commandFactory) {
        return new SshShellSessionManager(commandFactory);
    }

    @Bean("sshHelpCommand")
    public Command sshHelpCommand() {
        return new SshHelpCommand();
    }

    @Bean(name = HistoryCommand.AVAILABILITY_PROVIDER)
    public AvailabilityProvider historyAvailabilityProvider(ObjectProvider<HistoryCommand> commandProvider) {
        return () -> {
            HistoryCommand command = commandProvider.getIfAvailable();
            return command != null ? command.historyAvailability() : Availability.available();
        };
    }

    @Bean(name = HistoryCommand.COMPLETION_PROVIDER)
    public CompletionProvider historyCompletionProvider(ExtendedFileCompletionProvider fileCompletionProvider) {
        return context -> isOption(context, "file") ? fileCompletionProvider.apply(context) : List.of();
    }

    @Bean(name = ActuatorCommand.AUDIT_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorAuditAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.auditAvailability() : Availability.available();
        };
    }

    @Bean(name = ActuatorCommand.BEANS_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorBeansAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.beansAvailability() : Availability.available();
        };
    }

    @Bean(name = ActuatorCommand.CONDITIONS_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorConditionsAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.conditionsAvailability() : Availability.available();
        };
    }

    @Bean(name = ActuatorCommand.CONFIGPROPS_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorConfigpropsAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.configpropsAvailability() : Availability.available();
        };
    }

    @Bean(name = ActuatorCommand.ENV_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorEnvAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.envAvailability() : Availability.available();
        };
    }

    @Bean(name = ActuatorCommand.HEALTH_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorHealthAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.healthAvailability() : Availability.available();
        };
    }

    @Bean(name = ActuatorCommand.HTTP_EXCHANGES_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorHttpExchangesAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.httpExchangesAvailability() : Availability.available();
        };
    }

    @Bean(name = ActuatorCommand.INFO_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorInfoAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.infoAvailability() : Availability.available();
        };
    }

    @Bean(name = ActuatorCommand.LOGGERS_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorLoggersAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.loggersAvailability() : Availability.available();
        };
    }

    @Bean(name = ActuatorCommand.METRICS_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorMetricsAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.metricsAvailability() : Availability.available();
        };
    }

    @Bean(name = ActuatorCommand.MAPPINGS_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorMappingsAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.mappingsAvailability() : Availability.available();
        };
    }

    @Bean(name = ActuatorCommand.SESSIONS_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorSessionsAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.sessionsAvailability() : Availability.available();
        };
    }

    @Bean(name = ActuatorCommand.SCHEDULED_TASKS_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorScheduledTasksAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.scheduledtasksAvailability() : Availability.available();
        };
    }

    @Bean(name = ActuatorCommand.SHUTDOWN_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorShutdownAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.shutdownAvailability() : Availability.available();
        };
    }

    @Bean(name = ActuatorCommand.THREAD_DUMP_AVAILABILITY_PROVIDER)
    public AvailabilityProvider actuatorThreadDumpAvailabilityProvider(ObjectProvider<ActuatorCommand> commandProvider) {
        return () -> {
            ActuatorCommand command = commandProvider.getIfAvailable();
            return command != null ? command.threaddumpAvailability() : Availability.available();
        };
    }

    @Bean(name = DatasourceCommand.LIST_AVAILABILITY_PROVIDER)
    public AvailabilityProvider datasourceListAvailabilityProvider(ObjectProvider<DatasourceCommand> commandProvider) {
        return () -> {
            DatasourceCommand command = commandProvider.getIfAvailable();
            return command != null ? command.datasourceListAvailability() : Availability.available();
        };
    }

    @Bean(name = DatasourceCommand.PROPERTIES_AVAILABILITY_PROVIDER)
    public AvailabilityProvider datasourcePropertiesAvailabilityProvider(ObjectProvider<DatasourceCommand> commandProvider) {
        return () -> {
            DatasourceCommand command = commandProvider.getIfAvailable();
            return command != null ? command.datasourcePropertiesAvailability() : Availability.available();
        };
    }

    @Bean(name = DatasourceCommand.QUERY_AVAILABILITY_PROVIDER)
    public AvailabilityProvider datasourceQueryAvailabilityProvider(ObjectProvider<DatasourceCommand> commandProvider) {
        return () -> {
            DatasourceCommand command = commandProvider.getIfAvailable();
            return command != null ? command.datasourceQueryAvailability() : Availability.available();
        };
    }

    @Bean(name = DatasourceCommand.UPDATE_AVAILABILITY_PROVIDER)
    public AvailabilityProvider datasourceUpdateAvailabilityProvider(ObjectProvider<DatasourceCommand> commandProvider) {
        return () -> {
            DatasourceCommand command = commandProvider.getIfAvailable();
            return command != null ? command.datasourceUpdateAvailability() : Availability.available();
        };
    }

    @Bean(name = DatasourceCommand.INDEX_COMPLETION_PROVIDER)
    public CompletionProvider datasourceIndexCompletionProvider(ObjectProvider<DatasourceCommand> commandProvider) {
        return context -> {
            DatasourceCommand command = commandProvider.getIfAvailable();
            return command != null && isOption(context, "id")
                    ? command.getDatasourceIndexes().stream().map(CompletionProposal::new).toList()
                    : List.of();
        };
    }

    @Bean(name = JmxCommand.LIST_AVAILABILITY_PROVIDER)
    public AvailabilityProvider jmxListAvailabilityProvider(ObjectProvider<JmxCommand> commandProvider) {
        return () -> {
            JmxCommand command = commandProvider.getIfAvailable();
            return command != null ? command.jmxListAvailability() : Availability.available();
        };
    }

    @Bean(name = JmxCommand.INFO_AVAILABILITY_PROVIDER)
    public AvailabilityProvider jmxInfoAvailabilityProvider(ObjectProvider<JmxCommand> commandProvider) {
        return () -> {
            JmxCommand command = commandProvider.getIfAvailable();
            return command != null ? command.jmxInfoAvailability() : Availability.available();
        };
    }

    @Bean(name = JmxCommand.INVOKE_AVAILABILITY_PROVIDER)
    public AvailabilityProvider jmxInvokeAvailabilityProvider(ObjectProvider<JmxCommand> commandProvider) {
        return () -> {
            JmxCommand command = commandProvider.getIfAvailable();
            return command != null ? command.jmxInvokeAvailability() : Availability.available();
        };
    }

    @Bean(name = JmxCommand.OBJECT_NAME_COMPLETION_PROVIDER)
    public CompletionProvider jmxObjectNameCompletionProvider(ObjectProvider<JmxCommand> commandProvider) {
        return context -> {
            JmxCommand command = commandProvider.getIfAvailable();
            return command != null && isOption(context, "object-name")
                    ? command.getObjectNames().stream().map(CompletionProposal::new).toList()
                    : List.of();
        };
    }

    @Bean(name = ScriptCommand.AVAILABILITY_PROVIDER)
    public AvailabilityProvider scriptAvailabilityProvider(ObjectProvider<ScriptCommand> commandProvider) {
        return () -> {
            ScriptCommand command = commandProvider.getIfAvailable();
            return command != null ? command.scriptAvailability() : Availability.available();
        };
    }

    @Bean(name = ScriptCommand.COMPLETION_PROVIDER)
    public CompletionProvider scriptCompletionProvider(ExtendedFileCompletionProvider fileCompletionProvider) {
        return context -> isOption(context, "file") ? fileCompletionProvider.apply(context) : List.of();
    }

    @Bean(name = StacktraceCommand.AVAILABILITY_PROVIDER)
    public AvailabilityProvider stacktraceAvailabilityProvider(ObjectProvider<StacktraceCommand> commandProvider) {
        return () -> {
            StacktraceCommand command = commandProvider.getIfAvailable();
            return command != null ? command.stacktraceAvailability() : Availability.available();
        };
    }

    @Bean(name = ManageSessionsCommand.LIST_AVAILABILITY_PROVIDER)
    public AvailabilityProvider manageSessionsListAvailabilityProvider(ObjectProvider<ManageSessionsCommand> commandProvider) {
        return () -> {
            ManageSessionsCommand command = commandProvider.getIfAvailable();
            return command != null ? command.manageSessionsListAvailability() : Availability.available();
        };
    }

    @Bean(name = ManageSessionsCommand.INFO_AVAILABILITY_PROVIDER)
    public AvailabilityProvider manageSessionsInfoAvailabilityProvider(ObjectProvider<ManageSessionsCommand> commandProvider) {
        return () -> {
            ManageSessionsCommand command = commandProvider.getIfAvailable();
            return command != null ? command.manageSessionsInfoAvailability() : Availability.available();
        };
    }

    @Bean(name = ManageSessionsCommand.STOP_AVAILABILITY_PROVIDER)
    public AvailabilityProvider manageSessionsStopAvailabilityProvider(ObjectProvider<ManageSessionsCommand> commandProvider) {
        return () -> {
            ManageSessionsCommand command = commandProvider.getIfAvailable();
            return command != null ? command.manageSessionsStopAvailability() : Availability.available();
        };
    }

    @Bean(name = ManageSessionsCommand.INFO_COMPLETION_PROVIDER)
    public CompletionProvider manageSessionsInfoCompletionProvider(ObjectProvider<ManageSessionsCommand> commandProvider,
                                                                  SshShellSessionManager sessionManager) {
        return context -> commandProvider.getIfAvailable() != null
                ? sessionIdCompletion(context, sessionManager) : List.of();
    }

    @Bean(name = ManageSessionsCommand.STOP_COMPLETION_PROVIDER)
    public CompletionProvider manageSessionsStopCompletionProvider(ObjectProvider<ManageSessionsCommand> commandProvider,
                                                                  SshShellSessionManager sessionManager) {
        return context -> commandProvider.getIfAvailable() != null
                ? sessionIdCompletion(context, sessionManager) : List.of();
    }

    @Bean(name = PostProcessorsCommand.AVAILABILITY_PROVIDER)
    public AvailabilityProvider postProcessorsAvailabilityProvider(ObjectProvider<PostProcessorsCommand> commandProvider) {
        return () -> {
            PostProcessorsCommand command = commandProvider.getIfAvailable();
            return command != null ? command.postprocessorsAvailability() : Availability.available();
        };
    }

    @Bean(name = TasksCommand.LIST_AVAILABILITY_PROVIDER)
    public AvailabilityProvider tasksListAvailabilityProvider(ObjectProvider<TasksCommand> commandProvider) {
        return () -> {
            TasksCommand command = commandProvider.getIfAvailable();
            return command != null ? command.tasksListAvailability() : Availability.available();
        };
    }

    @Bean(name = TasksCommand.STOP_AVAILABILITY_PROVIDER)
    public AvailabilityProvider tasksStopAvailabilityProvider(ObjectProvider<TasksCommand> commandProvider) {
        return () -> {
            TasksCommand command = commandProvider.getIfAvailable();
            return command != null ? command.tasksStopAvailability() : Availability.available();
        };
    }

    @Bean(name = TasksCommand.RESTART_AVAILABILITY_PROVIDER)
    public AvailabilityProvider tasksRestartAvailabilityProvider(ObjectProvider<TasksCommand> commandProvider) {
        return () -> {
            TasksCommand command = commandProvider.getIfAvailable();
            return command != null ? command.tasksRestartAvailability() : Availability.available();
        };
    }

    @Bean(name = TasksCommand.SINGLE_AVAILABILITY_PROVIDER)
    public AvailabilityProvider tasksSingleAvailabilityProvider(ObjectProvider<TasksCommand> commandProvider) {
        return () -> {
            TasksCommand command = commandProvider.getIfAvailable();
            return command != null ? command.tasksSingleAvailability() : Availability.available();
        };
    }

    @Bean(name = TasksCommand.STOP_COMPLETION_PROVIDER)
    public CompletionProvider tasksStopCompletionProvider(ObjectProvider<TasksCommand> commandProvider) {
        return context -> {
            TasksCommand command = commandProvider.getIfAvailable();
            return command != null ? taskCompletion(context, command) : List.of();
        };
    }

    @Bean(name = TasksCommand.RESTART_COMPLETION_PROVIDER)
    public CompletionProvider tasksRestartCompletionProvider(ObjectProvider<TasksCommand> commandProvider) {
        return context -> {
            TasksCommand command = commandProvider.getIfAvailable();
            return command != null ? taskCompletion(context, command) : List.of();
        };
    }

    @Bean(name = TasksCommand.SINGLE_COMPLETION_PROVIDER)
    public CompletionProvider tasksSingleCompletionProvider(ObjectProvider<TasksCommand> commandProvider) {
        return context -> {
            TasksCommand command = commandProvider.getIfAvailable();
            return command != null ? taskCompletion(context, command) : List.of();
        };
    }

    @Bean(name = SystemCommand.ENV_AVAILABILITY_PROVIDER)
    public AvailabilityProvider systemEnvAvailabilityProvider(ObjectProvider<SystemCommand> commandProvider) {
        return () -> {
            SystemCommand command = commandProvider.getIfAvailable();
            return command != null ? command.jvmEnvAvailability() : Availability.available();
        };
    }

    @Bean(name = SystemCommand.PROPERTIES_AVAILABILITY_PROVIDER)
    public AvailabilityProvider systemPropertiesAvailabilityProvider(ObjectProvider<SystemCommand> commandProvider) {
        return () -> {
            SystemCommand command = commandProvider.getIfAvailable();
            return command != null ? command.jvmPropertiesAvailability() : Availability.available();
        };
    }

    @Bean(name = SystemCommand.THREADS_AVAILABILITY_PROVIDER)
    public AvailabilityProvider systemThreadsAvailabilityProvider(ObjectProvider<SystemCommand> commandProvider) {
        return () -> {
            SystemCommand command = commandProvider.getIfAvailable();
            return command != null ? command.threadsAvailability() : Availability.available();
        };
    }

    @Bean
    public ExtendedFileCompletionProvider extendedFileCompletionProvider() {
        return new ExtendedFileCompletionProvider();
    }

    private List<CompletionProposal> sessionIdCompletion(CompletionContext context,
                                                         SshShellSessionManager sessionManager) {
        return isOption(context, "session-id")
                ? sessionManager.listSessions().keySet().stream()
                .map(id -> new CompletionProposal(id.toString()))
                .toList()
                : List.of();
    }

    private List<CompletionProposal> taskCompletion(CompletionContext context, TasksCommand command) {
        return isOption(context, "task")
                ? command.getTaskNames().stream().map(CompletionProposal::new).toList()
                : List.of();
    }

    private boolean isOption(CompletionContext context, String longName) {
        CommandOption option = context.getCommandOption();
        return option != null && longName.equals(option.longName());
    }
}
