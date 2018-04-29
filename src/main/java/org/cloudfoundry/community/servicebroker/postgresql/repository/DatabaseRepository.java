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
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.postgresql.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class DatabaseRepository {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseRepository.class);
    
    private final PostgreSQLDatabase postgreSQLDatabase;
    
    public void createDatabaseForInstance(String instanceId, String serviceId, String planId, String organizationGuid, String spaceGuid) throws SQLException {
        Utils.checkValidUUID(instanceId);
        postgreSQLDatabase.executeUpdate("CREATE DATABASE \"" + instanceId + "\" ENCODING 'UTF8'");
        postgreSQLDatabase.executeUpdate("REVOKE all on database \"" + instanceId + "\" from public");

        Map<Integer, String> parameterMap = new HashMap<Integer, String>();
        parameterMap.put(1, instanceId);
        parameterMap.put(2, serviceId);
        parameterMap.put(3, planId);
        parameterMap.put(4, organizationGuid);
        parameterMap.put(5, spaceGuid);

        postgreSQLDatabase.executePreparedUpdate("INSERT INTO service (serviceinstanceid, servicedefinitionid, planid, organizationguid, spaceguid) VALUES (?, ?, ?, ?, ?)", parameterMap);
    }

    public void deleteDatabase(String instanceId) throws SQLException {
        Utils.checkValidUUID(instanceId);

        Map<Integer, String> parameterMap = new HashMap<Integer, String>();
        parameterMap.put(1, instanceId);

        Map<String, String> result = postgreSQLDatabase.executeSelect("SELECT current_user");
        String currentUser = null;

        if(result != null) {
            currentUser = result.get("current_user");
        }

        if(currentUser == null) {
            logger.error("Current user for instance '" + instanceId + "' could not be found");
        }

        postgreSQLDatabase.executePreparedSelect("SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = ? AND pid <> pg_backend_pid()", parameterMap);
        postgreSQLDatabase.executeUpdate("ALTER DATABASE \"" + instanceId + "\" OWNER TO \"" + currentUser + "\"");
        postgreSQLDatabase.executeUpdate("DROP DATABASE IF EXISTS \"" + instanceId + "\"");
        postgreSQLDatabase.executePreparedUpdate("DELETE FROM service WHERE serviceinstanceid=?", parameterMap);
    }

    public ServiceInstance findServiceInstance(String instanceId) throws SQLException {
        Utils.checkValidUUID(instanceId);

        Map<Integer, String> parameterMap = new HashMap<Integer, String>();
        parameterMap.put(1, instanceId);

        Map<String, String> result = postgreSQLDatabase.executePreparedSelect("SELECT * FROM service WHERE serviceinstanceid = ?", parameterMap);

        String serviceDefinitionId = result.get("servicedefinitionid");
        String organizationGuid = result.get("organizationguid");
        String planId = result.get("planid");
        String spaceGuid = result.get("spaceguid");

        CreateServiceInstanceRequest wrapper = new CreateServiceInstanceRequest(serviceDefinitionId, planId, organizationGuid, spaceGuid).withServiceInstanceId(instanceId);
        return new ServiceInstance(wrapper);
    }

    // TODO needs to be implemented
    public List<ServiceInstance> getAllServiceInstances() {
        return Collections.emptyList();
    }

    public int getDatabasePort() {
        return postgreSQLDatabase.getDatabasePort();
    }

    public String getDatabaseHost() {
        return postgreSQLDatabase.getDatabaseHost();
    }
}
