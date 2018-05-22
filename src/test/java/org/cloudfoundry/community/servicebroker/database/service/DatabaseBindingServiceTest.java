package org.cloudfoundry.community.servicebroker.database.service;

import org.cloudfoundry.community.servicebroker.database.jdbc.QueryExecutor;
import org.cloudfoundry.community.servicebroker.database.repository.Consts;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Created by taitz.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = BrokerTestConfig.class)
@ActiveProfiles(Consts.H2)
public class DatabaseBindingServiceTest {


    private static final String INSTANCE_ID = new UUID(1, 1).toString();
    private static final String BINDING_ID = new UUID(1, 2).toString();
    private static final CreateServiceInstanceRequest CREATE_REQUEST
            = new CreateServiceInstanceRequest().withServiceInstanceId(INSTANCE_ID);
    private static final DeleteServiceInstanceRequest DELETE_REQUEST
            = new DeleteServiceInstanceRequest(INSTANCE_ID, "", "");
    private static final CreateServiceInstanceBindingRequest BIND_REQUEST
            = new CreateServiceInstanceBindingRequest().withServiceInstanceId(INSTANCE_ID).withBindingId(BINDING_ID);
    private static final DeleteServiceInstanceBindingRequest UNBIND_REQUEST
            = new DeleteServiceInstanceBindingRequest(BINDING_ID, new ServiceInstance(DELETE_REQUEST), "", "");

    @Autowired
    private DatabaseCreationService databaseCreationService;

    @Autowired
    private DatabaseBindingService databaseBindingService;

    @Before
    public void clean() {
        try {
            databaseBindingService.deleteServiceInstanceBinding(UNBIND_REQUEST);
        } catch (Exception e) {
            System.out.println("failed to unbind binding " + BINDING_ID + " due to " + e.getMessage());
        }
        try {
            databaseCreationService.deleteServiceInstance(DELETE_REQUEST);
        } catch (Exception e) {
            System.out.println("failed to delete service instance " + INSTANCE_ID + " due to " + e.getMessage());
        }
    }


    @Test
    public void createServiceInstanceBinding_instanceDoesNotExist_fails() {
        assertThatThrownBy(
                () -> databaseBindingService.createServiceInstanceBinding(BIND_REQUEST)
        ).isInstanceOf(ServiceBrokerException.class);
    }

    @Test
    public void createServiceInstanceBinding_bindingDoesNotExist_returnsCredentials() throws ServiceInstanceBindingExistsException, ServiceBrokerException, ServiceInstanceExistsException {
        databaseCreationService.createServiceInstance(CREATE_REQUEST);

        ServiceInstanceBinding binding = databaseBindingService.createServiceInstanceBinding(BIND_REQUEST);

        assertThat(binding.getId(), is(BINDING_ID));
        assertConnectable(binding.getCredentials());
    }

    private void assertConnectable(Map<String, Object> credentials) {
        String url = (String) credentials.get("jdbcurl");
        List<Map<String, String>> rows = new QueryExecutor(url).select("select 1");
        assertThat(rows.size(), is(1));
        String result = rows.iterator().next().values().iterator().next();
        assertThat(result, is("1"));
    }

    private void assertNotConnectable(Map<String, Object> credentials) {
        String url = (String) credentials.get("jdbcurl"); //todo: re-use
        assertThatThrownBy(() ->
                new QueryExecutor(url).select("select 1")
        ).isInstanceOf(SQLException.class);

    }

    @Test
    public void createServiceInstanceBinding_bindingAlreadyExists_fails() throws ServiceBrokerException, ServiceInstanceExistsException, ServiceInstanceBindingExistsException {
        databaseCreationService.createServiceInstance(CREATE_REQUEST);
        databaseBindingService.createServiceInstanceBinding(BIND_REQUEST);
        
        assertThatThrownBy(
                () -> databaseBindingService.createServiceInstanceBinding(BIND_REQUEST)
        ).isInstanceOf(ServiceInstanceBindingExistsException.class);
    }

    @Test
    public void deleteServiceInstanceBinding_instanceDoesNotExists_fails() {
        assertThatThrownBy(
                () -> databaseBindingService.deleteServiceInstanceBinding(UNBIND_REQUEST)
        ).isInstanceOf(ServiceBrokerException.class);
    }

    @Test
    public void deleteServiceInstanceBinding_bindingDoesNotExist_returnsNull() throws ServiceBrokerException, ServiceInstanceExistsException {
        databaseCreationService.createServiceInstance(CREATE_REQUEST);

        ServiceInstanceBinding binding = databaseBindingService.deleteServiceInstanceBinding(UNBIND_REQUEST);

        assertNull(binding);
    }

    @Test
    public void deleteServiceInstanceBinding_bindingExists_revokesCredentials() throws ServiceBrokerException, ServiceInstanceExistsException, ServiceInstanceBindingExistsException {
        databaseCreationService.createServiceInstance(CREATE_REQUEST);
        Map<String, Object> credentials = databaseBindingService.createServiceInstanceBinding(BIND_REQUEST).getCredentials();
        assertConnectable(credentials);

        databaseBindingService.deleteServiceInstanceBinding(UNBIND_REQUEST);

        assertNotConnectable(credentials);
    }
//todo: multiple binds
}