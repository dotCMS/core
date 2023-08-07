package com.dotcms.cli.common;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class ExceptionHandler {

    public static Exception handle(Exception ex) {

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

     private static WebApplicationException handle(WebApplicationException in){
         Response response = in.getResponse();
         String errorMessage;

         // Depending on the response status code, create a custom exception with a cleaner message.
         switch (response.getStatus()) {
             case 400:
                 errorMessage = "Bad Request: The server encountered an error processing the request.";
                 break;
             case 401:
                 errorMessage = "Unauthorized: Authentication failed or Permission is denied.";
                 break;
             case 403:
                 errorMessage = "Forbidden: You don't have permission to access this resource.";
                 break;
             case 404:
                 errorMessage = "Not Found: The requested resource was not found or is restricted.";
                 break;
             case 500:
                 errorMessage = "Internal Server Error: An unexpected error occurred on the server.";
                 break;
             default:
                 errorMessage = "An error occurred. Please try again later.";
         }
           return new WebApplicationException(Response.status(response.getStatus(),errorMessage).build());
     }

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
