package com.dotcms.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.dotcms.config.DotInitializer;
import java.io.File;
import java.util.ServiceLoader;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Verifies the deployment registrations needed to initialize and expose management metrics.
 * Servlet-only tests cannot detect a missing route or an unregistered startup initializer.
 */
public class MetricsDeploymentConfigurationTest {

    /**
     * Given the shipped descriptor, the existing metrics servlet must own the management
     * metrics path; otherwise a running application returns 404 before reaching the servlet.
     */
    @Test
    public void testWebXml_mapsManagementMetricsToExistingServlet() throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        final Document descriptor = factory.newDocumentBuilder().parse(
                moduleFile("src/main/webapp/WEB-INF/web.xml"));
        final XPath xpath = XPathFactory.newInstance().newXPath();

        final String servletName = xpath.evaluate(
                "/web-app/servlet[servlet-class="
                        + "'com.dotcms.metrics.servlet.ManagementMetricsServlet']/servlet-name",
                descriptor).trim();
        assertFalse("ManagementMetricsServlet must be declared in web.xml", servletName.isEmpty());

        final String mappedName = xpath.evaluate(
                "/web-app/servlet-mapping[url-pattern='/dotmgt/metrics']/servlet-name",
                descriptor).trim();
        assertEquals("The management metrics path must resolve to the metrics servlet",
                servletName, mappedName);
    }

    /**
     * Given the startup service-loader mechanism, the existing metrics initializer must be
     * registered exactly once so metrics initialize before the first scrape.
     */
    @Test
    public void testServiceLoader_registersMetricsInitializerOnce() throws Exception {
        final long registrations = ServiceLoader.load(DotInitializer.class).stream()
                .map(provider -> provider.type().getName())
                .filter("com.dotcms.metrics.init.MetricsInitializer"::equals)
                .count();
        assertEquals("MetricsInitializer must be a single discoverable startup service provider",
                1L, registrations);
    }

    /** Finds a module file relative to target/test-classes, independent of the working directory. */
    private static File moduleFile(final String path) throws Exception {
        final File testClasses = new File(MetricsDeploymentConfigurationTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        return new File(testClasses, "../../" + path).getCanonicalFile();
    }
}
