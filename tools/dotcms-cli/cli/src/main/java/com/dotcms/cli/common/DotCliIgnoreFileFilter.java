package com.dotcms.cli.common;

import java.io.File;
import java.io.FileFilter;

/**
 * FileFilter implementation that combines hidden file filtering with .dotcliignore pattern
 * matching. This filter blocks:
 * <ul>
 *   <li>Hidden files (files starting with '.' on Unix systems or with hidden attribute on Windows)</li>
 *   <li>Files and directories matching patterns in .dotcliignore file</li>
 * </ul>
 */
public class DotCliIgnoreFileFilter implements FileFilter {

    private final DotCliIgnore dotCliIgnore;

    /**
     * Creates a new DotCliIgnoreFileFilter with the specified DotCliIgnore instance.
     *
     * @param dotCliIgnore the DotCliIgnore instance to use for pattern matching
     */
    public DotCliIgnoreFileFilter(DotCliIgnore dotCliIgnore) {
        this.dotCliIgnore = dotCliIgnore;
    }

    /**
     * Tests whether the specified file should be accepted (not filtered out).
     * A file is accepted if it is NOT:
     * <ul>
     *   <li>A hidden file</li>
     *   <li>Matching any ignore pattern in .dotcliignore</li>
     * </ul>
     *
     * @param file the file to test
     * @return true if the file should be accepted, false otherwise
     */
    @Override
    public boolean accept(File file) {
        // First check if it's a hidden file (original HiddenFileFilter logic)
        if (file.isFile() && file.isHidden()) {
            return false;
        }

        // Then check if it matches any ignore pattern
        return !dotCliIgnore.shouldIgnore(file);
    }
}
