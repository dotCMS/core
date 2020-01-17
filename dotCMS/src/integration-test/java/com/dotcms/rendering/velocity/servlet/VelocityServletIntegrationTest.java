package com.dotcms.rendering.velocity.servlet;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.util.FiltersUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.VanityURLFilter;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
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
import java.util.HashMap;
import java.util.Map;

import static com.dotcms.datagen.TestDataUtils.getNewsLikeContentType;

import static org.mockito.Mockito.*;

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
                final String key = invocation.getArgumentAt(0, String.class);
                final Object value = invocation.getArgumentAt(1, Object.class);
                attributes.put(key, value);
                return null;
            }
        }).when(request).setAttribute(anyString(), anyObject());

        // Mock getAttribute
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final String key = invocation.getArgumentAt(0, String.class);
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
     * when: There is exists a Content Type Url Map and is request
     * Should: return 200 Http code
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

        final String contentletURLMap = newsPatternPrefix + contentlet.getStringProperty("urlTitle");

        when(request.getRequestURI()).thenReturn(contentletURLMap);

        velocityServlet.service(request, response);

        verify(servletOutputStream).write("".getBytes());
        verify(response, never()).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Method to test: {@link VelocityServlet#service(HttpServletRequest, HttpServletResponse)}
     * when: There is exists a Content Type Url Map and is request
     * Should: return 200 Http code
     *
     * @throws ServletException
     * @throws IOException
     */
    @Test
    public void whenRequestVanityURL() throws ServletException, IOException, DotDataException, DotSecurityException {

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

        verify(servletOutputStream).write("".getBytes());
        verify(response, never()).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void createAndPublishVanityURL(final String forwardURL, final String VANITY_URI)
            throws DotDataException, DotSecurityException {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final Contentlet vanityUrl = FiltersUtil.getInstance().createVanityUrl("test", host.getIdentifier(), VANITY_URI,
                forwardURL, 200, 0, defaultLanguage.getId());

        FiltersUtil.getInstance().publishVanityUrl(vanityUrl);
    }

    /**
     * Method to test: {@link VelocityServlet#service(HttpServletRequest, HttpServletResponse)}
     * when: There is exists a VanityURL that forward to a URL Map
     * Should: return 200 Http code
     *
     * @throws ServletException
     * @throws IOException
     */
    @Test
    public void whenRequestURLMapAndVanityURLTogether() throws ServletException, IOException, DotSecurityException, DotDataException {

        final VelocityServlet velocityServlet = new VelocityServlet();

        final String newsPatternPrefix =
                TEST_PATTERN + System.currentTimeMillis() + "/";
        final Contentlet contentlet = createURLMapperContentType(newsPatternPrefix, host);
        ContentletDataGen.publish(contentlet);

        when(request.getRequestURI()).thenReturn("/vanityURL/" + contentlet.getStringProperty("urlTitle"));

        final String VANITY_URI = "/vanityURL/([a-zA-Z0-9-_]+)";
        final String FORWARD_URL = newsPatternPrefix + "$1";

        createAndPublishVanityURL(FORWARD_URL, VANITY_URI);

        when(request.getRequestURI()).thenReturn("/vanityURL/" + contentlet.getStringProperty("urlTitle"));
        FilterChain chain = Mockito.mock(FilterChain.class);

        final VanityURLFilter vanityURLFilter = new VanityURLFilter();
        vanityURLFilter.doFilter(request, response, chain);

        velocityServlet.service(request, response);

        verify(servletOutputStream).write("".getBytes());
        verify(response, never()).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private static Contentlet createURLMapperContentType(final String newsPatternPrefix, final Host host) {
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


    private static HTMLPageAsset createPage(){

        final Folder folder = new FolderDataGen()
                .nextPersisted();

        final Template template = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(folder, template)
                .pageURL("news-detail")
                .title("news-detail")
                .nextPersisted();
        ContentletDataGen.publish(htmlPageAsset);
        return  htmlPageAsset;
    }
}
