package org.cloudfoundry.community.servicebroker.database.controller;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;
import org.cloudfoundry.community.servicebroker.database.Application;
import org.cloudfoundry.community.servicebroker.database.config.CatalogConfig;
import org.cloudfoundry.community.servicebroker.database.repository.Consts;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

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

    @Value("${local.server.port}")
    protected int port;

    @Value("${security.user.password}")
    protected String password;

    protected String serviceId;

    @Value("${space.name}")
    protected String spaceName;
    
    protected String planId;

    protected final String username = "user";

    protected final String organizationGuid = "system";

    protected final String spaceGuid = "thespace";

    protected static String instanceId;

    protected static String appGuid;
    
    protected static final UUID BINDING_ID = new UUID(1, 1);

    protected final String fetchCatalogPath = "/v2/catalog";

    protected final String provisionOrRemoveInstanceBasePath = "/v2/service_instances/%s";

    protected final String createOrRemoveBindingBasePath = "/v2/service_instances/%s/service_bindings/%s";

    protected final Header apiVersionHeader = new Header("X-Broker-Api-Version", CatalogConfig.BROKER_API_VERSION);
    
    @PostConstruct
    public void init() {
        serviceId = "pg-" + spaceName;
        planId = "free-" + spaceName;
    }

    @Before
    public void setUp() throws Exception {
        RestAssured.port = port;
    }

    @BeforeClass
    public static void generateUniqueIds() {
        instanceId = UUID.randomUUID().toString();
        appGuid = UUID.randomUUID().toString();
        
    }

    @Test
    public void case1_fetchCatalogSucceedsWithCredentials() throws Exception {
        given().auth().basic(username, password).header(apiVersionHeader).when().get(fetchCatalogPath).then().statusCode(HttpStatus.SC_OK);


        // same as super code, but we need the response here
        ValidatableResponse response = given().auth().basic(username, password).header(apiVersionHeader).when().get(fetchCatalogPath).then().statusCode(HttpStatus.SC_OK);

        CatalogConfig catalogConfig = new CatalogConfig();
        ServiceDefinition serviceDefinition = catalogConfig.catalog().getServiceDefinitions().get(0);

        response.body("services[0].id", equalTo("pg-" + spaceName));
        response.body("services[0].name", equalTo("pgshared-" + spaceName));
        response.body("services[0].description", equalTo(serviceDefinition.getDescription()));
        response.body("services[0].requires", equalTo(serviceDefinition.getRequires()));
        response.body("services[0].tags", equalTo(serviceDefinition.getTags()));
        response.body("services[0].plans.id", equalTo(Collections.singletonList("free-" + spaceName)));


    }

    @Test
    public void case2_provisionInstanceSucceedsWithCredentials() throws Exception {
        String provisionInstancePath = String.format(provisionOrRemoveInstanceBasePath, instanceId);
        String request_body = "{\n" +
                "  \"service_id\":        \"" + serviceId + "\",\n" +
                "  \"plan_id\":           \"" + planId + "\",\n" +
                "  \"organization_guid\": \"" + organizationGuid + "\",\n" +
                "  \"space_guid\":        \"" + spaceGuid + "\"\n" +
                "}";

        given().auth().basic(username, password).header(apiVersionHeader).request().contentType(ContentType.JSON).body(request_body).when().put(provisionInstancePath).then().statusCode(HttpStatus.SC_CREATED);
    }

    @Test
    public void case3_createBindingSucceedsWithCredentials() throws Exception {
        String createBindingPath = String.format(createOrRemoveBindingBasePath, instanceId, BINDING_ID);
        String request_body = "{\n" +
                "  \"plan_id\":      \"" + planId + "\",\n" +
                "  \"service_id\":   \"" + serviceId + "\",\n" +
                "  \"app_guid\":     \"" + appGuid + "\"\n" +
                "}";

        given()
                .auth().basic(username, password).header(apiVersionHeader)
                .request().contentType(ContentType.JSON).body(request_body)
                .when().put(createBindingPath)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
        ;
    }

    @Test
    public void case4_removeBindingSucceedsWithCredentials() throws Exception {
        String removeBindingPath = String.format(createOrRemoveBindingBasePath, instanceId, BINDING_ID) + "?service_id=" + serviceId + "&plan_id=" + planId;
        given().auth().basic(username, password).header(apiVersionHeader).when().delete(removeBindingPath).then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void case5_removeInstanceSucceedsWithCredentials() throws Exception {
        String removeInstancePath = String.format(provisionOrRemoveInstanceBasePath, instanceId) + "?service_id=" + serviceId + "&plan_id=" + planId;
        given().auth().basic(username, password).header(apiVersionHeader).when().delete(removeInstancePath).then().statusCode(HttpStatus.SC_OK);
    }
}