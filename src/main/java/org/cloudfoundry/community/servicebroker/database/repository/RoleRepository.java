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
package org.cloudfoundry.community.servicebroker.database.repository;

import lombok.AllArgsConstructor;
import org.cloudfoundry.community.servicebroker.database.jdbc.QueryExecutor;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component
@AllArgsConstructor
public class RoleRepository {

    private final QueryExecutor queryExecutor;

    public void create(String name) throws SQLException {
        queryExecutor.executeUpdate("CREATE ROLE \"" + name + "\"");
    }

    public void delete(String name) throws SQLException {
        queryExecutor.executeUpdate("DROP ROLE IF EXISTS \"" + name + "\"");
    }

    public void setPassword(String roleName, String password) throws SQLException {
        queryExecutor.executeUpdate("ALTER ROLE \"" + roleName + "\" LOGIN password '" + password + "'");
    }

    public void unsetPassword(String roleName) throws SQLException{
        queryExecutor.executeUpdate("ALTER ROLE \"" + roleName + "\" NOLOGIN");
    }

    public void grantRoleTo(String roleMember, String roleGroup) throws SQLException {
        queryExecutor.executeUpdate("GRANT \"" + roleGroup + "\" TO \"" + roleMember + "\"");
    }

}