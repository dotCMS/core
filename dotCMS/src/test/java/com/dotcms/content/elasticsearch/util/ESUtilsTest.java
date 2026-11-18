package com.dotcms.content.elasticsearch.util;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class ESUtilsTest {

    /**
     * Reserved characters of the Elasticsearch query_string syntax, copied from the documentation
     * rather than from {@link ESUtils}, so a character missing from the production set fails here.
     *
     * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#_reserved_characters">query_string reserved characters</a>
     */
    private static final List<String> DOCUMENTED_RESERVED_CHARACTERS = List.of(
            "+", "-", "=", "&&", "||", ">", "<", "!", "(", ")", "{", "}", "[", "]",
            "^", "\"", "~", "*", "?", ":", "\\", "/");

    /** Cannot be backslash-escaped per the documentation, so the helper must drop them. */
    private static final Set<String> REMOVED = Set.of("<", ">");

    /** Deliberately left untouched: URL map values may contain path segments. */
    private static final Set<String> KEPT = Set.of("/");

    /**
     * Testing {@link ESUtils#escapeExcludingSlashIncludingSpace(String)}
     * Given Scenario: each documented reserved character embedded in a word
     * ExpectedResult: escaped with a backslash, except range operators (removed) and "/" (kept)
     */
    @Test
    public void test_escapeExcludingSlashIncludingSpace_method() {
        for (final String reserved : DOCUMENTED_RESERVED_CHARACTERS) {
            final String input = "Lorem" + reserved + "ipsum";
            final String expected;
            if (REMOVED.contains(reserved)) {
                expected = "Loremipsum";
            } else if (KEPT.contains(reserved)) {
                expected = input;
            } else {
                expected = "Lorem" + reserved.chars()
                        .mapToObj(c -> "\\" + (char) c)
                        .collect(Collectors.joining()) + "ipsum";
            }
            assertEquals("Reserved character [" + reserved + "]",
                    expected, ESUtils.escapeExcludingSlashIncludingSpace(input));
        }
    }

    /**
     * Testing {@link ESUtils#escapeExcludingSlashIncludingSpace(String)}
     * Given Scenario: a value containing white space
     * ExpectedResult: the space is escaped so the value stays a single term
     */
    @Test
    public void test_escapeExcludingSlashIncludingSpace_escapesSpace() {
        assertEquals("Lorem\\ ipsum", ESUtils.escapeExcludingSlashIncludingSpace("Lorem ipsum"));
    }

    /**
     * Testing {@link ESUtils#escapeExcludingSlashIncludingSpace(String)}
     * Given Scenario: URL slugs written as range expressions (#37033)
     * ExpectedResult: the range operators are stripped, leaving a literal term or nothing
     */
    @Test
    public void test_escapeExcludingSlashIncludingSpace_removesRangeOperators() {
        assertEquals("abc", ESUtils.escapeExcludingSlashIncludingSpace("<abc>"));
        assertEquals("\\=abc", ESUtils.escapeExcludingSlashIncludingSpace("<=abc"));
        assertEquals("\\=abc", ESUtils.escapeExcludingSlashIncludingSpace(">=abc"));
        assertEquals("", ESUtils.escapeExcludingSlashIncludingSpace("<>"));
    }
}
