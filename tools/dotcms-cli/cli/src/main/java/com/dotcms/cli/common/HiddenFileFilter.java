package com.dotcms.cli.common;

import java.io.File;
import java.io.FileFilter;

/**
 * FileFilter implementation to block hidden files and filter out system specific elements.
 */
public class HiddenFileFilter implements FileFilter {
    @Override
    public boolean accept(File file) {
        return !(file.isFile() && file.isHidden());
    }
}
