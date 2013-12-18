package com.dotcms.rest;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.ejb.CompanyManagerUtil;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import org.apache.commons.httpclient.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Jonathan Gamba
 *         Date: 7/22/13
 */
@Path ("/config")
public class CMSConfigResource extends WebResource {

    /**
     * Updates some given basic information to the current company, this method will be call it from the CMS Config portlet
     *
     * @param request
     * @param user
     * @param password
     * @param portalURL
     * @param mx
     * @param emailAddress
     * @param size
     * @param homeURL
     * @return
     * @throws IOException
     * @throws JSONException
     */
    @POST
    @Path ("/saveCompanyBasicInfo")
    @Produces ("application/json")
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveCompanyBasicInfo ( @Context HttpServletRequest request,
                                           @FormParam ("user") String user, @FormParam ("password") String password,
                                           @FormParam ("portalURL") String portalURL,
                                           @FormParam ("mx") String mx,
                                           @FormParam ("emailAddress") String emailAddress,
                                           @FormParam ("size") String size,
                                           @FormParam ("homeURL") String homeURL ) throws IOException, JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = init( "user/" + user + "/password/" + password, true, request, true );
        Map<String, String> paramsMap = initData.getParamsMap();

        //Validate the parameters
        if ( paramsMap == null ) {
            paramsMap = new HashMap<String, String>();
        }
        paramsMap.put( "portalURL", portalURL );
        paramsMap.put( "mx", mx );
        paramsMap.put( "emailAddress", emailAddress );
        paramsMap.put( "size", size );
        paramsMap.put( "homeURL", homeURL );
        if ( !validate( paramsMap, responseMessage, "portalURL", "mx", "emailAddress", "size", "homeURL" ) ) {
            return response( responseMessage.toString(), true );
        }

        try {
            PrincipalThreadLocal.setName( initData.getUser().getUserId() );

            //Getting the current company
            Company currentCompany = PublicCompanyFactory.getDefaultCompany();

            //Set the values
            currentCompany.setPortalURL( portalURL );
            currentCompany.setMx( mx );
            currentCompany.setEmailAddress( emailAddress );
            currentCompany.setSize( size );
            currentCompany.setHomeURL( homeURL );

            //Update the company
            CompanyManagerUtil.updateCompany( currentCompany );

            //And prepare the response
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", LanguageUtil.get( initData.getUser().getLocale(), "you-have-successfully-updated-the-company-profile" ) );
            responseMessage.append( jsonResponse.toString() );
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error saving basic information for current company.", e );

            if ( e.getMessage() != null ) {
                responseMessage.append( e.getMessage() );
            } else {
                responseMessage.append( "Error saving basic information for current company." );
            }
            return response( responseMessage.toString(), true );
        } finally {
            // Clear the principal associated with this thread
            PrincipalThreadLocal.setName( null );
        }

