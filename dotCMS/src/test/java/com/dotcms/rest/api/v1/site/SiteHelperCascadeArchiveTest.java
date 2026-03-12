package com.dotcms.rest.api.v1.site;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.liferay.portal.model.User;
import org.junit.Test;

/**
 * Unit tests for the {@link SiteHelper#cascadeArchive} and {@link SiteHelper#cascadeUnarchive}
 * delegation methods.
 *
 * <p>These tests verify that:
 * <ul>
 *   <li>{@link SiteHelper#cascadeArchive} calls {@link HostAPI#countDescendantHosts} to capture
 *       the descendant count, then calls {@link HostAPI#cascadeArchive}, and returns the
 *       previously captured count.</li>
 *   <li>{@link SiteHelper#cascadeUnarchive} calls {@link HostAPI#countDescendantHosts} to capture
 *       the descendant count, then calls {@link HostAPI#cascadeUnarchive}, and returns the
 *       previously captured count.</li>
 * </ul>
 */
public class SiteHelperCascadeArchiveTest extends UnitTestBase {

    // -------------------------------------------------------------------------
    // cascadeArchive
    // -------------------------------------------------------------------------

    /**
     * {@link SiteHelper#cascadeArchive} must invoke {@link HostAPI#cascadeArchive} and return
     * the descendant count obtained from {@link HostAPI#countDescendantHosts}.
     */
    @Test
    public void cascadeArchive_delegatesToHostApiAndReturnsCount()
            throws DotDataException, DotSecurityException {
        final HostAPI hostAPI = mock(HostAPI.class);
        final SiteHelper helper = new SiteHelper(hostAPI, mock(HostVariableAPI.class));

        final Host site = mock(Host.class);
        final User user = new User();
        final long expectedCount = 5L;

        when(hostAPI.countDescendantHosts(site)).thenReturn(expectedCount);

        final long actual = helper.cascadeArchive(site, user, false);

        assertEquals("cascadeArchive should return the descendant count", expectedCount, actual);
        verify(hostAPI).countDescendantHosts(site);
        verify(hostAPI).cascadeArchive(eq(site), eq(user), eq(false));
    }

    /**
     * When the site has no descendants, {@link SiteHelper#cascadeArchive} must return 0.
     */
    @Test
    public void cascadeArchive_noDescendants_returnsZero()
            throws DotDataException, DotSecurityException {
        final HostAPI hostAPI = mock(HostAPI.class);
        final SiteHelper helper = new SiteHelper(hostAPI, mock(HostVariableAPI.class));

        final Host site = mock(Host.class);
        final User user = new User();

        when(hostAPI.countDescendantHosts(site)).thenReturn(0L);

        final long actual = helper.cascadeArchive(site, user, false);

        assertEquals("cascadeArchive should return 0 when site has no descendants", 0L, actual);
    }

    /**
     * The descendant count must be captured <em>before</em> the archive call, so even if the
     * underlying implementation changes the count during archiving, the returned value reflects
     * the snapshot taken before the operation.
     */
    @Test
    public void cascadeArchive_capturesCountBeforeOperation()
            throws DotDataException, DotSecurityException {
        final HostAPI hostAPI = mock(HostAPI.class);
        final SiteHelper helper = new SiteHelper(hostAPI, mock(HostVariableAPI.class));

        final Host site = mock(Host.class);
        final User user = new User();

        // Simulate count before archive = 3, with respectFrontendRoles = true
        when(hostAPI.countDescendantHosts(site)).thenReturn(3L);

        final long actual = helper.cascadeArchive(site, user, true);

        assertEquals("Should return pre-archive snapshot count", 3L, actual);
        // Verify ordering: count before cascadeArchive
        final org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(hostAPI);
        inOrder.verify(hostAPI).countDescendantHosts(site);
        inOrder.verify(hostAPI).cascadeArchive(eq(site), eq(user), eq(true));
    }

    // -------------------------------------------------------------------------
    // cascadeUnarchive
    // -------------------------------------------------------------------------

    /**
     * {@link SiteHelper#cascadeUnarchive} must invoke {@link HostAPI#cascadeUnarchive} and return
     * the descendant count obtained from {@link HostAPI#countDescendantHosts}.
     */
    @Test
    public void cascadeUnarchive_delegatesToHostApiAndReturnsCount()
            throws DotDataException, DotSecurityException {
        final HostAPI hostAPI = mock(HostAPI.class);
        final SiteHelper helper = new SiteHelper(hostAPI, mock(HostVariableAPI.class));

        final Host site = mock(Host.class);
        final User user = new User();
        final long expectedCount = 4L;

        when(hostAPI.countDescendantHosts(site)).thenReturn(expectedCount);

        final long actual = helper.cascadeUnarchive(site, user, false);

        assertEquals("cascadeUnarchive should return the descendant count", expectedCount, actual);
        verify(hostAPI).countDescendantHosts(site);
        verify(hostAPI).cascadeUnarchive(eq(site), eq(user), eq(false));
    }

    /**
     * When the site has no descendants, {@link SiteHelper#cascadeUnarchive} must return 0.
     */
    @Test
    public void cascadeUnarchive_noDescendants_returnsZero()
            throws DotDataException, DotSecurityException {
        final HostAPI hostAPI = mock(HostAPI.class);
        final SiteHelper helper = new SiteHelper(hostAPI, mock(HostVariableAPI.class));

        final Host site = mock(Host.class);
        final User user = new User();

        when(hostAPI.countDescendantHosts(site)).thenReturn(0L);

        final long actual = helper.cascadeUnarchive(site, user, false);

        assertEquals("cascadeUnarchive should return 0 when site has no descendants", 0L, actual);
    }

    /**
     * The descendant count must be captured <em>before</em> the unarchive call.
     */
    @Test
    public void cascadeUnarchive_capturesCountBeforeOperation()
            throws DotDataException, DotSecurityException {
        final HostAPI hostAPI = mock(HostAPI.class);
        final SiteHelper helper = new SiteHelper(hostAPI, mock(HostVariableAPI.class));

        final Host site = mock(Host.class);
        final User user = new User();

        when(hostAPI.countDescendantHosts(site)).thenReturn(7L);

        final long actual = helper.cascadeUnarchive(site, user, true);

        assertEquals("Should return pre-unarchive snapshot count", 7L, actual);
        // Verify ordering: count before cascadeUnarchive
        final org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(hostAPI);
        inOrder.verify(hostAPI).countDescendantHosts(site);
        inOrder.verify(hostAPI).cascadeUnarchive(eq(site), eq(user), eq(true));
    }
}
