package org.cloudfoundry.community.servicebroker.database.service;

import lombok.SneakyThrows;
import org.cloudfoundry.community.servicebroker.database.Application;
import org.cloudfoundry.community.servicebroker.database.repository.Consts;
import org.cloudfoundry.community.servicebroker.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by taitz.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {Application.class})
//todo: need springboottest for this? perhaps test PostgresDatabaseRepository alone
@ActiveProfiles({Consts.POSTGRES})
public class DatabaseBindingServiceTest {

    @Autowired
    private
    DatabaseCreationService databaseCreationService;

    @Autowired
    private DatabaseBindingService databaseBindingService;


    @SneakyThrows
    @Test
    public void createServiceInstanceBinding() {
        String instanceId = new UUID(1, 1).toString();
        String bindingId = new UUID(1, 3).toString();
        CreateServiceInstanceRequest creationRequest
                = new CreateServiceInstanceRequest("serviceDef", "planid", "orgid", "spaceid")
                .withServiceInstanceId(instanceId);
        databaseCreationService.createServiceInstance(creationRequest);
        CreateServiceInstanceBindingRequest bindingRequest
                = new CreateServiceInstanceBindingRequest("pg", "free", new UUID(1, 2).toString())
                .withServiceInstanceId(instanceId)
                .withBindingId(bindingId);

        ServiceInstanceBinding binding = databaseBindingService.createServiceInstanceBinding(bindingRequest);

        //cleanup
        ServiceInstance instance = new ServiceInstance(creationRequest);
        databaseBindingService.deleteServiceInstanceBinding(new DeleteServiceInstanceBindingRequest(bindingId, instance, "servid", "planid"));
        databaseCreationService.deleteServiceInstance(new DeleteServiceInstanceRequest(instanceId, "serviceid", "planid"));

        //assertion todo: check this:
        Map<String, Object> credentials = binding.getCredentials();
        assertEquals(instanceId, credentials.get("database"));
        assertEquals("localhost", credentials.get("hostname"));
        assertEquals(5432, credentials.get("port"));
        assertEquals(bindingId, credentials.get("username"));
        String expectedUri = "postgresql://00000000-0000-0001-0000-000000000003:.*@localhost:5432/00000000-0000-0001-0000-000000000001";
        assertThat(credentials.get("uri").toString(), matchesPattern(expectedUri));
    }

}