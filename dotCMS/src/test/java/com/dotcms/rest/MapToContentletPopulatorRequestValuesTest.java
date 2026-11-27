package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongFunction;
import org.junit.Test;

/**
 * How {@link MapToContentletPopulator} reads request values that callers commonly send in more
 * than one shape (relationship values, field keys), and the save warnings it reports for
 * relationship values.
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

    private static final String ONE = "11111111-1111-1111-1111-111111111111";
    private static final String TWO = "2222aaaa-2222-bbbb-2222-cccc2222dddd";
    private static final LongFunction<String> LABEL = id -> "fr (id " + id + ")";

    private static Map<String, Long> related(final Object... idAndLanguage) {
        final Map<String, Long> related = new LinkedHashMap<>();
        for (int i = 0; i < idAndLanguage.length; i += 2) {
            related.put((String) idAndLanguage[i], (Long) idAndLanguage[i + 1]);
        }
        return related;
    }

    @Test
    public void test_no_relationship_warning_when_every_identifier_is_related_in_the_saved_language() {
        assertTrue(MapToContentletPopulator.relationshipResultWarnings("authors", ONE + "," + TWO,
                related(ONE, 2L, TWO, 2L), 2L, LABEL).isEmpty());
    }

    /**
     * An identifier that was not related is reported with one message whether it does not exist
     * or the caller cannot read it, so the message reveals nothing about content they cannot see.
     */
    @Test
    public void test_identifier_not_related_is_reported_without_saying_why() {
        final List<String> warnings = MapToContentletPopulator.relationshipResultWarnings(
                "authors", ONE + ", " + TWO, related(ONE, 1L), 1L, LABEL);

        assertEquals(1, warnings.size());
        assertEquals("Relationship field 'authors': '" + TWO + "' was not related: no content with "
                + "that identifier exists, or you do not have permission to read it.", warnings.get(0));
    }

    /** Identifiers are stored with or without dashes, and callers may send either, in any case. */
    @Test
    public void test_identifier_matches_the_related_one_regardless_of_dashes_and_case() {
        assertTrue(MapToContentletPopulator.relationshipResultWarnings("authors",
                ONE.replace("-", "") + "," + TWO.toUpperCase(), related(ONE, 1L, TWO, 1L), 1L, LABEL)
                .isEmpty());
        final String undashed = TWO.replace("-", "");
        assertTrue(MapToContentletPopulator.relationshipResultWarnings("authors",
                TWO.toUpperCase(), related(undashed, 1L), 1L, LABEL).isEmpty());
    }

    /** filterContentlet relates an item with no version in the saved language from another one. */
    @Test
    public void test_item_related_from_another_language_is_reported_with_the_fix() {
        final List<String> warnings = MapToContentletPopulator.relationshipResultWarnings(
                "authors", ONE, related(ONE, 1L), 2L, LABEL);

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0), warnings.get(0).contains("'" + ONE + "' has no version in language fr (id 2)"));
        assertTrue(warnings.get(0), warnings.get(0).contains("save '" + ONE + "' with languageId 2"));
    }

    /**
     * A Lucene query keeps the first version it finds, in any language, so a related item in
     * another language does not mean it has no version in the saved one: no language warning.
     */
    @Test
    public void test_no_language_warning_when_the_filter_has_a_lucene_query() {
        assertTrue(MapToContentletPopulator.relationshipResultWarnings("authors",
                "+contentType:Author", related(ONE, 1L), 2L, LABEL).isEmpty());
        assertTrue(MapToContentletPopulator.relationshipResultWarnings("authors",
                "+contentType:Author," + ONE, related(ONE, 1L), 2L, LABEL).isEmpty());
    }

    /** Lucene queries are not checked; identifiers mixed with them still are. */
    @Test
    public void test_lucene_query_elements_are_not_reported() {
        assertTrue(MapToContentletPopulator.relationshipResultWarnings("authors",
                "+contentType:Author +live:true", related(), 1L, LABEL).isEmpty());
        assertEquals(1, MapToContentletPopulator.relationshipResultWarnings("authors",
                "+contentType:Author," + TWO, related(ONE, 1L), 1L, LABEL).size());
    }
}
