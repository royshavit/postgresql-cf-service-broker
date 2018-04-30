package org.cloudfoundry.community.servicebroker.postgresql.service;

import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.postgresql.repository.DatabaseRepository;
import org.cloudfoundry.community.servicebroker.postgresql.repository.PostgreSQLDatabase;
import org.cloudfoundry.community.servicebroker.postgresql.repository.RoleRepository;
import org.junit.Test;
import org.postgresql.jdbc4.Jdbc4Connection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.hamcrest.text.MatchesPattern.*;

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
        return spy(new PostgreSQLDatabase(connection));
    }


    @SneakyThrows
    @Test
    public void createServiceInstanceBinding() {
        String masterUri = "postgres://master-user:secret@db.com:123/master-db";
        PostgreSQLDatabase postgreSQLDatabase = postgreSQLDatabase(masterUri);
        doReturn(ImmutableMap.of("", "")).when(postgreSQLDatabase).executePreparedSelect(anyString(), any());
        RoleRepository roleRepository = spy(new RoleRepository(postgreSQLDatabase));
        doNothing().when(roleRepository).setPassword(anyString(), anyString());
        PostgreSQLServiceInstanceBindingService bindingService
                = new PostgreSQLServiceInstanceBindingService(new DatabaseRepository(postgreSQLDatabase), roleRepository);
        UUID instanceId = new UUID(1, 1);
        CreateServiceInstanceBindingRequest bindingRequest
                = new CreateServiceInstanceBindingRequest("pg", "free", new UUID(1, 2).toString())
                .withServiceInstanceId(instanceId.toString())
                .withBindingId(new UUID(1, 2).toString());

        ServiceInstanceBinding binding = bindingService.createServiceInstanceBinding(bindingRequest);

        Map<String, Object> credentials = binding.getCredentials();
        assertEquals(instanceId.toString(), credentials.get("database"));
        assertEquals("db.com", credentials.get("hostname"));
        assertEquals(123, credentials.get("port"));
        assertEquals(instanceId, credentials.get("username"));
        String expectedUri = "postgres://00000000-0000-0001-0000-000000000001:.*@db.com:123/00000000-0000-0001-0000-000000000001";
        assertThat(credentials.get("uri").toString(), matchesPattern(expectedUri));
    }

}