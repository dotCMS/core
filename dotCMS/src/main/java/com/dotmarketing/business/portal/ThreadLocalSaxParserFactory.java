package com.dotmarketing.business.portal;

import com.dotmarketing.exception.DotRuntimeException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

public class ThreadLocalSaxParserFactory {

    private ThreadLocalSaxParserFactory() {
        // utility class, do not instantiate
    }

    private static final ThreadLocal<SAXParserFactory> saxParserFactoryThreadLocal =
            ThreadLocal.withInitial(
                    () -> {
                        try {
                            SAXParserFactory factory = SAXParserFactory.newInstance();
                            // https://rules.sonarsource.com/java/RSPEC-2755
                            // prevent XXE, completely disable DOCTYPE declaration:
                            factory.setFeature(
                                    "http://apache.org/xml/features/disallow-doctype-decl", true);
                            return factory;
                        } catch (Exception e) {
                            throw new DotRuntimeException(e);
                        }
                    });

    public static SAXParser getSaxParser() throws ParserConfigurationException, SAXException {
        // you could catch and re-throw the RuntimeException if the caller should handle it
        return saxParserFactoryThreadLocal.get().newSAXParser();
    }

    public static void cleanup() {
        saxParserFactoryThreadLocal.remove();
    }
}
