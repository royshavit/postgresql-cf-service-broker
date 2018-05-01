package org.cloudfoundry.community.servicebroker.database.repository;

import org.cloudfoundry.community.servicebroker.database.model.Database;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Created by taitz.
 */
public interface DatabaseRepository {
    
    void create(String databaseName, String owner) throws SQLException;

    void delete(String databaseName) throws SQLException;

    Optional<Database> findOne(String dbName);

    String toUrl(String host, int port, String databaseName, String user, String password);

}
