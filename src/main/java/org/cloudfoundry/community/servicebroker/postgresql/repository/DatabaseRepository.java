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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@AllArgsConstructor
public class DatabaseRepository {
    
    private final PostgreSQLDatabase postgreSQLDatabase;
    
    public void createDatabaseForInstance(UUID instanceId) throws SQLException {
        postgreSQLDatabase.executeUpdate("CREATE DATABASE \"" + instanceId + "\" ENCODING 'UTF8'");
        postgreSQLDatabase.executeUpdate("REVOKE all on database \"" + instanceId + "\" from public");
        postgreSQLDatabase.executeUpdate("ALTER DATABASE \"" + instanceId + "\" OWNER TO \"" + instanceId + "\"");
    }

    public void deleteDatabase(UUID instanceId) throws SQLException {
        Map<Integer, String> parameterMap = new HashMap<Integer, String>();
        parameterMap.put(1, instanceId.toString());

        Map<String, String> result = postgreSQLDatabase.executeSelect("SELECT current_user");
        String currentUser = null;

        if(result != null) {
            currentUser = result.get("current_user");
        }

        if(currentUser == null) {
            log.error("Current user for instance '" + instanceId + "' could not be found");
        }

        postgreSQLDatabase.executePreparedSelect("SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = ? AND pid <> pg_backend_pid()", parameterMap);
        postgreSQLDatabase.executeUpdate("ALTER DATABASE \"" + instanceId + "\" OWNER TO \"" + currentUser + "\"");
        postgreSQLDatabase.executeUpdate("DROP DATABASE IF EXISTS \"" + instanceId + "\"");
    }

    public int getDatabasePort() {
        return postgreSQLDatabase.getDatabasePort();
    }

    public String getDatabaseHost() {
        return postgreSQLDatabase.getDatabaseHost();
    }
}
