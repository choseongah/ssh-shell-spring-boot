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

package com.github.choseongah.ssh.shell.commands.actuator;

import com.github.choseongah.ssh.shell.SshShellHelper;
import com.github.choseongah.ssh.shell.SshShellProperties;
import com.github.choseongah.ssh.shell.commands.AbstractCommand;
import com.github.choseongah.ssh.shell.commands.SshShellComponent;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.audit.AuditEventsEndpoint;
import org.springframework.boot.actuate.autoconfigure.condition.ConditionsReportEndpoint;
import org.springframework.boot.actuate.beans.BeansEndpoint;
import org.springframework.boot.actuate.context.ShutdownEndpoint;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.actuate.management.ThreadDumpEndpoint;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint;
import org.springframework.boot.actuate.web.exchanges.HttpExchangesEndpoint;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.micrometer.metrics.actuate.endpoint.MetricsEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Actuator shell command
 */
@SshShellComponent("sshActuatorCommand")
@ConditionalOnClass(Endpoint.class)
@ConditionalOnProperty(
        name = SshShellProperties.SSH_SHELL_PREFIX + ".commands." + ActuatorCommand.GROUP + ".enabled",
        havingValue = "true"
)
public class ActuatorCommand extends AbstractCommand {

    public static final String GROUP = "actuator";

    public static final String AUDIT_AVAILABILITY_PROVIDER = "actuatorAuditAvailabilityProvider";
    public static final String BEANS_AVAILABILITY_PROVIDER = "actuatorBeansAvailabilityProvider";
    public static final String CONDITIONS_AVAILABILITY_PROVIDER = "actuatorConditionsAvailabilityProvider";
    public static final String CONFIGPROPS_AVAILABILITY_PROVIDER = "actuatorConfigpropsAvailabilityProvider";
    public static final String ENV_AVAILABILITY_PROVIDER = "actuatorEnvAvailabilityProvider";
    public static final String HEALTH_AVAILABILITY_PROVIDER = "actuatorHealthAvailabilityProvider";
    public static final String HTTP_EXCHANGES_AVAILABILITY_PROVIDER = "actuatorHttpExchangesAvailabilityProvider";
    public static final String INFO_AVAILABILITY_PROVIDER = "actuatorInfoAvailabilityProvider";
    public static final String LOGGERS_AVAILABILITY_PROVIDER = "actuatorLoggersAvailabilityProvider";
    public static final String METRICS_AVAILABILITY_PROVIDER = "actuatorMetricsAvailabilityProvider";
    public static final String MAPPINGS_AVAILABILITY_PROVIDER = "actuatorMappingsAvailabilityProvider";
    public static final String SESSIONS_AVAILABILITY_PROVIDER = "actuatorSessionsAvailabilityProvider";
    public static final String SCHEDULED_TASKS_AVAILABILITY_PROVIDER = "actuatorScheduledTasksAvailabilityProvider";
    public static final String SHUTDOWN_AVAILABILITY_PROVIDER = "actuatorShutdownAvailabilityProvider";
    public static final String THREAD_DUMP_AVAILABILITY_PROVIDER = "actuatorThreadDumpAvailabilityProvider";
    private static final String SESSIONS_ENDPOINT_CLASS_NAME =
            "org.springframework.boot.session.actuate.endpoint.SessionsEndpoint";

    private final ApplicationContext applicationContext;

    private final Environment environment;

    public ActuatorCommand(ApplicationContext applicationContext, Environment environment,
                           SshShellProperties properties, SshShellHelper helper) {
        super(helper, properties, properties.getCommands().getActuator());
        this.applicationContext = applicationContext;
        this.environment = environment;
    }

    /**
     * Audit method
     *
     * @param principal principal to filter with
     * @param type      to filter with
     * @return audit
     */
    @Command(name = "audit", group = "Actuator Commands", description = "Display audit endpoint.",
            availabilityProvider = AUDIT_AVAILABILITY_PROVIDER)
    public AuditEventsEndpoint.AuditEventsDescriptor audit(
            @Option(longName = "principal", description = "Principal to filter on", defaultValue = "") String principal,
            @Option(longName = "type", description = "Type to filter on", defaultValue = "") String type
    ) {
        return getEndpoint(AuditEventsEndpoint.class).events(emptyToNull(principal), null, emptyToNull(type));
    }

    /**
     * @return whether `audit` command is available
     */
    public Availability auditAvailability() {
        return availability("audit", AuditEventsEndpoint.class);
    }

