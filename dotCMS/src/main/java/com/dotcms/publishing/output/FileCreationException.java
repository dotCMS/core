package com.dotcms.publishing.output;


import java.io.IOException;

public class FileCreationException extends IOException {

    private String filePath;

    public FileCreationException(final Throwable cause, final String filePath) {
        super(cause);
        this.filePath = filePath;
    }

    @Override
    public String getMessage(){
        final Throwable cause = this.getCause();

        final String message = cause.getMessage().contains("Not a directory") ?
                "At least one subfolder already exists as a file" : cause.getMessage();

        return String.format("It is not possible create the File: %s because: %s", filePath,
                message);
    }
}
