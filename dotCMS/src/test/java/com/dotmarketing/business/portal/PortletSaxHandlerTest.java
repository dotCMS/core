package com.dotmarketing.business.portal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.liferay.portal.model.Portlet;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Tests the runtime path that reads {@code portlet.xml}: {@link PortletSaxHandler} rebuilds each
 * {@code <portlet>} element as a string and hands it to a mapper, which for
 * {@link PortletFactoryImpl#xmlToPortlets(InputStream)} parses it into a {@link Portlet}. The
 * rebuilt string has to be well-formed XML carrying the same values as the file, or the mapper
 * drops the portlet with only a debug log line.
 * <p>
 * {@code SerializationHelperTest} reads the same file through a different reader, which is why it
 * did not notice when {@code roles-legacy} went missing at runtime.
 */
public class PortletSaxHandlerTest {

    private static final String PORTLET_XML = "src/main/webapp/WEB-INF/portlet.xml";

    /**
     * Given Scenario: the shipped {@code portlet.xml} is loaded the way dotCMS loads it at startup.
     * Expected Result: {@code roles-legacy}, whose display name contains an ampersand, is among the
     * portlets, with its JSP intact.
     */
    @Test
    public void xmlToPortlets_realPortletXml_keepsRolesLegacy() throws Exception {
        final Map<String, Portlet> portlets;
        try (InputStream stream = new FileInputStream(PORTLET_XML)) {
            portlets = new PortletFactoryImpl(new String[0]).xmlToPortlets(stream);
        }

        final Portlet rolesLegacy = portlets.get("roles-legacy");
        assertNotNull("roles-legacy must survive the runtime portlet.xml reader", rolesLegacy);
        assertEquals("/portlet/ext/roleadmin/view_roles.jsp", rolesLegacy.getInitParams().get("view-jsp"));
        assertTrue("users-legacy must be there too", portlets.containsKey("users-legacy"));
    }

    /**
     * Given Scenario: the shipped {@code portlet.xml} goes through the handler, and the rebuilt
     * {@code roles-legacy} element is read back with a standard XML parser.
     * Expected Result: the element is well-formed and its display name is exactly
     * {@code Roles & Tools (Legacy)}, as written in the file.
     */
    @Test
    public void handler_realPortletXml_rebuildsRolesLegacyWithItsDisplayName() throws Exception {
        final Map<String, String> rebuilt;
        try (InputStream stream = new FileInputStream(PORTLET_XML)) {
            rebuilt = rebuildEachPortlet(stream);
        }

        final String rolesLegacy = rebuilt.get("roles-legacy");
        assertNotNull("the handler must rebuild roles-legacy", rolesLegacy);
        assertEquals("Roles & Tools (Legacy)", childText(parse(rolesLegacy), "display-name"));
    }

    /**
     * Given Scenario: a portlet whose element text and attribute value hold {@code &}, {@code <},
     * {@code >} and, in the attribute, {@code "}, all escaped as the XML requires.
     * Expected Result: the rebuilt element parses, and the text and attribute come back with the
     * same characters as the source.
     */
    @Test
    public void handler_specialCharactersInTextAndAttribute_survive() throws Exception {
        final String xml = "<portlet-app>"
                + "<portlet note=\"a &amp; &lt;b&gt; &quot;c&quot;\">"
                + "<portlet-name>special</portlet-name>"
                + "<display-name>Fish &amp; Chips &lt;Legacy&gt;</display-name>"
                + "<portlet-class>com.liferay.portlet.JSPPortlet</portlet-class>"
                + "<init-param><name>view-jsp</name><value>/a.jsp?x=1&amp;y=&lt;2&gt;</value></init-param>"
                + "</portlet>"
                + "</portlet-app>";

        final String rebuilt = rebuildEachPortlet(stream(xml)).get("special");
        assertNotNull("the handler must rebuild the portlet", rebuilt);

        final Element portlet = parse(rebuilt);
        assertEquals("a & <b> \"c\"", portlet.getAttribute("note"));
        assertEquals("Fish & Chips <Legacy>", childText(portlet, "display-name"));
        assertEquals("/a.jsp?x=1&y=<2>", childText(portlet, "value"));
    }

    /**
     * Given Scenario: the same portlet goes through {@link PortletFactoryImpl#xmlToPortlets(InputStream)}.
     * Expected Result: it is kept, and its init-param carries the decoded value.
     */
    @Test
    public void xmlToPortlets_specialCharactersInText_keepsThePortlet() throws Exception {
        final String xml = "<portlet-app>"
                + "<portlet>"
                + "<portlet-name>special</portlet-name>"
                + "<display-name>Fish &amp; Chips &lt;Legacy&gt;</display-name>"
                + "<portlet-class>com.liferay.portlet.JSPPortlet</portlet-class>"
                + "<init-param><name>view-jsp</name><value>/a.jsp?x=1&amp;y=&lt;2&gt;</value></init-param>"
                + "</portlet>"
                + "</portlet-app>";

        final Portlet portlet = new PortletFactoryImpl(new String[0]).xmlToPortlets(stream(xml)).get("special");

        assertNotNull("a portlet with escaped characters must not be dropped", portlet);
        assertEquals("/a.jsp?x=1&y=<2>", portlet.getInitParams().get("view-jsp"));
    }

    /**
     * Runs the handler over the given XML with a mapper that keeps the rebuilt element as is.
     *
     * @param xml the XML to read
     * @return each portlet id mapped to the element string the handler rebuilt for it
     * @throws Exception if the XML cannot be parsed
     */
    private static Map<String, String> rebuildEachPortlet(final InputStream xml) throws Exception {
        final PortletSaxHandler<String> handler = new PortletSaxHandler<>(Optional::of);
        ThreadLocalSaxParserFactory.getSaxParser().parse(xml, handler);
        return handler.getValueMap();
    }

    /**
     * Parses one rebuilt element with a standard DOM parser, failing the test when it is not
     * well-formed.
     *
     * @param element the element string
     * @return its root {@link Element}
     * @throws Exception if the string is not well-formed XML
     */
    private static Element parse(final String element) throws Exception {
        final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(element)));
        return document.getDocumentElement();
    }

    /**
     * Reads the text of the first descendant with the given tag.
     *
     * @param parent the element to search
     * @param tag    the tag name
     * @return the descendant's text content
     */
    private static String childText(final Element parent, final String tag) {
        return parent.getElementsByTagName(tag).item(0).getTextContent();
    }

    /**
     * Wraps a string as a UTF-8 stream.
     *
     * @param xml the XML text
     * @return a stream over it
     */
    private static InputStream stream(final String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}
