package com.dotmarketing.business.portal;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import javax.xml.XMLConstants;
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
                            // may not be able to use for backwards compatibility
                            //factory.setFeature(
                            //        "http://apache.org/xml/features/disallow-doctype-decl", true);

                            factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
                            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
                            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
                            factory.setXIncludeAware(false);
                            factory.setValidating(false);
                            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

                            return factory;
                        } catch (ParserConfigurationException e) {
                            Logger.error(ThreadLocalSaxParserFactory.class,"ParserConfigurationException was thrown. The feature 'XMLConstants.FEATURE_SECURE_PROCESSING'"
                                    + " is probably not supported by your XML processor.",e);
                            throw new DotRuntimeException(e);
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
