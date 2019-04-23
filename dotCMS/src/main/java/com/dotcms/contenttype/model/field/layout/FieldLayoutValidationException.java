package com.dotcms.contenttype.model.field.layout;

import com.dotmarketing.exception.DotDataValidationException;

/**
 * Throw when a {@link FieldLayout} is not valid
 *
 * @see FieldLayout#validate()
 */
public class FieldLayoutValidationException extends DotDataValidationException {

    FieldLayoutValidationException(final String message) {
        super(message);
    }
}
