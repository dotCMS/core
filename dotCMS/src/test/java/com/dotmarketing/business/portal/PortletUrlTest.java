package com.dotmarketing.business.portal;

import com.liferay.portal.model.Portlet;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test class for the portlet-url feature.
 * Tests both backward compatibility (without portlet-url) and new functionality (with portlet-url).
 */
public class PortletUrlTest {

    /**
     * Test XML parsing for portlet WITHOUT portlet-url tag (backward compatibility)
     */
    @Test
    public void testXmlToPortlet_WithoutPortletUrl() throws IOException {
        String xml = "<portlet>" +
                "<portlet-name>test-portlet</portlet-name>" +
                "<portlet-class>com.example.TestPortlet</portlet-class>" +
                "</portlet>";

        DotPortlet dotPortlet = DotPortlet.builder().fromXml(xml);

        assertNotNull("DotPortlet should not be null", dotPortlet);
        assertEquals("test-portlet", dotPortlet.getPortletId());
        assertEquals("com.example.TestPortlet", dotPortlet.getPortletClass());
        assertNull("portletUrl should be null when not present in XML", dotPortlet.getPortletUrl());
    }

    /**
     * Test XML parsing for portlet WITH portlet-url tag
     */
    @Test
    public void testXmlToPortlet_WithPortletUrl() throws IOException {
        String xml = "<portlet>" +
                "<portlet-name>content-types-angular</portlet-name>" +
                "<portlet-class>com.dotcms.spring.portlet.PortletController</portlet-class>" +
                "<portlet-url>contentTypeAngular</portlet-url>" +
                "</portlet>";

        DotPortlet dotPortlet = DotPortlet.builder().fromXml(xml);

        assertNotNull("DotPortlet should not be null", dotPortlet);
        assertEquals("content-types-angular", dotPortlet.getPortletId());
        assertEquals("com.dotcms.spring.portlet.PortletController", dotPortlet.getPortletClass());
        assertEquals("contentTypeAngular", dotPortlet.getPortletUrl());
    }

    /**
     * Test DotPortlet to Portlet conversion WITHOUT portlet-url
     */
    @Test
    public void testToPortlet_WithoutPortletUrl() {
        Map<String, String> initParams = new HashMap<>();
        initParams.put("param1", "value1");

        DotPortlet dotPortlet = DotPortlet.builder()
                .portletId("test-portlet")
                .portletClass("com.example.TestPortlet")
                .initParams(initParams)
                .build();

        Portlet portlet = dotPortlet.toPortlet();

        assertNotNull("Portlet should not be null", portlet);
        assertEquals("test-portlet", portlet.getPortletId());
        assertEquals("com.example.TestPortlet", portlet.getPortletClass());
        assertNull("portletUrl should be null", portlet.getPortletUrl());
    }

    /**
     * Test DotPortlet to Portlet conversion WITH portlet-url
     */
    @Test
    public void testToPortlet_WithPortletUrl() {
        Map<String, String> initParams = new HashMap<>();
        initParams.put("param1", "value1");

        DotPortlet dotPortlet = DotPortlet.builder()
                .portletId("content-types-angular")
                .portletClass("com.dotcms.spring.portlet.PortletController")
                .portletUrl("contentTypeAngular")
                .initParams(initParams)
                .build();

        Portlet portlet = dotPortlet.toPortlet();

        assertNotNull("Portlet should not be null", portlet);
        assertEquals("content-types-angular", portlet.getPortletId());
        assertEquals("com.dotcms.spring.portlet.PortletController", portlet.getPortletClass());
        assertEquals("contentTypeAngular", portlet.getPortletUrl());
    }

    /**
     * Test Portlet to DotPortlet conversion WITHOUT portlet-url
     */
    @Test
    public void testFromPortlet_WithoutPortletUrl() {
        Map<String, String> initParams = new HashMap<>();
        initParams.put("param1", "value1");

        Portlet portlet = new Portlet("test-portlet", "com.example.TestPortlet", initParams);

        DotPortlet dotPortlet = DotPortlet.from(portlet);

        assertNotNull("DotPortlet should not be null", dotPortlet);
        assertEquals("test-portlet", dotPortlet.getPortletId());
        assertEquals("com.example.TestPortlet", dotPortlet.getPortletClass());
        assertNull("portletUrl should be null", dotPortlet.getPortletUrl());
    }

    /**
     * Test Portlet to DotPortlet conversion WITH portlet-url
     */
    @Test
    public void testFromPortlet_WithPortletUrl() {
        Map<String, String> initParams = new HashMap<>();
        initParams.put("param1", "value1");

        Portlet portlet = new Portlet("content-types-angular",
                "com.dotcms.spring.portlet.PortletController",
                initParams,
                "contentTypeAngular");

        DotPortlet dotPortlet = DotPortlet.from(portlet);

        assertNotNull("DotPortlet should not be null", dotPortlet);
        assertEquals("content-types-angular", dotPortlet.getPortletId());
        assertEquals("com.dotcms.spring.portlet.PortletController", dotPortlet.getPortletClass());
        assertEquals("contentTypeAngular", dotPortlet.getPortletUrl());
    }

    /**
     * Test XML serialization/deserialization round-trip WITHOUT portlet-url
     */
    @Test
    public void testXmlRoundTrip_WithoutPortletUrl() throws IOException {
        Map<String, String> initParams = new HashMap<>();
        initParams.put("param1", "value1");

        DotPortlet original = DotPortlet.builder()
                .portletId("test-portlet")
                .portletClass("com.example.TestPortlet")
                .initParams(initParams)
                .build();

        String xml = original.toXml();
        DotPortlet deserialized = DotPortlet.builder().fromXml(xml);

        // Note: XML serialization may convert null to empty string, both are acceptable for optional fields
        assertEquals("portletId should match", original.getPortletId(), deserialized.getPortletId());
        assertEquals("portletClass should match", original.getPortletClass(), deserialized.getPortletClass());
        assertEquals("initParams should match", original.initParams(), deserialized.initParams());

        // portletUrl can be null or empty string when not present
        String portletUrl = deserialized.getPortletUrl();
        assertTrue("portletUrl should be null or empty",
                portletUrl == null || portletUrl.isEmpty());
    }

