package org.cloudfoundry.community.servicebroker.database.jdbc;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public List<Map<String, String>> executeSelect(String query) {
        try (Connection connection = dataSource.getConnection()) {
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
