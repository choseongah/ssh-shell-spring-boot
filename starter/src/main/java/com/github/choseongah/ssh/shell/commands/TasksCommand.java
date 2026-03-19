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

package com.github.choseongah.ssh.shell.commands;

import com.github.choseongah.ssh.shell.SimpleTable;
import com.github.choseongah.ssh.shell.SshShellHelper;
import com.github.choseongah.ssh.shell.SshShellProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

import static org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor.DEFAULT_TASK_SCHEDULER_BEAN_NAME;

/**
 * Command to list available post processors
 */
@SshShellComponent("sshTasksCommand")
@ConditionalOnBean(ScheduledTaskHolder.class)
@ConditionalOnProperty(
        name = SshShellProperties.SSH_SHELL_PREFIX + ".commands." + TasksCommand.GROUP + ".enabled",
        havingValue = "true"
)
public class TasksCommand extends AbstractCommand implements DisposableBean {

    public static final String GROUP = "tasks";
    private static final String COMMAND_TASKS_LIST = GROUP + "-list";
    private static final String COMMAND_TASKS_STOP = GROUP + "-stop";
    private static final String COMMAND_TASKS_RESTART = GROUP + "-restart";
    private static final String COMMAND_TASKS_SINGLE = GROUP + "-single";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    public static final String LIST_AVAILABILITY_PROVIDER = "tasksListAvailabilityProvider";
    public static final String STOP_AVAILABILITY_PROVIDER = "tasksStopAvailabilityProvider";
    public static final String RESTART_AVAILABILITY_PROVIDER = "tasksRestartAvailabilityProvider";
    public static final String SINGLE_AVAILABILITY_PROVIDER = "tasksSingleAvailabilityProvider";
    public static final String STOP_COMPLETION_PROVIDER = "tasksStopCompletionProvider";
    public static final String RESTART_COMPLETION_PROVIDER = "tasksRestartCompletionProvider";
    public static final String SINGLE_COMPLETION_PROVIDER = "tasksSingleCompletionProvider";

    private final java.util.Collection<ScheduledTaskHolder> scheduledTaskHolders;

    private final Map<String, TaskState> statesByName = new HashMap<>();

    private final ApplicationContext applicationContext;

    private TaskScheduler taskScheduler;

    public TasksCommand(SshShellHelper helper, SshShellProperties properties,
                        java.util.Collection<ScheduledTaskHolder> scheduledTaskHolders,
                        ApplicationContext applicationContext) {
        super(helper, properties, properties.getCommands().getTasks());
        this.scheduledTaskHolders = scheduledTaskHolders;
        this.applicationContext = applicationContext;
    }

    /**
     * <p>Specify specific task scheduler for task restart, use to set same scheduler if using registrar :</p>
     * <p>org.springframework.scheduling.annotation.SchedulingConfigurer#configureTasks(org.springframework
     * .scheduling.config.ScheduledTaskRegistrar)</p>
     * <p>Otherwise the one in context is used, if more than one, looking for the one named 'taskScheduler'</p>
     *
     * @param taskScheduler task scheduler
     */
    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    private TaskScheduler taskScheduler() {
        if (this.taskScheduler != null) {
            return this.taskScheduler;
        }
        Map<String, TaskScheduler> taskSchedulers = this.applicationContext.getBeansOfType(TaskScheduler.class);
        if (taskSchedulers.size() == 1) {
            this.taskScheduler = taskSchedulers.values().iterator().next();
        } else if (taskSchedulers.size() > 1) {
            this.taskScheduler = taskSchedulers.get(DEFAULT_TASK_SCHEDULER_BEAN_NAME);
        }
        if (this.taskScheduler == null) {
            this.taskScheduler = new ConcurrentTaskScheduler(Executors.newSingleThreadScheduledExecutor());
        }
        return this.taskScheduler;
    }

    public Set<String> getTaskNames() {
        return statesByName.keySet();
    }

