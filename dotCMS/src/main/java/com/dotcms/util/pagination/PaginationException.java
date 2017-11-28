package com.dotcms.util.pagination;

/**
 * Thrown to indicate that a Exception occur in a pagination proccess.
 */
public class PaginationException extends RuntimeException{

    public PaginationException(final Throwable cause) {
        super(cause);
    }
}
