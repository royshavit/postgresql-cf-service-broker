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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.database.jdbc.QueryExecutor;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@AllArgsConstructor
public class ServiceInstanceRepository {

    private final QueryExecutor queryExecutor;

    @SneakyThrows
    @Autowired
    private void createServiceInstanceTable(DataSource dataSource) { //todo: constructor -> flyway?
        try (Connection connection = dataSource.getConnection()) {
            String createServiceInstanceTable
                    = "CREATE TABLE IF NOT EXISTS service (serviceinstanceid varchar(200) not null default '',"
                    + " servicedefinitionid varchar(200) not null default '',"
                    + " planid varchar(200) not null default '',"
                    + " organizationguid varchar(200) not null default '',"
                    + " spaceguid varchar(200) not null default '')";
            connection.createStatement().execute(createServiceInstanceTable);
        }
    }

    public void save(CreateServiceInstanceRequest serviceInstance) {
        queryExecutor.executeUpdate(String.format(
                "INSERT INTO service (serviceinstanceid, servicedefinitionid, planid, organizationguid, spaceguid) VALUES ('%s', '%s', '%s', '%s', '%s')",
                serviceInstance.getServiceInstanceId(),
                serviceInstance.getServiceDefinitionId(),
                serviceInstance.getPlanId(),
                serviceInstance.getOrganizationGuid(),
                serviceInstance.getSpaceGuid()));
    }

    public void delete(UUID instanceId) {
        queryExecutor.executeUpdate("DELETE FROM service WHERE serviceinstanceid = '" + instanceId + "'");
    }

    public Optional<ServiceInstance> findServiceInstance(UUID instanceId) {
        List<Map<String, String>> instances = queryExecutor.executeSelect(
                "SELECT * FROM service WHERE serviceinstanceid = '" + instanceId + "'");
        Assert.state(instances.size() <= 1, "found multiple instances with id " + instanceId);
        if (instances.isEmpty()) {
            return Optional.empty();
        } else {
            Map<String, String> instance = instances.iterator().next();
            String serviceDefinitionId = instance.get("servicedefinitionid");
            String organizationGuid = instance.get("organizationguid");
            String planId = instance.get("planid");
            String spaceGuid = instance.get("spaceguid");
            CreateServiceInstanceRequest wrapper
                    = new CreateServiceInstanceRequest(serviceDefinitionId, planId, organizationGuid, spaceGuid)
                    .withServiceInstanceId(instanceId.toString());
            return Optional.of(new ServiceInstance(wrapper));
        }
    }

}
