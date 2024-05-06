package com.dotcms.publishing;


import com.dotcms.util.xstream.XStreamHandler;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.nio.charset.StandardCharsets;

/**
 * Util for serialize object to XML file
 */
public class XMLSerializerUtil {

    private static final XMLSerializerUtil instance= new XMLSerializerUtil();
    private XStream xmlSerializer;


    /**
     * Returns a singleton instance of this class.
     *
     * @return A unique instance of {@link XMLSerializerUtil}.
     */
    public static XMLSerializerUtil getInstance() {

        return instance;
    }

    private XMLSerializerUtil(){
        xmlSerializer = XStreamHandler.newXStreamInstance(StandardCharsets.UTF_8.name());
        xmlSerializer.autodetectAnnotations(true);
    }

    public XStream getXmlSerializer() {
        return xmlSerializer;
    }

    /**
     * Serialize and object to a hierarchical data structure (such as XML).
     *
     * @throws XStreamException if the object cannot be serialized
     */
    public void marshal(Object obj, final HierarchicalStreamWriter xmlWriter) {
        xmlSerializer.marshal(obj, xmlWriter);
    }
}
