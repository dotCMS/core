package com.dotmarketing.portlets.cmsmaintenance.ajax;

import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.ejb.CompanyManagerUtil;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author Jonathan Gamba
 *         Date: 7/18/13
 */
public class CMSConfigAjax extends AjaxAction {

    @Override
    public void action ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        return;
    }

    @Override
    public void service ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

        Map<String, String> map = getURIParams();
        String cmd = map.get( "cmd" );
        Method dispatchMethod;

        User user = getUser();

        try {
            // Check permissions if the user has access to the CMS Maintenance Portlet
            if ( user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet( "EXT_CMS_MAINTENANCE", user ) ) {
                String userName = map.get( "u" ) != null
                        ? map.get( "u" )
                        : map.get( "user" ) != null
                        ? map.get( "user" )
                        : null;

                String password = map.get( "p" ) != null
                        ? map.get( "p" )
                        : map.get( "passwd" ) != null
                        ? map.get( "passwd" )
                        : null;


                LoginFactory.doLogin( userName, password, false, request, response );
                user = (User) request.getSession().getAttribute( WebKeys.CMS_USER );
                if ( user == null ) {
                    setUser( request );
                    user = getUser();
                }
                if ( user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet( "9", user ) ) {
                    response.sendError( 401 );
                    return;
                }
            }

            PrincipalThreadLocal.setName( user.getUserId() );
        } catch ( Exception e ) {
            Logger.error( this.getClass(), e.getMessage() );
            response.sendError( 401 );
            return;
        }

        if ( null != cmd ) {
            try {
                dispatchMethod = this.getClass().getMethod( cmd, new Class[]{HttpServletRequest.class, HttpServletResponse.class} );
            } catch ( Exception e ) {
                try {
                    dispatchMethod = this.getClass().getMethod( "action", new Class[]{HttpServletRequest.class, HttpServletResponse.class} );
                } catch ( Exception e1 ) {
                    Logger.error( this.getClass(), "Trying to get method:" + cmd );
                    Logger.error( this.getClass(), e1.getMessage(), e1.getCause() );
                    throw new DotRuntimeException( e1.getMessage() );
                }
            }
            try {
                dispatchMethod.invoke( this, request, response );
            } catch ( Exception e ) {
                Logger.error( this.getClass(), "Trying to invoke method:" + cmd );
                Logger.error( this.getClass(), e.getMessage(), e.getCause() );
                throw new DotRuntimeException( e.getMessage() );
            }

            // Clear the principal associated with this thread
            PrincipalThreadLocal.setName( null );
        }

    }

    /**
     * Updates some given basic information to the current company, this method will be call it from the CMS Config portlet
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws JSONException
     */
    public void saveCompanyBasicInfo ( HttpServletRequest request, HttpServletResponse response ) throws IOException, JSONException {

        JSONObject jsonResponse = new JSONObject();

        //Read the parameters
        String portalURL = request.getParameter( "portalURL" );
        String mx = request.getParameter( "mx" );
        String emailAddress = request.getParameter( "emailAddress" );
        String size = request.getParameter( "size" );
        String homeURL = request.getParameter( "homeURL" );
        //Validate the fields
        if ( !validate( request, jsonResponse, "portalURL", "mx", "emailAddress", "size", "homeURL" ) ) {
            response.getWriter().println( jsonResponse.toString() );
            return;
        }

        try {
            //Getting the current company
            Company currentCompany = CompanyManagerUtil.getCompany();

            //Set the values
            currentCompany.setPortalURL( portalURL );
            currentCompany.setMx( mx );
            currentCompany.setEmailAddress( emailAddress );
            currentCompany.setSize( size );
            currentCompany.setHomeURL( homeURL );

            //Update the company
            CompanyManagerUtil.updateCompany( currentCompany );

            //And prepare the response
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", LanguageUtil.get( getUser().getLocale(), "you-have-successfully-updated-the-company-profile" ) );
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error saving basic information for current company.", e );

            jsonResponse.put( "success", false );
            if ( e.getMessage() != null ) {
                jsonResponse.put( "message", e.getMessage() );
            } else {
                jsonResponse.put( "message", "Error saving basic information for current company." );
            }
        }

        response.getWriter().println( jsonResponse.toString() );
    }

    /**
     * Updates some given locale information to the current company, this method will be call it from the CMS Config portlet
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws JSONException
     */
    public void saveCompanyLocaleInfo ( HttpServletRequest request, HttpServletResponse response ) throws IOException, JSONException {

        JSONObject jsonResponse = new JSONObject();

        //Read the parameters
        String languageId = request.getParameter( "languageId" );
        String timeZoneId = request.getParameter( "timeZoneId" );
        //Validate the fields
        if ( !validate( request, jsonResponse, "languageId", "timeZoneId" ) ) {
            response.getWriter().println( jsonResponse.toString() );
            return;
        }

        try {

            //Updating the locale info
            CompanyManagerUtil.updateUsers( languageId, timeZoneId, null, false, false, null );
            TimeZone.setDefault( TimeZone.getTimeZone( timeZoneId ) );

            //And prepare the response
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", LanguageUtil.get( getUser().getLocale(), "you-have-successfully-updated-the-company-profile" ) );
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error saving basic information for current company.", e );

            jsonResponse.put( "success", false );
            if ( e.getMessage() != null ) {
                jsonResponse.put( "message", e.getMessage() );
            } else {
                jsonResponse.put( "message", "Error saving basic information for current company." );
            }
        }

        response.getWriter().println( jsonResponse.toString() );
    }

    /**
     * Updates a given authentication type to the current company, this method will be call it from the CMS Config portlet
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws JSONException
     */
    public void saveCompanyAuthTypeInfo ( HttpServletRequest request, HttpServletResponse response ) throws IOException, JSONException {

        JSONObject jsonResponse = new JSONObject();

        //Read the parameters
        String authType = request.getParameter( "authType" );
        //Validate the fields
        if ( !validate( request, jsonResponse, "authType" ) ) {
            response.getWriter().println( jsonResponse.toString() );
            return;
        }

        try {
            //Getting the current company
            Company currentCompany = CompanyManagerUtil.getCompany();

            //Set the values
            currentCompany.setAuthType( authType );

            //Update the company
            CompanyManagerUtil.updateCompany( currentCompany );

            //And prepare the response
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", LanguageUtil.get( getUser().getLocale(), "you-have-successfully-updated-the-company-profile" ) );
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error saving basic information for current company.", e );

            jsonResponse.put( "success", false );
            if ( e.getMessage() != null ) {
                jsonResponse.put( "message", e.getMessage() );
            } else {
                jsonResponse.put( "message", "Error saving basic information for current company." );
            }
        }

        response.getWriter().println( jsonResponse.toString() );
    }

    /**
     * Deletes a given environment
     *
     * @param request
     * @param response
     * @throws JSONException
     * @throws IOException
     */
    public void deleteEnvironment ( HttpServletRequest request, HttpServletResponse response ) throws JSONException, IOException {

        EnvironmentAPI environmentAPI = APILocator.getEnvironmentAPI();

        JSONObject jsonResponse = new JSONObject();

        //Read the parameters
        String environment = request.getParameter( "environment" );
        //Validate the fields
        if ( !validate( request, jsonResponse, "environment" ) ) {
            response.getWriter().println( jsonResponse.toString() );
            return;
        }

        try {
            //Delete the environment
            environmentAPI.deleteEnvironment( environment );

            //And prepare the response
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", LanguageUtil.get( getUser().getLocale(), "publisher_Environment_deleted" ) );
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error deleting Environment: " + environment, e );

            jsonResponse.put( "success", false );
            if ( e.getMessage() != null ) {
                jsonResponse.put( "message", e.getMessage() );
            } else {
                jsonResponse.put( "message", "Error deleting Environment: " + environment );
            }
        }

        response.getWriter().println( jsonResponse.toString() );
    }

    /**
     * Deletes a given end point
     *
     * @param request
     * @param response
     * @throws JSONException
     * @throws IOException
     */
    public void deleteEndpoint ( HttpServletRequest request, HttpServletResponse response ) throws JSONException, IOException {

        PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();

        JSONObject jsonResponse = new JSONObject();

        //Read the parameters
        String endPoint = request.getParameter( "endPoint" );
        //Validate the fields
        if ( !validate( request, jsonResponse, "endPoint" ) ) {
            response.getWriter().println( jsonResponse.toString() );
            return;
        }

        try {
            //Delete the end point
            pepAPI.deleteEndPointById( endPoint );

            //And prepare the response
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", LanguageUtil.get( getUser().getLocale(), "publisher_End-Point_deleted" ) );
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error deleting End Point: " + endPoint, e );

            jsonResponse.put( "success", false );
            if ( e.getMessage() != null ) {
                jsonResponse.put( "message", e.getMessage() );
            } else {
                jsonResponse.put( "message", "Error deleting End Point: " + endPoint );
            }
        }

        response.getWriter().println( jsonResponse.toString() );
    }

    /**
     * Validates a Collection or string parameters.
     *
     * @param request
     * @param jsonResponse
     * @param args
     * @return True if all the params are present, false otherwise
     * @throws JSONException
     */
    private Boolean validate ( HttpServletRequest request, JSONObject jsonResponse, String... args ) throws JSONException {

        for ( String param : args ) {

            //Validate the given param
            if ( !UtilMethods.isSet( request.getParameter( param ) ) ) {

                //Prepare a proper response
                jsonResponse.put( "success", false );
                jsonResponse.put( "message", "Error: " + param + " is a required Field." );
                return false;
            }
        }

        return true;
    }

}