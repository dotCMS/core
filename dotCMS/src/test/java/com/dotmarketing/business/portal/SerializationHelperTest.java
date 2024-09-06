package com.dotmarketing.business.portal;

import static org.junit.Assert.*;

import com.dotmarketing.util.Logger;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import java.io.FileInputStream;
import java.io.IOException;

public class SerializationHelperTest {

    private DotPortlet testPortlet;
    private PortletList testPortletList;

    @Before
    public void setUp() {
        testPortlet = DotPortlet.builder()
                .portletId("categories")
                .portletClass("com.liferay.portlet.StrutsPortlet")
                .putInitParam("view-action", "/ext/c")
                .putInitParam("param2", "param2.value")
                .build();

        testPortletList = PortletList.builder()
                .addPortlet(testPortlet)
                .build();
    }

    @Test
    public void testFromXmlFile() throws IOException {
        try (FileInputStream fis = new FileInputStream("src/main/webapp/WEB-INF/portlet.xml")) {
            PortletList portletList = SerializationHelper.fromXml(PortletList.class, fis);
            Logger.debug(SerializationHelperTest.class,"Loaded src/main/webapp/WEB-INF/portlet.xml:"+portletList.toString());
            Logger.info(SerializationHelperTest.class, "Loaded portlet.xml: found: " + portletList.getPortlets().size() + " portlets");
            assertNotNull("Deserialized PortletList should not be null", portletList);
            assertEquals("PortletList should contain exactly 44 portlets", 44, portletList.getPortlets().size());

            // Check for specific portlets
            assertTrue("PortletList should contain 'categories' portlet",
                    portletList.getPortlets().stream().anyMatch(p -> p.getPortletId().equals("categories")));
            assertTrue("PortletList should contain 'dotai' portlet",
                    portletList.getPortlets().stream().anyMatch(p -> p.getPortletId().equals("dotai")));

            // Check a specific portlet's details
            Optional<DotPortlet> categoriesPortlet = portletList.getPortlets().stream()
                    .filter(p -> p.getPortletId().equals("categories"))
                    .findFirst();
            assertTrue("Categories portlet should exist", categoriesPortlet.isPresent());
            categoriesPortlet.ifPresent(portlet -> {
                assertEquals("Categories portlet class should match",
                        "com.liferay.portlet.StrutsPortlet", portlet.getPortletClass());
                assertEquals("Categories portlet should have correct view-action",
                        "/ext/categories/view_categories", portlet.initParams().get("view-action"));
            });
        }
    }

    @Test
    public void testPortletJsonSerialization() throws IOException {
        String json = SerializationHelper.toJson(testPortlet);
        Logger.info(SerializationHelperTest.class,"Portlet to Json: "+json);
        assertNotNull("JSON string should not be null", json);
        assertTrue("JSON should contain portlet ID", json.contains("\"portlet-name\":\"categories\""));
        assertTrue("JSON should contain portlet class", json.contains("\"portlet-class\":\"com.liferay.portlet.StrutsPortlet\""));
        assertTrue("JSON should contain init-param", json.contains("\"init-param\":[{"));
        assertTrue("JSON should contain view-action param", json.contains("\"name\":\"view-action\",\"value\":\"/ext/c\""));
        assertTrue("JSON should contain param2", json.contains("\"name\":\"param2\",\"value\":\"param2.value\""));
    }

    @Test
    public void testPortletListJsonSerialization() throws IOException {
        String jsonList = SerializationHelper.toJson(testPortletList);
        Logger.info(SerializationHelperTest.class,"PortletList to Json: "+jsonList);
        assertNotNull("JSON string should not be null", jsonList);
        assertTrue("JSON should contain portlet array", jsonList.contains("\"portlet\":[{"));
        assertTrue("JSON should contain portlet ID", jsonList.contains("\"portlet-name\":\"categories\""));
        assertTrue("JSON should contain portlet class", jsonList.contains("\"portlet-class\":\"com.liferay.portlet.StrutsPortlet\""));
        assertTrue("JSON should contain init-param", jsonList.contains("\"init-param\":[{"));
    }

    @Test
    public void testPortletListJsonDeserialization() throws IOException {
        String jsonList = SerializationHelper.toJson(testPortletList);
        PortletList deserializedJson = SerializationHelper.fromJson(PortletList.class, jsonList);
        Logger.info(SerializationHelperTest.class,"PortletList to Json: "+jsonList);
        assertNotNull("Deserialized PortletList should not be null", deserializedJson);
        assertEquals("PortletList should contain one portlet", 1, deserializedJson.getPortlets().size());
        DotPortlet deserializedPortlet = deserializedJson.getPortlets().get(0);
        assertEquals("Portlet ID should match", "categories", deserializedPortlet.getPortletId());
        assertEquals("Portlet class should match", "com.liferay.portlet.StrutsPortlet", deserializedPortlet.getPortletClass());
        assertEquals("Portlet should have 2 init params", 2, deserializedPortlet.initParams().size());
        assertEquals("view-action param should match", "/ext/c", deserializedPortlet.initParams().get("view-action"));
        assertEquals("param2 should match", "param2.value", deserializedPortlet.initParams().get("param2"));
    }

