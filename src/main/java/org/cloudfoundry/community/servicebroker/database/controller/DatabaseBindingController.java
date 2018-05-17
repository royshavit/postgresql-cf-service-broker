package org.cloudfoundry.community.servicebroker.database.controller;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.controller.ServiceInstanceBindingController;
import org.cloudfoundry.community.servicebroker.model.ErrorMessage;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

/**
 * Created by taitz.
 */
@Slf4j
@Controller
public class DatabaseBindingController extends ServiceInstanceBindingController {

    public DatabaseBindingController(ServiceInstanceBindingService serviceInstanceBindingService, ServiceInstanceService serviceInstanceService) {
        super(serviceInstanceBindingService, serviceInstanceService);
    }

    @Override
    public ResponseEntity<ErrorMessage> getErrorResponse(String message, HttpStatus status) {
        log.error(message);
        return super.getErrorResponse(message, status);
    }

}
