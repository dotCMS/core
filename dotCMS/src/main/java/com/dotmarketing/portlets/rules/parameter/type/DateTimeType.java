package com.dotmarketing.portlets.rules.parameter.type;

import com.dotcms.repackage.com.google.common.base.Strings;

import com.dotmarketing.portlets.rules.parameter.type.constraint.StandardConstraints;
import com.dotmarketing.portlets.rules.parameter.type.constraint.TypeConstraint;
import java.time.format.DateTimeParseException;
import org.joda.time.DateTime;

public class DateTimeType extends DataType<DateTime> {

    public DateTimeType() {
        super("dateTime", "api.system.type.datetime");
    }

    @Override
    public DateTime convert(String from) {
        return DateTime.parse(from);
    }

    @Override
    public void checkValid(String value) {
        if(!Strings.isNullOrEmpty(value)) {
            try {
                DateTime.parse(value);
            } catch (DateTimeParseException e) {
                throw new ParameterNotValidException(e, "Could not parse %s into a date-time type.", value);
            }
        }
    }

    public DateTimeType required(){
        return this.restrict(StandardConstraints.required);
    }

    @Override
    public DateTimeType restrict(TypeConstraint restriction) {
        return (DateTimeType)super.restrict(restriction);
    }

}
