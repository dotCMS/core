package com.dotcms.rest.api.v1.site;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.util.PaginationUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * Unit tests for the cascade archive and unarchive REST endpoints in {@link SiteResource}.
 *
 * <p>These tests cover:
 * <ul>
 *   <li>Manual (non-cascade) archive: verifies that {@link SiteHelper#archive} is called and
 *       {@link SiteHelper#cascadeArchive} is NOT called.</li>
 *   <li>Cascade archive: verifies that {@link SiteHelper#cascadeArchive} is called and the
 *       response body contains the expected {@code cascade} and {@code descendantsArchived}
 *       fields.</li>
 *   <li>Default-site guard: verifies that archiving the default site returns a 400 error.</li>
 *   <li>Site-not-found guard: verifies that a 404 is returned when the site is unknown.</li>
 *   <li>Manual unarchive: verifies that {@link SiteHelper#unarchive} is called and
 *       {@link SiteHelper#cascadeUnarchive} is NOT called.</li>
 *   <li>Cascade unarchive: verifies that {@link SiteHelper#cascadeUnarchive} is called and
 *       the response body contains the expected {@code cascade} and {@code descendantsUnarchived}
 *       fields.</li>
 * </ul>
 */
public class SiteResourceCascadeArchiveTest extends UnitTestBase {

    private static final String SITE_ID = "test-site-id-0001";

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a non-default, non-locked, non-archived Host mock with the given identifier.
     */
    private Host mockNormalSite(final String id)
            throws DotStateException, DotDataException, DotSecurityException {
        final Host site = mock(Host.class);
        when(site.getIdentifier()).thenReturn(id);
        when(site.getInode()).thenReturn("inode-" + id);
        when(site.isDefault()).thenReturn(false);
        when(site.isLocked()).thenReturn(false);
        when(site.isArchived()).thenReturn(false);
        when(site.isLive()).thenReturn(true);
        when(site.isWorking()).thenReturn(true);
        when(site.isSystemHost()).thenReturn(false);
        when(site.getHostname()).thenReturn("test.example.com");
        return site;
    }

    /**
     * Builds a {@link SiteResource} wired with the given mocks.
     */
    private SiteResource buildResource(final WebResource webResource,
                                       final SiteHelper siteHelper) {
        return new SiteResource(webResource, siteHelper, mock(PaginationUtil.class));
    }

    /**
     * Creates a {@link WebResource} mock whose {@code init()} returns an
     * {@link InitDataObject} that supplies the given user.
     */
    private WebResource mockWebResource(final User user) {
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initData = mock(InitDataObject.class);
        when(initData.getUser()).thenReturn(user);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initData);
        return webResource;
    }

    /**
     * Creates an {@link HttpServletRequest} mock that returns
     * {@link PageMode#PREVIEW_MODE} (respectAnonPerms = false).
     */
    private HttpServletRequest mockRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        // Force PREVIEW_MODE explicitly so respectAnonPerms = false and
        // SiteResource uses getSiteNoFrontEndRoles() for all site lookups.
        when(request.getAttribute(com.dotmarketing.util.WebKeys.PAGE_MODE_PARAMETER))
                .thenReturn(PageMode.PREVIEW_MODE.name());
        when(request.getSession(false)).thenReturn(null);
        return request;
    }

    /**
     * Stubs both getSite() and getSiteNoFrontEndRoles() on the helper to return {@code site}.
     * This guards against PageMode resolution choosing either path.
     */
    private void stubSiteLookup(final SiteHelper siteHelper, final User user, final Host site)
            throws DotDataException, DotSecurityException {
        when(siteHelper.getSite(user, SITE_ID)).thenReturn(site);
        when(siteHelper.getSiteNoFrontEndRoles(user, SITE_ID)).thenReturn(site);
    }

    // -------------------------------------------------------------------------
    // Archive tests
    // -------------------------------------------------------------------------

    /**
     * When {@code cascade=false}, {@link SiteHelper#archive} is called and
     * {@link SiteHelper#cascadeArchive} is never called.
     */
    @Test
    public void archiveSite_noCascade_callsArchive()
            throws DotDataException, DotSecurityException {
        final User user = new User();
        final Host site = mockNormalSite(SITE_ID);

        final SiteHelper siteHelper = mock(SiteHelper.class);
        stubSiteLookup(siteHelper, user, site);

        final SiteResource resource = buildResource(mockWebResource(user), siteHelper);
        final Response response = resource.archiveSite(
                mockRequest(), mock(HttpServletResponse.class), SITE_ID, false);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(siteHelper).archive(eq(site), eq(user), anyBoolean());
        verify(siteHelper, never()).cascadeArchive(any(), any(), anyBoolean());
    }

    /**
     * When {@code cascade=true} and the site has 3 descendants, {@link SiteHelper#cascadeArchive}
     * is called and the response body contains:
     * <ul>
     *   <li>{@code cascade = true}</li>
     *   <li>{@code descendantsArchived = 3}</li>
     * </ul>
     */
    @Test
    public void archiveSite_cascade_callsCascadeArchiveAndReturnsCount()
            throws DotDataException, DotSecurityException {
        final User user = new User();
        final Host site = mockNormalSite(SITE_ID);

        final SiteHelper siteHelper = mock(SiteHelper.class);
        stubSiteLookup(siteHelper, user, site);
        // cascadeArchive returns the number of descendants targeted (3 in this test)
        when(siteHelper.cascadeArchive(eq(site), eq(user), anyBoolean())).thenReturn(3L);

        final SiteResource resource = buildResource(mockWebResource(user), siteHelper);
        final Response response = resource.archiveSite(
                mockRequest(), mock(HttpServletResponse.class), SITE_ID, true);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(siteHelper).cascadeArchive(eq(site), eq(user), anyBoolean());
        verify(siteHelper, never()).archive(any(), any(), anyBoolean());

        @SuppressWarnings("unchecked")
        final ResponseEntityView<Map<String, Object>> entityView =
                (ResponseEntityView<Map<String, Object>>) response.getEntity();
        assertNotNull("Response entity must not be null", entityView);
        final Map<String, Object> entity = entityView.getEntity();
        assertNotNull("Entity map must not be null", entity);
        assertEquals("cascade flag should be true", Boolean.TRUE, entity.get("cascade"));
        assertEquals("descendantsArchived should be 3", 3L, entity.get("descendantsArchived"));
    }

    /**
     * When {@code cascade=true} and the site has 0 descendants,
     * {@code descendantsArchived} in the response must be 0.
     */
    @Test
    public void archiveSite_cascade_noDescendants_returnsZeroCount()
            throws DotDataException, DotSecurityException {
        final User user = new User();
        final Host site = mockNormalSite(SITE_ID);

        final SiteHelper siteHelper = mock(SiteHelper.class);
        stubSiteLookup(siteHelper, user, site);
        when(siteHelper.cascadeArchive(eq(site), eq(user), anyBoolean())).thenReturn(0L);

        final SiteResource resource = buildResource(mockWebResource(user), siteHelper);
        final Response response = resource.archiveSite(
                mockRequest(), mock(HttpServletResponse.class), SITE_ID, true);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        @SuppressWarnings("unchecked")
        final Map<String, Object> entity =
                ((ResponseEntityView<Map<String, Object>>) response.getEntity()).getEntity();
        assertEquals("descendantsArchived should be 0 when site has no children",
                0L, entity.get("descendantsArchived"));
    }

    /**
     * Archiving the default site must be rejected regardless of the cascade flag.
     */
    @Test(expected = DotStateException.class)
    public void archiveSite_defaultSite_throwsDotStateException()
            throws DotDataException, DotSecurityException {
        final User user = new User();
        final Host defaultSite = mockNormalSite(SITE_ID);
        when(defaultSite.isDefault()).thenReturn(true);

        final SiteHelper siteHelper = mock(SiteHelper.class);
        stubSiteLookup(siteHelper, user, defaultSite);

        final SiteResource resource = buildResource(mockWebResource(user), siteHelper);
        resource.archiveSite(mockRequest(), mock(HttpServletResponse.class), SITE_ID, false);
    }

    /**
     * When the site is not found, {@link com.dotcms.rest.exception.NotFoundException} must be
     * thrown (will produce a 404).
     */
    @Test(expected = com.dotcms.rest.exception.NotFoundException.class)
    public void archiveSite_siteNotFound_throwsNotFoundException()
            throws DotDataException, DotSecurityException {
        final User user = new User();

        final SiteHelper siteHelper = mock(SiteHelper.class);
        when(siteHelper.getSiteNoFrontEndRoles(user, SITE_ID)).thenReturn(null);

        final SiteResource resource = buildResource(mockWebResource(user), siteHelper);
        resource.archiveSite(mockRequest(), mock(HttpServletResponse.class), SITE_ID, false);
    }

    // -------------------------------------------------------------------------
    // Unarchive tests
    // -------------------------------------------------------------------------

    /**
     * When {@code cascade=false}, {@link SiteHelper#unarchive} is called and
     * {@link SiteHelper#cascadeUnarchive} is never called.
     */
    @Test
    public void unarchiveSite_noCascade_callsUnarchive()
            throws DotDataException, DotSecurityException {
        final User user = new User();
        final Host site = mockNormalSite(SITE_ID);

        final SiteHelper siteHelper = mock(SiteHelper.class);
        stubSiteLookup(siteHelper, user, site);

        final SiteResource resource = buildResource(mockWebResource(user), siteHelper);
        final Response response = resource.unarchiveSite(
                mockRequest(), mock(HttpServletResponse.class), SITE_ID, false);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(siteHelper).unarchive(eq(site), eq(user), anyBoolean());
        verify(siteHelper, never()).cascadeUnarchive(any(), any(), anyBoolean());
    }

    /**
     * When {@code cascade=true} and the site has 2 descendants, {@link SiteHelper#cascadeUnarchive}
     * is called and the response body contains:
     * <ul>
     *   <li>{@code cascade = true}</li>
     *   <li>{@code descendantsUnarchived = 2}</li>
     * </ul>
     */
    @Test
    public void unarchiveSite_cascade_callsCascadeUnarchiveAndReturnsCount()
            throws DotDataException, DotSecurityException {
        final User user = new User();
        final Host site = mockNormalSite(SITE_ID);

        final SiteHelper siteHelper = mock(SiteHelper.class);
        stubSiteLookup(siteHelper, user, site);
        when(siteHelper.cascadeUnarchive(eq(site), eq(user), anyBoolean())).thenReturn(2L);

        final SiteResource resource = buildResource(mockWebResource(user), siteHelper);
        final Response response = resource.unarchiveSite(
                mockRequest(), mock(HttpServletResponse.class), SITE_ID, true);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(siteHelper).cascadeUnarchive(eq(site), eq(user), anyBoolean());
        verify(siteHelper, never()).unarchive(any(), any(), anyBoolean());

        @SuppressWarnings("unchecked")
        final ResponseEntityView<Map<String, Object>> entityView =
                (ResponseEntityView<Map<String, Object>>) response.getEntity();
        assertNotNull("Response entity must not be null", entityView);
        final Map<String, Object> entity = entityView.getEntity();
        assertNotNull("Entity map must not be null", entity);
        assertEquals("cascade flag should be true", Boolean.TRUE, entity.get("cascade"));
        assertEquals("descendantsUnarchived should be 2", 2L, entity.get("descendantsUnarchived"));
    }

    /**
     * When the site is not found for unarchive, {@link IllegalArgumentException} must be
     * thrown (will produce a 400).
     */
    @Test(expected = IllegalArgumentException.class)
    public void unarchiveSite_siteNotFound_throwsIllegalArgumentException()
            throws DotDataException, DotSecurityException {
        final User user = new User();

        final SiteHelper siteHelper = mock(SiteHelper.class);
        when(siteHelper.getSiteNoFrontEndRoles(user, SITE_ID)).thenReturn(null);

        final SiteResource resource = buildResource(mockWebResource(user), siteHelper);
        resource.unarchiveSite(mockRequest(), mock(HttpServletResponse.class), SITE_ID, false);
    }

    /**
     * When {@code cascade=true} and the site has 0 descendants,
     * {@code descendantsUnarchived} must be 0 in the response.
     */
    @Test
    public void unarchiveSite_cascade_noDescendants_returnsZeroCount()
            throws DotDataException, DotSecurityException {
        final User user = new User();
        final Host site = mockNormalSite(SITE_ID);

        final SiteHelper siteHelper = mock(SiteHelper.class);
        stubSiteLookup(siteHelper, user, site);
        when(siteHelper.cascadeUnarchive(eq(site), eq(user), anyBoolean())).thenReturn(0L);

        final SiteResource resource = buildResource(mockWebResource(user), siteHelper);
        final Response response = resource.unarchiveSite(
                mockRequest(), mock(HttpServletResponse.class), SITE_ID, true);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        @SuppressWarnings("unchecked")
        final Map<String, Object> entity =
                ((ResponseEntityView<Map<String, Object>>) response.getEntity()).getEntity();
        assertEquals("descendantsUnarchived should be 0 when site has no children",
                0L, entity.get("descendantsUnarchived"));
    }
}
