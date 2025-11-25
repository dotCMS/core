package com.dotcms.rest.config;

import com.dotmarketing.util.Logger;
import com.jayway.jsonpath.internal.Utils;
import java.io.IOException;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

/**
 * Filter to check if a resource is disabled.
 */
@Provider
@Priority(1)
public class DisabledResourceFilter  implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    /**
     * Check if the resource is disabled.
     * @param requestContext request context.
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        if (resourceClass.isAnnotationPresent(Disabled.class)) {
            final Disabled annotation = resourceClass.getAnnotation(Disabled.class);
            final Status status = annotation.status();
            final String message = Utils.isEmpty(annotation.message()) ? status.getReasonPhrase() : annotation.message();
            requestContext.abortWith(Response.status(annotation.status()).entity(message).build());
            Logger.debug(this.getClass(), "Resource " + resourceClass.getName() + " is marked disabled.");
        }
    }
}
