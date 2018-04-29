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
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.UUID;

@Component
@AllArgsConstructor
public class RoleRepository {

    private final PostgreSQLDatabase postgreSQLDatabase;

    public void createRoleForInstance(UUID instanceId) throws SQLException {
        postgreSQLDatabase.executeUpdate("CREATE ROLE \"" + instanceId + "\"");
        postgreSQLDatabase.executeUpdate("GRANT \"" + postgreSQLDatabase.getUsername() + "\" TO \"" + instanceId + "\"");
    }

    public void deleteRole(UUID instanceId) throws SQLException {
        postgreSQLDatabase.executeUpdate("DROP ROLE IF EXISTS \"" + instanceId + "\"");
    }

    public String bindRoleToDatabase(UUID dbInstanceId) throws SQLException {
        SecureRandom random = new SecureRandom();
        String passwd = new BigInteger(130, random).toString(32);

        postgreSQLDatabase.executeUpdate("ALTER ROLE \"" + dbInstanceId + "\" LOGIN password '" + passwd + "'");
        return passwd;
    }

    public void unBindRoleFromDatabase(UUID dbInstanceId) throws SQLException{
        postgreSQLDatabase.executeUpdate("ALTER ROLE \"" + dbInstanceId + "\" NOLOGIN");
    }
}