package org.cloudfoundry.community.servicebroker.database.repository;

import java.util.Map;

/**
 * Created by taitz.
 */
public interface DatabaseRepository {

    void createDatabase(String databaseName);

    void deleteDatabase(String databaseName);

//    Optional<Database> findDatabase(String databaseName); //todo: what about this?

    Map<String, Object> createUser(String databaseName, String username, String password, boolean elevatedPrivileges);
    
    void deleteUser(String databaseName, String username);

}
