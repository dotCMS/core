package com.dotcms.jitsu.validators;

import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import org.junit.Test;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Test class for AnalyticsValidatorUtil
 */
public class AnalyticsValidatorUtilTest {

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}}
     * When: The site_key is a number
     * Should: return this error
     * <pre>
     *     {
     *         "field": "context.site_key",
     *         "code": "INVALID_STRING_TYPE",
     *         "message": "value is not a string: 123"
     *     }
     * </pre>
     * @throws Exception
     */
    @Test
    public void stringSiteKey() throws Exception {
        final String json = "{" +
            "\"context\": {" +
                "\"site_key\": 123," +
                "\"session_id\": \"abc\"," +
                "\"user_id\": \"abc\"" +
            "}," +
            "\"events\":[{}]" +
        "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

        assertEquals(1, errors.size());

        assertEquals("context.site_key", errors.get(0).getField());
        assertEquals("INVALID_STRING_TYPE", errors.get(0).getCode().toString());
        assertEquals("Field value is not a String: 123", errors.get(0).getMessage());
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}
     * When: The site_key is missing
     * Should: return this error
     * <pre>
     *     {
     *         "field": "context.site_key",
     *         "code": "INVALID_STRING_TYPE",
     *         "message": "value is not a string: 123"
     *     }
     * </pre>
     * @throws Exception
     */
    @Test
    public void requiredSiteKey() throws Exception {
        final String json = "{" +
                "\"context\": {" +
                    "\"session_id\": \"abc\"," +
                    "\"user_id\": \"abc\"" +
                "}," +
                "\"events\":[{}]" +
        "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

        assertEquals(1, errors.size());

        assertEquals("context.site_key", errors.get(0).getField());
        assertEquals("REQUIRED_FIELD_MISSING", errors.get(0).getCode().toString());
        assertEquals("Required field is missing: context.site_key", errors.get(0).getMessage());
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
     * @throws Exception
     */
    @Test
    public void stringSessionId() throws Exception {
        final String json = "{" +
                "\"context\": {" +
                    "\"site_key\": \"xyz\"," +
                    "\"session_id\": 456," +
                    "\"user_id\": \"abc\"" +
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
     * @throws Exception
     */
    @Test
    public void requiredSessionId() throws Exception {
        final String json = "{" +
                "\"context\": {" +
                    "\"site_key\": \"xyz\"," +
                    "\"user_id\": \"abc\"" +
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
     * @throws Exception
     */
    @Test
    public void stringUserId() throws Exception {
        final String json = "{" +
            "\"context\": {" +
                "\"site_key\": \"xyz\"," +
                "\"session_id\": \"abc\"," +
                "\"user_id\": 789" +
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
     * @throws Exception
     */
    @Test
    public void requiredUserId() throws Exception {
        final String json = "{" +
                "\"context\": {" +
                    "\"site_key\": \"xyz\"," +
                    "\"session_id\": \"abc\"" +
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
     * @throws Exception
     */
    @Test
    public void requiredContext() throws Exception {
        final String json = "{" +
            "\"site_key\": 123," +
            "\"session_id\": \"abc\"," +
            "\"user_id\": \"abc\"," +
            "\"events\":[{}]" +
        "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

        assertEquals(7, errors.size());
        final List<String> errorFields =
                errors.stream().map(AnalyticsValidatorUtil.Error::getField).collect(Collectors.toList());

        assertTrue(errorFields.contains("context.site_key"));
        assertTrue(errorFields.contains("context.session_id"));
        assertTrue(errorFields.contains("context.user_id"));
        assertTrue(errorFields.contains("context"));
        assertTrue(errorFields.contains("site_key"));
        assertTrue(errorFields.contains("session_id"));
        assertTrue(errorFields.contains("user_id"));

        for (AnalyticsValidatorUtil.Error error : errors) {
            if (error.getField().equals("context.site_key")) {
                assertEquals("context.site_key", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.site_key", error.getMessage());
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
            } else  if (error.getField().equals("site_key")){
                assertEquals("site_key", error.getField());
                assertEquals("UNKNOWN_FIELD", error.getCode().toString());
                assertEquals("Unknown field 'site_key'", error.getMessage());
            } else  if (error.getField().equals("session_id")){
                assertEquals("session_id", error.getField());
                assertEquals("UNKNOWN_FIELD", error.getCode().toString());
                assertEquals("Unknown field 'session_id'", error.getMessage());
            } else  if (error.getField().equals("user_id")){
                assertEquals("user_id", error.getField());
                assertEquals("UNKNOWN_FIELD", error.getCode().toString());
                assertEquals("Unknown field 'user_id'", error.getMessage());
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
     *         "message": "Required field is missing: context.\\"
     *     }
     * </pre>
     * @throws Exception
     */
    @Test
    public void noJsonContext() throws Exception {
        final String json = "{" +
            "\"context\": 123," +
            "\"events\":[{}]" +
        "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE.validateGlobalContext(jsonObject);

        assertEquals(4, errors.size());
        final List<String> errorFields =
                errors.stream().map(AnalyticsValidatorUtil.Error::getField).collect(Collectors.toList());

        assertTrue(errorFields.contains("context.site_key"));
        assertTrue(errorFields.contains("context.session_id"));
        assertTrue(errorFields.contains("context.user_id"));
        assertTrue(errorFields.contains("context"));

        for (AnalyticsValidatorUtil.Error error : errors) {
            if (error.getField().equals("context.site_key")) {
                assertEquals("context.site_key", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.site_key", error.getMessage());
            } else if (error.getField().equals("context.session_id")) {
                assertEquals("context.session_id", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.session_id", error.getMessage());
            } else if (error.getField().equals("context.user_id")) {
                assertEquals("context.user_id", error.getField());
                assertEquals("REQUIRED_FIELD_MISSING", error.getCode().toString());
                assertEquals("Required field is missing: context.user_id", error.getMessage());
            } else {
                assertEquals("context", error.getField());
                assertEquals("INVALID_JSON_OBJECT_TYPE", error.getCode().toString());
                assertEquals("Field value is not a JSON object: 123", error.getMessage());
            }
        }
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateGlobalContext(JSONObject)}}
     * When: The site_key is a number
     * Should: return this error
     * <pre>
     *     {
     *         "field": "context.site_key",
     *         "code": "INVALID_STRING_TYPE",
     *         "message": "value is not a string: 123"
     *     }
     * </pre>
     * @throws Exception
     */
    @Test
    public void eventsMustBeArray() throws Exception {
        final String json = "{" +
            "\"context\": {" +
                "\"site_key\": \"xyz\"," +
                "\"session_id\": \"abc\"," +
                "\"user_id\": \"abc\"" +
            "}," +
            "\"events\":123" +
        "}";

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
     * @throws Exception
     */
    @Test
    public void eventTypeIsRequired() {
        final String json = "{" +
            "\"context\": {" +
                "\"site_key\": \"xyz\"," +
                "\"session_id\": \"abc\"," +
                "\"user_id\": \"abc\"" +
            "}," +
            "\"events\":[" +
                "{" +
                    "\"data\": {}" +
                "}" +
            "]" +
        "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE
                .validateEvents((JSONArray) jsonObject.get("events"));

        assertEquals(1, errors.size());

        assertEquals("events[0].event_type", errors.get(0).getField());
        assertEquals("REQUIRED_FIELD_MISSING", errors.get(0).getCode().toString());
        assertEquals("Required field is missing: event_type", errors.get(0).getMessage());
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
        final int errorsCountExpected = 11;
        final String json = "{" +
            "\"context\": {" +
                "\"site_key\": \"xyz\"," +
                "\"session_id\": \"abc\"," +
                "\"user_id\": \"abc\"" +
            "}," +
             "\"events\":[" +
                "{" +
                    "\"event_type\": \"pageview\"" +
                "}" +
             "]" +
        "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE
                .validateEvents((JSONArray) jsonObject.get("events"));

        assertEquals(errorsCountExpected, errors.size());

        final List<String> errorsField = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getField)
                .distinct()
                .collect(Collectors.toList());

        assertEquals(errorsCountExpected, errorsField.size());
        assertTrue(errorsField.contains("events[0].data"));
        assertTrue(errorsField.contains("events[0].data.page"));
        assertTrue(errorsField.contains("events[0].data.page.url"));
        assertTrue(errorsField.contains("events[0].data.page.title"));
        assertTrue(errorsField.contains("events[0].data.page.doc_encoding"));
        assertTrue(errorsField.contains("events[0].data.device"));
        assertTrue(errorsField.contains("events[0].data.device.screen_resolution"));
        assertTrue(errorsField.contains("events[0].data.device.language"));
        assertTrue(errorsField.contains("events[0].data.device.viewport_width"));
        assertTrue(errorsField.contains("events[0].data.device.viewport_height"));
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

        assertEquals(errorsCountExpected, errorsField.size());
        assertTrue(errorsMessages.contains("Required field is missing: data.page"));
        assertTrue(errorsMessages.contains("Required field is missing: data"));
        assertTrue(errorsMessages.contains("Required field is missing: data.page.url"));
        assertTrue(errorsMessages.contains("Required field is missing: data.page.title"));
        assertTrue(errorsMessages.contains("Required field is missing: data.device"));
        assertTrue(errorsMessages.contains("Required field is missing: data.device.screen_resolution"));
        assertTrue(errorsMessages.contains("Required field is missing: data.device.language"));
        assertTrue(errorsMessages.contains("Required field is missing: data.device.viewport_width"));
        assertTrue(errorsMessages.contains("Required field is missing: data.device.viewport_height"));
        assertTrue(errorsMessages.contains("Required field is missing: local_time"));
    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateEvents(JSONArray)}
     * When: A pageview with all the required fields are sent
     * Should: no error should be returned
     */
    @Test
    public void rightPageView() {
        final String json = "{" +
            "\"context\": {" +
                "\"site_key\": \"xyz\"," +
                "\"session_id\": \"abc\"," +
                "\"user_id\": \"abc\"" +
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
                        "}," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}" +
                "}" +
            "]" +
        "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE
                .validateEvents((JSONArray) jsonObject.get("events"));

        assertTrue(errors.isEmpty());

    }

    /**
     * Method to test: {@link AnalyticsValidatorUtil#validateEvents(JSONArray)}
     * When: A pageview with all the required fields are sent, but with a wrong date format syntax
     * Should: no error should be returned
     */
    @Test
    public void wrongLocalTimeDateFormat() {
        final String json = "{" +
            "\"context\": {" +
                "\"site_key\": \"xyz\"," +
                "\"session_id\": \"abc\"," +
                "\"user_id\": \"abc\"" +
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
                        "}," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"" +
                        "}" +
                    "}" +
                "}" +
            "]" +
        "}";

        final JSONObject jsonObject = new JSONObject(json);
        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE
                .validateEvents((JSONArray) jsonObject.get("events"));

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
        final String json = "{" +
            "\"context\": {" +
                "\"site_key\": \"xyz\"," +
                "\"session_id\": \"abc\"," +
                "\"user_id\": \"abc\"" +
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
                        "}," +
                        "\"device\": {" +
                            "\"screen_resolution\": \"1200x800\"," +
                            "\"language\": \"en\"," +
                            "\"viewport_width\": \"1200\"," +
                            "\"viewport_height\": \"800\"," +
                            "\"extra_field\": \"extra\"" +
                        "}" +
                    "}," +
                    "\"extra_field_1\": \"extra\"," +
                    "\"extra_field_2\": {\"extra_field\": \"extra\"}" +
                "}" +
            "]" +
        "}";

        final JSONObject jsonObject = new JSONObject(json);

        final List<AnalyticsValidatorUtil.Error> errors = AnalyticsValidatorUtil.INSTANCE
                .validateEvents((JSONArray) jsonObject.get("events"));

        assertEquals(4, errors.size());

        final List<String> errorsField = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getField)
                .distinct()
                .collect(Collectors.toList());

        assertTrue(errorsField.contains("events[0].data.page.extra_field"));
        assertTrue(errorsField.contains("events[0].data.device.extra_field"));
        assertTrue(errorsField.contains("events[0].extra_field_1"));
        assertTrue(errorsField.contains("events[0].extra_field_2.extra_field"));

        final List<ValidationErrorCode> errorsCode = errors.stream()
                .map(AnalyticsValidatorUtil.Error::getCode)
                .distinct()
                .collect(Collectors.toList());

        assertEquals(1, errorsCode.size());
        assertEquals("UNKNOWN_FIELD", errorsCode.get(0).name());
    }
}
