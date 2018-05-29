# Cloud Foundry Service Broker for a PostgreSQL instance

A Cloud Foundry Service Broker for a PostgreSQL instance forked from [postgresql-cf-service-broker
](https://github.com/cloudfoundry-community/postgresql-cf-service-broker).

The broker currently publishes a single service and plan for provisioning PostgreSQL databases.

## Design 

The broker uses a PostgresSQL table for it's meta data. It does not maintain an internal database so it has no dependencies besides PostgreSQL.

Capability with the Cloud Foundry service broker API is indicated by the project version number. For example, version 2.4.0 is based off the 2.4 version of the broker API.

The broker connects to a provided Postgres server.  
For each service instance provisioned, the broker will create a database within that Postgres server.  
Each application that binds to a given service instance, will receive its own credentials to that service instance's database.


## How to build
```
mvn clean install
```

## How to run locally
Provide a PostgreSQL jdbc url and run the JAR file:
```
spring_datasource_url=<jdbc-url> java -jar target/postgresql-cf-service-broker-2.4.0-SNAPSHOT.jar
```

## How to run in Cloud Foundry
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
   cf set-env postgres-broker grant_users_elevated_privileges true
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
1. Or - register the service broker in the entire Cloud Foundry Marketplace:
   ```
   cf create-service-broker postgres-broker <broker-username> <broker-password> https://<broker-url>
   cf enable-service-access pgshared-<space-name> -p free
   ```

## Testing
To run all tests:
```
mvn test
```
By default, tests will run with an H2 in-memory database.  
To run a test with a Postgres database, replace `@ActiveProfiles(Consts.H2)` with 
`@ActiveProfiles(Consts.POSTGRES)` above the test class definition and ensure that the spring.datasource.url defined in `application.yml` matches the url of the Postgres database.

## Using the services in your application

### Format of Credentials

The credentials provided in a bind call have the following format:

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

## Broker Security

[spring-boot-starter-security](https://github.com/spring-projects/spring-boot/tree/master/spring-boot-starters/spring-boot-starter-security) is used. See the documentation here for configuration: [Spring boot security](http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#boot-features-security)

The default password configured is "password"

## Creation of PostgreSQL databases

A service provisioning call will create a PostgreSQL database. A binding call will return a database uri that can be used to connect to the database. Unbinding calls will disable the database user role and deprovisioning calls will delete all resources created.

## User for Broker

An PostgreSQL user must be created for the broker. The username and password must be provided using the environment variable `MASTER_JDBC_URL`.

## Registering a Broker with the Cloud Controller

See [Managing Service Brokers](http://docs.cloudfoundry.org/services/managing-service-brokers.html).

