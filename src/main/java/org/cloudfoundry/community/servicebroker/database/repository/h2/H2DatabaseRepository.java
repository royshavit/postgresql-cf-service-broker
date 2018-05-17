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

    @Value("${spring.datasource.password}")
    private String masterPassword;


    private QueryExecutor queryExecutor(String url) {
        return new QueryExecutor(url);
    }

    @SneakyThrows
    @Override
    public void createDatabase(String databaseName) {
        String url = String.format(CREATE_DATABASE_URL, databaseName, databaseName, masterPassword);
        List<Map<String, String>> rows = queryExecutor(url).select("select 1");
        String errorMessage = "unable to connect to new database " + databaseName;
        Assert.state(rows.size() == 1, errorMessage);
        String result = rows.iterator().next().values().iterator().next();
        Assert.state(result.equals("1"), errorMessage);
    }

    @SneakyThrows
    @Override
    public void deleteDatabase(String databaseName) {
        String url = String.format(JDBC_URL, databaseName, databaseName, masterPassword);
        queryExecutor(url).update("SHUTDOWN");
    }

    @SneakyThrows
    @Override
    public Map<String, Object> createUser(String databaseName, String username, String password, boolean elevatedPrivileges) {
        String url = String.format(JDBC_URL, databaseName, databaseName, masterPassword);
        //todo: test
        queryExecutor(url).update(String.format(elevatedPrivileges ? CREATE_ADMIN_USER : CREATE_USER, username, password));
        return buildCredentials(databaseName, username, password);
    }

    @SneakyThrows
    @Override
    public void deleteUser(String databaseName, String username) {
        String url = String.format(JDBC_URL, databaseName, databaseName, masterPassword);
        queryExecutor(url).update("DROP USER \"" + username + "\"");
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
