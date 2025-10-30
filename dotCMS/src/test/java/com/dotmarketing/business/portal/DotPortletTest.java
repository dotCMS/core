package com.dotmarketing.business.portal;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.liferay.portal.model.Portlet;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DotPortletTest {

    private DotPortlet testPortlet;
    private ObjectMapper jsonMapper;
    private XmlMapper xmlMapper;

    @Before
    public void setUp() {
        Map<String, String> initParams = new HashMap<>();
        initParams.put("param1", "value1");
        initParams.put("param2", "value2");

        testPortlet = DotPortlet.builder()
                .portletId("test-portlet")
                .portletClass("com.example.TestPortlet")
                .portletUrl("/test-portlet")
                .initParams(initParams)
                .build();

        jsonMapper = new ObjectMapper();
        xmlMapper = new XmlMapper();
    }

    @Test
    public void testGetPortletId() {
        assertEquals("test-portlet", testPortlet.getPortletId());
    }

    @Test
    public void testGetPortletClass() {
        assertEquals("com.example.TestPortlet", testPortlet.getPortletClass());
    }

    @Test
    public void testGetInitParams() {
        List<InitParam> initParams = testPortlet.getInitParams();
        assertEquals(2, initParams.size());
        assertTrue(initParams.contains(InitParam.of("param1", "value1")));
        assertTrue(initParams.contains(InitParam.of("param2", "value2")));
    }

    @Test
    public void testInitParamsMap() {
        Map<String, String> initParamsMap = testPortlet.initParams();
        assertEquals(2, initParamsMap.size());
        assertEquals("value1", initParamsMap.get("param1"));
        assertEquals("value2", initParamsMap.get("param2"));
    }

    @Test
    public void testFromPortlet() {
        Portlet portlet = new Portlet("test-portlet", "com.example.TestPortlet", testPortlet.initParams(),testPortlet.getPortletUrl());
        DotPortlet converted = DotPortlet.from(portlet);
        assertEquals(testPortlet, converted);
    }

    @Test
    public void testToPortlet() {
        Portlet portlet = testPortlet.toPortlet();
        assertEquals(testPortlet.getPortletId(), portlet.getPortletId());
        assertEquals(testPortlet.getPortletClass(), portlet.getPortletClass());
        assertEquals(testPortlet.initParams(), portlet.getInitParams());
    }

    @Test
    public void testJsonSerialization() throws IOException {
        String json = jsonMapper.writeValueAsString(testPortlet);
        DotPortlet deserialized = jsonMapper.readValue(json, DotPortlet.class);
        assertEquals(testPortlet, deserialized);
    }

    @Test
    public void testXmlSerialization() throws IOException {
        String xml = xmlMapper.writeValueAsString(testPortlet);
        DotPortlet deserialized = xmlMapper.readValue(xml, DotPortlet.class);
        assertEquals(testPortlet, deserialized);
    }

    @Test
    public void testBuilder() {
        DotPortlet.Builder builder = DotPortlet.builder()
                .portletId("builder-test")
                .portletClass("com.example.BuilderTest");

        builder.addAllInitParams(List.of(
                InitParam.of("param1", "value1"),
                InitParam.of("param2", "value2")
        ));

        DotPortlet built = builder.build();
        assertEquals("builder-test", built.getPortletId());
        assertEquals("com.example.BuilderTest", built.getPortletClass());
        assertEquals(2, built.getInitParams().size());
    }

    @Test
    public void testBuilderFromPortlet() {
        Portlet portlet = new Portlet("from-portlet", "com.example.FromPortlet", Map.of("key", "value"));
        DotPortlet built = DotPortlet.builder().from(portlet).build();
        assertEquals(portlet.getPortletId(), built.getPortletId());
        assertEquals(portlet.getPortletClass(), built.getPortletClass());
        assertEquals(portlet.getInitParams(), built.initParams());
    }

    @Test
    public void testXmlSerializable() throws IOException {
        String xml = testPortlet.toXml();
        DotPortlet deserialized = DotPortlet.builder().fromXml(xml);
        assertEquals(testPortlet, deserialized);
    }
}
