package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/**
 * How {@link MapToContentletPopulator} reads request values that callers commonly send in more
 * than one shape: relationship values, and field keys.
 */
public class MapToContentletPopulatorRequestValuesTest extends UnitTestBase {

    /** A JSON array arrives as a List; it used to fail with a ClassCastException (HTTP 500). */
    @Test
    public void test_relationship_value_given_as_an_array_becomes_the_comma_separated_filter() {
        assertEquals("id-one,id-two",
                MapToContentletPopulator.relationshipFilter(List.of("id-one", " id-two ")));
        assertEquals("id-one,id-two",
                MapToContentletPopulator.relationshipFilter(new String[]{"id-one", "id-two"}));
    }

    @Test
    public void test_relationship_value_array_skips_blank_and_null_elements() {
        assertEquals("id-one", MapToContentletPopulator.relationshipFilter(
                Arrays.asList("id-one", "", null, "  ")));
    }

    /** An empty array clears the relationship, the same as an empty string. */
    @Test
    public void test_empty_relationship_array_is_the_empty_filter() {
        assertEquals("", MapToContentletPopulator.relationshipFilter(List.of()));
    }

    @Test
    public void test_relationship_value_given_as_a_string_is_unchanged() {
        assertEquals("id-one,id-two", MapToContentletPopulator.relationshipFilter("id-one,id-two"));
        assertEquals("+contentType:Author +live:true",
                MapToContentletPopulator.relationshipFilter("+contentType:Author +live:true"));
    }

    @Test
    public void test_keys_matching_a_field_only_by_case_are_reported_with_the_field_variable() {
        final Map<String, String> mismatched = MapToContentletPopulator.caseMismatchedFieldKeys(
                List.of("Title", "body", "contentType", "LANGUAGEID", "unrelated"),
                Set.of("title", "body", "author"));

        assertEquals(Map.of("Title", "title"), mismatched);
    }

    @Test
    public void test_exact_keys_and_unknown_keys_are_not_reported() {
        assertTrue(MapToContentletPopulator.caseMismatchedFieldKeys(
                List.of("title", "identifier", "languageId", "stInode"),
                Set.of("title", "body")).isEmpty());
    }
}
