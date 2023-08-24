package com.dotcms.cli.common;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;


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
         final int status = response.getStatus();
         final String errorMessage =  config.messages().containsKey(status) ? config.messages().get(status) : config.defaultMessage();
         return new WebApplicationException(Response.status(status,errorMessage).build());
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
