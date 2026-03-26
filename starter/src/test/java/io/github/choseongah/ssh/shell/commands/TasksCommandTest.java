package io.github.choseongah.ssh.shell.commands;

import io.github.choseongah.ssh.shell.SshShellHelper;
import io.github.choseongah.ssh.shell.SshShellProperties;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TasksCommandTest {

    @Test
    void taskStateStoresFutureInAtomicReference() {
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> initialFuture = mock(ScheduledFuture.class);
        TasksCommand.TaskState state =
                new TasksCommand.TaskState("task", null, TasksCommand.TaskStatus.running, initialFuture);

        assertSame(initialFuture, state.getFuture());

        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> updatedFuture = mock(ScheduledFuture.class);
        state.setFuture(updatedFuture);

        assertSame(updatedFuture, state.getFuture());
    }

    @Test
    void tasksRestartReschedulesStoppedCronTask() {
        SshShellHelper helper = mock(SshShellHelper.class);
        when(helper.confirm(anyString())).thenReturn(true);
        when(helper.getSuccess(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        Runnable runnable = () -> {
        };
        CronTask cronTask = new CronTask(runnable, "0 0 0 * * *");
        ScheduledTask scheduledTask = mock(ScheduledTask.class);
        when(scheduledTask.getTask()).thenReturn(cronTask);

        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));

        TasksCommand command = new TasksCommand(helper, new SshShellProperties(), List.of(), mock(ApplicationContext.class));
        command.setTaskScheduler(taskScheduler);

        TasksCommand.TaskState state =
                new TasksCommand.TaskState("task", scheduledTask, TasksCommand.TaskStatus.stopped, null);
        statesByName(command).put("task", state);

        String result = command.tasksRestart(false, "task");

        assertEquals(TasksCommand.TaskStatus.running, state.getStatus());
        assertSame(future, state.getFuture());
        assertTrue(result.contains("task"));
        verify(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void tasksSingleStoresSingleExecutionState() {
        SshShellHelper helper = mock(SshShellHelper.class);
        when(helper.confirm(anyString())).thenReturn(true);
        when(helper.getSuccess(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        Runnable runnable = () -> {
        };
        CronTask cronTask = new CronTask(runnable, "0 0 0 * * *");
        ScheduledTask scheduledTask = mock(ScheduledTask.class);
        when(scheduledTask.getTask()).thenReturn(cronTask);

        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        TasksCommand command = new TasksCommand(helper, new SshShellProperties(), List.of(), mock(ApplicationContext.class));
        command.setTaskScheduler(taskScheduler);

        statesByName(command).put("task",
                new TasksCommand.TaskState("task", scheduledTask, TasksCommand.TaskStatus.running, null));

        String result = command.tasksSingle(false, "task");

        Map<String, TasksCommand.TaskState> statesByName = statesByName(command);
        TasksCommand.TaskState singleExecution = statesByName.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("task"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow();

        assertEquals(2, statesByName.size());
        assertSame(future, singleExecution.getFuture());
        assertTrue(result.contains("started"));
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, TasksCommand.TaskState> statesByName(TasksCommand command) {
        return (Map<String, TasksCommand.TaskState>) ReflectionTestUtils.getField(command, "statesByName");
    }
}
