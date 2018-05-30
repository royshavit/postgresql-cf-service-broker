package org.cloudfoundry.community.servicebroker.database.service;

import org.cloudfoundry.community.servicebroker.database.jdbc.QueryExecutor;
import org.cloudfoundry.community.servicebroker.database.repository.Consts;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assume.assumeThat;

/**
 * Created by taitz.
 */
@Configuration
@Import({
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class,
})
@ComponentScan(
        basePackageClasses = {
                QueryExecutor.class,
                Consts.class,
                DatabaseBindingService.class,
        }
)
class BrokerTestConfig {

    @Bean
    Random random() {
        return new SecureRandom();
    }

    static void assumePostgresProfile(Environment environment) {
        assumeThat(Arrays.asList(environment.getActiveProfiles()), hasItem(Consts.POSTGRES));
    }

}
