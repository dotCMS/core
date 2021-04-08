package com.dotcms.publishing;


import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Util for serialize object to XML file
 */
public class XMLSerializerUtil {

    private static final XMLSerializerUtil instance= new XMLSerializerUtil();
    static XStream xmlSerializer = null;


    /**
     * Returns a singleton instance of this class.
     *
     * @return A unique instance of {@link XMLSerializerUtil}.
     */
    public static XMLSerializerUtil getInstance() {

        return instance;
    }

    private XMLSerializerUtil(){
        xmlSerializer = new XStream(new DomDriver("UTF-8"));
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
