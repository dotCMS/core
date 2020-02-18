package com.dotcms.publishing;

import com.dotcms.config.DotInitializer;
import com.dotcms.util.YamlUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Path of the yaml files: /assets/server/publishing-filters/
 */
public class PushPublishFiltersInitializer implements DotInitializer {

    @Override
    public void init() {

        final String filtersDirectory = APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator + "server" + File.separator + "publishing-filters" + File.separator;
        final File basePath = new File(filtersDirectory);
        if (!basePath.exists()) {
            basePath.mkdir();
            //copiar archivos desde resources
        }
        Logger.info(PushPublishFiltersInitializer.class, " ymlFiles are set under:  " + filtersDirectory);
        Stream<Path> pathStream = null;
        try {
            pathStream = Files.list(basePath.toPath());
            pathStream.forEach(this::loadFilter);
        } catch (IOException e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    protected void loadFilter(final Path path){
        final String fileName = path.getFileName().toString();
        Logger.info(PushPublishFiltersInitializer.class, " ymlFileName:  " + fileName);
        final FilterDescriptor filterDescriptor = YamlUtil.parse(path,FilterDescriptor.class);
        Logger.info(PushPublishFiltersInitializer.class, filterDescriptor.toString());
        APILocator.getPublisherAPI().addFilter(filterDescriptor);
    }
}