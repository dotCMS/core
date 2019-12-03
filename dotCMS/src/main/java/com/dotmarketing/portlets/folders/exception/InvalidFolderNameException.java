package com.dotmarketing.portlets.folders.exception;

import com.dotmarketing.exception.DotDataException;

public class InvalidFolderNameException extends DotDataException {

    public InvalidFolderNameException(final String reservedFolderNameErrorMessage) {
        super(reservedFolderNameErrorMessage);
    }
}
