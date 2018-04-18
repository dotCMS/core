package com.dotcms.rest.exception.mapper;

import static com.dotcms.exception.ExceptionUtil.ValidationError;
import static com.dotcms.exception.ExceptionUtil.mapValidationException;
import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by Oscar Arrieta on 8/27/15.
 *
 * Class to abstract methods that will be used in Mapper Exception classes on dotCMS.
 */
public final class ExceptionMapperUtil {

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
                .entity(entity)
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
    public static Response  createResponse(final Exception exception, final Response.Status status){
        //Create the message.
        final String message = getI18NMessage(exception.getMessage());

        //Creating the message in JSON format.
        if (ConfigUtils.isDevMode()) {

            final StringWriter errors = new StringWriter();
            exception.printStackTrace(new PrintWriter(errors));

            return Response
                    .status(status)
                    .entity(map("message", message,
                            "stacktrace", errors))
                    .header("error-message", message)
                    .build();
        }

        return Response
                .status(status)
                .entity(map("message", message))
                .header("error-message", message)
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
            final User user = WebAPILocator.getUserWebAPI().getUser(request);
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
}
