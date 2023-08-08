package com.dotcms.cli.common;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;


/**
 * This is a small helper intended to improve how exceptions are presented to the user
 */
public class ExceptionHandler {

    public static final String FORBIDDEN = "Forbidden: You don't have permission to access this resource.";
    public static final String BAD_REQUEST = "Bad Request: The server encountered an error processing the request.";
    public static final String UNAUTHORIZED = "Unauthorized: Authentication failed or Permission is denied.";
    public static final String NOT_FOUND = "Not Found: The requested resource was not found or is restricted.";
    public static final String INTERNAL_SERVER_ERROR = "Internal Server Error: An unexpected error occurred on the server.";
    public static final String DEFAULT_ERROR = "An error occurred. Please try again later.";

    private ExceptionHandler(){
        // private constructor
    }

    public static Exception handle(Exception ex) {

       // Exceptions coming from Asynchronous code is wrapped within a few layers of exceptions, but they are inheritors from RunTimeExceptions
       // e.g. TraversalTaskException, CompletionException
       // This code gets me to the relevant exception
        while (ex instanceof RuntimeException && ex.getCause() != null ){
            ex = (Exception) ex.getCause();
        }

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
     private static WebApplicationException handle(WebApplicationException in){
         Response response = in.getResponse();
         String errorMessage;

         // Depending on the response status code, create a custom exception with a cleaner message.
         switch (response.getStatus()) {
             case 400:
                 errorMessage = BAD_REQUEST;
                 break;
             case 401:
                 errorMessage = UNAUTHORIZED;
                 break;
             case 403:
                 errorMessage = FORBIDDEN;
                 break;
             case 404:
                 errorMessage = NOT_FOUND;
                 break;
             case 500:
                 errorMessage = INTERNAL_SERVER_ERROR;
                 break;
             default:
                 errorMessage = DEFAULT_ERROR;
         }
           return new WebApplicationException(Response.status(response.getStatus(),errorMessage).build());
     }

    /**
     * Sometimes OurRest endpoint will respond with a descendant of  SecurityException instead of WebApplicationException
     * So here we make sure everything stays consistent and every exception presented to the user is a WebApplicationException
     * @param in
     * @return
     */
     private static Exception handle(SecurityException in){
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
