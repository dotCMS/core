package com.dotmarketing.portlets.rules.parameter.display;

import com.dotmarketing.portlets.rules.parameter.type.DataType;

public class DateTimeInput<T extends DataType> extends Input<T> {

    public DateTimeInput(T dataType) {
        super("datetime", dataType);
    }
}
