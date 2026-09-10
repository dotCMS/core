package com.dotcms.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import com.dotcms.contenttype.model.type.BaseContentType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * Unit tests for browser MIME filter validation and SQL generation.
 *
 * <p>The value reaching this fragment comes straight from a request body, so the two guarantees
 * that matter are that it is bound as a parameter rather than concatenated into the statement,
 * and that the builder rejects values outside the MIME type character set.</p>
 *
 * <p>SQL-building methods are exercised via reflection without an {@code APILocator} bootstrap
 * or database connection.</p>
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
                "a;b", "a|b", "a[b", "a]b", "a$b", "a?b", "a,b", "a\nb", "", "a\u0000b",
                "text/plain; charset=utf-8", "a".repeat(256)}) {
            assertThrows("must reject: " + rejected, IllegalArgumentException.class,
                    () -> BrowserQuery.builder().showMimeTypes(List.of(rejected)));
        }
    }

    @Test
    public void testWellFormedMimeTypesAreAcceptedByTheBuilder() {
        BrowserQuery.builder().showMimeTypes(List.of("image", "image/*", "application/pdf",
                "text/plain", "image/svg+xml", "application/vnd.hzn-3d-crossword", "a".repeat(255)));
    }

    /**
     * A null list means "no MIME type filter". Callers on their default path pass null, and the
     * query builder treats null and empty identically, so it must not be rejected.
     */
    @Test
    public void testNullMimeTypeListIsTreatedAsNoFilter() {
        BrowserQuery.builder().showMimeTypes(null);
    }

    @Test
    public void testValidatedMimeTypesAreCopied() throws Exception {
        final List<String> mimeTypes = new ArrayList<>(List.of("image/png"));
        final BrowserQuery.Builder builder = BrowserQuery.builder().showMimeTypes(mimeTypes);
        mimeTypes.set(0, "a\"b");

        final Field field = BrowserQuery.Builder.class.getDeclaredField("mimeTypes");
        field.setAccessible(true);
        assertEquals(List.of("image/png"), field.get(builder));
    }

    @Test
    public void testEmptyMimeTypeListOmitsFilterFromSelectQuery() throws Exception {
        // Avoid the API bootstrap in both constructors; exercise the real SQL-building methods.
        final BrowserAPIImpl api = mock(BrowserAPIImpl.class, CALLS_REAL_METHODS);
        final BrowserQuery query = mock(BrowserQuery.class, CALLS_REAL_METHODS);
        for (final String name : List.of("languageIds", "contentTypeIds", "excludedContentTypeIds",
                "workflowSchemeIds", "workflowStepIds", "contentStatuses")) {
            setQueryField(query, name, Set.of());
        }
        setQueryField(query, "baseTypes", Set.of(BaseContentType.ANY));
        setQueryField(query, "fieldCriteria", List.of());
        setQueryField(query, "mimeTypes", List.of());

        final Method method = BrowserAPIImpl.class.getDeclaredMethod("selectQuery", BrowserQuery.class);
        method.setAccessible(true);
        final BrowserAPIImpl.SelectQuery result = (BrowserAPIImpl.SelectQuery) method.invoke(api, query);

        assertFalse("no MIME predicate is emitted", result.selectQuery.contains("jsonb_path_exists"));
        assertFalse("no empty predicate group is emitted", result.selectQuery.contains("AND ()"));
        assertTrue("no value is bound for an empty list", result.params.isEmpty());
    }

    private static void setQueryField(final BrowserQuery query, final String name, final Object value)
            throws Exception {
        final Field field = BrowserQuery.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(query, value);
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
