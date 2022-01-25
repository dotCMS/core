package com.dotcms.storage;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Metadata generator
 * @author jsanca
 */
public interface MetadataGenerator {

    /**
     * Generates the metadata based on the binary
     * @param binary     {@link File} binary to generate the metadata
     * @param maxLength  {@link Long} max length to parse the content
     * @return Map
     */
    Map<String, Serializable> tikaBasedMetadata(File binary, long maxLength);

    /**
     * Stand alone metadata is the basic MD that can be generated with out having to rely o Tika
     * see
     * @param binary
     * @return
     */
    TreeMap<String, Serializable> standAloneMetadata(final File binary);
}
