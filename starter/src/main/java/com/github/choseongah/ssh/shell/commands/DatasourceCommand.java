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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

import javax.sql.DataSource;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Datasource command
 */
@Slf4j
@SshShellComponent("sshDatasourceCommand")
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(
        name = SshShellProperties.SSH_SHELL_PREFIX + ".commands." + DatasourceCommand.GROUP + ".create",
        havingValue = "true", matchIfMissing = true
)
public class DatasourceCommand extends AbstractCommand {

    public static final String GROUP = "datasource";
    public static final String COMMAND_DATA_SOURCE_LIST = GROUP + "-list";
    public static final String COMMAND_DATA_SOURCE_PROPERTIES = GROUP + "-properties";
    public static final String COMMAND_DATA_SOURCE_QUERY = GROUP + "-query";
    public static final String COMMAND_DATA_SOURCE_UPDATE = GROUP + "-update";
    public static final String LIST_AVAILABILITY_PROVIDER = "datasourceListAvailabilityProvider";
    public static final String PROPERTIES_AVAILABILITY_PROVIDER = "datasourcePropertiesAvailabilityProvider";
    public static final String QUERY_AVAILABILITY_PROVIDER = "datasourceQueryAvailabilityProvider";
    public static final String UPDATE_AVAILABILITY_PROVIDER = "datasourceUpdateAvailabilityProvider";
    public static final String INDEX_COMPLETION_PROVIDER = "datasourceIndexCompletionProvider";

    private final Map<Integer, DataSource> dataSourceByIndex = new HashMap<>();

    public DatasourceCommand(SshShellHelper helper, SshShellProperties properties,
                             @Autowired(required = false) List<DataSource> dataSourceList) {
        super(helper, properties, properties.getCommands().getDatasource());
        if (dataSourceList != null) {
            this.dataSourceByIndex.putAll(IntStream.range(0, dataSourceList.size()).boxed()
                    .collect(Collectors.toMap(Function.identity(), dataSourceList::get)));
        }
    }

    /**
     * List datasources found in context
     *
     * @return datasource list
     */
    @Command(name = COMMAND_DATA_SOURCE_LIST, group = "Datasource Commands",
            description = "List available datasources", availabilityProvider = LIST_AVAILABILITY_PROVIDER)
    public String datasourceList() {
        if (dataSourceByIndex.isEmpty()) {
            helper.printWarning("No datasource found in context.");
            return null;
        }

        SimpleTable.SimpleTableBuilder builder = SimpleTable.builder()
                .column("Id").column("Name").column("Url").column("Username").column("Product").column("Error");
        for (Map.Entry<Integer, DataSource> entry : dataSourceByIndex.entrySet()) {
            try (Connection connection = entry.getValue().getConnection()) {
                DatabaseMetaData databaseMetaData = connection.getMetaData();
                builder.line(Arrays.asList(
                        entry.getKey(), entry.getValue().toString(),
                        databaseMetaData.getURL(),
                        databaseMetaData.getUserName(),
                        databaseMetaData.getDatabaseProductName() + " " + databaseMetaData.getDatabaseProductVersion(),
                        "-")
                );
            } catch (SQLException e) {
                LOGGER.warn("Unable to get datasource information for [{}] : {}-{}", entry.getValue().toString(),
                        e.getErrorCode(), e.getMessage());
                String url = find(entry.getValue(), "jdbcUrl", "url");
                String userName = find(entry.getValue(), "username", "user");
                builder.line(Arrays.asList(entry.getKey(), entry.getValue().toString(), url, userName, "-",
                        "Unable to get datasource information for [" + url + "] : " + e.getErrorCode() + "-"
                                + e.getMessage())
                );
            }
        }
        return helper.renderTable(builder.build());
    }

    private String find(Object object, String... fieldNames) {
        if (fieldNames != null) {
            for (String fieldName : fieldNames) {
                try {
                    PropertyDescriptor pd = new PropertyDescriptor(fieldName, object.getClass());
                    Object result = pd.getReadMethod().invoke(object);
                    if (result != null) {
                        return result.toString();
                    }
                } catch (IllegalAccessException | IntrospectionException | InvocationTargetException e) {
                    LOGGER.debug("Unable to access field [{}] on class [{}]", fieldName, object.getClass(), e);
                }
            }
        }
        return "-";
    }

