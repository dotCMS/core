package com.dotcms.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Checks how {@code web.xml} declares the {@code HttpHeaderSecurityFilter}, so that reverting the
 * block to Tomcat's stock filter or to a fixed {@code max-age} fails a test.
 */
public class HttpHeaderSecurityFilterWebXmlTest {

    private static final String FILTER_NAME = "HttpHeaderSecurityFilter";

    /**
     * Method to test: the HttpHeaderSecurityFilter declaration in WEB-INF/web.xml
     * Given scenario: the web.xml shipped in this module
     * Expected result: the filter uses {@link DotHttpHeaderSecurityFilter}, keeps
     * hstsIncludeSubDomains=true and does not declare hstsMaxAgeSeconds
     */
    @Test
    public void testWebXml_shouldWireDotHttpHeaderSecurityFilter() throws Exception {
        final Element filter = findFilter(parseWebXml());

        assertEquals(DotHttpHeaderSecurityFilter.class.getName(), childText(filter, "filter-class"));
        final Map<String, String> initParams = initParams(filter);
        assertEquals("true", initParams.get("hstsIncludeSubDomains"));
        assertFalse("hstsMaxAgeSeconds would override HSTS_MAX_AGE_SECONDS",
                initParams.containsKey("hstsMaxAgeSeconds"));
    }

    /**
     * Parses the module's web.xml, found relative to the compiled test classes
     * ({@code target/test-classes}) so the result does not depend on the working directory.
     */
    private static Document parseWebXml() throws Exception {
        final File testClasses = new File(HttpHeaderSecurityFilterWebXmlTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        final File webXml = new File(testClasses,
                "../../src/main/webapp/WEB-INF/web.xml").getCanonicalFile();
        assertTrue("web.xml not found at " + webXml, webXml.isFile());

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(webXml);
    }

    /** Returns the {@code <filter>} element whose name is {@value #FILTER_NAME}. */
    private static Element findFilter(final Document webXml) {
        final NodeList filters = webXml.getElementsByTagName("filter");
        for (int i = 0; i < filters.getLength(); i++) {
            final Element filter = (Element) filters.item(i);
            if (FILTER_NAME.equals(childText(filter, "filter-name"))) {
                return filter;
            }
        }
        throw new AssertionError(FILTER_NAME + " is not declared in web.xml");
    }

    /** Collects the filter's init-params as name to value. */
    private static Map<String, String> initParams(final Element filter) {
        final Map<String, String> params = new HashMap<>();
        final NodeList nodes = filter.getElementsByTagName("init-param");
        for (int i = 0; i < nodes.getLength(); i++) {
            final Element param = (Element) nodes.item(i);
            params.put(childText(param, "param-name"), childText(param, "param-value"));
        }
        return params;
    }

    /** Returns the trimmed text of the first child element with the given tag. */
    private static String childText(final Element parent, final String tag) {
        final NodeList nodes = parent.getElementsByTagName(tag);
        assertNotNull(tag, nodes.item(0));
        return nodes.item(0).getTextContent().trim();
    }

}
