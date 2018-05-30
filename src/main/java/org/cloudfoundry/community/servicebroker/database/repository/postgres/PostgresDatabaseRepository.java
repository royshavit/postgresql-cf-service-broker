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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.cloudfoundry.community.servicebroker.database.jdbc.QueryExecutor;
import org.cloudfoundry.community.servicebroker.database.repository.Consts;
import org.cloudfoundry.community.servicebroker.database.repository.DatabaseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile(Consts.POSTGRES)
@Slf4j
public class PostgresDatabaseRepository implements DatabaseRepository {

    private static final String POSTGRES_URL = "postgresql://%s:%s@%s:%d/%s";
    private static final String JDBC_URL = "jdbc:postgresql://%s:%d/%s?user=%s&password=%s";

    private final QueryExecutor queryExecutor;
    private final boolean elevatedPrivileges;
    private final String masterDbHost;
    private final int masterDbPort;
    private final String masterUsername;

    @SneakyThrows
    public PostgresDatabaseRepository(QueryExecutor queryExecutor,
                                      DataSource masterDataSource,
                                      @Value("${database.privileges.elevated}") boolean grantUsersElevatedPrivileges) {
        this.queryExecutor = queryExecutor;
        elevatedPrivileges = grantUsersElevatedPrivileges;
        URI uri = new URI(new URI(masterDataSource.getUrl()).getSchemeSpecificPart());
        masterDbPort = uri.getPort();
        masterDbHost = uri.getHost();
        try (Connection connection = masterDataSource.getConnection()) {
            masterUsername = connection.getMetaData().getUserName();
        }
    }

    @Override
    public void createDatabase(String databaseName, int databaseConnectionsMax) {
        log.info("creating database {} with {} max connections", databaseName, databaseConnectionsMax);
        queryExecutor.update(createRole(databaseName));
        if (elevatedPrivileges) {
            queryExecutor.update(grantRole(masterUsername, databaseName));
        }
        queryExecutor.update("CREATE DATABASE \"" + databaseName + "\" ENCODING 'UTF8'");
        queryExecutor.update("ALTER DATABASE \"" + databaseName + "\" CONNECTION LIMIT " + databaseConnectionsMax);
        queryExecutor.update("REVOKE all on database \"" + databaseName + "\" from public");
        queryExecutor.update(setOwner(databaseName, databaseName));
        log.info("created database {} with {} max connections", databaseName, databaseConnectionsMax);
    }

    @Override
    public void deleteDatabase(String databaseName) {
        log.info("deleting database {}", databaseName);
        List<Map<String, String>> terminatedConnections = queryExecutor.select(terminateConnections(databaseName));
        log.warn("terminated {} connections to {}", terminatedConnections.size(), databaseName);
        queryExecutor.update(setOwner(databaseName, masterUsername));
        queryExecutor.update("DROP DATABASE \"" + databaseName + "\"");
        queryExecutor.update(deleteRole(databaseName));
        log.info("deleted database {}", databaseName);
    }

    private String terminateConnections(String databaseName) {
        return "SELECT pg_terminate_backend(pid) FROM pg_stat_activity" +
                " WHERE datname = '" + databaseName + "' AND pid <> pg_backend_pid()";
    }

    @Override
    public Map<String, Object> createUser(String databaseName, String username, String password) {
        log.info("creating user {} for database {} with{} elevated privileges", username, databaseName, elevatedPrivileges ? "" : "out");
        queryExecutor.update(createRole(username));
        queryExecutor.update(grantRole(databaseName, username));
        queryExecutor.update("ALTER ROLE \"" + username + "\" LOGIN password '" + password + "'");
        Map<String, Object> credentials = buildCredentials(databaseName, username, password);
        QueryExecutor newUserQueryExecutor = new QueryExecutor((String) credentials.get("jdbcurl"));
        newUserQueryExecutor.update("ALTER ROLE \"" + username + "\" SET role \"" + databaseName + "\""); //If user owns database objects, user cannot be deleted. Logging in as parent role allows database objects created by this user to be owned by parent role.
        log.info("created user {} for database {} with{} elevated privileges", username, databaseName, elevatedPrivileges ? "" : "out");
        return credentials;
    }

    private String setOwner(String databaseName, String owner) {
        return "ALTER DATABASE \"" + databaseName + "\" OWNER TO \"" + owner + "\"";
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
        log.info("deleting user {} of database {}", username, databaseName);
        List<Map<String, String>> terminatedConnections = queryExecutor.select(
                terminateConnections(databaseName) + " AND usename = '" + username + "'");
        log.warn("terminated {} connections of user {} to database {}", terminatedConnections.size(), username, databaseName);
        queryExecutor.update(deleteRole(username));
        log.info("deleted user {} of database {}", username, databaseName);
    }

    @Override
    public boolean userExists(String databaseName, String username) {
        List<Map<String, String>> result = queryExecutor.select("SELECT 1 FROM pg_roles WHERE rolname='" + username + "'");
        return result.size() >= 1;
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
