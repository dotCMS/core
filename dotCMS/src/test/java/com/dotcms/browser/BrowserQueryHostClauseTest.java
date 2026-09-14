package com.dotcms.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.folders.model.Folder;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * The host clause a listing emits when the caller says nothing about System Host.
 *
 * <p><b>This is a characterization test, and it passes the day it is written.</b> That is the
 * point of it. Content Drive is gaining a browse scope that needs the System-Host-only clause,
 * which means replacing {@code forceSystemHost} with a three-state. Seven callers reach this same
 * builder and none of them sets that flag: the assets API, the older file browser and its
 * deprecated tree endpoint, the legacy admin browser, a Velocity viewtool, and two internal
 * callers in the file-asset and folder factories. If the three-state's default emits anything
 * other than the clause asserted below, every one of them silently changes what it returns.</p>
 *
 * <p>A regression guard that failed first would be guarding something else. This one is committed
 * before the refactor and must stay green through it; the assertions below are deliberately about
 * the emitted SQL rather than about how the builder is invoked, so the refactor should not need
 * to touch this file at all. If it does, that is the signal to look harder.</p>
 *
 * <p>Exercised through the real SQL-building method by reflection, with no {@code APILocator}
 * bootstrap and no database, following {@link BrowserAPIMimeTypeQueryTest}.</p>
 */
public class BrowserQueryHostClauseTest {

    private static final String SITE_ID = "48190c8c-42c4-46af-8d1a-0cd5db894797";

    /**
     * Given a query that names a site and says nothing about System Host — the shape every caller
     * outside Content Drive builds — When the statement is built, Then it is scoped to that site
     * alone: one bound identifier, and no System Host literal anywhere in the text.
     */
    @Test
    public void testAQueryThatSaysNothingAboutSystemHostIsScopedToItsSiteAlone() throws Exception {
        final SelectResult result = buildSelect();

        assertTrue("the statement must filter on the site's identifier",
                result.sql.contains("id.host_inode = ?"));
        assertFalse("a caller that asked for nothing must not be given System Host content",
                result.sql.contains("SYSTEM_HOST"));
        assertEquals("the site identifier is bound, never concatenated", SITE_ID,
                String.valueOf(result.params.get(0)));
    }

    /**
     * The same guarantee stated the other way round, because the failure this protects against is
     * a widened clause rather than a missing one: the site predicate must not be OR'd with
     * anything. An {@code OR} in this fragment is how "only this site" becomes "this site plus
     * everything shared", which is exactly the silent change being guarded.
     */
    @Test
    public void testTheSitePredicateIsNotWidenedByAnOr() throws Exception {
        final SelectResult result = buildSelect();

        final int hostClauseStart = result.sql.indexOf("id.host_inode");
        assertTrue("the host predicate must be present to be checked", hostClauseStart >= 0);

        final String hostClause = result.sql.substring(hostClauseStart,
                result.sql.indexOf(')', hostClauseStart) + 1);
        assertFalse("the host predicate must stand alone: " + hostClause,
                hostClause.toUpperCase().contains(" OR "));
    }

    /**
     * The other half of the same guarantee: when the caller asks for shared content — which is
     * what the "Show System Host" chip sends, on by default — the site predicate widens to admit
     * System Host as well.
     * <p>
     * Untested anywhere before this. The refactor that gives this flag a third state passes
     * through here, so without this a broken "chip on" would ship in silence: shared assets would
     * simply stop appearing, with every existing test still green.
     */
    @Test
    public void testAskingForSharedContentWidensThePredicateToAdmitSystemHost() throws Exception {
        final SelectResult result = buildSelect(true);

        assertTrue("the site is still matched", result.sql.contains("id.host_inode = ?"));
        assertTrue("and System Host is admitted alongside it",
                result.sql.contains("id.host_inode = 'SYSTEM_HOST'"));
        assertEquals("the site identifier is still the only bound value", SITE_ID,
                String.valueOf(result.params.get(0)));
    }

    /**
     * Builds the statement for a query that names a site and a folder and nothing else, which is
     * the default shape of every non-Content-Drive caller.
     */
    private static SelectResult buildSelect() throws Exception {
        return buildSelect(false);
    }

    /**
     * @param askForSharedContent what the caller says about System Host. This is the one line the
     *        three-state refactor changes in this file; the assertions above it must not move.
     */
    private static SelectResult buildSelect(final boolean askForSharedContent) throws Exception {
        final BrowserAPIImpl api = mock(BrowserAPIImpl.class, CALLS_REAL_METHODS);
        final BrowserQuery query = mock(BrowserQuery.class, CALLS_REAL_METHODS);

        for (final String emptyCollection : List.of("languageIds", "contentTypeIds",
                "excludedContentTypeIds", "workflowSchemeIds", "workflowStepIds",
                "contentStatuses")) {
            setField(query, emptyCollection, Set.of());
        }
        setField(query, "baseTypes", Set.of(BaseContentType.ANY));
        setField(query, "fieldCriteria", List.of());
        setField(query, "mimeTypes", List.of());
        setField(query, "systemHostMode",
                askForSharedContent ? SystemHostMode.INCLUDE : SystemHostMode.EXCLUDE);

        // Both are mocked rather than constructed: `new Host()` resolves its content type through
        // the legacy cache, which calls APILocator.systemUser() and reaches for a database
        // connection. Only two accessors are read while the statement is built.
        final Host site = mock(Host.class);
        when(site.getIdentifier()).thenReturn(SITE_ID);
        setField(query, "site", site);

        final Folder folder = mock(Folder.class);
        when(folder.getPath()).thenReturn("/");
        setField(query, "folder", folder);

        final Method selectQuery =
                BrowserAPIImpl.class.getDeclaredMethod("selectQuery", BrowserQuery.class);
        selectQuery.setAccessible(true);
        final BrowserAPIImpl.SelectQuery built =
                (BrowserAPIImpl.SelectQuery) selectQuery.invoke(api, query);

        return new SelectResult(built.selectQuery, built.params);
    }

    private static void setField(final BrowserQuery query, final String name, final Object value)
            throws Exception {
        final Field field = BrowserQuery.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(query, value);
    }

    private static class SelectResult {
        private final String sql;
        private final List<Object> params;

        private SelectResult(final String sql, final List<Object> params) {
            this.sql = sql;
            this.params = params;
        }
    }
}
