package com.dotcms.rest.exception.mapper;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.workflows.business.WorkflowPortletAccessException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dotcms.exception.ExceptionUtil.ValidationError;
import static com.dotcms.exception.ExceptionUtil.getRootCause;
import static com.dotcms.exception.ExceptionUtil.mapValidationException;

/**
 * Created by Oscar Arrieta on 8/27/15.
 *
 * Class to abstract methods that will be used in Mapper Exception classes on dotCMS.
 */
public final class ExceptionMapperUtil {

    public static final String ACCESS_CONTROL_HEADER_INVALID_LICENSE = "Invalid-License";
    public static final String ACCESS_CONTROL_HEADER_PORTLET_ACCESS_DENIED = "Portlet-Access-Denied";
    public static final String ACCESS_CONTROL_HEADER_PERMISSION_VIOLATION = "Permission-Violation";
    public static final String ACCESS_CONTROL_HEADER_OK = "OK";

    /**
     *
     * @param message error message to include in the JSON.
     * @return string with the Json formed in this format: {error:message}.
     */
    public static String getJsonErrorAsString(String message){

        //Creating the message in JSON format.
        String entity;
        try {
            JSONObject json = new JSONObject();
            json.put("error", message);
            entity = json.toString();
        } catch (JSONException e) {
            entity = "{ \"error\": \"" + message.replace("\"", "\\\"") + "\" }";
        }
        return entity;
    }

    /**
     * Format the exception message and field as a json response
     * @param field String field that has the error
     * @param message String error message to include in the JSON.
     * @return string with the Json formed in this format: {error:message}.
     */
    public static String getJsonErrorAsString(final String field, final String message){

        //Creating the message in JSON format.
        String entity;
        try {
            JSONObject json = new JSONObject();
            json.put("field", field);
            json.put("error", message);
            entity = json.toString();
        } catch (JSONException e) {
            entity = "{ \"error\": \"" + message.replace("\"", "\\\"") + "\" }";
        }
        return entity;
    }

    /**
     *
     * @param entity JSON as String.
     * @return Response with Status 400 and Media Type JSON.
     */
    public static Response createResponse(final String entity, final String message){

        //Return 4xx message to the client.
        return createResponse(entity, message, Response.Status.BAD_REQUEST);
    }

    /**
     * Creates an error response with a specific status.
     * @param entity JSON as String.
     * @return Response with Status given in the parameter and Media Type JSON.
     */
    public static Response createResponse(final Object entity,
                                          final String message,
                                          final Response.Status status){

        return Response
                .status(status)
                .entity(entity)
                .header("error-message", getI18NMessage(message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static String getI18NMessage (final String message) {

        String i18nmessage = message;
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if(null != request){
            try{
                final User user = WebAPILocator.getUserWebAPI().getUser(request);
                i18nmessage = LanguageUtil.get(user, message);
            }catch (Exception e){
                Logger.debug(ExceptionMapperUtil.class,e.getMessage(),e);
            }
        }

        return i18nmessage;
    }

    /**
     * Creates an error response with a specific status.
     * @param entity JSON as String.
     * @return Response with Status 400 and Media Type JSON.
     */
    public static Response createResponse(final String entity,
                                          final String message,
                                          final Response.Status status){

        return Response
                .status(status)
                .entity(Map.of("message", message))
                .header("error-message", message)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /***
     * Creates an response based on a status and exception
     * @param exception {@link Exception}
     * @param status {@link Response}
     * @return Response
     */
    public static Response  createResponse(final Throwable exception, final Response.Status status){

        return createResponse(exception, null, status);
    }

    /***
     * Creates an response based on a status and exception
     * @param exception {@link Exception}
     * @param status {@link Response}
     * @return Response
     */
    public static Response  createResponse(final Throwable exception, final String key, final Response.Status status){
        //Create the message.

        if (exception instanceof WebApplicationException) {

            return WebApplicationException.class.cast(exception).getResponse();
        }

        final String message = getI18NMessage(exception.getMessage()); // todo: this must be switchable by osgi plugin, also the  error must be returned as ResponseEntityView

        //Creating the message in JSON format.
        if (ConfigUtils.isDevMode()) {

            final StringWriter errors = new StringWriter();
            exception.printStackTrace(new PrintWriter(errors));

            final Map<String, Object> entityMap = new HashMap<>();
            entityMap.put("message", message);
            entityMap.put("stacktrace", errors);
            return Response
                    .status(status)
                    .entity(entityMap)
                    .header("error-key", key)
                    .header("access-control", getAccessControlHeader(exception))
                    .build();
        }

        final Map<String, Object> entityMap = new HashMap<>();
        entityMap.put("message", message);

        return Response
                .status(status)
                .entity(entityMap)
                .header("error-key", key)
                .header("access-control", getAccessControlHeader(exception))
                .build();
    }

    public static Response  createResponse(final Response.Status status){
        return Response
                .status(status)
                .build();
    }

    /**
     * Build a response extracting the info from the Content validation exception
     * @param status
     * @param ve
     * @return
     */
    public static Response createResponse(final Response.Status status,
            final DotContentletValidationException ve) {
        final List<ErrorEntity> errorEntities = new ArrayList<>();
        try {
            final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
            final User user = (request == null ? APILocator.systemUser() : WebAPILocator.getUserWebAPI().getUser(request));
            final Map<String, List<ValidationError>> contentValidationErrors =
                    mapValidationException(user, ve);

            contentValidationErrors.forEach((k, errors)
                    -> {
                for (ValidationError e :errors) {
                    errorEntities.add(new ErrorEntity(k, e.getMessage(), e.getField()));
                }
            });
        } catch (Exception e) {
            Logger.debug(ExceptionMapperUtil.class, e.getMessage(), e);
        }
        return Response.status(status).entity(new ResponseEntityView(errorEntities))
                .type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * Translates any exception related to security or permissions transgression into a header
     * @param e
     * @return
     */
    private static String getAccessControlHeader(final Throwable e){

        final Throwable rootCause = getRootCause(e);

        if(e instanceof InvalidLicenseException  || rootCause instanceof InvalidLicenseException ){
           return ACCESS_CONTROL_HEADER_INVALID_LICENSE;
        }

        if(e instanceof WorkflowPortletAccessException || rootCause instanceof WorkflowPortletAccessException ){
           return ACCESS_CONTROL_HEADER_PORTLET_ACCESS_DENIED;
        }

        if(e instanceof DotSecurityException || rootCause instanceof DotSecurityException ){
            return ACCESS_CONTROL_HEADER_PERMISSION_VIOLATION;
        }

        return null;
    }
}
