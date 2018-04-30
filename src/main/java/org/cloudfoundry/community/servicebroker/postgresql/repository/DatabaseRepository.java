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
package org.cloudfoundry.community.servicebroker.postgresql.repository;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.postgresql.model.Database;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@AllArgsConstructor
public class DatabaseRepository {
    
    private final PostgreSQLDatabase postgreSQLDatabase;
    private final Database masterDatabase;
    
    public void create(String databaseName, String owner) throws SQLException {
        postgreSQLDatabase.executeUpdate("CREATE DATABASE \"" + databaseName + "\" ENCODING 'UTF8'");
        postgreSQLDatabase.executeUpdate("REVOKE all on database \"" + databaseName + "\" from public");
        postgreSQLDatabase.executeUpdate("ALTER DATABASE \"" + databaseName + "\" OWNER TO \"" + owner + "\"");
    }

    public void delete(String databaseName) throws SQLException {
        Map<Integer, String> parameterMap = new HashMap<>();
        parameterMap.put(1, databaseName);

        Map<String, String> result = postgreSQLDatabase.executeSelect("SELECT current_user");
        String currentUser = null;

        if(result != null) {
            currentUser = result.get("current_user");
        }

        if(currentUser == null) {
            log.error("Current user for instance '" + databaseName + "' could not be found");
        }

        postgreSQLDatabase.executePreparedSelect("SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = ? AND pid <> pg_backend_pid()", parameterMap);
        postgreSQLDatabase.executeUpdate("ALTER DATABASE \"" + databaseName + "\" OWNER TO \"" + currentUser + "\"");
        postgreSQLDatabase.executeUpdate("DROP DATABASE IF EXISTS \"" + databaseName + "\"");
    }

    @SneakyThrows
    public Optional<Database> findOne(String databaseName) {
        Map<Integer, String> parameterMap = new HashMap<>();
        parameterMap.put(1, databaseName);
        Map<String, String> result = postgreSQLDatabase.executePreparedSelect(
                "SELECT d.datname, pg_catalog.pg_get_userbyid(d.datdba) AS owner FROM pg_catalog.pg_database d WHERE d.datname = ?;",
                parameterMap);
        if (result.isEmpty()) {
            return Optional.empty();
        } else {
            String owner = result.get("owner");
            return Optional.of(new Database(masterDatabase.getHost(), masterDatabase.getPort(), databaseName, owner));
        }
    }

}
