package com.ettrema.httpclient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.dotcms.repackage.commons_io_2_0_1.org.apache.commons.io.IOUtils;
import com.dotcms.repackage.tika_app_1_3.org.jdom.Element;
import com.dotcms.repackage.tika_app_1_3.org.jdom.JDOMException;
import com.dotcms.repackage.tika_app_1_3.org.jdom.Namespace;
import com.dotcms.repackage.tika_app_1_3.org.jdom.filter.ElementFilter;
import com.dotcms.repackage.tika_app_1_3.org.jdom.input.SAXBuilder;
import com.dotcms.repackage.slf4j_api_1_6_0.org.slf4j.Logger;
import com.dotcms.repackage.slf4j_api_1_6_0.org.slf4j.LoggerFactory;



/**
 *
 * @author mcevoyb
 */
public class RespUtils {

    private static final Logger log = LoggerFactory.getLogger( RespUtils.class );
    
    public static Namespace NS_DAV = Namespace.getNamespace("D", "DAV:");
    
    public static String asString( Element el, String name ) {
        Element elChild = el.getChild( name, NS_DAV  );
        if( elChild == null ) {
            //log.debug("No child: " + name + " of " + el.getName());            
            return null;
        }
        return elChild.getText();
    }

    public static String asString( Element el, String name, Namespace ns ) {
//        System.out.println("asString: " + qname + " in: " + el.getName());
//        for( Object o : el.elements() ) {
//            Element e = (Element) o;
//            System.out.println(" - " + e.getQualifiedName());
//        }
        Element elChild = el.getChild( name, ns );
        if( elChild == null ) return null;
        return elChild.getText();
    }    
    
    public static Long asLong( Element el, String name ) {
        String s = asString( el, name );
        if( s == null || s.length()==0 ) return null;
        long l = Long.parseLong( s );
        return l;
    }
    
    public static Long asLong( Element el, String name, Namespace ns ) {
        String s = asString( el, name, ns );
        if( s == null || s.length()==0 ) return null;
        long l = Long.parseLong( s );
        return l;
    }    

    public static boolean hasChild( Element el, String name ) {
        if( el == null ) return false;
        List<Element> list = getElements(el, name);
        
        return !list.isEmpty();
    }    
    

    public static  List<Element> getElements(Element root, String name) {
        List<Element> list = new ArrayList<Element>();
        Iterator it = root.getDescendants(new ElementFilter(name));
        while(it.hasNext()) {
            Object o = it.next();
            if( o instanceof Element) {
                list.add((Element)o);
            }
        }
        return list;
    }    
    
    public static  com.dotcms.repackage.tika_app_1_3.org.jdom.Document getJDomDocument(InputStream in) throws JDOMException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try {
			IOUtils.copy(in, bout);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}		
//		System.out.println("");
//		System.out.println(bout.toString());
//		System.out.println("");
		ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        try {
            SAXBuilder builder = new SAXBuilder();
            builder.setExpandEntities(false);
            return builder.build(bin);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }        
}
