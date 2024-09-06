package com.dotcms.cli.exception;

import static org.apache.commons.lang3.StringUtils.*;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import java.util.function.Supplier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;


/**
 * This is a small helper intended to improve how exceptions are presented to the user
 */

@ApplicationScoped
public class ExceptionHandlerImpl implements ExceptionHandler {

    @Inject
    ExceptionMappingConfig config;

    public Exception unwrap(Exception ex) {
        // Exceptions coming from Asynchronous code is wrapped within a few layers of exceptions, but they are inheritors from RunTimeExceptions
        // e.g. TraversalTaskException, CompletionException
        // This code gets me to the relevant exception
        while (ex instanceof RuntimeException && ex.getCause() != null ){
            ex = (Exception) ex.getCause();
        }
        return ex;
    }

    public  Exception handle(Exception ex) {

        if (ex instanceof SecurityException) {
            ex = handle((SecurityException) ex);
        }
        if (ex instanceof WebApplicationException) {
            ex = handle((WebApplicationException) ex);
        }
        return ex;
    }

    /**
     * Since Exceptions returned by the back-end can be too noisy and sometimes ambiguous
     * e.g. Sometimes a 404 is returned when no resources are available
     * but sometimes the same code is returned when the resource isn't available product of permission restrictions
     * So here we override the message returned by the server side and have it replaced by something more generic that can be used to describe both scenarios
     * @param in
     * @return
     */
     private  WebApplicationException handle(WebApplicationException in){
         final Response response = in.getResponse();
         final String serverError = response.getStatusInfo().getReasonPhrase();
         final int status = response.getStatusInfo().getStatusCode();
         final String errorMessage = config.override() ?
                 messageOverride(status, () -> getIfEmpty(config.fallback(), () -> serverError)) :
                 serverError;
         return new WebApplicationException(Response.status(status,errorMessage).build());
     }

    private String messageOverride(final int status, final Supplier<String> fallback) {
        return config.messages().containsKey(status) ?
               config.messages().get(status) :
                fallback.get();
    }

    /**
     * Sometimes OurRest endpoint will respond with a descendant of  SecurityException instead of WebApplicationException
     * So here we make sure everything stays consistent and every exception presented to the user is a WebApplicationException
     * @param in
     * @return
     */
     private Exception handle(SecurityException in){
         int code = 0;
         if(in instanceof UnauthorizedException){
             code = 401;
         }
         if (in instanceof ForbiddenException){
             code = 403;
         }
         if(code > 0) {
             return new WebApplicationException(
                     Response.status(code).build());
         }
         return in;
     }


}
