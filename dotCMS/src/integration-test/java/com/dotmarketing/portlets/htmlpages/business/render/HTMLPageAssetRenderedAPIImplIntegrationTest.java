package com.dotmarketing.portlets.htmlpages.business.render;

import static com.dotcms.rendering.velocity.directive.ParseContainer.getDotParserContainerUUID;
import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.ThemeDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContext;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageLivePreviewVersionBean;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
        APILocator.getPermissionAPI().save(list(permission), htmlPageAsset, systemUser, false);
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

        APILocator.getPermissionAPI().save(list(permission), host, systemUser, false);
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

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: Try to render a page with {@link VariantAPI#DEFAULT_VARIANT} and a specific {@link Language}
     * and the page had a contentlet that:
     * - had version in that language and DEFAULT variant.
     * - The page just have version in that specific language.
     * Should: render the page for the  {@link VariantAPI#DEFAULT_VARIANT} and  {@link Language} {@link Contentlet} version
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void renderPageWithDefaultVariantAndLanguage() throws WebAssetException, DotDataException, DotSecurityException {
        final Language language = new LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = createContentType();
        final Container container = createAndPublishContainer(host, contentType);
        final HTMLPageAsset page = createHtmlPageAsset(language, host, container);
        final Contentlet contentlet = createContentlet(language, host, contentType);

        addToPage(container, page, contentlet);

        final HttpServletRequest mockRequest = createHttpServletRequest(language, host,
                VariantAPI.DEFAULT_VARIANT, page);

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final HttpSession session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(APILocator.systemUser())
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals("<div>" + VariantAPI.DEFAULT_VARIANT.name() + " content-default-" + language.getId() + "</div>", html);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: Try to render a page with a specific {@link Variant}} and a specific {@link Language}
     * and the page had a contentlet that:
     * - had version in another language and that variant.
     * - had version in another language and DEFAULT variant.
     * - had version in that language and another variant.
     * - The page just have version in that specific language.
     * Should: render an empty page
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void emptyPageWithMultiContentletVersion() throws WebAssetException, DotDataException, DotSecurityException {
        final Language language_1 = new LanguageDataGen().nextPersisted();
        final Language language_2 = new LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Variant variant_1 = new VariantDataGen().nextPersisted();
        final Variant variant_2 = new VariantDataGen().nextPersisted();

        final ContentType contentType = createContentType();
        final Container container = createAndPublishContainer(host, contentType);
        final HTMLPageAsset page = createHtmlPageAsset(language_2, host, container);
        final Contentlet contentlet = createContentlet(language_1, host, contentType);

        createNewVersion(contentlet, language_2, variant_2);
        createNewVersion(contentlet, language_1, variant_1);

        addToPage(container, page, contentlet);

        final HttpServletRequest mockRequest = createHttpServletRequest(language_2, host,
                variant_1, page);

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final HttpSession session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(APILocator.systemUser())
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals("<div></div>", html);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: Try to render a page with a specific {@link Variant}} and a specific {@link Language}
     * and the page had a contentlet that:
     * - had version in that language and that variant.
     * - had version in that language and DEFAULT variant.
     * - The page just have version in that specific language.
     * Should: render the page for the specific {@link Variant}} and  {@link Language} {@link Contentlet} version
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void renderPageWithSpecificVariantAndLanguage() throws WebAssetException, DotDataException, DotSecurityException {
        final Language language = new LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Variant variant = new VariantDataGen().nextPersisted();

        final ContentType contentType = createContentType();
        final Container container = createAndPublishContainer(host, contentType);
        final HTMLPageAsset page = createHtmlPageAsset(language, host, container);
        final Contentlet contentlet = createContentlet(language, host, contentType);

        createNewVersion(contentlet, language, variant);

        addToPage(container, page, contentlet);

        final HttpServletRequest mockRequest = createHttpServletRequest(language, host,
                variant, page);

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final HttpSession session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(APILocator.systemUser())
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals("<div>" + variant.name() + " content-default-" + language.getId() + "</div>", html);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: Try to render a page with a specific {@link Variant}} and a specific {@link Language}
     * and the page had a contentlet that had:
     * - had version in default language and that variant.
     * - had version in that language and DEFAULT variant.
     * - The page just have version in that specific language.
     * Should: render the page for the DEFAULT {@link Variant}} version
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void fallbackToDefaultVariantSameLanguage() throws WebAssetException, DotDataException, DotSecurityException {
        final Language language = new LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Variant variant = new VariantDataGen().nextPersisted();

        final ContentType contentType = createContentType();
        final Container container = createAndPublishContainer(host, contentType);
        final HTMLPageAsset page = createHtmlPageAsset(language, host, container);
        final Contentlet contentlet = createContentlet(language, host, contentType);

        createNewVersion(contentlet, APILocator.getLanguageAPI().getDefaultLanguage(), variant);

        addToPage(container, page, contentlet);

        final HttpServletRequest mockRequest = createHttpServletRequest(language, host,
                variant, page);

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final HttpSession session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(APILocator.systemUser())
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals("<div>DEFAULT content-default-" + language.getId() + "</div>", html);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: Try to render a page with a specific {@link Variant}} and a specific {@link Language}
     * and the page had a contentlet that had:
     * - had version in default language and that variant.
     * - had version in default language and DEFAULT variant.
     * - The page just have version in that specific language.
     * - and the language fallback is on
     * Should: render the page for the specific {@link Variant} version
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void fallbackToDefaultVariantDefaultLanguage() throws WebAssetException, DotDataException, DotSecurityException {
        final boolean defaultContentToDefaultLanguage = Config.getBooleanProperty(
                "DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false);
        Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", true);

        try {
            final Language language = new LanguageDataGen().nextPersisted();
            final Host host = new SiteDataGen().nextPersisted();
            final Variant variant = new VariantDataGen().nextPersisted();
            final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

            final ContentType contentType = createContentType();
            final Container container = createAndPublishContainer(host, contentType);
            final HTMLPageAsset page = createHtmlPageAsset(language, host, container);
            final Contentlet contentlet = createContentlet(defaultLanguage, host, contentType);

            createNewVersion(contentlet, defaultLanguage, variant);

            addToPage(container, page, contentlet);

            final HttpServletRequest mockRequest = createHttpServletRequest(language, host,
                    variant, page);

            final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
            final HttpSession session = createHttpSession(mockRequest);
            when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

            String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                    PageContextBuilder.builder()
                            .setUser(APILocator.systemUser())
                            .setPageUri(page.getURI())
                            .setPageMode(PageMode.LIVE)
                            .build(),
                    mockRequest, mockResponse);
            Assert.assertEquals(
                    "<div>" + variant.name() + " content-default-" + defaultLanguage.getId()
                            + "</div>", html);
        }finally {
            Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", defaultContentToDefaultLanguage);
        }
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: Try to render a page with a specific {@link Variant}} and a specific {@link Language}
     * and the page had a contentlet that had:
     * - had version in default language and that variant.
     * - had version in default language and DEFAULT variant.
     * - The page just have version in that specific language.
     * - and the language fallback is off
     * Should: render the page for the specific {@link Variant} version
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void fallbackOff() throws WebAssetException, DotDataException, DotSecurityException {
        final boolean defaultContentToDefaultLanguage = Config.getBooleanProperty(
                "DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false);
        Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false);

        try {
            final Language language = new LanguageDataGen().nextPersisted();
            final Host host = new SiteDataGen().nextPersisted();
            final Variant variant = new VariantDataGen().nextPersisted();
            final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

            final ContentType contentType = createContentType();
            final Container container = createAndPublishContainer(host, contentType);
            final HTMLPageAsset page = createHtmlPageAsset(language, host, container);
            final Contentlet contentlet = createContentlet(defaultLanguage, host, contentType);

            createNewVersion(contentlet, defaultLanguage, variant);

            addToPage(container, page, contentlet);

            final HttpServletRequest mockRequest = createHttpServletRequest(language, host,
                    variant, page);

            final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
            final HttpSession session = createHttpSession(mockRequest);
            when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

            String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                    PageContextBuilder.builder()
                            .setUser(APILocator.systemUser())
                            .setPageUri(page.getURI())
                            .setPageMode(PageMode.LIVE)
                            .build(),
                    mockRequest, mockResponse);
            Assert.assertEquals("<div></div>", html);
        }finally {
            Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", defaultContentToDefaultLanguage);
        }
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: Try to render a page with a specific {@link Variant}} and a specific {@link Language}
     * and the page had a contentlet that had:
     * - had version in default language and DEFAULT variant.
     * - The page just have version in that specific language.
     * - and the language fallback is on
     * Should: render the page for the DEFAULT {@link Variant} version
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void fallbackToDefaultVariantDefaultLanguageWithoutSpecificVariantVersion() throws WebAssetException, DotDataException, DotSecurityException {
        final boolean defaultContentToDefaultLanguage = Config.getBooleanProperty(
                "DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false);
        Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", true);

        try {
            final Language language = new LanguageDataGen().nextPersisted();
            final Host host = new SiteDataGen().nextPersisted();
            final Variant variant = new VariantDataGen().nextPersisted();
            final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

            final ContentType contentType = createContentType();
            final Container container = createAndPublishContainer(host, contentType);
            final HTMLPageAsset page = createHtmlPageAsset(language, host, container);
            final Contentlet contentlet = createContentlet(defaultLanguage, host, contentType);

            addToPage(container, page, contentlet);

            final HttpServletRequest mockRequest = createHttpServletRequest(language, host,
                    variant, page);

            final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
            final HttpSession session = createHttpSession(mockRequest);
            when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

            String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                    PageContextBuilder.builder()
                            .setUser(APILocator.systemUser())
                            .setPageUri(page.getURI())
                            .setPageMode(PageMode.LIVE)
                            .build(),
                    mockRequest, mockResponse);
            Assert.assertEquals(
                    "<div>DEFAULT content-default-" + defaultLanguage.getId()
                            + "</div>", html);
        }finally {
            Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", defaultContentToDefaultLanguage);
        }
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: Try to render a page with {@link VariantAPI#DEFAULT_VARIANT} and a specific {@link Language}
     * and the page had a contentlet that:
     * - had version in that language and that variant.
     * - The page has version in that specific language and variant.
     * Should: render the page for the  {@link VariantAPI#DEFAULT_VARIANT} and  {@link Language} {@link Contentlet} version
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void renderPageWithDifferentVariantsVersion() throws WebAssetException, DotDataException, DotSecurityException {
        final Language language = new LanguageDataGen().nextPersisted();
        final Variant variant = new VariantDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = createContentType();
        final Container container = createAndPublishContainer(host, contentType);
        final HTMLPageAsset page = createHtmlPageAsset(language, host, container, variant);
        final Contentlet contentlet = createContentlet(language, host, contentType);
        createNewVersion(contentlet, language, variant);

        addToPage(container, page, contentlet);

        final HttpServletRequest mockRequest = createHttpServletRequest(language, host,
                variant, page);

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final HttpSession session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(APILocator.systemUser())
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals(
                "<div>" + variant.name() + " content-default-" + language.getId()
                        + "</div>", html);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: The page has different content for different {@link Variant}
     * should: render de page with the cotentlet for the specific {@link Variant}
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void renderPageWithDifferentVariantsVersionAndContentlet() throws WebAssetException, DotDataException, DotSecurityException {
        final Language language = new LanguageDataGen().nextPersisted();
        final Variant variant = new VariantDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = createContentType();
        final Container container = createAndPublishContainer(host, contentType);
        final HTMLPageAsset page = createHtmlPageAsset(language, host, container, variant);
        final Contentlet contentlet = createContentlet(language, host, contentType);
        createNewVersion(contentlet, language, variant);

        addToPage(container, page, contentlet);

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .languageId(language.getId())
                .host(host)
                .setProperty("title", "DEFAULT second-content-default-" + language.getId())
                .variant(VariantAPI.DEFAULT_VARIANT)
                .nextPersistedAndPublish();
        createNewVersion(contentlet_2, language, variant, "title",
                variant.name() + " second-content-default-" + language.getId());

        new MultiTreeDataGen()
                .setPage(page)
                .setContentlet(contentlet_2)
                .setInstanceID(ContainerUUID.UUID_START_VALUE)
                .setTreeOrder(0)
                .setContainer(container)
                .setVariant(variant)
                .nextPersisted();

        final HttpServletRequest mockRequest = createHttpServletRequest(language, host,
                variant, page);

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final HttpSession session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);


        final String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(APILocator.systemUser())
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals(
                "<div>" + variant.name() + " second-content-default-" + language.getId()
                        + "</div>", html);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When:
     * - Create two {@link Contentlet}.
     * - Create a {@link HTMLPageAsset}.
     * - Create a new {@link Variant}
     * - Add the first contentlet into the Page for the DEFAULT {@link Variant}
     * - Add the second contentlet into the Page for the specific {@link Variant}
     * should:
     * - When the page is render to the DEFAULT variant should render the first contentlet.
     * - When the page is render to the specific variant should render the second contentlet.
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void usingLiveRenderCache() throws WebAssetException, DotDataException, DotSecurityException {
        final Language language = new LanguageDataGen().nextPersisted();
        final Variant variant = new VariantDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = createContentType();
        final Container container = createAndPublishContainer(host, contentType);
        final HTMLPageAsset page = createHtmlPageAsset(language, host, container, VariantAPI.DEFAULT_VARIANT);
        final Contentlet contentlet = createContentlet(language, host, contentType);
        createNewVersion(contentlet, language, variant);

        addToPage(container, page, contentlet);

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .languageId(language.getId())
                .host(host)
                .setProperty("title", "DEFAULT second-content-default-" + language.getId())
                .variant(VariantAPI.DEFAULT_VARIANT)
                .nextPersistedAndPublish();
        createNewVersion(contentlet_2, language, variant, "title",
                variant.name() + " second-content-default-" + language.getId());

        new MultiTreeDataGen()
                .setPage(page)
                .setContentlet(contentlet_2)
                .setInstanceID(ContainerUUID.UUID_START_VALUE)
                .setTreeOrder(0)
                .setContainer(container)
                .setVariant(variant)
                .nextPersisted();

        HttpServletRequest mockRequest = createHttpServletRequest(language, host,
                variant, page);

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        HttpSession session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);


        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(APILocator.systemUser())
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals(
                "<div>" + variant.name() + " second-content-default-" + language.getId()
                        + "</div>", html);

        mockRequest = createHttpServletRequest(language, host,
                VariantAPI.DEFAULT_VARIANT, page);

        mockResponse = mock(HttpServletResponse.class);
        session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);


        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(APILocator.systemUser())
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals(
                "<div>DEFAULT content-default-" + language.getId() + "</div>", html);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When:
     * - Create two {@link Contentlet}.
     * - Create a {@link HTMLPageAsset}, with a cacheTTL of 600.
     * - Create a new {@link Variant}
     * - Add the first contentlet into the Page for the DEFAULT {@link Variant}
     * - Add the second contentlet into the Page for the specific {@link Variant}
     * should:
     * - When the page is render to the DEFAULT variant should render the first contentlet.
     * - When the page is render to the specific variant should render the second contentlet.
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void usingCacheTTL() throws WebAssetException, DotDataException, DotSecurityException {
        final Language language = new LanguageDataGen().nextPersisted();
        final Variant variant = new VariantDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = createContentType();
        final Container container = createAndPublishContainer(host, contentType);
        final HTMLPageAsset page = createHtmlPageAsset(language, host, container, VariantAPI.DEFAULT_VARIANT, 600);
        final Contentlet contentlet = createContentlet(language, host, contentType);
        createNewVersion(contentlet, language, variant);

        addToPage(container, page, contentlet);

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .languageId(language.getId())
                .host(host)
                .setProperty("title", "DEFAULT second-content-default-" + language.getId())
                .variant(VariantAPI.DEFAULT_VARIANT)
                .nextPersistedAndPublish();
        createNewVersion(contentlet_2, language, variant, "title",
                variant.name() + " second-content-default-" + language.getId());

        new MultiTreeDataGen()
                .setPage(page)
                .setContentlet(contentlet_2)
                .setInstanceID(ContainerUUID.UUID_START_VALUE)
                .setTreeOrder(0)
                .setContainer(container)
                .setVariant(variant)
                .nextPersisted();

        HttpServletRequest mockRequest = createHttpServletRequest(language, host,
                variant, page);
        when(mockRequest.getMethod()).thenReturn("GET");

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        when(mockResponse.getStatus()).thenReturn(200);

        HttpSession session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);


        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(APILocator.systemUser())
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals(
                "<div>" + variant.name() + " second-content-default-" + language.getId()
                        + "</div>", html);

        mockRequest = createHttpServletRequest(language, host,
                VariantAPI.DEFAULT_VARIANT, page);
        when(mockRequest.getMethod()).thenReturn("GET");

        mockResponse = mock(HttpServletResponse.class);
        when(mockResponse.getStatus()).thenReturn(200);
        session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);


        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(APILocator.systemUser())
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals(
                "<div>DEFAULT content-default-" + language.getId() + "</div>", html);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: Try to render a page with a specific {@link Variant}} and a specific {@link Language}
     * and the page had a widget that:
     * - had version in that language and that variant.
     * - had version in that language and DEFAULT variant.
     * - The page just have version in that specific language.
     * Should: render the page for the specific {@link Variant}} and  {@link Language} {@link Contentlet} version
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void renderWidgetWithSpecificVariantAndLanguage() throws WebAssetException, DotDataException, DotSecurityException {
        final Language language = new LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Variant variant = new VariantDataGen().nextPersisted();

        ContentType contentType = ContentTypeDataGen.createWidgetContentType("$widgetTitle")
                .host(host)
                .nextPersisted();

        final Container container = createAndPublishContainer(host, contentType);
        final HTMLPageAsset page = createHtmlPageAsset(language, host, container);
        final Contentlet contentlet = createContentlet(language, host, contentType, "widgetTitle");

        createNewVersion(contentlet, language, variant, "widgetTitle");

        addToPage(container, page, contentlet);

        final HttpServletRequest mockRequest = createHttpServletRequest(language, host,
                variant, page);

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final HttpSession session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(APILocator.systemUser())
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals("<div>" + variant.name() + " content-default-" + language.getId() + "</div>", html);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: Try to render a page with a specific {@link Variant}} and a specific {@link Language}
     * and the page had a widget that:
     * - had version in that language and DEFAULT variant.
     * - The page just have version in that specific language.
     * Should: render the page for the specific {@link Variant}} and  {@link Language} {@link Contentlet} version
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void renderWidgetWithDefaultVariantAndLanguage() throws WebAssetException, DotDataException, DotSecurityException {
        final Language language = new LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Variant variant = new VariantDataGen().nextPersisted();

        ContentType contentType = ContentTypeDataGen.createWidgetContentType("$widgetTitle")
                .host(host)
                .nextPersisted();

        final Container container = createAndPublishContainer(host, contentType);
        final HTMLPageAsset page = createHtmlPageAsset(language, host, container);
        final Contentlet contentlet = createContentlet(language, host, contentType, "widgetTitle");

        addToPage(container, page, contentlet);

        final HttpServletRequest mockRequest = createHttpServletRequest(language, host,
                variant, page);

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final HttpSession session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(APILocator.systemUser())
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals("<div>DEFAULT content-default-" + language.getId() + "</div>", html);
    }

    private HttpServletRequest createHttpServletRequest(Language language, Host host, Variant variant,
            HTMLPageAsset page) throws DotDataException {
        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameter("host_id")).thenReturn(host.getIdentifier());
        when(mockRequest.getParameter(WebKeys.HTMLPAGE_LANGUAGE))
                .thenReturn(String.valueOf(language.getId()));
        when(mockRequest.getParameter(VariantAPI.VARIANT_KEY))
                .thenReturn(variant.name());
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        when(mockRequest.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);
        when(mockRequest.getRequestURI()).thenReturn(page.getURI());
        when(mockRequest.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(APILocator.getUserAPI().getAnonymousUserNoThrow());
        return mockRequest;
    }

    private HTMLPageAsset createHtmlPageAsset(final Language language, final Host host, final Container container)
            throws WebAssetException, DotSecurityException, DotDataException {

        return createHtmlPageAsset(language, host, container, VariantAPI.DEFAULT_VARIANT);
    }

    private HTMLPageAsset createHtmlPageAsset(
            final Language language,
            final Host host,
            final Container container,
            final Variant variant)

            throws WebAssetException, DotSecurityException, DotDataException {

        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final Template template = createTemplate(host, container);

        return createHtmlPageAsset(language, folder, template, variant, 0);
    }


    private HTMLPageAsset createHtmlPageAsset(final Language language, final Host host,
            final Container container, final Variant variant, final int cacheTTL)
            throws WebAssetException, DotSecurityException, DotDataException {

        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final Template template = createTemplate(host, container);

        return createHtmlPageAsset(language, folder, template, variant, cacheTTL);
    }

    private HTMLPageAsset createHtmlPageAssetWithHead(
            final Language language,
            final Host host,
            final Container container,
            final String bodyHead)

            throws WebAssetException, DotSecurityException, DotDataException {

        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final Template template = new TemplateDataGen()
                .host(host)
                .withContainer(container.getIdentifier(), "1")
                .addBodyHead(bodyHead)
                .nextPersisted();


        PublishFactory.publishAsset(template, APILocator.systemUser(), false, false);

        return createHtmlPageAsset(language, folder, template, VariantAPI.DEFAULT_VARIANT, 0);
    }

    private void addToPage(Container container, HTMLPageAsset page, Contentlet contentlet) {
         new MultiTreeDataGen()
            .setPage(page)
            .setContentlet(contentlet)
            .setInstanceID(ContainerUUID.UUID_START_VALUE)
            .setTreeOrder(0)
            .setContainer(container)
            .nextPersisted();
    }

    private Contentlet createContentlet(final Language language, final Host host, final ContentType contentType)
            throws DotDataException, DotSecurityException {
        return createContentlet(language, host, contentType, "title");
    }

    private Contentlet createContentlet(final Language language, final Host host, final ContentType contentType, final String fieldName)
            throws DotDataException, DotSecurityException {

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .languageId(language.getId())
                .host(host)
                .setProperty(fieldName, "DEFAULT content-default-" + language.getId())
                .nextPersisted();

        APILocator.getContentletAPI().publish(contentlet, APILocator.systemUser(), false);
        return contentlet;
    }

    private Contentlet createNewVersion(final Contentlet contentlet, final Language language,
            final Variant variant) {
        return createNewVersion(contentlet, language, variant, "title" );
    }

    private Contentlet createNewVersion(final Contentlet contentlet, final Language language, final Variant variant,
            final String fieldName) {
        return createNewVersion(contentlet, language, variant, fieldName, variant.name() + " content-default-" + language.getId());
    }

    private Contentlet createNewVersion(final Contentlet contentlet, final Language language,
        final Variant variant, final String fieldName, final String fieldContent) {

        final Contentlet checkout = ContentletDataGen.checkout(contentlet);
        checkout.setVariantId(variant.name());
        checkout.setProperty(fieldName, fieldContent);
        checkout.setLanguageId(language.getId());

        final Contentlet checkin = ContentletDataGen.checkin(checkout);
        ContentletDataGen.publish(checkin);

        try {
            addAnonymousPermissions(checkin);
        } catch (DotDataException | DotSecurityException  e) {
            throw new RuntimeException(e);
        }

        return checkin;
    }

    private HTMLPageAsset createHtmlPageAsset(
            final Language language, final Folder folder,final  Template template, final Variant variant, final int cacheTTL)
            throws DotSecurityException, DotDataException {
        final String pageName = "variant-render-test-" + System.currentTimeMillis();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(language.getId())
                .pageURL(pageName)
                .title(pageName)
                .cacheTTL(cacheTTL)
                .next();

        page.setVariantId(variant.name());
        APILocator.getContentletAPI().checkin(page, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(page, APILocator.systemUser(), false);
        return page;
    }

    private Template createTemplate(final Host host, final Container container)
            throws WebAssetException, DotSecurityException, DotDataException {

        final Template template = new TemplateDataGen()
                .host(host)
                .withContainer(container.getIdentifier(), "1")
                .nextPersisted();

        PublishFactory.publishAsset(template, APILocator.systemUser(), false, false);
        return template;
    }

    private Container createAndPublishContainer(final Host host, final ContentType contentType)
            throws WebAssetException, DotSecurityException, DotDataException {
        Container container = new ContainerDataGen()
                .site(host)
                .nextPersisted();
        PublishFactory.publishAsset(container, APILocator.systemUser(), false, false);

        final ContainerStructure containerStructure = new ContainerStructure();
        containerStructure.setStructureId(contentType.id());
        containerStructure.setCode("$!{title}");

        container = APILocator.getContainerAPI().save(container,
                list(containerStructure), host, APILocator.systemUser(), false);
        PublishFactory.publishAsset(container, APILocator.systemUser(), false, false);

        return container;
    }

    private ContentType createContentType() {
        final Field title = new FieldDataGen().velocityVarName("title").next();

        return new ContentTypeDataGen()
                .field(title)
                .nextPersisted();
    }

    private HttpSession createHttpSession(final HttpServletRequest mockRequest) {
        final HttpSession session = mock(HttpSession.class);
        when(mockRequest.getSession()).thenReturn(session);
        when(mockRequest.getSession(false)).thenReturn(session);
        when(mockRequest.getSession(true)).thenReturn(session);
        return session;
    }


    private static void addAnonymousPermissions(final Contentlet contentlet)
            throws DotDataException, DotSecurityException {

        //Assign permissions
        APILocator.getPermissionAPI().save(
                new Permission(contentlet.getPermissionId(),
                        APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                        PermissionAPI.PERMISSION_READ),
                contentlet, APILocator.systemUser(), false);
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: You have at least one {@link Experiment} RUNNING and try to render a page and the page had a HEAD section
     * Should inject the JS Code need for Experiment to work into the head tag
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void injectJSCodeWithHead() throws WebAssetException, DotDataException, DotSecurityException {
        final Experiment experiment = new ExperimentDataGen().nextPersisted();

        try {
            ExperimentDataGen.start(experiment);

            final Language language = new LanguageDataGen().nextPersisted();
            final Host host = new SiteDataGen().nextPersisted();

            final ContentType contentType = createContentType();
            final Container container = createAndPublishContainer(host, contentType);
            final HTMLPageAsset page = createHtmlPageAssetWithHead(language, host, container,
                    "<head><title>This is a testing</title></head>");
            final Contentlet contentlet = createContentlet(language, host, contentType);

            addToPage(container, page, contentlet);

            final HttpServletRequest mockRequest = createHttpServletRequest(language, host,
                    VariantAPI.DEFAULT_VARIANT, page);

            final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
            final HttpSession session = createHttpSession(mockRequest);
            when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

            String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                    PageContextBuilder.builder()
                            .setUser(APILocator.systemUser())
                            .setPageUri(page.getURI())
                            .setPageMode(PageMode.LIVE)
                            .build(),
                    mockRequest, mockResponse);
            final String expectedContentRender =
                    "<div>DEFAULT content-default-" + language.getId() + "</div>";
            final String expectedHead = "<head>"
                    + "<script src=\"/s/lib.js\" data-key=\"\"\n"
                    + "        data-init-only=\"false\"\n"
                    + "        defer>\n"
                    + "</script>\n"
                    + "\n"
                    + "<script>window.jitsu = window.jitsu || (function(){(window.jitsuQ = window.jitsuQ || []).push(arguments);})</script>\n"
                    + "<SCRIPT>\n"
                    + "let currentRunningExperimentsId = ['" + experiment.id().get() + "']\n"
                    + "\n"
                    + "function shouldHitEndPoint() {\n"
                    + "    let experimentData = localStorage.getItem('experiment_data');\n"
                    + "\n"
                    + "    if (experimentData) {\n"
                    + "        let includedExperimentIds = JSON.parse(\n"
                    + "            experimentData).includedExperimentIds;\n"
                    + "\n"
                    + "        return !currentRunningExperimentsId.every(element => includedExperimentIds.includes(element));\n"
                    + "    } else {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n"
                    + "\n"
                    + "window.addEventListener(\"experiment_data_loaded\", function (event) {\n"
                    + "\n"
                    + "    let experimentData = event.detail;\n"
                    + "    console.log('experiment_data', experimentData);\n"
                    + "    for (let i = 0; i < experimentData.experiments.length; i++){\n"
                    + "        let pageUrl = experimentData.experiments[i].pageUrl;\n"
                    + "\n"
                    + "        let alternativePageUrl = experimentData.experiments[i].pageUrl.endsWith(\"/index\") ?\n"
                    + "            experimentData.experiments[i].pageUrl.replace(\"/index\", \"\") :\n"
                    + "            experimentData.experiments[i].pageUrl;\n"
                    + "\n"
                    + "        if (window.location.href.includes(pageUrl) || window.location.href.includes(alternativePageUrl)) {\n"
                    + "\n"
                    + "            let url = experimentData.experiments[i].variant.url\n"
                    + "            const param = (url.includes(\"?\") ? \"&\" : \"?\") + \"redirect=true\";\n"
                    + "            window.location.href = url + param;\n"
                    + "            break;\n"
                    + "        }\n"
                    + "    }\n"
                    + "});\n\n"
                    + "if (shouldHitEndPoint()) {\n"
                    + "    let experimentData = localStorage.getItem('experiment_data');\n"
                    + "    let body = experimentData ?\n"
                    + "        {\n"
                    + "            exclude: JSON.parse(experimentData).includedExperimentIds\n"
                    + "        } : {\n"
                    + "            exclude: []\n"
                    + "        };\n"
                    + "\n"
                    + "    fetch('/api/v1/experiments/isUserIncluded', {\n"
                    + "        method: 'POST',\n"
                    + "        body: JSON.stringify(body),\n"
                    + "        headers: {\n"
                    + "            'Accept': 'application/json',\n"
                    + "            'Content-Type': 'application/json'\n"
                    + "        }\n"
                    + "    })\n"
                    + "    .then(response => response.json())\n"
                    + "    .then(data => {\n"
                    + "        if (data.entity.experiments) {\n"
                    + "            let dataToStorage = Object.assign({}, data.entity);\n"
                    + "            let oldExperimentData = JSON.parse(localStorage.getItem('experiment_data'));\n"
                    + "\n"
                    + "            delete dataToStorage['excludedExperimentIds'];\n"
                    + "\n"
                    + "            dataToStorage.includedExperimentIds = [\n"
                    + "                ...dataToStorage.includedExperimentIds,\n"
                    + "                ...data.entity.excludedExperimentIds\n"
                    + "            ];\n"
                    + "\n"
                    + "            if (oldExperimentData) {\n"
                    + "                dataToStorage.experiments = [\n"
                    + "                    ...oldExperimentData.experiments,\n"
                    + "                    ...dataToStorage.experiments\n"
                    + "                ];\n"
                    + "            }\n"
                    + "\n"
                    + "            localStorage.setItem('experiment_data', JSON.stringify(dataToStorage));\n\n"
                    + "            const event = new CustomEvent('experiment_data_loaded', { detail: dataToStorage });\n"
                    + "            window.dispatchEvent(event);\n"
                    + "        }\n"
                    + "    });\n"
                    + "} else if (!window.location.href.includes(\"redirect=true\")) {\n"
                    + "    let experimentData = JSON.parse(localStorage.getItem('experiment_data'));\n"
                    + "\n"
                    + "    const event = new CustomEvent('experiment_data_loaded', { detail: experimentData });\n"
                    + "    window.dispatchEvent(event);\n"
                    + "}\n\n"
                    + "</SCRIPT>"
                    + "<title>This is a testing</title>"
                    + "</head>";
            final String expectedCode = expectedHead + expectedContentRender;

            Assert.assertEquals(expectedCode, html);
        } finally {
            ExperimentDataGen.end(experiment);
        }
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: You have at least one {@link Experiment} RUNNING and try to render a page and the page does not had a HEAD section
     * Should inject the JS Code need for Experiment to work on the top of the HTML code
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void injectJSCodeWithoutHead() throws WebAssetException, DotDataException, DotSecurityException {
        final Experiment experiment = new ExperimentDataGen().nextPersisted();

        try {
            ExperimentDataGen.start(experiment);

            final Language language = new LanguageDataGen().nextPersisted();
            final Host host = new SiteDataGen().nextPersisted();

            final ContentType contentType = createContentType();
            final Container container = createAndPublishContainer(host, contentType);
            final HTMLPageAsset page = createHtmlPageAsset(language, host, container);
            final Contentlet contentlet = createContentlet(language, host, contentType);

            addToPage(container, page, contentlet);

            final HttpServletRequest mockRequest = createHttpServletRequest(language, host,
                    VariantAPI.DEFAULT_VARIANT, page);

            final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
            final HttpSession session = createHttpSession(mockRequest);
            when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

            String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                    PageContextBuilder.builder()
                            .setUser(APILocator.systemUser())
                            .setPageUri(page.getURI())
                            .setPageMode(PageMode.LIVE)
                            .build(),
                    mockRequest, mockResponse);
            final String expectedContentRender =
                    "<div>DEFAULT content-default-" + language.getId() + "</div>";
            final String expectedHead = "<script src=\"/s/lib.js\" data-key=\"\"\n"
                    + "        data-init-only=\"false\"\n"
                    + "        defer>\n"
                    + "</script>\n"
                    + "\n"
                    + "<script>window.jitsu = window.jitsu || (function(){(window.jitsuQ = window.jitsuQ || []).push(arguments);})</script>\n"
                    + "<SCRIPT>\n"
                    + "let currentRunningExperimentsId = ['" + experiment.id().get() + "']\n"
                    + "\n"
                    + "function shouldHitEndPoint() {\n"
                    + "    let experimentData = localStorage.getItem('experiment_data');\n"
                    + "\n"
                    + "    if (experimentData) {\n"
                    + "        let includedExperimentIds = JSON.parse(\n"
                    + "            experimentData).includedExperimentIds;\n"
                    + "\n"
                    + "        return !currentRunningExperimentsId.every(element => includedExperimentIds.includes(element));\n"
                    + "    } else {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n"
                    + "\n"
                    + "window.addEventListener(\"experiment_data_loaded\", function (event) {\n"
                    + "\n"
                    + "    let experimentData = event.detail;\n"
                    + "    console.log('experiment_data', experimentData);\n"
                    + "    for (let i = 0; i < experimentData.experiments.length; i++){\n"
                    + "        let pageUrl = experimentData.experiments[i].pageUrl;\n"
                    + "\n"
                    + "        let alternativePageUrl = experimentData.experiments[i].pageUrl.endsWith(\"/index\") ?\n"
                    + "            experimentData.experiments[i].pageUrl.replace(\"/index\", \"\") :\n"
                    + "            experimentData.experiments[i].pageUrl;\n"
                    + "\n"
                    + "        if (window.location.href.includes(pageUrl) || window.location.href.includes(alternativePageUrl)) {\n"
                    + "\n"
                    + "            let url = experimentData.experiments[i].variant.url\n"
                    + "            const param = (url.includes(\"?\") ? \"&\" : \"?\") + \"redirect=true\";\n"
                    + "            window.location.href = url + param;\n"
                    + "            break;\n"
                    + "        }\n"
                    + "    }\n"
                    + "});\n\n"
                    + "if (shouldHitEndPoint()) {\n"
                    + "    let experimentData = localStorage.getItem('experiment_data');\n"
                    + "    let body = experimentData ?\n"
                    + "        {\n"
                    + "            exclude: JSON.parse(experimentData).includedExperimentIds\n"
                    + "        } : {\n"
                    + "            exclude: []\n"
                    + "        };\n"
                    + "\n"
                    + "    fetch('/api/v1/experiments/isUserIncluded', {\n"
                    + "        method: 'POST',\n"
                    + "        body: JSON.stringify(body),\n"
                    + "        headers: {\n"
                    + "            'Accept': 'application/json',\n"
                    + "            'Content-Type': 'application/json'\n"
                    + "        }\n"
                    + "    })\n"
                    + "    .then(response => response.json())\n"
                    + "    .then(data => {\n"
                    + "        if (data.entity.experiments) {\n"
                    + "            let dataToStorage = Object.assign({}, data.entity);\n"
                    + "            let oldExperimentData = JSON.parse(localStorage.getItem('experiment_data'));\n"
                    + "\n"
                    + "            delete dataToStorage['excludedExperimentIds'];\n"
                    + "\n"
                    + "            dataToStorage.includedExperimentIds = [\n"
                    + "                ...dataToStorage.includedExperimentIds,\n"
                    + "                ...data.entity.excludedExperimentIds\n"
                    + "            ];\n"
                    + "\n"
                    + "            if (oldExperimentData) {\n"
                    + "                dataToStorage.experiments = [\n"
                    + "                    ...oldExperimentData.experiments,\n"
                    + "                    ...dataToStorage.experiments\n"
                    + "                ];\n"
                    + "            }\n"
                    + "\n"
                    + "            localStorage.setItem('experiment_data', JSON.stringify(dataToStorage));\n\n"
                    + "            const event = new CustomEvent('experiment_data_loaded', { detail: dataToStorage });\n"
                    + "            window.dispatchEvent(event);\n"
                    + "        }\n"
                    + "    });\n"
                    + "} else if (!window.location.href.includes(\"redirect=true\")) {\n"
                    + "    let experimentData = JSON.parse(localStorage.getItem('experiment_data'));\n"
                    + "\n"
                    + "    const event = new CustomEvent('experiment_data_loaded', { detail: experimentData });\n"
                    + "    window.dispatchEvent(event);\n"
                    + "}\n\n"
                    + "</SCRIPT>\n";
            final String expectedCode = expectedHead + expectedContentRender;

            Assert.assertEquals(expectedCode, html);
        }  finally {
            ExperimentDataGen.end(experiment);
        }
    }
}
