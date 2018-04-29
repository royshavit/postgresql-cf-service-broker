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
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@AllArgsConstructor
public class ServiceInstanceRepository {
    
    private final PostgreSQLDatabase postgreSQLDatabase;

    public void save(CreateServiceInstanceRequest createServiceInstanceRequest) throws SQLException {
        Map<Integer, String> parameterMap = new HashMap<>();
        parameterMap.put(1, createServiceInstanceRequest.getServiceInstanceId());
        parameterMap.put(2, createServiceInstanceRequest.getServiceDefinitionId());
        parameterMap.put(3, createServiceInstanceRequest.getPlanId());
        parameterMap.put(4, createServiceInstanceRequest.getOrganizationGuid());
        parameterMap.put(5, createServiceInstanceRequest.getSpaceGuid());
        postgreSQLDatabase.executePreparedUpdate("INSERT INTO service (serviceinstanceid, servicedefinitionid, planid, organizationguid, spaceguid) VALUES (?, ?, ?, ?, ?)", parameterMap);
    }

    public void delete(UUID instanceId) throws SQLException {
        Map<Integer, String> parameterMap = new HashMap<>();
        parameterMap.put(1, instanceId.toString());
        postgreSQLDatabase.executePreparedUpdate("DELETE FROM service WHERE serviceinstanceid=?", parameterMap);
    }

    @SneakyThrows
    public Optional<ServiceInstance> findServiceInstance(UUID instanceId) {
        Map<Integer, String> parameterMap = new HashMap<>();
        parameterMap.put(1, instanceId.toString());
        Map<String, String> result = postgreSQLDatabase.executePreparedSelect("SELECT * FROM service WHERE serviceinstanceid = ?", parameterMap);
        if (result.isEmpty()) {
            return Optional.empty();
        } else {
            String serviceDefinitionId = result.get("servicedefinitionid");
            String organizationGuid = result.get("organizationguid");
            String planId = result.get("planid");
            String spaceGuid = result.get("spaceguid");
            CreateServiceInstanceRequest wrapper
                    = new CreateServiceInstanceRequest(serviceDefinitionId, planId, organizationGuid, spaceGuid)
                    .withServiceInstanceId(instanceId.toString());
            return Optional.of(new ServiceInstance(wrapper));
        }
    }

}
