package com.dotmarketing.config;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.dotmarketing.util.Logger;

/**
 * Loads up either clickstream.xml or clickstream-default.xml and
 * returns a singleton instance of ClickstreamConfig.
 *
 * @author 
 */
public class ConfigLoader {

    private ClickstreamConfig config;
    private static ConfigLoader singleton;

    public static ConfigLoader getInstance() {
        if (singleton == null) {
            singleton = new ConfigLoader();
        }

        return singleton;
    }

    private ConfigLoader() {
    }

    public synchronized ClickstreamConfig getConfig() {
        if (config != null) {
            return config;
        }

        InputStream is = getInputStream("clickstream.xml");
        
        if (is == null) {
            is = getInputStream("/clickstream.xml");
        }
        if (is == null) {
            is = getInputStream("META-INF/clickstream-default.xml");
        }
        if (is == null) {
            is = getInputStream("/META-INF/clickstream-default.xml");
        }

        config = new ClickstreamConfig();

        try {
            Logger.debug(ConfigLoader.class, "Loading config");
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(is, new ConfigHandler());
            return config;
        }
        catch (SAXException e) {
            Logger.fatal(ConfigLoader.class, "Could not parse config XML", e);
            throw new RuntimeException(e.getMessage());
        }
        catch (IOException e) {
        	Logger.fatal(ConfigLoader.class, "Could not read config from stream", e);
            throw new RuntimeException(e.getMessage());
        }
        catch (ParserConfigurationException e) {
        	Logger.fatal(ConfigLoader.class, "Could not obtain SAX parser", e);
            throw new RuntimeException(e.getMessage());
        }
        catch (RuntimeException e) {
        	Logger.fatal(ConfigLoader.class, "RuntimeException", e);
            throw e;
        }
        catch (Throwable e) {
        	Logger.fatal(ConfigLoader.class, "Exception", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * SAX Handler implementation for handling tags in config file and building
     * config objects.
     */
    private class ConfigHandler extends DefaultHandler {
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("logger")) {
                config.setLoggerClass(attributes.getValue("class"));
            }
            else if (qName.equals("bot-host")) {
                config.addBotHost(attributes.getValue("name"));
            }
            else if (qName.equals("bot-agent")) {
                config.addBotAgent(attributes.getValue("name"));
            }
        }
    }

    private InputStream getInputStream(String resourceName) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);

        if (is == null) {
            ConfigLoader.class.getClassLoader().getResourceAsStream(resourceName);
        }

        return is;
    }
}
