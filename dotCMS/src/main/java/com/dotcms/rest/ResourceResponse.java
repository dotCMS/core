package com.dotcms.rest;

import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Jonathan Gamba
 *         Date: 8/22/13
 */
public class ResourceResponse {

    public static final String AUTHENTICATE_RESPONSE_ERROR_HEADER_NAME = "WWW-Authenticate";
    private String type;
    private Map<String, String> paramsMap;

    public ResourceResponse ( Map<String, String> paramsMap ) {
        this.paramsMap = paramsMap;
        this.type = paramsMap.get( RESTParams.TYPE.getValue() );
    }

    public String getType () {
        return type;
    }

    public void setType ( String type ) {
        this.type = type;
    }

    public Map<String, String> getParamsMap () {
        return paramsMap;
    }

    /**
     * Validates a Collection or string parameters.
     *
     * @param responseMessage
     * @param args            List of required parameters names to find in the parameters map
     * @return True if all the params are present, false otherwise
     * @throws com.dotmarketing.util.json.JSONException
     *
     */
    public Boolean validate ( StringBuilder responseMessage, String... args ) throws JSONException {

        for ( String param : args ) {

            //Validate the given param
            if ( !UtilMethods.isSet( getParamsMap().get( param ) ) ) {
                //Prepare a proper response
                responseMessage.append( "Error: " ).append( param ).append( " is a required Field." );
                return false;
            }
        }

        return true;
    }

    /**
     * Just return a response with a Http status 200 (Ok)
     *
     * @return
     */
    public Response response () {
        return Response.status( HttpStatus.SC_OK ).build();
    }

    /**
     * Prepares a Response object with a given response text for success operations (will return a 200 status code).<br/>
     * <p/>
     * If a parameter <strong>"type"</strong> was sent in the request we will use it to define the content type for the response and in the case of<br/>
     * a <strong>type=jsonp</strong> we will also wrap the response within a javascript callback.<br/>
     * For JSONP we can specify the name of the callback method using the parameter <strong>"callback"</strong> if such parameter is not present the name<br/>
     * of the default callback method is <strong>dotJsonpCall</strong>.<br/>
     * <p/>
     * Is important to clarify that if a <strong>"type"</strong> parameter is not send in the request the defined content type for the response will be<br/>
     * the defined by the <i>@Produces</i> annotation of the RESTful method.
     *
     * @param response String with the data to response, this response format should depend if exist of the <strong>"type"</strong> parameter
     * @return
     */
    public Response response ( final String response ) {
        return response( response, null );
    }

    public Response response ( final String response, final CacheControl cacheControl ) {

        return response( response, null, Optional.empty() );
    }

    public Response response ( String response,
                               final CacheControl cacheControl,
                               final Optional<Response.Status> status ) {

        String contentType = null;
        if ( UtilMethods.isSet( getType() ) ) {
            if ( getType().equalsIgnoreCase( "jsonp" ) ) {
                contentType = "application/javascript";

                /*
                For jsonp we need to wrap the given response code into javascript.
                But before that lets verify if we have a callback method set in the params
                 */
                String callback = getParamsMap().get( RESTParams.CALLBACK.getValue() );
                if ( !UtilMethods.isSet( callback ) ) {
                    callback = "dotJsonpCall";
                }

                response = callback + "(" + response + ")";
            } else if ( getType().equalsIgnoreCase( "json" ) ) {
                contentType = MediaType.APPLICATION_JSON;
            } else if ( getType().equalsIgnoreCase( "xml" ) ) {
                contentType = MediaType.APPLICATION_XML;
            } else if ( getType().equalsIgnoreCase( "plain" ) ) {
                contentType = MediaType.TEXT_PLAIN;
            }
        }

        final Response.ResponseBuilder responseBuilder =
                ( contentType != null )?
                        Response.ok( response, contentType ):
                    /*
                    If the Content type of the response is null the default
                    will be the defined by the @Produces annotation of the RESTful method
                     */
                        Response.ok( response );

        status.ifPresent( theStatus ->  responseBuilder.status(status.get()));

        if ( cacheControl != null ) {
            return responseBuilder.cacheControl( cacheControl ).build();
        }

        return responseBuilder.build();
    }


    public Response responseError ( String response ) {
        return responseError( response, HttpStatus.SC_INTERNAL_SERVER_ERROR );
    }

    public Response responseError ( int statusCode ) {
        return responseError( null, statusCode );
    }


    public Response responseAuthenticateError ( final String message, final String errorCode, final String errorKey ) {
        return responseWithWWWAuthenticate(message, HttpStatus.SC_UNAUTHORIZED, errorCode, errorKey);
    }

    public Response responseWithWWWAuthenticate ( final String message, final int status, final String errorCode, final String errorKey ) {
        return responseError(
                message,
                status,
                new HashMap<>(Map.of("WWW-Authenticate",
                        String.format("Bearer realm=\"example\",error=\"%s\",error_key=\"%s\",error_description=\"\"", errorCode, errorKey)
                ))
        );
    }

    public Response responseError ( String response, int statusCode ) {
        return this.responseError(response, statusCode, null);
    }

    public Response responseError (
            final String response, int statusCode,
            final Map<String, Object> headers) {
        Response.ResponseBuilder responseBuilder = Response.status( statusCode );

        if ( UtilMethods.isSet( response ) ) {
            responseBuilder.entity( response );
        }

        if ( UtilMethods.isSet( headers ) ) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                responseBuilder.header(entry.getKey(), entry.getValue());
            }
        }

        return responseBuilder.build();
    }

    public Response responseUnauthorizedError(final String errorKey) {
        return responseWithWWWAuthenticate(null, HttpStatus.SC_FORBIDDEN, "insufficient_scope", errorKey);
    }

    public static Map<String, String> getWWWAuthenticateHeader(final Response response) {
        final String[] headerFields = response.getHeaderString(AUTHENTICATE_RESPONSE_ERROR_HEADER_NAME).split(",");

        final Map<String, String> headerValues = new HashMap<>();

        for (final String headerField : headerFields) {
            final String[] fieldSplit = headerField.split("=");
            headerValues.put(fieldSplit[0], fieldSplit[1]);
        }

        return headerValues;
    }

}
