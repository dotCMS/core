package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.xml.XMLConstants;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.util.Config;
import java.io.StringReader;
import java.util.Optional;
import java.io.StringWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.junit.Test;

/**
 * Covers the settings {@link XsltTool} applies to the XSLT engine before it runs a stylesheet.
 *
 * <p>The tool is meant to transform XML and nothing else, so the engine it builds must keep a
 * stylesheet within XSLT and must do so by its own configuration rather than by whatever the
 * runtime happens to default to.
 */
public class XsltToolTest {

    private static final String XML = "<root><name>dotCMS</name></root>";

    /** JVM-wide switch that puts extension functions back on, over what the factory asks for. */
    private static final String ENABLE_EXTENSION_FUNCTIONS = "jdk.xml.enableExtensionFunctions";

    private static final String ENABLE_SCRIPTING = "ENABLE_SCRIPTING";

    /** Uses an XSLT extension function, which is outside what this tool allows. */
    private static final String STYLESHEET_WITH_EXTENSION_FUNCTION =
            "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'"
                    + " xmlns:sys='http://xml.apache.org/xalan/java/java.lang.System'>"
                    + "<xsl:template match='/'><out>"
                    + "<xsl:value-of select=\"sys:getProperty('java.version')\"/>"
                    + "</out></xsl:template></xsl:stylesheet>";

    private static final String PLAIN_STYLESHEET =
            "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
                    + "<xsl:template match='/'><out><xsl:value-of select='/root/name'/></out>"
                    + "</xsl:template></xsl:stylesheet>";

    /**
     * The factory asks for secure processing itself rather than inheriting whatever the runtime
     * happens to default to.
     *
     * <p>Worth knowing when reading a green run: on runtimes whose own defaults already match
     * these settings, the class passes whether or not the factory sets them.
     */
    @Test
    public void test_factory_asks_for_secure_processing() throws Exception {
        assertTrue("Secure processing must be on",
                XsltTool.restrictedTransformerFactory()
                        .getFeature(XMLConstants.FEATURE_SECURE_PROCESSING));
    }

    /**
     * A stylesheet that uses an extension function must be refused, whichever runtime this is
     * compiled and run on.
     */
    @Test
    public void test_extension_function_is_refused() throws Exception {
        final TransformerFactory factory = XsltTool.restrictedTransformerFactory();
        final Transformer transformer =
                factory.newTransformer(new StreamSource(new StringReader(STYLESHEET_WITH_EXTENSION_FUNCTION)));
        final StringWriter output = new StringWriter();

        try {
            transformer.transform(new StreamSource(new StringReader(XML)), new StreamResult(output));
            fail("Expected the extension function to be refused, got: " + output);
        } catch (TransformerException e) {
            // The message is localised by the JDK, so the assertion is on what came out instead.
            assertFalse("The call ran anyway", output.toString().contains("<out>"));
        }
    }

    /**
     * The check behind the warning has to agree with what the engine really does: where the call is
     * refused, it must report that extension functions do not run.
     */
    @Test
    public void test_probe_reports_extension_functions_are_off() {
        assertFalse("The engine still allows extension functions",
                XsltTool.extensionFunctionsStillRun());
    }

    /**
     * The case the warning exists for, forced.
     *
     * <p>A JVM started with {@code jdk.xml.enableExtensionFunctions} set puts extension functions
     * back on over what the factory asks for, so asking for secure processing is not enough on its
     * own and the tool has to find out by running something. Without this, the test above is green
     * whether or not the factory sets anything, because it cannot tell a refusal apart from an
     * engine it declined to read.
     *
     * <p>The property is read each time a factory is built, not once per JVM, so setting it here
     * takes effect even though other tests in this class have already built one.
     */
    @Test
    public void test_probe_reports_extension_functions_that_the_jvm_turned_back_on() {
        final String previous = System.getProperty(ENABLE_EXTENSION_FUNCTIONS);
        try {
            System.setProperty(ENABLE_EXTENSION_FUNCTIONS, "true");
            assertTrue("The engine runs extension functions but the check did not say so",
                    XsltTool.extensionFunctionsStillRun());
        } finally {
            if (previous == null) {
                System.clearProperty(ENABLE_EXTENSION_FUNCTIONS);
            } else {
                System.setProperty(ENABLE_EXTENSION_FUNCTIONS, previous);
            }
        }
    }

    /**
     * The restrictions must not get in the way of what the tool is for: an ordinary stylesheet,
     * with no Java in it, still transforms.
     */
    @Test
    public void test_plain_stylesheet_still_transforms() throws Exception {
        final TransformerFactory factory = XsltTool.restrictedTransformerFactory();
        final Transformer transformer =
                factory.newTransformer(new StreamSource(new StringReader(PLAIN_STYLESHEET)));
        final StringWriter output = new StringWriter();

        transformer.transform(new StreamSource(new StringReader(XML)), new StreamResult(output));

        assertTrue("Expected the transformed value, got: " + output,
                output.toString().contains("dotCMS"));
    }

    /**
     * Reading the XML from a URL is off unless it is turned on, and what gets logged has to point
     * whoever upgraded at the switch instead of at a missing file.
     */
    @Test
    public void test_remote_xml_is_refused_while_the_switch_is_off() {
        assertTrue("A URL should be recognised as a remote source",
                XsltTool.isRemoteSource("http://example.com/feed.xml"));
        assertFalse("A dotCMS path is not a remote source",
                XsltTool.isRemoteSource("/application/feed.xml"));

        assertEquals("The URL is not read while the switch is off",
                Optional.empty(), new XsltTool().loadRemoteXML("http://example.com/feed.xml"));

        final String message = XsltTool.remoteSourceDisabledMessage("http://example.com/feed.xml");
        assertTrue("The message does not name the switch: " + message,
                message.contains(FeatureFlagName.FEATURE_FLAG_XSLT_REMOTE_XML));
        assertTrue("The message does not name the URL: " + message,
                message.contains("http://example.com/feed.xml"));
    }

    /**
     * Whatever makes the tool decline to transform, the template gets an empty string back and the
     * page keeps rendering. Scripting being off is the cheapest way to reach that path; a URL
     * source with remote sources off reaches the same return further down.
     */
    @Test
    public void test_transform_renders_nothing_rather_than_failing_the_page() throws Exception {
        final boolean enableScripting = Config.getBooleanProperty(ENABLE_SCRIPTING, false);
        try {
            Config.setProperty(ENABLE_SCRIPTING, false);
            assertEquals("Declining to transform must not break the render", "",
                    new XsltTool().transform("/application/feed.xml", "/application/feed.xsl", 30));
        } finally {
            Config.setProperty(ENABLE_SCRIPTING, enableScripting);
        }
    }
}
