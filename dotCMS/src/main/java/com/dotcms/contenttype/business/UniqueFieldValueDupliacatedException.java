package com.dotcms.contenttype.business;

/**
 * Throw if try to insert a duplicated register in unique_fiedls table
 */
public class UniqueFieldValueDupliacatedException extends Exception{

    public UniqueFieldValueDupliacatedException(String message) {
        super(message);
    }
}
