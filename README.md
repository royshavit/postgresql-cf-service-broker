Cloud Foundry service broker for PostgreSQL
===========================================
A Cloud Foundry service broker for provisioning PostgreSQL databases, forked from [postgresql-cf-service-broker
](https://github.com/cloudfoundry-community/postgresql-cf-service-broker).

Overview
--------
1. The broker is provided with credentials to a Postgres server.  
1. A service provisioning call will create a database within that Postgres server.  
1. Each binding call will return unique credentials to the provisioned database.  
1. An unbinding call will revoke those credentials.  
1. A deprovisioning call will drop the database.

Versioning
----------
Compatibility with the Cloud Foundry service broker API is indicated by the project version number. For example, version 2.4.0 is based off the 2.4 version of the broker API.

How to build
------------
```
mvn clean install
```

How to run locally
------------------
Provide a PostgreSQL jdbc url and run the JAR file:
```
spring_datasource_url=<jdbc-url> java -jar target/postgresql-cf-service-broker-2.4.0-SNAPSHOT.jar
```

How to run in Cloud Foundry
---------------------------
1. Push the application:
   ```
   cf push postgres-broker --no-start
   ```
1. Provide the jdbc url of the Postgres server:
   ```
   cf set-env postgres-broker spring_datasource_url jdbc:postgresql://<hostname>:<port>/<database-name>?user=<user-name>&password=<password>
   ```
1. Provide the name of the Cloud Foundry space to which the broker has been pushed. This is used to determine catalog service and plan identifiers. It prevents naming conflicts when the broker is deployed to multiple spaces.
   ```
   cf set-env postgres-broker space_name <write-your-space-name-here>
   ```
1. Optional - grant elevated privileges to applications that will bind to this service, for instance to allow an application to create a Postgres extension:
   ```
   cf set-env postgres-broker database_privileges_elevated true
   ```
1. Optional - limit the number of open connections to each database, e.g. to a maximum of 25 connections:
   ```
   cf set-env postgres-broker database_connections_max 25
   ```
1. Optional - override the default broker credentials (username "user", password "password"):
   ```
   cf set-env postgres-broker security.user.name <choose-a-user-name>
   cf set-env postgres-broker security.user.password <choose-a-password>
   ```
1. Start the service broker:
   ```
   cf start postgres-broker
   ```
1. Either - register the service broker in the current space only:
   ```
   cf create-service-broker postgres-broker <broker-username> <broker-password> https://<broker-url> --space-scoped
   ```
   Or - register the service broker in the entire Cloud Foundry Marketplace:
   ```
   cf create-service-broker postgres-broker <broker-username> <broker-password> https://<broker-url>
   cf enable-service-access pgshared-<space-name> -p free
   ```

How to run tests
----------------
To run all tests:
```
mvn test
```
By default, tests will run with an H2 in-memory database.  
To run a test with a Postgres database, replace `@ActiveProfiles(Consts.H2)` with 
`@ActiveProfiles(Consts.POSTGRES)` in the line above the test class definition and ensure that the spring.datasource.url defined in `application.yml` matches the url of the Postgres database.

Usage
-----
1.  Create a Postgres instance:
    ```
    cf create-service pgshared-<space-name> free pg1
    ```
1.  Bind an application to the Postgres instance:
    ```
    cf bind-service app1 pg1
    ```
    Alternatively, create a service key:
    ```
    cf create-service-key pg1 key1
    ```
1.  The credentials returned in a bind call have the following format:
    ```
    "credentials": {
         "database": "database",
         "hostname": "hostname",
         "jdbcurl": "jdbc:postgresql://hostname:port/database?user=username&password=password",
         "password": "password",
         "port": port,
         "uri": "postgresql://username:password@hostname:port/database",
         "username": "username"
    }
    ```

Modifications since fork from parent project
--------------------------------------------
A list of key modifications:
1. Each bind call returns unique credentials (whereas previously a bind call would revoke the password of a previous bind call).
1. A bind call returns more detailed credentials.
1. Configurable elevated permissions to allow, for instance `CREATE EXTENSION btree_gist`
1. Configurable maximum connection limit.
1. Supports H2 inmemory database and is extendable to any database.
1. Extensive testing, error handling and logging.
1. Connection pooling.
1. Space dependent catalog.
1. Flyway for handling tenancy schema.
