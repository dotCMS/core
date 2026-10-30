package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.rendering.velocity.viewtools.cache.XSLTransformationCache;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.FileUtil;
import graphql.Assert;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Exercises the XSLT view tool end to end: the ordinary case from the documentation, and what the
 * tool does when it decides not to transform.
 *
 * <p>Every case here keeps the XML inside dotCMS or points at a host that cannot answer. Nothing
 * reaches a real server on the internet, so the class does not go red when someone else's site is
 * down. A successful download with {@code FEATURE_FLAG_XSLT_REMOTE_XML} on is deliberately not
 * covered: {@link com.dotcms.http.CircuitBreakerUrl} refuses private subnets unless
 * {@code ALLOW_ACCESS_TO_PRIVATE_SUBNETS} is true, and it reads that once into a static field, so a
 * local test server is only reachable if the whole module runs with that setting — which would
 * change the outbound policy for every other test in the suite.
 */
public class XsltToolTest extends IntegrationTestBase {

    static final String ENABLE_SCRIPTING = "ENABLE_SCRIPTING";

    /** Mirrors the stylesheet's {@code CATALOG/CD} structure, so a real transformation comes out. */
    private static final String FIRST_TITLE_IN_THE_CATALOG = "EmpireBurlesque";

    /** Calls a Java method from XSLT, which the tool's engine refuses. */
    private static final String STYLESHEET_THAT_FAILS =
            "<?xml version=\"1.0\"?>"
                    + "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'"
                    + " xmlns:sys='http://xml.apache.org/xalan/java/java.lang.System'>"
                    + "<xsl:template match='/'><out>"
                    + "<xsl:value-of select=\"sys:getProperty('java.version')\"/>"
                    + "</out></xsl:template></xsl:stylesheet>";

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Holds what one test case needs: the request the tool is initialised from, and the paths of
     * the assets published for it.
     *
     * <p>The tool itself is not kept here on purpose. {@link XsltTool#init(Object)} returns without
     * reading anything while {@code ENABLE_SCRIPTING} is off, so a tool built before the setting is
     * turned on silently refuses every call afterwards — and a test that only asserts "nothing was
     * rendered" would then pass without exercising anything.
     */
    private static final class Fixture {
        final ViewContext viewContext;
        final String xslPath;
        final String failingXslPath;
        final String xmlPath;

        Fixture(final ViewContext viewContext, final String xslPath, final String failingXslPath,
                final String xmlPath) {
            this.viewContext = viewContext;
            this.xslPath = xslPath;
            this.failingXslPath = failingXslPath;
            this.xmlPath = xmlPath;
        }
    }

    /**
     * Publishes a stylesheet and an XML document on a fresh site and hands back a tool pointed at
     * them.
     *
     * <p>Both the stylesheet from the documentation and one the engine refuses are published, so
     * every case can also show that the tool is willing to transform at all.
     *
     * @return the published paths and the request to initialise the tool from
     */
    private Fixture publishAssets() throws Exception {

        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(Host.HOST_VELOCITY_VAR_NAME);

        Assert.assertNotNull(contentType);

        final Host site = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().name("test").site(site).nextPersisted();

        final File xslFile = File.createTempFile("demo-stylesheet", ".xsl");
        copyResource("xml/demo-stylesheet.xsl", xslFile);
        final Contentlet stylesheetAsset = new FileAssetDataGen(folder, xslFile).nextPersisted();
        ContentletDataGen.publish(stylesheetAsset);

        final File failingXslFile = File.createTempFile("refused-stylesheet", ".xsl");
        Files.write(failingXslFile.toPath(), STYLESHEET_THAT_FAILS.getBytes(StandardCharsets.UTF_8));
        final Contentlet failingAsset = new FileAssetDataGen(folder, failingXslFile).nextPersisted();
        ContentletDataGen.publish(failingAsset);

        // The XML lives in dotCMS, which is how the tool is meant to be used.
        final File xmlFile = File.createTempFile("cd_catalog", ".xml");
        copyResource("xml/test.xml", xmlFile);
        final Contentlet xmlAsset = new FileAssetDataGen(folder, xmlFile).host(site).nextPersisted();
        ContentletDataGen.publish(xmlAsset);

        final File vtl = File.createTempFile("testing-file", ".vtl");
        FileUtil.write(vtl, "$xslttool.transform($xmlPath, $xsltPath, 30)");
        final Contentlet fileAssetShown = new FileAssetDataGen(folder, vtl).host(site).nextPersisted();
        ContentletDataGen.publish(fileAssetShown);

        final VelocityContext velocityContext = mock(VelocityContext.class);
        when(velocityContext.getCurrentTemplateName()).thenReturn("/"+fileAssetShown.getInode()+"_");

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final ViewContext viewContext = mock(ViewContext.class);
        final HttpSession session = mock(HttpSession.class);

        final User user = APILocator.systemUser();
        when(viewContext.getVelocityContext()).thenReturn(velocityContext);
        when(viewContext.getRequest()).thenReturn(request);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        when(request.getAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST)).thenReturn(site);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(user.getUserId());
        when(session.getAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST)).thenReturn(site);

