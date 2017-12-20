package com.dotcms.rendering.velocity.services;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

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
        String velocityRootPath = Config.getStringProperty("VELOCITY_ROOT", "/WEB-INF/velocity");
        if (velocityRootPath.startsWith("/WEB-INF")) {
            String startPath = velocityRootPath.substring(0, 8);
            String endPath = velocityRootPath.substring(9, velocityRootPath.length());
            velocityRootPath = FileUtil.getRealPath(startPath) + File.separator + endPath;
        } 

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

    InputStream streamFile(String filePath) throws IOException {
        boolean serveFile = false;
        Logger.debug(this, "Not a CMS Velocity File : " + filePath);

        java.io.File f = null;
        String lookingFor = "";
        if (filePath.startsWith("dynamic")) {
            lookingFor = ConfigUtils.getDynamicContentPath() + File.separator + "velocity" + File.separator + filePath;

        } else {
            lookingFor = VELOCITY_ROOT + filePath;
        }
        f = new java.io.File(lookingFor);
        if (!f.exists()) {
            f = new java.io.File(filePath);
        }
        if (!f.exists()) {
            throw new ResourceNotFoundException("cannot find resource");
        }
        String canon = f.getCanonicalPath();
        File dynamicContent = new File(ConfigUtils.getDynamicContentPath());

        if (assetRealCanoncalPath != null && canon.startsWith(assetRealCanoncalPath)) {
            serveFile = true;
        } else if (velocityCanoncalPath != null && canon.startsWith(velocityCanoncalPath)) {
            serveFile = true;
        } else if (assetCanoncalPath != null && canon.startsWith(assetCanoncalPath)) {
            serveFile = true;
        } else if (canon.startsWith(dynamicContent.getCanonicalPath())) {
            serveFile = true;
        }
        if (!serveFile) {
            Logger.warn(this, "POSSIBLE HACK ATTACK DotResourceLoader: " + lookingFor);
            throw new ResourceNotFoundException("cannot find resource");
        }
        return new BufferedInputStream(Files.newInputStream(f.toPath()));

    }


    @Override
    public InputStream writeObject(String id1, String id2, boolean live, String language, String filePath) {
        try {
            return streamFile(filePath);
        } catch (IOException e) {
            throw new ResourceNotFoundException("cannot find resource:" + filePath);
        }
    }

    @Override
    public void invalidate(Object obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void invalidate(Object obj, boolean live) {
        // TODO Auto-generated method stub

    }
}
