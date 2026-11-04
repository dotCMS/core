package com.dotcms.content.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.Test;

/**
 * Unit tests for the parts of {@link ContentFieldFilter} that need neither the configuration nor the
 * database: how values are masked and how queries and sorts on restricted properties are detected.
 */
public class ContentFieldFilterTest {

    private static final Set<String> RESTRICTED = Set.of("moduser", "owner", "publishuser");

    /**
     * Given an email address, a user ID and short values, when they are masked, then the first and last
     * characters of each part are kept and short parts are masked completely.
     */
    @Test
    public void test_mask_keepsFirstAndLastCharacterOfEachPart() {
        assertEquals("j*****n.e*****m", ContentFieldFilter.mask("john@example.com"));
        assertEquals("d*****1", ContentFieldFilter.mask("dotcms.org.1"));
        assertEquals("*****", ContentFieldFilter.mask("ab"));
        assertEquals("*****", ContentFieldFilter.mask("abc"));
        assertEquals("a*****d", ContentFieldFilter.mask("abcd"));
        assertEquals("*****.e*****m", ContentFieldFilter.mask("@example.com"));
        assertEquals("j*****n.*****", ContentFieldFilter.mask("john@x"));
        assertEquals("", ContentFieldFilter.mask(""));
        assertNull(ContentFieldFilter.mask(null));
    }

    /**
     * Given a value that is already masked, when it is masked again, then nothing more of it is revealed,
     * so filtering the same content twice is harmless.
     */
    @Test
    public void test_mask_twiceRevealsNothingMore() {
        final String once = ContentFieldFilter.mask("john@example.com");
        assertEquals("j*****m", ContentFieldFilter.mask(once));
        assertEquals("d*****1", ContentFieldFilter.mask(ContentFieldFilter.mask("dotcms.org.1")));
    }

    /**
     * Given a user returned as a nested map, a JSON object and a JSON array, when they are masked, then
     * they keep their type and every value inside them is masked.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void test_maskValue_keepsTheShapeOfNestedValues() {
        final Map<String, Object> user = new LinkedHashMap<>();
        user.put("userId", "dotcms.org.1");
        user.put("email", "john@example.com");
        user.put("active", true);

        final Map<String, Object> maskedMap = (Map<String, Object>) ContentFieldFilter.maskValue(user);
        assertEquals("d*****1", maskedMap.get("userId"));
        assertEquals("j*****n.e*****m", maskedMap.get("email"));
        assertEquals("t*****e", maskedMap.get("active"));
        assertEquals("the source map must not be changed", "dotcms.org.1", user.get("userId"));

        final Object maskedJson = ContentFieldFilter.maskValue(new JSONObject(user));
        assertTrue(maskedJson instanceof JSONObject);
        assertEquals("d*****1", ((JSONObject) maskedJson).get("userId"));

        final Object maskedArray = ContentFieldFilter.maskValue(new JSONArray(List.of("dotcms.org.1", "john@example.com")));
        assertTrue(maskedArray instanceof JSONArray);
        assertEquals("d*****1", ((JSONArray) maskedArray).get(0));

        assertNull(ContentFieldFilter.maskValue(null));
        assertEquals("1*****5", ContentFieldFilter.maskValue(12345));
    }

    /**
     * Given queries that name a restricted property in any of the ways the index allows, when they are
     * checked, then every one of them is detected.
     */
    @Test
    public void test_queryReferences_detectsRestrictedFields() {
        assertTrue(ContentFieldFilter.queryReferences("+modUser:dotcms.org.1", RESTRICTED));
        assertTrue(ContentFieldFilter.queryReferences("+MODUSER:dotcms*", RESTRICTED));
        assertTrue(ContentFieldFilter.queryReferences("+contentType:News +owner : x", RESTRICTED));
        assertTrue(ContentFieldFilter.queryReferences("+sysPublishUser:dotcms.org.1", RESTRICTED));
        assertTrue(ContentFieldFilter.queryReferences("+moduser_dotraw:dot*", RESTRICTED));
        assertTrue(ContentFieldFilter.queryReferences("+News.owner_text:x", RESTRICTED));
        assertTrue(ContentFieldFilter.queryReferences("+mod*:x", RESTRICTED));
        assertTrue(ContentFieldFilter.queryReferences("+own?r:x", RESTRICTED));
        assertTrue(ContentFieldFilter.queryReferences("+mod\\User:x", RESTRICTED));
        assertTrue(ContentFieldFilter.queryReferences("+(title:a OR modUser:b)", RESTRICTED));
        assertTrue(ContentFieldFilter.queryReferences("+*:dotcms.org.1", RESTRICTED));
    }

