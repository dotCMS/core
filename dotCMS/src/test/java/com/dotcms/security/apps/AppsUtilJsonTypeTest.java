package com.dotcms.security.apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the {@link Type#JSON} param support in {@link AppsUtil}: how JSON params are
 * persisted ({@link AppsUtil#storedType(Type)}) and how their values are validated
 * ({@link AppsUtil#isValidJson(String)}).
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
}
