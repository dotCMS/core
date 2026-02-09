package com.dotcms.jitsu.validators;

import com.dotcms.IntegrationTestBase;
import com.dotcms.JUnit4WeldRunner;
import com.dotcms.LicenseTestUtil;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.util.HttpHeaders;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import javax.enterprise.context.Dependent;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link AnalyticsValidatorUtil} class is able to properly validate the most
 * important attributes that are passed down when a CA Event is fired.
 * <p><b>NOTE:</b> This class was moved from the Unit Test suite to the Integration Test suite
 * because the Site Auth Validator uses dotCMS APIs to work as expected.</p>
 *
 * @author Freddy Rodriguez
 * @since Jun 23rd, 2025
 */
@Dependent
@RunWith(JUnit4WeldRunner.class)
public class AnalyticsValidatorUtilTest extends IntegrationTestBase {

    private static Host testSite;

    private static final String TEST_SITE_NAME = "www.test-ca-site.com";
    private static final String TEST_SITE_ALIAS = "www.alias-test-ca-site.com";
    // Test random Site Auth
    private static final String TEST_SITE_AUTH = "DOT.48190c8c-42c4-46af-8d1a-0cd5db894797.8N9Oq3uD311V8YN2L6-BoINgX";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        testSite = new SiteDataGen().name(TEST_SITE_NAME).aliases(TEST_SITE_ALIAS).nextPersisted(true);
        final AppSecrets.Builder builder = new AppSecrets.Builder();
        final AppSecrets secrets = builder.withKey(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY)
                .withSecret("siteAuth", TEST_SITE_AUTH)
                .build();
        final AppsAPI appsAPI = APILocator.getAppsAPI();
        appsAPI.saveSecrets(secrets, testSite, APILocator.systemUser());