    /**
     * Beans method
     *
     * @return beans
     */
    @Command(name = "beans", group = "Actuator Commands", description = "Display beans endpoint.",
            availabilityProvider = BEANS_AVAILABILITY_PROVIDER)
    public BeansEndpoint.BeansDescriptor beans() {
        return getEndpoint(BeansEndpoint.class).beans();
    }

    /**
     * @return whether `beans` command is available
     */
    public Availability beansAvailability() {
        return availability("beans", BeansEndpoint.class);
    }

    /**
     * Conditions method
     *
     * @return conditions
     */
    @Command(name = "conditions", group = "Actuator Commands", description = "Display conditions endpoint.",
            availabilityProvider = CONDITIONS_AVAILABILITY_PROVIDER)
    public ConditionsReportEndpoint.ConditionsDescriptor conditions() {
        return getEndpoint(ConditionsReportEndpoint.class).conditions();
    }

    /**
     * @return whether `conditions` command is available
     */
    public Availability conditionsAvailability() {
        return availability("conditions", ConditionsReportEndpoint.class);
    }

    /**
     * Config props method
     *
     * @return configprops
     */
    @Command(name = "configprops", group = "Actuator Commands", description = "Display configprops endpoint.",
            availabilityProvider = CONFIGPROPS_AVAILABILITY_PROVIDER)
    public ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesDescriptor configprops() {
        return getEndpoint(ConfigurationPropertiesReportEndpoint.class).configurationProperties();
    }

    /**
     * @return whether `configprops` command is available
     */
    public Availability configpropsAvailability() {
        return availability("configprops", ConfigurationPropertiesReportEndpoint.class);
    }

    /**
     * Environment method
     *
     * @param pattern pattern to filter with
     * @return env
     */
    @Command(name = "env", group = "Actuator Commands", description = "Display env endpoint.",
            availabilityProvider = ENV_AVAILABILITY_PROVIDER)
    public EnvironmentEndpoint.EnvironmentDescriptor env(
            @Option(longName = "pattern", description = "Pattern to filter on", defaultValue = "") String pattern) {
        return getEndpoint(EnvironmentEndpoint.class).environment(emptyToNull(pattern));
    }

    /**
     * @return whether `env` command is available
     */
    public Availability envAvailability() {
        return availability("env", EnvironmentEndpoint.class);
    }

    /**
     * Health method
     *
     * @return health
     */
    @Command(name = "health", group = "Actuator Commands", description = "Display health endpoint.",
            availabilityProvider = HEALTH_AVAILABILITY_PROVIDER)
    public Object health(
            @Option(longName = "path", description = "Path to query health (component name, group name)",
                    defaultValue = "")
            String path) {
        HealthEndpoint endpoint = getEndpoint(HealthEndpoint.class);
        String targetPath = emptyToNull(path);
        return targetPath != null ? endpoint.healthForPath(targetPath) : endpoint.health();
    }

    /**
     * @return whether `health` command is available
     */
    public Availability healthAvailability() {
        return availability("health", HealthEndpoint.class);
    }

    /**
     * Http traces method
     *
     * @return httptrace
     */
    @Command(name = "httpexchanges", group = "Actuator Commands",
            description = "Display httpexchanges endpoint.", availabilityProvider = HTTP_EXCHANGES_AVAILABILITY_PROVIDER)
    public HttpExchangesEndpoint.HttpExchangesDescriptor httptrace() {
        return getEndpoint(HttpExchangesEndpoint.class).httpExchanges();
    }

    /**
     * @return whether `httpexchanges` command is available
     */
    public Availability httpExchangesAvailability() {
        return availability("httpexchanges", HttpExchangesEndpoint.class);
    }

    /**
     * Info method
     *
     * @return info
     */
    @Command(name = "info", group = "Actuator Commands", description = "Display info endpoint.",
            availabilityProvider = INFO_AVAILABILITY_PROVIDER)
    public Map<String, Object> info() {
        return getEndpoint(InfoEndpoint.class).info();
    }

    /**
     * @return whether `info` command is available
     */
    public Availability infoAvailability() {
        return availability("info", InfoEndpoint.class);
    }

