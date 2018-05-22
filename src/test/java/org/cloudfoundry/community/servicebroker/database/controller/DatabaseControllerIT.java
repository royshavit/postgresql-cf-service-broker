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
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
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

/**
 * Abstract test base class for the Service Broker V2 API.
 *
 * Usage:
 * Annotate the implementing test class with the following implementation-specific annotation:
 *
 *      @SpringApplicationConfiguration(classes = Application.class)
 *
 * If you would want to test the actual creation/deletion of resources, you might also want this annotation:
 *
 *      @FixMethodOrder(MethodSorters.NAME_ASCENDING)
 *
 * This would cause JUnit to run the methods in name-ascending order, causing the cases to run in order.
 */
@SpringApplicationConfiguration(classes = Application.class) //todo: deprecated
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles({Consts.H2})
@WebAppConfiguration
@IntegrationTest("server.port:0")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DatabaseControllerIT {

    private static final UUID INSTANCE_ID = new UUID(1, 1);
    private static final UUID BINDING_ID = new UUID(1, 2);
    private static final String INSTANCE_BASE_PATH = "/v2/service_instances/%s"; 
    private static final String BINDING_BASE_PATH = "/v2/service_instances/%s/service_bindings/%s"; 
    private static final Header API_VERSION_HEADER = new Header("X-Broker-Api-Version", CatalogConfig.BROKER_API_VERSION); //todo: use org.cloudfoundry.community.servicebroker.model.BrokerApiVersion

    private String serviceId;
    private String planId;
    private Supplier<RequestSpecification> requestSupplier;

    @Autowired
    private void setupAuthentication(
            @Value("${security.user.name}") String username,
            @Value("${security.user.password}") String password) {
        requestSupplier = () -> given().auth().basic(username, password).header(API_VERSION_HEADER); 
    }
    
    @Autowired
    private void setupSpaceParameters(@Value("${space.name}") String spaceName) { //todo: why need this?
        serviceId = "pg-" + spaceName;
        planId = "free-" + spaceName;
    }

    @Autowired
    private void setPort(@Value("${local.server.port}") int port) {
        RestAssured.port = port;
    }

    private RequestSpecification givenRequest() {
        return requestSupplier.get();
    }

    @Test
    public void case1_fetchCatalogSucceedsWithCredentials() {
        Catalog catalog = givenRequest().get(CatalogController.BASE_PATH).then().statusCode(HttpStatus.SC_OK).extract().body().as(Catalog.class); 
        List<ServiceDefinition> serviceDefinitions = catalog.getServiceDefinitions(); 
        assertFalse(serviceDefinitions.isEmpty()); 
        ServiceDefinition serviceDefinition = serviceDefinitions.iterator().next(); 
        assertTrue(serviceDefinition.isBindable());
        assertFalse(serviceDefinition.getPlans().isEmpty());
    }

    @Test
    public void case2_provisionInstanceSucceedsWithCredentials() {
        String provisionInstancePath = String.format(INSTANCE_BASE_PATH, INSTANCE_ID);
        String organizationGuid = "system";
        String spaceGuid = "thespace";
        String request_body = "{\n" +
                "  \"service_id\":        \"" + serviceId + "\",\n" +
                "  \"plan_id\":           \"" + planId + "\",\n" +
                "  \"organization_guid\": \"" + organizationGuid + "\",\n" +
                "  \"space_guid\":        \"" + spaceGuid + "\"\n" +
                "}";

        givenRequest().contentType(ContentType.JSON).body(request_body).put(provisionInstancePath).then().statusCode(HttpStatus.SC_CREATED);
    }

    @Test
    public void case3_createBindingSucceedsWithCredentials() {
        String createBindingPath = String.format(BINDING_BASE_PATH, INSTANCE_ID, BINDING_ID);
        String request_body = "{\n" +
                "  \"plan_id\":      \"" + planId + "\",\n" +
                "  \"service_id\":   \"" + serviceId + "\",\n" +
                "  \"app_guid\":     \"" + new UUID(1, 3) + "\"\n" +
                "}";

        givenRequest()
                .contentType(ContentType.JSON).body(request_body)
                .put(createBindingPath)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
        ;
    }

    @Test
    public void case4_removeBindingSucceedsWithCredentials() {
        String removeBindingPath = String.format(BINDING_BASE_PATH, INSTANCE_ID, BINDING_ID) + "?service_id=" + serviceId + "&plan_id=" + planId;
        givenRequest().delete(removeBindingPath).then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void case5_removeInstanceSucceedsWithCredentials() {
        String removeInstancePath = String.format(INSTANCE_BASE_PATH, INSTANCE_ID) + "?service_id=" + serviceId + "&plan_id=" + planId;
        givenRequest().delete(removeInstancePath).then().statusCode(HttpStatus.SC_OK);
    }

}