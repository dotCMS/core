package com.dotcms.datagen;

import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;

public class FieldDataGen extends AbstractDataGen<Field> {

    private long currentTime = System.currentTimeMillis();
    private String name = "test-field-name-" + currentTime;
    private Field.FieldType type = Field.FieldType.TEXT;
    private Field.DataType dataType = Field.DataType.TEXT;
    private boolean required;
    private String velocityVarName = "test-field-varname-" + currentTime;
    private int sortOrder = 1;
    private String values;
    private String regexCheck;
    private String hint;
    private String defaultValue;
    private boolean indexed;
    private boolean listed;
    private boolean fixed;
    private boolean readOnly;
    private boolean searchable;
    private boolean unique;
    private Structure structure;

    public FieldDataGen(Structure structure) {
        this.structure = structure;
    }

    @SuppressWarnings("unused")
    public FieldDataGen name(String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen type(Field.FieldType type) {
        this.type = type;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen dataType(Field.DataType dataType) {
        this.dataType = dataType;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen required(boolean required) {
        this.required = required;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen velocityVarName(String velocityVarName) {
        this.velocityVarName = velocityVarName;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen sortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen values(String values) {
        this.values = values;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen regexCheck(String regexCheck) {
        this.regexCheck = regexCheck;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen hint(String hint) {
        this.hint = hint;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen defaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen indexed(boolean indexed) {
        this.indexed = indexed;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen listed(boolean listed) {
        this.listed = listed;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen fixed(boolean fixed) {
        this.fixed = fixed;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen readOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen searchable(boolean searchable) {
        this.searchable = searchable;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen unique(boolean unique) {
        this.unique = unique;
        return this;
    }

    public FieldDataGen structure(Structure structure) {
        this.structure = structure;
        return this;
    }

    @Override
    public Field next() {
        Field ff = new Field(name, type, dataType, structure, required, listed, indexed, sortOrder, values,
                defaultValue, regexCheck, fixed, readOnly, searchable);
        ff.setHint(hint);
        ff.setUnique(unique);
        ff.setVelocityVarName(velocityVarName);
        return ff;
    }

    @Override
    public Field persist(Field object) {
        try {
            FieldFactory.saveField(object);
        } catch (DotHibernateException e) {
            throw new RuntimeException("Error persisting Field", e);
        }

        return object;
    }

    public void remove(Field object) {
        try {
            FieldFactory.deleteField(object);
        } catch (DotHibernateException e) {
            throw new RuntimeException("Error persisting Field", e);
        }
    }
}
