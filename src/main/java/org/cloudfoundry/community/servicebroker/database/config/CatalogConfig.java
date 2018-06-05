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
package org.cloudfoundry.community.servicebroker.database.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.model.BrokerApiVersion;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.service.BeanCatalogService;
import org.cloudfoundry.community.servicebroker.service.CatalogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Slf4j
@Configuration
public class CatalogConfig {

    @Bean
    public CatalogService beanCatalogService(Catalog catalog) {
        return new BeanCatalogService(catalog);
    }

    @Bean
    public BrokerApiVersion brokerApiVersion() {
        return new BrokerApiVersion(BrokerApiVersion.API_VERSION_ANY);
    }

    @Bean
    public Catalog getCatalog(@Value("${catalog}") String catalogJson) throws IOException {
        log.info("using catalog - {}", catalogJson);
        return new ObjectMapper().readValue(catalogJson, Catalog.class);
    }

}