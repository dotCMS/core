package com.dotcms.rendering.velocity.servlet;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rendering.velocity.viewtools.VelocityRequestWrapper;
import com.dotcms.util.FiltersUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.vanityurl.filters.VanityURLFilter;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.filters.TimeMachineFilter;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContext;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.LoginMode;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dotcms.datagen.TestDataUtils.getNewsLikeContentType;
import static com.dotmarketing.util.WebKeys.LOGIN_MODE_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VelocityServletIntegrationTest {

    public static final String TEST_PATTERN = "/testpattern";
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Host host;
    private ServletOutputStream servletOutputStream;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void init() {
        host = new SiteDataGen().nextPersisted();

        final Map<String, Object> attributes = new HashMap<>();

        request = mock(HttpServletRequest.class);
        // Mock setAttribute
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final String key = invocation.getArgument(0, String.class);
                final Object value = invocation.getArgument(1, Object.class);
                attributes.put(key, value);
                return null;
            }
        }).when(request).setAttribute(anyString(), any());

        // Mock getAttribute
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final String key = invocation.getArgument(0, String.class);
                return attributes.get(key);
            }
        }).when(request).getAttribute(anyString());

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        when(request.getParameter("host_id")).thenReturn(host.getIdentifier());

        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(WebKeys.PAGE_MODE_SESSION)).thenReturn(PageMode.LIVE);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        final Clickstream clickstream = mock(Clickstream.class);
        when(session.getAttribute("clickstream")).thenReturn(clickstream);

        response = mock(HttpServletResponse.class);
        servletOutputStream = mock(ServletOutputStream.class);
        try {
            when(response.getOutputStream()).thenReturn(servletOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method to test: {@link VelocityServlet#service(HttpServletRequest, HttpServletResponse)}
     * when: There is exists a Content Type Url Map and is request Should: return 200 Http code
     *
     * @throws ServletException
     * @throws IOException
     */
    @Test
    public void whenRequestURLMap() throws ServletException, IOException {

        final VelocityServlet velocityServlet = new VelocityServlet();

        final String newsPatternPrefix =
                TEST_PATTERN + System.currentTimeMillis() + "/";
        final Contentlet contentlet = createURLMapperContentType(newsPatternPrefix, host);
        ContentletDataGen.publish(contentlet);

        final String contentletURLMap =
                newsPatternPrefix + contentlet.getStringProperty("urlTitle");

        when(request.getRequestURI()).thenReturn(contentletURLMap);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);

        velocityServlet.service(request, response);

        verify(response, never()).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Method to test: {@link VelocityServlet#service(HttpServletRequest, HttpServletResponse)}
     * when: There is exists a Content Type Url Map and is request Should: return 200 Http code
     *
     * @throws ServletException
     * @throws IOException
     */
    @Test
    public void whenRequestVanityURL()
            throws ServletException, IOException, DotDataException, DotSecurityException {

        final VelocityServlet velocityServlet = new VelocityServlet();

        final Template template = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        ContentletDataGen.publish(htmlPageAsset);

        when(request.getRequestURI()).thenReturn("/vanityURL");

        final String VANITY_URI = "/vanityURL";
        createAndPublishVanityURL(htmlPageAsset.getPageUrl(), VANITY_URI);

        when(request.getRequestURI()).thenReturn(VANITY_URI);
        final FilterChain chain = mock(FilterChain.class);

        final VanityURLFilter vanityURLFilter = new VanityURLFilter();
        vanityURLFilter.doFilter(request, response, chain);

        velocityServlet.service(request, response);

        verify(response, never()).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void createAndPublishVanityURL(final String forwardURL, final String VANITY_URI)
            throws DotDataException, DotSecurityException {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final Contentlet vanityUrl = FiltersUtil.getInstance()
                .createVanityUrl("test", host, VANITY_URI,
                        forwardURL, 200, 0, defaultLanguage.getId());

        FiltersUtil.getInstance().publishVanityUrl(vanityUrl);
    }

    /**
     * Method to test: {@link VelocityServlet#service(HttpServletRequest, HttpServletResponse)}
     * when: There is exists a VanityURL that forward to a URL Map Should: return 200 Http code
     *
     * @throws ServletException
     * @throws IOException
     */
    @Test
    public void whenRequestURLMapAndVanityURLTogether()
            throws ServletException, IOException, DotSecurityException, DotDataException {

        final VelocityServlet velocityServlet = new VelocityServlet();

        final String newsPatternPrefix =
                TEST_PATTERN + System.currentTimeMillis() + "/";
        final Contentlet contentlet = createURLMapperContentType(newsPatternPrefix, host);
        ContentletDataGen.publish(contentlet);

        final String uri = "/vanityURL/" + contentlet.getStringProperty("urlTitle");

        when(request.getRequestURI()).thenReturn(uri);

        final String VANITY_URI = "/vanityURL/([a-zA-Z0-9-_]+)";
        final String FORWARD_URL = newsPatternPrefix + "$1";

        createAndPublishVanityURL(FORWARD_URL, VANITY_URI);

        when(request.getRequestURI()).thenReturn(
                "/vanityURL/" + contentlet.getStringProperty("urlTitle"));
        final FilterChain chain = Mockito.mock(FilterChain.class);

        final VanityURLFilter vanityURLFilter = new VanityURLFilter();
        vanityURLFilter.doFilter(request, response, chain);

        assert (request.getAttribute(com.dotmarketing.filters.Constants.CMS_FILTER_URI_OVERRIDE)
                != null);
        final String vanity = (String) request.getAttribute(
                com.dotmarketing.filters.Constants.CMS_FILTER_URI_OVERRIDE);

        assertEquals("vanity rewritten", vanity,
                FORWARD_URL.replace("$1", contentlet.getStringProperty("urlTitle")));
        velocityServlet.service(request, response);

        verify(response, never()).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private Contentlet createURLMapperContentType(final String newsPatternPrefix, final Host host) {
        final String urlMapPattern = newsPatternPrefix + "{urlTitle}";
        final HTMLPageAsset page = createPage();

        final ContentType newsContentType = getNewsLikeContentType(
                "News" + System.currentTimeMillis(),
                host,
                page.getIdentifier(),
                urlMapPattern);

        return TestDataUtils
                .getNewsContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        newsContentType.id(), host);
    }


    private HTMLPageAsset createPage() {

        final Folder folder = new FolderDataGen().site(host)
                .nextPersisted();

        final Template template = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(folder, template)
                .pageURL("news-detail")
                .title("news-detail")
                .nextPersisted();
        ContentletDataGen.publish(htmlPageAsset);
        return htmlPageAsset;
    }

    /**
     * Method to test: {@link VelocityServlet#service(HttpServletRequest, HttpServletResponse)}
     * <p>
     * When: TimeMachine in running and request a existing Page Should: Return the page
     */
    @Test
    public void testingTimeMachine() throws Exception {

        final User systemUser = APILocator.systemUser();
        final ContentType contentGenericType = APILocator.getContentTypeAPI(systemUser)
                .find("webPageContent");

        Container container = new ContainerDataGen().site(host).nextPersisted();
        final Template template = new TemplateDataGen().site(host)
                .withContainer(container.getIdentifier()).nextPersisted();

        final List<ContainerStructure> csList = new ArrayList<>();
        final ContainerStructure containerStructure = new ContainerStructure();
        containerStructure.setStructureId(contentGenericType.id());
        containerStructure.setCode("$!{body}");
        csList.add(containerStructure);

        container = APILocator.getContainerAPI().save(container, csList, host, systemUser, false);
        PublishFactory.publishAsset(container, systemUser, false, false);

        final boolean defaultContentToDefaultLanguage = Config.getBooleanProperty(
                "DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false);
        final boolean defaultPageToDefaultLanguage = Config.getBooleanProperty(
                "DEFAULT_PAGE_TO_DEFAULT_LANGUAGE", true);

        Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false);
        Config.setProperty("DEFAULT_PAGE_TO_DEFAULT_LANGUAGE", false);

        try {
            final Contentlet page = new HTMLPageDataGen(host, template).host(host).languageId(1)
                    .nextPersisted();

            final Contentlet contentlet = new ContentletDataGen(contentGenericType.id())
                    .languageId(1)
                    .host(host)
                    .setProperty("title", "content1")
                    .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                    .nextPersisted();

            ContentletDataGen.publish(contentlet);
            ContentletDataGen.publish(page);

            new MultiTreeDataGen()
                    .setContainer(container)
                    .setPage((HTMLPageAsset) page)
                    .setContentlet(contentlet)
                    .nextPersisted();

            final HttpServletRequest mockRequest = new MockSessionRequest(
                    new MockAttributeRequest(new MockHttpRequestIntegrationTest(host.getName(),
                            ((HTMLPageAsset) page).getURI()).request()).request()
            )
                    .request();

            when(mockRequest.getParameter("host_id")).thenReturn(host.getIdentifier());
            mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
            mockRequest.getSession().setAttribute("tm_host", host);
            mockRequest.getSession().setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");

            final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

            final ServletOutputStream outputStream = mock(ServletOutputStream.class);
            when(mockResponse.getOutputStream()).thenReturn(outputStream);

            final Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DATE, 1);

            //setting Time Machine
            mockRequest.getSession()
                    .setAttribute("tm_date", Long.toString(tomorrow.getTime().getTime()));
            mockRequest.getSession().setAttribute("tm_lang", "1");

            mockRequest.setAttribute(com.liferay.portal.util.WebKeys.USER, systemUser);
            mockRequest.getSession().setAttribute(WebKeys.PAGE_MODE_SESSION, PageMode.EDIT_MODE);

            final FilterChain chain = mock(FilterChain.class);
            final TimeMachineFilter timeMachineFilter = new TimeMachineFilter();
            timeMachineFilter.doFilter(mockRequest, mockResponse, chain);

            final VelocityServlet velocityServlet = new VelocityServlet();
            velocityServlet.service(mockRequest, mockResponse);

            verify(mockResponse, never()).sendError(anyInt());
            final String pageContent = "<div>" + TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT + "</div>";
            verify(outputStream).write(pageContent.getBytes());
        } finally {
            Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE",
                    defaultContentToDefaultLanguage);
            Config.setProperty("DEFAULT_PAGE_TO_DEFAULT_LANGUAGE", defaultPageToDefaultLanguage);
        }
    }

    /**
     * Here were testing that a logged in user with FE and BE roles will But login in the Front end
     * will get the Page Per SE and not the Edit Mode More context here:
     * https://github.com/dotcms/core/issues/22124
     *
     * @throws Exception
     */
    @Test
    public void Test_Frontend_Login_Overrides_BackendLogin_When_Page_Requested() throws Exception {

        final User loginUser = mock(User.class);
        when(loginUser.hasConsoleAccess()).thenReturn(true);
        when(loginUser.isAnonymousUser()).thenReturn(false);
        when(loginUser.isBackendUser()).thenReturn(true);
        when(loginUser.isFrontendUser()).thenReturn(true);
        when(loginUser.isActive()).thenReturn(true);

        testServerPageFor(loginUser, LoginMode.FE);
        testServerPageFor(loginUser, LoginMode.BE);
        testServerPageFor(loginUser, LoginMode.UNKNOWN);

    }

    /**
     * Method to test: {@link VelocityServlet#service(HttpServletRequest, HttpServletResponse)}
     * When: A backend user is logged in and request and the referer is empty
     * Should: Return the page content in {@link PageMode#LIVE}
     * @throws Exception
     */
    @Test
    public void backendUserRefererEmpty() throws Exception {
        final User loginUser = mock(User.class);
        when(loginUser.hasConsoleAccess()).thenReturn(false);
        when(loginUser.isAnonymousUser()).thenReturn(false);
        when(loginUser.isBackendUser()).thenReturn(true);
        when(loginUser.isFrontendUser()).thenReturn(false);
        when(loginUser.isActive()).thenReturn(true);

        final HttpServletRequest mockRequest = createMockRequest(null, loginUser, LoginMode.BE);

        final ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outputStream);

        final HTMLPageAssetRenderedAPI pageAssetRenderedAPI = createHtmlPageAssetRenderedAPIMock(
                loginUser, "<html>lol</html>", PageMode.PREVIEW_MODE);

        final VelocityServlet velocityServlet = new VelocityServlet(WebAPILocator.getUserWebAPI(),
                pageAssetRenderedAPI);
        velocityServlet.service(mockRequest, response);

        verifyPageServed("<html>lol</html>", outputStream);
    }

    private static HTMLPageAssetRenderedAPI createHtmlPageAssetRenderedAPIMock(final User loginUser,
            final String htmlContent, final PageMode pageMode) throws DotSecurityException, DotDataException {
        final HTMLPageAssetRenderedAPI pageAssetRenderedAPI = mock(
                HTMLPageAssetRenderedAPIImpl.class);

        final PageContext pageContext = PageContextBuilder.builder()
                .setPageUri("/lol")
                .setPageMode(pageMode)
                .setUser(loginUser)
                .build();

        when(pageAssetRenderedAPI.getPageHtml(eq(pageContext),
                any(HttpServletRequest.class),
                any(HttpServletResponse.class)))
                .thenReturn(htmlContent);

        return pageAssetRenderedAPI;
    }

    /**
     * Method to test: {@link VelocityServlet#service(HttpServletRequest, HttpServletResponse)}
     * When: A backend user is logged in and request and the referer is not from dotAdmin
     * Should: Return the page content in {@link PageMode#LIVE}
     * @throws Exception
     */
    @Test
    public void backendUserRefererNotDotAdmin() throws Exception {

        final User loginUser = mock(User.class);
        when(loginUser.hasConsoleAccess()).thenReturn(false);
        when(loginUser.isAnonymousUser()).thenReturn(false);
        when(loginUser.isBackendUser()).thenReturn(true);
        when(loginUser.isFrontendUser()).thenReturn(false);
        when(loginUser.isActive()).thenReturn(true);

        final HttpServletRequest mockRequest = createMockRequest("/blog/index", loginUser, LoginMode.BE);

        final ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outputStream);

        final HTMLPageAssetRenderedAPI pageAssetRenderedAPI = createHtmlPageAssetRenderedAPIMock(
                loginUser, "<html>lol</html>", PageMode.PREVIEW_MODE);

        final VelocityServlet velocityServlet = new VelocityServlet(WebAPILocator.getUserWebAPI(),
                pageAssetRenderedAPI);
        velocityServlet.service(mockRequest, response);

        verifyPageServed("<html>lol</html>", outputStream);
    }

    /**
     * Method to test: {@link VelocityServlet#service(HttpServletRequest, HttpServletResponse)}
     * When: A backend user is logged in and request and the referer is from dotAdmin
     * Should: Return the page content in {@link PageMode#NAVIGATE_EDIT_MODE}
     * @throws Exception
     */
    @Test
    public void backendUserRefererDotAdmin() throws Exception {

        final User loginUser = mock(User.class);
        when(loginUser.hasConsoleAccess()).thenReturn(false);
        when(loginUser.isAnonymousUser()).thenReturn(false);
        when(loginUser.isBackendUser()).thenReturn(true);
        when(loginUser.isFrontendUser()).thenReturn(false);
        when(loginUser.isActive()).thenReturn(true);

        final String responseBody = "<script type=\"text/javascript\">\n" +
                "var customEvent = window.top.document.createEvent('CustomEvent');\n" +
                "customEvent.initCustomEvent('ng-event', false, false,  {\n" +
                "            name: 'load-edit-mode-page',\n" +
                "            data: 'lol'" +
                "});\n" +
                "window.top.document.dispatchEvent(customEvent);" +
                "</script>";

        final HttpServletRequest mockRequest = createMockRequest("/dotAdmin/blog/index", loginUser, LoginMode.BE);

        final ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outputStream);

        final HTMLPageAssetRenderedAPI pageAssetRenderedAPI = createHtmlPageAssetRenderedAPIMock(
                loginUser, responseBody, PageMode.NAVIGATE_EDIT_MODE);

        final VelocityServlet velocityServlet = new VelocityServlet(WebAPILocator.getUserWebAPI(),
                pageAssetRenderedAPI);
        velocityServlet.service(mockRequest, response);

        verifyPageServed(responseBody, outputStream);
    }

    /**
     * Method to test: {@link VelocityServlet#service(HttpServletRequest, HttpServletResponse)}
     * When: A backend user is logged in and request and the referer is empty but the disabledNavigateMode is set
     * Should: Return the page content in {@link PageMode#LIVE}
     * @throws Exception
     */
    @Test
    public void backendUserDisabledNavigateModeSet() throws Exception {
        final User loginUser = mock(User.class);
        when(loginUser.hasConsoleAccess()).thenReturn(false);
        when(loginUser.isAnonymousUser()).thenReturn(false);
        when(loginUser.isBackendUser()).thenReturn(true);
        when(loginUser.isFrontendUser()).thenReturn(false);
        when(loginUser.isActive()).thenReturn(true);

        final HttpServletRequest mockRequest = createMockRequest("/dotAdmin/blog/index",
                loginUser, LoginMode.BE, true);

        final ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outputStream);

        final HTMLPageAssetRenderedAPI pageAssetRenderedAPI = createHtmlPageAssetRenderedAPIMock(
                loginUser, "<html>lol</html>", PageMode.PREVIEW_MODE);

        final VelocityServlet velocityServlet = new VelocityServlet(WebAPILocator.getUserWebAPI(),
                pageAssetRenderedAPI);
        velocityServlet.service(mockRequest, response);

        verifyPageServed("<html>lol</html>", outputStream);
    }

    /**
     * This is the actual test body
     *
     * @param user
     * @param mode
     * @throws IOException
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws ServletException
     */
    private void testServerPageFor(final User user, final LoginMode mode)
            throws IOException, DotSecurityException, DotDataException, ServletException {
        final String pageContent = "<html>lol</html>";

        final String pageUri = "/lol";

        VelocityRequestWrapper velocityRequest = mock(VelocityRequestWrapper.class);
        when(velocityRequest.getRequestURI()).thenReturn(pageUri);
        when(velocityRequest.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(LOGIN_MODE_PARAMETER)).thenReturn(mode);
        when(velocityRequest.getSession(Mockito.anyBoolean())).thenReturn(session);
        when(velocityRequest.getSession()).thenReturn(session);


        final ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outputStream);

        final HTMLPageAssetRenderedAPI pageAssetRenderedAPI = createHtmlPageAssetRenderedAPIMock(user,
                pageContent,
                mode == LoginMode.BE ? PageMode.PREVIEW_MODE : PageMode.LIVE);

        final VelocityServlet velocityServlet = new VelocityServlet(WebAPILocator.getUserWebAPI(),
                pageAssetRenderedAPI);
        velocityServlet.service(velocityRequest, response);
        verifyPageServed(pageContent, outputStream);
    }

    private static void verifyPageServed(final String pageContent, final ServletOutputStream outputStream)
            throws IOException {
        verify(outputStream, times(1)).write(pageContent.getBytes());
    }

    public HttpServletRequest createMockRequest(final String referer, final User user, final LoginMode loginMode) {
        return createMockRequest(referer, user, loginMode, false);
    }
    public HttpServletRequest createMockRequest(final String referer, final User user,
            final LoginMode loginMode, final boolean disabledNavigateMode) {

        VelocityRequestWrapper velocityRequest = mock(VelocityRequestWrapper.class);
        when(velocityRequest.getRequestURI()).thenReturn("/lol");
        when(velocityRequest.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(LOGIN_MODE_PARAMETER)).thenReturn(loginMode);
        when(velocityRequest.getSession(Mockito.anyBoolean())).thenReturn(session);
        when(velocityRequest.getSession()).thenReturn(session);

        when(velocityRequest.getParameter("disabledNavigateMode")).thenReturn(String.valueOf(disabledNavigateMode));

        if (referer != null) {
            when(velocityRequest.getHeader("referer")).thenReturn(referer);
        }

        return velocityRequest;
    }
}
