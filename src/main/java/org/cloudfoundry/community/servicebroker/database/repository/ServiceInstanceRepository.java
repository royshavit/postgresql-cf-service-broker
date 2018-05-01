package org.cloudfoundry.community.servicebroker.database.repository;

import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by taitz.
 */
public interface ServiceInstanceRepository {

    void save(CreateServiceInstanceRequest createServiceInstanceRequest) throws SQLException;

    void delete(UUID instanceId) throws SQLException;

    Optional<ServiceInstance> findServiceInstance(UUID instanceId) throws SQLException;

}
