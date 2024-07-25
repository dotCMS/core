package com.dotmarketing.business.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class PortletListTest {

    private PortletList testPortletList;
    private ObjectMapper jsonMapper;
    private XmlMapper xmlMapper;

    @Before
    public void setUp() {
        DotPortlet portlet1 = DotPortlet.builder()
                .portletId("portlet1")
                .portletClass("com.example.Portlet1")
                .putInitParam("key1", "value1")
                .build();

        DotPortlet portlet2 = DotPortlet.builder()
                .portletId("portlet2")
                .portletClass("com.example.Portlet2")
                .putInitParam("key2", "value2")
                .build();

        testPortletList = PortletList.builder()
                .addPortlets(portlet1, portlet2)
                .build();

        jsonMapper = new ObjectMapper();
        xmlMapper = new XmlMapper();
    }

    @Test
    public void testGetPortlets() {
        List<DotPortlet> portlets = testPortletList.getPortlets();
        assertEquals(2, portlets.size());
        assertEquals("portlet1", portlets.get(0).getPortletId());
        assertEquals("portlet2", portlets.get(1).getPortletId());
    }

    @Test
    public void testBuilder() {
        PortletList.Builder builder = PortletList.builder();
        DotPortlet portlet = DotPortlet.builder()
                .portletId("testPortlet")
                .portletClass("com.example.TestPortlet")
                .build();

        PortletList built = builder.addPortlets(portlet).build();
        assertEquals(1, built.getPortlets().size());
        assertEquals("testPortlet", built.getPortlets().get(0).getPortletId());
    }

    @Test
    public void testBuilderAddAll() {
        PortletList.Builder builder = PortletList.builder();
        DotPortlet portlet1 = DotPortlet.builder()
                .portletId("portlet1")
                .portletClass("com.example.Portlet1")
                .build();
        DotPortlet portlet2 = DotPortlet.builder()
                .portletId("portlet2")
                .portletClass("com.example.Portlet2")
                .build();

        PortletList built = builder.addAllPortlets(Arrays.asList(portlet1, portlet2)).build();
        assertEquals(2, built.getPortlets().size());
    }

    @Test
    public void testJsonSerialization() throws IOException {
        String json = jsonMapper.writeValueAsString(testPortletList);
        PortletList deserialized = jsonMapper.readValue(json, PortletList.class);
        assertEquals(testPortletList.getPortlets().size(), deserialized.getPortlets().size());
        assertEquals(testPortletList.getPortlets().get(0).getPortletId(), deserialized.getPortlets().get(0).getPortletId());
    }

    @Test
    public void testXmlSerialization() throws IOException {
        String xml = xmlMapper.writeValueAsString(testPortletList);
        PortletList deserialized = xmlMapper.readValue(xml, PortletList.class);
        assertEquals(testPortletList.getPortlets().size(), deserialized.getPortlets().size());
        assertEquals(testPortletList.getPortlets().get(0).getPortletId(), deserialized.getPortlets().get(0).getPortletId());
    }

    @Test
    public void testXmlSerializable() throws IOException {
        String xml = testPortletList.toXml();
        PortletList deserialized = PortletList.builder().fromXml(xml);
        assertEquals(testPortletList.getPortlets().size(), deserialized.getPortlets().size());
        assertEquals(testPortletList.getPortlets().get(0).getPortletId(), deserialized.getPortlets().get(0).getPortletId());
    }

    @Test
    public void testEmptyPortletList() {
        PortletList emptyList = PortletList.builder().build();
        assertTrue(emptyList.getPortlets().isEmpty());
    }

    @Test
    public void testImmutability() {
        List<DotPortlet> portlets = testPortletList.getPortlets();
        DotPortlet builder = DotPortlet.builder()
                .portletId("newPortlet")
                .portletClass("com.example.NewPortlet")
                .build();
        try {
            portlets.add(builder);
            fail("PortletList should be immutable");
        } catch (UnsupportedOperationException e) {
            // Expected exception
        }
    }

    @Test
    public void testBuilderReset() {
        PortletList.Builder builder = PortletList.builder()
                .addPortlets(DotPortlet.builder()
                        .portletId("portlet1")
                        .portletClass("com.example.Portlet1")
                        .build());

        builder.portlets(Collections.emptyList());
        PortletList reset = builder.build();
        assertTrue(reset.getPortlets().isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void testDotPortletMissingPortletClass() {
        DotPortlet.builder()
                .portletId("testPortlet")
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testDotPortletMissingPortletId() {
        DotPortlet.builder()
                .portletClass("com.example.TestPortlet")
                .build();
    }

    @Test
    public void testDotPortletBuilderWithAllRequiredFields() {
        DotPortlet portlet = DotPortlet.builder()
                .portletId("testPortlet")
                .portletClass("com.example.TestPortlet")
                .build();
        assertNotNull(portlet);
        assertEquals("testPortlet", portlet.getPortletId());
        assertEquals("com.example.TestPortlet", portlet.getPortletClass());
    }
}