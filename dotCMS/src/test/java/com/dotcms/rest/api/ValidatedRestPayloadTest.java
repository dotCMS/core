package com.dotcms.rest.api;

import static org.junit.Assert.assertEquals;

import com.dotcms.rest.api.v1.apps.ExportSecretForm;
import com.dotcms.rest.api.v1.company.CompanyLocaleForm;
import com.dotcms.rest.validation.constraints.Operator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Pins the 400 response body that REST endpoints return when a {@link Validated} form fails its
 * bean-validation constraints. The expected messages are the ones dotCMS has always returned, so
 * a change of the validation library must keep every one of them byte-for-byte.
 *
 * <p>Requests go through an in-memory Jersey application with the same JSON provider dotCMS
 * registers, so the payload is read from JSON and the error list is written back as JSON.
 */
public class ValidatedRestPayloadTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static ApplicationHandler handler;
    private static Locale originalLocale;

    /**
     * Pins the default locale to English, because {@link Validated} interpolates messages with
     * {@link Locale#getDefault()} and the expected texts are the English ones.
     */
    @BeforeClass
    public static void startApplication() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        handler = new ApplicationHandler(new ResourceConfig()
                .register(JacksonJaxbJsonProvider.class)
                .register(FormResource.class));
    }

    /** Restores the default locale the JVM had before this class ran. */
    @AfterClass
    public static void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    /** A {@code @NotNull} field without its own message reports the library default text. */
    @Test
    public void notNull_defaultMessage() throws Exception {
        assertBadRequest("notNull", "{}", "may not be null", "value");
    }

    /** A real form whose {@code @NotNull} carries its own message keeps that message. */
    @Test
    public void notNull_customMessage_companyLocaleForm() throws Exception {
        assertBadRequest("companyLocale", "{\"languageId\":\"en_US\"}",
                "timeZoneId is required", "timeZoneId");
    }

    /** A real form whose {@code @Length} carries its own message keeps that message. */
    @Test
    public void length_customMessage_exportSecretForm() throws Exception {
        assertBadRequest("exportSecret", "{\"password\":\"short\",\"exportAll\":true,\"appKeys\":{}}",
                "The password must be a string with a length between 14 and 32", "password");
    }

    /** The default {@code @Length} text interpolates its bounds. */
    @Test
    public void length_defaultMessage() throws Exception {
        assertBadRequest("length", "{\"value\":\"too long\"}", "length must be between 1 and 3", "value");
    }

    /** The default Hibernate {@code @NotBlank} text. */
    @Test
    public void notBlank_defaultMessage() throws Exception {
        assertBadRequest("notBlank", "{\"value\":\"  \"}", "may not be empty", "value");
    }

    /** The default {@code @Min} text interpolates its bound. */
    @Test
    public void min_defaultMessage() throws Exception {
        assertBadRequest("min", "{\"value\":-1}", "must be greater than or equal to 0", "value");
    }

    /** The default {@code @Max} text interpolates its bound. */
    @Test
    public void max_defaultMessage() throws Exception {
        assertBadRequest("max", "{\"value\":11}", "must be less than or equal to 10", "value");
    }

    /** The default {@code @Size} text interpolates its bounds. */
    @Test
    public void size_defaultMessage() throws Exception {
        assertBadRequest("size", "{\"value\":\"abcd\"}", "size must be between 1 and 3", "value");
    }

    /** The default {@code @Pattern} text interpolates the expression. */
    @Test
    public void pattern_defaultMessage() throws Exception {
        assertBadRequest("pattern", "{\"value\":\"ABC\"}", "must match \"[a-z]+\"", "value");
    }

    /** The default {@code @AssertTrue} text. */
    @Test
    public void assertTrue_defaultMessage() throws Exception {
        assertBadRequest("assertTrue", "{\"value\":false}", "must be true", "value");
    }

    /** dotCMS's own {@link Operator} constraint has no bundle entry, so its key is returned as is. */
    @Test
    public void customConstraint_operator() throws Exception {
        assertBadRequest("operator", "{\"value\":\"NOPE\"}",
                "{javax.validation.constraints.Operator.message}", "value");
    }

    /**
     * A message template built by a constraint validator is returned as literal text: expressions
     * in it are not evaluated. The payload value ends up in such templates when a validator echoes
     * the input back, so evaluating them would run caller-supplied expressions.
     */
    @Test
    public void validatorBuiltTemplate_isNotEvaluated() throws Exception {
        assertBadRequest("echo", "{\"value\":\"${1+1}\"}", "rejected ${1+1}", "value");
    }

    /** A valid payload passes. */
    @Test
    public void validPayload_returns200() throws Exception {
        assertEquals(200, post("min", "{\"value\":5}").getStatus());
    }

    private static void assertBadRequest(final String path, final String json,
            final String message, final String fieldName) throws Exception {
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        final ContainerResponse response = post(path, json, body);

        assertEquals(400, response.getStatus());
        final String expected = "[{\"errorCode\":null,\"message\":"
                + MAPPER.writeValueAsString(message) + ",\"fieldName\":\"" + fieldName + "\"}]";
        assertEquals(MAPPER.readTree(expected),
                MAPPER.readTree(body.toString(StandardCharsets.UTF_8)));
    }

    private static ContainerResponse post(final String path, final String json) throws Exception {
        return post(path, json, new ByteArrayOutputStream());
    }

    private static ContainerResponse post(final String path, final String json,
            final ByteArrayOutputStream body) throws Exception {
        final ContainerRequest request = new ContainerRequest(URI.create("http://localhost/"),
                URI.create("http://localhost/forms/" + path), "POST", null,
                new MapPropertiesDelegate(), handler.getConfiguration());
        request.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        request.setEntityStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        return handler.apply(request, body).get();
    }

    /** Validates each form the way dotCMS resources do: by calling {@link Validated#checkValid()}. */
    @Path("/forms")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static class FormResource {

        private static Response check(final Validated form) {
            form.checkValid();
            return Response.ok("{}").build();
        }

        @POST @Path("notNull") public Response notNull(final NotNullForm f) { return check(f); }
        @POST @Path("companyLocale") public Response companyLocale(final CompanyLocaleForm f) { return check(f); }
        @POST @Path("exportSecret") public Response exportSecret(final ExportSecretForm f) { return check(f); }
        @POST @Path("length") public Response length(final LengthForm f) { return check(f); }
        @POST @Path("notBlank") public Response notBlank(final NotBlankForm f) { return check(f); }
        @POST @Path("min") public Response min(final MinForm f) { return check(f); }
        @POST @Path("max") public Response max(final MaxForm f) { return check(f); }
        @POST @Path("size") public Response size(final SizeForm f) { return check(f); }
        @POST @Path("pattern") public Response pattern(final PatternForm f) { return check(f); }
        @POST @Path("assertTrue") public Response assertTrue(final AssertTrueForm f) { return check(f); }
        @POST @Path("operator") public Response operator(final OperatorForm f) { return check(f); }
        @POST @Path("echo") public Response echo(final EchoForm f) { return check(f); }
    }

    public static class NotNullForm extends Validated {
        @NotNull public final String value;
        @JsonCreator public NotNullForm(@JsonProperty("value") final String value) { this.value = value; }
    }

    public static class LengthForm extends Validated {
        @Length(min = 1, max = 3) public final String value;
        @JsonCreator public LengthForm(@JsonProperty("value") final String value) { this.value = value; }
    }

    public static class NotBlankForm extends Validated {
        @NotBlank public final String value;
        @JsonCreator public NotBlankForm(@JsonProperty("value") final String value) { this.value = value; }
    }

    public static class MinForm extends Validated {
        @Min(0) public final int value;
        @JsonCreator public MinForm(@JsonProperty("value") final int value) { this.value = value; }
    }

    public static class MaxForm extends Validated {
        @Max(10) public final int value;
        @JsonCreator public MaxForm(@JsonProperty("value") final int value) { this.value = value; }
    }

    public static class SizeForm extends Validated {
        @Size(min = 1, max = 3) public final String value;
        @JsonCreator public SizeForm(@JsonProperty("value") final String value) { this.value = value; }
    }

    public static class PatternForm extends Validated {
        @Pattern(regexp = "[a-z]+") public final String value;
        @JsonCreator public PatternForm(@JsonProperty("value") final String value) { this.value = value; }
    }

    public static class AssertTrueForm extends Validated {
        @AssertTrue public final boolean value;
        @JsonCreator public AssertTrueForm(@JsonProperty("value") final boolean value) { this.value = value; }
    }

    public static class OperatorForm extends Validated {
        @Operator public final String value;
        @JsonCreator public OperatorForm(@JsonProperty("value") final String value) { this.value = value; }
    }

    public static class EchoForm extends Validated {
        @Echo public final String value;
        @JsonCreator public EchoForm(@JsonProperty("value") final String value) { this.value = value; }
    }

    /** Rejects every value and reports it inside a message template, as some validators do. */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = EchoValidator.class)
    public @interface Echo {
        String message() default "unused";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    public static class EchoValidator implements ConstraintValidator<Echo, String> {
        @Override
        public void initialize(final Echo annotation) {
            // nothing to configure
        }

        @Override
        public boolean isValid(final String value, final ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("rejected " + value).addConstraintViolation();
            return false;
        }
    }
}
