package com.dotcms.graphql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.UserAPI;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;

/**
 * Tests for the TTL, mutation and resolved-user handling in {@link GraphqlCacheWebInterceptor}.
 */
public class GraphqlCacheWebInterceptorTest {

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#capTTL(int, int)}
     * Given Scenario: a requested TTL above the configured maximum
     * ExpectedResult: the maximum is applied
     */
    @Test
    public void capTTL_capsValuesAboveTheMaximum() {
        assertEquals(3600, GraphqlCacheWebInterceptor.capTTL(999999, 3600));
        assertEquals(3600, GraphqlCacheWebInterceptor.capTTL(3601, 3600));
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#capTTL(int, int)}
     * Given Scenario: a requested TTL at or below the configured maximum
     * ExpectedResult: the requested value is kept
     */
    @Test
    public void capTTL_keepsValuesWithinTheMaximum() {
        assertEquals(15, GraphqlCacheWebInterceptor.capTTL(15, 3600));
        assertEquals(3600, GraphqlCacheWebInterceptor.capTTL(3600, 3600));
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#capTTL(int, int)}
     * Given Scenario: the bypass (0) and invalidate (-1) values
     * ExpectedResult: they are returned unchanged so they keep their meaning
     */
    @Test
    public void capTTL_keepsSpecialValues() {
        assertEquals(0, GraphqlCacheWebInterceptor.capTTL(0, 3600));
        assertEquals(GraphqlCacheWebInterceptor.INVALIDATE_CACHE,
                GraphqlCacheWebInterceptor.capTTL(GraphqlCacheWebInterceptor.INVALIDATE_CACHE, 3600));
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#capTTL(int, int)}
     * Given Scenario: a non-positive maximum
     * ExpectedResult: no cap is applied
     */
    @Test
    public void capTTL_nonPositiveMaximumDisablesTheCap() {
        assertEquals(999999, GraphqlCacheWebInterceptor.capTTL(999999, 0));
        assertEquals(999999, GraphqlCacheWebInterceptor.capTTL(999999, -1));
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#isMutation(String)}
     * Given Scenario: bodies that run a mutation, as a single operation, as the selected one of
     * several, as a raw document and inside a batch
     * ExpectedResult: all of them are flagged
     */
    @Test
    public void isMutation_flagsBodiesThatRunAMutation() {
        assertTrue(GraphqlCacheWebInterceptor.isMutation(
                "{\"query\":\"mutation { publish(id: \\\"1\\\") { title } }\"}"));
        assertTrue(GraphqlCacheWebInterceptor.isMutation(
                "{\"query\":\"query Pages { page(url: \\\"/\\\") { title } }\\nmutation Publish { publish(id: \\\"1\\\") { title } }\",\"operationName\":\"Publish\"}"));
        assertTrue(GraphqlCacheWebInterceptor.isMutation(
                "mutation { publish(id: \"1\") { title } }"));
        assertTrue(GraphqlCacheWebInterceptor.isMutation(
                "[{\"query\":\"{ BlogCollection { title } }\"},{\"query\":\"mutation { publish(id: \\\"1\\\") { title } }\"}]"));
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#isMutation(String)}
     * Given Scenario: queries, including ones that carry the word in a literal, a variable or an
     * unselected operation
     * ExpectedResult: none of them is flagged
     */
    @Test
    public void isMutation_ignoresQueriesThatOnlyMentionTheWord() {
        assertFalse(GraphqlCacheWebInterceptor.isMutation(
                "{\"query\":\"{ BlogCollection { title } }\"}"));
        assertFalse(GraphqlCacheWebInterceptor.isMutation(
                "{\"query\":\"{ BlogCollection(query: \\\"+title:mutation\\\") { title } }\"}"));
        assertFalse(GraphqlCacheWebInterceptor.isMutation(
                "{\"query\":\"query Search($text: String) { BlogCollection(query: $text) { title } }\",\"variables\":{\"text\":\"mutation\"}}"));
        assertFalse(GraphqlCacheWebInterceptor.isMutation(
                "{\"query\":\"query Pages { page(url: \\\"/\\\") { title } }\\nmutation Publish { publish(id: \\\"1\\\") { title } }\",\"operationName\":\"Pages\"}"));
        assertFalse(GraphqlCacheWebInterceptor.isMutation(
                "{ mutation: BlogCollection { title } }"));
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#resolvedToUser(HttpServletRequest)}
     * Given Scenario: the servlet left no user, the anonymous user, or a real user on the request
     * ExpectedResult: only the real user counts as resolved
     */
    @Test
    public void resolvedToUser_requiresARealUser() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        assertFalse(GraphqlCacheWebInterceptor.resolvedToUser(request));

        when(request.getAttribute(WebKeys.USER_ID)).thenReturn(UserAPI.CMS_ANON_USER_ID);
        assertFalse(GraphqlCacheWebInterceptor.resolvedToUser(request));

        when(request.getAttribute(WebKeys.USER_ID)).thenReturn("dotcms.org.1");
        assertTrue(GraphqlCacheWebInterceptor.resolvedToUser(request));
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#isMutation(String)}
     * Given Scenario: a body that is not a GraphQL document
     * ExpectedResult: it is kept out of the cache
     */
    @Test
    public void isMutation_flagsUnparseableBodies() {
        assertTrue(GraphqlCacheWebInterceptor.isMutation("{\"query\":\"{ Blog\"}"));
        assertTrue(GraphqlCacheWebInterceptor.isMutation("not a document"));
    }
}
