package org.cloudfoundry.community.servicebroker.postgresql.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.cloudfoundry.community.servicebroker.postgresql.model.Database;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;

/**
 * Created by taitz.
 */
@AllArgsConstructor
@Configuration
public class DataSourceConfig {


    @SneakyThrows
    @Bean
    public HikariDataSource dataSource(@Value("${MASTER_JDBC_URL}") String jdbcUrl) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        return new HikariDataSource(hikariConfig);
    }

    @SneakyThrows
    @Bean
    Database masterDatabaseUrl(HikariDataSource dataSource) {
        URI uri = new URI(new URI(dataSource.getJdbcUrl()).getSchemeSpecificPart());
        int port = uri.getPort();
        String host = uri.getHost();
        String databaseName = new URI(new URI(dataSource.getJdbcUrl()).getSchemeSpecificPart()).getPath().replaceFirst("/", "");
        String user;
        try (Connection connection = dataSource.getConnection()) {
            user = connection.getMetaData().getUserName();
        }
        return new Database(host, port, databaseName, user);
    }

    @SneakyThrows
    @Autowired
    public void createServiceInstanceTable(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            String createServiceInstanceTable
                    = "CREATE TABLE IF NOT EXISTS service (serviceinstanceid varchar(200) not null default '',"
                    + " servicedefinitionid varchar(200) not null default '',"
                    + " planid varchar(200) not null default '',"
                    + " organizationguid varchar(200) not null default '',"
                    + " spaceguid varchar(200) not null default '')";
            connection.createStatement().execute(createServiceInstanceTable);
        }
    }

}