    /**
     * Test XML serialization/deserialization round-trip WITH portlet-url
     */
    @Test
    public void testXmlRoundTrip_WithPortletUrl() throws IOException {
        Map<String, String> initParams = new HashMap<>();
        initParams.put("param1", "value1");

        DotPortlet original = DotPortlet.builder()
                .portletId("content-types-angular")
                .portletClass("com.dotcms.spring.portlet.PortletController")
                .portletUrl("contentTypeAngular")
                .initParams(initParams)
                .build();

        String xml = original.toXml();
        DotPortlet deserialized = DotPortlet.builder().fromXml(xml);

        assertEquals("Original and deserialized should be equal", original, deserialized);
        assertEquals("contentTypeAngular", deserialized.getPortletUrl());
    }

    /**
     * Test Builder.from() method WITHOUT portlet-url
     */
    @Test
    public void testBuilderFrom_WithoutPortletUrl() {
        Map<String, String> initParams = new HashMap<>();
        initParams.put("param1", "value1");

        Portlet portlet = new Portlet("test-portlet", "com.example.TestPortlet", initParams);

        DotPortlet dotPortlet = DotPortlet.builder()
                .from(portlet)
                .build();

        assertEquals("test-portlet", dotPortlet.getPortletId());
        assertEquals("com.example.TestPortlet", dotPortlet.getPortletClass());
        assertNull("portletUrl should be null", dotPortlet.getPortletUrl());
    }

    /**
     * Test Builder.from() method WITH portlet-url
     */
    @Test
    public void testBuilderFrom_WithPortletUrl() {
        Map<String, String> initParams = new HashMap<>();
        initParams.put("param1", "value1");

        Portlet portlet = new Portlet("content-types-angular",
                "com.dotcms.spring.portlet.PortletController",
                initParams,
                "contentTypeAngular");

        DotPortlet dotPortlet = DotPortlet.builder()
                .from(portlet)
                .build();

        assertEquals("content-types-angular", dotPortlet.getPortletId());
        assertEquals("com.dotcms.spring.portlet.PortletController", dotPortlet.getPortletClass());
        assertEquals("contentTypeAngular", dotPortlet.getPortletUrl());
    }

    /**
     * Test Portlet constructors initialize portletUrl correctly
     */
    @Test
    public void testPortletConstructors_InitializePortletUrl() {
        Map<String, String> initParams = new HashMap<>();

        // Constructor without portletUrl
        Portlet portlet1 = new Portlet("test1", "TestClass1", initParams);
        assertNull("portletUrl should be null", portlet1.getPortletUrl());

        // Constructor with portletUrl
        Portlet portlet2 = new Portlet("test2", "TestClass2", initParams, "testUrl");
        assertEquals("testUrl", portlet2.getPortletUrl());

        // Test setter
        portlet1.setPortletUrl("newUrl");
        assertEquals("newUrl", portlet1.getPortletUrl());
    }

    /**
     * Test real-world XML from portlet.xml - without portlet-url
     */
    @Test
    public void testRealWorldXml_DotBrowser() throws IOException {
        String xml = "<portlet>" +
                "<portlet-name>dot-browser</portlet-name>" +
                "<display-name>dotCMS Site Browser</display-name>" +
                "<portlet-class>com.dotcms.spring.portlet.PortletController</portlet-class>" +
                "</portlet>";

        DotPortlet dotPortlet = DotPortlet.builder().fromXml(xml);

        assertNotNull("DotPortlet should not be null", dotPortlet);
        assertEquals("dot-browser", dotPortlet.getPortletId());
        assertEquals("com.dotcms.spring.portlet.PortletController", dotPortlet.getPortletClass());
        assertNull("portletUrl should be null for backward compatibility", dotPortlet.getPortletUrl());

        // Ensure it can be converted to Portlet without errors
        Portlet portlet = dotPortlet.toPortlet();
        assertNotNull("Portlet should not be null", portlet);
        assertNull("Portlet portletUrl should be null", portlet.getPortletUrl());
    }

    /**
     * Test real-world XML from portlet.xml - with portlet-url
     */
    @Test
    public void testRealWorldXml_ContentTypesAngular() throws IOException {
        String xml = "<portlet>" +
                "<portlet-name>content-types-angular</portlet-name>" +
                "<display-name>Content Types Angular</display-name>" +
                "<portlet-class>com.dotcms.spring.portlet.PortletController</portlet-class>" +
                "<portlet-url>contentTypeAngular</portlet-url>" +
                "</portlet>";

        DotPortlet dotPortlet = DotPortlet.builder().fromXml(xml);

        assertNotNull("DotPortlet should not be null", dotPortlet);
        assertEquals("content-types-angular", dotPortlet.getPortletId());
        assertEquals("com.dotcms.spring.portlet.PortletController", dotPortlet.getPortletClass());
        assertEquals("contentTypeAngular", dotPortlet.getPortletUrl());

        // Ensure it can be converted to Portlet without errors
        Portlet portlet = dotPortlet.toPortlet();
        assertNotNull("Portlet should not be null", portlet);
        assertEquals("contentTypeAngular", portlet.getPortletUrl());
    }
}
