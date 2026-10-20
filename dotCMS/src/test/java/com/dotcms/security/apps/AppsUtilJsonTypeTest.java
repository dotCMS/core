package com.dotcms.security.apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.exception.DotDataValidationException;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

/**
 * Unit tests for the {@link Type#JSON} param support in {@link AppsUtil}: how JSON params are
 * persisted ({@link AppsUtil#storedType(Type)}) and how their values are validated
 * ({@link AppsUtil#isValidJson(String)}, the save check in
 * {@link AppsUtil#validateForSave(Map, AppDescriptor, Optional)} and the descriptor check in
 * {@code AppDescriptorHelper}).
 */
public class AppsUtilJsonTypeTest {

    /**
     * Given: a param declared as JSON
     * Expected: it is persisted as STRING, so a release without Type.JSON can still read it.
     */
    @Test
    public void test_storedType_persists_json_as_string() {
        assertEquals(Type.STRING, AppsUtil.storedType(Type.JSON));
    }

    /**
     * Given: params of every other type
     * Expected: they are persisted with their own type, unchanged.
     */
    @Test
    public void test_storedType_keeps_other_types() {
        for (final Type type : Type.values()) {
            if (type != Type.JSON) {
                assertEquals(type, AppsUtil.storedType(type));
            }
        }
    }

    /**
     * Given: well-formed JSON documents
     * Expected: they are accepted.
     */
    @Test
    public void test_isValidJson_accepts_valid_json() {
        assertTrue(AppsUtil.isValidJson("{\"config\":[{\"pattern\":\".*\",\"url\":\"https://a.com\"}]}"));
        assertTrue(AppsUtil.isValidJson("[]"));
    }

    /**
     * Given: malformed JSON, trailing text after a document, and blank values
     * Expected: they are rejected.
     */
    @Test
    public void test_isValidJson_rejects_invalid_json() {
        assertFalse(AppsUtil.isValidJson("{\"config\": ["));
        assertFalse(AppsUtil.isValidJson("{\"url\": \"https://a.com\",}"));
        assertFalse(AppsUtil.isValidJson("{} trailing"));
        assertFalse(AppsUtil.isValidJson(""));
        assertFalse(AppsUtil.isValidJson("   "));
        assertFalse(AppsUtil.isValidJson(null));
    }

    /**
     * Given: JSON scalars
     * Expected: they are accepted, since they are valid JSON documents.
     */
    @Test
    public void test_isValidJson_accepts_scalars() {
        assertTrue(AppsUtil.isValidJson("42"));
        assertTrue(AppsUtil.isValidJson("\"text\""));
        assertTrue(AppsUtil.isValidJson("true"));
    }

    /**
     * Given: a required JSON param saved with malformed JSON
     * Expected: the save is rejected with a message naming the param.
     */
    @Test
    public void test_validateForSave_rejects_invalid_json() {
        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AppsUtil.validateForSave(
                        Map.of("configuration", Optional.of("{\"config\": [".toCharArray())),
                        uveDescriptor(""), Optional.empty()));

        assertTrue(error.getMessage().contains("`configuration` is of type JSON"));
    }

    /**
     * Given: a required JSON param saved with valid JSON
     * Expected: the save passes validation.
     */
    @Test
    public void test_validateForSave_accepts_valid_json() {
        AppsUtil.validateForSave(
                Map.of("configuration", Optional.of("{\"config\":[]}".toCharArray())),
                uveDescriptor(""), Optional.empty());
    }

    /**
     * Given: an app YAML whose JSON param has an empty default
     * Expected: the descriptor is valid.
     */
    @Test
    public void test_descriptor_accepts_empty_json_default() throws DotDataValidationException {
        assertTrue(new AppDescriptorHelper().validateAppDescriptor(uveDescriptor("")));
    }

    /**
     * Given: an app YAML whose JSON param has a malformed default
     * Expected: descriptor validation fails and names the param.
     */
    @Test
    public void test_descriptor_rejects_invalid_json_default() {
        final DotDataValidationException error = assertThrows(DotDataValidationException.class,
                () -> new AppDescriptorHelper().validateAppDescriptor(uveDescriptor("{bad")));

        assertTrue(error.getMessage().contains("JSON Param `configuration`"));
    }

    /**
     * Given: an app YAML whose JSON param default is not a string
     * Expected: descriptor validation fails.
     */
    @Test
    public void test_descriptor_rejects_non_string_json_default() {
        assertThrows(DotDataValidationException.class,
                () -> new AppDescriptorHelper().validateAppDescriptor(uveDescriptor(42)));
    }

    /**
     * Builds a UVE-like app descriptor with one required JSON param.
     *
     * @param defaultValue the param's default value from the YAML
     */
    private static AppDescriptorImpl uveDescriptor(final Object defaultValue) {
        final ParamDescriptor configuration = ParamDescriptor.builder()
                .withType(Type.JSON)
                .withValue(defaultValue)
                .withLabel("Configuration")
                .withHint("UVE routes")
                .withRequired(true)
                .withHidden(false)
                .build();

        return new AppDescriptorImpl("dotema-config-v2.yml", true, "UVE", "UVE app",
                "https://example.com/icon.png", false, Map.of("configuration", configuration));
    }
}
