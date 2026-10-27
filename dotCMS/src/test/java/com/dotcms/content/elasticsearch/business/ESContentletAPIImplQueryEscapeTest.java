package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Verifies how {@link ESContentletAPIImpl#balanceTrailingEscape(StringBuffer)} treats a query that
 * ends in backslashes. The clauses that {@code addPermissionsToQuery} appends are separated from
 * the caller's query by a single space, and a query ending in an unbalanced escape would consume
 * that space, so the escape has to be balanced before anything is appended.
 */
public class ESContentletAPIImplQueryEscapeTest {

    private static String balanced(final String query) {
        final StringBuffer buffy = new StringBuffer(query);
        ESContentletAPIImpl.balanceTrailingEscape(buffy);
        return buffy.toString();
    }

    @Test
    public void test_single_trailing_backslash_is_balanced() {
        assertEquals("+contentType:Blog x\\\\", balanced("+contentType:Blog x\\"));
    }

    @Test
    public void test_odd_run_of_trailing_backslashes_is_balanced() {
        assertEquals("+contentType:Blog x\\\\\\\\", balanced("+contentType:Blog x\\\\\\"));
    }

    @Test
    public void test_even_run_of_trailing_backslashes_is_left_alone() {
        assertEquals("+contentType:Blog x\\\\", balanced("+contentType:Blog x\\\\"));
    }

    @Test
    public void test_query_without_trailing_backslash_is_left_alone() {
        assertEquals("+contentType:Blog +live:true", balanced("+contentType:Blog +live:true"));
    }

    @Test
    public void test_escape_inside_the_query_is_left_alone() {
        assertEquals("+title:a\\ b +deleted:false", balanced("+title:a\\ b +deleted:false"));
    }

    @Test
    public void test_empty_query_is_left_alone() {
        assertEquals("", balanced(""));
    }

    @Test
    public void test_balanced_query_keeps_the_appended_clause_separate() {
        final StringBuffer buffy = new StringBuffer("+contentType:Blog x\\");
        ESContentletAPIImpl.balanceTrailingEscape(buffy);
        buffy.append(" +(permissions:p1.1p*)");

        // The space before '+(' is the separator: it must not be the character the escape consumes
        assertEquals("+contentType:Blog x\\\\ +(permissions:p1.1p*)", buffy.toString());
    }
}