    /**
     * Given queries that do not name a restricted property, including the match-all clause, when they are
     * checked, then they are allowed.
     */
    @Test
    public void test_queryReferences_allowsOtherQueries() {
        assertFalse(ContentFieldFilter.queryReferences("*:*", RESTRICTED));
        assertFalse(ContentFieldFilter.queryReferences("+*:* +contentType:News", RESTRICTED));
        assertFalse(ContentFieldFilter.queryReferences("+contentType:News +title:owner +languageId:1", RESTRICTED));
        assertFalse(ContentFieldFilter.queryReferences("+modDate:[2020-01-01 TO 2021-01-01]", RESTRICTED));
        assertFalse(ContentFieldFilter.queryReferences("+ownerCanRead:true", RESTRICTED));
        assertFalse(ContentFieldFilter.queryReferences("+system:true", RESTRICTED));
        assertFalse(ContentFieldFilter.queryReferences("+url:http\\://example.com", RESTRICTED));
        assertFalse(ContentFieldFilter.queryReferences("", RESTRICTED));
        assertFalse(ContentFieldFilter.queryReferences(null, RESTRICTED));
        assertFalse(ContentFieldFilter.queryReferences("+modUser:x", Set.of()));
    }

    /**
     * Given sort clauses, when they are checked, then the ones that sort by a restricted property are
     * detected and the others are allowed.
     */
    @Test
    public void test_sortReferences() {
        assertTrue(ContentFieldFilter.sortReferences("modUser desc", RESTRICTED));
        assertTrue(ContentFieldFilter.sortReferences("title asc, owner", RESTRICTED));
        assertTrue(ContentFieldFilter.sortReferences("moduser_dotraw", RESTRICTED));
        assertFalse(ContentFieldFilter.sortReferences("modDate desc", RESTRICTED));
        assertFalse(ContentFieldFilter.sortReferences("title asc, score", RESTRICTED));
        assertFalse(ContentFieldFilter.sortReferences(null, RESTRICTED));
    }

    /**
     * Given the keys of an index document, when the ones to leave out of catchall are picked, then every
     * key that holds a restricted property is picked, whatever its variant, and no other key is.
     */
    @Test
    public void test_catchallExclusion_picksEveryIndexVariantOfRestrictedFields() {
        final Predicate<String> excluded = ContentFieldFilter.catchallExclusion(Set.of("moduser", "publishuser", "ssn"));
        assertTrue(excluded.test("moduser"));
        assertTrue(excluded.test("moduser_dotraw"));
        assertTrue(excluded.test("syspublishuser"));
        assertTrue(excluded.test("employee.ssn"));
        assertTrue(excluded.test("employee.ssn_text"));
        assertTrue(excluded.test("employee.ssn_sha256"));
        assertFalse(excluded.test("owner"));
        assertFalse(excluded.test("ownercanread"));
        assertFalse(excluded.test("employee.ssnissuer"));
        assertFalse(excluded.test("title"));
        assertFalse(excluded.test("identifier"));

        assertFalse(ContentFieldFilter.catchallExclusion(Set.of()).test("moduser"));
    }

    /**
     * Given no user and the anonymous user, when trust is checked, then neither is trusted, without
     * looking up any role.
     */
    @Test
    public void test_isTrusted_rejectsMissingAndAnonymousUsers() {
        assertFalse(ContentFieldFilter.isTrusted(null));

        final User anonymous = mock(User.class);
        when(anonymous.isAnonymousUser()).thenReturn(true);
        when(anonymous.isAdmin()).thenReturn(true);
        assertFalse(ContentFieldFilter.isTrusted(anonymous));
    }

    /**
     * Given an administrator, a back-end user and a front-end user, when trust is checked, then only the
     * front-end user is filtered.
     */
    @Test
    public void test_isTrusted_acceptsAdminsAndBackendUsers() {
        final User admin = mock(User.class);
        when(admin.isAdmin()).thenReturn(true);
        assertTrue(ContentFieldFilter.isTrusted(admin));

        final User backend = mock(User.class);
        when(backend.isBackendUser()).thenReturn(true);
        assertTrue(ContentFieldFilter.isTrusted(backend));

        final User frontend = mock(User.class);
        assertFalse(ContentFieldFilter.isTrusted(frontend));
    }

    /**
     * Given the result of looking up the search-refused message, when it is a translation it is used as is,
     * and when the lookup failed or only gave the key back, the English text is used instead.
     */
    @Test
    public void test_localizedOrDefault_fallsBackToEnglishText() {
        assertEquals("Consulta no permitida", ContentFieldFilter.localizedOrDefault("Consulta no permitida"));
        assertEquals(ContentFieldFilter.SEARCH_REJECTED_DEFAULT_MESSAGE,
                ContentFieldFilter.localizedOrDefault(ContentFieldFilter.SEARCH_REJECTED_MESSAGE_KEY));
        assertEquals(ContentFieldFilter.SEARCH_REJECTED_DEFAULT_MESSAGE, ContentFieldFilter.localizedOrDefault(null));
        assertEquals(ContentFieldFilter.SEARCH_REJECTED_DEFAULT_MESSAGE, ContentFieldFilter.localizedOrDefault(""));
    }

}
