package com.dotcms.contenttype.model.field.event;

public class FieldDeletedEvent {
    private String fieldVar;

    public FieldDeletedEvent(String fieldVar) {
        this.fieldVar = fieldVar;
    }

    public String getFieldVar() {
        return fieldVar;
    }
}