    /**
     * Loggers method
     *
     * @param action      action to make
     * @param loggerName  logger name for get or configure
     * @param loggerLevel logger level for configure
     * @return loggers
     */
    @Command(name = "loggers", group = "Actuator Commands", description = "Display or configure loggers.",
            availabilityProvider = LOGGERS_AVAILABILITY_PROVIDER)
    public Object loggers(
            @Option(longName = "action", description = "Action to perform", defaultValue = "list")
            LoggerAction action,
            @Option(longName = "logger-name", description = "Logger name for configuration or display",
                    defaultValue = "")
            String loggerName,
            @Option(longName = "logger-level", description = "Logger level for configuration", defaultValue = "")
            LogLevel loggerLevel) {
        LoggersEndpoint endpoint = getEndpoint(LoggersEndpoint.class);
        String targetLoggerName = emptyToNull(loggerName);
        if ((action == LoggerAction.get || action == LoggerAction.conf) && targetLoggerName == null) {
            throw new IllegalArgumentException("Logger name is mandatory for '" + action + "' action");
        }
        return switch (action) {
            case get -> {
                LoggersEndpoint.LoggerLevelsDescriptor levels = endpoint.loggerLevels(targetLoggerName);
                yield "Logger named [" + targetLoggerName + "] : [configured: " + levels.getConfiguredLevel() + "]";
            }
            case conf -> {
                if (loggerLevel == null) {
                    throw new IllegalArgumentException("Logger level is mandatory for '" + action + "' action");
                }
                endpoint.configureLogLevel(targetLoggerName, loggerLevel);
                yield "Logger named [" + targetLoggerName + "] now configured to level [" + loggerLevel + "]";
            }
            default -> endpoint.loggers();
        };
    }

    /**
     * @return whether `loggers` command is available
     */
    public Availability loggersAvailability() {
        return availability("loggers", LoggersEndpoint.class, true, true);
    }

    /**
     * Metrics method
     *
     * @param name metrics name to display
     * @param tags tags to filter with
     * @return metrics
     */
    @Command(name = "metrics", group = "Actuator Commands", description = "Display metrics endpoint.",
            availabilityProvider = METRICS_AVAILABILITY_PROVIDER)
    public Object metrics(
            @Option(longName = "name", description = "Metric name to get", defaultValue = "") String name,
            @Option(longName = "tags", description = "Tags (key=value, separated by coma)", defaultValue = "")
            String tags
    ) {
        MetricsEndpoint endpoint = getEndpoint(MetricsEndpoint.class);
        String metricName = emptyToNull(name);
        String metricTags = emptyToNull(tags);
        if (metricName != null) {
            MetricsEndpoint.MetricDescriptor result = endpoint.metric(metricName,
                    metricTags != null ? Arrays.asList(metricTags.split(",")) : null);
            if (result == null) {
                String tagsStr = metricTags != null ? " and tags: " + metricTags : "";
                throw new IllegalArgumentException("No result for metrics name: " + metricName + tagsStr);
            }
            return result;
        }
        return endpoint.listNames();
    }

    /**
     * @return whether `metrics` command is available
     */
    public Availability metricsAvailability() {
        return availability("metrics", MetricsEndpoint.class);
    }

    /**
     * Mappings method
     *
     * @return mappings
     */
    @Command(name = "mappings", group = "Actuator Commands", description = "Display mappings endpoint.",
            availabilityProvider = MAPPINGS_AVAILABILITY_PROVIDER)
    public MappingsEndpoint.ApplicationMappingsDescriptor mappings() {
        return getEndpoint(MappingsEndpoint.class).mappings();
    }

    /**
     * @return whether `mappings` command is available
     */
    public Availability mappingsAvailability() {
        return availability("mappings", MappingsEndpoint.class);
    }

    /**
     * Sessions method
     *
     * @return sessions
     */
    @Command(name = "sessions", group = "Actuator Commands", description = "Display sessions endpoint.",
            availabilityProvider = SESSIONS_AVAILABILITY_PROVIDER)
    public Object sessions() {
        return invokeEndpointMethod(SESSIONS_ENDPOINT_CLASS_NAME, "sessionsForUsername",
                new Class[]{String.class}, new Object[]{null});
    }

    /**
     * @return whether `sessions` command is available
     */
    public Availability sessionsAvailability() {
        return endpointAvailability("sessions", SESSIONS_ENDPOINT_CLASS_NAME);
    }

    /**
     * Scheduled tasks method
     *
     * @return scheduledtasks
     */
    @Command(name = "scheduledtasks", group = "Actuator Commands",
            description = "Display scheduledtasks endpoint.", availabilityProvider = SCHEDULED_TASKS_AVAILABILITY_PROVIDER)
    public ScheduledTasksEndpoint.ScheduledTasksDescriptor scheduledtasks() {
        return getEndpoint(ScheduledTasksEndpoint.class).scheduledTasks();
    }

    /**
     * @return whether `scheduledtasks` command is available
     */
    public Availability scheduledtasksAvailability() {
        return availability("scheduledtasks", ScheduledTasksEndpoint.class);
    }

