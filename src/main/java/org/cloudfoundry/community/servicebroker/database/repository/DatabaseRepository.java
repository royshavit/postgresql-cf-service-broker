package org.cloudfoundry.community.servicebroker.database.repository;

import lombok.SneakyThrows;
import org.cloudfoundry.community.servicebroker.database.model.Database;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Created by taitz.
 */
public interface DatabaseRepository {
    void create(String databaseName, String owner) throws SQLException;

    void delete(String databaseName) throws SQLException;

    @SneakyThrows
    Optional<Database> findOne(String dbName);
}