    /**
     * Executes <b>show variables</b> on given datasource
     *
     * @param id     datasource identifier
     * @param filter filter properties according to pattern
     * @return server sql properties
     */
    @Command(name = COMMAND_DATA_SOURCE_PROPERTIES, group = "Datasource Commands",
            description = "Datasource properties command. Executes 'show variables'",
            availabilityProvider = PROPERTIES_AVAILABILITY_PROVIDER, completionProvider = INDEX_COMPLETION_PROVIDER)
    public String datasourceProperties(
            @Option(longName = "id", description = "Datasource identifier", required = true) int id,
            @Option(longName = "filter", description = "Add like %<filter>% to sql query", defaultValue = "")
            String filter) {
        String query = "show variables";
        if (filter != null && !filter.isBlank()) {
            query += " LIKE '%" + filter + "%'";
        }
        return datasourceQuery(id, query);
    }

    /**
     * Executes <b>query</b> on given datasource
     *
     * @param id    datasource identifier
     * @param query sql query
     * @return query result in table
     */
    @Command(name = COMMAND_DATA_SOURCE_QUERY, group = "Datasource Commands",
            description = "Datasource query command.", availabilityProvider = QUERY_AVAILABILITY_PROVIDER,
            completionProvider = INDEX_COMPLETION_PROVIDER)
    public String datasourceQuery(
            @Option(longName = "id", description = "Datasource identifier", required = true) int id,
            @Option(longName = "query", description = "SQL query to execute", required = true) String query
    ) {
        StringBuilder sb = new StringBuilder();
        DataSource ds = getOrDie(id);
        try (Connection connection = ds.getConnection()) {
            sb.append("Query [").append(query).append("] for datasource : ").append(ds).append(" (")
                    .append(connection.getMetaData().getURL()).append(")\n");
            try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(query)) {
                SimpleTable.SimpleTableBuilder builder = SimpleTable.builder();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    builder.column(rs.getMetaData().getColumnName(i));
                }
                while (rs.next()) {
                    List<Object> list = new ArrayList<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        list.add(rs.getString(i));
                    }
                    builder.line(list);
                }
                sb.append(helper.renderTable(builder.build()));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to execute SQL query : " + e.getMessage(), e);
        }
        return sb.toString();
    }

    /**
     * Executes <b>update</b> on given datasource
     *
     * @param id     datasource identifier
     * @param update sql update
     */
    @Command(name = COMMAND_DATA_SOURCE_UPDATE, group = "Datasource Commands",
            description = "Datasource update command.", availabilityProvider = UPDATE_AVAILABILITY_PROVIDER,
            completionProvider = INDEX_COMPLETION_PROVIDER)
    public void datasourceUpdate(
            @Option(longName = "id", description = "Datasource identifier", required = true) int id,
            @Option(longName = "update", description = "SQL update to execute", required = true) String update
    ) {
        DataSource ds = getOrDie(id);
        try (Connection connection = ds.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                int result = statement.executeUpdate(update);
                helper.printSuccess("Query [" + update + "] for datasource : [" + ds +
                        " (" + connection.getMetaData().getURL() + ")] updated " + result + " row(s)");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to execute SQL update : " + e.getMessage(), e);
        }
    }

    private DataSource getOrDie(int index) {
        DataSource ds = dataSourceByIndex.get(index);
        if (ds == null) {
            throw new IllegalArgumentException("Cannot find datasource with identifier [" + index + "]");
        }
        return ds;
    }

    public List<String> getDatasourceIndexes() {
        return new TreeSet<>(dataSourceByIndex.keySet()).stream().map(String::valueOf).toList();
    }

    public Availability datasourceListAvailability() {
        return availability(GROUP, COMMAND_DATA_SOURCE_LIST);
    }

    public Availability datasourcePropertiesAvailability() {
        return availability(GROUP, COMMAND_DATA_SOURCE_PROPERTIES);
    }

    public Availability datasourceQueryAvailability() {
        return availability(GROUP, COMMAND_DATA_SOURCE_QUERY);
    }

    public Availability datasourceUpdateAvailability() {
        return availability(GROUP, COMMAND_DATA_SOURCE_UPDATE);
    }
}
