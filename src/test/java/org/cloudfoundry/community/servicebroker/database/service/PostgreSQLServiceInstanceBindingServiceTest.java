package org.cloudfoundry.community.servicebroker.database.service;

import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.database.jdbc.QueryExecutor;
import org.cloudfoundry.community.servicebroker.database.model.Database;
import org.cloudfoundry.community.servicebroker.database.repository.DatabaseRepository;
import org.cloudfoundry.community.servicebroker.database.repository.RoleRepository;
import org.junit.Test;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by taitz.
 */
public class PostgreSQLServiceInstanceBindingServiceTest {

    @SneakyThrows
    private QueryExecutor queryExecutor() {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        Connection connection = mock(Connection.class);
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.createStatement()).thenReturn(mock(Statement.class));
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        return spy(new QueryExecutor(dataSource));
    }


    @SneakyThrows
    @Test
    public void createServiceInstanceBinding() {
        UUID instanceId = new UUID(1, 1);
        String owner = instanceId.toString();
        QueryExecutor queryExecutor = queryExecutor();
        doReturn(ImmutableMap.of("", "")).when(queryExecutor).executePreparedSelect(anyString(), any());
        doReturn(ImmutableMap.of("owner", owner)).when(queryExecutor).executePreparedSelect(anyString(), any());
        RoleRepository roleRepository = new RoleRepository(queryExecutor);
        String hostName = "db.com";
        int port = 123;
        PostgreSQLServiceInstanceBindingService bindingService
                = new PostgreSQLServiceInstanceBindingService(
                new DatabaseRepository(queryExecutor, new Database(hostName, port, "master-db", "master-user",
                        (host, port1, name, owner1, password) -> "postgres://master-user:secret@db.com:123/master-db"
                )),
                roleRepository,
                new SecureRandom());
        CreateServiceInstanceBindingRequest bindingRequest
                = new CreateServiceInstanceBindingRequest("pg", "free", new UUID(1, 2).toString())
                .withServiceInstanceId(instanceId.toString())
                .withBindingId(new UUID(1, 3).toString());

        ServiceInstanceBinding binding = bindingService.createServiceInstanceBinding(bindingRequest);

        Map<String, Object> credentials = binding.getCredentials();
        assertEquals(instanceId.toString(), credentials.get("database"));
        assertEquals(hostName, credentials.get("hostname"));
        assertEquals(port, credentials.get("port"));
        assertEquals(owner, credentials.get("username"));
        String expectedUri = "postgres://00000000-0000-0001-0000-000000000001:.*@db.com:123/00000000-0000-0001-0000-000000000001";
        assertThat(credentials.get("uri").toString(), matchesPattern(expectedUri));
    }

}