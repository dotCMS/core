package com.dotcms.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Pure unit test for the MIME type SQL fragment built by
 * {@link com.dotcms.browser.BrowserAPIImpl}{@code #appendMIMETypeQuery}.
 *
 * <p>The value reaching this fragment comes straight from a request body, so the two guarantees
 * that matter are that it is bound as a parameter rather than concatenated into the statement,
 * and that the builder rejects values outside the MIME type character set.</p>
 *
 * <p>The method is a DB-free string builder, exercised via reflection so no
 * {@code BrowserAPIImpl} instance (and thus no {@code APILocator} bootstrap) is required.</p>
 */
public class BrowserAPIMimeTypeQueryTest {

    private static Result invoke(final List<String> mimeTypes) throws Exception {
        final Method method = BrowserAPIImpl.class.getDeclaredMethod("appendMIMETypeQuery",
                StringBuilder.class, List.class, List.class);
        method.setAccessible(true);
        final StringBuilder sql = new StringBuilder();
        final List<Object> params = new ArrayList<>();
        method.invoke(null, sql, mimeTypes, params);
        return new Result(sql.toString(), params);
    }

    @Test
    public void testMimeTypeIsBoundAsAParameterNotConcatenated() throws Exception {
        final Result result = invoke(List.of("application/pdf"));

        assertFalse("the MIME type must not appear in the statement text",
                result.sql.contains("application/pdf"));
        assertTrue("the statement must carry a placeholder instead", result.sql.contains("?"));
        assertEquals("exactly one value is bound", 1, result.params.size());
        assertTrue("the bound value carries the MIME type",
                String.valueOf(result.params.get(0)).contains("application/pdf"));
    }

    /**
     * The wildcard forms the file browser sends must keep working. These rely on {@code *} being
     * a regex metacharacter in the generated JSONPath, so they must not be escaped away.
     */
    @Test
    public void testWildcardAndPunctuatedMimeTypesAreAccepted() throws Exception {
        // application/dotpage is deliberately absent: it takes the structure-type branch and binds
        // nothing, which testDotPageStillResolvesToAStructureTypeAndBindsNothing covers.
        for (final String accepted : new String[]{"image/*", "text/*", "application/pdf",
                "application/vnd.hzn-3d-crossword", "application/x-7z-compressed", "image/svg+xml"}) {
            final Result result = invoke(List.of(accepted));
            assertEquals(accepted + " must bind one value", 1, result.params.size());
        }
    }

    @Test
    public void testDotPageStillResolvesToAStructureTypeAndBindsNothing() throws Exception {
        final Result result = invoke(List.of("application/dotpage"));

        assertTrue("dotPage is matched by structure type, not by metadata",
                result.sql.contains("struc.structuretype"));
        assertTrue("no value is bound for the dotPage branch", result.params.isEmpty());
    }

    @Test
    public void testMultipleMimeTypesBindOneParameterEachInOrder() throws Exception {
        final Result result = invoke(List.of("image/png", "application/pdf"));

        assertEquals(2, result.params.size());
        assertTrue(String.valueOf(result.params.get(0)).contains("image/png"));
        assertTrue(String.valueOf(result.params.get(1)).contains("application/pdf"));
        assertEquals("both branches are combined with OR", 1,
                result.sql.split(" OR ", -1).length - 1);
    }

    /**
     * Values outside the MIME type character set are refused where the query is built, so every
     * caller of {@code showMimeTypes} is covered without each having to validate. The cases below
     * walk the characters that carry meaning in the surrounding SQL and JSONPath: quotes, string
     * and expression delimiters, escapes, statement separators and whitespace.
     */
    @Test
    public void testMalformedMimeTypesAreRejectedByTheBuilder() {
        for (final String rejected : new String[]{"a\"b", "a'b", "a(b", "a)b", "a\\b", "a b",
                "a;b", "a|b", "a[b", "a]b", "a$b", "a?b", "a,b", "a\nb", "", "a\u0000b"}) {
            assertThrows("must reject: " + rejected, IllegalArgumentException.class,
                    () -> BrowserQuery.builder().showMimeTypes(List.of(rejected)));
        }
    }

    @Test
    public void testWellFormedMimeTypesAreAcceptedByTheBuilder() {
        BrowserQuery.builder().showMimeTypes(List.of("image/*", "application/pdf", "text/plain"));
    }

    private static class Result {
        private final String sql;
        private final List<Object> params;

        private Result(final String sql, final List<Object> params) {
            this.sql = sql;
            this.params = params;
        }
    }
}
