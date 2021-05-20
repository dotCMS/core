package com.dotcms.publishing.output;

import com.dotcms.enterprise.publishing.bundlers.ShortyBundler;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.io.IOException;

public class AddFileNotPossibleException extends IOException {

    private String filePath;

    public AddFileNotPossibleException(final Throwable cause, final String filePath) {
        super(cause);
        this.filePath = filePath;
    }

    @Override
    public String getMessage(){
        final Throwable cause = this.getCause();

        final String message = cause.getMessage().contains("Not a directory") ?
                "At least one subfolder is really a file" : cause.getMessage();

        return String.format("It is not possible create the File: %s because: %s", filePath,
                message);
    }
}
