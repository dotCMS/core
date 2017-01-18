package com.dotmarketing.util.file;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by Oscar Arrieta on 1/18/17.
 */
public class FilteredFile extends FileWrapper {

    private final FileFilter fileFilter;

    public FilteredFile(File wrappee, FileFilter fileFilter) {
        super(wrappee);
        this.fileFilter = fileFilter;
    }

    @Override
    public File[] listFiles() {
        File[] files =  super.listFiles(this.fileFilter);
        FilteredFile[] filteredFiles = new FilteredFile[files.length];
        for (int i = 0; i < files.length; i++) {
            filteredFiles[i] = new FilteredFile(files[i], fileFilter);
        }
        return filteredFiles;
    }
}
