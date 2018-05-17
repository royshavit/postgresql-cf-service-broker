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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.database.repository.DatabaseRepository;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class DatabaseBindingService implements ServiceInstanceBindingService {

    private final DatabaseRepository databaseRepository;
    private final Random random;
    
    @Value("${grant.users.elevated.privileges:false}")
    private boolean grantUsersElevatedPrivileges; //todo: constructor
    
    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest)
            throws ServiceInstanceBindingExistsException, ServiceBrokerException {
        String serviceInstanceId = createServiceInstanceBindingRequest.getServiceInstanceId();
        String bindingId = createServiceInstanceBindingRequest.getBindingId();
        log.info("creating binding {} for instance {}", bindingId, serviceInstanceId);
        String password = new BigInteger(130, random).toString(32);
        Map<String, Object> credentials = databaseRepository.createUser(serviceInstanceId, bindingId, password, grantUsersElevatedPrivileges);
        ServiceInstanceBinding binding = new ServiceInstanceBinding(
                bindingId,
                serviceInstanceId,
                credentials,
                null,
                createServiceInstanceBindingRequest.getAppGuid());
        log.info("created binding {} for instance {}", bindingId, serviceInstanceId);
        return binding;
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest deleteServiceInstanceBindingRequest)
            throws ServiceBrokerException {
        String serviceInstanceId = deleteServiceInstanceBindingRequest.getInstance().getServiceInstanceId();
        String bindingId = deleteServiceInstanceBindingRequest.getBindingId();
        log.info("deleting binding {} for instance {}", bindingId, serviceInstanceId);
        databaseRepository.deleteUser(serviceInstanceId, bindingId);
        ServiceInstanceBinding binding = new ServiceInstanceBinding(bindingId, serviceInstanceId, null, null, null);
        log.info("deleted binding {} for instance {}", bindingId, serviceInstanceId);
        return binding;
    }

    //todo: throw ServiceInstanceDoesNotExistException - it is an advised exception, also:
    //ServiceInstanceBindingExistsException
}