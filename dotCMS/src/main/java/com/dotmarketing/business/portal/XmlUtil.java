package com.dotmarketing.business.portal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;

public final class XmlUtil {
    private static final XmlMapper xmlMapper;

    static {

        xmlMapper = XmlMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .addModule(new JacksonXmlModule())
                .addModule(new JaxbAnnotationModule())
                .addModule(new GuavaModule())
                .defaultUseWrapper(false)
                .build();
    }

    private XmlUtil() {
        // Utility class, no instantiation.
    }

    public static <T> T fromXml(Class<T> clazz, String xml) throws IOException {
        return xmlMapper.readValue(new StringReader(xml), clazz);
    }

    public static <T> T fromXml(Class<T> clazz, InputStream xml) throws IOException {
        return xmlMapper.readValue(xml, clazz);
    }

    public static String toXml(Object obj) throws IOException {
        StringWriter writer = new StringWriter();
        xmlMapper.writeValue(writer, obj);
        return writer.toString();
    }

}