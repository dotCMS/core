package com.dotcms.config;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

public class EnvironmentConfigSource implements ConfigSource {

    private Properties properties;

    public EnvironmentConfigSource() {
        loadPropertiesFromFile();
    }

    private void loadPropertiesFromFile() {
        properties = new Properties();
        try (InputStream inputStream = getClass().getResourceAsStream("/application.properties")) {
            properties.load(inputStream);
            // properties.stringPropertyNames().forEach(property -> System.out.println(property + " = " + properties.getProperty(property)));
        } catch (IOException e) {
            // Handle the exception or log the error
        }
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.stringPropertyNames();
    }

    @Override
    public String getValue(String propertyName) {
        return properties.getProperty(propertyName);
    }

    @Override
    public String getName() {
        return "DiskConfigSource";
    }

    @Override
    public int getOrdinal() {
        return 100; // The ordinal of the config source. Config sources with a lower ordinal are tried first.
    }

}