    @Override
    public void destroy() {
        refresh(true);
        this.statesByName.values().stream().filter(s -> s.getFuture() != null).forEach(s -> s.getFuture().cancel(true));
    }

    @Command(name = COMMAND_TASKS_LIST, group = "Tasks Commands",
            description = "Display the available scheduled tasks",
            availabilityProvider = LIST_AVAILABILITY_PROVIDER)
    public String tasksList(
            @Option(longName = "status", description = "Filter on status (running, stopped)", defaultValue = "")
            TaskStatus status,
            @Option(longName = "refresh", description = "Refresh task from context", defaultValue = "false")
            boolean refresh
    ) {
        refresh(refresh);

        if (this.statesByName.isEmpty()) {
            return "No task found in context";
        }

        SimpleTable.SimpleTableBuilder builder = SimpleTable.builder()
                .column("Task").column("Running").column("Type").column("Trigger").column("Next execution");
        for (TaskState state : this.statesByName.values()) {
            if (status == null || state.getStatus() == status) {
                List<Object> line = new ArrayList<>();
                line.add(state.getName());
                if (state.getScheduledTask() != null) {
                    line.add(state.getStatus());
                    Task task = state.getScheduledTask().getTask();
                    if (task instanceof CronTask cronTask) {
                        line.add("cron");
                        line.add("expression : " + cronTask.getExpression());
                        Instant next = cronTask.getTrigger().nextExecution(new SimpleTriggerContext());
                        line.add(next == null ? "-"
                                : FORMATTER.format(next.atOffset(ZoneOffset.UTC).toLocalDateTime()));
                    } else if (task instanceof FixedDelayTask fixedDelayTask) {
                        line.add("fixed-delay");
                        line.add(getTrigger(fixedDelayTask));
                        line.add("-");
                    } else if (task instanceof FixedRateTask fixedRateTask) {
                        line.add("fixed-rate");
                        line.add(getTrigger(fixedRateTask));
                        line.add("-");
                    } else {
                        line.add("custom");
                        line.add("-");
                        line.add("-");
                    }
                } else {
                    line.add(state.getFuture() == null || state.getFuture().isDone() ? TaskStatus.stopped : TaskStatus.running);
                    line.add("single");
                    line.add("-");
                    line.add("never");
                }
                builder.line(line);
            }
        }

        return helper.renderTable(builder.build());
    }

    private void refresh(boolean refresh) {
        if (this.statesByName.isEmpty() || refresh) {
            if (refresh) {
                this.statesByName.entrySet().removeIf(
                        e -> e.getValue().getScheduledTask() == null
                                && (e.getValue().getFuture() == null || e.getValue().getFuture().isDone())
                );
            }
            for (ScheduledTaskHolder scheduledTaskHolder : this.scheduledTaskHolders) {
                for (ScheduledTask scheduledTask : scheduledTaskHolder.getScheduledTasks()) {
                    String taskName = getTaskName(scheduledTask.getTask().getRunnable());
                    this.statesByName.putIfAbsent(taskName, new TaskState(taskName, scheduledTask, TaskStatus.running,
                            null));
                }
            }
        }
    }

    @Command(name = COMMAND_TASKS_STOP, group = "Tasks Commands",
            description = "Stop all or specified task(s)",
            availabilityProvider = STOP_AVAILABILITY_PROVIDER, completionProvider = STOP_COMPLETION_PROVIDER)
    public String tasksStop(
            @Option(longName = "all", description = "Stop all tasks", defaultValue = "false") boolean all,
            @Option(longName = "task", description = "Task name to stop", defaultValue = "") String task
    ) {

        List<String> toStop = listTasks(all, task, true);
        if (toStop.isEmpty()) {
            return "No task to stop";
        }
        if (!helper.confirm("Do you really want to stop tasks " + toStop + " ?")) {
            return "Stop aborted";
        }

        List<String> stopped = new ArrayList<>();
        for (String taskName : toStop) {
            TaskState state = this.statesByName.get(taskName);
            if (state != null) {
                if (state.getStatus() == TaskStatus.running) {
                    if (state.getScheduledTask() != null) {
                        state.getScheduledTask().cancel();
                    }
                    if (state.getFuture() != null) {
                        state.getFuture().cancel(true);
                        state.setFuture(null);
                    }
                    state.setStatus(TaskStatus.stopped);
                    stopped.add(taskName);
                } else {
                    helper.printWarning("Task [" + taskName + "] already stopped.");
                }
            }
        }
        if (stopped.isEmpty()) {
            return "No task stopped";
        }
        return helper.getSuccess("Tasks " + stopped + " stopped");
    }

