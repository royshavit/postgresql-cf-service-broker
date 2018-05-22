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
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.database.jdbc.QueryExecutor;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@AllArgsConstructor
public class ServiceInstanceRepository {

    private final QueryExecutor queryExecutor;

    public void save(CreateServiceInstanceRequest serviceInstance) {
        log.info("saving service instance {}", serviceInstance.getServiceInstanceId());
        queryExecutor.update(String.format(
                "INSERT INTO \"brokerdb\".serviceinstance (id, service_definition_id, plan_id, org_id, space_id) VALUES ('%s', '%s', '%s', '%s', '%s')",
                serviceInstance.getServiceInstanceId(),
                serviceInstance.getServiceDefinitionId(),
                serviceInstance.getPlanId(),
                serviceInstance.getOrganizationGuid(),
                serviceInstance.getSpaceGuid()));
        log.info("saved service instance {}", serviceInstance.getServiceInstanceId());
    }

    public void delete(UUID instanceId) {
        log.info("deleting service instance {}", instanceId);
        queryExecutor.update("DELETE FROM \"brokerdb\".serviceinstance WHERE id = '" + instanceId + "'");
        log.info("deleted service instance {}", instanceId);
    }

    public Optional<ServiceInstance> findServiceInstance(UUID instanceId) {
        log.info("locating service instance {}", instanceId);
        List<Map<String, String>> instances = queryExecutor.select(
                "SELECT * FROM \"brokerdb\".serviceinstance WHERE id = '" + instanceId + "'");
        Assert.state(instances.size() <= 1, "found multiple instances with id " + instanceId);
        if (instances.isEmpty()) {
            log.info("service instance {} not found", instanceId);
            return Optional.empty();
        } else {
            log.info("found service instance {}", instanceId);
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
