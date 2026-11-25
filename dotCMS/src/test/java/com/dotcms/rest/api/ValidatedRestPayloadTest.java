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
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.MessageInterpolator;
import javax.validation.Payload;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Pins the 400 response body that REST endpoints return when a {@link Validated} form fails its
 * bean-validation constraints. The expected messages are the ones dotCMS has always returned, so
 * a change of the validation library must keep every one of them byte-for-byte.
 *
 * <p>Each payload is read with the same JSON provider dotCMS registers, validated the way dotCMS
 * resources do (by calling {@link Validated#checkValid()}), and the response carried by the thrown
 * exception is written back as JSON with that provider, which is what JAX-RS does with a
 * {@link WebApplicationException} that carries an entity.
 */
public class ValidatedRestPayloadTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JacksonJaxbJsonProvider PROVIDER = new JacksonJaxbJsonProvider();
    private static Locale originalLocale;

    /**
     * Pins the default locale to English, because {@link Validated} interpolates messages with
     * {@link Locale#getDefault()} and the expected texts are the English ones.
     */
    @BeforeClass
    public static void pinLocale() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
    }

    /** Restores the default locale the JVM had before this class ran. */
    @AfterClass
    public static void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    /** A {@code @NotNull} field without its own message reports the library default text. */
    @Test
    public void notNull_defaultMessage() throws Exception {
        assertBadRequest(NotNullForm.class, "{}", "may not be null", "value");
    }

    /**
     * The "may not be null" wording is an English override, so a deployment running in another
     * language keeps the library's translation of the {@code @NotNull} default.
     */
    @Test
    public void notNull_defaultMessage_isTranslatedForOtherLocales() {
        final Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.GERMAN);
        try {
            final MessageInterpolator german = new ResourceBundleMessageInterpolator() {
                @Override
                public String interpolate(final String template, final Context context) {
                    return interpolate(template, context, Locale.GERMAN);
                }
            };
            final Validator validator = Validation.byDefaultProvider().configure()
                    .messageInterpolator(german).buildValidatorFactory().getValidator();
            assertEquals("darf nicht null sein",
                    validator.validate(new NotNullForm(null)).iterator().next().getMessage());
        } finally {
            Locale.setDefault(previous);
        }
    }

    /** A real form whose {@code @NotNull} carries its own message keeps that message. */
    @Test
    public void notNull_customMessage_companyLocaleForm() throws Exception {
        assertBadRequest(CompanyLocaleForm.class, "{\"languageId\":\"en_US\"}",
                "timeZoneId is required", "timeZoneId");
    }

    /** A real form whose {@code @Length} carries its own message keeps that message. */
    @Test
    public void length_customMessage_exportSecretForm() throws Exception {
        assertBadRequest(ExportSecretForm.class, "{\"password\":\"short\",\"exportAll\":true,\"appKeys\":{}}",
                "The password must be a string with a length between 14 and 32", "password");
    }

    /** The default {@code @Length} text interpolates its bounds. */
    @Test
    public void length_defaultMessage() throws Exception {
        assertBadRequest(LengthForm.class, "{\"value\":\"too long\"}", "length must be between 1 and 3", "value");
    }

    /** The default Hibernate {@code @NotBlank} text. */
    @Test
    public void notBlank_defaultMessage() throws Exception {
        assertBadRequest(NotBlankForm.class, "{\"value\":\"  \"}", "may not be empty", "value");
    }

    /** The default {@code @Min} text interpolates its bound. */
    @Test
    public void min_defaultMessage() throws Exception {
        assertBadRequest(MinForm.class, "{\"value\":-1}", "must be greater than or equal to 0", "value");
    }

    /** The default {@code @Max} text interpolates its bound. */
    @Test
    public void max_defaultMessage() throws Exception {
        assertBadRequest(MaxForm.class, "{\"value\":11}", "must be less than or equal to 10", "value");
    }

    /** The default {@code @Size} text interpolates its bounds. */
    @Test
    public void size_defaultMessage() throws Exception {
        assertBadRequest(SizeForm.class, "{\"value\":\"abcd\"}", "size must be between 1 and 3", "value");
    }

    /** The default {@code @Pattern} text interpolates the expression. */
    @Test
    public void pattern_defaultMessage() throws Exception {
        assertBadRequest(PatternForm.class, "{\"value\":\"ABC\"}", "must match \"[a-z]+\"", "value");
    }

    /** The default {@code @AssertTrue} text. */
    @Test
    public void assertTrue_defaultMessage() throws Exception {
        assertBadRequest(AssertTrueForm.class, "{\"value\":false}", "must be true", "value");
    }

    /** dotCMS's own {@link Operator} constraint has no bundle entry, so its key is returned as is. */
    @Test
    public void customConstraint_operator() throws Exception {
        assertBadRequest(OperatorForm.class, "{\"value\":\"NOPE\"}",
                "{javax.validation.constraints.Operator.message}", "value");
    }

    /**
     * A message template built by a constraint validator is returned as literal text: expressions
     * in it are not evaluated. The payload value ends up in such templates when a validator echoes
     * the input back, so evaluating them would run caller-supplied expressions.
     */
    @Test
    public void validatorBuiltTemplate_isNotEvaluated() throws Exception {
        assertBadRequest(EchoForm.class, "{\"value\":\"${1+1}\"}", "rejected ${1+1}", "value");
    }

    /** A valid payload passes. */
    @Test
    public void validPayload_returns200() throws Exception {
        assertEquals(200, submit(MinForm.class, "{\"value\":5}").getStatus());
    }

    private static void assertBadRequest(final Class<? extends Validated> formClass, final String json,
            final String message, final String fieldName) throws Exception {
        final Response response = submit(formClass, json);
        assertEquals(400, response.getStatus());

        final Object entity = response.getEntity();
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        PROVIDER.writeTo(entity, entity.getClass(), entity.getClass(), new Annotation[0],
                MediaType.APPLICATION_JSON_TYPE, new MultivaluedHashMap<>(), body);

        final String expected = "[{\"errorCode\":null,\"message\":"
                + MAPPER.writeValueAsString(message) + ",\"fieldName\":\"" + fieldName + "\"}]";
        assertEquals(MAPPER.readTree(expected),
                MAPPER.readTree(body.toString(StandardCharsets.UTF_8)));
    }

    /**
     * Reads the JSON payload into the form and validates it, returning the response a resource
     * would send: 200 when the form is valid, otherwise the response carried by the exception.
     */
    @SuppressWarnings("unchecked")
    private static Response submit(final Class<? extends Validated> formClass, final String json)
            throws Exception {
        final Validated form = (Validated) PROVIDER.readFrom((Class<Object>) (Class<?>) formClass,
                formClass, new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                new MultivaluedHashMap<>(),
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        try {
            form.checkValid();
            return Response.ok().build();
        } catch (final WebApplicationException e) {
            return e.getResponse();
        }
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
