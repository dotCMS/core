package com.dotcms.contenttype.model.field.event;

import com.dotcms.contenttype.model.field.Field;

public class FieldDeletedEvent {
    private Field field;

    public FieldDeletedEvent(Field field) {
        this.field = field;
    }

    public String getFieldVar() {
        return field.variable();
    }

    public Field getField(){
        return field;
    }
}
