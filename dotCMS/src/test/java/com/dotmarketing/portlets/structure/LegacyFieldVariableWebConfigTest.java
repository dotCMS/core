package com.dotmarketing.portlets.structure;

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
 * Checks the DWR configuration in {@code WEB-INF/dwr.xml} for content type field variables: they are
 * managed through the {@code /api/v1/contenttype/.../variables} REST resource only, so no DWR script
 * for them is registered, and every class that {@code dwr.xml} still registers exists.
 */
class LegacyFieldVariableWebConfigTest {

    private static final String DWR_XML_PATH = "src/main/webapp/WEB-INF/dwr.xml";
    private static final String FIELD_VARIABLE_SCRIPT = "FieldVariableAjax";
    private static final String FIELD_VARIABLE_CLASS =
            "com.dotmarketing.portlets.structure.ajax.FieldVariableAjax";

    private static final List<String> scriptNames = new ArrayList<>();
    private static final List<String> registeredClasses = new ArrayList<>();

    /**
     * Reads every {@code <create>} element of {@code dwr.xml}: its {@code javascript} name and the
     * value of its {@code class} parameter.
     */
    @BeforeAll
    static void loadConfiguration() throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // dwr.xml declares a DOCTYPE; do not fetch its DTD
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setExpandEntityReferences(false);
        final Document document = factory.newDocumentBuilder().parse(new File(DWR_XML_PATH));

        final NodeList creates = document.getElementsByTagName("create");
        for (int i = 0; i < creates.getLength(); i++) {
            final Element create = (Element) creates.item(i);
            scriptNames.add(create.getAttribute("javascript"));
            final NodeList params = create.getElementsByTagName("param");
            for (int j = 0; j < params.getLength(); j++) {
                final Element param = (Element) params.item(j);
                if ("class".equals(param.getAttribute("name"))) {
                    registeredClasses.add(param.getAttribute("value").trim());
                }
            }
        }
    }

    /**
     * Method to test: the DWR scripts registered in {@code dwr.xml}
     * Given: the webapp DWR configuration
     * Expected: no script and no class for content type field variables is registered; they are
     * read and written through the REST resource only
     */
    @Test
    void dwrXml_registersNoFieldVariableScript() {
        assertFalse(scriptNames.contains(FIELD_VARIABLE_SCRIPT),
                "no DWR script named " + FIELD_VARIABLE_SCRIPT + " is registered");
        assertFalse(registeredClasses.contains(FIELD_VARIABLE_CLASS),
                "no DWR script is backed by " + FIELD_VARIABLE_CLASS);
    }

    /**
     * Method to test: the classes registered in {@code dwr.xml}
     * Given: the webapp DWR configuration
     * Expected: every registered class can be loaded, so removing a DWR class without removing its
     * registration (or the reverse mistake in a rename) is caught at build time
     */
    @Test
    void dwrXml_everyRegisteredClassExists() {
        assertTrue(registeredClasses.size() > 1, "dwr.xml was parsed: " + registeredClasses);
        final List<String> missing = registeredClasses.stream()
                .filter(className -> !loadable(className))
                .toList();
        assertTrue(missing.isEmpty(), "every class registered in dwr.xml exists: " + missing);
    }

    /**
     * Tells whether a class can be found on the test classpath, without initializing it.
     *
     * @param className the fully qualified class name
     * @return {@code true} when the class can be loaded
     */
    private static boolean loadable(final String className) {
        try {
            Class.forName(className, false, LegacyFieldVariableWebConfigTest.class.getClassLoader());
            return true;
        } catch (final ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
