package org.cloudfoundry.community.servicebroker.database.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.cloudfoundry.community.servicebroker.database.model.Database;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        String databaseName = uri.getPath().replaceFirst("/", "");
        String user;
        try (Connection connection = dataSource.getConnection()) {
            user = connection.getMetaData().getUserName();
        }
        return new Database(host, port, databaseName, user,
                (host1, port1, name, owner, password) -> uri.toString()
        );
    }

}
