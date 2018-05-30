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

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.database.repository.DatabaseRepository;
import org.cloudfoundry.community.servicebroker.database.repository.ServiceInstanceRepository;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class DatabaseBindingService implements ServiceInstanceBindingService {

    private final DatabaseRepository databaseRepository;
    private final ServiceInstanceRepository serviceInstanceRepository;
    private final Random random;
    private final boolean grantUsersElevatedPrivileges;

    public DatabaseBindingService(DatabaseRepository databaseRepository,
                                  ServiceInstanceRepository serviceInstanceRepository,
                                  Random random,
                                  @Value("${database.privileges.elevated:false}") boolean grantUsersElevatedPrivileges) {
        this.databaseRepository = databaseRepository;
        this.serviceInstanceRepository = serviceInstanceRepository;
        this.random = random;
        this.grantUsersElevatedPrivileges = grantUsersElevatedPrivileges;
    }

    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest)
            throws ServiceInstanceBindingExistsException, ServiceBrokerException {
        UUID serviceInstanceId = UUID.fromString(createServiceInstanceBindingRequest.getServiceInstanceId());
        String bindingId = createServiceInstanceBindingRequest.getBindingId();
        verifyServiceInstanceExists(serviceInstanceId);
        if (databaseRepository.userExists(serviceInstanceId.toString(), bindingId)) {
            throw new ServiceInstanceBindingExistsException(getServiceInstanceBinding(
                    createServiceInstanceBindingRequest, serviceInstanceId, bindingId, Collections.emptyMap()));
        } else {
            return createBinding(createServiceInstanceBindingRequest, serviceInstanceId, bindingId);
        }
    }

    private ServiceInstanceBinding createBinding(CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest, UUID serviceInstanceId, String bindingId) {
        log.info("creating binding {} for instance {}", bindingId, serviceInstanceId);
        String password = new BigInteger(130, random).toString(32);
        Map<String, Object> credentials = databaseRepository.createUser(
                serviceInstanceId.toString(), bindingId, password, grantUsersElevatedPrivileges);
        ServiceInstanceBinding binding = getServiceInstanceBinding(
                createServiceInstanceBindingRequest, serviceInstanceId, bindingId, credentials);
        log.info("created binding {} for instance {}", bindingId, serviceInstanceId);
        return binding;
    }

    private ServiceInstanceBinding getServiceInstanceBinding(CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest, UUID serviceInstanceId, String bindingId, Map<String, Object> credentials) {
        return new ServiceInstanceBinding(
                bindingId,
                serviceInstanceId.toString(),
                credentials,
                null,
                createServiceInstanceBindingRequest.getAppGuid());
    }

    private ServiceInstance verifyServiceInstanceExists(UUID serviceInstanceId) throws ServiceBrokerException {
        return serviceInstanceRepository.findServiceInstance(serviceInstanceId).orElseThrow(
                () -> new ServiceBrokerException("instance " + serviceInstanceId + " not found")
        );
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest deleteServiceInstanceBindingRequest)
            throws ServiceBrokerException {
        UUID serviceInstanceId = UUID.fromString(deleteServiceInstanceBindingRequest.getInstance().getServiceInstanceId());
        String bindingId = deleteServiceInstanceBindingRequest.getBindingId();
        verifyServiceInstanceExists(serviceInstanceId);
        if (databaseRepository.userExists(serviceInstanceId.toString(), bindingId)) {
            return deleteBinding(serviceInstanceId, bindingId);
        } else {
            log.info("binding {} of instance {} does not exist", bindingId, serviceInstanceId);
            return null;
        }
    }

    private ServiceInstanceBinding deleteBinding(UUID serviceInstanceId, String bindingId) {
        log.info("deleting binding {} for instance {}", bindingId, serviceInstanceId);
        databaseRepository.deleteUser(serviceInstanceId.toString(), bindingId);
        ServiceInstanceBinding binding = new ServiceInstanceBinding(bindingId, serviceInstanceId.toString(), null, null, null);
        log.info("deleted binding {} for instance {}", bindingId, serviceInstanceId);
        return binding;
    }

}