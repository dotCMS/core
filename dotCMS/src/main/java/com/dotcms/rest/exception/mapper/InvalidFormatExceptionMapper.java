package com.dotcms.rest.exception.mapper;

import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import com.dotmarketing.util.Logger;

/**
 * Created by Oscar Arrieta on 8/25/15.
 *
 * Instead of returning 5xx error, we need a way to tell the user is their fault that the Rest call is not working.
 * With this class we catch InvalidFormatExceptions and print our own status error and message in order to
 * alert the user they are sending incorrect data and should fix it before try again.
 */
@Provider
public class InvalidFormatExceptionMapper implements ExceptionMapper<InvalidFormatException> {

    /** Longest field path written to the log. */
    private static final int MAX_PATH_LENGTH = 128;

    /** Characters removed from a field name before it is written to the log. */
    private static final Pattern NAME_FILTER = Pattern.compile("[^A-Za-z0-9_$-]");

    /**
     * Answers a request whose body holds a value that cannot be converted to the field's type.
     * The log line names the field and the target type; the value the client sent is left out.
     *
     * @param exception the conversion failure raised by Jackson
     * @return a 400 response describing the value and the expected type
     */
    @Override
    public Response toResponse(final InvalidFormatException exception)
    {
        //Log into our logs first: the field and the target type, never the value.
        final String fieldPath = fieldPath(exception.getPath());
        Logger.warn(this.getClass(), "Rejected request: "
                + (fieldPath.isEmpty() ? "a body value" : "body field '" + fieldPath + "'")
                + " could not be converted to "
                + (exception.getTargetType() != null
                        ? exception.getTargetType().getSimpleName()
                        : "the expected type"));

        //Create the message.
        String message = "Can not construct instance from value '" + exception.getValue()
                        + "': not a valid " + exception.getTargetType() + " value";

        //Creating the message in JSON format.
        String entity = ExceptionMapperUtil.getJsonErrorAsString(message);

        //Return 4xx message to the client.
        return ExceptionMapperUtil.createResponse(entity, message);
    }

    /**
     * Builds the path to the field that failed, such as {@code items[1].count}. Field names come
     * from the target form; list positions are written as {@code [n]}; map entries are written as
     * {@code [key]} because the key is whatever the client sent. Names are reduced to letters,
     * digits, {@code _}, {@code $} and {@code -}, and the path is cut at {@value #MAX_PATH_LENGTH}
     * characters.
     *
     * @param path the references Jackson collected while reading the body, outermost first
     * @return the field path, or an empty string when there is none
     */
    private static String fieldPath(final List<Reference> path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        for (final Reference reference : path) {
            if (reference.getFrom() instanceof Map) {
                builder.append("[key]");
            } else if (reference.getFieldName() != null) {
                if (builder.length() > 0) {
                    builder.append('.');
                }
                builder.append(NAME_FILTER.matcher(reference.getFieldName()).replaceAll(""));
            } else if (reference.getIndex() >= 0) {
                builder.append('[').append(reference.getIndex()).append(']');
            }
        }
        return builder.length() > MAX_PATH_LENGTH
                ? builder.substring(0, MAX_PATH_LENGTH) + "..."
                : builder.toString();
    }
}