        return new Fixture(viewContext,
                String.format("/%s/%s", folder.getName(), xslFile.getName()),
                String.format("/%s/%s", folder.getName(), failingXslFile.getName()),
                String.format("/%s/%s", folder.getName(), xmlFile.getName()));
    }

    /**
     * Copies a test resource to a scratch file.
     *
     * <p>Checking a file asset in hands the binary over to the asset store, so the copies in
     * {@code target/test-classes} are never passed in directly: a second test in this class would
     * find them gone.
     *
     * @param resource path of the resource on the test classpath
     * @param target the file to write it to
     */
    private static void copyResource(final String resource, final File target) throws Exception {
        FileUtils.copyFile(new File(Objects.requireNonNull(Thread.currentThread()
                .getContextClassLoader().getResource(resource)).getFile()), target);
    }

    /**
     * Turns scripting on, hands the body a tool initialised under that setting, and puts the
     * setting back the way it was.
     *
     * <p>The order matters: the tool reads the request only while scripting is on, so it is built
     * here rather than by the caller.
     *
     * @param fixture the assets and request this case runs against
     * @param body what to do with the tool
     */
    private void withScriptingOn(final Fixture fixture, final ToolConsumer body) throws Exception {
        final boolean enableScripting = Config.getBooleanProperty(ENABLE_SCRIPTING, false);
        try {
            Config.setProperty(ENABLE_SCRIPTING, true);
            final XsltTool tool = new XsltTool();
            tool.init(fixture.viewContext);
            body.accept(tool);
        } finally {
            Config.setProperty(ENABLE_SCRIPTING, enableScripting);
        }
    }

    private interface ToolConsumer {
        void accept(XsltTool tool) throws Exception;
    }

    /**
     * Shows that this tool does transform when it is supposed to.
     *
     * <p>Every assertion in this class that an empty string came back would also hold if the tool
     * had simply refused the caller, so each case says out loud that it did not.
     */
    private void assertTheToolIsWillingToTransform(final XsltTool tool, final Fixture fixture)
            throws Exception {
        final String control = tool.transform(fixture.xmlPath, fixture.xslPath, 30);
        assertTrue("The tool refused the caller, so this case proves nothing. Got: " + control,
                control != null && control.contains(FIRST_TITLE_IN_THE_CATALOG));
    }

    /**
     * Test Our View Tool still works using the example from our documentation <a href="https://www.dotcms.com/docs/latest/xslttool">xslTool</a>
     * Even after adding the security changes
     * @throws Exception
     */
    @Test
    public void TestTransform() throws Exception {

        final Fixture fixture = publishAssets();

        withScriptingOn(fixture, tool -> {
            final String transform = tool.transform(fixture.xmlPath, fixture.xslPath, 30);
            assertNotNull(transform);
            assertTrue("The catalog was not transformed, got: " + transform,
                    transform.contains(FIRST_TITLE_IN_THE_CATALOG));
        });
    }

    /**
     * A template that still passes a URL as the XML source renders nothing and says why in the log,
     * rather than breaking the page it sits on.
     */
    @Test
    public void test_remote_xml_renders_nothing_while_the_switch_is_off() throws Exception {

        final Fixture fixture = publishAssets();

        withScriptingOn(fixture, tool -> {
            assertTheToolIsWillingToTransform(tool, fixture);

            final boolean remoteXml = Config.getBooleanProperty(
                    FeatureFlagName.FEATURE_FLAG_XSLT_REMOTE_XML, false);
            try {
                Config.setProperty(FeatureFlagName.FEATURE_FLAG_XSLT_REMOTE_XML, false);
                assertEquals("A URL source must not render anything while the switch is off",
                        "", tool.transform(
                                "http://example.com/cd_catalog.xml", fixture.xslPath, 30));
            } finally {
                Config.setProperty(FeatureFlagName.FEATURE_FLAG_XSLT_REMOTE_XML, remoteXml);
            }
        });
    }

    /**
     * With the switch on, a URL that cannot be read still renders nothing and leaves the cache
     * alone: the promise is that a failed download never ends up transformed and served as if it
     * were the document.
     *
     * <p>The host is in the reserved {@code .invalid} domain, so nothing is ever reached. Which
     * layer turns it away varies — the name may fail to resolve, or the outbound policy may refuse
     * whatever a wildcard resolver hands back — and the point of the case is that the tool behaves
     * the same either way.
     */
    @Test
    public void test_remote_xml_that_cannot_be_read_renders_nothing() throws Exception {

        final Fixture fixture = publishAssets();
        final String unreachable = "http://xslt-tool-test.invalid/cd_catalog.xml";

        withScriptingOn(fixture, tool -> {
            assertTheToolIsWillingToTransform(tool, fixture);

            final boolean remoteXml = Config.getBooleanProperty(
                    FeatureFlagName.FEATURE_FLAG_XSLT_REMOTE_XML, false);
            try {
                Config.setProperty(FeatureFlagName.FEATURE_FLAG_XSLT_REMOTE_XML, true);

                assertEquals("A URL that cannot be read must not render anything",
                        "", tool.transform(unreachable, fixture.xslPath, 30));

                assertNull("A failed download must not leave anything in the cache",
                        XSLTransformationCache.getXSLTranformationDocByXMLPath(
                                unreachable, fixture.xslPath));
            } finally {
                Config.setProperty(FeatureFlagName.FEATURE_FLAG_XSLT_REMOTE_XML, remoteXml);
            }
        });
    }

    /**
     * A stylesheet the engine refuses leaves nothing in the cache, so the next render tries again
     * instead of serving the empty result for the whole time to live.
     */
    @Test
    public void test_a_failed_transformation_is_not_cached() throws Exception {

        final Fixture fixture = publishAssets();

        withScriptingOn(fixture, tool -> {
            assertTheToolIsWillingToTransform(tool, fixture);

            assertEquals("A stylesheet that is refused produces no output",
                    "", tool.transform(fixture.xmlPath, fixture.failingXslPath, 300000));

            assertNull("A failed transformation must not be cached",
                    XSLTransformationCache.getXSLTranformationDocByXMLPath(
                            fixture.xmlPath, fixture.failingXslPath));

            // Second call: still empty, and still nothing cached, so it really did try again.
            assertEquals("", tool.transform(fixture.xmlPath, fixture.failingXslPath, 300000));
            assertNull(XSLTransformationCache.getXSLTranformationDocByXMLPath(
                    fixture.xmlPath, fixture.failingXslPath));
        });
    }
}
