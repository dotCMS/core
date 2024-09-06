package com.dotcms.rest.observability;

import com.dotcms.observability.service.SystemStateService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@RequestScoped
public class SystemStateApiResource {

    @Inject
    private SystemStateService stateService;

    public Response getSubsystemState(String subsystemName) {
        // implementation
        return Response.ok().build();
    }

    public Response getAllSubsystemStates() {
        // implementation
        return Response.ok().build();
    }
}
