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
package org.cloudfoundry.community.servicebroker.database.repository.h2;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.database.repository.Consts;
import org.cloudfoundry.community.servicebroker.database.repository.DatabaseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Profile(Consts.H2)
@Slf4j
public class H2DatabaseRepository implements DatabaseRepository {

    private static final String CONNECTION_URL_FORMAT = "h2:mem:%s;USER=%s;PASSWORD=%s;IFEXISTS=TRUE";
    private static final String JDBC_URL_FORMAT = "jdbc:h2:mem:%s;USER=%s;PASSWORD=%s;IFEXISTS=TRUE";
    private static final String CREATE_CONNECTION_URL_FORMAT = "jdbc:h2:mem:%s;USER=%s;PASSWORD=%s;IFEXISTS=FALSE;DB_CLOSE_DELAY=-1"; //todo: duplicate urls
    private static final String CREATE_USER = "CREATE USER \"%s\" PASSWORD '%s'";
    private static final String CREATE_ADMIN_USER = CREATE_USER + " ADMIN";

    @Value("${spring.datasource.password}")
    private String masterPassword;

    @SneakyThrows
    @Override
    public void createDatabase(String databaseName) {  //todo: not using owner
        String url = String.format(CREATE_CONNECTION_URL_FORMAT, databaseName, databaseName, masterPassword);
        try (Connection connection = DriverManager.getConnection(url)) {
            try (Statement statement = connection.createStatement()) {
                ResultSet result = statement.executeQuery("select 1");
                Map<String, String> resultMap = getResultMapFromResultSet(result);
                System.out.println(resultMap);
            }
        }
    }

    private void testWrongPassword(String databaseName, String owner) throws SQLException { //todo: move to test
        String url;
        url = String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;USER=%s;PASSWORD=%s", databaseName, owner, UUID.randomUUID());
        try (Connection connection = DriverManager.getConnection(url)) {
            try (Statement statement = connection.createStatement()) {
                ResultSet result = statement.executeQuery("select 1");
                Map<String, String> resultMap = getResultMapFromResultSet(result);
                System.out.println(resultMap);
            }
        }
    }

    private static Map<String, String> getResultMapFromResultSet(ResultSet result) throws SQLException {
        ResultSetMetaData resultMetaData = result.getMetaData();
        int columns = resultMetaData.getColumnCount();
        Map<String, String> resultMap = new HashMap<>(columns);
        if (result.next()) {
            for (int i = 1; i <= columns; i++) {
                resultMap.put(resultMetaData.getColumnName(i), result.getString(i));
            }
        }
        return resultMap;
    }

    @SneakyThrows
    @Override
    public void deleteDatabase(String databaseName) {
        String url = String.format(JDBC_URL_FORMAT, databaseName, databaseName, masterPassword);
        try (Connection connection = DriverManager.getConnection(url)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }

    @SneakyThrows
    @Override
    public Map<String, Object> createUser(String databaseName, String username, String password, boolean elevatedPrivileges) {
        String url = String.format(JDBC_URL_FORMAT, databaseName, databaseName, masterPassword);
        try (Connection connection = DriverManager.getConnection(url)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(String.format(elevatedPrivileges ? CREATE_ADMIN_USER : CREATE_USER, username, password)); //todo: test
            }
        }
        return buildCredentials(databaseName, username, password);
    }

    @SneakyThrows
    @Override
    public void deleteUser(String databaseName, String username) { //todo: use queryupdater
        String url = String.format(JDBC_URL_FORMAT, databaseName, databaseName, masterPassword);
        try (Connection connection = DriverManager.getConnection(url)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP USER \"" + username + "\"");
            }
        }
    }

    private Map<String, Object> buildCredentials(String databaseName, String userName, String password) {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("uri", String.format(CONNECTION_URL_FORMAT, databaseName, userName, password));
        credentials.put("username", userName);
        credentials.put("password", password);
        credentials.put("database", databaseName);
        return credentials;
    }

}
