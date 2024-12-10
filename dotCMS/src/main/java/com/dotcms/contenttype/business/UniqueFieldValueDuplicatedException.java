package com.dotcms.contenttype.business;

import java.util.List;

/**
 * Throw if try to insert a duplicated register in unique_fiedls table
 */
public class UniqueFieldValueDuplicatedException extends Exception{

    private List<String> contentletsIDS;

    public UniqueFieldValueDuplicatedException(String message) {
        super(message);
    }

    public UniqueFieldValueDuplicatedException(String message, List<String> contentletsIDS) {
        super(message);
        this.contentletsIDS = contentletsIDS;
    }

    public List<String> getContentlets() {
        return contentletsIDS;
    }
}
