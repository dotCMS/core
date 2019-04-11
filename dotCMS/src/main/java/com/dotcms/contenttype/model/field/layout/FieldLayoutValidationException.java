package com.dotcms.contenttype.model.field.layout;

import com.dotmarketing.exception.DotDataValidationException;

public class FieldLayoutValidationException extends DotDataValidationException {

    FieldLayoutValidationException(final String message) {
        super(message);
    }
}
