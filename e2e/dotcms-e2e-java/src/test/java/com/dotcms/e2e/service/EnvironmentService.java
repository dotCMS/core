package com.dotcms.e2e.service;

import com.dotcms.e2e.util.StringToolbox;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Properties;

public class EnvironmentService {

    private static final EnvironmentService INSTANCE = new EnvironmentService();

    public static EnvironmentService get() {
        return INSTANCE;
    }

    private final Properties props;

    private EnvironmentService() {
        props = new Properties();
        final String envFile = System.getProperty("env", "environment");
        final String filePath = envFile.concat(".properties");

        try {
            props.load(EnvironmentService.class.getClassLoader().getResourceAsStream(filePath));
        } catch (IOException e) {
            System.out.println("Did not manage to read this property file");
            throw new RuntimeException(e);
        }
    }

    public String getProperty(final String key, final String defaultValue) {
        final String propValue = props.getProperty(key);
        if (StringUtils.isNotBlank(propValue)) {
            return propValue;
        }

        return StringUtils.defaultString(System.getenv(StringToolbox.camelToSnake(key)), defaultValue);
    }

    public String getProperty(final String key) {
        return getProperty(key, null);
    }

}
