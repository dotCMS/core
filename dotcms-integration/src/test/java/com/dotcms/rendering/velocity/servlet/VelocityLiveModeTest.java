package com.dotcms.rendering.velocity.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.mock.request.DotCMSMockRequestWithSession;
import com.dotcms.security.ContentSecurityPolicyUtil;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.business.PageCacheParameters;
import com.dotmarketing.filters.Constants;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class VelocityLiveModeTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider(format = "%m: %p[0]")
    public static Object[] configs() throws Exception {
        prepare();

        return new TestCase[]{
                new TestCase(
            "style-src {style-src nonce};script-src {script-src nonce}",
                        "<script %1$s>console.log(\"This is a test\");</script>\n"
                                + "<style %1$s>h1 {color: red;}</style><h1></h1></div>"
                ),
                new TestCase(
                        "style-src {style-src nonce};",
                        "<script>console.log(\"This is a test\");</script>\n"
                                + "<style %1$s>h1 {color: red;}</style><h1></h1></div>"
                ),
                new TestCase(
                        "script-src {script-src nonce}",
                        "<script %1$s>console.log(\"This is a test\");</script>\n"
                                + "<style>h1 {color: red;}</style><h1></h1></div>"
                ),
                new TestCase(
                        "default-src 'self' ",
                        "<script>console.log(\"This is a test\");</script>\n"
                                + "<style>h1 {color: red;}</style><h1></h1></div>"
                ),
                new TestCase(null,
                        "<script>console.log(\"This is a test\");</script>\n"
                                + "<style>h1 {color: red;}</style><h1></h1></div>"
                )
        };
    }

    /**
     * Method: {@link VelocityModeHandler#modeHandler(PageMode, HttpServletRequest, HttpServletResponse)}
     * When: The ContentSecurityPolicy.header property is set
     * Should: Set the Content-Security-Policy header and change the htmlCode to include the nonce value
     *
     * @param testCase
     * @throws DotDataException
     * @throws WebAssetException
     * @throws DotSecurityException
     */
    @Test
    @UseDataProvider("configs")
    public void contentSecurityPolice(final TestCase testCase) throws DotDataException, WebAssetException, DotSecurityException {
        final String previousValue = Config.getStringProperty("ContentSecurityPolicy.header",
                null);
        Config.setProperty("ContentSecurityPolicy.header", testCase.contentSecurityPolicyHeader);

        try {
            final Host host = new SiteDataGen().nextPersisted();

            final Field field = new FieldDataGen().type(TextField.class).next();
            final ContentType contentType = new ContentTypeDataGen().field(field).nextPersisted();

            final Container container = new ContainerDataGen()
                    .site(host)
                    .withContentType(contentType, "<h1>$!{test}</h1>")
                    .preLoop("<script>console.log(\"This is a test\");</script>\n"
                            + "<style>h1 {color: red;}</style>")
                    .nextPersisted();

            ContainerDataGen.publish(container);

            final Template template = new TemplateDataGen()
                    .site(host)
                    .withContainer(container, "1")
                    .nextPersisted();

            TemplateDataGen.publish(template);

            final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
            HTMLPageDataGen.publish(htmlPageAsset);

            final HttpServletResponse response = mock(HttpServletResponse.class);

            final HttpSession session = mock(HttpSession.class);
            final DotCMSMockRequestWithSession request = new DotCMSMockRequestWithSession(session, false);

            request.setRequestURI(htmlPageAsset.getURI());

            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            Clickstream clickstream = new Clickstream();
            when(session.getAttribute("clickstream")).thenReturn(clickstream);

            final Contentlet contentlet = new ContentletDataGen(contentType)
                    .setProperty(field.variable(), "TEST!!!")
                    .nextPersisted();

            ContentletDataGen.publish(contentlet);

            new MultiTreeDataGen()
                    .setPage(htmlPageAsset)
                    .setContainer(container)
                    .setContentlet(contentlet)
                    .setInstanceID("1")
                    .nextPersisted();

            final VelocityModeHandler velocityModeHandler = VelocityModeHandler.modeHandler(
                    PageMode.LIVE, request, response, htmlPageAsset.getURI(), host);

            final String htmlCode = velocityModeHandler.eval();

            final Object nonceRequestAttribute = request.getAttribute("NONCE_REQUEST_ATTRIBUTE");
            final String nonce = String.format("nonce='%s'", nonceRequestAttribute);

            final String htmlExpected = String.format(testCase.htmlCodeExpected, nonce);
            assertEquals(htmlExpected, htmlCode);
        } finally {
            Config.setProperty("ContentSecurityPolicy.header", previousValue);
        }
    }

    /**
     * Method: {@link VelocityLiveMode#buildCacheParameters(long, IHTMLPage)}
     * When: Two requests go through the same vanity URL 200-forward to the same page but carry
     *       different original request URIs in FORWARD_REQUEST_URI
     * Should: Produce different cache keys so each URL gets its own cache entry, preventing
     *         the page cache collision bug where one affiliate's content is served to another
     */
    @Test
    public void vanityForwardDifferentOriginalUriProducesDifferentCacheKeys()
            throws DotDataException, DotSecurityException, WebAssetException {

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().site(host).nextPersisted();
        TemplateDataGen.publish(template);
        final HTMLPageAsset detailPage = new HTMLPageDataGen(host, template)
                .pageURL("detail-page")
                .cacheTTL(3600)
                .nextPersisted();
        HTMLPageDataGen.publish(detailPage);

        // A vanity URL that regex-forwards multiple incoming URLs to the same detail page
        final CachedVanityUrl vanity = new CachedVanityUrl(
                "test-vanity-id",
                "/store/([0-9]+)/.*",
                1L,
                host.getIdentifier(),
                detailPage.getURI(),
                javax.servlet.http.HttpServletResponse.SC_OK, // 200 = forward
                1
        );

        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getStatus()).thenReturn(200);
        when(response.getHeader("Cache-Control")).thenReturn(null);
        final HttpSession session = mock(HttpSession.class);

        // Request 1: affiliate 123 hits /store/123/acme/catalog/
        final DotCMSMockRequestWithSession req1 = new DotCMSMockRequestWithSession(session, false);
        req1.setRequestURI(detailPage.getURI());
        req1.setAttribute(Constants.VANITY_URL_OBJECT, vanity);
        req1.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI, "/store/123/acme/catalog/");

        // Request 2: affiliate 456 hits /store/456/globex/catalog/
        final DotCMSMockRequestWithSession req2 = new DotCMSMockRequestWithSession(session, false);
        req2.setRequestURI(detailPage.getURI());
        req2.setAttribute(Constants.VANITY_URL_OBJECT, vanity);
        req2.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI, "/store/456/globex/catalog/");

        final VelocityLiveMode handler1 = new VelocityLiveMode(req1, response, detailPage, host);
        final VelocityLiveMode handler2 = new VelocityLiveMode(req2, response, detailPage, host);

        final PageCacheParameters params1 = handler1.buildCacheParameters(1L, detailPage);
        final PageCacheParameters params2 = handler2.buildCacheParameters(1L, detailPage);

        assertNotEquals(
                "Vanity URL forward requests with different original URIs must produce different cache keys",
                params1.getKey(), params2.getKey());
        assertTrue("Cache key must include the original request URI for request 1",
                params1.getKey().contains("originalUri:/store/123/acme/catalog/"));
        assertTrue("Cache key must include the original request URI for request 2",
                params2.getKey().contains("originalUri:/store/456/globex/catalog/"));
    }

    private static class TestCase {
        String htmlCodeExpected;
        String contentSecurityPolicyHeader;

        public TestCase(final String contentSecurityPolicyHeader, final String htmlCodeExpected) {
            this.htmlCodeExpected = htmlCodeExpected;
            this.contentSecurityPolicyHeader = contentSecurityPolicyHeader;
        }
    }

}
