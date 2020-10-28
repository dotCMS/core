package com.dotcms.publishing;

import com.dotcms.config.DotInitializer;
import com.dotcms.util.YamlUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
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
            //Clear filtersMap
            PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
            //Path where the YAML files are stored
            final String filtersDirectoryString =
                    APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator + "server"
                            + File.separator + "publishing-filters" + File.separator;
            final File basePath = new File(filtersDirectoryString);
            if (!basePath.exists()) {
                Logger.debug(PushPublishFiltersInitializer.class, ()->"PushPublishing Filters directory does not exists, creating under: " + filtersDirectoryString);
                basePath.mkdir();
                //If the directory does not exists, copy the YAML files that are ship with
                //dotcms to the created directory
                final String systemFiltersDirectory = "publishing-filters" + File.separator;
                final String systemFiltersPathString = Config.CONTEXT
                        .getRealPath("/WEB-INF/" + systemFiltersDirectory);
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
            Logger.debug(PushPublishFiltersInitializer.class, ()->" PushPublishing Filters Directory: " + filtersDirectoryString);
            //For each YAML file under the directory,
            // read it and load the Filter to the PublisherAPI.loadedFilters
            Files.list(basePath.toPath()).forEach(this::loadFilter);
        } catch (IOException e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    protected void loadFilter(final Path path){
        final String fileName = path.getFileName().toString();
        Logger.info(PushPublishFiltersInitializer.class, " Loading PushPublish Filter:  " + fileName);
        try {
            final FilterDescriptor filterDescriptor = YamlUtil.parse(path, FilterDescriptor.class);
            filterDescriptor.setKey(fileName);
            Logger.info(PushPublishFiltersInitializer.class, filterDescriptor.toString());
            filterDescriptor.validate();
            APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);
        }catch(Exception e) {
            Logger.warnAndDebug(this.getClass(), "unable to load PP filter:" + path.toString() + " cause: " + e.getMessage(), e);
        }
    }
}