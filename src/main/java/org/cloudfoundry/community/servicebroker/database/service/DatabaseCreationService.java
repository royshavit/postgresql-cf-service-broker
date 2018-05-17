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
package org.cloudfoundry.community.servicebroker.database.service;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.database.repository.DatabaseRepository;
import org.cloudfoundry.community.servicebroker.database.repository.ServiceInstanceRepository;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class DatabaseCreationService implements ServiceInstanceService {

    private final DatabaseRepository databaseRepository;
    private final ServiceInstanceRepository serviceInstanceRepository;

    @Override
    public ServiceInstance createServiceInstance(CreateServiceInstanceRequest createServiceInstanceRequest)
            throws ServiceInstanceExistsException, ServiceBrokerException {
        UUID serviceInstanceId = UUID.fromString(createServiceInstanceRequest.getServiceInstanceId());
        serviceInstanceRepository.findServiceInstance(serviceInstanceId).ifPresent(this::throwAlreadyExistsException); 
        log.info("creating instance {}", serviceInstanceId);
        databaseRepository.createDatabase(serviceInstanceId.toString());
        serviceInstanceRepository.save(createServiceInstanceRequest);
        log.info("created instance {}", serviceInstanceId);
        return new ServiceInstance(createServiceInstanceRequest);
    }

    @SneakyThrows
    private void throwAlreadyExistsException(ServiceInstance serviceInstance) {
        throw new ServiceInstanceExistsException(serviceInstance);
    }

    @Override
    public ServiceInstance deleteServiceInstance(DeleteServiceInstanceRequest deleteServiceInstanceRequest)
            throws ServiceBrokerException {
        UUID serviceInstanceId = UUID.fromString(deleteServiceInstanceRequest.getServiceInstanceId());
        log.info("deleting instance {}", serviceInstanceId);
        Optional<ServiceInstance> instance = serviceInstanceRepository.findServiceInstance(serviceInstanceId);
        instance.ifPresent(serviceInstance -> deleteServiceInstance(serviceInstanceId));
        return instance.orElse(null);
    }

    //todo: perhaps should verify that no bindings exist before deleting instance, although cloud foundry should ensure this.
    private void deleteServiceInstance(UUID serviceInstanceId) {
        serviceInstanceRepository.delete(serviceInstanceId);
        databaseRepository.deleteDatabase(serviceInstanceId.toString());
        log.info("deleted instance {}", serviceInstanceId);
    }

    @Override
    public ServiceInstance updateServiceInstance(UpdateServiceInstanceRequest updateServiceInstanceRequest)
            throws ServiceInstanceUpdateNotSupportedException, ServiceBrokerException, ServiceInstanceDoesNotExistException { //todo: ServiceInstanceDoesNotExistException is exception advised
        throw new ServiceInstanceUpdateNotSupportedException("service update not supported");
    }

    @Override
    public ServiceInstance getServiceInstance(String id) {
        return serviceInstanceRepository.findServiceInstance(UUID.fromString(id)).orElse(null);
    }

}