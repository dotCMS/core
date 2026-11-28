package com.dotcms.content.export;

/**
 * How {@link ContentletCsvWriter} writes the host/folder column of a contentlet.
 */
public enum HostFolderMode {

    /**
     * The value Content Search has always written: the host identifier when the contentlet sits in
     * a site's system folder, otherwise the folder inode.
     */
    IDENTIFIER,

    /**
     * A readable location that the content import resolves back to the same place:
     * {@code //host/path/} for content in a folder, the bare host name for content at a site root,
     * and {@code SYSTEM_HOST} for content in System Host.
     */
    LOCATION
}
