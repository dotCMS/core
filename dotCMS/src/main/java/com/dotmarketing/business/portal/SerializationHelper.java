package com.dotmarketing.business.portal;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;

/* Eventually we may want to reuse this logic generically
   at the moment we will encapsulate in this package to keep modular
 */
final class SerializationHelper {
    private static final ObjectMapper jsonMapper;
    private static final XmlMapper xmlMapper;

    static {
        jsonMapper = JsonMapper.builder()
                .addModule(new GuavaModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();

        xmlMapper = XmlMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .addModule(new JacksonXmlModule())
                .addModule(new JaxbAnnotationModule())
                .addModule(new GuavaModule())
                .defaultUseWrapper(true)
                .build();
    }

    private SerializationHelper() {
        // Utility class, no instantiation.
    }

    public static XmlMapper getXmlMapper()  {
        return xmlMapper;
    }
    public static ObjectMapper getJsonMapper()  {
        return jsonMapper;
    }

    public static <T> T fromJson(Class<T> clazz, String json) throws IOException {
        return jsonMapper.readValue(json, clazz);
    }

    public static <T> T fromXml(Class<T> clazz, String xml) throws IOException {
        return xmlMapper.readValue(new StringReader(xml), clazz);
    }

    public static <T> T fromJson(Class<T> clazz, InputStream json) throws IOException {
        return jsonMapper.readValue(json, clazz);
    }

    public static <T> T fromXml(Class<T> clazz, InputStream xml) throws IOException {
        return xmlMapper.readValue(xml, clazz);
    }

    public static String toJson(Object obj) throws IOException {
        return jsonMapper.writeValueAsString(obj);
    }

    public static String toXml(Object obj) throws IOException {
        StringWriter writer = new StringWriter();
        xmlMapper.writeValue(writer, obj);
        return writer.toString();
    }

    public static void main(String[] args) throws IOException {

        try (FileInputStream fis = new FileInputStream("/Users/stevebolton/git/core-baseline/dotCMS/src/main/webapp/WEB-INF/portlet.xml")) {
            PortletList portletList = SerializationHelper.fromXml(PortletList.class, fis);
            System.out.println("Deserialized from XML file : " + portletList);
        }




        // Example DotPortlet object
        DotPortlet portlet =
                DotPortlet.builder()
                        .portletId("categories")
                        .portletClass("com.liferay.portlet.StrutsPortlet")

                        .putInitParam("view-action", "/ext/c")
                        .putInitParam("param2", "param2.value")
                        .build();


        // Serialize to JSON
        String json = SerializationHelper.toJson(portlet);
        System.out.println("JSON portlent: " + json);

        // Deserialize from JSON
        PortletList deserializedJsonPortlet = SerializationHelper.fromJson(PortletList.class, json);
        System.out.println("Deserialized from JSON portlet: " + deserializedJsonPortlet);


        // Example PortletList object
        PortletList portletList = PortletList.builder()
                .addPortlet(portlet)
                .build();

        // Serialize to JSON
        String jsonList = SerializationHelper.toJson(portletList);
        System.out.println("JSON: " + jsonList);

        // Deserialize from JSON
        PortletList deserializedJson = SerializationHelper.fromJson(PortletList.class, jsonList);
        System.out.println("Deserialized from JSON: " + deserializedJson);

        // Serialize to XML
        String portletXml = SerializationHelper.toXml(portlet);
        System.out.println("Portlet XML: " + portletXml);

        // Serialize to XML
        String xml = SerializationHelper.toXml(portletList);
        System.out.println("XML: " + xml);

        // Deserialize from XML
        DotPortlet deserializedPortletXml = SerializationHelper.fromXml(DotPortlet.class, portletXml);
        System.out.println("Deserialized Portlet from XML: " + deserializedPortletXml);

        // Deserialize from XML
        PortletList deserializedXml = SerializationHelper.fromXml(PortletList.class, xml);
        System.out.println("Deserialized from XML: " + deserializedXml);


        String out1 = deserializedXml.toXml();
        System.out.println("Deserialized directly from XML: " + out1);

        PortletList newPortletList = PortletList.builder().fromXml(out1);
        System.out.println("Deserialized portletList from XML: " + newPortletList);

    }
}