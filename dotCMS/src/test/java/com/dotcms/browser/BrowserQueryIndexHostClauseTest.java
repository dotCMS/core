package com.dotcms.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.folders.model.Folder;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * The host clause the <em>index</em> query carries, and that it agrees with the one the SQL
 * carries.
 *
 * <p>Two builders answer the same question about the same request. ADR-0018 makes the database
 * authoritative for the structural criteria — parent folder and site, System Host included — and
 * says such criteria "must never be silently re-routed to the index". Two builders disagreeing
 * about one of them is the failure that decision exists to prevent, so this is a correctness fix
 * rather than tidying.</p>
 *
 * <p>The disagreement was narrow and easy to miss: the index builder widened the clause to admit
 * System Host whenever the folder happened to be the system folder, no matter what the caller
 * asked for. It only surfaces under the non-default {@code PURE_ES} heuristic, which is why it
 * has gone unnoticed, and why it is pinned here rather than left to an integration run.</p>
 *
 * <p>Exercised through the real query-building method by reflection, with no {@code APILocator}
 * bootstrap and no database, following {@link BrowserAPIMimeTypeQueryTest}.</p>
 */
public class BrowserQueryIndexHostClauseTest {

    private static final String SITE_ID = "48190c8c-42c4-46af-8d1a-0cd5db894797";

    /**
     * Given a caller that says nothing about System Host, When the index query is built at the
     * site root, Then it matches that site alone.
     * <p>
     * This is the case that was wrong. The site root resolves to the system folder, and the
     * builder used to read that as a reason to admit System Host regardless of the request.
     */
    @Test
    public void testAtTheSiteRootSayingNothingStillMatchesThatSiteAlone() throws Exception {
        final String query = buildIndexQuery(SystemHostMode.EXCLUDE, true);

        assertTrue("the site must be matched", query.contains("+conhost:" + SITE_ID));
        assertFalse("being at the root is not a request for shared content: " + query,
                query.contains("SYSTEM_HOST"));
    }

    /**
     * Given a caller asking for shared content, When the index query is built, Then the clause
     * widens to admit System Host alongside the site.
     */
    @Test
    public void testAskingForSharedContentAdmitsSystemHostAlongsideTheSite() throws Exception {
        final String query = buildIndexQuery(SystemHostMode.INCLUDE, true);

        assertTrue("both must be admitted: " + query,
                query.contains("+(conhost:" + SITE_ID + " OR conhost:SYSTEM_HOST)"));
    }

    /**
     * Given a caller asking for System Host alone, When the index query is built, Then the site is
     * not matched at all — it is context for the request, not a filter on it.
     */
    @Test
    public void testAskingForSystemHostAloneDoesNotMatchTheSite() throws Exception {
        final String query = buildIndexQuery(SystemHostMode.ONLY, true);

        assertTrue("System Host must be matched", query.contains("+conhost:SYSTEM_HOST"));
        assertFalse("the site must not be matched as well: " + query,
                query.contains(SITE_ID));
    }

    /**
     * Inside a folder the two builders already agreed, and must keep agreeing: the site is matched
     * and nothing else is admitted.
     */
    @Test
    public void testInsideAFolderTheSiteAloneIsMatched() throws Exception {
        final String query = buildIndexQuery(SystemHostMode.EXCLUDE, false);

        assertTrue("the site must be matched", query.contains("+conhost:" + SITE_ID));
        assertFalse("nothing else may be admitted: " + query, query.contains("SYSTEM_HOST"));
    }

    /**
     * The point of the whole exercise, stated directly: for one request, both builders name the
     * same hosts. Asserted on the clause each emits rather than on results, because the index
     * builder runs under a heuristic that gives up read-your-writes (ADR-0018), so comparing
     * returned content would be comparing two different moments.
     */
    @Test
    public void testBothBuildersNameTheSameHostsForTheSameRequest() throws Exception {
        for (final SystemHostMode mode : SystemHostMode.values()) {
            final String indexQuery = buildIndexQuery(mode, true);
            final String sql = buildSelect(mode);

            final boolean indexAdmitsSystemHost = indexQuery.contains("SYSTEM_HOST");
            final boolean sqlAdmitsSystemHost = sql.contains("SYSTEM_HOST");
            assertEquals(mode + ": the two builders must agree about System Host",
                    sqlAdmitsSystemHost, indexAdmitsSystemHost);

            final boolean indexMatchesSite = indexQuery.contains(SITE_ID);
            // The SQL binds the site as a parameter rather than inlining it, so its equivalent of
            // "matches the site" is the presence of the bound predicate.
            final boolean sqlMatchesSite = sql.contains("id.host_inode = ?");
            assertEquals(mode + ": the two builders must agree about the site",
                    sqlMatchesSite, indexMatchesSite);
        }
    }

    private static String buildIndexQuery(final SystemHostMode mode, final boolean atSiteRoot)
            throws Exception {
        final BrowserAPIImpl api = mock(BrowserAPIImpl.class, CALLS_REAL_METHODS);
        final BrowserQuery query = baseQuery(mode, atSiteRoot);

        final Method method =
                BrowserAPIImpl.class.getDeclaredMethod("buildPureESQuery", BrowserQuery.class);
        method.setAccessible(true);

        return (String) method.invoke(api, query);
    }

    private static String buildSelect(final SystemHostMode mode) throws Exception {
        final BrowserAPIImpl api = mock(BrowserAPIImpl.class, CALLS_REAL_METHODS);
        final BrowserQuery query = baseQuery(mode, true);

        final Method method =
                BrowserAPIImpl.class.getDeclaredMethod("selectQuery", BrowserQuery.class);
        method.setAccessible(true);
        final BrowserAPIImpl.SelectQuery built =
                (BrowserAPIImpl.SelectQuery) method.invoke(api, query);

        return built.selectQuery;
    }

    private static BrowserQuery baseQuery(final SystemHostMode mode, final boolean atSiteRoot)
            throws Exception {
        final BrowserQuery query = mock(BrowserQuery.class, CALLS_REAL_METHODS);

        for (final String emptyCollection : List.of("languageIds", "contentTypeIds",
                "excludedContentTypeIds", "workflowSchemeIds", "workflowStepIds",
                "contentStatuses")) {
            setField(query, emptyCollection, Set.of());
        }
        setField(query, "baseTypes", Set.of());
        setField(query, "fieldCriteria", List.of());
        setField(query, "mimeTypes", List.of());
        setField(query, "systemHostMode", mode);

        // Mocked rather than constructed: `new Host()` resolves its content type through the
        // legacy cache, which calls APILocator.systemUser() and reaches for a database connection.
        final Host site = mock(Host.class);
        when(site.getIdentifier()).thenReturn(SITE_ID);
        setField(query, "site", site);

        final Folder folder = mock(Folder.class);
        when(folder.isSystemFolder()).thenReturn(atSiteRoot);
        when(folder.getPath()).thenReturn(atSiteRoot ? "/" : "/application/");
        when(folder.getHostId()).thenReturn(SITE_ID);
        setField(query, "folder", folder);

        return query;
    }

    private static void setField(final BrowserQuery query, final String name, final Object value)
            throws Exception {
        final Field field = BrowserQuery.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(query, value);
    }
}
