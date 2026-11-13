package com.dotmarketing.portlets.director;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Checks the Struts and Tiles configuration of the webapp for the legacy director action: no
 * action is mapped to {@code /ext/director/direct}, no Tiles definition belongs to the director
 * portlet.
 */
class LegacyDirectorWebConfigTest {

    private static final String STRUTS_CONFIG_PATH = "src/main/webapp/WEB-INF/struts-config.xml";
    private static final String TILES_DEFS_PATH = "src/main/webapp/WEB-INF/tiles-defs.xml";
    private static final String DIRECTOR_PATH = "/ext/director/direct";
    private static final String DIRECTOR_CLASS =
            "com.dotmarketing.portlets.director.action.DirectorAction";
    private static final String DIRECTOR_TILES_PREFIX = "portlet.ext.director";

    private static final List<String> actionPaths = new ArrayList<>();
    private static final List<String> actionTypes = new ArrayList<>();
    private static final List<String> tilesDefinitions = new ArrayList<>();

    /**
     * Reads the {@code path} and {@code type} of every {@code <action>} in
     * {@code struts-config.xml}, and the name of every {@code <definition>} in
     * {@code tiles-defs.xml}.
     */
    @BeforeAll
    static void loadConfiguration() throws Exception {
        final NodeList actions = parse(STRUTS_CONFIG_PATH).getElementsByTagName("action");
        for (int i = 0; i < actions.getLength(); i++) {
            final Element action = (Element) actions.item(i);
            actionPaths.add(action.getAttribute("path").trim());
            final String type = action.getAttribute("type").trim();
            if (!type.isEmpty()) {
                actionTypes.add(type);
            }
        }
        final NodeList definitions = parse(TILES_DEFS_PATH).getElementsByTagName("definition");
        for (int i = 0; i < definitions.getLength(); i++) {
            tilesDefinitions.add(((Element) definitions.item(i)).getAttribute("name").trim());
        }
    }

    /**
     * Method to test: the actions mapped in {@code struts-config.xml}
     * Given: the webapp Struts configuration
     * Expected: no action is mapped to the director path, and none is handled by the director
     * class
     */
    @Test
    void strutsConfig_mapsNoDirectorAction() {
        assertTrue(actionPaths.size() > 1, "struts-config.xml was parsed: " + actionPaths);
        assertFalse(actionPaths.contains(DIRECTOR_PATH),
                "no action is mapped to " + DIRECTOR_PATH);
        assertFalse(actionTypes.contains(DIRECTOR_CLASS),
                "no action is handled by " + DIRECTOR_CLASS);
    }

    /**
     * Method to test: the definitions in {@code tiles-defs.xml}
     * Given: the webapp Tiles configuration
     * Expected: no definition belongs to the director portlet
     */
    @Test
    void tilesDefs_defineNoDirectorPage() {
        assertTrue(tilesDefinitions.size() > 1, "tiles-defs.xml was parsed: " + tilesDefinitions);
        final List<String> director = tilesDefinitions.stream()
                .filter(name -> name.startsWith(DIRECTOR_TILES_PREFIX))
                .toList();
        assertTrue(director.isEmpty(), "no Tiles definition for the director portlet: " + director);
    }

    /**
     * Parses a webapp XML file without fetching the DTD it declares.
     *
     * @param path the file path, relative to the module
     * @return the parsed document
     */
    private static Document parse(final String path) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new File(path));
    }
}
