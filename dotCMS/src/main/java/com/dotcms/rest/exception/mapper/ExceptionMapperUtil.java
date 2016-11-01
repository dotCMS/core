package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

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
    public static Response createResponse(String entity, String message){

        //Return 4xx message to the client.
        return Response
                .status(Response.Status.BAD_REQUEST)
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
        final String message = exception.getMessage();
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
}
