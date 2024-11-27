package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotcms.analytics.web.AnalyticsWebAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.cms.urlmap.URLMapAPIImpl;
import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.cms.urlmap.UrlMapContext;
import com.dotmarketing.cms.urlmap.UrlMapContextBuilder;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
    private final HttpSession httpSession = mock(HttpSession.class);
    private final URLMapAPIImpl urlMapAPIImpl = mock(URLMapAPIImpl.class);
    private final LanguageWebAPI languageWebAPI = mock(LanguageWebAPI.class);
    private HTMLPageAssetRenderedAPIImpl hTMLPageAssetRenderedAPIImpl;

    @Before
    public void init() throws DotDataException, DotSecurityException {
        permissionAPI = mock(PermissionAPI.class);


        userAPI = mock(UserAPI.class);
        when(userAPI.getSystemUser()).thenReturn(systemUser);

        when(request.getServerName()).thenReturn(CURRENT_HOST_NAME);
        when(request.getSession()).thenReturn(httpSession);
        when(request.getSession(false)).thenReturn(httpSession);

        when(languageAPI.getDefaultLanguage()).thenReturn(DEFAULT_LANGUAGE);
        when(DEFAULT_LANGUAGE.getId()).thenReturn(1l);
        when(languageWebAPI.getLanguage(any(HttpServletRequest.class))).thenReturn(DEFAULT_LANGUAGE);

        hTMLPageAssetRenderedAPIImpl = new HTMLPageAssetRenderedAPIImpl(
                permissionAPI, userAPI, hostWebAPI, languageAPI, htmlPageAssetAPI,
                urlMapAPIImpl, languageWebAPI, mock(AnalyticsWebAPI.class));
    }

    @Test
    public void testShouldReturnPREVIEWMODE_whenPageIsNotLockAndUserHaveReadPermission()
            throws DotSecurityException, DotDataException {

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("user");

        final String pageUri = "/pageUri";
        final HTMLPageAsset htmlPage = mock(HTMLPageAsset.class);

        when(htmlPage.getIdentifier()).thenReturn("1");
        when(htmlPage.getLanguageId()).thenReturn(1l);

        when(htmlPageAssetAPI.getPageByPath(pageUri, currentHost, DEFAULT_LANGUAGE.getId(),
                PageMode.PREVIEW_MODE.showLive)).thenReturn(htmlPage);

        when(permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), user,
                false)).thenReturn(true);

        when(this.permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), systemUser,
                PageMode.PREVIEW_MODE.respectAnonPerms)).thenReturn(true);

        when(this.hostWebAPI.getCurrentHost(request, systemUser)).thenReturn(currentHost);

        final PageMode defaultEditPageMode =
                hTMLPageAssetRenderedAPIImpl.getDefaultEditPageMode(user, request, pageUri);

        assertEquals(PageMode.PREVIEW_MODE,defaultEditPageMode);
    }

    @Test()
    public void testShouldReturnADMINMODE_whenPageIsNotLockAndUserNotHaveReadPermission()
            throws DotSecurityException, DotDataException{

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("user");

        final String pageUri = "/pageUri";
        final HTMLPageAsset htmlPage = mock(HTMLPageAsset.class);

        when(htmlPage.getIdentifier()).thenReturn("1");
        when(htmlPage.getLanguageId()).thenReturn(2l);

        when(htmlPageAssetAPI.getPageByPath(pageUri, currentHost, DEFAULT_LANGUAGE.getId(),
                PageMode.PREVIEW_MODE.showLive)).thenReturn(htmlPage);

        when(permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), user,
                false)).thenReturn(false);

        when(this.permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), systemUser,
                PageMode.PREVIEW_MODE.respectAnonPerms)).thenReturn(true);

        when(this.permissionAPI.doesUserHavePermission(currentHost, PermissionLevel.READ.getType(), user,
                PageMode.PREVIEW_MODE.respectAnonPerms)).thenReturn(true);


        when(this.hostWebAPI.getCurrentHost(request, systemUser)).thenReturn(currentHost);
        try {
            hTMLPageAssetRenderedAPIImpl.getDefaultEditPageMode(user, request, pageUri);
        } catch (final DotRuntimeException e) {
            assertEquals(DotSecurityException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testShouldReturnEDITEMODE_whenPageIsLockByCurrentUser()
            throws DotSecurityException, DotDataException{

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("user");

        final String pageUri = "/pageUri";
        final HTMLPageAsset htmlPage = mock(HTMLPageAsset.class);

        when(htmlPage.getIdentifier()).thenReturn("1");
        when(htmlPage.getLanguageId()).thenReturn(2l);

        when(htmlPageAssetAPI.getPageByPath(pageUri, currentHost, DEFAULT_LANGUAGE.getId(),
                PageMode.PREVIEW_MODE.showLive)).thenReturn(htmlPage);

        when(this.permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), user,
                PageMode.PREVIEW_MODE.respectAnonPerms)).thenReturn(true);

        when(this.permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), systemUser,
                PageMode.PREVIEW_MODE.respectAnonPerms)).thenReturn(true);

        when(this.hostWebAPI.getCurrentHost(request, systemUser)).thenReturn(currentHost);

        final PageMode defaultEditPageMode =
                hTMLPageAssetRenderedAPIImpl.getDefaultEditPageMode(user, request, pageUri);

        assertEquals(PageMode.PREVIEW_MODE,defaultEditPageMode);
    }

    @Test
    public void testShouldDoAURLMapper_whenPageIsNotFoundByURI()
            throws DotSecurityException, DotDataException {

        final HTMLPageAsset htmlPage = mock(HTMLPageAsset.class);

        final Contentlet contentlet = mock(Contentlet.class);
        final Identifier identifier = mock(Identifier.class);
        final URLMapInfo urlMapInfo = mock(URLMapInfo.class);

        final Language language = mock(Language.class);
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("user");

        final String pageUri = "/pageUri";

        when(htmlPage.getIdentifier()).thenReturn("1");
        when(htmlPage.getLanguageId()).thenReturn(2l);

        when(htmlPageAssetAPI.getPageByPath(pageUri, currentHost, DEFAULT_LANGUAGE.getId(),
                PageMode.PREVIEW_MODE.showLive)).thenReturn(null);

        when(htmlPageAssetAPI.getPageByPath(pageUri, currentHost, DEFAULT_LANGUAGE.getId(),
                PageMode.PREVIEW_MODE.showLive)).thenReturn(htmlPage);

        when(languageWebAPI.getLanguage(any(HttpServletRequest.class))).thenReturn(language);

        when(urlMapInfo.getContentlet()).thenReturn(contentlet);
        when(urlMapInfo.getIdentifier()).thenReturn(identifier);

        when(contentlet.getIdentifier()).thenReturn("identifier");
        when(contentlet.getInode()).thenReturn("inode");
        when(identifier.getURI()).thenReturn("uri");

        when(language.getId()).thenReturn(1l);

        when(this.permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), user,
                PageMode.PREVIEW_MODE.respectAnonPerms)).thenReturn(true);
        when(this.permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), systemUser,
                PageMode.PREVIEW_MODE.respectAnonPerms)).thenReturn(true);

        final UrlMapContext urlMapContext = UrlMapContextBuilder.builder()
                .setMode(PageMode.PREVIEW_MODE)
                .setUri(pageUri)
                .setUser(systemUser)
                .setHost(currentHost)
                .setLanguageId(language.getId())
                .build();

        when(urlMapAPIImpl.processURLMap(urlMapContext))
                .thenReturn(Optional.of(urlMapInfo));

        when(this.hostWebAPI.getCurrentHost(request, systemUser)).thenReturn(currentHost);

        final PageMode defaultEditPageMode =
                hTMLPageAssetRenderedAPIImpl.getDefaultEditPageMode(user, request, pageUri);

        assertEquals(PageMode.PREVIEW_MODE, defaultEditPageMode);
    }

    @Test
    public void testShouldReturnAdminMODE_whenUserDontHavePermission()
            throws DotSecurityException, DotDataException{

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("user");

        final String pageUri = "/pageUri";
        final HTMLPageAsset htmlPage = mock(HTMLPageAsset.class);

        when(htmlPage.getIdentifier()).thenReturn("1");
        when(htmlPage.getLanguageId()).thenReturn(2l);

        when(htmlPageAssetAPI.getPageByPath(pageUri, currentHost, DEFAULT_LANGUAGE.getId(),
                PageMode.PREVIEW_MODE.showLive)).thenReturn(htmlPage);

        when(this.permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), user,
                PageMode.PREVIEW_MODE.respectAnonPerms)).thenReturn(false);

        when(this.permissionAPI.doesUserHavePermission(htmlPage, PermissionLevel.READ.getType(), systemUser,
                PageMode.PREVIEW_MODE.respectAnonPerms)).thenReturn(true);

        when(this.hostWebAPI.getCurrentHost(request, systemUser)).thenReturn(currentHost);

        final PageMode defaultEditPageMode =
                hTMLPageAssetRenderedAPIImpl.getDefaultEditPageMode(user, request, pageUri);

        assertEquals(PageMode.ADMIN_MODE, defaultEditPageMode);
    }
}