        setSiteAliasInThreadLocalHttpRequest();
    }

    @AfterClass
    public static void tearDownClass() {
        // Restore original config value
        final HttpServletRequestThreadLocal requestThreadLocal = HttpServletRequestThreadLocal.INSTANCE;
        requestThreadLocal.setRequest(null);
        final HostAPI hostAPI = APILocator.getHostAPI();
        try {
            hostAPI.unpublish(testSite, APILocator.systemUser(), DONT_RESPECT_FRONT_END_ROLES);
            hostAPI.archive(testSite, APILocator.systemUser(), DONT_RESPECT_FRONT_END_ROLES);
            hostAPI.delete(testSite, APILocator.systemUser(), DONT_RESPECT_FRONT_END_ROLES);
        } catch (final DotDataException | DotSecurityException e) {
            Logger.warn(AnalyticsValidatorUtilTest.class, String.format("Failed to delete site '%s': %s",
                    TEST_SITE_NAME, ExceptionUtil.getErrorMessage(e)), e);
        }
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}}
     * When: The site_auth is a number
     * Should: return this error
     * <pre>
     *     {
     *         "field": "context.site_auth",
     *         "code": "INVALID_STRING_TYPE",
     *         "message": "value is not a string: 123"
     *     }
     * </pre>
     */
    @Test
    public void stringSiteAuthInvalidType() {
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": 123," +
                        "\"session_id\": \"abc\"," +
                        "\"user_id\": \"abc\"," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}," +
                    "\"events\":[{}]" +
                "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

        assertEquals(1, errors.size());

        assertEquals("context.site_auth", errors.get(0).getField());
        assertEquals("INVALID_STRING_TYPE", errors.get(0).getCode().toString());
        assertEquals("Field value is not a String: 123", errors.get(0).getMessage());
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}
     * When: The site_auth is missing
     * Should: return this error
     * <pre>
     *     {
     *         "field": "context.site_auth",
     *         "code": "REQUIRED_FIELD_MISSING",
     *         "message": "Required field is missing: context.site_auth"
     *     }
     * </pre>
     */
    @Test
    public void requiredSiteAuth() {
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"session_id\": \"abc\"," +
                        "\"user_id\": \"abc\"," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}," +
                    "\"events\":[{}]" +
                "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

        assertEquals(1, errors.size());

        assertEquals("context.site_auth", errors.get(0).getField());
        assertEquals("REQUIRED_FIELD_MISSING", errors.get(0).getCode().toString());
        assertEquals("Required field is missing: context.site_auth", errors.get(0).getMessage());
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}</li>
     *     <li><b>Given Scenario: </b>The site_auth does not match the value in the Content
     *     Analytics App.</li>
     *     <li><b>Expected Result: </b>The following error must be returned:</li>
     * </ul>
     * <pre>
     *     {
     *         "field": "context.site_auth",
     *         "code": "INVALID_SITE_AUTH",
     *         "message": "Invalid Site Auth"
     *     }
     * </pre>
     */
    @Test
    public void invalidSiteAuth() {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"invalid-auth\"," +
                        "\"session_id\": \"abc\"," +
                        "\"user_id\": \"abc\"," +
                        "\"device\": {" +
                        "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}," +
                    "\"events\":[{}]" +
                "}";
        final JSONObject jsonObject = new JSONObject(json);

        // ╔════════════════════════╗
        // ║  Generating Test data  ║
        // ╚════════════════════════╝
        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        assertEquals(1, errors.size());
        assertEquals("context.site_auth", errors.get(0).getField());
        assertEquals("INVALID_SITE_AUTH", errors.get(0).getCode().toString());
        assertEquals("Invalid Site Auth", errors.get(0).getMessage());
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}</li>
     *     <li><b>Given Scenario: </b>The Origin and Referer HTTP Headers provide an invalid Site
     *     Name or Alias.</li>
     *     <li><b>Expected Result: </b>The following error must be returned:</li>
     * </ul>
     * <pre>
     *     {
     *         "field": "context.site_auth",
     *         "code": "INVALID_SITE_AUTH",
     *         "message": "Site with name/alias '{{invalidSiteName}}' was not found"
     *     }
     * </pre>
     */
    @Test
    public void invalidOriginAndRefererHeadersForSiteAuth() {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"" + TEST_SITE_AUTH + "\"," +
                        "\"session_id\": \"abc\"," +
                        "\"user_id\": \"abc\"," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}," +
                    "\"events\":[{}]" +
                "}";
        final String invalidSiteName = "www.invalidsite.com";
        final JSONObject jsonObject = new JSONObject(json);
        try {
            // Set the expected test data in the thread-local HTTP Request
            final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
            Mockito.when(req.getHeader(HttpHeaders.ORIGIN)).thenReturn("https://" + invalidSiteName);
            Mockito.when(req.getHeader(HttpHeaders.REFERER)).thenReturn("https://" + invalidSiteName);
            final HttpServletRequestThreadLocal requestThreadLocal = HttpServletRequestThreadLocal.INSTANCE;
            requestThreadLocal.setRequest(req);

            // ╔════════════════════════╗
            // ║  Generating Test data  ║
            // ╚════════════════════════╝
            final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

            // ╔══════════════╗
            // ║  Assertions  ║
            // ╚══════════════╝
            assertEquals(1, errors.size());
            assertEquals("context.site_auth", errors.get(0).getField());
            assertEquals("INVALID_SITE_AUTH", errors.get(0).getCode().toString());
            assertEquals("Site with name/alias '" + invalidSiteName + "' was not found", errors.get(0).getMessage());
        } finally {
            // ╔═══════════╗
            // ║  Cleanup  ║
            // ╚═══════════╝
            setSiteAliasInThreadLocalHttpRequest();
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}</li>
     *     <li><b>Given Scenario: </b>Site Name or Alias could not bre retrieved from Origin and/or
     *     Referer HTTP Headers.</li>
     *     <li><b>Expected Result: </b>The following error must be returned:</li>
     * </ul>
     * <pre>
     *     {
     *         "field": "context.site_auth",
     *         "code": "INVALID_SITE_AUTH",
     *         "message": "Site could not be retrieved from Origin or Referer HTTP Headers"
     *     }
     * </pre>
     */
    @Test
    public void missingOriginAndRefererHeadersForSiteAuth() {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"" + TEST_SITE_AUTH + "\"," +
                        "\"session_id\": \"abc\"," +
                        "\"user_id\": \"abc\"," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}," +
                    "\"events\":[{}]" +
                "}";
        final JSONObject jsonObject = new JSONObject(json);
        try {
            // Set the expected test data in the thread-local HTTP Request
            final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
            Mockito.when(req.getHeader(HttpHeaders.ORIGIN)).thenReturn(null);
            Mockito.when(req.getHeader(HttpHeaders.REFERER)).thenReturn(null);
            final HttpServletRequestThreadLocal requestThreadLocal = HttpServletRequestThreadLocal.INSTANCE;
            requestThreadLocal.setRequest(req);

            // ╔════════════════════════╗
            // ║  Generating Test data  ║
            // ╚════════════════════════╝
            final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

            // ╔══════════════╗
            // ║  Assertions  ║
            // ╚══════════════╝
            assertEquals(1, errors.size());
            assertEquals("context.site_auth", errors.get(0).getField());
            assertEquals("INVALID_SITE_AUTH", errors.get(0).getCode().toString());
            assertEquals("Site could not be retrieved from Origin or Referer HTTP Headers", errors.get(0).getMessage());
        } finally {
            // ╔═══════════╗
            // ║  Cleanup  ║
            // ╚═══════════╝
            setSiteAliasInThreadLocalHttpRequest();
        }
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}
     * When: The session_id is a number
     * Should: return this error
     * <pre>
     *     {
     *         "field": "context.session_id",
     *         "code": "INVALID_STRING_TYPE",
     *         "message": "value is not a string: 456"
     *     }
     * </pre>
     */
    @Test
    public void stringSessionIdInvalidType() {
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"" + TEST_SITE_AUTH + "\"," +
                        "\"session_id\": 456," +
                        "\"user_id\": \"abc\"," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}," +
                    "\"events\":[{}]" +
                "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

        assertEquals(1, errors.size());

        assertEquals("context.session_id", errors.get(0).getField());
        assertEquals("INVALID_STRING_TYPE", errors.get(0).getCode().toString());
        assertEquals("Field value is not a String: 456", errors.get(0).getMessage());
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}
     * When: The session_id is missing
     * Should: return this error
     * <pre>
     *     {
     *         "field": "context.session_id",
     *         "code": "REQUIRED_FIELD_MISSING",
     *         "message": "Required field is missing: context.session_id"
     *     }
     * </pre>
     */
    @Test
    public void requiredSessionId() {
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"" + TEST_SITE_AUTH + "\"," +
                        "\"user_id\": \"abc\"," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}," +
                    "\"events\":[{}]" +
                "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

        assertEquals(1, errors.size());

        assertEquals("context.session_id", errors.get(0).getField());
        assertEquals("REQUIRED_FIELD_MISSING", errors.get(0).getCode().toString());
        assertEquals("Required field is missing: context.session_id", errors.get(0).getMessage());
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}
     * When: The user_id is a number
     * Should: return this error
     * <pre>
     *     {
     *         "field": "context.user_id",
     *         "code": "INVALID_STRING_TYPE",
     *         "message": "value is not a string: 789"
     *     }
     * </pre>
     */
    @Test
    public void stringUserIdInvalidType() {
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"" + TEST_SITE_AUTH + "\"," +
                        "\"session_id\": \"abc\"," +
                        "\"user_id\": 789," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}," +
                    "\"events\":[{}]" +
                "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

        assertEquals(1, errors.size());

        assertEquals("context.user_id", errors.get(0).getField());
        assertEquals("INVALID_STRING_TYPE", errors.get(0).getCode().toString());
        assertEquals("Field value is not a String: 789", errors.get(0).getMessage());
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}
     * When: The session_id is missing
     * Should: return this error
     * <pre>
     *     {
     *         "field": "context.session_id",
     *         "code": "REQUIRED_FIELD_MISSING",
     *         "message": "Required field is missing: context.session_id"
     *     }
     * </pre>
     */
    @Test
    public void requiredUserId() {
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"" + TEST_SITE_AUTH + "\"," +
                        "\"session_id\": \"abc\"," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}," +
                    "\"events\":[{}]" +
                "}";

        final JSONObject jsonObject = new JSONObject(json);
        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

        assertEquals(1, errors.size());

        assertEquals("context.user_id", errors.get(0).getField());
        assertEquals("REQUIRED_FIELD_MISSING", errors.get(0).getCode().toString());
        assertEquals("Required field is missing: context.user_id", errors.get(0).getMessage());
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}
     * When: The context attribute is not there
     * Should: return this error
     * <pre>
     *     {
     *         "field": "context",
     *         "code": "REQUIRED_FIELD_MISSING",
     *         "message": "Required field is missing: context.\\"
     *     }
     * </pre>
     */
    @Test
    public void requiredContext() {
        final String json =
                "{" +
                    "\"site_auth\": 123," +
                    "\"session_id\": \"abc\"," +
                    "\"user_id\": \"abc\"," +
                    "\"device\": {" +
                        "\"screen_resolution\": \"1200x800\"," +
                        "\"language\": \"en\"," +
                        "\"viewport_width\": \"1200\"," +
                        "\"viewport_height\": \"800\"" +
                    "}," +
                    "\"events\":[{}]" +
                "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

        assertEquals(16, errors.size());
        final List<String> errorFields =
                errors.stream().map(AnalyticsValidatorUtil.Error::getField).collect(Collectors.toList());

        assertTrue(errorFields.contains("context.site_auth"));
        assertTrue(errorFields.contains("context.session_id"));
        assertTrue(errorFields.contains("context.user_id"));
        assertTrue(errorFields.contains("context"));
        assertTrue(errorFields.contains("site_auth"));
        assertTrue(errorFields.contains("session_id"));
        assertTrue(errorFields.contains("user_id"));
        assertTrue(errorFields.contains("context.device.language"));
        assertTrue(errorFields.contains("context.device.screen_resolution"));
        assertTrue(errorFields.contains("context.device.viewport_width"));
        assertTrue(errorFields.contains("context.device.viewport_height"));
        assertTrue(errorFields.contains("context.device"));
        assertTrue(errorFields.contains("device.language"));
        assertTrue(errorFields.contains("device.screen_resolution"));
        assertTrue(errorFields.contains("device.viewport_width"));
        assertTrue(errorFields.contains("device.viewport_height"));

        for (AnalyticsValidatorUtil.Error error : errors) {
            if (error.getField().equals("context.site_auth")) {
                assertEquals("context.site_auth", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.site_auth", error.getMessage());
            } else if (error.getField().equals("context.session_id")) {
                assertEquals("context.session_id", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.session_id", error.getMessage());
            } else if (error.getField().equals("context.user_id")) {
                assertEquals("context.user_id", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.user_id", error.getMessage());
            } else  if (error.getField().equals("context")){
                assertEquals("context", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context", error.getMessage());
            } else  if (error.getField().equals("site_auth")){
                assertEquals("site_auth", error.getField());
                assertEquals("UNKNOWN_FIELD", error.getCode().toString());
                assertEquals("Unknown field 'site_auth'", error.getMessage());
            } else  if (error.getField().equals("session_id")){
                assertEquals("session_id", error.getField());
                assertEquals("UNKNOWN_FIELD", error.getCode().toString());
                assertEquals("Unknown field 'session_id'", error.getMessage());
            } else  if (error.getField().equals("user_id")){
                assertEquals("user_id", error.getField());
                assertEquals("UNKNOWN_FIELD", error.getCode().toString());
                assertEquals("Unknown field 'user_id'", error.getMessage());
            } else  if (error.getField().equals("context.device.language")){
                assertEquals("context.device.language", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.device.language", error.getMessage());
            } else  if (error.getField().equals("context.device.screen_resolution")){
                assertEquals("context.device.screen_resolution", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.device.screen_resolution", error.getMessage());
            } else  if (error.getField().equals("context.device.viewport_width")){
                assertEquals("context.device.viewport_width", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.device.viewport_width", error.getMessage());
            } else  if (error.getField().equals("context.device.viewport_height")){
                assertEquals("context.device.viewport_height", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.device.viewport_height", error.getMessage());
            } else  if (error.getField().equals("context.device")){
                assertEquals("context.device", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.device", error.getMessage());
            } else  if (error.getField().equals("device.language")){
                assertEquals("device.language", error.getField());
                assertEquals("UNKNOWN_FIELD", error.getCode().toString());
                assertEquals("Unknown field 'device.language'", error.getMessage());
            } else  if (error.getField().equals("device.screen_resolution")){
                assertEquals("device.screen_resolution", error.getField());
                assertEquals("UNKNOWN_FIELD", error.getCode().toString());
                assertEquals("Unknown field 'device.screen_resolution'", error.getMessage());
            } else  if (error.getField().equals("device.viewport_width")){
                assertEquals("device.viewport_width", error.getField());
                assertEquals("UNKNOWN_FIELD", error.getCode().toString());
                assertEquals("Unknown field 'device.viewport_width'", error.getMessage());
            } else  if (error.getField().equals("device.viewport_height")){
                assertEquals("device.viewport_height", error.getField());
                assertEquals("UNKNOWN_FIELD", error.getCode().toString());
                assertEquals("Unknown field 'device.viewport_height'", error.getMessage());
            } else {
                throw new AssertionError("Unexpected field: " + errorFields);
            }
        }
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}
     * When: The context attribute is notJSON
     * Should: return this error
     * <pre>
     *     {
     *         "field": "context",
     *         "code": "REQUIRED_FIELD_MISSING",
     *         "message": "Required field is missing: context."
     *     }
     * </pre>
     */
    @Test
    public void noJsonContext() {
        final String json =
                "{" +
                    "\"context\": 123," +
                    "\"events\":[{}]" +
                "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

        assertEquals(9, errors.size());
        final List<String> errorFields =
                errors.stream().map(AnalyticsValidatorUtil.Error::getField).collect(Collectors.toList());

        assertTrue(errorFields.contains("context.site_auth"));
        assertTrue(errorFields.contains("context.session_id"));
        assertTrue(errorFields.contains("context.user_id"));
        assertTrue(errorFields.contains("context"));
        assertTrue(errorFields.contains("context.device.language"));
        assertTrue(errorFields.contains("context.device.screen_resolution"));
        assertTrue(errorFields.contains("context.device.viewport_width"));
        assertTrue(errorFields.contains("context.device.viewport_height"));
        assertTrue(errorFields.contains("context.device"));

        for (AnalyticsValidatorUtil.Error error : errors) {
            if (error.getField().equals("context.site_auth")) {
                assertEquals("context.site_auth", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.site_auth", error.getMessage());
            } else if (error.getField().equals("context.session_id")) {
                assertEquals("context.session_id", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.session_id", error.getMessage());
            } else if (error.getField().equals("context.user_id")) {
                assertEquals("context.user_id", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.user_id", error.getMessage());
            } else if (error.getField().equals("context")) {
                assertEquals("context", error.getField());
                assertEquals("INVALID_JSON_OBJECT_TYPE", error.getCode().toString());
                assertEquals("Field value is not a JSON object: 123", error.getMessage());
            } else  if (error.getField().equals("context.device.language")){
                assertEquals("context.device.language", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.device.language", error.getMessage());
            } else  if (error.getField().equals("context.device.screen_resolution")){
                assertEquals("context.device.screen_resolution", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.device.screen_resolution", error.getMessage());
            } else  if (error.getField().equals("context.device.viewport_width")){
                assertEquals("context.device.viewport_width", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.device.viewport_width", error.getMessage());
            } else  if (error.getField().equals("context.device.viewport_height")){
                assertEquals("context.device.viewport_height", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.device.viewport_height", error.getMessage());
            } else  if (error.getField().equals("context.device")){
                assertEquals("context.device", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.device", error.getMessage());
            } else {
                throw new AssertionError("Unexpected field: " + errorFields);
            }
        }
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}}
     * When: The site_auth is a number
     * Should: return this error
     * <pre>
     *     {
     *         "field": "context.site_auth",
     *         "code": "INVALID_STRING_TYPE",
     *         "message": "value is not a string: 123"
     *     }
     * </pre>

     */
    @Test
    public void eventsMustBeArray() {
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"" + TEST_SITE_AUTH + "\"," +
                        "\"session_id\": \"abc\"," +
                        "\"user_id\": \"abc\"," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}," +
                    "}," +
                    "\"events\":123" +
                "}";
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader(HttpHeaders.ORIGIN)).thenReturn("localhost");

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

        assertEquals(1, errors.size());

        assertEquals("events", errors.get(0).getField());
        assertEquals("INVALID_JSON_ARRAY_TYPE", errors.get(0).getCode().toString());
        assertEquals("Field value is not a JSON array: 123", errors.get(0).getMessage());
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateEvents(JSONArray)}
     * When: The event_type is required
     * Should: return this error
     * <pre>
     *     {
     *         "field": "events.event_type",
     *         "code": "REQUIRED_FIELD_MISSING",
     *         "message": "Required field is missing: events.event_type"."
     *     }
     * </pre>

     */
    @Test
    public void eventTypeIsRequired() {
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"xyz\"," +
                        "\"session_id\": \"abc\"," +
                        "\"user_id\": \"abc\"," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}," +
                    "\"events\":[" +
                        "{" +
                            "\"data\": {}" +
                        "}" +
                    "]" +
                "}";

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE
                .validateEvents((JSONArray) new JSONObject(json).get("events"));

        assertEquals(2, errors.size());

        final List<String> errorsField = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getField)
                .distinct()
                .collect(Collectors.toList());

        assertTrue(errorsField.contains("events[0].event_type"));
        assertTrue(errorsField.contains("events[0].local_time"));

        final List<ValidationErrorCode> errorsCode = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getCode)
                .distinct()
                .collect(Collectors.toList());

        assertEquals(1, errorsCode.size());
        assertEquals("REQUIRED_FIELD_MISSING", errorsCode.get(0).name());
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateEvents(JSONArray)}
     * When: You send a pageview you should get an error for each required field
     * Should: return this error for each required field
     * <pre>
     *     {
     *         "field": "[field name]",
     *         "code": "REQUIRED_FIELD_MISSING",
     *         "message": "Required field is missing: [field name]"
     *     }
     * </pre>
     */
    @Test
    public void dataIsRequired() {
        final int expectedErrorCount = 4;
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"" + TEST_SITE_AUTH + "\"," +
                        "\"session_id\": \"abc\"," +
                        "\"user_id\": \"abc\"," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}," +
                    "\"events\":[" +
                        "{" +
                        "\"event_type\": \"pageview\"" +
                        "}" +
                    "]" +
                "}";

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE
                .validateEvents((JSONArray) new JSONObject(json).get("events"));

        assertEquals(String.format("The test is expecting %s errors in the validation",
                errors.size() > expectedErrorCount ? "less" : "more"),
                expectedErrorCount, errors.size());

        final List<String> errorsField = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getField)
                .distinct()
                .collect(Collectors.toList());

        assertEquals("Expected error count must match the number of error messages in the response",
                expectedErrorCount, errorsField.size());
        assertTrue(errorsField.contains("events[0].data.page"));
        assertTrue(errorsField.contains("events[0].data.page.url"));
        assertTrue(errorsField.contains("events[0].data.page.doc_encoding"));
        assertTrue(errorsField.contains("events[0].local_time"));

        final List<ValidationErrorCode> errorsCode = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getCode)
                .distinct()
                .collect(Collectors.toList());

        assertEquals("Only one error message is expected", 1, errorsCode.size());
        assertEquals("Validation code is not the expected one",
                "REQUIRED_FIELD_MISSING", errorsCode.get(0).name());

        final List<String> errorsMessages = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getMessage)
                .distinct()
                .collect(Collectors.toList());

        assertEquals(expectedErrorCount, errorsField.size());
        assertTrue(errorsMessages.contains("Required field is missing: data.page"));
        assertTrue(errorsMessages.contains("Required field is missing: data.page.url"));
        assertTrue(errorsMessages.contains("Required field is missing: local_time"));
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateEvents(JSONArray)}
     * When: A pageview with all the required fields are sent
     * Should: no error should be returned
     */
    @Test
    public void rightPageView() {
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"" + TEST_SITE_AUTH +"\"," +
                        "\"session_id\": \"abc\"," +
                        "\"user_id\": \"abc\"," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}," +
                    "\"events\":[" +
                        "{" +
                            "\"event_type\": \"pageview\"," +
                            "\"local_time\": \"2025-06-09T14:30:00+02:00\"," +
                            "\"data\": {" +
                                "\"page\": {" +
                                    "\"url\": \"http://www.google.com\"," +
                                    "\"title\": \"Google\"," +
                                    "\"doc_encoding\": \"UTF8\"" +
                                "}" +
                            "}" +
                        "}" +
                    "]" +
                "}";

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE
                .validateEvents((JSONArray) new JSONObject(json).get("events"));

        assertTrue(errors.isEmpty());
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateEvents(JSONArray)}
     * When: A pageview with all the required fields are sent, but with a wrong date format syntax
     * Should: no error should be returned
     */
    @Test
    public void wrongLocalTimeDateFormat() {
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"" + TEST_SITE_AUTH + "\"," +
                        "\"session_id\": \"abc\"," +
                        "\"user_id\": \"abc\"," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}," +
                    "\"events\":[" +
                        "{" +
                            "\"event_type\": \"pageview\"," +
                            "\"local_time\": \"2025-06-09T14:30:00\"," +
                            "\"data\": {" +
                                "\"page\": {" +
                                    "\"url\": \"http://www.google.com\"," +
                                    "\"title\": \"Google\"," +
                                    "\"doc_encoding\": \"UTF8\"" +
                                "}" +
                            "}" +
                        "}" +
                    "]" +
                "}";

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE
                .validateEvents((JSONArray) new JSONObject(json).get("events"));

        assertEquals(1, errors.size());
        assertEquals("events[0].local_time", errors.get(0).getField());
        assertEquals("INVALID_DATE_FORMAT", errors.get(0).getCode().toString());
        assertEquals("Field value is not a valid date in format '2025-06-09T14:30:00+02:00': 2025-06-09T14:30:00",
                errors.get(0).getMessage());
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateEvents(JSONArray)}
     * When: A pageview with all the required fields and some extra fields
     * Should: one error for each extra fields
     */
    @Test
    public void extraAttributesPageView() {
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"" + TEST_SITE_AUTH + "\"," +
                        "\"session_id\": \"abc\"," +
                        "\"user_id\": \"abc\"," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}," +
                    "\"events\":[" +
                        "{" +
                            "\"event_type\": \"pageview\"," +
                            "\"local_time\": \"2025-06-09T14:30:00+02:00\"," +
                            "\"data\": {" +
                                "\"page\": {" +
                                    "\"url\": \"http://www.google.com\"," +
                                    "\"title\": \"Google\"," +
                                    "\"doc_encoding\": \"UTF8\"," +
                                    "\"extra_field\": \"extra\"" +
                                "}" +
                            "}," +
                            "\"extra_field_1\": \"extra\"," +
                            "\"extra_field_2\": {\"extra_field\": \"extra\"}" +
                        "}" +
                    "]" +
                "}";

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE
                .validateEvents((JSONArray) new JSONObject(json).get("events"));

        assertEquals(3, errors.size());

        final List<String> errorsField = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getField)
                .distinct()
                .collect(Collectors.toList());

        assertTrue(errorsField.contains("events[0].data.page.extra_field"));
        assertTrue(errorsField.contains("events[0].extra_field_1"));
        assertTrue(errorsField.contains("events[0].extra_field_2.extra_field"));

        final List<ValidationErrorCode> errorsCode = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getCode)
                .distinct()
                .collect(Collectors.toList());

        assertEquals(1, errorsCode.size());
        assertEquals("UNKNOWN_FIELD", errorsCode.get(0).name());
    }

    /**
     * Utility method to set the expected test data in the thread-local HTTP Request. The mechanism
     * used to save Events for a specific Site is to specify the Site Alias or Name in either the
     * Origin or Referer HTTP Headers.
     */
    private static void setSiteAliasInThreadLocalHttpRequest() {
        // Set the expected test data in the thread-local HTTP Request
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader(HttpHeaders.ORIGIN)).thenReturn("https://" + TEST_SITE_ALIAS);
        Mockito.when(req.getHeader(HttpHeaders.REFERER)).thenReturn("https://" + TEST_SITE_ALIAS);
        final HttpServletRequestThreadLocal requestThreadLocal = HttpServletRequestThreadLocal.INSTANCE;
        requestThreadLocal.setRequest(req);
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}}
     * When: We are going to send a event without event_type
     * Should: return this error
     * <pre>
     *     {
     *         "field": "events[0].event_type",
     *         "code": "REQUIRED_FIELD_MISSING",
     *         "message": "Required field is missing: events.event_type"
     *     }
     * </pre>
     */
    @Test
    public void eventsRequiredFields() {
        final String json =
                "{" +
                        "\"context\": {" +
                            "\"site_auth\": \"" + TEST_SITE_AUTH + "\"," +
                            "\"session_id\": \"abc\"," +
                            "\"user_id\": \"abc\"" +
                        "}," +
                        "\"events\":[{}]" +
                "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE
                .validateEvents(jsonObject.getJSONArray("events"));

        assertEquals(2, errors.size());

        final List<String> errorsField = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getField)
                .distinct()
                .collect(Collectors.toList());

        assertTrue(errorsField.contains("events[0].event_type"));
        assertTrue(errorsField.contains("events[0].local_time"));

        final List<ValidationErrorCode> errorsCode = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getCode)
                .distinct()
                .collect(Collectors.toList());

        assertEquals(1, errorsCode.size());
        assertEquals("REQUIRED_FIELD_MISSING", errorsCode.get(0).name());

    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}}
     * When: I sent a vent just with the event_type for a custom_event
     * Should: return this error
     * <pre>
     *     {
     *         "field": "events[0].local_time",
     *         "code": "REQUIRED_FIELD_MISSING",
     *         "message": "Required field is missing: events.local_time"
     *     }
     * </pre>
     */
    @Test
    public void eventsRequiredFieldsLocalTimeAndData() {
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"" + TEST_SITE_AUTH + "\"," +
                        "\"session_id\": \"abc\"," +
                        "\"user_id\": \"abc\"" +
                    "}," +
                    "\"events\":[{" +
                        "\"event_type\": \"custom_event\"," +
                    "}]" +
                "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE
                .validateEvents(jsonObject.getJSONArray("events"));

        assertEquals(1, errors.size());

        final List<String> errorsField = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getField)
                .distinct()
                .collect(Collectors.toList());

        assertEquals(1, errorsField.size());
        assertTrue(errorsField.contains("events[0].local_time"));


        final List<ValidationErrorCode> errorsCode = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getCode)
                .distinct()
                .collect(Collectors.toList());

        assertEquals(1, errorsCode.size());
        assertEquals("REQUIRED_FIELD_MISSING", errorsCode.get(0).name());

        final List<String> errorsMessages = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getMessage)
                .distinct()
                .collect(Collectors.toList());

        assertEquals(1, errorsField.size());
        assertTrue(errorsMessages.contains("Required field is missing: local_time"));
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}}
     * When: Send a custom section
     * Should: it is ok, no errors must be trigger
     */
    @Test
    public void eventsWithCustomSection() {
        final String json =
                "{" +
                    "\"context\": {" +
                        "\"site_auth\": \"" + TEST_SITE_AUTH + "\"," +
                        "\"session_id\": \"abc\"," +
                        "\"user_id\": \"abc\"" +
                    "}," +
                    "\"events\":[{" +
                        "\"event_type\": \"custom_event\"," +
                        "\"local_time\": \"2025-06-09T14:30:00+02:00\"," +
                        "\"data\": {" +
                            "\"custom\": {" +
                                 "\"key1\": \"value1\"," +
                                 "\"key2\": \"value2\"" +
                            "}" +
                        "}" +
                    "}]" +
                "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE
                .validateEvents(jsonObject.getJSONArray("events"));

        assertTrue( errors.isEmpty());

    }


}
