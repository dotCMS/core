package com.dotcms.rendering.velocity.services;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;

import com.dotmarketing.util.VelocityUtil;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;

import org.apache.velocity.exception.ResourceNotFoundException;

import com.liferay.util.FileUtil;

public class VTLLoader implements DotLoader {
    private static String velocityCanoncalPath;
    private static String assetCanoncalPath;
    private static String assetRealCanoncalPath;
    private static String VELOCITY_ROOT;
    private static VTLLoader vtlServices;

    private VTLLoader() {
        init();
    }

    public static VTLLoader instance() {
        if (vtlServices == null) {
            synchronized (VTLLoader.class) {
                if (vtlServices == null) {
                    vtlServices = new VTLLoader();
                }
            }
        }
        return vtlServices;
    }

    private void init() {
        String velocityRootPath = VelocityUtil.getVelocityRootPath();

        VELOCITY_ROOT = velocityRootPath + File.separator;

        File f = new File(VELOCITY_ROOT);
        try {
            if(f.exists()){
                   velocityCanoncalPath = f.getCanonicalPath();
            }
        } catch (IOException e) {
            Logger.fatal(this,e.getMessage(),e);
        }

        try {
            if(UtilMethods.isSet(Config.getStringProperty("ASSET_REAL_PATH"))){
                f = new File(Config.getStringProperty("ASSET_REAL_PATH"));
                if(f.exists()){
                        assetRealCanoncalPath = f.getCanonicalPath();
                }
            }
        } catch (IOException e) {
            Logger.fatal(this,e.getMessage(),e);
        }

        try {
            if(UtilMethods.isSet(Config.getStringProperty("ASSET_PATH"))){
                f = new File(FileUtil.getRealPath(Config.getStringProperty("ASSET_PATH")));
                if(f.exists()){
                    assetCanoncalPath = f.getCanonicalPath();
                }
            }
        } catch (IOException e) {
            Logger.fatal(this,e.getMessage(),e);
        }


    }

    /**
     * Resolves the file a VTL resource path points at, under the same rules this loader serves
     * it by: the path is tried under the Velocity root -- or under the dynamic content folder,
     * for a {@code dynamic} path -- and then as it was given, and whatever it lands on must
     * canonically sit inside one of the folders Velocity is allowed to read from.
     *
     * <p>The path is written by whoever wrote the Velocity that asked for it, so it may carry
     * {@code ..} segments: canonicalising here is what makes the answer the file that will
     * actually be read rather than the one the text appears to name. A caller that needs to know
     * who wrote the code it is about to run must ask this and not read the path itself.
     *
     * @param filePath The resource path the renderer resolved.
     *
     * @return The canonical file to read, or empty when nothing readable sits there.
     *
     * @throws IOException If the canonical path cannot be worked out.
     */
    public Optional<File> resolveServableFile(final String filePath) throws IOException {

        final String lookingFor = filePath.startsWith("dynamic")
                ? ConfigUtils.getDynamicContentPath() + File.separator + "velocity" + File.separator + filePath
                : VELOCITY_ROOT + filePath;

        File file = new File(lookingFor);
        if (!file.exists()) {
            file = new File(filePath);
        }
        if (!file.exists()) {
            return Optional.empty();
        }

        final File canonical = file.getCanonicalFile();
        final String canon = canonical.getPath();
        final File dynamicContent = new File(ConfigUtils.getDynamicContentPath());

        final boolean serveFile =
                (assetRealCanoncalPath != null && canon.startsWith(assetRealCanoncalPath))
                        || (velocityCanoncalPath != null && canon.startsWith(velocityCanoncalPath))
                        || (assetCanoncalPath != null && canon.startsWith(assetCanoncalPath))
                        || canon.startsWith(dynamicContent.getCanonicalPath());

        if (!serveFile) {
            Logger.warn(this, "POSSIBLE HACK ATTACK DotResourceLoader: " + lookingFor);
            return Optional.empty();
        }
        return Optional.of(canonical);
    }

    /**
     * Streams the VTL resource the given path points at.
     *
     * @param filePath The resource path the renderer resolved.
     *
     * @return The contents of the file that path resolves to.
     *
     * @throws IOException If the file cannot be opened.
     */
    InputStream streamFile(final String filePath) throws IOException {
        Logger.debug(this, "Not a CMS Velocity File : " + filePath);
        final File file = resolveServableFile(filePath)
                .orElseThrow(() -> new ResourceNotFoundException("cannot find resource"));
        return new BufferedInputStream(Files.newInputStream(file.toPath()));
    }


    @Override
    public InputStream writeObject(final VelocityResourceKey key) throws DotDataException, DotSecurityException {
        try {
            return streamFile(key.path);
        } catch (IOException e) {
            throw new ResourceNotFoundException("cannot find resource:" + key.path);
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
