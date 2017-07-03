package com.dotmarketing.filters;

import static org.mockito.Matchers.startsWith;

import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.contenttype.transform.contenttype.ImplClassContentTypeTransformer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.servlets.SpeedyAssetServlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.velocity.ClientVelocityServlet;
import com.dotmarketing.velocity.VelocityServlet;
import com.liferay.portal.model.User;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class CMSFilterTest {

    private static ContentletAPI contentletAPI;
    private static VanityUrlAPI vanityUrlAPI;
    private static Host defaultHost;
    private static HostAPI hostAPI;
    private static User user;
    private static LanguageAPI languageAPI;
    private static long defaultLanguageId;
    private static ContentTypeAPI contentTypeAPI;
    private static PermissionAPI permissionAPI;
    private static ContentType contentType;

    private static final String VANITY_URL_CONTENT_TYPE_NAME = "Vanity URL Asset";
    private static final String VANITY_URL_CONTENT_TYPE_VARNAME = "Vanityurlasset";

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
        Mockito.when(Config.CONTEXT.getRealPath(startsWith("/"))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return (String) invocation.getArguments()[0];
            }
        });

        Mockito.when(Config.CONTEXT.getResourceAsStream(startsWith("/")))
                .thenAnswer(new Answer<FileInputStream>() {
                    @Override
                    public FileInputStream answer(InvocationOnMock invocation) throws Throwable {
                        return new FileInputStream((String) invocation.getArguments()[0]);
                    }
                });
        IntegrationTestInitService.getInstance().mockStrutsActionModule();

        /* Default user */
        user = APILocator.systemUser();

        /* APIs initialization */
        hostAPI = APILocator.getHostAPI();
        contentletAPI = APILocator.getContentletAPI();
        vanityUrlAPI = APILocator.getVanityUrlAPI();
        languageAPI = APILocator.getLanguageAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        permissionAPI = APILocator.getPermissionAPI();

        /* Default variables */
        defaultHost = hostAPI.findDefaultHost(user, false);
        defaultLanguageId = languageAPI.getDefaultLanguage().getId();
        getContentType();
    }

    @Test
    public void shouldWorkVanityUrl() throws IOException, DotDataException {

        //Init APIs and test values
        final User systemUser = APILocator.getUserAPI().getSystemUser();

        Contentlet vanityUrl1 = null;
        Contentlet vanityUrl2 = null;
        Contentlet vanityUrl3 = null;
        Contentlet vanityUrl4 = null;
        Contentlet vanityUrl5 = null;

        // build them up
        try {
            vanityUrl1 = createVanityUrl("test link1", Host.SYSTEM_HOST, "/testLink1",
                    "/about-us/" + CMSFilter.CMS_INDEX_PAGE, 200, 1, defaultLanguageId);

            vanityUrl2 = createVanityUrl("test link2", defaultHost.getIdentifier(),
                    "/testLink2", "/about-us/" + CMSFilter.CMS_INDEX_PAGE, 200, 1,
                    defaultLanguageId);

            vanityUrl3 = createVanityUrl("test link3", defaultHost.getIdentifier(),
                    "/testLink3", "http://demo.dotcms.com/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    301, 1, defaultLanguageId);

            vanityUrl4 = createVanityUrl("test link4", defaultHost.getIdentifier(),
                    "/testLink4", "http://demo.dotcms.com/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    301, 1, defaultLanguageId);

            vanityUrl5 = createVanityUrl("test link5", defaultHost.getIdentifier(),
                    "/testLink5", "/products/", 200, 1, defaultLanguageId);

            CMSFilter cmsFilter = new CMSFilter();
            HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
            MockResponseWrapper response = new MockResponseWrapper(res);
            FilterChain chain = Mockito.mock(FilterChain.class);
            Logger.info(this.getClass(),
                    "/testLink1 should forward to /about-us/" + CMSFilter.CMS_INDEX_PAGE);
            HttpServletRequest request = getMockRequest("demo.dotcms.com", "/testLink1");
            cmsFilter.doFilter(request, response, chain);
            Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
            Assert.assertEquals(200, response.getStatus());
            Logger.info(this.getClass(),
                    "looking for /about-us/" + CMSFilter.CMS_INDEX_PAGE + ", got;" + request
                            .getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
            Assert.assertEquals("/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));

            Logger.info(this.getClass(),
                    "/testLink2 should forward to /about-us/" + CMSFilter.CMS_INDEX_PAGE);
            request = getMockRequest("demo.dotcms.com", "/testLink2");
            response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
            cmsFilter.doFilter(request, response, chain);
            Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
            Assert.assertEquals(200, response.getStatus());
            Logger.info(this.getClass(),
                    "looking for /about-us" + CMSFilter.CMS_INDEX_PAGE + ", got;" + request
                            .getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
            Assert.assertEquals("/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));

            Logger.info(this.getClass(),
                    "/testLink3 should redirect to http://demo.dotcms.com/about-us/"
                            + CMSFilter.CMS_INDEX_PAGE);
            request = getMockRequest("demo.dotcms.com", "/testLink3");
            response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
            cmsFilter.doFilter(request, response, chain);
            Logger.info(this.getClass(), "looking for 301, got;" + response.getStatus());
            Assert.assertEquals(301, response.getStatus());
            Logger.info(this.getClass(),
                    "looking for http://demo.dotcms.com/about-us" + CMSFilter.CMS_INDEX_PAGE
                            + ", got;" + response.getRedirectLocation());
            Assert.assertEquals("http://demo.dotcms.com/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    response.getRedirectLocation());

            Logger.info(this.getClass(),
                    "/testLink4 should redirect to http://demo.dotcms.com/about-us/"
                            + CMSFilter.CMS_INDEX_PAGE);
            request = getMockRequest("demo.dotcms.com", "/testLink4");
            response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
            cmsFilter.doFilter(request, response, chain);

            Logger.info(this.getClass(), "looking for 301, got;" + response.getStatus());
            Assert.assertEquals(301, response.getStatus());
            Logger.info(this.getClass(),
                    "looking for http://demo.dotcms.com/about-us" + CMSFilter.CMS_INDEX_PAGE
                            + ", got;" + response.getRedirectLocation());
            Assert.assertEquals("http://demo.dotcms.com/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    response.getRedirectLocation());

            Logger.info(this.getClass(),
                    "/testLink5 should forward to /products/" + CMSFilter.CMS_INDEX_PAGE);
            request = getMockRequest("demo.dotcms.com", "/testLink5/");
            cmsFilter.doFilter(request, response, chain);
            response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
            Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
            Assert.assertEquals(200, response.getStatus());
            Logger.info(this.getClass(),
                    "looking for /products/" + CMSFilter.CMS_INDEX_PAGE + ", got;" + request
                            .getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
            Assert.assertEquals("/products/" + CMSFilter.CMS_INDEX_PAGE,
                    request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));


        } catch (Exception e) {

            e.printStackTrace();
            Assert.fail();

        } finally {
            // cleanup
            try {
                if (vanityUrl1 != null) {
                    contentletAPI.delete(vanityUrl1, systemUser, false);
                }
                if (vanityUrl2 != null) {
                    contentletAPI.delete(vanityUrl2, systemUser, false);
                }
                if (vanityUrl3 != null) {
                    contentletAPI.delete(vanityUrl3, systemUser, false);
                }
                if (vanityUrl4 != null) {
                    contentletAPI.delete(vanityUrl4, systemUser, false);
                }
                if (vanityUrl5 != null) {
                    contentletAPI.delete(vanityUrl5, systemUser, false);
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error cleaning up Vanity Url Links");
            }

        }
    }

    @Test
    public void shouldWorkVanityUrlCMSHomePage() throws IOException, DotDataException {

        //Init APIs and test values
        Contentlet vanityURLContentlet = null;
        // build them up
        try {

            vanityURLContentlet = createVanityUrl("cmsHomePage", defaultHost.getIdentifier(), "/cmsHomePage",
                    "/about-us/" + CMSFilter.CMS_INDEX_PAGE, 200, 1, defaultLanguageId);
            contentletAPI.isInodeIndexed(vanityURLContentlet.getInode(), true);
            
            CMSFilter cmsFilter = new CMSFilter();
            HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
            MockResponseWrapper response = new MockResponseWrapper(res);
            FilterChain chain = Mockito.mock(FilterChain.class);
            HttpServletRequest request = null;

            Logger.info(this.getClass(),
                    "/cmsHomePage should forward to /about-us/" + CMSFilter.CMS_INDEX_PAGE);
            request = getMockRequest("demo.dotcms.com", "/");
            response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
            cmsFilter.doFilter(request, response, chain);
            Logger.info(this.getClass(), "looking for 200, got:" + response.getStatus());
            Assert.assertEquals(200, response.getStatus());
            Logger.info(this.getClass(),
                    "looking for /about-us/" + CMSFilter.CMS_INDEX_PAGE + ", got:" + request
                            .getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
            Assert.assertEquals("/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
            //Delete the test Vanity URL
            contentletAPI.delete(vanityURLContentlet, user, false);

            
            //And save it
            vanityURLContentlet = createVanityUrl("cmsHomePage Host", Host.SYSTEM_HOST, "/cmsHomePage",
                    "/about-us/" + CMSFilter.CMS_INDEX_PAGE, 200, 1, defaultLanguageId);
            contentletAPI.isInodeIndexed(vanityURLContentlet.getInode(), true);

            Logger.info(this.getClass(), "demo.dotcms.com:/cmsHomePage should forward to /about-us/"
                    + CMSFilter.CMS_INDEX_PAGE);
            request = getMockRequest("demo.dotcms.com", "/");
            response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
            cmsFilter.doFilter(request, response, chain);
            Logger.info(this.getClass(), "looking for 200, got:" + response.getStatus());
            Assert.assertEquals(200, response.getStatus());
            Logger.info(this.getClass(),
                    "looking for /about-us" + CMSFilter.CMS_INDEX_PAGE + ", got:" + request
                            .getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
            Assert.assertEquals("/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));

        } catch (Exception e) {

            e.printStackTrace();
            Assert.fail();

        } finally {
            try {
                //Delete the test Vanity URL
                if(vanityURLContentlet != null) {
                    contentletAPI.delete(vanityURLContentlet, user, false);
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error deleting Vanity URL");
            }

        }
    }


    private HttpServletRequest getMockRequest(String hostname, String uri) {

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn(uri);
        Mockito.when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://" + hostname + uri));
        Mockito.when(request.getCookies()).thenReturn(new Cookie[]{});
        Mockito.when(request.getServerName()).thenReturn(hostname);
        Mockito.when(request.getSession()).thenReturn(new MockSession());
        Mockito.when(request.getSession(Mockito.anyBoolean())).thenReturn(new MockSession());
        Mockito.when(request.getRequestDispatcher("/servlets/VelocityServlet"))
                .thenReturn(new RequestDispatcher() {

                    @Override
                    public void include(ServletRequest arg0, ServletResponse arg1)
                            throws ServletException, IOException {

                    }

                    @Override
                    public void forward(ServletRequest arg0, ServletResponse arg1)
                            throws ServletException, IOException {

                        VelocityServlet servlet = new ClientVelocityServlet();
                        servlet.init(null);
                        servlet.service(arg0, arg1);
                    }
                });
        Mockito.when(request.getRequestDispatcher(Mockito.startsWith("/dotAsset/")))
                .thenReturn(new RequestDispatcher() {

                    @Override
                    public void include(ServletRequest arg0, ServletResponse arg1)
                            throws ServletException, IOException {

                    }

                    @Override
                    public void forward(ServletRequest arg0, ServletResponse arg1)
                            throws ServletException, IOException {

                        SpeedyAssetServlet servlet = new SpeedyAssetServlet();
                        servlet.init(null);
                        servlet.service(arg0, arg1);
                    }
                });

        MockRequestWrapper reqWrap = new MockRequestWrapper(request);
        return reqWrap;

    }


    public void runTests() throws IOException, DotDataException {

        shouldRedirectToFolderIndex();
        shouldWorkVanityUrl();
        shouldForwardToImage();
        shouldRedirect401();
        shouldWorkVanityUrlCMSHomePage();
    }

    @Test
    public void shouldReturnStrutsPage() throws IOException {

        Logger.info(this.getClass(),
                "/dotCMS/login should forward to /application/login/" + CMSFilter.CMS_INDEX_PAGE);
        MockResponseWrapper response = new MockResponseWrapper(
                Mockito.mock(HttpServletResponse.class));
        FilterChain chain = Mockito.mock(FilterChain.class);
        HttpServletRequest request = getMockRequest("demo.dotcms.com", "/dotCMS/login");

        try {
            new CMSFilter().doFilter(request, response, chain);
            Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
            Assert.assertEquals(200, response.getStatus());
            Mockito.verify(chain).doFilter(request, response);
        } catch (ServletException e) {
            Assert.fail();
            e.printStackTrace();
        }

    }

    /**
     * This tests the demo site for its 404 image
     */
    @Test
    public void shouldForwardToImage() throws IOException {

        ServletOutputStream sos = Mockito.mock(ServletOutputStream.class);

        Logger.info(this.getClass(), "/images/404.jpg should give us a 200");
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        Mockito.when(res.getOutputStream()).thenReturn(sos);
        MockResponseWrapper response = new MockResponseWrapper(res);

        FilterChain chain = Mockito.mock(FilterChain.class);
        HttpServletRequest request = getMockRequest("demo.dotcms.com", "/images/404.jpg");

        try {
            new CMSFilter().doFilter(request, response, chain);
            Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
            Assert.assertEquals(200, response.getStatus());


        } catch (ServletException e) {
            Assert.fail();
            e.printStackTrace();
        }

    }

    /**
     * This tests the demo site for its 404 image
     */
    @Test
    public void shouldRedirect401() throws IOException {

        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        MockResponseWrapper response = new MockResponseWrapper(res);

        FilterChain chain = Mockito.mock(FilterChain.class);
        HttpServletRequest request = getMockRequest("demo.dotcms.com", "/intranet/");

        try {
            new CMSFilter().doFilter(request, response, chain);
            Logger.info(this.getClass(), "looking for 401, got;" + response.getStatus());
            Assert.assertEquals(401, response.getStatus());


        } catch (ServletException e) {
            Assert.fail();
            e.printStackTrace();
        }

    }

    /*
     * This tests if the cms filter correctly redirects a user from /products to
     * /products/
     */
    @Test
    public void shouldRedirectToFolderIndex() throws IOException {
        Logger.info(this.getClass(), "/products should redirect to /products/");

        MockResponseWrapper response = new MockResponseWrapper(
                Mockito.mock(HttpServletResponse.class));
        FilterChain chain = Mockito.mock(FilterChain.class);
        HttpServletRequest request = getMockRequest("demo.dotcms.com", "/products");

        try {
            new CMSFilter().doFilter(request, response, chain);

            Assert.assertEquals("/products/",
                    ((MockResponseWrapper) response).getRedirectLocation());
            Assert.assertEquals(301, response.getStatus());
        } catch (ServletException e) {
            Assert.fail();
        }

        Logger.info(this.getClass(),
                "/home/ should forward to /products/" + CMSFilter.CMS_INDEX_PAGE);
        request = getMockRequest("demo.dotcms.com", "/products/");
        response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
        try {
            new CMSFilter().doFilter(request, response, chain);
            Logger.info(this.getClass(),
                    "looking for /products/" + CMSFilter.CMS_INDEX_PAGE + " , got;" + request
                            .getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
            Assert.assertEquals("/products/" + CMSFilter.CMS_INDEX_PAGE,
                    request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
            Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
            Assert.assertEquals(200, response.getStatus());
        } catch (ServletException e) {
            Assert.fail();
        }


    }

    /**
     * Get the Vanity Url Content Type. If the content Type doesn't
     * exist then it will be created
     *
     * @return a Vanity Url Content Type
     */
    private static ContentType getContentType() throws DotDataException, DotSecurityException {
        String query = " velocity_var_name = '" + VANITY_URL_CONTENT_TYPE_VARNAME + "'";
        List<ContentType> contentTypes = contentTypeAPI.search(query);

        if (contentTypes.size() == 0) {
            contentType = createVanityUrl();
        }else {
            contentType = contentTypes.get(0);
        }

        return contentType;
    }

    /**
     * Create a VanityUrl content type
     *
     * @return A new vanity Url content Type
     */
    private static ContentType createVanityUrl() throws DotDataException, DotSecurityException {
        BaseContentType base = BaseContentType
                .getBaseContentType(BaseContentType.VANITY_URL.getType());

        final ContentType type = new ContentType() {
            @Override
            public String name() {
                return VANITY_URL_CONTENT_TYPE_NAME;
            }

            @Override
            public String id() {
                return null;
            }

            @Override
            public String description() {
                return null;
            }

            @Override
            public String variable() {
                return VANITY_URL_CONTENT_TYPE_VARNAME;
            }

            @Override
            public BaseContentType baseType() {
                return base;
            }
        };

        return contentTypeAPI.save(new ImplClassContentTypeTransformer(type).from());
    }

    /**
     * Creates a new Vanity URL contentlet
     */
    private Contentlet createVanityUrl(String title, String site, String uri,
            String forwardTo, int action, int order, long languageId)
            throws DotDataException, DotSecurityException {
        //Create the new Contentlet
        Contentlet contentlet = new Contentlet();
        contentlet.setStructureInode(contentType.inode());
        contentlet.setHost(defaultHost.getIdentifier());
        contentlet.setLanguageId(languageId);

        contentlet.setStringProperty(VanityUrlContentType.TITLE_FIELD_VAR, title);
        contentlet.setStringProperty(VanityUrlContentType.SITE_FIELD_VAR, site);
        contentlet.setStringProperty(VanityUrlContentType.URI_FIELD_VAR, uri);
        contentlet.setStringProperty(VanityUrlContentType.FORWARD_TO_FIELD_VAR, forwardTo);
        contentlet.setLongProperty(VanityUrlContentType.ACTION_FIELD_VAR, action);
        contentlet.setLongProperty(VanityUrlContentType.ORDER_FIELD_VAR, order);

        //Get The permissions of the Content Type
        List<Permission> contentTypePermissions = permissionAPI.getPermissions(contentType);

        //Validate if the contenlet is OK
        contentletAPI.validateContentlet(contentlet, new ArrayList());

        //Save the contentlet
        contentlet = contentletAPI.checkin(contentlet, contentTypePermissions, user, true);
        contentletAPI.isInodeIndexed(contentlet.getInode());
        //Publish Vanity Url
        contentletAPI.publish(contentlet, user, false);
        contentletAPI.isInodeIndexed(contentlet.getInode());
        return contentlet;
    }

    class MockRequestWrapper extends HttpServletRequestWrapper {

        Map<String, Object> valmap = new HashMap<>();

        public MockRequestWrapper(HttpServletRequest request) {
            super(request);

        }

        @Override
        public void setAttribute(String arg0, Object arg1) {
            valmap.put(arg0, arg1);

        }

        @Override
        public Object getAttribute(String arg0) {
            return valmap.get(arg0);
        }

    }

    class MockResponseWrapper extends HttpServletResponseWrapper {

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        int status = 200;
        String location = null;
        Map<String, String> headers = new HashMap<String, String>();

        @Override
        public PrintWriter getWriter() throws IOException {
            return writer;
        }

        public MockResponseWrapper(HttpServletResponse response) {
            super(response);

        }

        @Override
        public int getStatus() {
            // TODO Auto-generated method stub
            return status;
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            status = sc;
            Logger.info(this.getClass(), msg);
        }

        @Override
        public void sendError(int sc) throws IOException {
            status = sc;
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            this.location = location;
            Logger.info(this.getClass(), "redirecting;" + location);
            status = 301;
        }

        @Override
        public void setStatus(int sc, String sm) {
            Logger.info(this.getClass(), sm);
            status = sc;
        }

        @Override
        public void setStatus(int sc) {
            status = sc;
        }

        public String getRedirectLocation() {
            if (location != null && !location.isEmpty()) {
                return location;
            }
            return headers.get("Location");
        }

        @Override
        public void setHeader(String key, String value) {
            headers.put(key, value);
        }
    }

    class MockSession implements HttpSession {

        Map<String, Object> valmap = new HashMap<>();

        @Override
        public void setMaxInactiveInterval(int arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setAttribute(String arg0, Object arg1) {
            valmap.put(arg0, arg1);

        }

        @Override
        public void removeValue(String arg0) {
            valmap.remove(arg0);

        }

        @Override
        public void removeAttribute(String arg0) {
            valmap.remove(arg0);

        }

        @Override
        public void putValue(String arg0, Object arg1) {
            valmap.put(arg0, arg1);

        }

        @Override
        public boolean isNew() {

            return true;
        }

        @Override
        public void invalidate() {
            valmap = new HashMap<>();

        }

        @Override
        public String[] getValueNames() {

            return valmap.keySet().toArray(new String[valmap.size()]);
        }

        @Override
        public Object getValue(String arg0) {
            return valmap.get(arg0);
        }

        @Override
        public ServletContext getServletContext() {

            return null;
        }

        @Override
        public int getMaxInactiveInterval() {

            return 0;
        }

        @Override
        public long getLastAccessedTime() {

            return 0;
        }

        @Override
        public String getId() {

            return null;
        }

        @Override
        public long getCreationTime() {

            return 0;
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object getAttribute(String arg0) {
            return valmap.get(arg0);
        }

        @Override
        public HttpSessionContext getSessionContext() {
            // TODO Auto-generated method stub
            return null;
        }
    }

}
