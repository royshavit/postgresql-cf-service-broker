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

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
@AllArgsConstructor
public class PostgreSQLServiceInstanceBindingService implements ServiceInstanceBindingService {

    private final DatabaseRepository databaseRepository;
    private final RoleRepository roleRepository;
    private final Random random;

    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest)
            throws ServiceInstanceBindingExistsException, ServiceBrokerException {
        String serviceInstanceId = createServiceInstanceBindingRequest.getServiceInstanceId();
        String password = new BigInteger(130, random).toString(32);
        try {
            roleRepository.setPassword(serviceInstanceId, password);
        } catch (SQLException e) {
            throw new ServiceBrokerException(e.getMessage());
        }
        return new ServiceInstanceBinding(
                createServiceInstanceBindingRequest.getBindingId(),
                serviceInstanceId,
                buildCredentials(serviceInstanceId, password),
                null,
                createServiceInstanceBindingRequest.getAppGuid());
    }

    private Map<String, Object> buildCredentials(String serviceInstanceId, String password) {
        Database database = databaseRepository.findOne(serviceInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("found no database for service instance " + serviceInstanceId));
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("uri", database.getUrl(password));
        credentials.put("username", database.getOwner());
        credentials.put("password", password);
        credentials.put("hostname", database.getHost());
        credentials.put("port", database.getPort());
        credentials.put("database", database.getName());
        return credentials;
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest deleteServiceInstanceBindingRequest)
            throws ServiceBrokerException {
        String serviceInstanceId = deleteServiceInstanceBindingRequest.getInstance().getServiceInstanceId();
        try {
            roleRepository.unsetPassword(serviceInstanceId);
        } catch (SQLException e) {
            throw new ServiceBrokerException(e.getMessage());
        }
        return new ServiceInstanceBinding(deleteServiceInstanceBindingRequest.getBindingId(), serviceInstanceId, null,
                null, null);
    }
    
}