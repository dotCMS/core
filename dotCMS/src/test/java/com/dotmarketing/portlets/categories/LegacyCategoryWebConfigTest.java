package com.dotmarketing.portlets.categories;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Checks the webapp configuration in {@code WEB-INF} for the categories admin: the Angular
 * {@code categories} portlet is the only one declared, and none of the legacy category screens,
 * nor the {@code /categoriesServlet} JSON endpoint, are registered.
 */
class LegacyCategoryWebConfigTest {

    private static final String STRUTS_CONFIG_PATH = "src/main/webapp/WEB-INF/struts-config.xml";
    private static final String TILES_DEFS_PATH = "src/main/webapp/WEB-INF/tiles-defs.xml";
    private static final String WEB_XML_PATH = "src/main/webapp/WEB-INF/web.xml";
    private static final String PORTLET_XML_PATH = "src/main/webapp/WEB-INF/portlet.xml";

    private static List<String> actionPaths;
    private static List<String> tilesDefinitionNames;
    private static List<String> servletClasses;
    private static List<String> servletUrlPatterns;
    private static List<String> portletNames;

    @BeforeAll
    static void loadConfiguration() throws Exception {
        actionPaths = values(STRUTS_CONFIG_PATH, "action", element -> element.getAttribute("path"));
        tilesDefinitionNames = values(TILES_DEFS_PATH, "definition",
                element -> element.getAttribute("name"));
        servletClasses = values(WEB_XML_PATH, "servlet-class", Element::getTextContent);
        servletUrlPatterns = values(WEB_XML_PATH, "url-pattern", Element::getTextContent);
        portletNames = values(PORTLET_XML_PATH, "portlet-name", Element::getTextContent);
    }

    /**
     * Method to test: the Struts action mappings in {@code struts-config.xml}
     * Given: the webapp Struts configuration
     * Expected: no action is mapped under {@code /ext/categories/}
     */
    @Test
    void strutsConfig_mapsNoLegacyCategoryScreen() {
        assertTrue(actionPaths.stream().noneMatch(path -> path.startsWith("/ext/categories/")),
                "no Struts action is mapped under /ext/categories/: " + actionPaths.stream()
                        .filter(path -> path.startsWith("/ext/categories/")).toList());
    }

    /**
     * Method to test: the tiles definitions in {@code tiles-defs.xml}
     * Given: the webapp tiles configuration
     * Expected: no definition is named {@code portlet.ext.categories} or starts with
     * {@code portlet.ext.categories.}
     */
    @Test
    void tilesDefs_defineNoLegacyCategoryScreen() {
        final List<String> categoryDefinitions = tilesDefinitionNames.stream()
                .filter(name -> name.equals("portlet.ext.categories")
                        || name.startsWith("portlet.ext.categories."))
                .toList();
        assertTrue(categoryDefinitions.isEmpty(),
                "no legacy category tiles definition: " + categoryDefinitions);
    }

    /**
     * Method to test: the servlets registered in {@code web.xml}
     * Given: the webapp deployment descriptor
     * Expected: neither the categories JSON servlet class nor the {@code /categoriesServlet} path
     * is registered
     */
    @Test
    void webXml_registersNoCategoriesServlet() {
        assertFalse(servletClasses.contains("com.dotmarketing.servlets.JSONCategoriesServlet"),
                "the categories JSON servlet is not registered");
        assertFalse(servletUrlPatterns.contains("/categoriesServlet"),
                "no servlet is mapped to /categoriesServlet");
    }

    /**
     * Method to test: the portlets declared in {@code portlet.xml}
     * Given: the webapp portlet configuration
     * Expected: the Angular {@code categories} portlet is declared and {@code categories-legacy}
     * is not
     */
    @Test
    void portletXml_declaresTheCategoriesPortletOnly() {
        assertTrue(portletNames.contains("categories"), "the categories portlet is declared");
        assertFalse(portletNames.contains("categories-legacy"),
                "the categories-legacy portlet is not declared");
    }

    /**
     * Reads one value from every element with the given tag name in a webapp XML file, without
     * loading the DTD named in its {@code DOCTYPE}. Values are trimmed.
     */
    private static List<String> values(final String path, final String tagName,
            final Function<Element, String> reader) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setExpandEntityReferences(false);
        final Document document = factory.newDocumentBuilder().parse(new File(path));

        final NodeList elements = document.getElementsByTagName(tagName);
        final List<String> values = new ArrayList<>();
        for (int i = 0; i < elements.getLength(); i++) {
            values.add(reader.apply((Element) elements.item(i)).trim());
        }
        assertFalse(values.isEmpty(), "no <" + tagName + "> element found in " + path);
        return values;
    }
}
