package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

@JsonTypeInfo(
        use = Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "clazz"
)
@JsonSubTypes({
        @Type(value = StoryBlockField.class),
        @Type(value = BinaryField.class,name = "BinaryField"),
        @Type(value = CategoryField.class),
        @Type(value = CheckboxField.class),
        @Type(value = ConstantField.class),
        @Type(value = CustomField.class),
        @Type(value = DateField.class),
        @Type(value = DateTimeField.class),
        @Type(value = EmptyField.class),
        @Type(value = FileField.class),
        @Type(value = HiddenField.class),
        @Type(value = HostFolderField.class),
        @Type(value = ImageField.class),
        @Type(value = KeyValueField.class),
        @Type(value = LineDividerField.class),
        @Type(value = MultiSelectField.class),
        @Type(value = PermissionTabField.class),
        @Type(value = RadioField.class),
        @Type(value = RelationshipField.class),
        @Type(value = RelationshipsTabField.class),
        @Type(value = SelectField.class),
        @Type(value = TabDividerField.class),
        @Type(value = TagField.class),
        @Type(value = TextAreaField.class),
        @Type(value = TextField.class),
        @Type(value = TimeField.class),
        @Type(value = WysiwygField.class),
        @Type(value = RowField.class),
        @Type(value = ColumnField.class),
})
public abstract class Field {

    public abstract boolean searchable();

    public abstract boolean unique();

    public abstract boolean indexed();

    public abstract boolean listed();

    public abstract boolean readOnly();

    public abstract boolean forceIncludeInApi();

    @Nullable
    public abstract String owner();

    @Nullable
    public abstract String id();

    @Nullable
    public abstract String inode();

    public abstract Date modDate();

    public abstract String name();

    @Nullable
    public abstract String relationType();

    public abstract boolean required();

    public abstract String variable();

    public abstract int sortOrder();

    @Nullable
    public abstract String values();

    @Nullable
    public abstract String regexCheck();

    @Nullable
    public abstract String hint();

    @Nullable
    public abstract String defaultValue();

    public abstract boolean fixed();

    public abstract DataTypes dataType();

    @Nullable
    public abstract String contentTypeId();

    @Nullable
    public abstract String fieldType();

    @Nullable
    public abstract String fieldTypeLabel();

    @Nullable
    public abstract List<FieldVariable> fieldVariables();

    @Nullable
    public abstract Date iDate();

    @Nullable
    public abstract List<ContentTypeFieldProperties> fieldContentTypeProperties();

}
