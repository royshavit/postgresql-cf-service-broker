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
        queryExecutor.executeUpdate("CREATE ROLE \"" + databaseName + "\"");
        queryExecutor.executeUpdate("CREATE DATABASE \"" + databaseName + "\" ENCODING 'UTF8'");
        queryExecutor.executeUpdate("REVOKE all on database \"" + databaseName + "\" from public");
        queryExecutor.executeUpdate("ALTER DATABASE \"" + databaseName + "\" OWNER TO \"" + databaseName + "\"");
    }

    @Override
    public void deleteDatabase(String databaseName) {
        queryExecutor.executePreparedSelect(
                "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = ? AND pid <> pg_backend_pid()",
                ImmutableMap.of(1, databaseName));
        queryExecutor.executeUpdate("DROP DATABASE IF EXISTS \"" + databaseName + "\"");
        queryExecutor.executeUpdate("DROP ROLE IF EXISTS \"" + databaseName + "\""); //todo: share common sql
    }

    @Override
    public Map<String, Object> createUser(String databaseName, String username, String password, boolean elevatedPrivileges) {
        queryExecutor.executeUpdate("CREATE ROLE \"" + username + "\"");
        queryExecutor.executeUpdate("GRANT \"" + databaseName + "\" TO \"" + username + "\"");
        if (elevatedPrivileges) {
            queryExecutor.executeUpdate("GRANT \"" + masterUsername + "\" TO \"" + username + "\"");
        }
        queryExecutor.executeUpdate("ALTER ROLE \"" + username + "\" LOGIN password '" + password + "'");
        return buildCredentials(databaseName, username, password);
    }

    @Override
    public void deleteUser(String databaseName, String username) {
        queryExecutor.executeUpdate("DROP ROLE IF EXISTS \"" + username + "\""); //todo if exists?
    }

    private String toUrl(String host, int port, String databaseName, String user, String password) { //todo: necessary?
        return String.format("postgresql://%s:%s@%s:%d/%s", user, password, host, port, databaseName);
    }

    private String toJdbcUrl(String host, int port, String databaseName, String user, String password) {
        return String.format("jdbc:postgresql://%s:%d/%s?%s&%s", host, port, databaseName, user, password);
    }

    private Map<String, Object> buildCredentials(String databaseName, String userName, String password) {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("uri", toUrl(masterDbHost, masterDbPort, databaseName, userName, password));
        credentials.put("jdbcurl", toJdbcUrl(masterDbHost, masterDbPort, databaseName, userName, password));
        credentials.put("username", userName);
        credentials.put("password", password);
        credentials.put("hostname", masterDbHost);
        credentials.put("port", masterDbPort);
        credentials.put("database", databaseName);
        return credentials;
    }

}
