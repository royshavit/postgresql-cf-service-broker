package org.cloudfoundry.community.servicebroker.database.service;

import lombok.SneakyThrows;
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
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cloudfoundry.community.servicebroker.database.service.BrokerTestConfig.assumePostgresProfile;
import static org.cloudfoundry.community.servicebroker.database.service.Exceptions.swallowException;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by taitz.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = BrokerTestConfig.class)
@ActiveProfiles(Consts.H2)
public class DatabaseBindingServiceTest {


    private static final String INSTANCE_ID = new UUID(1, 1).toString();
    private static final String BINDING_ID1 = new UUID(1, 2).toString();
    private static final String BINDING_ID2 = new UUID(1, 3).toString();
    private static final CreateServiceInstanceRequest CREATE_REQUEST
            = new CreateServiceInstanceRequest().withServiceInstanceId(INSTANCE_ID);
    private static final DeleteServiceInstanceRequest DELETE_REQUEST
            = new DeleteServiceInstanceRequest(INSTANCE_ID, "", "");
    private static final CreateServiceInstanceBindingRequest BIND_REQUEST1
            = new CreateServiceInstanceBindingRequest().withServiceInstanceId(INSTANCE_ID).withBindingId(BINDING_ID1);
    private static final CreateServiceInstanceBindingRequest BIND_REQUEST2
            = new CreateServiceInstanceBindingRequest().withServiceInstanceId(INSTANCE_ID).withBindingId(BINDING_ID2);
    private static final DeleteServiceInstanceBindingRequest UNBIND_REQUEST1
            = new DeleteServiceInstanceBindingRequest(BINDING_ID1, new ServiceInstance(DELETE_REQUEST), "", "");
    private static final DeleteServiceInstanceBindingRequest UNBIND_REQUEST2
            = new DeleteServiceInstanceBindingRequest(BINDING_ID2, new ServiceInstance(DELETE_REQUEST), "", "");

    @Autowired
    private DatabaseCreationService databaseCreationService;

    @Autowired
    private DatabaseBindingService databaseBindingService;

    @Autowired
    private Environment environment;

    @Before
    public void clean() {
        swallowException(() -> databaseBindingService.deleteServiceInstanceBinding(UNBIND_REQUEST1));
        swallowException(() -> databaseBindingService.deleteServiceInstanceBinding(UNBIND_REQUEST2));
        swallowException(() -> databaseCreationService.deleteServiceInstance(DELETE_REQUEST));
    }


    @Test
    public void createServiceInstanceBinding_instanceDoesNotExist_fails() {
        assertThatThrownBy(
                () -> databaseBindingService.createServiceInstanceBinding(BIND_REQUEST1)
        ).isInstanceOf(ServiceBrokerException.class);
    }

    @Test
    public void createServiceInstanceBinding_bindingDoesNotExist_returnsCredentials() throws ServiceInstanceBindingExistsException, ServiceBrokerException, ServiceInstanceExistsException {
        databaseCreationService.createServiceInstance(CREATE_REQUEST);

        ServiceInstanceBinding binding = databaseBindingService.createServiceInstanceBinding(BIND_REQUEST1);

        assertThat(binding.getId(), is(BINDING_ID1));
        assertConnectable(binding.getCredentials());
    }

    private void assertConnectable(Map<String, Object> credentials) {
        List<Map<String, String>> rows = selectOne(credentials);
        assertThat(rows.size(), is(1));
        String result = rows.iterator().next().values().iterator().next();
        assertThat(result, is("1"));
    }

    private void assertNotConnectable(Map<String, Object> credentials) {
        assertThatThrownBy(
                () -> selectOne(credentials)
        ).isInstanceOf(SQLException.class);
    }

    private List<Map<String, String>> selectOne(Map<String, Object> credentials) {
        String url = (String) credentials.get("jdbcurl");
        return new QueryExecutor(url).select("select 1");
    }

    @Test
    public void createServiceInstanceBinding_bindingAlreadyExists_fails() throws ServiceBrokerException, ServiceInstanceExistsException, ServiceInstanceBindingExistsException {
        databaseCreationService.createServiceInstance(CREATE_REQUEST);
        databaseBindingService.createServiceInstanceBinding(BIND_REQUEST1);

        assertThatThrownBy(
                () -> databaseBindingService.createServiceInstanceBinding(BIND_REQUEST1)
        ).isInstanceOf(ServiceInstanceBindingExistsException.class);
    }

