package com.dotcms.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.apache.felix.framework.OSGIUtil;
import org.osgi.framework.Bundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;

/**
 * @author Jonathan Gamba
 *         Date: 28/05/14
 */
@Path ("/osgi")
public class OSGIResource  {

    List<String> systemBundles = Arrays.asList(
            "org.apache.felix.http.bundle",
            "org.apache.felix.gogo.shell",
            "org.apache.felix.bundlerepository",
            "org.apache.felix.framework",
            "org.apache.felix.fileinstall",
            "org.apache.felix.gogo.command",
            "org.apache.felix.gogo.runtime",
            "osgi.cmpn",
            "osgi.core",
            "org.apache.tika.core",
            "org.apache.tika.bundle",
            "slf4j.simple",
            "slf4j.api",
            "jcl.over.slf4j",
            "com.dotcms.tika",
            "org.apache.felix.http.api",
            "org.apache.felix.configadmin"
    );

    private final WebResource webResource = new WebResource();

    /**
     * This method returns a list of all bundles installed in the OSGi environment at the time of the call to this method.
     *
     * @param request
     * @param params
     * @return
     * @throws JSONException
     */
    @GET
    @Path ("/getInstalledBundles/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response getInstalledBundles (@Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam ("params") String params ) throws JSONException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .params(params)
                .rejectWhenNoUser(true)
                .init();

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        StringBuilder responseMessage = new StringBuilder();

        //Verify if the user have access to the OSGI portlet
        User currentUser = initData.getUser();
        try {
            if ( currentUser == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet( "dynamic-plugins", currentUser ) ) {
                throw new ForbiddenException("User does not have access to the Dynamic Plugins Portlet");
            }
        } catch ( DotDataException e ) {
            Logger.error( this.getClass(), "Error validating User access to the Dynamic Plugins Portlet.", e );

            if ( e.getMessage() != null ) {
                responseMessage.append( e.getMessage() );
            } else {
                responseMessage.append( "Error validating User access to the Dynamic Plugins Portlet." );
            }
            return responseResource.responseError( responseMessage.toString() );
        }

        /*
        This method returns a list of all bundles installed in the OSGi environment at the time of the call to this method. However,
        since the Framework is a very dynamic environment, bundles can be installed or uninstalled at anytime.
         */
        final Bundle[] installedBundles = OSGIUtil.getInstance().getBundles();

        //Read the parameters
        String ignoreSystemBundlesParam = initData.getParamsMap().get( "ignoresystembundles" );
        String type = initData.getParamsMap().get( RESTParams.TYPE.getValue() );
        Boolean ignoreSystemBundles = false;
        if ( UtilMethods.isSet( ignoreSystemBundlesParam ) && ignoreSystemBundlesParam.equalsIgnoreCase( "true" ) ) {
            ignoreSystemBundles = true;
        }

        try {

            //And prepare the response
            if ( UtilMethods.isSet( type ) && type.equalsIgnoreCase( "xml" ) ) {

                ArrayList<Map> bundlesArray = new ArrayList<Map>();
                for ( Bundle bundle : installedBundles ) {

                    if ( ignoreSystemBundles && systemBundles.contains( bundle.getSymbolicName() ) ) {
                        continue;
                    }

                    //Getting the jar file name
                    String separator = File.separator;
                    if ( bundle.getLocation().contains( "/" ) ) {
                        separator = "/";
                    }
                    String jarFile = bundle.getLocation().contains( separator ) ? bundle.getLocation().substring( bundle.getLocation().lastIndexOf( separator ) + 1 ) : "System";

                    //Build the version string
                    String version = bundle.getVersion().getMajor() + "." + bundle.getVersion().getMinor() + "." + bundle.getVersion().getMicro();

                    //Reading and setting bundle information
                    Map<String, Object> mapResponse = new HashMap<String, Object>();
                    mapResponse.put( "bundleId", bundle.getBundleId() );
                    mapResponse.put( "symbolicName", bundle.getSymbolicName() );
                    mapResponse.put( "location", bundle.getLocation() );
                    mapResponse.put( "jarFile", jarFile );
                    mapResponse.put( "state", bundle.getState() );
                    mapResponse.put( "version", version );
                    mapResponse.put( "separator", separator );

                    bundlesArray.add( mapResponse );
                }

                XStream xstream = new XStream( new DomDriver() );
                xstream.alias( "response", ArrayList.class );

                StringBuilder xmlBuilder = new StringBuilder();
                xmlBuilder.append( "<?xml version=\"1.0\" encoding='UTF-8'?>" );
                xmlBuilder.append( xstream.toXML( bundlesArray ) );

                responseMessage.append( xmlBuilder );
            } else {

                JSONArray bundlesArray = new JSONArray();
                for ( Bundle bundle : installedBundles ) {

                    if ( ignoreSystemBundles && systemBundles.contains( bundle.getSymbolicName() ) ) {
                        continue;
                    }

                    //Getting the jar file name
                    String separator = File.separator;
                    if ( bundle.getLocation().contains( "/" ) ) {
                        separator = "/";
                    }
                    String jarFile = bundle.getLocation().contains( separator ) ? bundle.getLocation().substring( bundle.getLocation().lastIndexOf( separator ) + 1 ) : "System";

                    //Build the version string
                    String version = bundle.getVersion().getMajor() + "." + bundle.getVersion().getMinor() + "." + bundle.getVersion().getMicro();

                    //Reading and setting bundle information
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put( "bundleId", bundle.getBundleId() );
                    jsonResponse.put( "symbolicName", bundle.getSymbolicName() );
                    jsonResponse.put( "location", bundle.getLocation() );
                    jsonResponse.put( "jarFile", jarFile );
                    jsonResponse.put( "state", bundle.getState() );
                    jsonResponse.put( "version", version );
                    jsonResponse.put( "separator", separator );

                    bundlesArray.add( jsonResponse );
                }

                responseMessage.append( bundlesArray.toString() );
            }


        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error getting installed OSGI bundles.", e );

            if ( e.getMessage() != null ) {
                responseMessage.append( e.getMessage() );
            } else {
                responseMessage.append( "Error getting installed OSGI bundles." );
            }
            return responseResource.responseError( responseMessage.toString() );
        }

        return responseResource.response( responseMessage.toString() );
    }

}