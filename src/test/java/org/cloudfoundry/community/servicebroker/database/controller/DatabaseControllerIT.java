package org.cloudfoundry.community.servicebroker.database.controller;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.cloudfoundry.community.servicebroker.controller.CatalogController;
import org.cloudfoundry.community.servicebroker.database.Application;
import org.cloudfoundry.community.servicebroker.database.config.CatalogConfig;
import org.cloudfoundry.community.servicebroker.database.repository.Consts;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SpringApplicationConfiguration(classes = Application.class) //todo: deprecated
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles({Consts.H2})
@WebAppConfiguration
@IntegrationTest("server.port:0")
public class DatabaseControllerIT {

    private static final UUID INSTANCE_ID = new UUID(1, 1);
    private static final UUID BINDING_ID = new UUID(1, 2);
    private static final CreateServiceInstanceBindingRequest BIND_REQUEST
            = new CreateServiceInstanceBindingRequest("a", "b", "")
            .withServiceInstanceId(INSTANCE_ID.toString())
            .withBindingId(BINDING_ID.toString());
    private static final String INSTANCE_BASE_PATH = "/v2/service_instances/%s";
    private static final String BINDING_BASE_PATH = "/v2/service_instances/%s/service_bindings/%s";
    private static final String PROVISION_INSTANCE_PATH;
    private static final String CREATE_BINDING_PATH;
    private static final Header API_VERSION_HEADER = new Header("X-Broker-Api-Version", CatalogConfig.BROKER_API_VERSION); //todo: use org.cloudfoundry.community.servicebroker.model.BrokerApiVersion

    static {
        PROVISION_INSTANCE_PATH = String.format(INSTANCE_BASE_PATH, INSTANCE_ID);
        CREATE_BINDING_PATH = String.format(BINDING_BASE_PATH, INSTANCE_ID, BINDING_ID);
    }

    private CreateServiceInstanceRequest createRequest;
    private Supplier<RequestSpecification> requestSupplier;
    private String removeBindingPath;
    private String removeInstancePath;

    @Autowired
    private void setupAuthentication(
            @Value("${security.user.name}") String username,
            @Value("${security.user.password}") String password) {
        requestSupplier = () -> given().auth().basic(username, password).header(API_VERSION_HEADER);
    }

    @Autowired
    private void setupServiceDefinition(@Value("${space.name}") String spaceName) {
        String serviceId = "pg-" + spaceName;
        createRequest = new CreateServiceInstanceRequest(serviceId, "a", "b", "c").withServiceInstanceId(INSTANCE_ID.toString());
        removeInstancePath = PROVISION_INSTANCE_PATH + "?service_id=" + serviceId + "&plan_id=";
        removeBindingPath = CREATE_BINDING_PATH + "?service_id=" + serviceId + "&plan_id=";
    }

    @Autowired
    private void setPort(@Value("${local.server.port}") int port) {
        RestAssured.port = port;
    }

    @Before
    public void cleanup() {
        givenRequest().delete(removeBindingPath);
        givenRequest().delete(removeInstancePath);
    }

    private RequestSpecification givenRequest() {
        return requestSupplier.get();
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
        givenRequest()
                .contentType(ContentType.JSON)
                .body(createRequest)
                .put(PROVISION_INSTANCE_PATH)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
        ;
    }

    @Test
    public void bind() {
        provision();

        givenRequest()
                .contentType(ContentType.JSON)
                .body(BIND_REQUEST)
                .put(CREATE_BINDING_PATH)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
        ;
    }

    @Test
    public void unBind() {
        bind();

        givenRequest()
                .delete(removeBindingPath)
                .then()
                .statusCode(HttpStatus.SC_OK)
        ;
    }

    @Test
    public void deprovision() {
        provision();

        givenRequest()
                .delete(removeInstancePath)
                .then()
                .statusCode(HttpStatus.SC_OK)
        ;
    }

}