package com.dotcms.datagen;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

public class FieldVariableDataGen extends AbstractDataGen<FieldVariable> {

    private String key;
    private String value;
    private Field field;

    public FieldVariableDataGen key(final String newKey){
        this.key = newKey;
        return this;
    }

    public FieldVariableDataGen value(final String newValue){
        this.value = newValue;
        return this;
    }

    public FieldVariableDataGen field(final Field newField){
        this.field = newField;
        return this;
    }

    @Override
    public FieldVariable next() {
        return ImmutableFieldVariable.builder()
                .key(key)
                .value(value)
                .fieldId(field.id()).build();
    }

    @Override
    public FieldVariable persist(final FieldVariable fieldVariable) {
        try {
            return APILocator.getContentTypeFieldAPI().save(fieldVariable, APILocator.systemUser());
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
