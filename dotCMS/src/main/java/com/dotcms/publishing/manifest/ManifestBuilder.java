package com.dotcms.publishing.manifest;

import java.io.Closeable;
import java.io.File;

/**
 * Represent a Manifest Builder
 */
public interface ManifestBuilder extends Closeable {


    public static String MANIFEST_NAME = "manifest.csv";

    /**
     * Add a INCLUDE register into the Manifest
     * @param manifestItem Asset information
     * @param evaluateReason Reason why the asset was evaluated to be included
     * @param <T> Asset's class
     */
     <T> void include(final ManifestItem manifestItem, final String evaluateReason);

    /**
     * Add a EXCLUDE register into the Manifest
     *
     * @param <T>            Asset's class
     * @param manifestItem   Asset information
     * @param evaluateReason Reason why the asset was evaluated
     * @param excludeReason  Reason why the asset was excluded
     */
     <T> void exclude(final ManifestItem manifestItem, String evaluateReason, final String excludeReason);

     
    /**
     * Return th Manifest File
     * @return
     */
    File getManifestFile();

    /**
     * Add a metadata header to the manifest file
     * @param name
     * @param value
     */
    void addMetadata(final String name, final String value);
}