    @Command(name = COMMAND_TASKS_RESTART, group = "Tasks Commands",
            description = "Restart all or specified task(s)",
            availabilityProvider = RESTART_AVAILABILITY_PROVIDER, completionProvider = RESTART_COMPLETION_PROVIDER)
    public String tasksRestart(
            @Option(longName = "all", description = "Stop all tasks", defaultValue = "false") boolean all,
            @Option(longName = "task", description = "Task name to stop", defaultValue = "") String task
    ) {

        List<String> toRestart = listTasks(all, task, false);
        if (toRestart.isEmpty()) {
            return "No task to restart";
        }
        if (!helper.confirm("Do you really want to restart tasks " + toRestart + " ?")) {
            return "Restart aborted";
        }

        List<String> started = new ArrayList<>();
        for (String taskName : toRestart) {
            TaskState state = this.statesByName.get(taskName);
            if (state != null && state.getScheduledTask() != null) {
                if (state.getStatus() == TaskStatus.stopped) {
                    Task taskObj = state.getScheduledTask().getTask();
                    ScheduledFuture<?> future = null;
                    if (taskObj instanceof CronTask cronTask) {
                        future = taskScheduler().schedule(state.getScheduledTask().getTask().getRunnable(),
                                cronTask.getTrigger());
                    } else if (taskObj instanceof FixedDelayTask fixedDelayTask) {
                        future = taskScheduler().scheduleWithFixedDelay(
                                state.getScheduledTask().getTask().getRunnable(),
                                fixedDelayTask.getIntervalDuration());
                    } else if (taskObj instanceof FixedRateTask fixedRateTask) {
                        future = taskScheduler().scheduleAtFixedRate(
                                state.getScheduledTask().getTask().getRunnable(),
                                fixedRateTask.getIntervalDuration());
                    } else {
                        helper.printWarning("Task [" + taskName + "] of class [" + taskObj.getClass().getName() + "] "
                                + "cannot be restarted.");
                    }
                    if (future != null) {
                        state.setFuture(future);
                        state.setStatus(TaskStatus.running);
                        started.add(taskName);
                    }
                } else {
                    helper.printWarning("Task [" + taskName + "] already running.");
                }
            } else {
                helper.printWarning("Cannot relaunch this task execution [" + task + "]. Use the original task instead.");
            }
        }
        if (started.isEmpty()) {
            return "No task restarted";
        }
        return helper.getSuccess("Tasks " + started + " restarted");
    }

