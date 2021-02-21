package com.dotcms.config;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FilePropertiesConfigurationProvider extends AbstractOrderedConfigurationProvider {

    private final File [] configurationFiles;

    public FilePropertiesConfigurationProvider(final File... configurationFiles) {

        this.configurationFiles = configurationFiles;
    }

    @Override
    public Map<String, Object> getConfig() {

        final Map<String, Object> configMap = new HashMap<>();

        if (null != configurationFiles) {
            for (final File file : this.configurationFiles) {

                if (null != file && file.exists() && file.canRead()) {
                    this.readConfig(file, configMap);
                }
            }
        }

        return configMap;
    }

    private void readConfig (final File fileToRead, final Map<String, Object> configMap) {

        final PropertiesConfiguration props = this.loadPropertiesConfiguration(fileToRead);
        final Iterator<String> it = props.getKeys();

        while(it.hasNext()) {

            final String key = it.next();
            configMap.put(key, props.getProperty(key));
        }
    }

    protected PropertiesConfiguration loadPropertiesConfiguration (final File fileToRead) {

        final PropertiesConfiguration props = new PropertiesConfiguration();

        try (InputStream propsInputStream = Files.newInputStream(fileToRead.toPath())) {

            Logger.info( Config.class, "Loading dotCMS [" + fileToRead.getName() + "] Properties..." );

            props.load(new InputStreamReader(propsInputStream));
            Logger.info( Config.class, "dotCMS Properties [" + fileToRead.getName() + "] Loaded" );

        } catch (Exception e) {

            Logger.fatal( Config.class,
                    "Exception loading properties for file [" + fileToRead.getName() + "]", e);
        }

        return props;
    }
}
