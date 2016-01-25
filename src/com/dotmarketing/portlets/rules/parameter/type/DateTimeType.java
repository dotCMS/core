package com.dotmarketing.portlets.rules.parameter.type;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.base.Strings;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class DateTimeType extends DataType {

    private int minLength = 0;

    @Override
    public Object convert(String from) {
        return null;
    }

    @Override
    public void checkValid(String value) {
        if(!Strings.isNullOrEmpty(value)) {
            try {
                LocalDateTime.parse(value);
            } catch (DateTimeParseException e) {
                throw new ParameterNotValidException(e, "Could not parse %s into a date-time type.", value);
            }
        }
    }

    public DateTimeType() {
        super("dateTime", "api.system.type.datetime");
    }

    public DateTimeType minLength(int minLength) {
        this.minLength = minLength;
        return this;
    }
}
