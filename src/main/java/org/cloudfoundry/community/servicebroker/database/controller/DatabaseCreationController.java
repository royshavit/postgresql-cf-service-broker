package org.cloudfoundry.community.servicebroker.database.controller;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.controller.ServiceInstanceController;
import org.cloudfoundry.community.servicebroker.model.ErrorMessage;
import org.cloudfoundry.community.servicebroker.service.CatalogService;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

/**
 * Created by taitz.
 */
@Slf4j
@Controller
public class DatabaseCreationController extends ServiceInstanceController {
    
    public DatabaseCreationController(ServiceInstanceService service, CatalogService catalogService) {
        super(service, catalogService);
    }

    @Override
    public ResponseEntity<ErrorMessage> getErrorResponse(String message, HttpStatus status) {
        log.error(message);
        return super.getErrorResponse(message, status);
    }

}
