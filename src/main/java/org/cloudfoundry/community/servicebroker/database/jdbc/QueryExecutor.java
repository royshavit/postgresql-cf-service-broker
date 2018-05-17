package org.cloudfoundry.community.servicebroker.database.jdbc;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Slf4j
@Component
@AllArgsConstructor
public class QueryExecutor {

    private final DataSource dataSource;


    @SneakyThrows
    public void executeUpdate(String query) {
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }
        }
    }

    @SneakyThrows
    public Map<String, String> executeSelect(String query) {
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                ResultSet result = statement.executeQuery(query);
                return getResultMapFromResultSet(result);
            }
        }
    }

    @SneakyThrows
    public List<Map<String, String>> executeSelectAll(String query) {
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                ResultSet result = statement.executeQuery(query);
                return getResultsFromResultSet(result);
            }
        }
    }

    @SneakyThrows
    private static Map<String, String> getResultMapFromResultSet(ResultSet result) {
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
    
    @SneakyThrows
    private static List<Map<String, String>> getResultsFromResultSet(ResultSet result) {
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
    
}
