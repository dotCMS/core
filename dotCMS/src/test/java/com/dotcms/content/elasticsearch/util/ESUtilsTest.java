package com.dotcms.content.elasticsearch.util;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;

public class ESUtilsTest {

    /**
     * Testing {@link ESUtils#escapeExcludingSlashIncludingSpace(String)} we want to make sure all
     * the character are being escape properly.
     */
    @Test
    public void test_escapeExcludingSlashIncludingSpace_method() {

        final char escapeChar = '\\';

        final Set<String> TO_ESCAPE_COLLECTION =
                Stream.of("\\", "+", "-", "!", "(", ")", ":",
                        "^", "[", "]", "\"", "{", "}",
                        "~",
                        "*", "?", "|", "&",
                        " "
                ).collect(Collectors.toCollection(HashSet::new));

        for (final String toEscape : TO_ESCAPE_COLLECTION) {

            final String wordToScape = String.format("Lorem%sipsumdolorsitamet", toEscape);
            final String expected = String
                    .format("Lorem%s%sipsumdolorsitamet", escapeChar, toEscape);

            // Applying the escaping method
            final String result = ESUtils.escapeExcludingSlashIncludingSpace(wordToScape);

            Assert.assertNotNull(result);
            Assert.assertEquals(expected, result);
        }
    }

}