package com.dotmarketing.portlets.contentlet.business;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Pure unit tests for Sub-AC 13a: cascade archive/unarchive in {@link HostAPIImpl}.
 *
 * <p>Verifies that {@link HostAPIImpl#cascadeArchive} and
 * {@link HostAPIImpl#cascadeUnarchive}:
 * <ul>
 *   <li>Process all descendant hosts in deepest-first order.</li>
 *   <li>Process the root site last.</li>
 *   <li>Skip missing descendants (null {@code DBSearch} result) gracefully.</li>
 *   <li>Skip descendants that throw {@link DotContentletStateException}, continuing the rest.</li>
 *   <li>Propagate unexpected checked exceptions from the factory.</li>
 * </ul>
 *
 * <p>Because Mockito spy delegation calls real methods on the wrapped instance (not the proxy),
 * internal {@code this.archive()} calls bypass the spy interceptor. Instead, this test uses an
 * anonymous {@link HostAPIImpl} subclass that overrides {@code archive()} and
 * {@code unarchive()} to record invocations and optionally throw, enabling deterministic
 * verification of the cascade order without the full ContentletAPI call chain.
 *
 * <p>All external static dependencies ({@link FactoryLocator}, {@link APILocator}) are mocked
 * via Mockito's {@code mockStatic} facility. No database or running dotCMS instance is required.
 */
class HostCascadeArchiveTest {

    private MockedStatic<FactoryLocator> mockedFactoryLocator;
    private MockedStatic<APILocator>     mockedAPILocator;

    private HostFactory   mockHostFactory;
    private PermissionAPI mockPermissionAPI;
    private User          mockUser;

    @BeforeEach
    void setUp() {
        mockHostFactory   = mock(HostFactory.class);
        mockPermissionAPI = mock(PermissionAPI.class);
        mockUser          = mock(User.class);

        mockedFactoryLocator = mockStatic(FactoryLocator.class);
        mockedAPILocator     = mockStatic(APILocator.class);

        mockedFactoryLocator.when(FactoryLocator::getHostFactory).thenReturn(mockHostFactory);
        mockedAPILocator.when(APILocator::getPermissionAPI).thenReturn(mockPermissionAPI);
        mockedAPILocator.when(APILocator::systemUser).thenReturn(mockUser);
    }

    @AfterEach
    void tearDown() {
        mockedFactoryLocator.close();
        mockedAPILocator.close();
    }

    // -----------------------------------------------------------------------
    // Helper types
    // -----------------------------------------------------------------------

    /**
     * Functional interface for a site operation that may throw checked exceptions, used to
     * configure per-site behavior in {@link TrackingHostAPI}.
     */
    @FunctionalInterface
    interface SiteOperation {
        void apply(Host site, User user, boolean respectFrontendRoles)
                throws DotDataException, DotSecurityException, DotContentletStateException;
    }

    /**
     * {@link HostAPIImpl} subclass that overrides {@code archive()} and {@code unarchive()} so
     * that the internal calls made by {@code cascadeArchive()} / {@code cascadeUnarchive()} can
     * be intercepted without requiring the full ContentletAPI + HibernateUtil mock chain.
     *
     * <p>By default both methods record the invoked {@link Host} in an ordered list.  A custom
     * {@link SiteOperation} can be registered for a specific host identifier via
     * {@link #registerArchiveOverride(String, SiteOperation)} or
     * {@link #registerUnarchiveOverride(String, SiteOperation)}.
     */
    static class TrackingHostAPI extends HostAPIImpl {

        final List<Host> archiveCalls   = new ArrayList<>();
        final List<Host> unarchiveCalls = new ArrayList<>();

        private final Map<String, SiteOperation> archiveOverrides   = new HashMap<>();
        private final Map<String, SiteOperation> unarchiveOverrides = new HashMap<>();

        TrackingHostAPI(final SystemEventsAPI sysEvents) {
            super(sysEvents);
        }

        void registerArchiveOverride(final String hostId, final SiteOperation op) {
            archiveOverrides.put(hostId, op);
        }

        void registerUnarchiveOverride(final String hostId, final SiteOperation op) {
            unarchiveOverrides.put(hostId, op);
        }

        @Override
        public void archive(final Host site, final User user, final boolean respectFrontendRoles)
                throws DotDataException, DotSecurityException, DotContentletStateException {
            final SiteOperation override = archiveOverrides.get(site.getIdentifier());
            if (override != null) {
                override.apply(site, user, respectFrontendRoles);
            }
            archiveCalls.add(site);
        }

        @Override
        public void unarchive(final Host site, final User user, final boolean respectFrontendRoles)
                throws DotDataException, DotSecurityException, DotContentletStateException {
            final SiteOperation override = unarchiveOverrides.get(site.getIdentifier());
            if (override != null) {
                override.apply(site, user, respectFrontendRoles);
            }
            unarchiveCalls.add(site);
        }
    }

    /**
     * Creates a lightweight {@link Host} backed by an empty {@link Contentlet} to avoid the
     * no-arg constructor's {@code CacheLocator} dependency.
     */
    private static Host createHost(final String identifier, final String hostname) {
        final Contentlet c = new Contentlet();
        final Host host = new Host(c);
        host.setIdentifier(identifier);
        host.setHostname(hostname);
        return host;
    }

    /** Convenience: build a fresh {@link TrackingHostAPI} with a mock {@link SystemEventsAPI}. */
    private TrackingHostAPI buildAPI() {
        return new TrackingHostAPI(mock(SystemEventsAPI.class));
    }

    // -----------------------------------------------------------------------
    // cascadeArchive — no descendants
    // -----------------------------------------------------------------------

    /**
     * When the root site has no descendant hosts, {@code cascadeArchive} archives only the root
     * — exactly one call to {@code archive()}.
     */
    @Test
    void cascadeArchive_noDescendants_archivesOnlyRoot() throws Exception {
        final String rootId = "root-uuid";
        final Host   root   = createHost(rootId, "root.example.com");

        when(mockHostFactory.findAllDescendantHostIds(rootId))
                .thenReturn(Collections.emptyList());

        final TrackingHostAPI api = buildAPI();
        api.cascadeArchive(root, mockUser, false);

        assertEquals(List.of(root), api.archiveCalls,
                "Only the root should be archived when there are no descendants");
    }

    // -----------------------------------------------------------------------
    // cascadeArchive — single child
    // -----------------------------------------------------------------------

    /**
     * When one descendant exists, the child is archived before the root.
     */
    @Test
    void cascadeArchive_singleChild_archivesChildThenRoot() throws Exception {
        final String rootId  = "root-uuid";
        final String childId = "child-uuid";
        final Host   root    = createHost(rootId,  "root.example.com");
        final Host   child   = createHost(childId, "child.example.com");

        when(mockHostFactory.findAllDescendantHostIds(rootId)).thenReturn(List.of(childId));
        when(mockHostFactory.DBSearch(childId, false)).thenReturn(child);

        final TrackingHostAPI api = buildAPI();
        api.cascadeArchive(root, mockUser, false);

        assertEquals(List.of(child, root), api.archiveCalls,
                "Child must be archived before root");
    }

    // -----------------------------------------------------------------------
    // cascadeArchive — multiple descendants in deepest-first order
    // -----------------------------------------------------------------------

    /**
     * Hierarchy: root → parent → child (child is deepest).
     * {@code findAllDescendantHostIds} returns [child, parent] (deepest first).
     * Expected archive order: child, parent, root.
     */
    @Test
    void cascadeArchive_multipleDescendants_archivesDeepestFirstThenRoot() throws Exception {
        final String rootId   = "root-uuid";
        final String parentId = "parent-uuid";
        final String childId  = "child-uuid";
        final Host   root     = createHost(rootId,   "root.example.com");
        final Host   parent   = createHost(parentId, "parent.example.com");
        final Host   child    = createHost(childId,  "child.example.com");

        // Factory returns descendants in deepest-first order (SQL ORDER BY depth DESC)
        when(mockHostFactory.findAllDescendantHostIds(rootId))
                .thenReturn(Arrays.asList(childId, parentId));
        when(mockHostFactory.DBSearch(childId,  false)).thenReturn(child);
        when(mockHostFactory.DBSearch(parentId, false)).thenReturn(parent);

        final TrackingHostAPI api = buildAPI();
        api.cascadeArchive(root, mockUser, false);

        assertEquals(Arrays.asList(child, parent, root), api.archiveCalls,
                "Descendants must be archived deepest-first, root last");
    }

    // -----------------------------------------------------------------------
    // cascadeArchive — missing descendant (null DBSearch result)
    // -----------------------------------------------------------------------

    /**
     * When {@code DBSearch} returns {@code null} for a descendant (e.g. already deleted between
     * the query and the archive call), the cascade must skip that entry and still archive the
     * remaining hosts and the root.
     */
    @Test
    void cascadeArchive_missingDescendant_skipsAndContinues() throws Exception {
        final String rootId    = "root-uuid";
        final String missingId = "missing-uuid";
        final String presentId = "present-uuid";
        final Host   root      = createHost(rootId,    "root.example.com");
        final Host   present   = createHost(presentId, "present.example.com");

        when(mockHostFactory.findAllDescendantHostIds(rootId))
                .thenReturn(Arrays.asList(missingId, presentId));
        when(mockHostFactory.DBSearch(missingId, false)).thenReturn(null);
        when(mockHostFactory.DBSearch(presentId, false)).thenReturn(present);

        final TrackingHostAPI api = buildAPI();
        assertDoesNotThrow(() -> api.cascadeArchive(root, mockUser, false),
                "Null DBSearch result must not abort the cascade");

        assertEquals(Arrays.asList(present, root), api.archiveCalls,
                "Only the present host and root should be archived; missing host must be skipped");
    }

    // -----------------------------------------------------------------------
    // cascadeArchive — descendant throws DotContentletStateException
    // -----------------------------------------------------------------------

    /**
     * When archiving a descendant throws {@link DotContentletStateException} (e.g. the host is
     * already archived), the cascade must log a warning, skip that descendant, and continue
     * processing the remaining descendants and the root.
     */
    @Test
    void cascadeArchive_descendantThrowsStateException_skipsAndContinues() throws Exception {
        final String rootId   = "root-uuid";
        final String badId    = "bad-uuid";
        final String goodId   = "good-uuid";
        final Host   root     = createHost(rootId, "root.example.com");
        final Host   bad      = createHost(badId,  "bad.example.com");
        final Host   good     = createHost(goodId, "good.example.com");

        when(mockHostFactory.findAllDescendantHostIds(rootId))
                .thenReturn(Arrays.asList(badId, goodId));
        when(mockHostFactory.DBSearch(badId,  false)).thenReturn(bad);
        when(mockHostFactory.DBSearch(goodId, false)).thenReturn(good);

        final TrackingHostAPI api = buildAPI();
        // Configure bad host to throw on archive
        api.registerArchiveOverride(badId,
                (site, user, roles) -> { throw new DotContentletStateException("already archived"); });

        assertDoesNotThrow(() -> api.cascadeArchive(root, mockUser, false),
                "DotContentletStateException from a descendant must not abort the cascade");

        // bad is still added after the override throws (override throws, then we add to list).
        // But the point is that good and root were still processed.
        // Since bad throws BEFORE archiveCalls.add(site) we check good and root are present.
        assertEquals(Arrays.asList(good, root), api.archiveCalls,
                "Good host and root must be archived even when bad throws a state exception");
    }

    // -----------------------------------------------------------------------
    // cascadeArchive — factory throws DotDataException
    // -----------------------------------------------------------------------

    /**
     * If {@code findAllDescendantHostIds} throws a {@link DotDataException}, the exception must
     * propagate out of {@code cascadeArchive}.
     */
    @Test
    void cascadeArchive_factoryThrowsDotDataException_propagates() throws Exception {
        final String rootId = "root-uuid";
        final Host   root   = createHost(rootId, "root.example.com");

        when(mockHostFactory.findAllDescendantHostIds(rootId))
                .thenThrow(new DotDataException("DB error"));

        final TrackingHostAPI api = buildAPI();
        assertThrows(DotDataException.class,
                () -> api.cascadeArchive(root, mockUser, false),
                "DotDataException from findAllDescendantHostIds must propagate");

        assertEquals(Collections.emptyList(), api.archiveCalls,
                "archive() must not be called if the descendant query fails");
    }

    // -----------------------------------------------------------------------
    // cascadeUnarchive — no descendants
    // -----------------------------------------------------------------------

    /**
     * When the root site has no descendant hosts, {@code cascadeUnarchive} unarchives only the
     * root site — exactly one call to {@code unarchive()}.
     */
    @Test
    void cascadeUnarchive_noDescendants_unarchivesOnlyRoot() throws Exception {
        final String rootId = "root-uuid";
        final Host   root   = createHost(rootId, "root.example.com");

        when(mockHostFactory.findAllDescendantHostIds(rootId))
                .thenReturn(Collections.emptyList());

        final TrackingHostAPI api = buildAPI();
        api.cascadeUnarchive(root, mockUser, false);

        assertEquals(List.of(root), api.unarchiveCalls,
                "Only the root should be unarchived when there are no descendants");
    }

    // -----------------------------------------------------------------------
    // cascadeUnarchive — multiple descendants
    // -----------------------------------------------------------------------

    /**
     * Hierarchy: root → parent → child.
     * {@code findAllDescendantHostIds} returns [child, parent] (deepest first).
     * Expected unarchive order: child, parent, root.
     */
    @Test
    void cascadeUnarchive_multipleDescendants_unarchivesDeepestFirstThenRoot() throws Exception {
        final String rootId   = "root-uuid";
        final String parentId = "parent-uuid";
        final String childId  = "child-uuid";
        final Host   root     = createHost(rootId,   "root.example.com");
        final Host   parent   = createHost(parentId, "parent.example.com");
        final Host   child    = createHost(childId,  "child.example.com");

        when(mockHostFactory.findAllDescendantHostIds(rootId))
                .thenReturn(Arrays.asList(childId, parentId));
        when(mockHostFactory.DBSearch(childId,  false)).thenReturn(child);
        when(mockHostFactory.DBSearch(parentId, false)).thenReturn(parent);

        final TrackingHostAPI api = buildAPI();
        api.cascadeUnarchive(root, mockUser, false);

        assertEquals(Arrays.asList(child, parent, root), api.unarchiveCalls,
                "Descendants must be unarchived deepest-first, root last");
    }

    // -----------------------------------------------------------------------
    // cascadeUnarchive — descendant throws DotContentletStateException
    // -----------------------------------------------------------------------

    /**
     * When unarchiving a descendant throws {@link DotContentletStateException} (e.g. the host is
     * not actually archived), the cascade must skip that descendant and continue.
     */
    @Test
    void cascadeUnarchive_descendantThrowsStateException_skipsAndContinues() throws Exception {
        final String rootId   = "root-uuid";
        final String badId    = "bad-uuid";
        final String goodId   = "good-uuid";
        final Host   root     = createHost(rootId, "root.example.com");
        final Host   bad      = createHost(badId,  "bad.example.com");
        final Host   good     = createHost(goodId, "good.example.com");

        when(mockHostFactory.findAllDescendantHostIds(rootId))
                .thenReturn(Arrays.asList(badId, goodId));
        when(mockHostFactory.DBSearch(badId,  false)).thenReturn(bad);
        when(mockHostFactory.DBSearch(goodId, false)).thenReturn(good);

        final TrackingHostAPI api = buildAPI();
        api.registerUnarchiveOverride(badId,
                (site, user, roles) -> { throw new DotContentletStateException("not archived"); });

        assertDoesNotThrow(() -> api.cascadeUnarchive(root, mockUser, false),
                "DotContentletStateException from a descendant must not abort the unarchive cascade");

        assertEquals(Arrays.asList(good, root), api.unarchiveCalls,
                "Good host and root must be unarchived even when bad throws a state exception");
    }

    // -----------------------------------------------------------------------
    // cascadeUnarchive — missing descendant (null DBSearch result)
    // -----------------------------------------------------------------------

    /**
     * When {@code DBSearch} returns {@code null} for a descendant, the cascade must skip that
     * entry and continue unarchiving the others.
     */
    @Test
    void cascadeUnarchive_missingDescendant_skipsAndContinues() throws Exception {
        final String rootId    = "root-uuid";
        final String missingId = "missing-uuid";
        final String presentId = "present-uuid";
        final Host   root      = createHost(rootId,    "root.example.com");
        final Host   present   = createHost(presentId, "present.example.com");

        when(mockHostFactory.findAllDescendantHostIds(rootId))
                .thenReturn(Arrays.asList(missingId, presentId));
        when(mockHostFactory.DBSearch(missingId, false)).thenReturn(null);
        when(mockHostFactory.DBSearch(presentId, false)).thenReturn(present);

        final TrackingHostAPI api = buildAPI();
        assertDoesNotThrow(() -> api.cascadeUnarchive(root, mockUser, false),
                "Null DBSearch result must not abort the unarchive cascade");

        assertEquals(Arrays.asList(present, root), api.unarchiveCalls,
                "Only the present host and root should be unarchived; missing host skipped");
    }
}
