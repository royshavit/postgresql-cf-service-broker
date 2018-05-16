/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cloudfoundry.community.servicebroker.database.repository.postgres;

import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.cloudfoundry.community.servicebroker.database.jdbc.QueryExecutor;
import org.cloudfoundry.community.servicebroker.database.repository.Consts;
import org.cloudfoundry.community.servicebroker.database.repository.DatabaseRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@Component
@Profile(Consts.POSTGRES)
@Slf4j
public class PostgresDatabaseRepository implements DatabaseRepository {

    private static final String POSTGRES_URL = "postgresql://%s:%s@%s:%d/%s";
    private static final String JDBC_URL = "jdbc:postgresql://%s:%d/%s?%s&%s";

    private final QueryExecutor queryExecutor;
    private final String masterDbHost;
    private final int masterDbPort;
    private final String masterUsername;

    @SneakyThrows
    public PostgresDatabaseRepository(QueryExecutor queryExecutor, DataSource masterDataSource) {
        this.queryExecutor = queryExecutor;
        URI uri = new URI(new URI(masterDataSource.getUrl()).getSchemeSpecificPart());
        masterDbPort = uri.getPort();
        masterDbHost = uri.getHost();
        try (Connection connection = masterDataSource.getConnection()) {
            masterUsername = connection.getMetaData().getUserName();
        }
    }

    @Override
    public void createDatabase(String databaseName) {
        queryExecutor.executeUpdate(createRole(databaseName));
        queryExecutor.executeUpdate("CREATE DATABASE \"" + databaseName + "\" ENCODING 'UTF8'");
        queryExecutor.executeUpdate("REVOKE all on database \"" + databaseName + "\" from public");
        queryExecutor.executeUpdate("ALTER DATABASE \"" + databaseName + "\" OWNER TO \"" + databaseName + "\"");
    }

    @Override
    public void deleteDatabase(String databaseName) {
        queryExecutor.executePreparedSelect(
                "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = ? AND pid <> pg_backend_pid()",
                ImmutableMap.of(1, databaseName));
        queryExecutor.executeUpdate("DROP DATABASE \"" + databaseName + "\"");
        queryExecutor.executeUpdate(deleteRole(databaseName));
    }

    @Override
    public Map<String, Object> createUser(String databaseName, String username, String password, boolean elevatedPrivileges) {
        queryExecutor.executeUpdate(createRole(username));
        queryExecutor.executeUpdate(grantRole(databaseName, username));
        if (elevatedPrivileges) {
            queryExecutor.executeUpdate(grantRole(masterUsername, username)); //todo: WITH ADMIN OPTION
        }
        queryExecutor.executeUpdate("ALTER ROLE \"" + username + "\" LOGIN password '" + password + "'");
        return buildCredentials(databaseName, username, password);
    }

    private String createRole(String role) {
        return "CREATE ROLE \"" + role + "\"";
    }

    private String deleteRole(String role) {
        return "DROP ROLE \"" + role + "\"";
    }

    private String grantRole(String roleToGrant, String roleToBenefit) {
        return "GRANT \"" + roleToGrant + "\" TO \"" + roleToBenefit + "\"";
    }

    @Override
    public void deleteUser(String databaseName, String username) {
        queryExecutor.executeUpdate(deleteRole(username));
    }

    private Map<String, Object> buildCredentials(String databaseName, String userName, String password) {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("uri", String.format(POSTGRES_URL, userName, password, masterDbHost, masterDbPort, databaseName));
        credentials.put("jdbcurl", String.format(JDBC_URL, masterDbHost, masterDbPort, databaseName, userName, password));
        credentials.put("username", userName);
        credentials.put("password", password);
        credentials.put("hostname", masterDbHost);
        credentials.put("port", masterDbPort);
        credentials.put("database", databaseName);
        return credentials;
    }

}
