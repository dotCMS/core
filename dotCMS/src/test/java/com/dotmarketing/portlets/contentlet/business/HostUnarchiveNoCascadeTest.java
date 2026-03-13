package com.dotmarketing.portlets.contentlet.business;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Pure unit tests that verify Sub-AC 13b: the manual {@link HostAPIImpl#unarchive} operation
 * restores <em>only</em> the targeted host and does <strong>not</strong> cascade to its
 * descendants.
 *
 * <p>The suite covers three related behaviours:
 * <ol>
 *   <li>Direct call to {@code unarchive(host)} results in exactly one host being unarchived (the
 *       targeted host), while {@link HostFactory#findAllDescendantHostIds} is never invoked.</li>
 *   <li>Descendants that were archived remain archived after a non-cascade {@code unarchive} of
 *       the parent — modelled via the tracking subclass call count.</li>
 *   <li>By contrast, {@code cascadeUnarchive(host)} <em>does</em> invoke
 *       {@link HostFactory#findAllDescendantHostIds} and calls {@code unarchive} for each
 *       descendant, confirming that the two methods differ intentionally in scope.</li>
 * </ol>
 *
 * <h2>Test design</h2>
 * <p>Because {@link HostCache} cannot be mocked in unit tests without a running dotCMS container
 * (its static initializer requires the cache infrastructure), this suite uses a
 * {@link NoopFlushHostAPIImpl} subclass that overrides the package-private
 * {@link HostAPIImpl#flushAllCaches} method with a no-op.  For cascade tests a
 * {@link TrackingHostAPIImpl} subclass additionally overrides {@code archive()} and
 * {@code unarchive()} to record call counts without invoking the full {@link ContentletAPI}
 * pipeline.</p>
 *
 * <p>All external static dependencies ({@link FactoryLocator}, {@link APILocator},
 * {@link HibernateUtil}) are mocked via Mockito's {@code mockStatic} facility.</p>
 */
class HostUnarchiveNoCascadeTest {

    // -----------------------------------------------------------------------
    // Static-mock handles (static mocks must be opened before constructing SUT
    // because HostAPIImpl reads CacheLocator in its field initialiser).
    // -----------------------------------------------------------------------
    private MockedStatic<FactoryLocator> mockedFactoryLocator;
    private MockedStatic<APILocator>     mockedAPILocator;
    private MockedStatic<HibernateUtil>  mockedHibernateUtil;

    // -----------------------------------------------------------------------
    // Collaborator mocks
    // -----------------------------------------------------------------------
    private HostFactory   mockHostFactory;
    private ContentletAPI mockContentletAPI;
    private User          mockUser;

    @BeforeEach
    void setUp() {
        mockHostFactory   = mock(HostFactory.class);
        mockContentletAPI = mock(ContentletAPI.class);
        mockUser          = mock(User.class);

        mockedFactoryLocator = mockStatic(FactoryLocator.class);
        mockedAPILocator     = mockStatic(APILocator.class);
        mockedHibernateUtil  = mockStatic(HibernateUtil.class);

        mockedFactoryLocator.when(FactoryLocator::getHostFactory).thenReturn(mockHostFactory);
        mockedAPILocator.when(APILocator::getContentletAPI).thenReturn(mockContentletAPI);
        // HibernateUtil.addCommitListener — default is do-nothing (no explicit stub needed).
    }

    @AfterEach
    void tearDown() {
        mockedFactoryLocator.close();
        mockedAPILocator.close();
        mockedHibernateUtil.close();
    }

    // -----------------------------------------------------------------------
    // Helper types
    // -----------------------------------------------------------------------

    /**
     * {@link HostAPIImpl} subclass that overrides {@link HostAPIImpl#flushAllCaches} with a no-op
     * so that pure unit tests do not need a running cache infrastructure.
     *
     * <p>The real {@code archive()} and {@code unarchive()} implementations are left intact, so
     * tests using this class exercise the actual non-cascade behaviour.
     */
    static class NoopFlushHostAPIImpl extends HostAPIImpl {
        NoopFlushHostAPIImpl(final SystemEventsAPI sysEvents) {
            super(sysEvents);
        }

        /** Cache flush is a no-op in unit tests — no container available. */
        @Override
        void flushAllCaches(final Host site) {
            // intentional no-op
        }
    }

    /**
     * {@link HostAPIImpl} subclass that overrides {@code archive()} and {@code unarchive()} so
     * that the internal calls made by {@code cascadeArchive()} / {@code cascadeUnarchive()} can
     * be intercepted without requiring the full ContentletAPI + HibernateUtil mock chain.  It
     * also overrides {@link HostAPIImpl#flushAllCaches} so no cache infrastructure is required.
     *
     * <p>Each call to {@code archive(host)} or {@code unarchive(host)} appends the targeted
     * host to the respective list for later assertion.
     */
    static class TrackingHostAPIImpl extends HostAPIImpl {

        final List<Host> archiveCalls   = new ArrayList<>();
        final List<Host> unarchiveCalls = new ArrayList<>();

        TrackingHostAPIImpl(final SystemEventsAPI sysEvents) {
            super(sysEvents);
        }

        @Override
        public void archive(final Host host, final User user, final boolean respectFrontendRoles)
                throws DotDataException, DotSecurityException, DotContentletStateException {
            archiveCalls.add(host);
        }

        @Override
        public void unarchive(final Host host, final User user, final boolean respectFrontendRoles)
                throws DotDataException, DotSecurityException, DotContentletStateException {
            unarchiveCalls.add(host);
        }

        @Override
        void flushAllCaches(final Host site) {
            // intentional no-op
        }
    }

    /**
     * Creates a minimal {@link Host} backed by a {@link Contentlet} to avoid the
     * {@link Host#Host()} no-arg constructor which requires the dotCMS container.
     */
    private static Host createHost(final String identifier, final String inode,
                                   final String hostname) {
        final Contentlet c = new Contentlet();
        c.setIdentifier(identifier);
        c.setInode(inode);
        final Host host = new Host(c);
        host.setHostname(hostname);
        return host;
    }

    // -----------------------------------------------------------------------
    // unarchive() — non-cascade: ContentletAPI called exactly once
    // -----------------------------------------------------------------------

    /**
     * {@link HostAPIImpl#unarchive} must call {@link ContentletAPI#unarchive} exactly once — for
     * the targeted host — regardless of how many descendant hosts exist.
     *
     * <p>This is the core behavioural contract of Sub-AC 13b: a manual unarchive is
     * single-target only.
     */
    @Test
    void unarchive_callsContentletAPIUnarchiveExactlyOnce()
            throws DotDataException, DotSecurityException {

        final String siteInode = "inode-parent-001";
        final Host   site      = createHost("site-parent-uuid", siteInode, "parent.example.com");

        final Contentlet siteContentlet = new Contentlet();
        siteContentlet.setInode(siteInode);
        when(mockContentletAPI.find(eq(siteInode), any(User.class), anyBoolean()))
                .thenReturn(siteContentlet);
        doNothing().when(mockContentletAPI)
                .unarchive(any(Contentlet.class), any(User.class), anyBoolean());

        final NoopFlushHostAPIImpl hostAPI =
                new NoopFlushHostAPIImpl(mock(SystemEventsAPI.class));

        assertDoesNotThrow(() -> hostAPI.unarchive(site, mockUser, false),
                "unarchive() must not throw for a valid archived host");

        // ContentletAPI.unarchive() called exactly once — for the target only.
        verify(mockContentletAPI, times(1))
                .unarchive(any(Contentlet.class), any(User.class), anyBoolean());
    }

    // -----------------------------------------------------------------------
    // unarchive() — non-cascade: factory never queried for descendants
    // -----------------------------------------------------------------------

    /**
     * {@link HostAPIImpl#unarchive} must <em>never</em> call
     * {@link HostFactory#findAllDescendantHostIds}.  That method is only used by
     * {@link HostAPIImpl#cascadeUnarchive}; the non-cascading variant must not inspect the
     * descendant set at all.
     */
    @Test
    void unarchive_neverQueriesDescendantHostIds()
            throws DotDataException, DotSecurityException {

        final String siteInode = "inode-parent-002";
        final Host   site      = createHost("site-parent-uuid-2", siteInode, "site2.example.com");

        final Contentlet siteContentlet = new Contentlet();
        siteContentlet.setInode(siteInode);
        when(mockContentletAPI.find(eq(siteInode), any(User.class), anyBoolean()))
                .thenReturn(siteContentlet);
        doNothing().when(mockContentletAPI)
                .unarchive(any(Contentlet.class), any(User.class), anyBoolean());

        final NoopFlushHostAPIImpl hostAPI =
                new NoopFlushHostAPIImpl(mock(SystemEventsAPI.class));

        assertDoesNotThrow(() -> hostAPI.unarchive(site, mockUser, false));

        // The factory method for descendant traversal must NEVER be invoked.
        verify(mockHostFactory, never()).findAllDescendantHostIds(anyString());
    }

    /**
     * Descendants must not be looked up: even when descendant IDs are present in the factory,
     * {@link ContentletAPI#find} is invoked exactly once — for the root site's inode only.
     *
     * <p>This verifies that the non-cascade {@code unarchive()} makes no attempt to discover or
     * restore child hosts.
     */
    @Test
    void unarchive_doesNotLookUpDescendantsViaContentletAPI()
            throws DotDataException, DotSecurityException {

        final String parentInode = "inode-parent-003";
        final String childInode  = "inode-child-001";
        final Host   parent      = createHost("parent-uuid-3", parentInode, "parent3.example.com");

        final Contentlet parentContentlet = new Contentlet();
        parentContentlet.setInode(parentInode);
        when(mockContentletAPI.find(eq(parentInode), any(User.class), anyBoolean()))
                .thenReturn(parentContentlet);
        doNothing().when(mockContentletAPI)
                .unarchive(any(Contentlet.class), any(User.class), anyBoolean());

        final NoopFlushHostAPIImpl hostAPI =
                new NoopFlushHostAPIImpl(mock(SystemEventsAPI.class));

        assertDoesNotThrow(() -> hostAPI.unarchive(parent, mockUser, false));

        // find() must be called for the parent inode only — never for any child inode.
        verify(mockContentletAPI, times(1))
                .find(eq(parentInode), any(User.class), anyBoolean());
        verify(mockContentletAPI, never())
                .find(eq(childInode), any(User.class), anyBoolean());
    }

    // -----------------------------------------------------------------------
    // unarchive() via TrackingHostAPIImpl: single-target tracking
    // -----------------------------------------------------------------------

    /**
     * Using the tracking subclass, a direct call to {@code unarchive(parent)} records exactly one
     * unarchive invocation.  Descendants are never touched.
     *
     * <p>This models the scenario where a parent host was archived (potentially via
     * {@code cascadeArchive}) and the operator manually restores only the parent, leaving child
     * sites in their archived state.
     */
    @Test
    void unarchive_viaTracking_recordsExactlyOneInvocation()
            throws DotDataException, DotSecurityException, DotContentletStateException {

        final Host parent = createHost("parent-track-uuid", "inode-track", "track.example.com");

        final TrackingHostAPIImpl trackingAPI =
                new TrackingHostAPIImpl(mock(SystemEventsAPI.class));

        // Call the overridden (tracking) unarchive directly
        trackingAPI.unarchive(parent, mockUser, false);

        assertEquals(1, trackingAPI.unarchiveCalls.size(),
                "unarchive() must record exactly one call");
        assertTrue(trackingAPI.unarchiveCalls.contains(parent),
                "The recorded call must be for the targeted host");
        assertEquals(0, trackingAPI.archiveCalls.size(),
                "unarchive() must not trigger any archive calls");
    }

    /**
     * Even when child hosts exist in the factory, a direct call to {@code unarchive(parent)} via
     * the tracking API records only the parent in the unarchive list — demonstrating that
     * descendants are left in their current (archived) state.
     */
    @Test
    void unarchive_viaTracking_descendantsRemainUntouched()
            throws DotDataException, DotSecurityException, DotContentletStateException {

        final Host parent = createHost("parent-id", "inode-p", "parent.example.com");
        final Host child1 = createHost("child1-id", "inode-c1", "child1.example.com");
        final Host child2 = createHost("child2-id", "inode-c2", "child2.example.com");

        final TrackingHostAPIImpl trackingAPI =
                new TrackingHostAPIImpl(mock(SystemEventsAPI.class));

        // Only unarchive the parent — children are ignored
        trackingAPI.unarchive(parent, mockUser, false);

        assertEquals(1, trackingAPI.unarchiveCalls.size(),
                "Direct unarchive() must affect only the targeted host, not descendants");
        assertTrue(trackingAPI.unarchiveCalls.contains(parent));
        // child1 and child2 are NOT in the list — they were left in archived state
        assertTrue(trackingAPI.unarchiveCalls.stream()
                .noneMatch(h -> "child1-id".equals(h.getIdentifier())
                        || "child2-id".equals(h.getIdentifier())),
                "Descendants must NOT be unarchived by a non-cascade unarchive call");
    }

    // -----------------------------------------------------------------------
    // cascadeUnarchive() — contrast: descendant query IS performed
    // -----------------------------------------------------------------------

    /**
     * Contrast test: {@link HostAPIImpl#cascadeUnarchive} <em>must</em> call
     * {@link HostFactory#findAllDescendantHostIds} to discover the descendants it needs to
     * unarchive.  This confirms that the behaviour difference between {@code unarchive} and
     * {@code cascadeUnarchive} is intentional and verified.
     *
     * <p>When the descendant list is empty the cascade degrades to a single-host unarchive,
     * but the lookup still occurs — which is the key distinction from the plain
     * {@code unarchive()} path.
     */
    @Test
    void cascadeUnarchive_doesQueryDescendantHostIds()
            throws DotDataException, DotSecurityException, DotContentletStateException {

        final Host site = createHost("cascade-uuid", "inode-cascade", "cascade.example.com");

        // Return an empty descendant list so the loop body is a no-op.
        when(mockHostFactory.findAllDescendantHostIds(site.getIdentifier()))
                .thenReturn(List.of());

        final TrackingHostAPIImpl trackingAPI =
                new TrackingHostAPIImpl(mock(SystemEventsAPI.class));

        assertDoesNotThrow(() -> trackingAPI.cascadeUnarchive(site, mockUser, false));

        // cascadeUnarchive MUST query the descendant set.
        verify(mockHostFactory, times(1))
                .findAllDescendantHostIds(site.getIdentifier());

        // Root is always unarchived at the end of the cascade.
        assertEquals(1, trackingAPI.unarchiveCalls.size(),
                "With no descendants, cascadeUnarchive must unarchive exactly the root");
        assertTrue(trackingAPI.unarchiveCalls.contains(site));
    }

    /**
     * When {@code cascadeUnarchive()} finds descendant hosts it calls {@code unarchive()} for
     * each archived descendant AND the root.
     *
     * <p>This test sets up two archived descendants (via mocked {@link HostFactory#DBSearch}) and
     * verifies that the tracking list contains three entries (two descendants + root), reinforcing
     * that {@code cascadeUnarchive()} differs from the single-target {@code unarchive()}.
     */
    @Test
    void cascadeUnarchive_withArchivedDescendants_unarchivesEachDescendantPlusRoot()
            throws DotDataException, DotSecurityException, DotContentletStateException {

        final String rootId    = "root-uuid";
        final String child1Id  = "child1-uuid";
        final String child2Id  = "child2-uuid";
        final Host   root      = createHost(rootId,   "inode-root",   "root.example.com");
        final Host   child1    = createHost(child1Id, "inode-child1", "child1.example.com");
        final Host   child2    = createHost(child2Id, "inode-child2", "child2.example.com");

        // Mark children as archived
        child1.setBoolProperty(Contentlet.ARCHIVED_KEY, true);
        child2.setBoolProperty(Contentlet.ARCHIVED_KEY, true);

        when(mockHostFactory.findAllDescendantHostIds(rootId))
                .thenReturn(List.of(child1Id, child2Id));
        when(mockHostFactory.DBSearch(eq(child1Id), anyBoolean())).thenReturn(child1);
        when(mockHostFactory.DBSearch(eq(child2Id), anyBoolean())).thenReturn(child2);

        final TrackingHostAPIImpl trackingAPI =
                new TrackingHostAPIImpl(mock(SystemEventsAPI.class));

        assertDoesNotThrow(() -> trackingAPI.cascadeUnarchive(root, mockUser, false));

        // All three hosts — two children + root — must appear in the unarchive list.
        assertEquals(3, trackingAPI.unarchiveCalls.size(),
                "cascadeUnarchive must unarchive all descendants AND the root");
        assertTrue(trackingAPI.unarchiveCalls.contains(child1),
                "First child must have been unarchived");
        assertTrue(trackingAPI.unarchiveCalls.contains(child2),
                "Second child must have been unarchived");
        assertTrue(trackingAPI.unarchiveCalls.contains(root),
                "Root must have been unarchived last");
    }
}
