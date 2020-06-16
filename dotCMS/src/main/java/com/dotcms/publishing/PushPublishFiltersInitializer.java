package com.dotcms.publishing;

import com.dotcms.config.DotInitializer;
import com.dotcms.util.YamlUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Path of the yaml files: /assets/server/publishing-filters/
 */
public class PushPublishFiltersInitializer implements DotInitializer {

    @Override
    public void init() {

        try {
            //Path where the YAML files are stored
            final String filtersDirectoryString =
                    APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator + "server"
                            + File.separator + "publishing-filters" + File.separator;
            final File basePath = new File(filtersDirectoryString);
            if (!basePath.exists()) {
                basePath.mkdir();
                //If the directory does not exists, copy the YAML files that are ship with
                //dotcms to the created directory
                final String systemFiltersDirectory = "com" + File.separator + "dotcms" +
                        File.separator + "publishing-filters" + File.separator;
                final String systemFiltersPathString = Thread.currentThread()
                        .getContextClassLoader().getResource(systemFiltersDirectory).getPath();
                final File systemFilters = new File(systemFiltersPathString);
                Files.list(systemFilters.toPath()).forEach(filter -> {
                    try {
                        Files.copy(filter,
                                Paths.get(filtersDirectoryString + filter.getFileName()));
                    } catch (IOException e) {
                        Logger.error(this, e.getMessage(), e);
                    }
                });
                Logger.debug(PushPublishFiltersInitializer.class, ()->" dotcms filters files copied");
            }
            Logger.info(PushPublishFiltersInitializer.class, " ymlFiles are set under:  " + filtersDirectoryString);
            //For each YAML file under the directory,
            // read it and load the Filter to the PublisherAPI.loadedFilters
            Files.list(basePath.toPath()).forEach(this::loadFilter);
        } catch (IOException e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    protected void loadFilter(final Path path){
        final String fileName = path.getFileName().toString();
        Logger.info(PushPublishFiltersInitializer.class, " ymlFileName:  " + fileName);
        final FilterDescriptor filterDescriptor = YamlUtil.parse(path,FilterDescriptor.class);
        filterDescriptor.setKey(fileName);
        Logger.info(PushPublishFiltersInitializer.class, filterDescriptor.toString());
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);
    }
}