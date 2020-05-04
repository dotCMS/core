package com.dotcms.storage;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * This class is in charge of resolve File (on diff storages), metadata, etc.
 * @author jsanca
 */
public interface FileStorageAPI {

    /**
     * Gets the basic metadata from the binary
     * @param binary {@link File} file to get the information
     * @return Map with the metadata
     */
    Map<String, Object> generateBasicMetaData(final File binary) throws IOException;

    /**
     * Gets the full metadata from the binary, this could involved a more expensive process such as Tika
     * @param binary  {@link File} file to get the information
     * @return Map with the metadata
     */
    Map<String, Object> generateFullMetaData(final File binary);
}
