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
package org.cloudfoundry.community.servicebroker.postgresql.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.postgresql.model.Database;
import org.cloudfoundry.community.servicebroker.postgresql.repository.DatabaseRepository;
import org.cloudfoundry.community.servicebroker.postgresql.repository.RoleRepository;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class PostgreSQLServiceInstanceBindingService implements ServiceInstanceBindingService {

    private final DatabaseRepository databaseRepository;
    private final RoleRepository roleRepository;


    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest)
            throws ServiceInstanceBindingExistsException, ServiceBrokerException {
        UUID bindingId = UUID.fromString(createServiceInstanceBindingRequest.getBindingId());
        UUID serviceInstanceId = UUID.fromString(createServiceInstanceBindingRequest.getServiceInstanceId());
        UUID appGuid = UUID.fromString(createServiceInstanceBindingRequest.getAppGuid());
        String passwd;
        Database database;

        try {
            database = databaseRepository.findOne(serviceInstanceId)
                    .orElseThrow(() -> new IllegalArgumentException("found no database for service instance " + serviceInstanceId));
            passwd = roleRepository.bindRoleToDatabase(serviceInstanceId);
        } catch (SQLException e) {
            log.error("Error while creating service instance binding '" + bindingId + "'", e);
            throw new ServiceBrokerException(e.getMessage());
        }

        String dbURL = String.format("postgres://%s:%s@%s:%d/%s", serviceInstanceId, passwd, database.getHost(),
                database.getPort(), database.getName());

        Map<String, Object> credentials = new HashMap<>();
        credentials.put("uri", dbURL);
        credentials.put("username", serviceInstanceId);
        credentials.put("password", passwd);
        credentials.put("hostname", database.getHost());
        credentials.put("port", database.getPort());
        credentials.put("database", database.getName());

        return new ServiceInstanceBinding(bindingId.toString(), serviceInstanceId.toString(), credentials, null, appGuid.toString());
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest deleteServiceInstanceBindingRequest)
            throws ServiceBrokerException {
        UUID serviceInstanceId = UUID.fromString(deleteServiceInstanceBindingRequest.getInstance().getServiceInstanceId());
        UUID bindingId = UUID.fromString(deleteServiceInstanceBindingRequest.getBindingId());
        try {
            roleRepository.unBindRoleFromDatabase(serviceInstanceId);
        } catch (SQLException e) {
            log.error("Error while deleting service instance binding '" + bindingId + "'", e);
            throw new ServiceBrokerException(e.getMessage());
        }
        return new ServiceInstanceBinding(bindingId.toString(), serviceInstanceId.toString(), null, null, null);
    }
}