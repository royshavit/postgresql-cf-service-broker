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
package org.cloudfoundry.community.servicebroker.database;

import org.cloudfoundry.community.servicebroker.config.ServiceBrokerAutoConfiguration;
import org.cloudfoundry.community.servicebroker.controller.CatalogController;
import org.cloudfoundry.community.servicebroker.controller.ServiceInstanceBindingController;
import org.cloudfoundry.community.servicebroker.controller.ServiceInstanceController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.security.SecureRandom;
import java.util.Random;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = ServiceBrokerAutoConfiguration.class)
@ComponentScan(
        basePackages = "org.cloudfoundry.community.servicebroker",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
                        ServiceBrokerAutoConfiguration.class,
                        CatalogController.class,
                        ServiceInstanceBindingController.class,
                        ServiceInstanceController.class,
                })
        }
)
@ComponentScan(basePackageClasses = Application.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    Random random() {
        return new SecureRandom();
    }

}