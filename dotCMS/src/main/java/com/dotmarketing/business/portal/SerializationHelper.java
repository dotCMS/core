package com.dotmarketing.business.portal;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
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
}