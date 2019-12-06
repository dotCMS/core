package com.dotmarketing.portlets.folders.exception;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;

/**
 * Exception thrown when a {@link com.dotmarketing.portlets.folders.model.Folder} is tried
 * to be persisted with an invalid name. Folder names are validated by
 * {@link com.dotmarketing.portlets.folders.business.FolderAPIImpl#validateFolderName(String)}
 *
 */

public class InvalidFolderNameException extends DotRuntimeException {

    public InvalidFolderNameException(final String reservedFolderNameErrorMessage) {
        super(reservedFolderNameErrorMessage);
    }
}
