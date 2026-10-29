package com.dotmarketing.portlets.categories;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Checks the Struts configuration of the categories portlet in {@code WEB-INF}: the category list
 * screen is mapped, and the category edit and single-category screens have neither a Struts
 * action nor a tiles definition.
 */
class CategoryPortletStrutsConfigTest {

    private static final String STRUTS_CONFIG_PATH = "src/main/webapp/WEB-INF/struts-config.xml";
    private static final String TILES_DEFS_PATH = "src/main/webapp/WEB-INF/tiles-defs.xml";

    private static List<String> actionPaths;
    private static List<String> tilesDefinitionNames;

    @BeforeAll
    static void loadConfiguration() throws Exception {
        actionPaths = attributeValues(STRUTS_CONFIG_PATH, "action", "path");
        tilesDefinitionNames = attributeValues(TILES_DEFS_PATH, "definition", "name");
    }

    /**
     * Method to test: the Struts action mappings in {@code struts-config.xml}
     * Given: the webapp Struts configuration
     * Expected: the category list action is mapped, and no action is mapped for the category edit,
     * single-category and edit-page paths
     */
    @Test
    void strutsConfig_mapsTheCategoryListButNoCategoryEditScreen() {
        assertTrue(actionPaths.contains("/ext/categories/view_categories"),
                "the category list action is mapped");
        for (final String path : List.of("/ext/categories/edit_category",
                "/ext/categories/view_category", "/ext/categories/edit_category_page")) {
            assertEquals(0, actionPaths.stream().filter(path::equals).count(),
                    "no Struts action is mapped to " + path);
        }
    }

    /**
     * Method to test: the tiles definitions in {@code tiles-defs.xml}
     * Given: the webapp tiles configuration
     * Expected: the category list screen and the categories definition it extends are defined,
     * and the category edit and single-category screens are not
     */
    @Test
    void tilesDefs_defineTheCategoryListButNoCategoryEditScreen() {
        assertTrue(tilesDefinitionNames.contains("portlet.ext.categories"),
                "the categories definition that the list screen extends is defined");
        assertTrue(tilesDefinitionNames.contains("portlet.ext.categories.view_categories"),
                "the category list screen is defined");
        for (final String name : List.of("portlet.ext.categories.edit_category",
                "portlet.ext.categories.view_category")) {
            assertEquals(0, tilesDefinitionNames.stream().filter(name::equals).count(),
                    "no tiles definition is named " + name);
        }
    }

    /**
     * Reads the given attribute of every element with the given tag name in a webapp XML file,
     * without loading the DTD named in its {@code DOCTYPE}.
     */
    private static List<String> attributeValues(final String path, final String tagName,
            final String attribute) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setExpandEntityReferences(false);
        final Document document = factory.newDocumentBuilder().parse(new File(path));

        final NodeList elements = document.getElementsByTagName(tagName);
        final List<String> values = new ArrayList<>();
        for (int i = 0; i < elements.getLength(); i++) {
            values.add(((Element) elements.item(i)).getAttribute(attribute));
        }
        assertFalse(values.isEmpty(), "no <" + tagName + "> element found in " + path);
        return values;
    }
}
