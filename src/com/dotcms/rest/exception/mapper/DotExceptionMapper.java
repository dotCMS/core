package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

/**
 * Created by Oscar Arrieta on 8/27/15.
 *
 * Class to abstract methods that will be used in Mapper Exception classes on dotCMS.
 */
public class DotExceptionMapper {

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
    public static Response createResponse(String entity){

        //Return 4xx message to the client.
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(entity)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
