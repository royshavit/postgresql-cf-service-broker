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
import org.cloudfoundry.community.servicebroker.database.jdbc.QueryExecutor;
import org.cloudfoundry.community.servicebroker.database.repository.Consts;
import org.cloudfoundry.community.servicebroker.database.repository.DatabaseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile(Consts.H2)
@Slf4j
public class H2DatabaseRepository implements DatabaseRepository {

    private static final String JDBC_URL = "jdbc:h2:mem:%s;USER=%s;PASSWORD=%s;IFEXISTS=TRUE";
    private static final String CREATE_DATABASE_URL = "jdbc:h2:mem:%s;USER=%s;PASSWORD=%s;IFEXISTS=FALSE;DB_CLOSE_DELAY=-1";
    private static final String CREATE_USER = "CREATE USER \"%s\" PASSWORD '%s'";
    private static final String CREATE_ADMIN_USER = CREATE_USER + " ADMIN";

    private final String masterPassword;


    public H2DatabaseRepository(@Value("${spring.datasource.password}") String masterPassword) {
        this.masterPassword = masterPassword;
    }


    @SneakyThrows
    @Override
    public void createDatabase(String databaseName) {
        log.info("creating database {}", databaseName);
        String url = String.format(CREATE_DATABASE_URL, databaseName, databaseName, masterPassword);
        List<Map<String, String>> rows = new QueryExecutor(url).select("select 1");
        validateConnection(databaseName, rows);
        log.info("created database {}", databaseName);
    }

    private void validateConnection(String databaseName, List<Map<String, String>> rows) {
        String errorMessage = "unable to connect to new database " + databaseName;
        Assert.state(rows.size() == 1, errorMessage);
        String result = rows.iterator().next().values().iterator().next();
        Assert.state(result.equals("1"), errorMessage);
    }

    private QueryExecutor queryExecutor(String databaseName) {
        String url = String.format(JDBC_URL, databaseName, databaseName, masterPassword);
        return new QueryExecutor(url);
    }

    @SneakyThrows
    @Override
    public void deleteDatabase(String databaseName) {
        log.info("deleting database {}", databaseName);
        queryExecutor(databaseName).update("SHUTDOWN");
        log.info("deleted database {}", databaseName);
    }

    @SneakyThrows
    @Override
    public Map<String, Object> createUser(String databaseName, String username, String password, boolean elevatedPrivileges) {
        log.info("creating user {} for database {} with{} elevated privileges", username, databaseName, elevatedPrivileges ? "" : "out");
        queryExecutor(databaseName).update(String.format(elevatedPrivileges ? CREATE_ADMIN_USER : CREATE_USER, username, password));
        Map<String, Object> credentials = buildCredentials(databaseName, username, password);
        log.info("created user {} for database {} with{} elevated privileges", username, databaseName, elevatedPrivileges ? "" : "out");
        return credentials;
    }

    @SneakyThrows
    @Override
    public void deleteUser(String databaseName, String username) {
        log.info("deleting user {} of database {}", username, databaseName);
        queryExecutor(databaseName).update("DROP USER \"" + username + "\"");
        log.info("deleted user {} of database {}", username, databaseName);
    }

    @Override
    public boolean userExists(String databaseName, String username) {
        List<Map<String, String>> result
                = queryExecutor(databaseName).select("select 1 from information_schema.users where name = '" + username + "'");
        return result.size() >= 1;
    }

    private Map<String, Object> buildCredentials(String databaseName, String userName, String password) {
        Map<String, Object> credentials = new HashMap<>();
        String jdbcUrl = String.format(JDBC_URL, databaseName, userName, password);
        credentials.put("uri", jdbcUrl);
        credentials.put("jdbcurl", jdbcUrl);
        credentials.put("username", userName);
        credentials.put("password", password);
        credentials.put("database", databaseName);
        return credentials;
    }

}
