/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cloudfoundry.community.servicebroker.postgresql.config;

import org.cloudfoundry.community.servicebroker.model.BrokerApiVersion;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.*;

@Configuration
public class CatalogConfig {

    public static final String BROKER_API_VERSION = "2.12";

    @Value("${space.name}")
    private String spaceName;

    @Bean
    public Catalog catalog() throws IOException {
        ServiceDefinition serviceDefinition = new ServiceDefinition(
                "pg-" + spaceName,
                "pgshared-" + spaceName,
                "PostgreSQL on shared instance.",
                true, false, getPlans(), getTags(), getServiceDefinitionMetadata(), Collections.singletonList("syslog_drain"), null);
        return new Catalog(Collections.singletonList(serviceDefinition));
    }

    private static List<String> getTags() {
        return Arrays.asList("PostgreSQL", "Database storage");
    }

    private static Map<String, Object> getServiceDefinitionMetadata() {
        Map<String, Object> sdMetadata = new HashMap<>();
        sdMetadata.put("displayName", "PostgreSQL");
        sdMetadata.put("imageUrl", "https://wiki.postgresql.org/images/3/30/PostgreSQL_logo.3colors.120x120.png");
        sdMetadata.put("longDescription", "PostgreSQL Service");
        sdMetadata.put("providerDisplayName", "PostgreSQL");
        sdMetadata.put("documentationUrl", "http://mendix.com/postgresql");
        sdMetadata.put("supportUrl", "https://support.mendix.com");
        return sdMetadata;
    }

    private List<Plan> getPlans() {
        Plan basic = new Plan("free-" + spaceName, "free",
                "A PG plan providing a single database on a shared instance with limited storage.",
                getBasicPlanMetadata(), true);
        return Collections.singletonList(basic);
    }

    private static Map<String, Object> getBasicPlanMetadata() {
        Map<String, Object> planMetadata = new HashMap<>();
        planMetadata.put("bullets", getBasicPlanBullets());
        return planMetadata;
    }

    private static List<String> getBasicPlanBullets() {
        return Arrays.asList("Single PG database", "Limited storage", "Shared instance");
    }

    @Bean
    public BrokerApiVersion brokerApiVersion() {
        return new BrokerApiVersion(BROKER_API_VERSION);
    }

}