package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.util.importer.ImportLineValidationCodes;
import com.dotmarketing.util.importer.exception.ImportLineError;
import java.util.Optional;

public class DotNumericFieldException extends DotContentletStateException implements
        ImportLineError {

    public static final String INVALID_NUMERIC_FIELD_MESSAGE = "Unable to set string value '%s' as a %s for the field: %s";

    private final String field;
    private final String value;

    DotNumericFieldException(String message, String field, Object value) {
        super(message);
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

    public static DotNumericFieldException newLongFieldException(String field, Object value) {
        return new DotNumericFieldException(
                String.format(INVALID_NUMERIC_FIELD_MESSAGE,value,"Long",field),
                field, value
        );
    }

    public static DotNumericFieldException newFloatFieldException(String field, Object value) {
        return new DotNumericFieldException(
                String.format(INVALID_NUMERIC_FIELD_MESSAGE,value,"Float",field),
                field, value
        );
    }
}
