package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link HTMLPageAssetRenderedAPIImpl}
 */
public class HTMLPageAssetRenderedAPIImplTest {
    private final String CURRENT_HOST_NAME = "currentHost";
    private final Host currentHost = mock(Host.class);

    private final Language DEFAULT_LANGUAGE = mock(Language.class);

    private PermissionAPI permissionAPI;
    private UserAPI userAPI;
    private final User systemUser = mock(User.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HostWebAPI hostWebAPI = mock(HostWebAPI.class);
    private final LanguageAPI languageAPI = mock(LanguageAPI.class);
    private final HTMLPageAssetAPI htmlPageAssetAPI = mock(HTMLPageAssetAPI.class);
    private final VersionableAPI versionableAPI = mock(VersionableAPI.class);
    private final HostAPI hostAPI = mock(HostAPI.class);
    private final HttpSession httpSession = mock(HttpSession.class);
    private HTMLPageAssetRenderedAPIImpl hTMLPageAssetRenderedAPIImpl;

    @Before
    public void init() throws DotDataException, DotSecurityException {
        permissionAPI = mock(PermissionAPI.class);

        userAPI = mock(UserAPI.class);
        when(userAPI.getSystemUser()).thenReturn(systemUser);

        when(request.getServerName()).thenReturn(CURRENT_HOST_NAME);
        when(request.getSession()).thenReturn(httpSession);

        when(this.hostWebAPI.resolveHostName(CURRENT_HOST_NAME, systemUser, PageMode.PREVIEW_MODE.respectAnonPerms)).thenReturn(currentHost);

        when(languageAPI.getDefaultLanguage()).thenReturn(DEFAULT_LANGUAGE);

        when(DEFAULT_LANGUAGE.getId()).thenReturn(1l);

        when(httpSession.getAttribute( com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID )).thenReturn(null);

        hTMLPageAssetRenderedAPIImpl = new HTMLPageAssetRenderedAPIImpl(permissionAPI, userAPI, hostWebAPI, languageAPI,
                htmlPageAssetAPI, versionableAPI, hostAPI);
    }

    @Test
    public void testShouldReturnPREVIEWMODE_whenPageIsNotLockAndUserHaveReadPermission()
            throws DotSecurityException, DotDataException {

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("user");

        final String pageUri = "/pageUri";
        final HTMLPageAsset htmlPage = mock(HTMLPageAsset.class);

        final ContentletVersionInfo info = mock(ContentletVersionInfo.class);
        when(info.getLockedBy()).thenReturn(null);

        when(htmlPage.getIdentifier()).thenReturn("1");
        when(htmlPage.getLanguageId()).thenReturn(2l);

        when(htmlPageAssetAPI.getPageByPath(pageUri, currentHost, DEFAULT_LANGUAGE.getId(),
                PageMode.PREVIEW_MODE.showLive)).thenReturn(htmlPage);
        when(versionableAPI.getContentletVersionInfo(htmlPage.getIdentifier(), htmlPage.getLanguageId())).thenReturn(info);

        when(permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), user,
                false)).thenReturn(true);

        when(this.permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), systemUser,
                PageMode.PREVIEW_MODE.respectAnonPerms)).thenReturn(true);

        final PageMode defaultEditPageMode = hTMLPageAssetRenderedAPIImpl.getDefaultEditPageMode(user, request, pageUri);

        assertEquals(PageMode.PREVIEW_MODE,defaultEditPageMode);
    }

    @Test
    public void testShouldReturnADMINMODE_whenPageIsNotLockAndUserNotHaveReadPermission()
            throws DotSecurityException, DotDataException{

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("user");

        final String pageUri = "/pageUri";
        final HTMLPageAsset htmlPage = mock(HTMLPageAsset.class);

        final ContentletVersionInfo info = mock(ContentletVersionInfo.class);
        when(info.getLockedBy()).thenReturn(null);

        when(htmlPage.getIdentifier()).thenReturn("1");
        when(htmlPage.getLanguageId()).thenReturn(2l);

        when(htmlPageAssetAPI.getPageByPath(pageUri, currentHost, DEFAULT_LANGUAGE.getId(),
                PageMode.PREVIEW_MODE.showLive)).thenReturn(htmlPage);
        when(versionableAPI.getContentletVersionInfo(htmlPage.getIdentifier(), htmlPage.getLanguageId())).thenReturn(info);

        when(permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), user,
                false)).thenReturn(false);

        when(this.permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), systemUser,
                PageMode.PREVIEW_MODE.respectAnonPerms)).thenReturn(true);

        final PageMode defaultEditPageMode = hTMLPageAssetRenderedAPIImpl.getDefaultEditPageMode(user, request, pageUri);

        assertEquals(PageMode.ADMIN_MODE, defaultEditPageMode);
    }

    @Test
    public void testShouldReturnEDITEMODE_whenPageIsLockByCurrentUser()
            throws DotSecurityException, DotDataException{

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("user");

        final String pageUri = "/pageUri";
        final HTMLPageAsset htmlPage = mock(HTMLPageAsset.class);

        final ContentletVersionInfo info = mock(ContentletVersionInfo.class);
        when(info.getLockedBy()).thenReturn("user");

        when(htmlPage.getIdentifier()).thenReturn("1");
        when(htmlPage.getLanguageId()).thenReturn(2l);

        when(htmlPageAssetAPI.getPageByPath(pageUri, currentHost, DEFAULT_LANGUAGE.getId(),
                PageMode.PREVIEW_MODE.showLive)).thenReturn(htmlPage);
        when(versionableAPI.getContentletVersionInfo(htmlPage.getIdentifier(), htmlPage.getLanguageId())).thenReturn(info);

        when(this.permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), systemUser,
                PageMode.PREVIEW_MODE.respectAnonPerms)).thenReturn(true);

        final PageMode defaultEditPageMode = hTMLPageAssetRenderedAPIImpl.getDefaultEditPageMode(user, request, pageUri);

        assertEquals(PageMode.EDIT_MODE,defaultEditPageMode);
    }
}
