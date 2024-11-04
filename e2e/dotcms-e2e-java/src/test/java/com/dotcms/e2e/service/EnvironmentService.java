package com.dotcms.e2e.service;

import com.dotcms.e2e.util.StringToolbox;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * Service class for managing environment properties.
 *
 * This singleton class loads properties from a specified environment file and provides methods
 * to retrieve these properties. If a property is not found in the file, it attempts to retrieve
 * it from the system environment variables.
 *
 * Example usage:
 * <pre>
 *  String value = EnvironmentService.get().getProperty("someKey", "defaultValue");
 * </pre>
 *
 * Methods:
 * <ul>
 *   <li>{@link #get()} - Returns the singleton instance of the service.</li>
 *   <li>{@link #getProperty(String, String)} - Retrieves a property value with a default.</li>
 *   <li>{@link #getProperty(String)} - Retrieves a property value without a default.</li>
 * </ul>
 *
 * The {@link StringToolbox} class is used to convert camelCase keys to snake_case for environment variable lookup.
 *
 * @author vico
 */
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

    /**
     * Retrieves a property value with a default.
     *
     * @param key the property key
     * @param defaultValue the default value if the property is not found
     * @return the property value or the default value if not found
     */
    public String getProperty(final String key, final String defaultValue) {
        String propValue = props.getProperty(key);
        if (StringUtils.isNotBlank(propValue)) {
            return propValue;
        }

        propValue = System.getProperty(key);
        if (StringUtils.isNotBlank(propValue)) {
            return propValue;
        }

        return StringUtils.defaultString(System.getenv(StringToolbox.camelToSnake(key)), defaultValue);
    }

    /**
     * Retrieves a property value without a default.
     *
     * @param key the property key
     * @return the property value or null if not found
     */
    public String getProperty(final String key) {
        return getProperty(key, null);
    }

}
