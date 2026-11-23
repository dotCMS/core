package com.dotcms.rest.exception.mapper;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;


/**
 * Maps Jackson's {@link UnrecognizedPropertyException} (an unknown field in a JSON request body)
 * to a clean 400 response. This {@code @Provider} is auto-discovered, so it applies to every write
 * endpoint whose form is deserialized with standard Jackson semantics.
 *
 * <p>The raw {@code exception.getMessage()} is verbose and leaks Java class names and JSON pointer
 * paths. We replace it with a concise, caller-actionable message: the offending property plus the
 * sorted list of valid field names for that form.</p>
 */
@Provider
public class UnrecognizedPropertyExceptionMapper implements ExceptionMapper<UnrecognizedPropertyException> {

    /**
     * Answers a request whose body has a field the target form does not declare. The response
     * names the field and lists the valid ones; the log line names the form only.
     *
     * @param exception the unknown-field failure raised by Jackson
     * @return a 400 response naming the field and the valid field names
     */
    @Override
    public Response toResponse(final UnrecognizedPropertyException exception) {

        //Log into our logs first, without the field name the client sent.
        Logger.warn(this.getClass(), "Rejected request: the body has a field that "
                + (exception.getReferringClass() != null
                        ? exception.getReferringClass().getSimpleName()
                        : "the target form")
                + " does not declare");

        final String message = buildMessage(exception);

        //Creating the message in JSON format.
        final String entity = ExceptionMapperUtil.getJsonErrorAsString(message);

        //Return 4xx message to the client.
        return ExceptionMapperUtil.createResponse(entity, message);
    }

    /**
     * Builds a concise message naming the unrecognized field and listing the valid field names for
     * the target form, e.g.
     * {@code Unrecognized field 'notARealField'. Valid fields are: [body, drawed, theme, title]}.
     */
    private String buildMessage(final UnrecognizedPropertyException exception) {

        final String unknownField = exception.getPropertyName();

        final Collection<Object> knownIds = exception.getKnownPropertyIds();
        final String knownFields = knownIds == null ? "" :
                knownIds.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .sorted()
                        .collect(Collectors.joining(", "));

        final StringBuilder builder = new StringBuilder("Unrecognized field");
        if (UtilMethods.isSet(unknownField)) {
            builder.append(" '").append(unknownField).append('\'');
        }
        builder.append('.');
        if (UtilMethods.isSet(knownFields)) {
            builder.append(" Valid fields are: [").append(knownFields).append(']');
        }
        return builder.toString();
    }
}
