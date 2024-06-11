package com.dotcms.api;

import com.dotcms.model.asset.BuildVersion;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import javax.enterprise.context.ApplicationScoped;

/**
 * Service to get the version of the build
 */
@ApplicationScoped
public class BuiltVersionServiceImpl implements BuiltVersionService {

    private static final String PROPERTIES_FILE = "build.properties";

    private Properties properties;

    private BuildVersion buildVersion;

    /**
     * Get the version of the build
     * @return the build version
     */
    @Override
    public Optional<BuildVersion> version() {
        if (properties == null) {
            properties = loadProperties();
        }
        if(buildVersion == null) {
            buildVersion = buildVersion(properties);
        }
        return Optional.of(buildVersion);

    }

    private BuildVersion buildVersion( Properties properties) {
        return BuildVersion.builder()
                .name(properties.getProperty("name"))
                .version(properties.getProperty("version"))
                .timestamp(Long.parseLong(properties.getProperty("timestamp")))
                .revision(properties.getProperty("revision"))
                .build();
    }

    /**
     * Load the properties file
     * @return the properties once loaded
     */
    private Properties loadProperties() {
        final Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new IOException("Unable to find " + PROPERTIES_FILE);
            }
            props.load(input);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Failed to load properties %s file containing build info.a",PROPERTIES_FILE), e);
        }
        return props;
    }

}
