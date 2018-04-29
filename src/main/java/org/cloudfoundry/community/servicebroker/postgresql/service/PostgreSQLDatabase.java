package org.cloudfoundry.community.servicebroker.postgresql.service;

import org.postgresql.jdbc4.Jdbc4Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@Component
public class PostgreSQLDatabase {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLDatabase.class);

    private final Connection conn; 
    private final String databaseHost; 
    private final int databasePort; 
    private final String username;
    

    @Autowired
    public PostgreSQLDatabase(Connection conn) {
        this.conn = conn;

        try {
            String jdbcUrl = conn.getMetaData().getURL();
            // Remove "jdbc:" prefix from the connection JDBC URL to create an URI out of it.
            String cleanJdbcUrl = jdbcUrl.replace("jdbc:", "");

            URI uri = new URI(cleanJdbcUrl);
            databaseHost = uri.getHost();
            databasePort = uri.getPort() == -1 ? 5432 : uri.getPort();
            username = ((Jdbc4Connection) conn).getUserName();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to get DatabaseMetadata from Connection", e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to parse JDBC URI for Database Connection", e);
        }
    }


    public void executeUpdate(String query) throws SQLException {
        Statement statement = conn.createStatement();

        try {
            statement.execute(query);
        } finally {
            statement.close();
        }
    }

    public Map<String, String> executeSelect(String query) throws SQLException {
        Statement statement = conn.createStatement();

        try {
            ResultSet result = statement.executeQuery(query);
            return getResultMapFromResultSet(result);
        } finally {
            statement.close();
        }
    }

    public void executePreparedUpdate(String query, Map<Integer, String> parameterMap) throws SQLException {
        if(parameterMap == null) {
            throw new IllegalStateException("parameterMap cannot be null");
        }

        PreparedStatement preparedStatement = conn.prepareStatement(query);

        for(Map.Entry<Integer, String> parameter : parameterMap.entrySet()) {
            preparedStatement.setString(parameter.getKey(), parameter.getValue());
        }

        try {
            preparedStatement.executeUpdate();
        } finally {
            preparedStatement.close();
        }
    }

    public Map<String, String> executePreparedSelect(String query, Map<Integer, String> parameterMap) throws SQLException {
        if(parameterMap == null) {
            throw new IllegalStateException("parameterMap cannot be null");
        }

        PreparedStatement preparedStatement = conn.prepareStatement(query);

        for(Map.Entry<Integer, String> parameter : parameterMap.entrySet()) {
            preparedStatement.setString(parameter.getKey(), parameter.getValue());
        }

        try {
            ResultSet result = preparedStatement.executeQuery();
            return getResultMapFromResultSet(result);
        } finally {
            preparedStatement.close();
        }
    }

    public String getDatabaseHost() {
        return databaseHost;
    }

    public int getDatabasePort() {
        return databasePort;
    }

    public String getUsername() {
        return username;
    }

    private static Map<String, String> getResultMapFromResultSet(ResultSet result) throws SQLException {
        ResultSetMetaData resultMetaData = result.getMetaData();
        int columns = resultMetaData.getColumnCount();

        Map<String, String> resultMap = new HashMap<String, String>(columns);

        if(result.next()) {
            for(int i = 1; i <= columns; i++) {
                resultMap.put(resultMetaData.getColumnName(i), result.getString(i));
            }
        }

        return resultMap;
    }
}
