package com.dotcms.api;

import com.dotcms.model.asset.BuildVersion;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Service to get the version of the build
 */
@ApplicationScoped
public class BuiltVersionServiceImpl implements BuiltVersionService {

    private static final String PROPERTIES_FILE = "build.properties";

    private BuildVersion buildVersion;

    @Inject
    Logger logger;

    /**
     * Get the version of the build
     * @return the build version
     */
    @Override
    public Optional<BuildVersion> version() {
        if (buildVersion == null) {
            try {
                buildVersion = buildVersion(loadProperties());
            } catch (IOException e) {
                logger.error("Unable to load properties file", e);
            }
        }
        return Optional.ofNullable(buildVersion);

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
    private Properties loadProperties() throws IOException{
        final Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new IOException("Unable to find " + PROPERTIES_FILE);
            }
            props.load(input);
        }
        return props;
    }

}
