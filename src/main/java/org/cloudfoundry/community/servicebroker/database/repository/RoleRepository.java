package org.cloudfoundry.community.servicebroker.database.repository;

import java.sql.SQLException;

/**
 * Created by taitz.
 */
public interface RoleRepository {

    void create(String roleName) throws SQLException;

    void delete(String roleName) throws SQLException;

    void setPassword(String roleName, String password) throws SQLException;

    void unsetPassword(String roleName) throws SQLException;

    void grantRoleTo(String roleMember, String roleGroup) throws SQLException;

}
