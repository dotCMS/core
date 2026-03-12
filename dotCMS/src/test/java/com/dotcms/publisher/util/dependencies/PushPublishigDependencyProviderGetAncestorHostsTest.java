package com.dotcms.publisher.util.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Pure unit tests for
 * {@link PushPublishigDependencyProvider#getAncestorHosts(Host)}.
 *
 * <p>All API dependencies are mocked via Mockito's {@code mockStatic} facility so that no running
 * dotCMS instance is required.</p>
 *
 * <p>Covers AC 14 (Sub-AC 1): push-publish dependency resolution must auto-include all ancestor
 * hosts when a nested host is selected for push publish, ensuring the full host hierarchy is
 * bundled.</p>
 */
class PushPublishigDependencyProviderGetAncestorHostsTest {

    private MockedStatic<APILocator> mockedAPILocator;
    private IdentifierAPI identifierAPI;
    private HostAPI hostAPI;
    private User testUser;
    private PushPublishigDependencyProvider provider;

    @BeforeEach
    void setUp() {
        identifierAPI = mock(IdentifierAPI.class);
        hostAPI       = mock(HostAPI.class);
        testUser      = mock(User.class);

        mockedAPILocator = mockStatic(APILocator.class);
        mockedAPILocator.when(APILocator::getIdentifierAPI).thenReturn(identifierAPI);
        mockedAPILocator.when(APILocator::getHostAPI).thenReturn(hostAPI);

        provider = new PushPublishigDependencyProvider(testUser);
    }

    @AfterEach
    void tearDown() {
        mockedAPILocator.close();
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    private static Host makeHost(final String identifier, final String hostname) {
        final Contentlet c = new Contentlet();
        final Host host = new Host(c);
        host.setIdentifier(identifier);
        host.setHostname(hostname);
        return host;
    }

    private static Identifier makeIdentifier(final String id, final String hostId) {
        final Identifier ident = new Identifier(id);
        ident.setHostId(hostId);
        return ident;
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    /**
     * A top-level host (whose Identifier.hostId == SYSTEM_HOST) has no ancestors.
     */
    @Test
    void topLevelHost_returnsEmptyList() throws DotDataException, DotSecurityException {
        final Host site = makeHost("site-1", "example.com");
        when(identifierAPI.find("site-1"))
                .thenReturn(makeIdentifier("site-1", Host.SYSTEM_HOST));

        final List<Host> ancestors = provider.getAncestorHosts(site);

        assertTrue(ancestors.isEmpty(),
                "A top-level host should have no ancestors");
    }

    /**
     * A singly-nested host (child → parent → System Host) returns exactly the parent host.
     */
    @Test
    void singlyNestedHost_returnsImmediateParent() throws DotDataException, DotSecurityException {
        final Host parent = makeHost("parent-id", "parent.com");
        final Host child  = makeHost("child-id",  "child.parent.com");

        when(identifierAPI.find("child-id"))
                .thenReturn(makeIdentifier("child-id", "parent-id"));
        when(identifierAPI.find("parent-id"))
                .thenReturn(makeIdentifier("parent-id", Host.SYSTEM_HOST));
        when(hostAPI.find(eq("parent-id"), any(User.class), anyBoolean()))
                .thenReturn(parent);

        final List<Host> ancestors = provider.getAncestorHosts(child);

        assertEquals(1, ancestors.size(), "One ancestor expected");
        assertEquals("parent-id", ancestors.get(0).getIdentifier());
    }

    /**
     * A doubly-nested host (grandchild → child → parent → System Host) returns both the
     * intermediate host and the top-level host, in nearest-first order.
     */
    @Test
    void doublyNestedHost_returnsBothAncestorsNearestFirst()
            throws DotDataException, DotSecurityException {

        final Host grandparent = makeHost("gp-id",    "gp.com");
        final Host parent      = makeHost("parent-id", "child.gp.com");
        final Host grandchild  = makeHost("gc-id",    "gc.child.gp.com");

        when(identifierAPI.find("gc-id"))
                .thenReturn(makeIdentifier("gc-id", "parent-id"));
        when(identifierAPI.find("parent-id"))
                .thenReturn(makeIdentifier("parent-id", "gp-id"));
        when(identifierAPI.find("gp-id"))
                .thenReturn(makeIdentifier("gp-id", Host.SYSTEM_HOST));

        when(hostAPI.find(eq("parent-id"), any(User.class), anyBoolean())).thenReturn(parent);
        when(hostAPI.find(eq("gp-id"),     any(User.class), anyBoolean())).thenReturn(grandparent);

        final List<Host> ancestors = provider.getAncestorHosts(grandchild);

        assertEquals(2, ancestors.size(), "Two ancestors expected");
        assertEquals("parent-id", ancestors.get(0).getIdentifier(), "Nearest ancestor first");
        assertEquals("gp-id",     ancestors.get(1).getIdentifier(), "Top-level ancestor second");
    }

    /**
     * When the Identifier record is null the method returns an empty list gracefully.
     */
    @Test
    void nullIdentifier_returnsEmptyList() throws DotDataException, DotSecurityException {
        final Host site = makeHost("orphan-id", "orphan.com");
        when(identifierAPI.find("orphan-id")).thenReturn(null);

        final List<Host> ancestors = provider.getAncestorHosts(site);

        assertTrue(ancestors.isEmpty(),
                "Null identifier should yield an empty ancestor list");
    }

    /**
     * When the host itself is null the method returns an empty list without throwing.
     */
    @Test
    void nullSite_returnsEmptyList() throws DotDataException, DotSecurityException {
        final List<Host> ancestors = provider.getAncestorHosts(null);
        assertTrue(ancestors.isEmpty(), "Null site should yield an empty ancestor list");
    }

    /**
     * If the parent host object cannot be resolved (API returns null) the traversal stops
     * gracefully and returns whatever ancestors were already collected.
     */
    @Test
    void danglingParentReference_stopsGracefully()
            throws DotDataException, DotSecurityException {

        final Host child = makeHost("child-id", "child.example.com");
        when(identifierAPI.find("child-id"))
                .thenReturn(makeIdentifier("child-id", "missing-parent-id"));
        when(hostAPI.find(eq("missing-parent-id"), any(User.class), anyBoolean()))
                .thenReturn(null);

        final List<Host> ancestors = provider.getAncestorHosts(child);

        assertTrue(ancestors.isEmpty(),
                "Dangling parent reference should stop traversal and return empty list");
    }

    /**
     * Detects an artificial circular reference (A → B → A) without infinite-looping:
     * the cycle-detection guard breaks the traversal and the returned list contains only the
     * directly-reachable ancestors before the cycle was detected.
     */
    @Test
    void circularReference_stopsWithCycleGuard()
            throws DotDataException, DotSecurityException {

        final Host siteA = makeHost("site-a", "a.com");
        final Host siteB = makeHost("site-b", "b.a.com");

        // Artificially create a cycle: A → B → A
        when(identifierAPI.find("site-a"))
                .thenReturn(makeIdentifier("site-a", "site-b"));
        when(identifierAPI.find("site-b"))
                .thenReturn(makeIdentifier("site-b", "site-a"));   // cycle!
        when(hostAPI.find(eq("site-b"), any(User.class), anyBoolean())).thenReturn(siteB);
        when(hostAPI.find(eq("site-a"), any(User.class), anyBoolean())).thenReturn(siteA);

        // Must not throw or block; returns up to the first duplicated node
        final List<Host> ancestors = provider.getAncestorHosts(siteA);

        // site-a started the traversal; site-b is added; then we try site-a again → cycle → stop
        assertEquals(1, ancestors.size(),
                "Cycle guard should stop after one ancestor and not loop");
        assertEquals("site-b", ancestors.get(0).getIdentifier());
    }
}
