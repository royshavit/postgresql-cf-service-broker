package org.cloudfoundry.community.servicebroker.database.controller;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.cloudfoundry.community.servicebroker.controller.CatalogController;
import org.cloudfoundry.community.servicebroker.database.repository.Consts;
import org.cloudfoundry.community.servicebroker.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Consts.H2)
public class DatabaseBrokerControllerIT {

    private static final UUID INSTANCE_ID = new UUID(1, 1);
    private static final UUID BINDING_ID = new UUID(1, 2);
    private static final String PROVISION_PATH = "/v2/service_instances/" + INSTANCE_ID;
    private static final String BIND_PATH = PROVISION_PATH + "/service_bindings/" + BINDING_ID;
    private static final Header API_VERSION_HEADER = new Header(BrokerApiVersion.DEFAULT_API_VERSION_HEADER, "2.12");

    @Value("${security.user.name}")
    private String username;

    @Value("${security.user.password}")
    private String password;

    private String serviceId;
    private String deprovisionPath;
    private String unBindPath;

    @Autowired
    private void setupServiceDefinition(Catalog catalog) {
        serviceId = catalog.getServiceDefinitions().iterator().next().getId();
        deprovisionPath = PROVISION_PATH + "?service_id=" + serviceId + "&plan_id=";
        unBindPath = BIND_PATH + "?service_id=" + serviceId + "&plan_id=";
    }

    @Autowired
    private void setPort(@LocalServerPort int port) {
        RestAssured.port = port;
    }

    @Before
    public void cleanup() {
        givenRequest().delete(unBindPath);
        givenRequest().delete(deprovisionPath);
    }

    private RequestSpecification givenRequest() {
        return given().auth().basic(username, password).header(API_VERSION_HEADER);
    }

    @Test
    public void fetchCatalog() {
        Catalog catalog = givenRequest()
                .get(CatalogController.BASE_PATH)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().as(Catalog.class);

        List<ServiceDefinition> serviceDefinitions = catalog.getServiceDefinitions();
        assertFalse(serviceDefinitions.isEmpty());
        ServiceDefinition serviceDefinition = serviceDefinitions.iterator().next();
        assertTrue(serviceDefinition.isBindable());
        assertFalse(serviceDefinition.getPlans().isEmpty());
    }

    @Test
    public void provision() {
        CreateServiceInstanceRequest createRequest = new CreateServiceInstanceRequest(serviceId, "a", "b", "c")
                .withServiceInstanceId(INSTANCE_ID.toString());

        givenRequest()
                .contentType(ContentType.JSON)
                .body(createRequest)
                .put(PROVISION_PATH)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
        ;
    }

    @Test
    public void bind() {
        provision();
        CreateServiceInstanceBindingRequest bindRequest = new CreateServiceInstanceBindingRequest("a", "b", "")
                .withServiceInstanceId(INSTANCE_ID.toString())
                .withBindingId(BINDING_ID.toString());

        givenRequest()
                .contentType(ContentType.JSON)
                .body(bindRequest)
                .put(BIND_PATH)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
        ;
    }

    @Test
    public void unBind() {
        bind();

        givenRequest()
                .delete(unBindPath)
                .then()
                .statusCode(HttpStatus.SC_OK)
        ;
    }

    @Test
    public void deprovision() {
        provision();

        givenRequest()
                .delete(deprovisionPath)
                .then()
                .statusCode(HttpStatus.SC_OK)
        ;
    }

}