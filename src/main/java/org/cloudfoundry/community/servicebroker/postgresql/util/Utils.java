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
package org.cloudfoundry.community.servicebroker.postgresql.util;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class Utils {

    private Utils() {
    }

    public static void checkValidUUID(String uuidString) {
        log.info("Checking if this UUID string is a valid UUID: " + uuidString);
        UUID uuid = UUID.fromString(uuidString);

        if(!uuidString.equals(uuid.toString())) {
            throw new IllegalStateException("UUID '" + uuidString + "' is not an UUID.");
        }
    }
}
