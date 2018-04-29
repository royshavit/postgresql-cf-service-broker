package org.cloudfoundry.community.servicebroker.postgresql.service;

import lombok.SneakyThrows;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.junit.Test;
import org.postgresql.jdbc4.Jdbc4Connection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by taitz.
 */
public class PostgreSQLServiceInstanceBindingServiceTest {

    @SneakyThrows
    private PostgreSQLDatabase postgreSQLDatabase(String uri) {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getURL()).thenReturn(uri);
        Connection connection = mock(Jdbc4Connection.class);
        when(connection.getMetaData()).thenReturn(metaData);
        return new PostgreSQLDatabase(connection);
    }


    @SneakyThrows
    @Test
    public void createServiceInstanceBinding() {
        String uri = "postgres://00000000-0000-0001-0000-000000000001:secret@db.com:123/00000000-0000-0001-0000-000000000001";
        PostgreSQLDatabase postgreSQLDatabase = postgreSQLDatabase(uri);
        Role role = spy(new Role(postgreSQLDatabase));
        String password = "secret";
        doReturn(password).when(role).bindRoleToDatabase(anyString());
        PostgreSQLServiceInstanceBindingService bindingService
                = new PostgreSQLServiceInstanceBindingService(new Database(postgreSQLDatabase), role);
        String instanceId = new UUID(1, 1).toString();
        CreateServiceInstanceBindingRequest bindingRequest
                = new CreateServiceInstanceBindingRequest("pg", "free", "appguid")
                .withServiceInstanceId(instanceId)
                .withBindingId(new UUID(1, 2).toString());

        ServiceInstanceBinding binding = bindingService.createServiceInstanceBinding(bindingRequest);

        Map<String, Object> credentials = binding.getCredentials();
        assertEquals(uri, credentials.get("uri"));
        assertEquals(instanceId, credentials.get("database"));
        assertEquals("db.com", credentials.get("hostname"));
        assertEquals(password, credentials.get("password"));
        assertEquals(123, credentials.get("port"));
        assertEquals(instanceId, credentials.get("username"));
    }
    
}