package org.cloudfoundry.community.servicebroker.database.jdbc;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class QueryExecutor {

    private final Supplier<Connection> connectionSupplier;

    @Autowired
    public QueryExecutor(DataSource dataSource) {
        connectionSupplier = () -> getConnection(dataSource);
    }

    public QueryExecutor(String url) {
        connectionSupplier = () -> getConnection(url);
    }

    @SneakyThrows
    private Connection getConnection(DataSource dataSource) {
        return dataSource.getConnection();
    }

    @SneakyThrows
    private Connection getConnection(String url) {
        return DriverManager.getConnection(url);
    }

    @SneakyThrows
    public void update(String query) {
        try (Connection connection = connectionSupplier.get()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }
        }
    }

    @SneakyThrows
    public List<Map<String, String>> select(String query) {
        try (Connection connection = connectionSupplier.get()) {
            try (Statement statement = connection.createStatement()) {
                ResultSet result = statement.executeQuery(query);
                return toRows(result);
            }
        }
    }

    @SneakyThrows
    private List<Map<String, String>> toRows(ResultSet result) {
        ResultSetMetaData metaData = result.getMetaData();
        int columns = metaData.getColumnCount();
        List<Map<String, String>> rows = new ArrayList<>();
        while (result.next()) {
            Map<String, String> row = new HashMap<>(columns);
            for (int i = 1; i <= columns; i++) {
                row.put(metaData.getColumnName(i), result.getString(i));
            }
            rows.add(row);
        }
        return rows;
    }

}