    /**
     * Shutdown method
     *
     * @return shutdown message
     */
    @Command(name = "shutdown", group = "Actuator Commands", description = "Shutdown application.",
            availabilityProvider = SHUTDOWN_AVAILABILITY_PROVIDER)
    public String shutdown() {
        if (helper.confirm("Are you sure you want to shutdown application ?")) {
            helper.print("Shutting down application...");
            getEndpoint(ShutdownEndpoint.class).shutdown();
            return "";
        } else {
            return "Aborting shutdown";
        }
    }

    /**
     * @return whether `shutdown` command is available
     */
    public Availability shutdownAvailability() {
        return availability("shutdown", ShutdownEndpoint.class, false, true);
    }

    /**
     * Thread dump method
     *
     * @return threaddump
     */
    @Command(name = "threaddump", group = "Actuator Commands", description = "Display threaddump endpoint.",
            availabilityProvider = THREAD_DUMP_AVAILABILITY_PROVIDER)
    public ThreadDumpEndpoint.ThreadDumpDescriptor threaddump() {
        return getEndpoint(ThreadDumpEndpoint.class).threadDump();
    }

    /**
     * @return whether `threaddump` command is available
     */
    public Availability threaddumpAvailability() {
        return availability("threaddump", ThreadDumpEndpoint.class);
    }

    private Availability availability(String name, Class<?> clazz) {
        return availability(name, clazz, true, false);
    }

    private Availability availability(String name, Class<?> clazz, boolean defaultEnabled) {
        return availability(name, clazz, defaultEnabled, false);
    }

    private Availability availability(String name, Class<?> clazz, boolean defaultEnabled, boolean writeOperation) {
        Availability availability = availability(GROUP, name);
        boolean forbidden = availability.reason() != null && availability.reason().contains("forbidden");
        if (!availability.isAvailable() && (!forbidden || !"info".equals(name))) {
            return availability;
        }

        String enabledProperty = "management.endpoint." + name + ".enabled";
        if (!environment.getProperty(enabledProperty, Boolean.TYPE, defaultEnabled)) {
            return Availability.unavailable("endpoint '" + name + "' deactivated (please check property '"
                    + enabledProperty + "')");
        }

        String maxPermitted = environment.getProperty("management.endpoints.access.max-permitted");
        if ("none".equalsIgnoreCase(maxPermitted)) {
            return Availability.unavailable("endpoint '" + name
                    + "' is not accessible (please check property 'management.endpoints.access.max-permitted')");
        }
        if (writeOperation && "read-only".equalsIgnoreCase(maxPermitted)) {
            return Availability.unavailable("endpoint '" + name
                    + "' is read-only (please check property 'management.endpoints.access.max-permitted')");
        }

        String accessProperty = "management.endpoint." + name + ".access";
        String access = environment.getProperty(accessProperty);
        if ("none".equalsIgnoreCase(access)) {
            return Availability.unavailable("endpoint '" + name + "' is not accessible (please check property '"
                    + accessProperty + "')");
        }
        if (writeOperation && "read-only".equalsIgnoreCase(access)) {
            return Availability.unavailable("endpoint '" + name + "' is read-only (please check property '"
                    + accessProperty + "')");
        }

        try {
            applicationContext.getBean(clazz);
        } catch (NoSuchBeanDefinitionException e) {
            return Availability.unavailable(clazz.getName() + " is not in application context");
        }
        return Availability.available();
    }

    private Availability endpointAvailability(String name, String className) {
        return endpointAvailability(name, className, true, false);
    }

    private Availability endpointAvailability(String name, String className, boolean defaultEnabled,
                                             boolean writeOperation) {
        Class<?> clazz = resolveClass(className);
        if (clazz == null) {
            return Availability.unavailable(className + " is not on classpath");
        }
        return availability(name, clazz, defaultEnabled, writeOperation);
    }

    private <T> T getEndpoint(Class<T> endpointType) {
        return applicationContext.getBean(endpointType);
    }

    private Object invokeEndpointMethod(String className, String methodName, Class<?>[] parameterTypes,
                                        Object[] args) {
        Class<?> endpointClass = resolveClass(className);
        if (endpointClass == null) {
            throw new IllegalStateException(className + " is not on classpath");
        }
        Object endpoint = applicationContext.getBean(endpointClass);
        try {
            Method method = endpointClass.getMethod(methodName, parameterTypes);
            return method.invoke(endpoint, args);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to invoke " + className + "." + methodName, e);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Unable to invoke " + className + "." + methodName, targetException);
        }
    }

    private Class<?> resolveClass(String className) {
        try {
            return Class.forName(className, false, applicationContext.getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * Logger action enum
     */
    public enum LoggerAction {
        list, get, conf
    }
}
