package com.dotcms.rest.api.v1.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotmarketing.util.Config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * Unit tests for {@link ConfigurationResource}.
 *
 * <p>This class is the single home for all unit-level assertions about
 * {@code ConfigurationResource}. Add new test methods here as new behaviour
 * is introduced;
 *
 * <p>Integration-level tests (requiring a running dotCMS instance) live in
 * {@code dotcms-integration/.../ConfigurationResourceTest.java}.
 *
 */
public class ConfigurationResourceTest {

    private static final String FLAG = FeatureFlagName.FEATURE_FLAG_UVE_TOGGLE_LOCK;
    private static final String UNDEFINED_FLAG = FeatureFlagName.FEATURE_FLAG_UVE_STYLE_EDITOR;
    private static final String NON_FLAG_KEY = "EMAIL_SYSTEM_ADDRESS";
    private static final String UNLISTED_KEY = "SOME_INTERNAL_SECRET";

    private WebResource webResource;
    private ConfigurationResource resource;
    private HttpServletRequest request;
    private HttpServletResponse response;

    // ── Setup / teardown ──────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        webResource = mock(WebResource.class);
        resource = new ConfigurationResource(webResource);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        final InitDataObject initData = mock(InitDataObject.class);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initData);
    }

    // ── Truthy variants (AC: "true", "True", "TRUE", " true ", "1" → true) ──

    /**
     * Method to test: {@link ConfigurationResource#getConfigVariables}
     * Given scenario: A boolean feature flag is set to lowercase {@code "true"}.
     * Expected result: Response contains native boolean {@code true} (not the string).
     */
    @Test
    void getConfigVariables_flagSetToLowercaseTrue_returnsNativeBooleanTrue() {
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(FLAG, null)).thenReturn("true");

            final Map<String, Object> entity = entityMap(resource.getConfigVariables(request, response, FLAG));

            assertEquals(Boolean.TRUE, entity.get(FLAG));
        }
    }

    /**
     * Method to test: {@link ConfigurationResource#getConfigVariables}
     * Given scenario: A boolean feature flag is set to capitalized {@code "True"} —
     *   the customer-reported reproduction case.
     * Expected result: Response contains native boolean {@code true}.
     */
    @Test
    void getConfigVariables_flagSetToCapitalizedTrue_returnsNativeBooleanTrue() {
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(FLAG, null)).thenReturn("True");

            final Map<String, Object> entity = entityMap(resource.getConfigVariables(request, response, FLAG));

            assertEquals(Boolean.TRUE, entity.get(FLAG));
        }
    }

    /**
     * Method to test: {@link ConfigurationResource#getConfigVariables}
     * Given scenario: A boolean feature flag is set to all-caps {@code "TRUE"}.
     * Expected result: Response contains native boolean {@code true}.
     */
    @Test
    void getConfigVariables_flagSetToUppercaseTrue_returnsNativeBooleanTrue() {
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(FLAG, null)).thenReturn("TRUE");

            final Map<String, Object> entity = entityMap(resource.getConfigVariables(request, response, FLAG));

            assertEquals(Boolean.TRUE, entity.get(FLAG));
        }
    }

    /**
     * Method to test: {@link ConfigurationResource#getConfigVariables}
     * Given scenario: A boolean feature flag is set to {@code " true "} with surrounding whitespace.
     * Expected result: Response contains native boolean {@code true} — whitespace is trimmed.
     */
    @Test
    void getConfigVariables_flagSetToTrueWithWhitespace_returnsNativeBooleanTrue() {
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(FLAG, null)).thenReturn(" true ");

            final Map<String, Object> entity = entityMap(resource.getConfigVariables(request, response, FLAG));

            assertEquals(Boolean.TRUE, entity.get(FLAG));
        }
    }

    /**
     * Method to test: {@link ConfigurationResource#getConfigVariables}
     * Given scenario: A boolean feature flag is set to the numeric shorthand {@code "1"}.
     * Expected result: Response contains native boolean {@code true}.
     */
    @Test
    void getConfigVariables_flagSetToNumericOne_returnsNativeBooleanTrue() {
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(FLAG, null)).thenReturn("1");

            final Map<String, Object> entity = entityMap(resource.getConfigVariables(request, response, FLAG));

            assertEquals(Boolean.TRUE, entity.get(FLAG));
        }
    }

    // ── Falsy variants (AC: "false", "False", "FALSE", " false ", "0", "" → false) ──

    /**
     * Method to test: {@link ConfigurationResource#getConfigVariables}
     * Given scenario: A boolean feature flag is set to lowercase {@code "false"}.
     * Expected result: Response contains native boolean {@code false}.
     */
    @Test
    void getConfigVariables_flagSetToLowercaseFalse_returnsNativeBooleanFalse() {
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(FLAG, null)).thenReturn("false");

            final Map<String, Object> entity = entityMap(resource.getConfigVariables(request, response, FLAG));

            assertEquals(Boolean.FALSE, entity.get(FLAG));
        }
    }

    /**
     * Method to test: {@link ConfigurationResource#getConfigVariables}
     * Given scenario: A boolean feature flag is set to all-caps {@code "FALSE"}.
     * Expected result: Response contains native boolean {@code false}.
     */
    @Test
    void getConfigVariables_flagSetToUppercaseFalse_returnsNativeBooleanFalse() {
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(FLAG, null)).thenReturn("FALSE");

            final Map<String, Object> entity = entityMap(resource.getConfigVariables(request, response, FLAG));

            assertEquals(Boolean.FALSE, entity.get(FLAG));
        }
    }

    /**
     * Method to test: {@link ConfigurationResource#getConfigVariables}
     * Given scenario: A boolean feature flag is set to the numeric shorthand {@code "0"}.
     * Expected result: Response contains native boolean {@code false}.
     */
    @Test
    void getConfigVariables_flagSetToNumericZero_returnsNativeBooleanFalse() {
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(FLAG, null)).thenReturn("0");

            final Map<String, Object> entity = entityMap(resource.getConfigVariables(request, response, FLAG));

            assertEquals(Boolean.FALSE, entity.get(FLAG));
        }
    }

    /**
     * Method to test: {@link ConfigurationResource#getConfigVariables}
     * Given scenario: A boolean feature flag is set to an empty string.
     * Expected result: Response contains native boolean {@code false}.
     */
    @Test
    void getConfigVariables_flagSetToEmptyString_returnsNativeBooleanFalse() {
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(FLAG, null)).thenReturn("");

            final Map<String, Object> entity = entityMap(resource.getConfigVariables(request, response, FLAG));

            assertEquals(Boolean.FALSE, entity.get(FLAG));
        }
    }

    // ── NOT_FOUND sentinel ────────────────────────────────────────────────────

    /**
     * Method to test: {@link ConfigurationResource#getConfigVariables}
     * Given scenario: A boolean feature flag key is not defined anywhere on the server
     *   ({@code Config.getStringProperty} returns {@code null}).
     * Expected result: Response contains the literal string {@code "NOT_FOUND"}, never a boolean —
     *   the frontend uses this sentinel to apply its own enabled-by-default opt-out logic.
     */
    @Test
    void getConfigVariables_flagNotDefined_returnsNotFoundSentinel() {
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(UNDEFINED_FLAG, null)).thenReturn(null);

            final Map<String, Object> entity = entityMap(
                    resource.getConfigVariables(request, response, UNDEFINED_FLAG));

            assertEquals("NOT_FOUND", entity.get(UNDEFINED_FLAG));
        }
    }

    // ── Unrecognised value ────────────────────────────────────────────────────

    /**
     * Method to test: {@link ConfigurationResource#getConfigVariables}
     * Given scenario: A boolean feature flag has an unrecognized value such as {@code "enabled"}.
     * Expected result: Response contains native boolean {@code false} — safe default preserved.
     */
    @Test
    void getConfigVariables_flagSetToUnrecognizedValue_returnsNativeBooleanFalse() {
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(FLAG, null)).thenReturn("enabled");

            final Map<String, Object> entity = entityMap(resource.getConfigVariables(request, response, FLAG));

            assertEquals(Boolean.FALSE, entity.get(FLAG));
        }
    }

    // ── Whitelist filtering ───────────────────────────────────────────────────

    /**
     * Method to test: {@link ConfigurationResource#getConfigVariables}
     * Given scenario: The caller requests a key that is not on the whitelist.
     * Expected result: The key is absent from the response — it is silently excluded.
     */
    @Test
    void getConfigVariables_keyNotOnWhitelist_isExcludedFromResponse() {
        final Map<String, Object> entity = entityMap(
                resource.getConfigVariables(request, response, UNLISTED_KEY));

        assertFalse(entity.containsKey(UNLISTED_KEY));
    }

    // ── Non-boolean whitelisted key ───────────────────────────────────────────

    /**
     * Method to test: {@link ConfigurationResource#getConfigVariables}
     * Given scenario: The caller requests a whitelisted key that is not a boolean feature flag
     *   (e.g. {@code EMAIL_SYSTEM_ADDRESS}).
     * Expected result: The raw string value is returned unchanged — boolean coercion is not applied
     *   to non-flag keys.
     */
    @Test
    void getConfigVariables_nonFlagWhitelistedKey_returnsRawStringValue() {
        final String testAddress = "admin@example.com";
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(NON_FLAG_KEY, "NOT_FOUND")).thenReturn(testAddress);

            final Map<String, Object> entity = entityMap(
                    resource.getConfigVariables(request, response, NON_FLAG_KEY));

            assertEquals(testAddress, entity.get(NON_FLAG_KEY));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entityMap(final Response response) {
        return (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity();
    }
}
