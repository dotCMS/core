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
    private final XStreamHandler.Cached xmlSerializer;


    /**
     * Returns a singleton instance of this class.
     *
     * @return A unique instance of {@link XMLSerializerUtil}.
     */
    public static XMLSerializerUtil getInstance() {

        return instance;
    }

    private XMLSerializerUtil(){
        xmlSerializer = new XStreamHandler.Cached(StandardCharsets.UTF_8.name(),
                xstream -> xstream.autodetectAnnotations(true));
    }

    /**
     * The serializer this singleton hands out. Rebuilt behind the scenes when the trusted-list
     * configuration changes, so an installation does not have to restart for a configured
     * extension to take effect on the bundle-reading path.
     *
     * @return the serializer to marshal with or read from
     */
    public XStream getXmlSerializer() {
        return xmlSerializer.get();
    }

    /**
     * Serialize and object to a hierarchical data structure (such as XML).
     *
     * @throws XStreamException if the object cannot be serialized
     */
    public void marshal(Object obj, final HierarchicalStreamWriter xmlWriter) {
        xmlSerializer.get().marshal(obj, xmlWriter);
    }
}
