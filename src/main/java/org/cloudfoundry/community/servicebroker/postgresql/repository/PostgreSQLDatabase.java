package org.cloudfoundry.community.servicebroker.postgresql.repository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Slf4j
@Component
@AllArgsConstructor
public class PostgreSQLDatabase {

    private final DataSource dataSource;


    public void executeUpdate(String query) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }
        }
    }

    public Map<String, String> executeSelect(String query) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                ResultSet result = statement.executeQuery(query);
                return getResultMapFromResultSet(result);
            }
        }
    }

    public void executePreparedUpdate(String query, Map<Integer, String> parameterMap) throws SQLException {
        requireNonNull(parameterMap);
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                for (Map.Entry<Integer, String> parameter : parameterMap.entrySet()) {
                    preparedStatement.setString(parameter.getKey(), parameter.getValue());
                }
                preparedStatement.executeUpdate();
            }
        }
    }

    public Map<String, String> executePreparedSelect(String query, Map<Integer, String> parameterMap) throws SQLException {
        requireNonNull(parameterMap);
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                for (Map.Entry<Integer, String> parameter : parameterMap.entrySet()) {
                    preparedStatement.setString(parameter.getKey(), parameter.getValue());
                }
                ResultSet result = preparedStatement.executeQuery();
                return getResultMapFromResultSet(result);
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
