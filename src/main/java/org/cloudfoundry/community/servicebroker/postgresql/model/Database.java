package org.cloudfoundry.community.servicebroker.postgresql.model;

import lombok.Value;

import java.util.UUID;

/**
 * Created by taitz.
 */
@Value
public class Database {
    private final String host;
    private final int port;
    private final UUID name;

}
