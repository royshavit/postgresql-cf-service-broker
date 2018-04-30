package org.cloudfoundry.community.servicebroker.postgresql.model;

import lombok.Value;

import java.util.UUID;

/**
 * Created by taitz.
 */
@Value
public class Database {
    String host;
    int port;
    String name;
    String owner;
}
