package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TextField;
import com.dotmarketing.business.APILocator;

public class FieldDataGen extends AbstractDataGen<Field> {

    private String name;
    private Class type = TextField.class;
    private boolean required = Boolean.FALSE;
    private String velocityVarName;
    private int sortOrder = 1;
    private String values;
    private String hint;
    private String defaultValue;
    private boolean indexed = Boolean.TRUE;
    private boolean listed = Boolean.FALSE;
    private boolean fixed = Boolean.FALSE;
    private boolean readOnly = Boolean.FALSE;
    private boolean searchable = Boolean.TRUE;
    private boolean unique = Boolean.FALSE;
    private String contentTypeId;
    private String relationType;
    private String id;

    public FieldDataGen() {

        final long currentTime = System.currentTimeMillis();
        name = "testFieldName" + currentTime;
        velocityVarName = "testFieldVarname" + currentTime;
        values = "testValue" + currentTime;
        hint = "testHint" + currentTime;
        defaultValue = "testDefaultValue" + currentTime;
    }

    @SuppressWarnings("unused")
    public FieldDataGen name(final String name) {
        this.name = name;
        return this;
    }

    public FieldDataGen id(final String id) {
        this.id = id;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen type(final Class type) {
        this.type = type;
        return this;
    }

    public FieldDataGen contentTypeId(final String contentTypeId) {
        this.contentTypeId = contentTypeId;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen required(final boolean required) {
        this.required = required;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen velocityVarName(final String velocityVarName) {
        this.velocityVarName = velocityVarName;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen sortOrder(final int sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen values(final String values) {
        this.values = values;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen hint(final String hint) {
        this.hint = hint;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen defaultValue(final String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen relationType(final String relationType) {
        this.relationType = relationType;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen indexed(final boolean indexed) {
        this.indexed = indexed;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen listed(final boolean listed) {
        this.listed = listed;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen fixed(final boolean fixed) {
        this.fixed = fixed;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen readOnly(final boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen searchable(final boolean searchable) {
        this.searchable = searchable;
        return this;
    }

    @SuppressWarnings("unused")
    public FieldDataGen unique(final boolean unique) {
        this.unique = unique;
        return this;
    }

    @Override
    public Field next() {

        return FieldBuilder
                .builder(type)
                .id(id)
                .name(name)
                .contentTypeId(contentTypeId)
                .required(required)
                .variable(velocityVarName)
                .sortOrder(sortOrder)
                .values(values)
                .hint(hint)
                .defaultValue(defaultValue)
                .indexed(indexed)
                .listed(listed)
                .fixed(fixed)
                .readOnly(readOnly)
                .searchable(searchable)
                .unique(unique)
                .relationType(relationType)
                .build();
    }

    @WrapInTransaction
    @Override
    public Field persist(Field field) {

        try {
            field = APILocator.getContentTypeFieldAPI().save(field, APILocator.systemUser(), false);
        } catch (Exception e) {
            throw new RuntimeException("Error persisting Field", e);
        }

        return field;
    }

    /**
     * Creates a new {@link Field} instance and persists it in DB
     *
     * @return A new Field instance persisted in DB
     */
    @Override
    public Field nextPersisted() {
        return persist(next());
    }

    @WrapInTransaction
    public static void remove(Field field) {
        try {
            APILocator.getContentTypeFieldAPI().delete(field);
        } catch (Exception e) {
            throw new RuntimeException("Error persisting Field", e);
        }
    }

}