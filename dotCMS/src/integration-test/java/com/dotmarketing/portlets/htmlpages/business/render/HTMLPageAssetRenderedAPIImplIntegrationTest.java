package com.dotmarketing.portlets.htmlpages.business.render;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.*;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContext;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HTMLPageAssetRenderedAPIImplIntegrationTest extends IntegrationTestBase {

    private HttpServletRequest request;
    private Host host;
    private User user;
    private HttpServletResponse response;
    private HTMLPageAsset htmlPageAsset;
    private HttpSession session;
    private Role role;

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void init () throws DotDataException, DotSecurityException {
        init(null);
    }

    private void init (final Host host) throws DotSecurityException, DotDataException {
        final User systemUser = APILocator.systemUser();
        request = mock(HttpServletRequest.class);

        session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);

        response = mock(HttpServletResponse.class);

        role = new RoleDataGen().nextPersisted();
        user = new UserDataGen().roles(role).nextPersisted();

        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadBackEndUserRole(), user);
        this.host = host == null ? createHost() : host;

        final Template template = new TemplateDataGen().nextPersisted();

        htmlPageAsset = createPage(systemUser, role, template);
        when(request.getRequestURI()).thenReturn(htmlPageAsset.getURI());
    }

    private HTMLPageAsset createPage(final User systemUser, final Role role, final Template template)
            throws DotDataException, DotSecurityException {

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        HTMLPageDataGen.publish(htmlPageAsset);

        final Permission permission = new Permission();
        permission.setInode(htmlPageAsset.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(PermissionAPI.PERMISSION_READ);
        APILocator.getPermissionAPI().save(CollectionsUtils.list(permission), htmlPageAsset, systemUser, false);
        return htmlPageAsset;
    }

    private Host createHost() throws DotDataException, DotSecurityException {
        return new SiteDataGen().nextPersisted();
    }

    private void addPermission(final Role role, final Host host)
            throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();

        final Permission permission = new Permission();
        permission.setInode(host.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(PermissionAPI.PERMISSION_READ);

        APILocator.getPermissionAPI().save(CollectionsUtils.list(permission), host, systemUser, false);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPIImpl#getPageRendered(PageContext, HttpServletRequest, HttpServletResponse)}
     * When The host_id request's parameter is set, it should take this over session attribute
     * Should return a right {@link PageView}
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test
    public void shouldUseHostIdParameter() throws DotDataException, DotSecurityException {
        init();
        final PageMode pageMode = PageMode.ADMIN_MODE;
        addPermission(role, host);

        final Host anotherHost = new SiteDataGen().nextPersisted();
        when(session.getAttribute( WebKeys.CMS_SELECTED_HOST_ID )).thenReturn(anotherHost.getIdentifier());

        when(request.getParameter("host_id")).thenReturn(this.host.getIdentifier());
        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(this.host.getHostname());

        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);

        final HTMLPageAssetRenderedAPIImpl htmlPageAssetRenderedAPIImpl = new HTMLPageAssetRenderedAPIImpl();

        final PageView pageRendered = htmlPageAssetRenderedAPIImpl.getPageRendered(
                request, response, user, htmlPageAsset.getURI(), pageMode);

        assertEquals(htmlPageAsset, pageRendered.getPageInfo().getPage());
        assertEquals(host, pageRendered.getSite());
        assertEquals(htmlPageAsset.getURI(), pageRendered.getPageUrlMapper());
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPIImpl#getPageRendered(PageContext, HttpServletRequest, HttpServletResponse)
     * When The host_id request's parameter is set but it does not exists, it should take hostsession attribute
     * Should return a right {@link PageView}
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test
    public void shouldUseDefaultHostWhenHostIdDoesNotExists() throws DotDataException, DotSecurityException {
        init();
        final PageMode pageMode = PageMode.ADMIN_MODE;
        addPermission(role, host);
        when(session.getAttribute( WebKeys.CMS_SELECTED_HOST_ID )).thenReturn(host.getIdentifier());

        when(request.getParameter("host_id")).thenReturn("not_exists_host_id");
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(this.host);

        final HTMLPageAssetRenderedAPIImpl htmlPageAssetRenderedAPIImpl = new HTMLPageAssetRenderedAPIImpl();

        final PageView pageRendered = htmlPageAssetRenderedAPIImpl.getPageRendered(
                request, response, user, htmlPageAsset.getURI(), pageMode);

        assertEquals(htmlPageAsset, pageRendered.getPageInfo().getPage());
        assertEquals(this.host, pageRendered.getSite());
        assertEquals(htmlPageAsset.getURI(), pageRendered.getPageUrlMapper());
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPIImpl#getPageRendered(PageContext, HttpServletRequest, HttpServletResponse)
     * When the {@link Host#HOST_VELOCITY_VAR_NAME} request's parameter is set, it should take this over session attribute
     * Should return a right {@link PageView}
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test
    public void shouldUseHostNameParameter()
            throws DotDataException, DotSecurityException {
        init();
        final PageMode pageMode = PageMode.ADMIN_MODE;
        addPermission(role, host);
        final Host anotherHost = new SiteDataGen().nextPersisted();
        when(session.getAttribute( WebKeys.CMS_SELECTED_HOST_ID )).thenReturn(anotherHost.getIdentifier());

        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(this.host.getHostname());
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(this.host);

        final HTMLPageAssetRenderedAPIImpl htmlPageAssetRenderedAPIImpl = new HTMLPageAssetRenderedAPIImpl();

        final PageView pageRendered = htmlPageAssetRenderedAPIImpl.getPageRendered(
                request, response, user, htmlPageAsset.getURI(), pageMode);

        assertEquals(htmlPageAsset, pageRendered.getPageInfo().getPage());
        assertEquals(this.host, pageRendered.getSite());
        assertEquals(htmlPageAsset.getURI(), pageRendered.getPageUrlMapper());
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPIImpl#getPageRendered(PageContext, HttpServletRequest, HttpServletResponse)
     * When the {@link Host#HOST_VELOCITY_VAR_NAME} request's parameter is set but does not exists,
     * it should take the host session attribute
     * Should return a right {@link PageView}
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test
    public void shouldUseDefaultHostWhenHostNameDoesNotExists()
            throws DotDataException, DotSecurityException {
        init();
        final PageMode pageMode = PageMode.ADMIN_MODE;
        addPermission(role, host);
        when(session.getAttribute( WebKeys.CMS_SELECTED_HOST_ID )).thenReturn(host.getIdentifier());

        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn("not_exists_host_id");
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(this.host);

        final HTMLPageAssetRenderedAPIImpl htmlPageAssetRenderedAPIImpl = new HTMLPageAssetRenderedAPIImpl();

        final PageView pageRendered = htmlPageAssetRenderedAPIImpl.getPageRendered(
                request, response, user, htmlPageAsset.getURI(), pageMode);

        assertEquals(htmlPageAsset, pageRendered.getPageInfo().getPage());
        assertEquals(this.host, pageRendered.getSite());
        assertEquals(htmlPageAsset.getURI(), pageRendered.getPageUrlMapper());
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPIImpl#getPageRendered(PageContext, HttpServletRequest, HttpServletResponse)
     * When Host is not set into request it should use session host,
     * Should return a right {@link PageView}
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test
    public void shouldUseSessionHostWhenHostIsNotSetInRequest()
            throws DotDataException, DotSecurityException {
        init();
        final PageMode pageMode = PageMode.ADMIN_MODE;
        addPermission(role, host);
        when(session.getAttribute( WebKeys.CMS_SELECTED_HOST_ID )).thenReturn(host.getIdentifier());

        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(this.host);

        final HTMLPageAssetRenderedAPIImpl htmlPageAssetRenderedAPIImpl = new HTMLPageAssetRenderedAPIImpl();

        final PageView pageRendered = htmlPageAssetRenderedAPIImpl.getPageRendered(
                request, response, user, htmlPageAsset.getURI(), pageMode);

        assertEquals(htmlPageAsset, pageRendered.getPageInfo().getPage());
        assertEquals(this.host, pageRendered.getSite());
        assertEquals(htmlPageAsset.getURI(), pageRendered.getPageUrlMapper());
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPIImpl#getPageRendered(PageContext, HttpServletRequest, HttpServletResponse)
     * When Host is not set into request neither session
     * it should take the default host
     * Should return a right {@link PageView}
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test
    public void shouldUseDefaultHostWhenHostIsNotSet()
            throws DotDataException, DotSecurityException {
        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), true);
        init(defaultHost);
        final PageMode pageMode = PageMode.ADMIN_MODE;
        addPermission(role, defaultHost);

        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);

        final HTMLPageAssetRenderedAPIImpl htmlPageAssetRenderedAPIImpl = new HTMLPageAssetRenderedAPIImpl();

        final PageView pageRendered = htmlPageAssetRenderedAPIImpl.getPageRendered(
                request, response, user, htmlPageAsset.getURI(), pageMode);

        assertEquals(htmlPageAsset, pageRendered.getPageInfo().getPage());
        assertEquals(defaultHost, pageRendered.getSite());
        assertEquals(htmlPageAsset.getURI(), pageRendered.getPageUrlMapper());
    }


    /**
     * Method to test: {@link HTMLPageAssetRenderedAPIImpl#getPageRendered(PageContext, HttpServletRequest, HttpServletResponse)
     * When Host is not set into request neither session
     * it should take the request's server name before then the default host
     * Should return a right {@link PageView}
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test
    public void shouldUseRequestServerNameHostWhenHostIsNotSet()
            throws DotDataException, DotSecurityException {
        init(host);

        when(request.getServerName()).thenReturn(host.getHostname());
        final PageMode pageMode = PageMode.ADMIN_MODE;
        addPermission(role, host);

        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);

        final HTMLPageAssetRenderedAPIImpl htmlPageAssetRenderedAPIImpl = new HTMLPageAssetRenderedAPIImpl();

        final PageView pageRendered = htmlPageAssetRenderedAPIImpl.getPageRendered(
                request, response, user, htmlPageAsset.getURI(), pageMode);

        assertEquals(htmlPageAsset, pageRendered.getPageInfo().getPage());
        assertEquals(this.host, pageRendered.getSite());
        assertEquals(htmlPageAsset.getURI(), pageRendered.getPageUrlMapper());
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPIImpl#getPageRendered(PageContext, HttpServletRequest, HttpServletResponse)
     * When The host_id request's parameter is set, but the user does not have permission over it
     * Should return a right {@link PageView}
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test(expected = DotSecurityException.class)
    public void shouldUseHostIdParameterAndThrowDotSecurityException() throws DotDataException, DotSecurityException {
        init();
        final PageMode pageMode = PageMode.WORKING;
        when(request.getParameter("host_id")).thenReturn(this.host.getIdentifier());
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);

        final HTMLPageAssetRenderedAPIImpl htmlPageAssetRenderedAPIImpl = new HTMLPageAssetRenderedAPIImpl();

        htmlPageAssetRenderedAPIImpl.getPageRendered(request, response, user, htmlPageAsset.getURI(), pageMode);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPIImpl#getPageRendered(PageContext, HttpServletRequest, HttpServletResponse)
     * When The {@link Host#HOST_VELOCITY_VAR_NAME} request's parameter is set, but the user does not have permission over it
     * Should return a right {@link PageView}
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test(expected = DotSecurityException.class)
    public void shouldUseHostNameParameterAndThrowDotSecurityException() throws DotDataException, DotSecurityException {
        init();
        final PageMode pageMode = PageMode.WORKING;
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(host.getName());

        final HTMLPageAssetRenderedAPIImpl htmlPageAssetRenderedAPIImpl = new HTMLPageAssetRenderedAPIImpl();

        htmlPageAssetRenderedAPIImpl.getPageRendered(request, response, user, htmlPageAsset.getURI(), pageMode);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPIImpl#getPageRendered(PageContext, HttpServletRequest, HttpServletResponse)
     * When The session host is set, but the user does not have permission over it
     * Should throw a {@link DotSecurityException}
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test(expected = DotSecurityException.class)
    public void shouldUseSessionHostAndThrowDotSecurityException() throws DotDataException, DotSecurityException {
        init();
        final PageMode pageMode = PageMode.WORKING;
        when(session.getAttribute( WebKeys.CMS_SELECTED_HOST_ID )).thenReturn(host.getIdentifier());
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);

        final HTMLPageAssetRenderedAPIImpl htmlPageAssetRenderedAPIImpl = new HTMLPageAssetRenderedAPIImpl();

        htmlPageAssetRenderedAPIImpl.getPageRendered(request, response, user, htmlPageAsset.getURI(), pageMode);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPIImpl#getPageRendered(PageContext, HttpServletRequest, HttpServletResponse)
     * When The WebKeys.CURRENT_HOST request attribute is set, but the user does not have permission over it
     * Should throw a {@link DotSecurityException}
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test(expected = DotSecurityException.class)
    public void shouldUseDefaultHostAndThrowDotSecurityException() throws DotDataException, DotSecurityException {
        init();
        final PageMode pageMode = PageMode.WORKING;
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);

        final HTMLPageAssetRenderedAPIImpl htmlPageAssetRenderedAPIImpl = new HTMLPageAssetRenderedAPIImpl();

        htmlPageAssetRenderedAPIImpl.getPageRendered(request, response, user, htmlPageAsset.getURI(), pageMode);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPIImpl#getPageRendered(PageContext, HttpServletRequest, HttpServletResponse)
     * When Host is not set into request neither session
     * it should take the request's server name before then the default host
     * Should return a right {@link PageView}
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test(expected = DotSecurityException.class)
    public void shouldUseRequestServerNameAndThrowDotSecurityException()
            throws DotDataException, DotSecurityException {
        init(host);

        when(request.getServerName()).thenReturn(host.getHostname());
        final PageMode pageMode = PageMode.WORKING;

        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);

        final HTMLPageAssetRenderedAPIImpl htmlPageAssetRenderedAPIImpl = new HTMLPageAssetRenderedAPIImpl();

        htmlPageAssetRenderedAPIImpl.getPageRendered(request, response, user, htmlPageAsset.getURI(), pageMode);

    }
}
