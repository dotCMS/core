package com.dotmarketing.portlets.folders.exception;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.model.Folder;

/**
 * Exception thrown when a {@link com.dotmarketing.portlets.folders.model.Folder} is tried
 * to be persisted with an invalid name. Folder names are validated by
 * {@link com.dotmarketing.portlets.folders.business.FolderFactoryImpl#validateFolderName(Folder)}
 *
 */

public class InvalidFolderNameException extends DotRuntimeException {

    public InvalidFolderNameException(final String reservedFolderNameErrorMessage) {
        super(reservedFolderNameErrorMessage);
    }
}
