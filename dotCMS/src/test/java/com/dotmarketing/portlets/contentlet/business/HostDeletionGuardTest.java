package com.dotmarketing.portlets.contentlet.business;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.Optional;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Pure unit tests for the deletion guard in {@link HostAPIImpl#delete}.
 *
 * <p>Verifies Sub-AC 12a: before executing a host delete, the descendant count (child hosts and
 * folders) is queried; if the count of descendant hosts is > 0 the operation is aborted and a
 * {@link HostHasDescendantsException} is thrown so that the REST layer can return HTTP 409.</p>
 *
 * <p>All external dependencies ({@link FactoryLocator}, {@link APILocator}) are mocked via
 * Mockito's {@code mockStatic} facility.  No database or running dotCMS instance is required.</p>
 */
class HostDeletionGuardTest {

    private MockedStatic<FactoryLocator> mockedFactoryLocator;
    private MockedStatic<APILocator>     mockedAPILocator;

    private HostFactory    mockHostFactory;
    private PermissionAPI  mockPermissionAPI;
    private User           mockUser;
    private HostAPIImpl    hostAPI;

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

        // Use the @VisibleForTesting constructor (package-private, same package)
        hostAPI = new HostAPIImpl(mock(SystemEventsAPI.class));
    }

    @AfterEach
    void tearDown() {
        mockedFactoryLocator.close();
        mockedAPILocator.close();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Creates a {@link Host} via the copy-constructor to avoid the no-arg constructor's
     * {@code CacheLocator} dependency.
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

    // -----------------------------------------------------------------------
    // HostHasDescendantsException — property & message tests
    // -----------------------------------------------------------------------

    /**
     * The exception must store siteId, descendantHostCount, and descendantFolderCount accurately.
     */
    @Test
    void exception_storesAllFields() {
        final HostHasDescendantsException ex =
                new HostHasDescendantsException("site-id-123", 3L, 7L);

        assertEquals("site-id-123", ex.getSiteId());
        assertEquals(3L, ex.getDescendantHostCount());
        assertEquals(7L, ex.getDescendantFolderCount());
        assertEquals(10L, ex.getTotalDescendantCount(),
                "totalDescendantCount must equal hostCount + folderCount");
    }

    /**
     * The exception message must include the site identifier so operators can identify
     * which site is blocked.
     */
    @Test
    void exception_messageContainsSiteId() {
        final HostHasDescendantsException ex =
                new HostHasDescendantsException("my-special-site", 1L, 0L);

        assertTrue(ex.getMessage().contains("my-special-site"),
                "Exception message must contain the siteId");
    }

    /**
     * The exception message must include the descendant host count so the caller can report it
     * in the HTTP 409 body.
     */
    @Test
    void exception_messageContainsDescendantHostCount() {
        final HostHasDescendantsException ex =
                new HostHasDescendantsException("any-site", 5L, 2L);

        assertTrue(ex.getMessage().contains("5"),
                "Exception message must contain the descendant host count");
    }

    /**
     * Zero folder count is a valid state (child hosts exist but have no folders).
     */
    @Test
    void exception_zeroFolderCount_totalEqualsHostCount() {
        final HostHasDescendantsException ex =
                new HostHasDescendantsException("site-no-folders", 4L, 0L);

        assertEquals(4L, ex.getTotalDescendantCount());
    }

    // -----------------------------------------------------------------------
    // delete() — descendant guard: no descendants → deletion proceeds
    // -----------------------------------------------------------------------

    /**
     * When there are no descendant hosts, {@link HostAPIImpl#delete} must delegate to the
     * factory and return its {@code Optional<Future<Boolean>>} result.
     */
    @Test
    @SuppressWarnings("unchecked")
    void delete_withNoDescendants_delegatesToFactory() throws Exception {
        final String siteId = "site-no-children-uuid";
        final Host   site   = createHost(siteId, "no-children.example.com");

        when(mockHostFactory.countDescendantHosts(siteId)).thenReturn(0L);
        when(mockHostFactory.countDescendantFolders(siteId)).thenReturn(0L);

        final Future<Boolean> futureMock = mock(Future.class);
        when(mockHostFactory.delete(eq(site), any(User.class), eq(false), eq(true)))
                .thenReturn(Optional.of(futureMock));

        final Optional<Future<Boolean>> result = hostAPI.delete(site, mockUser, false, true);

        assertTrue(result.isPresent(), "delete() must return a non-empty Optional when no descendants exist");
        verify(mockHostFactory).delete(eq(site), any(User.class), eq(false), eq(true));
    }

    /**
     * When both descendant host count and folder count are zero, deletion must not be blocked.
     */
    @Test
    @SuppressWarnings("unchecked")
    void delete_withZeroDescendantHostsAndZeroFolders_isNotBlocked() throws Exception {
        final String siteId = "clean-site-uuid";
        final Host   site   = createHost(siteId, "clean.example.com");

        when(mockHostFactory.countDescendantHosts(siteId)).thenReturn(0L);
        when(mockHostFactory.countDescendantFolders(siteId)).thenReturn(0L);
        when(mockHostFactory.delete(eq(site), any(User.class), anyBoolean(), anyBoolean()))
                .thenReturn(Optional.of(mock(Future.class)));

        assertDoesNotThrow(() -> hostAPI.delete(site, mockUser, false, false),
                "delete() must not throw when there are no descendant hosts");
    }

    // -----------------------------------------------------------------------
    // delete() — descendant guard: hosts present → deletion blocked
    // -----------------------------------------------------------------------

    /**
     * When descendant hosts exist, {@link HostAPIImpl#delete} must throw
     * {@link HostHasDescendantsException} <em>before</em> invoking the factory delete.
     */
    @Test
    void delete_withDescendantHosts_throwsHostHasDescendantsException() throws Exception {
        final String siteId = "site-with-children-uuid";
        final Host   site   = createHost(siteId, "parent.example.com");

        when(mockHostFactory.countDescendantHosts(siteId)).thenReturn(2L);
        when(mockHostFactory.countDescendantFolders(siteId)).thenReturn(5L);

        final HostHasDescendantsException ex = assertThrows(
                HostHasDescendantsException.class,
                () -> hostAPI.delete(site, mockUser, false, true),
                "delete() must throw HostHasDescendantsException when descendant hosts exist");

        assertEquals(siteId, ex.getSiteId());
        assertEquals(2L, ex.getDescendantHostCount());
        assertEquals(5L, ex.getDescendantFolderCount());
        assertEquals(7L, ex.getTotalDescendantCount());

        // The actual delete must NEVER be called
        verify(mockHostFactory, never()).delete(any(), any(), anyBoolean(), anyBoolean());
    }

    /**
     * Even a single descendant host (count = 1) must block deletion.
     */
    @Test
    void delete_withSingleDescendantHost_isBlocked() throws Exception {
        final String siteId = "site-single-child";
        final Host   site   = createHost(siteId, "parent3.example.com");

        when(mockHostFactory.countDescendantHosts(siteId)).thenReturn(1L);
        when(mockHostFactory.countDescendantFolders(siteId)).thenReturn(0L);

        assertThrows(
                HostHasDescendantsException.class,
                () -> hostAPI.delete(site, mockUser, false, false),
                "Even a single descendant host must block deletion");
        verify(mockHostFactory, never()).delete(any(), any(), anyBoolean(), anyBoolean());
    }

    /**
     * The exception reports exact counts so the REST layer can include them in the 409 body.
     */
    @Test
    void delete_withManyDescendants_exceptionReportsExactCounts() throws Exception {
        final String siteId = "site-many-children";
        final Host   site   = createHost(siteId, "parent2.example.com");

        when(mockHostFactory.countDescendantHosts(siteId)).thenReturn(100L);
        when(mockHostFactory.countDescendantFolders(siteId)).thenReturn(500L);

        final HostHasDescendantsException ex = assertThrows(
                HostHasDescendantsException.class,
                () -> hostAPI.delete(site, mockUser, false, false));

        assertEquals(100L, ex.getDescendantHostCount());
        assertEquals(500L, ex.getDescendantFolderCount());
        assertEquals(600L, ex.getTotalDescendantCount());
    }

    // -----------------------------------------------------------------------
    // delete() — folders-only scenario (no descendant hosts) must NOT block
    // -----------------------------------------------------------------------

    /**
     * Folders belonging to the site itself (not to descendant hosts) must not prevent deletion.
     * The guard only fires on descendant <em>hosts</em>.
     */
    @Test
    @SuppressWarnings("unchecked")
    void delete_withZeroDescendantHostsButPositiveFolders_isNotBlocked() throws Exception {
        final String siteId = "site-only-folders";
        final Host   site   = createHost(siteId, "folders-only.example.com");

        // countDescendantHosts returns 0 → guard condition is false
        when(mockHostFactory.countDescendantHosts(siteId)).thenReturn(0L);
        when(mockHostFactory.countDescendantFolders(siteId)).thenReturn(10L);
        when(mockHostFactory.delete(eq(site), any(User.class), anyBoolean(), anyBoolean()))
                .thenReturn(Optional.of(mock(Future.class)));

        assertDoesNotThrow(
                () -> hostAPI.delete(site, mockUser, false, true),
                "delete() must NOT throw when there are no descendant hosts (folder count alone is irrelevant)");
    }

    // -----------------------------------------------------------------------
    // delete() — factory DotDataException is wrapped in DotRuntimeException
    // -----------------------------------------------------------------------

    /**
     * If the factory throws {@link DotDataException} while counting descendants, the method must
     * wrap it in a {@link DotRuntimeException} so that callers without checked-exception handling
     * still see a well-typed failure.
     */
    @Test
    void delete_factoryThrowsDotDataException_wrappedInDotRuntimeException() throws Exception {
        final String siteId = "site-db-error";
        final Host   site   = createHost(siteId, "db-error.example.com");

        when(mockHostFactory.countDescendantHosts(siteId))
                .thenThrow(new DotDataException("DB unavailable"));

        assertThrows(
                DotRuntimeException.class,
                () -> hostAPI.delete(site, mockUser, false, false),
                "DotDataException from factory must be wrapped in DotRuntimeException");
        verify(mockHostFactory, never()).delete(any(), any(), anyBoolean(), anyBoolean());
    }

    // -----------------------------------------------------------------------
    // countDescendantHosts() — public API delegation
    // -----------------------------------------------------------------------

    /**
     * {@code countDescendantHosts(null)} must return 0 without calling the factory.
     */
    @Test
    void countDescendantHosts_nullSite_returnsZero() throws Exception {
        assertEquals(0L, hostAPI.countDescendantHosts(null));
        verify(mockHostFactory, never()).countDescendantHosts(any());
    }

    /**
     * A site with a blank identifier is treated as "not yet persisted" — return 0.
     */
    @Test
    void countDescendantHosts_blankIdentifier_returnsZero() throws Exception {
        final Host site = createHost("", "blank.example.com");
        assertEquals(0L, hostAPI.countDescendantHosts(site));
        verify(mockHostFactory, never()).countDescendantHosts(any());
    }

    /**
     * For a properly identified site, {@code countDescendantHosts} must delegate to the factory
     * and return its value.
     */
    @Test
    void countDescendantHosts_delegatesToFactory() throws Exception {
        final String siteId = "delegate-uuid";
        final Host   site   = createHost(siteId, "delegate.example.com");

        when(mockHostFactory.countDescendantHosts(siteId)).thenReturn(7L);

        assertEquals(7L, hostAPI.countDescendantHosts(site));
        verify(mockHostFactory).countDescendantHosts(siteId);
    }

    // -----------------------------------------------------------------------
    // countDescendantFolders() — public API delegation
    // -----------------------------------------------------------------------

    /**
     * {@code countDescendantFolders(null)} must return 0 without calling the factory.
     */
    @Test
    void countDescendantFolders_nullSite_returnsZero() throws Exception {
        assertEquals(0L, hostAPI.countDescendantFolders(null));
        verify(mockHostFactory, never()).countDescendantFolders(any());
    }

    /**
     * A site with a blank identifier is treated as not persisted — return 0.
     */
    @Test
    void countDescendantFolders_blankIdentifier_returnsZero() throws Exception {
        final Host site = createHost("", "blank-folder.example.com");
        assertEquals(0L, hostAPI.countDescendantFolders(site));
        verify(mockHostFactory, never()).countDescendantFolders(any());
    }

    /**
     * For a properly identified site, {@code countDescendantFolders} must delegate to the factory.
     */
    @Test
    void countDescendantFolders_delegatesToFactory() throws Exception {
        final String siteId = "folder-count-uuid";
        final Host   site   = createHost(siteId, "foldercount.example.com");

        when(mockHostFactory.countDescendantFolders(siteId)).thenReturn(3L);

        assertEquals(3L, hostAPI.countDescendantFolders(site));
        verify(mockHostFactory).countDescendantFolders(siteId);
    }
}
