package com.dotcms.rest;

import com.dotcms.company.CompanyAPI;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.api.v1.system.ConfigurationHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.InvalidTimeZoneException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.ejb.CompanyManagerUtil;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * @author Jonathan Gamba
 *         Date: 7/22/13
 */
@Path ("/config")
@Tag(name = "System Configuration")
public class CMSConfigResource {

    private final WebResource webResource = new WebResource();

    /**
     * Updates some given basic information to the current company, this method will be call it from the CMS Config portlet
     *
     * @param request
     * @param user
     * @param password
     * @param portalURL
     * @param mx
     * @param emailAddress
     * @param backgroundColor this one is the Background Color
     * @param primaryColor this one is the Primary Color
     * @param secondaryColor this one is the Secondary Color
     * @param homeURL
     * @param loginLogo dotAsset Path of the logo showed at the login screen
     * @paramnavLogo dotAsset Path of the logo showed at the nav bar
     * @return
     * @throws IOException
     * @throws JSONException
     */
    @POST
    @Path("/saveCompanyBasicInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveCompanyBasicInfo(@Context HttpServletRequest request,
            @Context final HttpServletResponse response,
            @FormParam("user") final String user,
            @FormParam("password") final String password,
            @FormParam("portalURL") final String portalURL,
            @FormParam("mx") final String mx,
            @FormParam("emailAddress") final String emailAddress,
            @FormParam("size") final String backgroundColor,
            @FormParam("type") final String primaryColor,
            @FormParam("street") final String secondaryColor,
            @FormParam("homeURL") final String homeURL,
            @FormParam("city") final String loginLogo,
            @FormParam("state") String navLogo) throws IOException, JSONException {

        InitDataObject initData = webResource.init( "user/" + user + "/password/" + password, request, response, true, PortletID.CONFIGURATION.toString() );

        //Nav Logo Feature is not for Community level license
        if(LicenseUtil.getLevel() == LicenseLevel.COMMUNITY.level && UtilMethods.isSet(navLogo)){
            Logger.warn(this,"NavLogo Feature is only for Enterprise Edition");
            navLogo = StringPool.BLANK;
        }

        Map<String, String> paramsMap = initData.getParamsMap();
        paramsMap.put( "portalURL", portalURL );
        paramsMap.put( "mx", mx );
        paramsMap.put( "emailAddress", emailAddress );
        paramsMap.put( "size", backgroundColor );
        paramsMap.put("type", primaryColor);
        paramsMap.put("street", secondaryColor);
        paramsMap.put( "homeURL", homeURL );
        paramsMap.put("city",loginLogo);
        paramsMap.put("state",navLogo);

        ResourceResponse responseResource = new ResourceResponse( paramsMap );
        StringBuilder responseMessage = new StringBuilder();

        //Validate the parameters
        if ( !responseResource.validate( responseMessage, "portalURL", "emailAddress", "size","type","street" ) ) {
            return responseResource.responseError( responseMessage.toString(), HttpStatus.SC_BAD_REQUEST );
        }

        try {
            ConfigurationHelper.INSTANCE.parseMailAndSender(emailAddress);
        } catch (IllegalArgumentException iae){
            return responseResource.responseError( iae.getMessage(), HttpStatus.SC_BAD_REQUEST );
        }

        try {
            PrincipalThreadLocal.setName( initData.getUser().getUserId() );

            //Getting the current company
            final Company currentCompany = APILocator.getCompanyAPI().getDefaultCompany();

            //Set the values
            currentCompany.setPortalURL( portalURL );
            currentCompany.setMx( mx );
            currentCompany.setEmailAddress( emailAddress );
            currentCompany.setSize( backgroundColor );
            currentCompany.setType(primaryColor);
            currentCompany.setStreet(secondaryColor);
            currentCompany.setHomeURL( homeURL );
            currentCompany.setCity(loginLogo);
            currentCompany.setState(navLogo);

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
                                            @Context final HttpServletResponse response,
                                            @FormParam ("user") String user, @FormParam ("password") String password,
                                            @FormParam ("languageId") String languageId,
                                            @FormParam ("timeZoneId") String timeZoneId ) throws IOException, JSONException {

        InitDataObject initData = webResource.init( "user/" + user + "/password/" + password, request, response, true, PortletID.CONFIGURATION.toString() );

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

            //And prepare the response
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", LanguageUtil.get( initData.getUser().getLocale(), "you-have-successfully-updated-the-company-profile" ) );
            responseMessage.append( jsonResponse.toString() );
        } catch ( Exception e ) {
            if (e instanceof InvalidTimeZoneException) {
                Logger.error(this.getClass(), "Error saving basic information for current company: " + e.getMessage());
            } else {
                Logger.error( this.getClass(), "Error saving basic information for current company.", e );
            }

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
                                              @Context final HttpServletResponse response,
                                              @FormParam ("user") String user, @FormParam ("password") String password,
                                              @FormParam ("authType") String authType ) throws IOException, JSONException {

        InitDataObject initData = webResource.init( "user/" + user + "/password/" + password, request, response, true, PortletID.CONFIGURATION.toString() );

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
     * Updates the company logo. Now to update the logo use the {@link CMSConfigResource#saveCompanyBasicInfo}
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
    @Deprecated
    @POST
    @Path ("/saveCompanyLogo")
    @Produces (MediaType.TEXT_HTML)
    @Consumes (MediaType.MULTIPART_FORM_DATA)
    public Response saveCompanyLogo ( @Context HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @FormDataParam("user") String user, @FormDataParam ("password") String password,
                                      @FormDataParam ("logoFile") File logoFile,
                                      @FormDataParam ("logoFile") FormDataContentDisposition logoDetail ) throws IOException, JSONException {

        InitDataObject initData = webResource.init( "user/" + user + "/password/" + password, request, response, true , PortletID.CONFIGURATION.toString());

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
                                        @Context final HttpServletResponse response,
                                        @FormParam ("user") String user, @FormParam ("password") String password,
                                        @FormParam ("environment") String environment,
                                        @FormParam ("type") String type,
                                        @FormParam ("callback") String callback ) throws JSONException, IOException {

        InitDataObject initData = webResource.init( "user/" + user + "/password/" + password, request, response, true, PortletID.CONFIGURATION.toString() );

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
            if ( UtilMethods.isSet( request.getSession().getAttribute( WebKeys.SELECTED_ENVIRONMENTS + user ) ) ) {

                //Get the selected environments from the session
                List<Environment> lastSelectedEnvironments = (List<Environment>) request.getSession().getAttribute( WebKeys.SELECTED_ENVIRONMENTS + user );
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
     * @deprecated use {@link EndpointResource#delete(HttpServletRequest, HttpServletResponse, String)}
     * @param request
     * @param user
     * @param password
     * @param endPoint
     * @return
     * @throws JSONException
     * @throws IOException
     */
    @Deprecated
    @POST
    @Path ("/deleteEndpoint")
    @Produces (MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteEndpoint ( @Context HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @FormParam ("user") String user, @FormParam ("password") String password,
                                     @FormParam ("endPoint") String endPoint,
                                     @FormParam ("type") String type,
                                     @FormParam ("callback") String callback ) throws JSONException, IOException {

        InitDataObject initData = webResource.init( "user/" + user + "/password/" + password, request, response, true , PortletID.CONFIGURATION.toString());

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

    @POST
    @Path ("/regenerateKey")
    @Produces (MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response regenerateKey ( @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) throws JSONException, IOException {
        try {
            final InitDataObject initData =
                    new WebResource.InitBuilder(webResource)
                            .requiredBackendUser(true)
                            .requiredFrontendUser(false)
                            .requestAndResponse(request, response)
                            .rejectWhenNoUser(true)
                            .init();
            final User user = initData.getUser();
            final CompanyAPI companyAPI = APILocator.getCompanyAPI();
            final Company defaultCompany = companyAPI.getDefaultCompany();
            final Company updatedCompany = companyAPI.regenerateKey(defaultCompany, user);
            return Response.ok(new ResponseEntityView(updatedCompany.getKeyDigest())).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(), "Exception calling regenerateKey." , e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }


}