    @Test
    public void testPortletXmlSerialization() throws IOException {
        String portletXml = SerializationHelper.toXml(testPortlet);
        Logger.info(SerializationHelperTest.class,"Portlet to Xml: "+portletXml);
        assertNotNull("XML string should not be null", portletXml);
        assertTrue("XML should contain portlet tag", portletXml.contains("<portlet>"));
        assertTrue("XML should contain portlet-name", portletXml.contains("<portlet-name>categories</portlet-name>"));
        assertTrue("XML should contain portlet-class", portletXml.contains("<portlet-class>com.liferay.portlet.StrutsPortlet</portlet-class>"));
        assertTrue("XML should contain init-param", portletXml.contains("<init-param>"));
        assertTrue("XML should contain view-action param", portletXml.contains("<name>view-action</name><value>/ext/c</value>"));
        assertTrue("XML should contain param2", portletXml.contains("<name>param2</name><value>param2.value</value>"));
    }

    @Test
    public void testPortletListXmlSerialization() throws IOException {
        String xml = SerializationHelper.toXml(testPortletList);
        Logger.info(SerializationHelperTest.class,"PortletList to xml: "+xml);
        assertNotNull("XML string should not be null", xml);
        assertTrue("XML should contain portlet-app tag", xml.contains("<portlet-app>"));
        assertTrue("XML should contain portlet tag", xml.contains("<portlet>"));
        assertTrue("XML should contain portlet-name", xml.contains("<portlet-name>categories</portlet-name>"));
        assertTrue("XML should contain portlet-class", xml.contains("<portlet-class>com.liferay.portlet.StrutsPortlet</portlet-class>"));
        assertTrue("XML should contain init-param", xml.contains("<init-param>"));
    }

    @Test
    public void testPortletXmlDeserialization() throws IOException {
        String portletXml = SerializationHelper.toXml(testPortlet);
        DotPortlet deserializedPortletXml = SerializationHelper.fromXml(DotPortlet.class, portletXml);
        assertNotNull("Deserialized DotPortlet should not be null", deserializedPortletXml);
        assertEquals("Portlet ID should match", "categories", deserializedPortletXml.getPortletId());
        assertEquals("Portlet class should match", "com.liferay.portlet.StrutsPortlet", deserializedPortletXml.getPortletClass());
        assertEquals("Portlet should have 2 init params", 2, deserializedPortletXml.initParams().size());
        assertEquals("view-action param should match", "/ext/c", deserializedPortletXml.initParams().get("view-action"));
        assertEquals("param2 should match", "param2.value", deserializedPortletXml.initParams().get("param2"));
    }

    @Test
    public void testPortletListXmlDeserialization() throws IOException {
        String xml = SerializationHelper.toXml(testPortletList);
        PortletList deserializedXml = SerializationHelper.fromXml(PortletList.class, xml);
        assertNotNull("Deserialized PortletList should not be null", deserializedXml);
        assertEquals("PortletList should contain one portlet", 1, deserializedXml.getPortlets().size());

        Optional<DotPortlet> deserializedPortlet = deserializedXml.getPortlets().stream()
                .filter(p -> p.getPortletId().equals("categories"))
                .findFirst();

        assertTrue("Deserialized PortletList should contain 'categories' portlet", deserializedPortlet.isPresent());

        deserializedPortlet.ifPresent(portlet -> {
            assertEquals("Portlet ID should match", "categories", portlet.getPortletId());
            assertEquals("Portlet class should match", "com.liferay.portlet.StrutsPortlet", portlet.getPortletClass());
            assertEquals("Portlet should have 2 init params", 2, portlet.initParams().size());
            assertEquals("view-action param should match", "/ext/c", portlet.initParams().get("view-action"));
            assertEquals("param2 should match", "param2.value", portlet.initParams().get("param2"));
        });
    }

    @Test
    public void testPortletListXmlRoundTrip() throws IOException {
        String xml = testPortletList.toXml();
        PortletList newPortletList = PortletList.builder().fromXml(xml);
        assertNotNull("New PortletList should not be null", newPortletList);
        assertEquals("PortletList should contain one portlet", 1, newPortletList.getPortlets().size());

        Optional<DotPortlet> roundTripPortlet = newPortletList.getPortlets().stream()
                .filter(p -> p.getPortletId().equals("categories"))
                .findFirst();

        assertTrue("Round-tripped PortletList should contain 'categories' portlet", roundTripPortlet.isPresent());

        roundTripPortlet.ifPresent(portlet -> {
            assertEquals("Portlet ID should match", "categories", portlet.getPortletId());
            assertEquals("Portlet class should match", "com.liferay.portlet.StrutsPortlet", portlet.getPortletClass());
            assertEquals("Portlet should have 2 init params", 2, portlet.initParams().size());
            assertEquals("view-action param should match", "/ext/c", portlet.initParams().get("view-action"));
            assertEquals("param2 should match", "param2.value", portlet.initParams().get("param2"));
        });
    }
}