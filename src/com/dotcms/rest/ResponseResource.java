package com.dotcms.rest;

import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import org.apache.commons.httpclient.HttpStatus;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * @author Jonathan Gamba
 *         Date: 8/22/13
 */
public class ResponseResource {

    private String type;
    private Map<String, String> paramsMap;

    public ResponseResource ( Map<String, String> paramsMap ) {
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
     * Prepares a Response object with a given response text for success operations (Will return a 200 status code).<br/>
     * If a parameter <strong>"type"</strong> was sent in the request we will use it to define the content type for the response and in the case of<br/>
     * a <strong>type=jsonp</strong> we will also wrap the response (should be JSON format) with javascript.<br/>
     * Is important to clarify that if a <strong>"type"</strong> parameter is not send in the request the defined content type for the response will be<br/>
     * the defined by the <i>@Produces</i> annotation of the RESTful method.
     *
     * @param response String with the data to response, this response format should depend if exist of the <strong>"type"</strong> parameter
     * @return
     */
    public Response response ( String response ) {

        String contentType = null;
        if ( UtilMethods.isSet( getType() ) ) {
            if ( getType().equalsIgnoreCase( "jsonp" ) ) {
                contentType = "application/javascript";

                /*
                For jsonp we need to wrap the given json code into javascript.
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
            }
        }

        Response.ResponseBuilder responseBuilder;
        if ( contentType != null ) {
            responseBuilder = Response.ok( response, contentType );
        } else {
            /*
            If the Content type of the response is null the default
            will be the defined by the @Produces annotation of the RESTful method
             */
            responseBuilder = Response.ok( response );
        }

        return responseBuilder.build();
    }

    public Response responseError ( String response ) {
        return responseError( response, HttpStatus.SC_INTERNAL_SERVER_ERROR );
    }

    public Response responseError ( String response, int status ) {

        Response.ResponseBuilder responseBuilder = Response.status( status );
        responseBuilder.entity( response );

        return responseBuilder.build();
    }

}