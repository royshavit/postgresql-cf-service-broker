package org.cloudfoundry.community.servicebroker.database;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.ExtractableResponse;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ResponseBodyExtractionOptions;
import com.jayway.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;
import org.cloudfoundry.community.servicebroker.ServiceBrokerV2ITBase;
import org.cloudfoundry.community.servicebroker.database.config.CatalogConfig;
import org.cloudfoundry.community.servicebroker.database.jdbc.QueryExecutor;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SpringApplicationConfiguration(classes = Application.class) //todo: deprecated
public class PostgreSQLServiceBrokerV2IT extends ServiceBrokerV2ITBase {

    @Autowired
    private QueryExecutor queryExecutor;

    @Autowired
    private DataSource dataSource;


    /**
     * cf marketplace
     * cf create-service-broker
     * <p>
     * Fetch Catalog (GET /v2/catalog)
     */
    @Override
    @Test
    public void case1_fetchCatalogSucceedsWithCredentials() throws Exception {
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

    /**
     * cf create-service
     * <p>
     * Provision Instance (PUT /v2/service_instances/:id)
     */

    @Override
    @Test
    public void case2_provisionInstanceSucceedsWithCredentials() throws Exception {
        super.case2_provisionInstanceSucceedsWithCredentials();
        //todo: check deletion when does / does not exist
    }

    /**
     * cf bind-service
     * <p>
     * Create Binding (PUT /v2/service_instances/:instance_id/service_bindings/:id)
     */

    @Override
    @Test
    public void case3_createBindingSucceedsWithCredentials() throws Exception {

        // same as super code, but we need the response here
        String createBindingPath = String.format(createOrRemoveBindingBasePath, instanceId, BINDING_ID);
        String request_body = "{\n" +
                "  \"plan_id\":      \"" + planId + "\",\n" +
                "  \"service_id\":   \"" + serviceId + "\",\n" +
                "  \"app_guid\":     \"" + appGuid + "\"\n" +
                "}";

        ValidatableResponse response = given().auth().basic(username, password).header(apiVersionHeader).request().contentType(ContentType.JSON).body(request_body).when().put(createBindingPath).then().statusCode(HttpStatus.SC_CREATED);

        ExtractableResponse<Response> response1 = response.extract();
        ResponseBodyExtractionOptions body = response1.body();
        String url = body.jsonPath().getString("credentials.jdbcurl");
        String user = body.jsonPath().getString("credentials.username");
        String password = body.jsonPath().getString("credentials.password");
        
        testConnection(url, user, password);
    }

    /**
     * cf unbind-service
     * <p>
     * Remove Binding (DELETE /v2/service_instances/:instance_id/service_bindings/:id)
     */

    @Override
    @Test
    public void case4_removeBindingSucceedsWithCredentials() throws Exception {
        // same as super code, but we need the response here
        String removeBindingPath = String.format(createOrRemoveBindingBasePath, instanceId, BINDING_ID) + "?service_id=" + serviceId + "&plan_id=" + planId;
        ValidatableResponse response = given().auth().basic(username, password).header(apiVersionHeader).when().delete(removeBindingPath).then().statusCode(HttpStatus.SC_OK);

        // response body is empty json
        response.body(equalTo("{}"));
    }

    /**
     * cf delete-service
     * <p>
     * Remove Instance (DELETE /v2/service_instances/:id)
     */

    @Override
    @Test
    public void case5_removeInstanceSucceedsWithCredentials() throws Exception {
        super.case5_removeInstanceSucceedsWithCredentials();
        
        //todo
//        assertFalse(checkDatabaseExists(instanceId));
//        assertFalse(checkRoleExists(instanceId));
//        assertFalse(checkRoleIsDatabaseOwner(instanceId, instanceId));

        List<Map<String, String>> serviceResult = queryExecutor.executeSelect(
                "SELECT * FROM service WHERE serviceinstanceid = '" + instanceId + "'"
        );
        assertTrue(serviceResult.isEmpty());
    }

    private boolean checkTableExists(String tableName) throws SQLException { //todo: delete
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData md = connection.getMetaData();
            ResultSet schemas = md.getSchemas();
            while (schemas.next()) {
                String value = schemas.getString(1);
                System.out.println(value);
            }
            ResultSet tables = md.getTables(null, null, null, null);
            ResultSet resultSet = connection.createStatement().executeQuery("select * from service");
            if (resultSet.next()) {
                String string = resultSet.getString(1);
                System.out.println(string);
            }

            while (tables.next()) {
                String table = String.format("catalog   %s  schema  %s  table   %s  type    %s",
                        tables.getString(1),
                        tables.getString(2),
                        tables.getString(3),
                        tables.getString(4)
                );
                System.out.println(table);
            }

            ResultSet rs = md.getTables(null, null, tableName, null);
            // ResultSet.last() followed by ResultSet.getRow() will give you the row count
            rs.last();
            int rowCount = rs.getRow();
            return rowCount == 1;
        }
    }

    private void testConnection(String url, String user, String password) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            try (Statement statement = connection.createStatement()) {
                ResultSet result = statement.executeQuery("select 1");
                List<Map<String, String>> resultMap = getResultsFromResultSet(result);
                assertThat(resultMap.size(), is(1));
                System.out.println(resultMap);
            }
        }
    }

    private static List<Map<String, String>> getResultsFromResultSet(ResultSet result) throws SQLException { //todo: reuse
        ResultSetMetaData resultMetaData = result.getMetaData();
        int columns = resultMetaData.getColumnCount();
        List<Map<String, String>> results = new ArrayList<>();
        while (result.next()) {
            Map<String, String> resultMap = new HashMap<>(columns);
            for (int i = 1; i <= columns; i++) {
                resultMap.put(resultMetaData.getColumnName(i), result.getString(i));
            }
            results.add(resultMap);
        }
        return results;
    }

    private void testWrongPassword(String databaseName, String owner) throws SQLException { //todo
        String url;
        url = String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;USER=%s;PASSWORD=%s", databaseName, owner, UUID.randomUUID());
        try (Connection connection = DriverManager.getConnection(url)) {
            try (Statement statement = connection.createStatement()) {
                ResultSet result = statement.executeQuery("select 1");
                Map<String, String> resultMap = getResultMapFromResultSet(result);
                System.out.println(resultMap);
            }
        }
    }

    private static Map<String, String> getResultMapFromResultSet(ResultSet result) throws SQLException {
        ResultSetMetaData resultMetaData = result.getMetaData();
        int columns = resultMetaData.getColumnCount();
        Map<String, String> resultMap = new HashMap<>(columns);
        if (result.next()) {
            for (int i = 1; i <= columns; i++) {
                resultMap.put(resultMetaData.getColumnName(i), result.getString(i));
            }
        }
        return resultMap;
    }

}