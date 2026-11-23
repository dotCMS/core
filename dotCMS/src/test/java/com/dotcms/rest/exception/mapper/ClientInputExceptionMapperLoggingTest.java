package com.dotcms.rest.exception.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.glassfish.jersey.server.ParamException;
import org.junit.jupiter.api.Test;

/**
 * Covers what the exception mappers for malformed request input write to the log: one warning that
 * names the parameter or the target type, without the value the client sent and without the stack
 * trace. The response the client gets is unchanged.
 */
class ClientInputExceptionMapperLoggingTest {

    private static final String CLIENT_TEXT = "1\r\nclient-text-marker";
    private static final String MARKER = "client-text-marker";
    /** {@link #CLIENT_TEXT} as it is written inside a JSON string. */
    private static final String JSON_CLIENT_TEXT = "1\\r\\nclient-text-marker";

    /**
     * Given a query parameter whose value cannot be converted to the declared type,
     * When the mapper answers the request,
     * Then it logs one warning that names the parameter, without the value or the stack trace,
     * and still answers 400.
     */
    @Test
    void paramMapper_logsParameterNameOnly() {
        final ParamException exception = new ParamException.QueryParamException(
                new NumberFormatException("For input string: \"" + CLIENT_TEXT + "\""), "page", null);

        final CapturedLog log = capture(ParamExceptionMapper.class,
                () -> new ParamExceptionMapper().toResponse(exception));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), log.response.getStatus());
        final LogEvent warning = log.singleWarning();
        assertTrue(warning.getMessage().getFormattedMessage().contains("page"),
                () -> "warning: " + warning.getMessage().getFormattedMessage());
        assertLogsNoClientText(warning);
    }

    /**
     * Given a request body that is not valid JSON,
     * When the mapper answers the request,
     * Then it logs one warning without the body text or the stack trace, and still answers 400.
     */
    @Test
    void jsonParseMapper_logsWithoutBodyText() throws IOException {
        final JsonParseException exception = new JsonParseException(parser(),
                "Unexpected character: " + CLIENT_TEXT);

        final CapturedLog log = capture(JsonParseExceptionMapper.class,
                () -> new JsonParseExceptionMapper().toResponse(exception));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), log.response.getStatus());
        assertLogsNoClientText(log.singleWarning());
    }

    /**
     * Given a body value that cannot be converted to the field's type,
     * When the mapper answers the request,
     * Then it logs one warning that names the target type, without the value or the stack trace,
     * and still answers 400.
     */
    @Test
    void invalidFormatMapper_logsTargetTypeOnly() throws IOException {
        final InvalidFormatException exception = InvalidFormatException.from(parser(),
                "Cannot deserialize value from \"" + CLIENT_TEXT + "\"", CLIENT_TEXT, Integer.class);

        final CapturedLog log = capture(InvalidFormatExceptionMapper.class,
                () -> new InvalidFormatExceptionMapper().toResponse(exception));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), log.response.getStatus());
        final LogEvent warning = log.singleWarning();
        assertTrue(warning.getMessage().getFormattedMessage().contains("Integer"),
                () -> "warning: " + warning.getMessage().getFormattedMessage());
        assertLogsNoClientText(warning);
    }

    /**
     * Given a JSON body whose field holds a value that cannot be converted to the field's type,
     * When the mapper answers the request,
     * Then the warning names the field and the target type, without the value or the stack trace.
     */
    @Test
    void invalidFormatMapper_namesTheField() {
        final InvalidFormatException exception = readInvalid(
                "{\"count\":\"" + JSON_CLIENT_TEXT + "\"}", CountForm.class);

        final CapturedLog log = capture(InvalidFormatExceptionMapper.class,
                () -> new InvalidFormatExceptionMapper().toResponse(exception));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), log.response.getStatus());
        final LogEvent warning = log.singleWarning();
        final String text = warning.getMessage().getFormattedMessage();
        assertTrue(text.contains("'count'") && text.contains("Integer"), () -> "warning: " + text);
        assertLogsNoClientText(warning);
    }

    /**
     * Given a nested JSON body whose inner field holds a value of the wrong type,
     * When the mapper answers the request,
     * Then the warning names the full path to the field, list positions included.
     */
    @Test
    void invalidFormatMapper_namesTheNestedFieldPath() {
        final InvalidFormatException exception = readInvalid(
                "{\"items\":[{\"count\":1},{\"count\":\"" + JSON_CLIENT_TEXT + "\"}]}",
                ItemsForm.class);

        final CapturedLog log = capture(InvalidFormatExceptionMapper.class,
                () -> new InvalidFormatExceptionMapper().toResponse(exception));

        final LogEvent warning = log.singleWarning();
        final String text = warning.getMessage().getFormattedMessage();
        assertTrue(text.contains("'items[1].count'"), () -> "warning: " + text);
        assertLogsNoClientText(warning);
    }

    /**
     * Given a JSON body with a map whose entry holds a value of the wrong type,
     * When the mapper answers the request,
     * Then the warning names the map field but not the entry key, which the client chose.
     */
    @Test
    void invalidFormatMapper_leavesMapKeysOut() {
        final InvalidFormatException exception = readInvalid(
                "{\"counts\":{\"" + JSON_CLIENT_TEXT + "\":\"x\"}}", CountsForm.class);

        final CapturedLog log = capture(InvalidFormatExceptionMapper.class,
                () -> new InvalidFormatExceptionMapper().toResponse(exception));

        final LogEvent warning = log.singleWarning();
        final String text = warning.getMessage().getFormattedMessage();
        assertTrue(text.contains("'counts["), () -> "warning: " + text);
        assertLogsNoClientText(warning);
    }

    /**
     * Given a body that cannot be turned into the target object,
     * When the mapper answers the request,
     * Then it logs one warning without the body text or the stack trace, and still answers 400.
     */
    @Test
    void valueInstantiationMapper_logsWithoutBodyText() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final ValueInstantiationException exception = ValueInstantiationException.from(parser(),
                "Cannot construct instance: " + CLIENT_TEXT, mapper.constructType(SampleForm.class),
                new IllegalArgumentException(CLIENT_TEXT));

        final CapturedLog log = capture(JsonMappingExceptionMapper.class,
                () -> new JsonMappingExceptionMapper().toResponse(exception));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), log.response.getStatus());
        assertLogsNoClientText(log.singleWarning());
    }

    /**
     * Given a body with a field the target form does not declare,
     * When the mapper answers the request,
     * Then it logs one warning without the field name or the stack trace, and the response still
     * names the field.
     */
    @Test
    void unrecognizedPropertyMapper_logsWithoutFieldName() throws IOException {
        final UnrecognizedPropertyException exception = UnrecognizedPropertyException.from(parser(),
                SampleForm.class, CLIENT_TEXT, List.of("title"));

        final CapturedLog log = capture(UnrecognizedPropertyExceptionMapper.class,
                () -> new UnrecognizedPropertyExceptionMapper().toResponse(exception));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), log.response.getStatus());
        assertTrue(String.valueOf(log.response.getEntity()).contains(MARKER));
        assertLogsNoClientText(log.singleWarning());
    }

    private static void assertLogsNoClientText(final LogEvent warning) {
        final String text = warning.getMessage().getFormattedMessage();
        assertFalse(text.contains(MARKER), () -> "warning: " + text);
        assertFalse(text.contains("\n") || text.contains("\r"), () -> "warning: " + text);
        assertNull(warning.getThrown(), "the warning carries no stack trace");
    }

    private static InvalidFormatException readInvalid(final String json, final Class<?> type) {
        try {
            new ObjectMapper().readValue(json, type);
        } catch (final InvalidFormatException e) {
            return e;
        } catch (final IOException e) {
            throw new AssertionError("expected a conversion failure, got " + e, e);
        }
        throw new AssertionError("expected a conversion failure for " + json);
    }

    private static JsonParser parser() throws IOException {
        return new ObjectMapper().getFactory().createParser("{}");
    }

    private static CapturedLog capture(final Class<?> mapperClass, final Supplier<Response> call) {
        final Logger logger = (Logger) LogManager.getLogger(mapperClass);
        final Level previousLevel = logger.getLevel();
        final CapturingAppender appender = new CapturingAppender();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            final Response response = call.get();
            return new CapturedLog(response, appender.events());
        } finally {
            logger.removeAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }

    /** Target type used by the body-conversion cases. */
    static final class SampleForm {
        public String title;
    }

    /** Form with a single numeric field. */
    static final class CountForm {
        public Integer count;
    }

    /** Form with a list of nested forms. */
    static final class ItemsForm {
        public List<CountForm> items;
    }

    /** Form with a map of numbers keyed by client-chosen names. */
    static final class CountsForm {
        public Map<String, Integer> counts;
    }

    private static final class CapturedLog {

        private final Response response;
        private final List<LogEvent> events;

        private CapturedLog(final Response response, final List<LogEvent> events) {
            this.response = response;
            this.events = events;
        }

        private LogEvent singleWarning() {
            final List<LogEvent> warnings = new ArrayList<>();
            for (final LogEvent event : events) {
                if (event.getLevel().isMoreSpecificThan(Level.WARN)) {
                    warnings.add(event);
                }
            }
            assertEquals(1, warnings.size(), () -> "warnings: " + warnings.size());
            return warnings.get(0);
        }
    }

    private static final class CapturingAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        private CapturingAppender() {
            super("ClientInputExceptionMapperLoggingTest-appender", null, null, true,
                    Property.EMPTY_ARRAY);
        }

        @Override
        public synchronized void append(final LogEvent event) {
            events.add(event.toImmutable());
        }

        private synchronized List<LogEvent> events() {
            return new ArrayList<>(events);
        }
    }
}
