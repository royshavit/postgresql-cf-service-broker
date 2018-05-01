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
    UrlGenerator urlGenerator;
    
    public String getUrl(String password) {
        return urlGenerator.getUrl(host, port, name, owner, password);
    }

    
    public interface UrlGenerator {
        String getUrl(String host, int port, String name, String owner, String password);
    }

}
