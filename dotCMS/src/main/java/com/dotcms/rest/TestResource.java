package com.dotcms.rest;

import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.rest.config.Disabled;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Jonathan Gamba
 *         Date: 8/22/13
 */
@Disabled
@Path ("/testResource")
public class TestResource {

    private final WebResource webResource = new WebResource();

    /**
     * Example method that handles a GET operation
     * <p/>
     * URL Examples
     * http://localhost:8081/api/testResource/testGet/user/admin@dotcms.com/password/admin/param1/parameter1/param2/parameter2
     * http://localhost:8081/api/testResource/testGet/user/admin@dotcms.com/password/admin/param1/parameter1/param2/parameter2/type/json
     * http://localhost:8081/api/testResource/testGet/user/admin@dotcms.com/password/admin/param1/parameter1/param2/parameter2/type/xml
     * http://localhost:8081/api/testResource/testGet/user/admin@dotcms.com/password/admin/param1/parameter1/param2/parameter2/type/jsonp
     * http://localhost:8081/api/testResource/testGet/user/admin@dotcms.com/password/admin/param1/parameter1/param2/parameter2/type/jsonp/callback/myMethodCallback
     *
     * @param request
     * @param params
     * @return
     * @throws JSONException
     */
    @GET
    @Path ("/testGet/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response getDocumentCount (@Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam ("params") String params ) throws JSONException {

        InitDataObject initData = webResource.init(params, request, response, true, null);

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        StringBuilder responseMessage = new StringBuilder();

        //Validate the parameters
        if ( !responseResource.validate( responseMessage, "param1", "param2" ) ) {
            return responseResource.responseError( responseMessage.toString(), HttpStatus.SC_BAD_REQUEST );
        }
        String param1 = initData.getParamsMap().get( "param1" );
        String param2 = initData.getParamsMap().get( "param2" );
        String type = initData.getParamsMap().get( RESTParams.TYPE.getValue() );

        try {

            //SOME VERY SMART CODE.....

            //And prepare the response
            if ( UtilMethods.isSet( type ) && type.equalsIgnoreCase( "xml" ) ) {

                Map<String, Object> mapResponse = new HashMap<>();
                mapResponse.put( "success", true );
                mapResponse.put( "message", "Success message" );
                mapResponse.put( "param1", param1 );
                mapResponse.put( "param2", param2 );

                XStream xstream = XStreamHandler.newXStreamInstance();
                xstream.alias( "response", Map.class );

                StringBuilder xmlBuilder = new StringBuilder();
                xmlBuilder.append( "<?xml version=\"1.0\" encoding='UTF-8'?>" );
                xmlBuilder.append( xstream.toXML( mapResponse ) );

                responseMessage.append( xmlBuilder );
            } else {

                //TODO: Handle JSON and JSONP the same

                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put( "success", true );
                jsonResponse.put( "message", "Success message" );
                jsonResponse.put( "param1", param1 );
                jsonResponse.put( "param2", param2 );

                responseMessage.append( jsonResponse.toString() );
            }


        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error on test method.", e );

            if ( e.getMessage() != null ) {
                responseMessage.append( e.getMessage() );
            } else {
                responseMessage.append( "Error on test method." );
            }
            return responseResource.responseError( responseMessage.toString() );
        }

        return responseResource.response( responseMessage.toString() );
    }

    /**
     * Example method that handles a POST operation
     *
     * @param request
     * @param user
     * @param password
     * @param param1
     * @param param2
     * @param type     respose type, example: json, jsonp, xml
     * @param callback
     * @return
     * @throws IOException
     * @throws JSONException
     */
    @POST
    @Path ("/testPost")
    @Produces (MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveTest ( @Context HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @FormParam ("user") String user, @FormParam ("password") String password,
                               @FormParam ("param1") String param1,
                               @FormParam ("param2") String param2,
                               @FormParam ("type") String type,
                               @FormParam ("callback") String callback ) throws IOException, JSONException {

        InitDataObject initData = webResource.init("user/" + user + "/password/" + password, request, response, true, null);

        Map<String, String> paramsMap = initData.getParamsMap();
        paramsMap.put( "param1", param1 );
        paramsMap.put( "param2", param2 );
        paramsMap.put( "type", type );
        paramsMap.put( "callback", callback );
        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        StringBuilder responseMessage = new StringBuilder();

        //Validate the parameters
        if ( !responseResource.validate( responseMessage, "param1", "param2" ) ) {
            return responseResource.responseError( responseMessage.toString(), HttpStatus.SC_BAD_REQUEST );
        }

        try {

            //SOME VERY SMART CODE.....

            //And prepare the response
            if ( UtilMethods.isSet( type ) && type.equalsIgnoreCase( "xml" ) ) {

                Map<String, Object> mapResponse = new HashMap<>();
                mapResponse.put( "success", true );
                mapResponse.put( "message", "Success message" );
                mapResponse.put( "param1", param1 );
                mapResponse.put( "param2", param2 );

                XStream xstream = XStreamHandler.newXStreamInstance();
                xstream.alias( "response", Map.class );

                StringBuilder xmlBuilder = new StringBuilder();
                xmlBuilder.append( "<?xml version=\"1.0\" encoding='UTF-8'?>" );
                xmlBuilder.append( xstream.toXML( mapResponse ) );

                responseMessage.append( xmlBuilder );
            } else {

                //TODO: Handle JSON and JSONP the same

                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put( "success", true );
                jsonResponse.put( "message", "Success message" );
                jsonResponse.put( "param1", param1 );
                jsonResponse.put( "param2", param2 );

                responseMessage.append( jsonResponse.toString() );
            }


        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error on test method.", e );

            if ( e.getMessage() != null ) {
                responseMessage.append( e.getMessage() );
            } else {
                responseMessage.append( "Error on test method." );
            }
            return responseResource.responseError( responseMessage.toString() );
        }

        return responseResource.response( responseMessage.toString() );
    }

}