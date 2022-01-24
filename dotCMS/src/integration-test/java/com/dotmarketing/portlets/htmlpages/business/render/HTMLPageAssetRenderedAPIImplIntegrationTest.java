package com.dotmarketing.portlets.htmlpages.business.render;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContext;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageLivePreviewVersionBean;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import io.swagger.annotations.Api;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Date;

import static com.dotcms.rendering.velocity.directive.ParseContainer.getDotParserContainerUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
        when(request.getSession(true)).thenReturn(session);

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
     * Method to test: {@link HTMLPageAssetRenderedAPIImpl#getPageRenderedLivePreviewVersion(String, User, long, HttpServletRequest, HttpServletResponse)}
     * 1) Creates a container with rich text + template
     * 2) Creates a page with that template
     * 3) Creates a rich content published
     * 4) associated the contentlet to the container in the page.
     * 5) modify the content and save (do not publish)
     * 6) Get the versions
     * 7) publish again the content
     * 8) Get the versions
     * Should return a right {@link PageView}
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test
    public void test_getPageRenderedLivePreviewVersion_diff() throws DotDataException, DotSecurityException, WebAssetException {
        init();
        final User adminUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadCMSAdminRole(), adminUser);
        assertTrue(APILocator.getUserAPI().isCMSAdmin(adminUser));
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        // 1) create a container with rich text
        final Host site     = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        final ContentType contentType = APILocator.getContentTypeAPI(adminUser).find("webPageContent");
        final String nameTitle = "anyTestContainer" + System.currentTimeMillis();
        final String containerUUID = UUIDGenerator.generateUuid();
        final Container container = new ContainerDataGen()
                .site(site)
                .modUser(adminUser)
                .friendlyName(nameTitle)
                .title(nameTitle)
                .withContentType(contentType, "$!{body}")
                .nextPersisted();
        ContainerDataGen.publish(container, adminUser);

        // 2) Creates a page with that template
        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), containerUUID)
                .nextPersisted();

        /*final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(container, containerUUID)
                .next();

        final Contentlet theme  = new ThemeDataGen().site(host).nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier())
                .host(host)
                .drawedBody(templateLayout)
                .theme(theme)
                .nextPersisted();*/
        TemplateDataGen.publish(template, adminUser);

        final String pageName = "test-page" + UUIDGenerator.generateUuid().substring(1, 6);
        final HTMLPageAsset page    = new HTMLPageDataGen(folder, template)
                .pageURL(pageName)
                .friendlyName(pageName)
                .title(pageName)
                .languageId(languageId)
                .nextPersisted();
        APILocator.getContentletAPI().publish(page, adminUser, false);

        // 3) Creates a rich content published
        final Contentlet contentlet = new ContentletDataGen(contentType)
                .folder(folder)
                .host(site)
                .languageId(languageId)
                .setProperty("title", "test")
                .setProperty("body", "Test1")
                .nextPersisted();
        contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().publish(contentlet, adminUser, false);

        // 4) associated the contentlet to the container in the page.
        final MultiTree multiTree = new MultiTree()
                .setContainer(container.getIdentifier())
                .setContentlet(contentlet.getIdentifier())
                .setInstanceId(getDotParserContainerUUID(containerUUID))
                .setTreeOrder(0)
                .setHtmlPage(page.getIdentifier());

        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);
        APILocator.getContentletAPI().publish(page, adminUser, false);

        // 5) modify the content and save (do not publish)
        final Contentlet workingPage = APILocator.getContentletAPI()
                .checkout(page.getInode(), adminUser, false);
        final Contentlet contentletCheckout = APILocator.getContentletAPI()
                .checkout(contentlet.getInode(), adminUser, false);
        contentletCheckout.setProperty("body", "Test1 Modified");
        contentletCheckout.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().checkin(contentletCheckout, adminUser, false);
        workingPage.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().checkin(workingPage, adminUser, false);

        // 6) Get the versions
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        final HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        final HttpSession session = mock(HttpSession.class);

        when(httpRequest.getRequestURI()).thenReturn("/"+pageName);
        when(httpRequest.getSession()).thenReturn(session);
        when(httpRequest.getSession(false)).thenReturn(session);
        when(httpRequest.getSession(true)).thenReturn(session);
        when(httpRequest.getAttribute(com.liferay.portal.util.WebKeys.USER_ID))
                .thenReturn(adminUser.getUserId());
        when(httpRequest.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(adminUser);
        when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(adminUser);

        when(httpRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        when(httpRequest.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(site);
        when(session.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn(languageId+"");
        when(session.getAttribute(WebKeys.PAGE_MODE_SESSION)).thenReturn(PageMode.PREVIEW_MODE);
        final PageLivePreviewVersionBean pageLivePreviewVersionBean =
                APILocator.getHTMLPageAssetRenderedAPI().getPageRenderedLivePreviewVersion(
                        page.getIdentifier(), adminUser, languageId, httpRequest, httpResponse);

        Assert.assertTrue(pageLivePreviewVersionBean.isDiff());

        // 7) publish again the content
        APILocator.getContentletAPI().publish(contentlet, adminUser, false);

        // 8) Get the versions
        final PageLivePreviewVersionBean pageLivePreviewVersionBean2 =
                APILocator.getHTMLPageAssetRenderedAPI().getPageRenderedLivePreviewVersion(
                    page.getIdentifier(), adminUser, languageId, httpRequest, httpResponse);

        Assert.assertFalse(pageLivePreviewVersionBean2.isDiff());
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

        assertEquals(htmlPageAsset, pageRendered.getPage());
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

        assertEquals(htmlPageAsset, pageRendered.getPage());
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

        assertEquals(htmlPageAsset, pageRendered.getPage());
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

        assertEquals(htmlPageAsset, pageRendered.getPage());
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

        assertEquals(htmlPageAsset, pageRendered.getPage());
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

        assertEquals(htmlPageAsset, pageRendered.getPage());
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

        assertEquals(htmlPageAsset, pageRendered.getPage());
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
