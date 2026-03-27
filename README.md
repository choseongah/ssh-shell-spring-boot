# Spring Boot Ssh Shell

[![Maven Central](https://img.shields.io/maven-central/v/io.github.choseongah/ssh-shell-spring-boot-starter.svg?label=maven%20central)](https://central.sonatype.com/artifact/io.github.choseongah/ssh-shell-spring-boot-starter)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=choseongah_ssh-shell-spring-boot&metric=alert_status)](https://sonarcloud.io/dashboard?id=choseongah_ssh-shell-spring-boot)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=choseongah_ssh-shell-spring-boot&metric=security_rating)](https://sonarcloud.io/dashboard?id=choseongah_ssh-shell-spring-boot)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=choseongah_ssh-shell-spring-boot&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=choseongah_ssh-shell-spring-boot)

> Maintained fork of François Onimus's original repository:
> [fonimus/ssh-shell-spring-boot](https://github.com/fonimus/ssh-shell-spring-boot)

---

> Spring shell in spring boot application over ssh

For more information please
visit `spring shell` [website](https://docs.spring.io/spring-shell/reference/4.0.1).

* [Getting started](#getting-started)
* [Configuration](#configuration)
* [Commands](#commands)
* [Post processors](#post-processors)
* [Parameter providers](#parameter-providers)
* [Custom authentication](#custom-authentication)
* [Command helper](#command-helper)
* [Banner](#banner)
* [Listeners](#listeners)
* [Session Manager](#session-manager)
* [Tests](#tests)
* [Samples](#samples)
* [Release notes](#release-notes)
* [For Next Maintainer](./docs/for-next-maintainer.md)

## Getting started

### Dependency

```xml
<dependency>
    <groupId>io.github.choseongah</groupId>
    <artifactId>ssh-shell-spring-boot-starter</artifactId>
</dependency>
```

_Warning:_ interactive shell is enabled by default.
You can set property `spring.shell.interactive.enabled=false` to disable it.

> **Note:** auto configuration `SshShellAutoConfiguration` (active by default)
> can be deactivated by property
> **ssh.shell.enable=false**.

It means that the ssh server won't start and the commands won't be scanned.
If you only want to use the application through SSH, you should usually also
disable the local interactive shell with the following property:

```yaml
spring:
  shell:
    interactive:
      enabled: false
```

### Configuration

Please check
class: [SshShellProperties.java](./starter/src/main/java/io/github/choseongah/ssh/shell/SshShellProperties.java)
for more
information.

```yaml
ssh:
  shell:
    enable: true
    host: 127.0.0.1
    port: 2222
    user: user
    # displayed in log if generated
    password:
    display-banner: true
    # 'simple' or 'security'
    authentication: simple
    # if authentication is set to 'security', the AuthenticationManager bean name
    # if not specified and only one AuthenticationManager bean is present in the context, it will be used
    auth-provider-bean-name:
    host-key-file: <java.io.tmpdir>/hostKey.ser
    # since 1.2.2, optional file containing authorized public keys (standard authorized_keys format, one key per line
    # starting with 'ssh-rsa'), takes precedence over authentication (simple or security)
    authorized-public-keys-file:
    # optional spring resource containing authorized public keys (file:, classpath:, etc)
    # note: in case of a non file resource, a temporary file can be created depending on resource type
    # this takes precedence over authentication (simple or security)
    authorized-public-keys:
    history-file: <java.io.tmpdir>/sshShellHistory.log
    # set to false have one file per user (<history-directory>/sshShellHistory-<user>.log)
    shared-history: true
    # only used if shared-history is set to false
    history-directory: <java.io.tmpdir>
    # for ssh helper 'confirm' method
    confirmation-words:
      - y
      - yes
    prompt:
      # in enum: io.github.choseongah.ssh.shell.PromptColor (black, red, green, yellow, blue, magenta, cyan, white, bright)
      color: white
      text: 'shell>'
    commands:
      # all starter command groups are disabled by default in the Spring Shell 4 line
      actuator:
        enabled: false
        restricted: true
        # empty by default
        includes:
          - ...
        # empty by default
        excludes:
          - ...
        authorized-roles:
          - ACTUATOR
      datasource:
        enabled: false
        restricted: true
        # empty by default
        includes:
          - ...
        # datasource-update is excluded by default
        excludes:
          - datasource-update
        authorized-roles:
          - ADMIN
      history:
        enabled: false
        restricted: true
        # empty by default
        includes:
          - ...
        # empty by default
        excludes:
          - ...
        authorized-roles:
          - ADMIN
      jmx:
        enabled: false
        restricted: true
        # empty by default
        includes:
          - ...
        # empty by default
        excludes:
          - ...
        authorized-roles:
          - ADMIN
      manage-sessions:
        enabled: false
        restricted: true
        # empty by default
        includes:
          - ...
        # empty by default
        excludes:
          - ...
        authorized-roles:
          - ADMIN
      postprocessors:
        enabled: false
        restricted: false
        # empty by default
        includes:
          - ...
        # empty by default
        excludes:
          - ...
        # not used if restricted is false
        authorized-roles:
          - ...
      script:
        enabled: false
        restricted: true
        # empty by default
        includes:
          - ...
        # empty by default
        excludes:
          - ...
        authorized-roles:
          - ADMIN
      stacktrace:
        enabled: false
        restricted: true
        # empty by default
        includes:
          - ...
        # empty by default
        excludes:
          - ...
        authorized-roles:
          - ADMIN
      system:
        enabled: false
        restricted: true
        # empty by default
        includes:
          - ...
        # empty by default
        excludes:
          - ...
        authorized-roles:
          - ADMIN
      tasks:
        enabled: false
        restricted: true
        # empty by default
        includes:
          - ...
        # empty by default
        excludes:
          - ...
        authorized-roles:
          - ADMIN
```

* Add `spring-boot-starter-actuator` dependency to get actuator commands

* Add `spring-boot-starter-security` dependency to
  configure `ssh.shell.authentication=security` with
  `AuthenticationManager`

* Role based restrictions are meaningful with `ssh.shell.authentication=security`
  because the default `simple` authentication does not attach authorities

#### Recommended settings

For a typical SSH only application, the following setup is the recommended
baseline:

```yaml
spring:
  shell:
    interactive:
      enabled: false

ssh:
  shell:
    authentication: security
    commands:
      actuator:
        enabled: true
      history:
        enabled: true
      script:
        enabled: true
      stacktrace:
        enabled: true
```

Recommended usage rules:

* Set `spring.shell.interactive.enabled=false` if the application is meant to
  be used through SSH only
* Prefer `ssh.shell.authentication=security` if you use restricted commands or
  `authorized-roles`
* Enable starter command groups explicitly with `ssh.shell.commands.<group>.enabled=true`
* Use `authorized-public-keys` or `authorized-public-keys-file` if public key
  authentication is preferred over password authentication
* Use `shared-history=false` with `history-directory` if you want one history
  file per SSH user

#### Supported settings with caveats

The following settings are supported, but need some context:

* `ssh.shell.authentication=simple` is supported, but restricted commands are
  effectively allowed for authenticated SSH users because no authorities are
  attached in simple mode.
* `ssh.shell.commands.<group>.restricted` and `authorized-roles` are meaningful
  for SSH sessions with Spring Security authorities; local prompt execution
  still bypasses authority checks.
* `management.endpoints.access.*` and `management.endpoint.<id>.access` are
  supported and recommended for actuator commands, but current implementation
  still also checks legacy `management.endpoint.<id>.enabled`.
* `ssh.shell.commands.actuator.restricted` is supported, but `info` is
  intentionally still available even when the rest of the actuator group is
  forbidden for the current SSH user.
* `authorized-public-keys` supports Spring `Resource` values such as `file:`
  and `classpath:`; for non-file resources the content is copied to a temporary
  file because sshd requires a file based authorized keys source.
* `ssh.shell.enable=false` disables this starter, but it is not a substitute
  for `spring.shell.interactive.enabled=false`.

#### Default behavior

All starter command groups are disabled by default.

To enable a group, set the **enabled** property to true:

```yaml
ssh:
  shell:
    commands:
      manage-sessions:
        enabled: true
```

If `enabled=false`, the command group bean is not created, the commands are not
registered, and they do not appear in `help`.

Sub commands in a group can be filtered by `includes` and `excludes`
properties:

```yaml
ssh:
  shell:
    commands:
      datasource:
        includes:
          - datasource-list
          - datasource-properties
        excludes:
          - datasource-update
```

To include all sub commands, set `excludes` to an empty array:

```yaml
ssh:
  shell:
    commands:
      datasource:
        enabled: true
        excludes:
```

### Writing commands

You can write your command exactly the way you would do with `spring shell`.
For more information please
visit `spring shell` [website](https://docs.spring.io/spring-shell/reference/).

Instead of using a regular `@Component`, you can use
`io.github.choseongah.ssh.shell.commands.SshShellComponent`:
it is just a conditional `@Component` with `@ConditionalOnProperty` on
property **ssh.shell.enable**.

Commands themselves are declared with Spring Shell 4 annotations such as
`@Command`, `@Argument` and `@Option`.

Example:

```java
import io.github.choseongah.ssh.shell.commands.SshShellComponent;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

@SshShellComponent
public class TestCommands {

    @Command(name = "test", group = "Test Commands", description = "test command")
    public String test(
            @Argument(index = 0, defaultValue = "world") String message,
            @Option(longName = "uppercase", defaultValue = "false") boolean uppercase
    ) {
        return uppercase ? message.toUpperCase() : message;
    }
}
```

## Commands

All command groups can be activated or deactivated by `enabled` property:

```yaml
ssh:
  shell:
    commands:
      <command>:
        enabled: true
```

Sub commands in group can also be filtered by `includes` and `excludes`
properties:

```yaml
ssh:
  shell:
    commands:
      <command>:
        includes:
          - xxx
        excludes:
          - xxx
```

### Actuator

If `org.springframework.boot:spring-boot-starter-actuator` dependency is
present and `ssh.shell.commands.actuator.enabled=true`,
actuator commands can be available.

Command availability is bound to endpoint access and bean presence.

For Spring Boot 4, actuator access is based on `management.endpoints.access`
and endpoint specific `access` properties.

Current implementation also still checks `management.endpoint.<id>.enabled`
in addition to the Spring Boot 4 `access` properties.

Example:

```yaml
management:
  endpoints:
    access:
      default: unrestricted
    web:
      exposure:
        include: "*"
    jmx:
      exposure:
        include: "*"
  endpoint:
    shutdown:
      access: unrestricted
    heapdump:
      access: unrestricted
```

Some endpoints also require extra beans or dependencies to exist in the
application context, for example `audit`, `httpexchanges`, `sessions`,
`prometheus`, `flyway`, `liquibase`, or `quartz`.

Also note that `info` is intentionally left available even when the rest of the
actuator command group is restricted for the current SSH user.

Available actuator commands in this starter are:

* `audit`
* `beans`
* `conditions`
* `configprops`
* `env`
* `health`
* `httpexchanges`
* `info`
* `loggers`
* `metrics`
* `mappings`
* `scheduledtasks`
* `sessions`
* `shutdown`
* `threaddump`

### Tasks

If you have `@EnableScheduling`,
these commands allow you to interact with spring boot scheduled tasks:

* `tasks-list`: Display the available scheduled tasks
* `tasks-stop`: Stop all or specified task(s)
* `tasks-restart`: Restart all or specified task(s)
* `tasks-single`: Launch one execution of all or specified task(s)

Note: refresh parameter in `tasks-list` will remove single executions.

#### Task scheduler

Based on spring
documentation `org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor.setScheduler`
the task scheduler used for scheduled tasks will be:

If not specified, it will look for unique bean of type `TaskScheduler`, or
with name
`taskScheduler`. Otherwise, a local single-threaded one will be created.

The `TasksCommand` keeps the same mechanism in order to be able to restart
stopped scheduled tasks.
It also provides a `setTaskScheduler()` in case you want to specify a custom
one.

##### Examples

| Context                                                                                                                                                     | Task scheduler used in TasksCommand                           |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| No `TaskScheduler` bean in context                                                                                                                          | Local single-threaded                                         |
| One `TaskScheduler` bean named **ts** in context                                                                                                            | **ts** bean                                                   |
| Multiple `TaskScheduler` beans named **ts1**, **ts2** in context                                                                                            | Local single-threaded (could not find name **taskScheduler**) |
| Multiple `TaskScheduler` beans named **taskScheduler**, **ts2**, **ts3** in context                                                                        | **taskScheduler** bean                                        |
| Task scheduler specified in method `SchedulingConfigurer#configureTasks`                                                                                    | Local single-threaded (not set in task)                       |
| Task scheduler specified in method `SchedulingConfigurer#configureTasks` **AND** `io.github.choseongah.ssh.shell.commands.TasksCommand.setTaskScheduler` | Scheduler manually set                                        |

### Jmx

* `jmx-info`: Displays information about jmx mbean. Use `--all-attributes-values` option to query
  attribute values.
* `jmx-invoke`: Invoke operation on object name.
* `jmx-list`: List jmx mbeans.

### System

* `system-env`: List system environment.
* `system-properties`: List system properties.
* `system-threads`: List jvm threads.

### Datasource

* `datasource-list`: List available datasources
* `datasource-properties`: Datasource properties command. Executes 'show
  variables'
* `datasource-query`: Datasource query command.
* `datasource-update`: Datasource update command.

### History

* `history`: Display or save previously run commands

### Script

* `script`: Execute commands from a script file

### Stacktrace

* `stacktrace`: Display the full stacktrace of the last error

### Postprocessors

* `postprocessors`: Display the available post processors

## Post processors

> **Note: since 1.0.6**

Post processors can be used with `|` (pipe character) followed by the name of
the post processor and the parameters.
Also, custom ones can be added.

### Provided post processors

#### Save

This specific post processor takes the key character `>`.

Example: `echo test > /path/to/file.txt`

#### Pretty

This post processor, named `pretty` takes an object and applies jackson pretty
writer.

Example: `info | pretty`

#### Json

This post processor, named `json` allows you to find a specific path within a
JSON object.

Caution: you need to have a JSON string. You can apply `pretty` post processor
before to do so.

Example: `info | pretty | json /build/version`

#### Grep

This post processor, named `grep` allows you to find specific patterns within a
string.

Examples: `info | grep boot`, `info | pretty | grep boot spring`

#### Highlight

This post processor, named `highlight` allows you to highlight specific patterns
within a
string.

Examples: `info | highlight boot`, `info | pretty | highlight boot spring`

### Custom

To register a new JSON result post processor, you need to implement
interface `PostProcessor`.

Then register it within a spring configuration.

Example:

```java
@Configuration
class PostProcessorConfiguration {
    @Bean
    public PostProcessor<String, String> quotePostProcessor() {
        return new PostProcessor<>() {

            @Override
            public String getName() {
                return "quote";
            }

            @Override
            public String getDescription() {
                return "Add quotes";
            }

            @Override
            public String process(String input, List<String> parameters) {
                return "'" + input + "'";
            }
        };
    }
}
```

## Parameter providers

### Enum

Enumeration option parameters have auto-completion by default.

### File

Thanks to [ExtendedFileCompletionProvider.java](./starter/src/main/java/io/github/choseongah/ssh/shell/completion/ExtendedFileCompletionProvider.java),
auto-completion is available
for `java.io.File` parameters and behaves correctly on Windows paths.

### Custom values

To enable auto-completion for a command, declare a `CompletionProvider` bean
and reference it from the command.

> **Note:** the completion provider has to be in the spring context.

```java
@SshShellComponent
class Commands {

    @Command(name = "echo", completionProvider = "customValuesProvider")
    public String command(@Argument(index = 0) String message) {
        return message;
    }
}

@Component("customValuesProvider")
class CustomValuesProvider implements CompletionProvider {

    private static final String[] VALUES = new String[]{
            "message1", "message2", "message3"
    };

    @Override
    public List<CompletionProposal> apply(CompletionContext completionContext) {
        return Arrays.stream(VALUES).map(CompletionProposal::new).toList();
    }
}
```

## Custom authentication

Instead of setting user and password (or using generated one), you can implement
your
own `SshShellAuthenticationProvider`.

Auto configuration will create default implementation only if there is not an
existing one in the spring context.

Example:

```java
import io.github.choseongah.ssh.shell.auth.SshShellAuthenticationProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomPasswordConfiguration {

    @Bean
    public SshShellAuthenticationProvider sshShellAuthenticationProvider() {
        return (user, pass, serverSession) -> user.equals(pass);
    }
}
```

If you prefer to rely on Spring Security, set
`ssh.shell.authentication=security`.
In that mode this starter looks for an `AuthenticationManager` bean.
If more than one is present, set `ssh.shell.auth-provider-bean-name`.

## Command helper

A `io.github.choseongah.ssh.shell.SshShellHelper` bean is provided in context to
help for additional functionalities.

You can either autowire it or inject it in a constructor:

```java
import io.github.choseongah.ssh.shell.SshShellHelper;
import io.github.choseongah.ssh.shell.commands.SshShellComponent;

@SshShellComponent
public class DemoCommand {

    @Autowired
    private SshShellHelper helper;

    // or

    public DemoCommand(SshShellHelper helper) {
        this.helper = helper;
    }
}
```

### User interaction

#### Print output

```java
@SshShellComponent
public class DemoCommand {

    @Autowired
    private SshShellHelper helper;

    @Command(name = "print", description = "Print command")
    public String print() {
        boolean success = true;
        helper.print("Some message");
        helper.print("Some black message", PromptColor.BLACK);
        helper.printSuccess("Some success message");
        return success ? helper.getSuccess("Some returned success message")
                : helper.getColored("Some returned blue message", PromptColor.BLUE);
    }
}
```

#### Read input

```java
@SshShellComponent
public class DemoCommand {

    @Autowired
    private SshShellHelper helper;

    @Command(name = "welcome", description = "Welcome command")
    public String welcome() {
        String name = helper.read("What's your name ?");
        return "Hello, '" + name + "' !";
    }
}
```

#### Confirmation

Util `confirm` method displays confirmation message and returns `true`
if response equals ignore case confirmation words.

Default confirmation words are **[`y`, `yes`]**:

You can specify if it is case-sensitive and provide your own confirmation words.

```java
@SshShellComponent
public class DemoCommand {

    @Autowired
    private SshShellHelper helper;

    @Command(name = "conf", description = "Confirmation command")
    public String conf() {
        return helper.confirm("Are you sure ?") ? "Great ! Let's do it !" : "Such a shame ...";
    }
}
```

#### Table

The `SimpleTable.builder()` API is available to
quickly set up print table.

Quick example:

```java
class Commands {
    public String table() {
        return helper.renderTable(SimpleTable.builder()
                .column("col1")
                .column("col2")
                .column("col3")
                .column("col4")
                .line(Arrays.asList("line1 col1", "line1 col2", "line1 col3", "line1 col4"))
                .line(Arrays.asList("line2 col1", "line2 col2", "line2 col3", "line2 col4"))
                .line(Arrays.asList("line3 col1", "line3 col2", "line3 col3", "line3 col4"))
                .line(Arrays.asList("line4 col1", "line4 col2", "line4 col3", "line4 col4"))
                .line(Arrays.asList("line5 col1", "line5 col2", "line5 col3", "line5 col4"))
                .line(Arrays.asList("line6 col1", "line6 col2", "line6 col3", "line6 col4"))
                .build());
    }
}
```

Result:

```text
┌──────────┬──────────┬──────────┬──────────┐
│   col1   │   col2   │   col3   │   col4   │
├──────────┼──────────┼──────────┼──────────┤
│line1 col1│line1 col2│line1 col3│line1 col4│
├──────────┼──────────┼──────────┼──────────┤
│line2 col1│line2 col2│line2 col3│line2 col4│
├──────────┼──────────┼──────────┼──────────┤
│line3 col1│line3 col2│line3 col3│line3 col4│
├──────────┼──────────┼──────────┼──────────┤
│line4 col1│line4 col2│line4 col3│line4 col4│
├──────────┼──────────┼──────────┼──────────┤
│line5 col1│line5 col2│line5 col3│line5 col4│
├──────────┼──────────┼──────────┼──────────┤
│line6 col1│line6 col2│line6 col3│line6 col4│
└──────────┴──────────┴──────────┴──────────┘
```

### Interactive

> **Note: since 1.1.3**

This method takes an interface to display lines at regular interval.

Every **refresh delay** (here 2
seconds), `io.github.choseongah.ssh.shell.interactive.InteractiveInput.getLines`
is
called.

This can be used to display progress, monitoring, etc.

The interactive
builder, [Interactive.java](./starter/src/main/java/io/github/choseongah/ssh/shell/interactive/Interactive.java)
allows you to build your interactive command.

This builder can also take key bindings to make specific actions, which can be
made by the following builder:
[KeyBinding.java](./starter/src/main/java/io/github/choseongah/ssh/shell/interactive/KeyBinding.java).

```java
@SshShellComponent
public class DemoCommand {

    @Autowired
    private SshShellHelper helper;

    @Command(name = "interactive", description = "Interactive command")
    public void interactive() {

        KeyBinding binding = KeyBinding.builder()
                .description("K binding example")
                .key("k").input(() -> LOGGER.info("In specific action triggered by key 'k' !")).build();

        Interactive interactive = Interactive.builder().input((size, currentDelay) -> {
            LOGGER.info("In interactive command for input...");
            List<AttributedString> lines = new ArrayList<>();
            AttributedStringBuilder sb = new AttributedStringBuilder(size.getColumns());

            sb.append("\nCurrent time", AttributedStyle.BOLD).append(" : ");
            sb.append(String.format("%8tT", new Date()));

            lines.add(sb.toAttributedString());

            SecureRandom sr = new SecureRandom();
            lines.add(new AttributedStringBuilder().append(helper.progress(sr.nextInt(100)),
                    AttributedStyle.DEFAULT.foreground(sr.nextInt(6) + 1)).toAttributedString());
            lines.add(AttributedString.fromAnsi(SshShellHelper.INTERACTIVE_LONG_MESSAGE + "\n"));

            return lines;
        }).binding(binding).fullScreen(true).refreshDelay(5000).build();

        helper.interactive(interactive);
    }
}
```

Note: existing key bindings are:

* `q`: to quit interactive command and go back to shell
* `+`: to increase refresh delay by 1000 milliseconds
* `-`: to decrease refresh delay by 1000 milliseconds

### Role check

If you are using Spring Security thanks to
property `ssh.shell.authentication=security`, you can check that the
connected user has the right authorities for a command.
With the current Spring Shell 4 integration, availability is provided through a
named `AvailabilityProvider` bean.

Example:

```java
@SshShellComponent
public class DemoCommand {

    @Autowired
    private SshShellHelper helper;

    @Command(name = "admin", description = "Admin command", availabilityProvider = "adminAvailabilityProvider")
    public String admin() {
        return "Finally an administrator !!";
    }

    public Availability adminAvailability() {
        if (!helper.checkAuthorities(Collections.singletonList("ADMIN"))) {
            return Availability.unavailable("admin command is only for an admin users !");
        }
        return Availability.available();
    }
}

@Configuration
class DemoConfiguration {

    @Bean("adminAvailabilityProvider")
    public AvailabilityProvider adminAvailabilityProvider(DemoCommand demoCommand) {
        return demoCommand::adminAvailability;
    }
}
```

### Retrieve spring security authentication

```java
@SshShellComponent
public class DemoCommand {

    @Autowired
    private SshShellHelper helper;

    @Command(name = "authentication", description = "Authentication command")
    public SshAuthentication authentication() {
        return helper.getAuthentication();
    }
}
```

## Banner

If a banner is found in spring context and `display-banner` is set to true,
it will be used as welcome prompt message.

## Listeners

An interface is provided in order to receive events on ssh
sessions: `io.github.choseongah.ssh.shell.listeners.SshShellListener`.

Implement it and define a spring bean in order to receive events.

_Example_

```java
@Configuration
class ShellListenerConfiguration {
    @Bean
    public SshShellListener sshShellListener() {
        return event -> LOGGER.info("[listener] event '{}' [id={}, ip={}]",
                event.getType(),
                event.getSession().getServerSession().getIoSession().getId(),
                event.getSession().getServerSession().getIoSession().getRemoteAddress());
    }
}
```

## Session Manager

> **Note: since 1.3.0**

A session manager bean is available and allows you to:

* list active sessions
* get information about one session
* stop a session

**Note: you need to use @Lazy injection if you are using it in a command**

_Example_

```java
class Commands {
    public MyCommand(@Lazy SshShellSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Command(name = "my-command", description = "My command")
    public String myCommand() {
        sessionManager.listSessions();
        //...
    }
}
```

### Manage sessions commands

If activated `ssh.shell.commands.manage-sessions.enabled=true`, the following
commands are available:

* `manage-sessions-info`: Displays information about single session
* `manage-sessions-list`: Displays active sessions
* `manage-sessions-stop`: Stop single specific session

## Tests

It can be annoying to load ssh server during spring boot tests.
`SshShellProperties` class provides constants to easily deactivate
this starter and the main Spring Shell auto configuration:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {"ssh.shell.port=2346",
        SshShellProperties.DISABLE_SSH_SHELL,
        SshShellProperties.DISABLE_SPRING_SHELL_AUTO_CONFIG
})
@ExtendWith(SpringExtension.class)
public class ApplicationTest {
}
```

## Samples

* [Basic sample](./samples/basic), no actuator, no security, no sessions
* [Complete sample](./samples/complete), with actuator, security dependencies and configurations

## Release notes

Please check [GitHub releases page](https://github.com/choseongah/ssh-shell-spring-boot/releases).

---

# For Next Maintainer

See [docs/for-next-maintainer.md](./docs/for-next-maintainer.md).