    @Command(name = COMMAND_TASKS_SINGLE, group = "Tasks Commands",
            description = "Launch one execution of all or specified task(s)",
            availabilityProvider = SINGLE_AVAILABILITY_PROVIDER, completionProvider = SINGLE_COMPLETION_PROVIDER)
    public String tasksSingle(
            @Option(longName = "all", description = "Launch one execution of all tasks", defaultValue = "false")
            boolean all,
            @Option(longName = "task", description = "Task name to launch once", defaultValue = "") String task
    ) {

        List<String> toLaunch = listTasks(all, task, true);
        if (!helper.confirm("Do you really want to launch tasks " + toLaunch + " ?")) {
            return "Launch aborted";
        }

        List<String> started = new ArrayList<>();
        for (String taskName : toLaunch) {
            TaskState state = this.statesByName.get(taskName);
            if (state.getScheduledTask() != null) {
                try {
                    String executionId = taskName + "-" + generateExecutionId();
                    ScheduledFuture<?> future = taskScheduler().schedule(
                            state.getScheduledTask().getTask().getRunnable(), Instant.now());
                    statesByName.put(executionId, new TaskState(executionId, null, TaskStatus.running, future));
                    started.add(executionId);
                } catch (TaskRejectedException e) {
                    helper.printError("The task '" + taskName + "' was not accepted for internal reasons");
                }
            } else if (task != null) {
                helper.printWarning("Cannot relaunch this task execution [" + task + "]. Use the original task instead.");
            }
        }
        return started.isEmpty() ? "No task started" : helper.getSuccess("Tasks " + started + " started");
    }

    private static String generateExecutionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private List<String> listTasks(boolean all, String task, boolean running) {
        refresh(false);
        List<String> result = new ArrayList<>();
        if (all) {
            TaskStatus filter = running ? TaskStatus.running : TaskStatus.stopped;
            result.addAll(this.statesByName.entrySet().stream().filter(e -> e.getValue().getStatus() == filter)
                    .map(Map.Entry::getKey).toList());
        } else {
            if (task == null || task.isEmpty()) {
                throw new IllegalArgumentException("You need to set either all option or task one");
            }
            if (!this.statesByName.containsKey(task)) {
                throw new IllegalArgumentException("Unknown task : " + task);
            }
            result.add(task);
        }
        return result;
    }

    private static String getTrigger(IntervalTask task) {
        return "interval : " + task.getIntervalDuration() +
                " (" + task.getIntervalDuration().toMillis() +
                "), init-delay : " + task.getInitialDelayDuration() +
                " (" + task.getInitialDelayDuration().toMillis() + ")";
    }

    private static String getTaskName(Runnable runnable) {
        Runnable target = unwrapRunnable(runnable);
        if (target instanceof ScheduledMethodRunnable scheduledMethodRunnable) {
            Method method = scheduledMethodRunnable.getMethod();
            return method.getDeclaringClass().getName() + "." + method.getName();
        } else {
            return target.getClass().getName();
        }
    }

    private static Runnable unwrapRunnable(Runnable runnable) {
        Runnable current = runnable;
        while (current != null) {
            Runnable delegate = getDelegate(current);
            if (delegate == null || delegate == current) {
                return current;
            }
            current = delegate;
        }
        return runnable;
    }

    private static Runnable getDelegate(Runnable runnable) {
        Class<?> type = runnable.getClass();
        while (type != null && type != Object.class) {
            for (String fieldName : List.of("runnable", "delegate", "task")) {
                try {
                    Field field = type.getDeclaredField(fieldName);
                    if (!Runnable.class.isAssignableFrom(field.getType())) {
                        continue;
                    }
                    field.setAccessible(true);
                    return (Runnable) field.get(runnable);
                } catch (NoSuchFieldException e) {
                    // Try with the next field name
                } catch (IllegalAccessException e) {
                    return null;
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    public Availability tasksListAvailability() {
        return availability(GROUP, COMMAND_TASKS_LIST);
    }

    public Availability tasksStopAvailability() {
        return availability(GROUP, COMMAND_TASKS_STOP);
    }

    public Availability tasksRestartAvailability() {
        return availability(GROUP, COMMAND_TASKS_RESTART);
    }

    public Availability tasksSingleAvailability() {
        return availability(GROUP, COMMAND_TASKS_SINGLE);
    }

    /**
     * Task state POJO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskState {

        private String name;

        private ScheduledTask scheduledTask;

        private TaskStatus status;

        private volatile ScheduledFuture<?> future;
    }

    /**
     * Task status enum
     */
    public enum TaskStatus {
        running, stopped
    }
}
