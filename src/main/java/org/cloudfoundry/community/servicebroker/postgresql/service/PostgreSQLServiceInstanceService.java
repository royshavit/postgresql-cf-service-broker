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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.postgresql.model.Database;
import org.cloudfoundry.community.servicebroker.postgresql.repository.DatabaseRepository;
import org.cloudfoundry.community.servicebroker.postgresql.repository.RoleRepository;
import org.cloudfoundry.community.servicebroker.postgresql.repository.ServiceInstanceRepository;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class PostgreSQLServiceInstanceService implements ServiceInstanceService {

    private final DatabaseRepository databaseRepository;
    private final RoleRepository roleRepository;
    private final ServiceInstanceRepository serviceInstanceRepository;
    private final Database masterDatabaseUrl;

    @Override
    public ServiceInstance createServiceInstance(CreateServiceInstanceRequest createServiceInstanceRequest)
            throws ServiceInstanceExistsException, ServiceBrokerException {
        UUID serviceInstanceId = UUID.fromString(createServiceInstanceRequest.getServiceInstanceId());
        try {
            roleRepository.create(serviceInstanceId.toString());
            roleRepository.grantRoleTo(serviceInstanceId.toString(), masterDatabaseUrl.getOwner());
            databaseRepository.create(serviceInstanceId.toString(), serviceInstanceId.toString());
            serviceInstanceRepository.save(createServiceInstanceRequest);
        } catch (SQLException e) {
            log.error("Error while creating service instance '" + serviceInstanceId + "'", e);
            throw new ServiceBrokerException(e.getMessage());
        }
        return new ServiceInstance(createServiceInstanceRequest);
    }

    @Override
    public ServiceInstance deleteServiceInstance(DeleteServiceInstanceRequest deleteServiceInstanceRequest)
            throws ServiceBrokerException {
        UUID serviceInstanceId = UUID.fromString(deleteServiceInstanceRequest.getServiceInstanceId());
        Optional<ServiceInstance> instance = getServiceInstance(serviceInstanceId);
        instance.ifPresent(serviceInstance -> deleteServiceInstance(serviceInstanceId));
        return instance.orElse(null);
    }

    @SneakyThrows
    private void deleteServiceInstance(UUID serviceInstanceId) {
        try {
            serviceInstanceRepository.delete(serviceInstanceId);
            databaseRepository.delete(serviceInstanceId.toString());
            roleRepository.delete(serviceInstanceId.toString());
        } catch (SQLException e) {
            log.error("Error while deleting service instance '" + serviceInstanceId + "'", e);
            throw new ServiceBrokerException(e.getMessage());
        }
    }

    @Override
    public ServiceInstance updateServiceInstance(UpdateServiceInstanceRequest updateServiceInstanceRequest)
            throws ServiceInstanceUpdateNotSupportedException, ServiceBrokerException, ServiceInstanceDoesNotExistException {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public ServiceInstance getServiceInstance(String id) {
        return getServiceInstance(UUID.fromString(id)).orElse(null);
    }

    private Optional<ServiceInstance> getServiceInstance(UUID id) {
        return serviceInstanceRepository.findServiceInstance(id);
    }

}