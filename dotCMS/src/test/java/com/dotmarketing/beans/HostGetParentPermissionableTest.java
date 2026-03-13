package com.dotmarketing.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Pure unit tests for {@link Host#getParentPermissionable()}.
 *
 * <p>These tests do not require a running dotCMS instance.  All API dependencies are mocked via
 * Mockito's {@code mockStatic} facility.  Host instances are created using the
 * {@link Host#Host(Contentlet)} constructor to bypass the {@code CacheLocator} dependency in the
 * no-arg constructor.</p>
 *
 * <p>Verifies AC 4: nested hosts return the parent host (not the System Host) from
 * {@link Host#getParentPermissionable()}, enabling permission inheritance through the host
 * hierarchy.</p>
 */
class HostGetParentPermissionableTest {

    private MockedStatic<APILocator> mockedAPILocator;
    private IdentifierAPI identifierAPI;
    private HostAPI hostAPI;
    private User systemUser;

    @BeforeEach
    void setUp() {
        identifierAPI = mock(IdentifierAPI.class);
        hostAPI       = mock(HostAPI.class);
        systemUser    = mock(User.class);

        mockedAPILocator = mockStatic(APILocator.class);
        mockedAPILocator.when(APILocator::getIdentifierAPI).thenReturn(identifierAPI);
        mockedAPILocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
        mockedAPILocator.when(APILocator::systemUser).thenReturn(systemUser);
    }

    @AfterEach
    void tearDown() {
        mockedAPILocator.close();
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    /**
     * Creates a {@link Host} using the copy constructor so that the no-arg constructor's
     * {@code CacheLocator} dependency is avoided.
     */
    private static Host createHost(final String identifier, final String hostname) {
        final Contentlet contentlet = new Contentlet();
        final Host host = new Host(contentlet);
        if (identifier != null) {
            host.setIdentifier(identifier);
        }
        host.setHostname(hostname);
        return host;
    }

    /** Creates a minimal {@link Identifier} populated with the supplied values. */
    private static Identifier makeIdentifier(final String id,
                                             final String hostId,
                                             final String parentPath,
                                             final String assetName) {
        final Identifier ident = new Identifier(id);
        ident.setHostId(hostId);
        ident.setParentPath(parentPath);
        ident.setAssetName(assetName);
        return ident;
    }

    // -----------------------------------------------------------------------
    // Tests: System Host
    // -----------------------------------------------------------------------

    /**
     * The System Host must return {@code null} – it sits at the root of the permission hierarchy
     * and has no parent.
     */
    @Test
    void systemHost_returnsNull() throws Exception {
        final Host sysHost = createHost(Host.SYSTEM_HOST, Host.SYSTEM_HOST_SITENAME);
        sysHost.setSystemHost(true);

        assertNull(sysHost.getParentPermissionable(),
                "System Host must return null from getParentPermissionable()");
    }

    // -----------------------------------------------------------------------
    // Tests: top-level host (identifier.hostId == SYSTEM_HOST)
    // -----------------------------------------------------------------------

    /**
     * A top-level host (one whose {@code Identifier.hostId} equals {@code "SYSTEM_HOST"}) must
     * return the System Host as its parent permissionable.  This preserves existing / legacy
     * behaviour.
     */
    @Test
    void topLevelHost_withSystemHostParent_returnsSystemHost() throws Exception {
        final String topId = "top-level-uuid";
        final Host topHost = createHost(topId, "dotcms.com");
        final Identifier ident = makeIdentifier(topId, Host.SYSTEM_HOST, "/", "dotcms.com");

        final Host sysHost = createHost(Host.SYSTEM_HOST, Host.SYSTEM_HOST_SITENAME);
        sysHost.setSystemHost(true);

        when(identifierAPI.find(topId)).thenReturn(ident);
        when(hostAPI.findSystemHost()).thenReturn(sysHost);

        final Permissionable parent = topHost.getParentPermissionable();

        assertNotNull(parent, "Top-level host must have a non-null parent permissionable");
        assertInstanceOf(Host.class, parent,
                "Parent permissionable of top-level host must be a Host instance");
        assertEquals(Host.SYSTEM_HOST, ((Host) parent).getIdentifier(),
                "Top-level host's parent permissionable must be the System Host");
    }

    /**
     * When the host has no identifier (e.g. not yet persisted) the method must fall through to
     * returning the System Host rather than throwing an exception.
     */
    @Test
    void noIdentifier_fallsThroughToSystemHost() throws Exception {
        final Host unpersisted = createHost(null, "example.com");

        final Host sysHost = createHost(Host.SYSTEM_HOST, Host.SYSTEM_HOST_SITENAME);
        sysHost.setSystemHost(true);
        when(hostAPI.findSystemHost()).thenReturn(sysHost);

        final Permissionable parent = unpersisted.getParentPermissionable();

        assertNotNull(parent, "Unpersisted host must still return a non-null parent permissionable");
        assertEquals(Host.SYSTEM_HOST, ((Host) parent).getIdentifier(),
                "Unpersisted host parent must be the System Host");
    }

    /**
     * When the {@link Identifier} for a host cannot be resolved (returns null) the method must
     * still return the System Host as a safe fallback.
     */
    @Test
    void nullIdentifier_fallsThroughToSystemHost() throws Exception {
        final String hostId = "some-uuid";
        final Host host = createHost(hostId, "example.com");

        when(identifierAPI.find(hostId)).thenReturn(null);

        final Host sysHost = createHost(Host.SYSTEM_HOST, Host.SYSTEM_HOST_SITENAME);
        sysHost.setSystemHost(true);
        when(hostAPI.findSystemHost()).thenReturn(sysHost);

        final Permissionable parent = host.getParentPermissionable();

        assertNotNull(parent, "Host with unresolvable identifier must fall back to System Host");
        assertEquals(Host.SYSTEM_HOST, ((Host) parent).getIdentifier(),
                "Fallback parent must be the System Host");
    }

    /**
     * When the {@link Identifier#getHostId()} is null the method must fall through to the System
     * Host (same as a top-level host whose parentId has not been set).
     */
    @Test
    void nullParentId_inIdentifier_fallsThroughToSystemHost() throws Exception {
        final String hostId = "uuid-null-parent";
        final Host host = createHost(hostId, "example.com");
        final Identifier ident = makeIdentifier(hostId, null, "/", "example.com");

        when(identifierAPI.find(hostId)).thenReturn(ident);

        final Host sysHost = createHost(Host.SYSTEM_HOST, Host.SYSTEM_HOST_SITENAME);
        sysHost.setSystemHost(true);
        when(hostAPI.findSystemHost()).thenReturn(sysHost);

        final Permissionable parent = host.getParentPermissionable();

        assertNotNull(parent, "Host with null parentId must fall back to System Host");
        assertEquals(Host.SYSTEM_HOST, ((Host) parent).getIdentifier(),
                "Fallback parent must be the System Host");
    }

    // -----------------------------------------------------------------------
    // Tests: nested host (identifier.hostId != SYSTEM_HOST)  – AC 4
    // -----------------------------------------------------------------------

    /**
     * <b>AC 4 – core assertion:</b> When a host is nested under another host (i.e. its
     * {@code Identifier.hostId} points to a non-system host UUID), {@code getParentPermissionable}
     * must return that parent host – <em>not</em> the System Host.
     *
     * <p>This ensures permission inheritance propagates through the nestable host hierarchy rather
     * than skipping intermediate hosts and landing directly on the System Host.</p>
     */
    @Test
    void nestedHost_returnsParentHost_notSystemHost() throws Exception {
        final String parentId  = "parent-host-uuid";
        final String childId   = "child-host-uuid";

        final Host parentHost = createHost(parentId, "parent.example.com");
        final Host childHost  = createHost(childId,  "child-segment");

        // Child host's identifier points to the parent host (not SYSTEM_HOST).
        final Identifier childIdent = makeIdentifier(childId, parentId, "/", "child-segment");

        when(identifierAPI.find(childId)).thenReturn(childIdent);
        when(hostAPI.find(parentId, systemUser, false)).thenReturn(parentHost);

        final Permissionable parent = childHost.getParentPermissionable();

        assertNotNull(parent, "Nested host must have a non-null parent permissionable");
        assertInstanceOf(Host.class, parent,
                "Parent permissionable of nested host must be a Host instance");
        assertEquals(parentId, ((Host) parent).getIdentifier(),
                "Nested host must return the parent host (not the System Host) as its parent "
                        + "permissionable so that permissions are inherited through the hierarchy");
    }

    /**
     * A deeply nested host (grandchild) must return its <em>direct</em> parent (the child host),
     * not the grandparent or the System Host.  The recursive permission chain walk
     * ({@code PermissionBitAPIImpl}) calls {@code getParentPermissionable()} repeatedly, so each
     * call need only return one level up.
     */
    @Test
    void deeplyNestedHost_returnsDirectParentOnly() throws Exception {
        final String grandparentId = "grandparent-uuid";
        final String parentId      = "parent-uuid";
        final String childId       = "child-uuid";

        final Host parentHost = createHost(parentId, "parent-segment");
        final Host childHost  = createHost(childId,  "child-segment");

        // Child's identifier.hostId = parentId  (one level up, not grandparent).
        final Identifier childIdent = makeIdentifier(childId, parentId, "/", "child-segment");

        when(identifierAPI.find(childId)).thenReturn(childIdent);
        when(hostAPI.find(parentId, systemUser, false)).thenReturn(parentHost);

        final Permissionable directParent = childHost.getParentPermissionable();

        assertNotNull(directParent, "Deeply nested host must return its direct parent");
        assertEquals(parentId, ((Host) directParent).getIdentifier(),
                "getParentPermissionable must return the direct parent (one level up) only");
    }

    /**
     * When the parent host cannot be resolved (e.g. orphaned identifier), the method must fall
     * back to the System Host rather than returning null or throwing an unchecked exception.
     */
    @Test
    void nestedHost_unresolvedParent_fallsThroughToSystemHost() throws Exception {
        final String parentId = "missing-parent-uuid";
        final String childId  = "orphan-child-uuid";

        final Host childHost = createHost(childId, "orphan-segment");

        // Identifier points to a parent that cannot be resolved (find returns null).
        final Identifier childIdent = makeIdentifier(childId, parentId, "/", "orphan-segment");
        when(identifierAPI.find(childId)).thenReturn(childIdent);
        when(hostAPI.find(parentId, systemUser, false)).thenReturn(null);

        final Host sysHost = createHost(Host.SYSTEM_HOST, Host.SYSTEM_HOST_SITENAME);
        sysHost.setSystemHost(true);
        when(hostAPI.findSystemHost()).thenReturn(sysHost);

        final Permissionable parent = childHost.getParentPermissionable();

        assertNotNull(parent,
                "Orphaned nested host must fall back to System Host rather than returning null");
        assertEquals(Host.SYSTEM_HOST, ((Host) parent).getIdentifier(),
                "Orphaned nested host fallback must be the System Host");
    }

    /**
     * When the parent host is found but has no identifier (i.e. it is a blank/empty Host shell),
     * the method must fall through to the System Host.
     */
    @Test
    void nestedHost_parentWithBlankIdentifier_fallsThroughToSystemHost() throws Exception {
        final String parentId = "parent-blank-id-uuid";
        final String childId  = "child-uuid-blank";

        final Host childHost = createHost(childId, "child-segment");

        // Parent host resolved but has no identifier (blank shell).
        final Host blankParent = createHost(null, "partial-parent");
        final Identifier childIdent = makeIdentifier(childId, parentId, "/", "child-segment");

        when(identifierAPI.find(childId)).thenReturn(childIdent);
        when(hostAPI.find(parentId, systemUser, false)).thenReturn(blankParent);

        final Host sysHost = createHost(Host.SYSTEM_HOST, Host.SYSTEM_HOST_SITENAME);
        sysHost.setSystemHost(true);
        when(hostAPI.findSystemHost()).thenReturn(sysHost);

        final Permissionable parent = childHost.getParentPermissionable();

        assertNotNull(parent,
                "Host with blank parent identifier must fall back to System Host");
        assertEquals(Host.SYSTEM_HOST, ((Host) parent).getIdentifier(),
                "Fallback must be the System Host when parent's identifier is blank");
    }
}
