package com.dotcms.rendering.velocity.servlet;

import static org.junit.Assert.assertEquals;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class VelocityLiveModeTest {

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

    private static class TestCase {
        String htmlCodeExpected;
        String contentSecurityPolicyHeader;

        public TestCase(final String contentSecurityPolicyHeader, final String htmlCodeExpected) {
            this.htmlCodeExpected = htmlCodeExpected;
            this.contentSecurityPolicyHeader = contentSecurityPolicyHeader;
        }
    }

}
