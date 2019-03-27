package com.dotcms.contenttype.model.field.event;

import com.dotcms.contenttype.model.field.Field;

public class FieldSavedEvent {
    private Field field;

    public FieldSavedEvent(Field field) {
        this.field = field;
    }

    public Field getField() {
        return field;
    }
}