        return response( responseMessage.toString(), false );
    }

    /**
     * Updates some given locale information to the current company, this method will be call it from the CMS Config portlet
     *
     * @param request
     * @param user
     * @param password
     * @param languageId
     * @param timeZoneId
     * @return
     * @throws IOException
     * @throws JSONException
     */
    @POST
    @Path ("/saveCompanyLocaleInfo")
    @Produces ("application/json")
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveCompanyLocaleInfo ( @Context HttpServletRequest request,
                                            @FormParam ("user") String user, @FormParam ("password") String password,
                                            @FormParam ("languageId") String languageId,
                                            @FormParam ("timeZoneId") String timeZoneId ) throws IOException, JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = init( "user/" + user + "/password/" + password, true, request, true );
        Map<String, String> paramsMap = initData.getParamsMap();

        //Validate the parameters
        if ( paramsMap == null ) {
            paramsMap = new HashMap<String, String>();
        }
        paramsMap.put( "languageId", languageId );
        paramsMap.put( "timeZoneId", timeZoneId );
        if ( !validate( paramsMap, responseMessage, "languageId", "timeZoneId" ) ) {
            return response( responseMessage.toString(), true );
        }

        try {
            PrincipalThreadLocal.setName( initData.getUser().getUserId() );

            //Updating the locale info
            CompanyManagerUtil.updateUsers( languageId, timeZoneId, null, false, false, null );
            TimeZone.setDefault( TimeZone.getTimeZone( timeZoneId ) );

            //And prepare the response
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", LanguageUtil.get( initData.getUser().getLocale(), "you-have-successfully-updated-the-company-profile" ) );
            responseMessage.append( jsonResponse.toString() );
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error saving basic information for current company.", e );

            if ( e.getMessage() != null ) {
                responseMessage.append( e.getMessage() );
            } else {
                responseMessage.append( "Error saving basic information for current company." );
            }
            return response( responseMessage.toString(), true );
        } finally {
            // Clear the principal associated with this thread
            PrincipalThreadLocal.setName( null );
        }

        return response( responseMessage.toString(), false );
    }

    /**
     * Updates a given authentication type to the current company, this method will be call it from the CMS Config portlet
     *
     * @param request
     * @param user
     * @param password
     * @param authType
     * @return
     * @throws IOException
     * @throws JSONException
     */
    @POST
    @Path ("/saveCompanyAuthTypeInfo")
    @Produces ("application/json")
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveCompanyAuthTypeInfo ( @Context HttpServletRequest request,
                                              @FormParam ("user") String user, @FormParam ("password") String password,
                                              @FormParam ("authType") String authType ) throws IOException, JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = init( "user/" + user + "/password/" + password, true, request, true );
        Map<String, String> paramsMap = initData.getParamsMap();

        //Validate the parameters
        if ( paramsMap == null ) {
            paramsMap = new HashMap<String, String>();
        }
        paramsMap.put( "authType", authType );
        if ( !validate( paramsMap, responseMessage, "authType" ) ) {
            return response( responseMessage.toString(), true );
        }

        try {
            PrincipalThreadLocal.setName( initData.getUser().getUserId() );

            //Getting the current company
            Company currentCompany = PublicCompanyFactory.getDefaultCompany();

            //Set the values
            currentCompany.setAuthType( authType );

            //Update the company
            CompanyManagerUtil.updateCompany( currentCompany );

            //And prepare the response
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", LanguageUtil.get( initData.getUser().getLocale(), "you-have-successfully-updated-the-company-profile" ) );
            responseMessage.append( jsonResponse.toString() );
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error saving basic information for current company.", e );

            if ( e.getMessage() != null ) {
                responseMessage.append( e.getMessage() );
            } else {
                responseMessage.append( "Error saving basic information for current company." );
            }
            return response( responseMessage.toString(), true );
        } finally {
            // Clear the principal associated with this thread
            PrincipalThreadLocal.setName( null );
        }

        return response( responseMessage.toString(), false );
    }

    /**
     * Updates the company logo.
     *
     * @param request
     * @param user
     * @param password
     * @param logoFile
     * @param logoDetail
     * @return
     * @throws IOException
     * @throws JSONException
     */
    @POST
    @Path ("/saveCompanyLogo")
    @Produces ("text/html")
    @Consumes (MediaType.MULTIPART_FORM_DATA)
    public Response saveCompanyLogo ( @Context HttpServletRequest request,
                                      @FormDataParam ("user") String user, @FormDataParam ("password") String password,
                                      @FormDataParam ("logoFile") File logoFile,
                                      @FormDataParam ("logoFile") FormDataContentDisposition logoDetail ) throws IOException, JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = init( "user/" + user + "/password/" + password, true, request, true );

        //Validate the parameters
        if ( !UtilMethods.isSet( logoFile ) ) {
            //Prepare a proper response
            responseMessage.append( "Error: The Logo file is a required Field." );
            return response( responseMessage.toString(), true );
        }

        try {
            PrincipalThreadLocal.setName( initData.getUser().getUserId() );

            //Update the logo
            CompanyManagerUtil.updateLogo( logoFile );

            //And prepare the response
            String message = LanguageUtil.get( initData.getUser().getLocale(), "you-have-successfully-updated-the-company-logo" );
            responseMessage.append( "<html><head></head><body><textarea>{'success':'true', 'message':'" ).append( message ).append( "'}</textarea></body></html>" );
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error the company logo.", e );

            if ( e.getMessage() != null ) {
                responseMessage.append( e.getMessage() );
            } else {
                responseMessage.append( "Error the company logo." );
            }
            return response( responseMessage.toString(), true );
        } finally {
            // Clear the principal associated with this thread
            PrincipalThreadLocal.setName( null );
        }

        return response( responseMessage.toString(), false, "text/html" );
    }

    /**
     * Deletes a given environment
     *
     * @param request
     * @param user
     * @param password
     * @param environment
     * @return
     * @throws JSONException
     * @throws IOException
     */
    @POST
    @Path ("/deleteEnvironment")
    @Produces ("application/json")
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteEnvironment ( @Context HttpServletRequest request,
                                        @FormParam ("user") String user, @FormParam ("password") String password,
                                        @FormParam ("environment") String environment ) throws JSONException, IOException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = init( "user/" + user + "/password/" + password, true, request, true );
        Map<String, String> paramsMap = initData.getParamsMap();

        //Validate the parameters
        if ( paramsMap == null ) {
            paramsMap = new HashMap<String, String>();
        }
        paramsMap.put( "environment", environment );
        if ( !validate( paramsMap, responseMessage, "environment" ) ) {
            return response( responseMessage.toString(), true );
        }

        try {
            EnvironmentAPI environmentAPI = APILocator.getEnvironmentAPI();

            //Delete the environment
            environmentAPI.deleteEnvironment( environment );

            //If it was deleted successfully lets remove it from session
            if ( UtilMethods.isSet( request.getSession().getAttribute( WebKeys.SELECTED_ENVIRONMENTS ) ) ) {

                //Get the selected environments from the session
                List<Environment> lastSelectedEnvironments = (List<Environment>) request.getSession().getAttribute( WebKeys.SELECTED_ENVIRONMENTS );
                Iterator<Environment> environmentsIterator = lastSelectedEnvironments.iterator();

                while ( environmentsIterator.hasNext() ) {

                    Environment currentEnv = environmentsIterator.next();
                    //Verify if the current env is on the ones stored in session
                    if ( currentEnv.getId().equals( environment ) ) {
                        //If we found it lets remove it
                        environmentsIterator.remove();
                    }
                }
            }

            //And prepare the response
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", LanguageUtil.get( initData.getUser().getLocale(), "publisher_Environment_deleted" ) );
            responseMessage.append( jsonResponse.toString() );
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error deleting Environment: " + environment, e );

            if ( e.getMessage() != null ) {
                responseMessage.append( e.getMessage() );
            } else {
                responseMessage.append( "Error deleting Environment: " ).append( environment );
            }
            return response( responseMessage.toString(), true );
        }

        return response( responseMessage.toString(), false );
    }

    /**
     * Deletes a given end point
     *
     * @param request
     * @param user
     * @param password
     * @param endPoint
     * @return
     * @throws JSONException
     * @throws IOException
     */
    @POST
    @Path ("/deleteEndpoint")
    @Produces ("application/json")
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteEndpoint ( @Context HttpServletRequest request,
                                     @FormParam ("user") String user, @FormParam ("password") String password,
                                     @FormParam ("endPoint") String endPoint ) throws JSONException, IOException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = init( "user/" + user + "/password/" + password, true, request, true );
        Map<String, String> paramsMap = initData.getParamsMap();

        //Validate the parameters
        if ( paramsMap == null ) {
            paramsMap = new HashMap<String, String>();
        }
        paramsMap.put( "endPoint", endPoint );
        if ( !validate( paramsMap, responseMessage, "endPoint" ) ) {
            return response( responseMessage.toString(), true );
        }

        try {
            PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();

            PublishingEndPoint pep = pepAPI.findEndPointById(endPoint);
            String environmentId = pep.getGroupId();

            //Delete the end point
            pepAPI.deleteEndPointById( endPoint );

            // if the environment is now empty, lets remove it from session
            if(pepAPI.findSendingEndPointsByEnvironment(environmentId).isEmpty()) {
            	//If it was deleted successfully lets remove it from session
                if ( UtilMethods.isSet( request.getSession().getAttribute( WebKeys.SELECTED_ENVIRONMENTS ) ) ) {

                    //Get the selected environments from the session
                    List<Environment> lastSelectedEnvironments = (List<Environment>) request.getSession().getAttribute( WebKeys.SELECTED_ENVIRONMENTS );
                    Iterator<Environment> environmentsIterator = lastSelectedEnvironments.iterator();

                    while ( environmentsIterator.hasNext() ) {

                        Environment currentEnv = environmentsIterator.next();
                        //Verify if the current env is on the ones stored in session
                        if ( currentEnv.getId().equals( environmentId ) ) {
                            //If we found it lets remove it
                            environmentsIterator.remove();
                        }
                    }
                }
            }


            //And prepare the response
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", LanguageUtil.get( initData.getUser().getLocale(), "publisher_End-Point_deleted" ) );
            responseMessage.append( jsonResponse.toString() );
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error deleting End Point: " + endPoint, e );

            if ( e.getMessage() != null ) {
                responseMessage.append( e.getMessage() );
            } else {
                responseMessage.append( "Error deleting End Point: " ).append( endPoint );
            }
            return response( responseMessage.toString(), true );
        }

        return response( responseMessage.toString(), false );
    }

    /**
     * Validates a Collection or string parameters.
     *
     * @param paramsMap
     * @param responseMessage
     * @param args
     * @return True if all the params are present, false otherwise
     * @throws JSONException
     */
    private Boolean validate ( Map<String, String> paramsMap, StringBuilder responseMessage, String... args ) throws JSONException {

        for ( String param : args ) {

            //Validate the given param
            if ( !UtilMethods.isSet( paramsMap.get( param ) ) ) {

                //Prepare a proper response
                responseMessage.append( "Error: " ).append( param ).append( " is a required Field." );
                return false;
            }
        }

        return true;
    }

    /**
     * Prepares a Response object with a given response text. The creation depends if it is an error or not.
     *
     * @param response
     * @param error
     * @return
     */
    private Response response ( String response, Boolean error ) {
        return response( response, error, "application/json" );
    }

    /**
     * Prepares a Response object with a given response text. The creation depends if it is an error or not.
     *
     * @param response
     * @param error
     * @param contentType
     * @return
     */
    private Response response ( String response, Boolean error, String contentType ) {

        Response.ResponseBuilder responseBuilder;
        if ( error ) {
            responseBuilder = Response.status( HttpStatus.SC_INTERNAL_SERVER_ERROR );
            responseBuilder.entity( response );
        } else {
            responseBuilder = Response.ok( response, contentType );
        }

        return responseBuilder.build();
    }

}