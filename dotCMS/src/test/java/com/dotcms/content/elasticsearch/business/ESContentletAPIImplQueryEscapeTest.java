package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.business.Role;
import com.liferay.portal.model.User;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Verifies how {@link ESContentletAPIImpl#balanceTrailingEscape(StringBuffer)} treats a query that
 * ends in backslashes, and that a query normalized by it keeps the clauses that
 * {@link ESContentletAPIImpl#addPermissionsToQuery} appends as clauses of their own. Those clauses
 * are separated from the caller's query by a single space, which a query ending in an unbalanced
 * escape would otherwise consume.
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
    public void test_query_made_only_of_backslashes_is_balanced() {
        assertEquals("\\\\", balanced("\\"));
    }

    @Test
    public void test_query_ending_in_an_escaped_space_is_left_alone() {
        assertEquals("+title:a\\ ", balanced("+title:a\\ "));
    }

    @Test
    public void test_clauses_added_by_addPermissionsToQuery_stay_separate() throws Exception {
        final User user = Mockito.mock(User.class);
        Mockito.when(user.getUserId()).thenReturn("u1");
        final Role role = Mockito.mock(Role.class);
        Mockito.when(role.getId()).thenReturn("r1");

        // The real method runs without the constructor, which would reach out to the APILocator
        final ESContentletAPIImpl contentletAPI =
                Mockito.mock(ESContentletAPIImpl.class, Mockito.CALLS_REAL_METHODS);
        final StringBuffer buffy = new StringBuffer("+contentType:Blog x\\");
        contentletAPI.addPermissionsToQuery(buffy, user, List.of(role), false);

        assertTrue("the clause must not be folded into the caller's last term: " + buffy,
                buffy.toString().startsWith("+contentType:Blog x\\\\ +(("));
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
