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
import com.github.choseongah.ssh.shell.manage.SshShellSessionManager;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.session.ServerSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

import java.util.Arrays;
import java.util.Map;

import static com.github.choseongah.ssh.shell.manage.SshShellSessionManager.sessionUserName;

/**
 * Command to manage ssh sessions, not available by default
 */
@SshShellComponent("sshManageSessionsCommand")
@ConditionalOnProperty(
        name = SshShellProperties.SSH_SHELL_PREFIX + ".commands." + ManageSessionsCommand.GROUP + ".create",
        havingValue = "true", matchIfMissing = true
)
public class ManageSessionsCommand extends AbstractCommand {

    public static final String GROUP = "manage-sessions";
    private static final String COMMAND_MANAGE_SESSIONS_LIST = GROUP + "-list";
    private static final String COMMAND_MANAGE_SESSIONS_INFO = GROUP + "-info";
    private static final String COMMAND_MANAGE_SESSIONS_STOP = GROUP + "-stop";
    public static final String LIST_AVAILABILITY_PROVIDER = "manageSessionsListAvailabilityProvider";
    public static final String INFO_AVAILABILITY_PROVIDER = "manageSessionsInfoAvailabilityProvider";
    public static final String STOP_AVAILABILITY_PROVIDER = "manageSessionsStopAvailabilityProvider";
    public static final String INFO_COMPLETION_PROVIDER = "manageSessionsInfoCompletionProvider";
    public static final String STOP_COMPLETION_PROVIDER = "manageSessionsStopCompletionProvider";

    private final SshShellSessionManager sessionManager;

    public ManageSessionsCommand(SshShellHelper helper, SshShellProperties properties,
                                 @Lazy SshShellSessionManager sessionManager) {
        super(helper, properties, properties.getCommands().getManageSessions());
        this.sessionManager = sessionManager;
    }

    @Command(name = COMMAND_MANAGE_SESSIONS_LIST, group = "Manage Sessions Commands",
            description = "Displays active sessions", availabilityProvider = LIST_AVAILABILITY_PROVIDER)
    public String manageSessionsList() {
        Map<Long, ChannelSession> sessions = sessionManager.listSessions();

        SimpleTable.SimpleTableBuilder builder = SimpleTable.builder()
                .column("Session Id").column("Local address").column("Remote address").column("Authenticated User");

        for (ChannelSession value : sessions.values()) {
            builder.line(Arrays.asList(
                    value.getServerSession().getIoSession().getId(),
                    value.getServerSession().getIoSession().getLocalAddress(),
                    value.getServerSession().getIoSession().getRemoteAddress(),
                    sessionUserName(value)
            ));
        }
        return helper.renderTable(builder.build());
    }

    @Command(name = COMMAND_MANAGE_SESSIONS_INFO, group = "Manage Sessions Commands",
            description = "Displays session", availabilityProvider = INFO_AVAILABILITY_PROVIDER,
            completionProvider = INFO_COMPLETION_PROVIDER)
    public String manageSessionsInfo(
            @Option(longName = "session-id", description = "Session identifier", required = true) long sessionId) {
        ChannelSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            return helper.getError("Session [" + sessionId + "] not found");
        }
        return helper.getSuccess(sessionTable(session.getServerSession()));
    }

    @Command(name = COMMAND_MANAGE_SESSIONS_STOP, group = "Manage Sessions Commands",
            description = "Stop session", availabilityProvider = STOP_AVAILABILITY_PROVIDER,
            completionProvider = STOP_COMPLETION_PROVIDER)
    public String manageSessionsStop(
            @Option(longName = "session-id", description = "Session identifier", required = true) long sessionId) {
        return sessionManager.stopSession(sessionId)
                ? helper.getSuccess("Session [" + sessionId + "] stopped")
                : helper.getWarning("Unable to stop session [" + sessionId + "], maybe it does not exist");
    }

    private String sessionTable(ServerSession session) {
        return helper.renderTable(SimpleTable.builder()
                .column("Property").column("Value")
                .line(Arrays.asList("Session id", session.getIoSession().getId()))
                .line(Arrays.asList("Local address", session.getIoSession().getLocalAddress()))
                .line(Arrays.asList("Remote address", session.getIoSession().getRemoteAddress()))
                .line(Arrays.asList("Server version", session.getServerVersion()))
                .line(Arrays.asList("Client version", session.getClientVersion()))
                .build());
    }

    public Availability manageSessionsListAvailability() {
        return availability(GROUP, COMMAND_MANAGE_SESSIONS_LIST);
    }

    public Availability manageSessionsInfoAvailability() {
        return availability(GROUP, COMMAND_MANAGE_SESSIONS_INFO);
    }

    public Availability manageSessionsStopAvailability() {
        return availability(GROUP, COMMAND_MANAGE_SESSIONS_STOP);
    }
}
