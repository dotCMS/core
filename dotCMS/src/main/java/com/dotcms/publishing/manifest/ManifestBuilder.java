package com.dotcms.publishing.manifest;

import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotmarketing.exception.DotRuntimeException;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Represent a Manifest Builder
 */
public interface ManifestBuilder extends Closeable {


    public static String MANIFEST_NAME = "manifest.csv";

    /**
     * Add a INCLUDE register into the Manifest
     * @param manifestItem Asset information
     * @param reason Reason why the asset was include
     * @param <T> Asset's class
     */
     <T> void include(final ManifestItem manifestItem, final String reason);

    /**
     * Add a EXCLUDE register into the Manifest
     * @param manifestItem Asset information
     * @param reason Reason why the asset was exclude
     * @param <T> Asset's class
     */
     <T> void exclude(final ManifestItem manifestItem, final String reason);

     
    /**
     * Return th Manifest File
     * @return
     */
    File getManifestFile();
}
