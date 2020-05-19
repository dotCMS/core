package com.dotcms.storage;

import java.io.File;
import java.util.Map;

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
    Map<String, Object> generate(File binary, long maxLength);
}
