package org.cloudfoundry.community.servicebroker.database.repository;

import java.util.Map;

/**
 * Created by taitz.
 */
public interface DatabaseRepository {

    void createDatabase(String databaseName, int databaseConnectionsMax);

    void deleteDatabase(String databaseName);

    Map<String, Object> createUser(String databaseName, String username, String password);

    void deleteUser(String databaseName, String username);
    
    boolean userExists(String databaseName, String username);

}
