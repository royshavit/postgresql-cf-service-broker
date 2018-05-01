package org.cloudfoundry.community.servicebroker.database.model;

import lombok.Value;

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
