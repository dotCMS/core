package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.util.importer.ImportLineValidationCodes;
import com.dotmarketing.util.importer.exception.ImportLineErrorAware;
import java.util.Optional;

public class DotNumericFieldException extends DotContentletStateException implements
        ImportLineErrorAware {

    public static final String INVALID_NUMERIC_FIELD = "Unable to set string value as a Float for the field: ";

    private final String field;
    private final String value;

    public DotNumericFieldException(String field, Object value) {
        super(INVALID_NUMERIC_FIELD + field );
        this.field = field;
        this.value = value == null ? "" : value.toString();
    }

    @Override
    public Optional<String> getField() {
        return Optional.of(field);
    }

    @Override
    public Optional<String> getValue() {
        return Optional.of(value);
    }

    @Override
    public String getCode() {
        return ImportLineValidationCodes.INVALID_NUMBER_FORMAT.name();
    }
}
