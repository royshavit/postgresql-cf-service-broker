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

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.postgresql.jdbc.QueryExecutor;
import org.cloudfoundry.community.servicebroker.postgresql.model.Database;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@AllArgsConstructor
public class DatabaseRepository {

    private static final Database.UrlGenerator URL_GENERATOR = (host, port, name, owner, password) ->
            String.format("postgres://%s:%s@%s:%d/%s", owner, password, host, port, name);

    private final QueryExecutor queryExecutor;
    private final Database masterDb;

    public void create(String databaseName, String owner) throws SQLException {
        queryExecutor.executeUpdate("CREATE DATABASE \"" + databaseName + "\" ENCODING 'UTF8'");
        queryExecutor.executeUpdate("REVOKE all on database \"" + databaseName + "\" from public");
        queryExecutor.executeUpdate("ALTER DATABASE \"" + databaseName + "\" OWNER TO \"" + owner + "\"");
    }

    public void delete(String databaseName) throws SQLException {
        queryExecutor.executePreparedSelect(
                "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = ? AND pid <> pg_backend_pid()",
                ImmutableMap.of(1, databaseName));
        queryExecutor.executeUpdate("DROP DATABASE IF EXISTS \"" + databaseName + "\"");
    }

    @SneakyThrows
    public Optional<Database> findOne(String dbName) {
        Map<String, String> result = queryExecutor.executePreparedSelect(
                "SELECT d.datname, pg_catalog.pg_get_userbyid(d.datdba) AS owner FROM pg_catalog.pg_database d WHERE d.datname = ?;",
                ImmutableMap.of(1, dbName));
        if (result.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(
                    new Database(masterDb.getHost(), masterDb.getPort(), dbName, result.get("owner"), URL_GENERATOR)
            );
        }
    }

}