    @Test
    public void deleteServiceInstanceBinding_instanceDoesNotExists_fails() {
        assertThatThrownBy(
                () -> databaseBindingService.deleteServiceInstanceBinding(UNBIND_REQUEST1)
        ).isInstanceOf(ServiceBrokerException.class);
    }

    @Test
    public void deleteServiceInstanceBinding_bindingDoesNotExist_returnsNull() throws ServiceBrokerException, ServiceInstanceExistsException {
        databaseCreationService.createServiceInstance(CREATE_REQUEST);

        ServiceInstanceBinding binding = databaseBindingService.deleteServiceInstanceBinding(UNBIND_REQUEST1);

        assertNull(binding);
    }

    @Test
    public void deleteServiceInstanceBinding_bindingExists_revokesCredentials() throws ServiceBrokerException, ServiceInstanceExistsException, ServiceInstanceBindingExistsException {
        databaseCreationService.createServiceInstance(CREATE_REQUEST);
        Map<String, Object> credentials = databaseBindingService.createServiceInstanceBinding(BIND_REQUEST1).getCredentials();
        assertConnectable(credentials);

        databaseBindingService.deleteServiceInstanceBinding(UNBIND_REQUEST1);

        assertNotConnectable(credentials);
    }

    @Test
    public void createServiceInstanceBinding_create2bindings_bothCredentialsAreValid() throws ServiceInstanceBindingExistsException, ServiceBrokerException, ServiceInstanceExistsException {
        databaseCreationService.createServiceInstance(CREATE_REQUEST);

        ServiceInstanceBinding binding1 = databaseBindingService.createServiceInstanceBinding(BIND_REQUEST1);
        ServiceInstanceBinding binding2 = databaseBindingService.createServiceInstanceBinding(BIND_REQUEST2);

        assertThat(binding1.getId(), is(BINDING_ID1));
        assertThat(binding2.getId(), is(BINDING_ID2));
        assertConnectable(binding1.getCredentials());
        assertConnectable(binding2.getCredentials());
    }

    @Test
    public void deleteServiceInstanceBinding_create2bindings_unboundCredentialsAreInvalidatedBoundCredentialsRemainValid() throws ServiceInstanceBindingExistsException, ServiceBrokerException, ServiceInstanceExistsException {
        databaseCreationService.createServiceInstance(CREATE_REQUEST);
        ServiceInstanceBinding binding1 = databaseBindingService.createServiceInstanceBinding(BIND_REQUEST1);
        ServiceInstanceBinding binding2 = databaseBindingService.createServiceInstanceBinding(BIND_REQUEST2);
        assertThat(binding1.getId(), is(BINDING_ID1));
        assertThat(binding2.getId(), is(BINDING_ID2));
        assertConnectable(binding1.getCredentials());
        assertConnectable(binding2.getCredentials());

        databaseBindingService.deleteServiceInstanceBinding(UNBIND_REQUEST1);

        assertNotConnectable(binding1.getCredentials());
        assertConnectable(binding2.getCredentials());
    }

    @Test
    public void deleteServiceInstanceBinding_createSchema_schemaRemains() throws ServiceInstanceBindingExistsException, ServiceBrokerException, ServiceInstanceExistsException, SQLException {
        assumePostgresProfile(environment);
        databaseCreationService.createServiceInstance(CREATE_REQUEST);
        ServiceInstanceBinding binding1 = databaseBindingService.createServiceInstanceBinding(BIND_REQUEST1);
        ServiceInstanceBinding binding2 = databaseBindingService.createServiceInstanceBinding(BIND_REQUEST2);
        String url1 = (String) binding1.getCredentials().get("jdbcurl");
        String url2 = (String) binding2.getCredentials().get("jdbcurl");
        String schema = "fruit";
        String table = schema + ".date";
        String rowValue = "medjool";
        try (Connection connection = getConnection(url1)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("create schema " + schema);
                statement.execute("create table " + table + " (name varchar(36) not null)");
                statement.execute("insert into " + table + " values ('" + rowValue + "')");
            }
        }

        databaseBindingService.deleteServiceInstanceBinding(UNBIND_REQUEST1);

        try (Connection connection = getConnection(url2)) {
            try (Statement statement = connection.createStatement()) {
                ResultSet result = statement.executeQuery("select * from " + table);
                assertTrue(result.next());
                assertThat(result.getString(1), is(rowValue));
            }
        }
    }

    @SneakyThrows
    private Connection getConnection(String url) {
        return DriverManager.getConnection(url);
    }

}