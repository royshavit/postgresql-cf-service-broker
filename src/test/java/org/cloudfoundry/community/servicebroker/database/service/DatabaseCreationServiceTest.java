package org.cloudfoundry.community.servicebroker.database.service;

import org.cloudfoundry.community.servicebroker.database.repository.Consts;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cloudfoundry.community.servicebroker.database.service.Exceptions.swallowException;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Created by taitz.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = BrokerTestConfig.class)
@ActiveProfiles(Consts.H2)
public class DatabaseCreationServiceTest {

    private static final String INSTANCE_ID = new UUID(1, 1).toString();
    private static final CreateServiceInstanceRequest CREATE_REQUEST
            = new CreateServiceInstanceRequest().withServiceInstanceId(INSTANCE_ID);
    private static final DeleteServiceInstanceRequest DELETE_REQUEST
            = new DeleteServiceInstanceRequest(INSTANCE_ID, "", "");
    private static final UpdateServiceInstanceRequest UPDATE_REQUEST
            = new UpdateServiceInstanceRequest().withInstanceId(INSTANCE_ID);

    @Autowired
    private DatabaseCreationService databaseCreationService;

    @Before
    public void clean() {
        swallowException(() -> databaseCreationService.deleteServiceInstance(DELETE_REQUEST));
    }

    @Test
    public void createServiceInstance_instanceDoesNotExist_returnsInstance() throws ServiceBrokerException, ServiceInstanceExistsException {
        ServiceInstance serviceInstance = databaseCreationService.createServiceInstance(CREATE_REQUEST);

        assertThat(serviceInstance.getServiceInstanceId(), is(INSTANCE_ID));
    }

    @Test
    public void createServiceInstance_instanceAlreadyExists_fails() throws ServiceBrokerException, ServiceInstanceExistsException {
        databaseCreationService.createServiceInstance(CREATE_REQUEST);

        assertThatThrownBy(
                () -> databaseCreationService.createServiceInstance(CREATE_REQUEST)
        ).isInstanceOf(ServiceInstanceExistsException.class);
    }

    @Test
    public void deleteServiceInstance_instanceDoesNotExist_returnsNull() throws ServiceBrokerException, ServiceInstanceExistsException {
        ServiceInstance serviceInstance = databaseCreationService.deleteServiceInstance(DELETE_REQUEST);

        assertNull(serviceInstance);
    }

    @Test
    public void deleteServiceInstance_instanceAlreadyExists_returnsInstance() throws ServiceBrokerException, ServiceInstanceExistsException {
        databaseCreationService.createServiceInstance(CREATE_REQUEST);

        ServiceInstance serviceInstance = databaseCreationService.deleteServiceInstance(DELETE_REQUEST);

        assertThat(serviceInstance.getServiceInstanceId(), is(INSTANCE_ID));
    }

    @Test
    public void getServiceInstance_instanceDoesNotExist_returnsNull() throws ServiceBrokerException, ServiceInstanceExistsException {
        ServiceInstance serviceInstance = databaseCreationService.getServiceInstance(INSTANCE_ID);

        assertNull(serviceInstance);
    }

    @Test
    public void getServiceInstance_instanceAlreadyExists_returnsInstance() throws ServiceBrokerException, ServiceInstanceExistsException {
        databaseCreationService.createServiceInstance(CREATE_REQUEST);
        ServiceInstance serviceInstance = databaseCreationService.getServiceInstance(INSTANCE_ID);

        assertThat(serviceInstance.getServiceInstanceId(), is(INSTANCE_ID));
    }

    @Test
    public void updateServiceInstance_instanceDoesNotExist_throwsUnsupportedException() {
        assertThatThrownBy(
                () -> databaseCreationService.updateServiceInstance(UPDATE_REQUEST)

        ).isInstanceOf(ServiceInstanceUpdateNotSupportedException.class);
    }

    @Test
    public void updateServiceInstance_instanceAlreadyExists_throwsUnsupportedException() throws ServiceBrokerException, ServiceInstanceExistsException {
        databaseCreationService.createServiceInstance(CREATE_REQUEST);

        assertThatThrownBy(
                () -> databaseCreationService.updateServiceInstance(UPDATE_REQUEST)

        ).isInstanceOf(ServiceInstanceUpdateNotSupportedException.class);
    }

}