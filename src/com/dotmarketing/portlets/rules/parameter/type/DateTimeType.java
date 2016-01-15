package com.dotmarketing.portlets.rules.parameter.type;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.base.Strings;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class DateTimeType extends DataType {
    @Override
    public Object convert(String from) {
        return null;
    }

    @Override
    public void checkValid(String value) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(value));
        try {
            LocalDate.parse(value);
        } catch(DateTimeParseException e) {
            throw new ParameterNotValidException(e, "Could not parse %s into a date-time type.", value);
        }
    }

    public DateTimeType() {
        super("dateTime");
    }
}
