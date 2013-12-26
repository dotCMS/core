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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

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
    @Produces (MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveCompanyBasicInfo ( @Context HttpServletRequest request,
                                           @FormParam ("user") String user, @FormParam ("password") String password,
                                           @FormParam ("portalURL") String portalURL,
                                           @FormParam ("mx") String mx,
                                           @FormParam ("emailAddress") String emailAddress,
                                           @FormParam ("size") String size,
                                           @FormParam ("homeURL") String homeURL ) throws IOException, JSONException {

        InitDataObject initData = init( "user/" + user + "/password/" + password, true, request, true );

        Map<String, String> paramsMap = initData.getParamsMap();
        paramsMap.put( "portalURL", portalURL );
        paramsMap.put( "mx", mx );
        paramsMap.put( "emailAddress", emailAddress );
        paramsMap.put( "size", size );
        paramsMap.put( "homeURL", homeURL );

        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        StringBuilder responseMessage = new StringBuilder();

        //Validate the parameters
        if ( !responseResource.validate( responseMessage, "portalURL", "mx", "emailAddress", "size", "homeURL" ) ) {
            return responseResource.responseError( responseMessage.toString(), HttpStatus.SC_BAD_REQUEST );
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
            return responseResource.responseError( responseMessage.toString() );
        } finally {
            // Clear the principal associated with this thread
            PrincipalThreadLocal.setName( null );
        }

        return responseResource.response( responseMessage.toString() );
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
    @Produces (MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveCompanyLocaleInfo ( @Context HttpServletRequest request,
                                            @FormParam ("user") String user, @FormParam ("password") String password,
                                            @FormParam ("languageId") String languageId,
                                            @FormParam ("timeZoneId") String timeZoneId ) throws IOException, JSONException {

        InitDataObject initData = init( "user/" + user + "/password/" + password, true, request, true );

        Map<String, String> paramsMap = initData.getParamsMap();
        paramsMap.put( "languageId", languageId );
        paramsMap.put( "timeZoneId", timeZoneId );
        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        StringBuilder responseMessage = new StringBuilder();

        //Validate the parameters
        if ( !responseResource.validate( responseMessage, "languageId", "timeZoneId" ) ) {
            return responseResource.responseError( responseMessage.toString(), HttpStatus.SC_BAD_REQUEST );
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
            return responseResource.responseError( responseMessage.toString() );
        } finally {
            // Clear the principal associated with this thread
            PrincipalThreadLocal.setName( null );
        }

        return responseResource.response( responseMessage.toString() );
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
    @Produces (MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveCompanyAuthTypeInfo ( @Context HttpServletRequest request,
                                              @FormParam ("user") String user, @FormParam ("password") String password,
                                              @FormParam ("authType") String authType ) throws IOException, JSONException {

        InitDataObject initData = init( "user/" + user + "/password/" + password, true, request, true );

        Map<String, String> paramsMap = initData.getParamsMap();
        paramsMap.put( "authType", authType );
        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        StringBuilder responseMessage = new StringBuilder();

        //Validate the parameters
        if ( !responseResource.validate( responseMessage, "authType" ) ) {
            return responseResource.responseError( responseMessage.toString(), HttpStatus.SC_BAD_REQUEST );
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
            return responseResource.responseError( responseMessage.toString() );
        } finally {
            // Clear the principal associated with this thread
            PrincipalThreadLocal.setName( null );
        }

        return responseResource.response( responseMessage.toString() );
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
    @Produces (MediaType.TEXT_HTML)
    @Consumes (MediaType.MULTIPART_FORM_DATA)
    public Response saveCompanyLogo ( @Context HttpServletRequest request,
                                      @FormDataParam ("user") String user, @FormDataParam ("password") String password,
                                      @FormDataParam ("logoFile") File logoFile,
                                      @FormDataParam ("logoFile") FormDataContentDisposition logoDetail ) throws IOException, JSONException {

        InitDataObject initData = init( "user/" + user + "/password/" + password, true, request, true );

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        StringBuilder responseMessage = new StringBuilder();

        //Validate the parameters
        if ( !UtilMethods.isSet( logoFile ) ) {
            //Prepare a proper response
            responseMessage.append( "Error: The Logo file is a required Field." );
            return responseResource.responseError( responseMessage.toString(), HttpStatus.SC_BAD_REQUEST );
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
            return responseResource.responseError( responseMessage.toString() );
        } finally {
            // Clear the principal associated with this thread
            PrincipalThreadLocal.setName( null );
        }

        return responseResource.response( responseMessage.toString() );
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
    @Produces (MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteEnvironment ( @Context HttpServletRequest request,
                                        @FormParam ("user") String user, @FormParam ("password") String password,
                                        @FormParam ("environment") String environment,
                                        @FormParam ("type") String type,
                                        @FormParam ("callback") String callback ) throws JSONException, IOException {

        InitDataObject initData = init( "user/" + user + "/password/" + password, true, request, true );

        Map<String, String> paramsMap = initData.getParamsMap();
        paramsMap.put( "environment", environment );
        paramsMap.put( "type", type );
        paramsMap.put( "callback", callback );
        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        StringBuilder responseMessage = new StringBuilder();

        //Validate the parameters
        if ( !responseResource.validate( responseMessage, "environment" ) ) {
            return responseResource.responseError( responseMessage.toString(), HttpStatus.SC_BAD_REQUEST );
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
            return responseResource.responseError( responseMessage.toString() );
        }

        return responseResource.response( responseMessage.toString() );
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
    @Produces (MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteEndpoint ( @Context HttpServletRequest request,
                                     @FormParam ("user") String user, @FormParam ("password") String password,
                                     @FormParam ("endPoint") String endPoint,
                                     @FormParam ("type") String type,
                                     @FormParam ("callback") String callback ) throws JSONException, IOException {

        InitDataObject initData = init( "user/" + user + "/password/" + password, true, request, true );

        Map<String, String> paramsMap = initData.getParamsMap();
        paramsMap.put( "endPoint", endPoint );
        paramsMap.put( "type", type );
        paramsMap.put( "callback", callback );
        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        StringBuilder responseMessage = new StringBuilder();

        //Validate the parameters
        if ( !responseResource.validate( responseMessage, "endPoint" ) ) {
            return responseResource.responseError( responseMessage.toString(), HttpStatus.SC_BAD_REQUEST );
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
            return responseResource.responseError( responseMessage.toString() );
        }

        return responseResource.response( responseMessage.toString() );
    }

}