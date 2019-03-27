package com.dotcms.rendering.velocity.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Set;

import org.apache.velocity.exception.ResourceNotFoundException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;

public class IncludeLoader implements DotLoader {

    private final String[] allowedPaths;
    private static IncludeLoader includeServices;
    private final Set<String> allowedExtensions;

    private IncludeLoader() {
        this.allowedPaths = buildAllowedPaths();
        this.allowedExtensions = buildAllowedExtensions();
    }

    public static IncludeLoader instance() {
        if (includeServices == null) {
            synchronized (IncludeLoader.class) {
                if (includeServices == null) {
                    includeServices = new IncludeLoader();
                }
            }
        }
        return includeServices;
    }

    private String[] buildAllowedPaths() {
        File velocityRootPath = new File(Config.CONTEXT.getRealPath(Config.getStringProperty("VELOCITY_ROOT", "/WEB-INF/velocity")));
        File assetPath = new File(APILocator.getFileAssetAPI().getRealAssetsRootPath());

        try {
            return new String[] {velocityRootPath.getCanonicalPath(), assetPath.getCanonicalPath()};
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

    }

    private Set<String> buildAllowedExtensions() {
        String allowedExtensions = Config.getStringProperty("VELOCITY_INCLUDE_ALLOWED_EXTENSIONS", "txt,css,js,html,htm,json");
        allowedExtensions = allowedExtensions.replace(" ", "");

        return ImmutableSet.copyOf(allowedExtensions.split(","));
    }


    private File allowedToServe(final String filePath) throws IOException {
        final File fileToServe = new File(filePath);
        if (!fileToServe.exists()) {
            throw new ResourceNotFoundException("cannot find resource:" + filePath);
        }
        String canonicalPath = fileToServe.getCanonicalPath();

        String fileExtension = UtilMethods.getFileExtension(canonicalPath).toLowerCase();

        if ((canonicalPath.contains(allowedPaths[0]) || canonicalPath.contains(allowedPaths[1]))
                && allowedExtensions.contains(fileExtension)) {
            return fileToServe;

        }
        Logger.warn(this, "POSSIBLE HACK ATTACK DotResourceLoader: " + canonicalPath);
        throw new ResourceNotFoundException("cannot find resource:" + canonicalPath);

    }


    InputStream streamFile(String filePath) throws IOException {
        Logger.debug(this, "Not a CMS Velocity File : " + filePath);
        final File fileToServe = allowedToServe(filePath);
        return new BufferedInputStream(Files.newInputStream(fileToServe.toPath()));
    }


    @Override
    public InputStream writeObject(final VelocityResourceKey key) {
        try {
            return streamFile(key.path);
        } catch (Exception e) {
            throw new ResourceNotFoundException("cannot find resource:" + key.path, e);
        }
    }

    @Override
    public void invalidate(Object obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void invalidate(Object obj, PageMode mode) {
        // TODO Auto-generated method stub

    }
}
