package com.dotcms.storage;

import com.dotmarketing.exception.DotDataException;

/**
 * Thrown by a storage provider when a stored object exists but cannot be deserialized, for example
 * a zero-length or truncated local metadata file. It is a read failure, not absence, so the object
 * is never treated as missing and regenerated. {@link ChainableStoragePersistenceAPI} uses it to
 * replace the unreadable copy from a later provider that holds a readable one.
 */
public class UnreadableStoredObjectException extends DotDataException {

    /**
     * Creates the exception.
     *
     * @param message describes the unreadable object
     * @param cause   the deserialization failure
     */
    public UnreadableStoredObjectException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
