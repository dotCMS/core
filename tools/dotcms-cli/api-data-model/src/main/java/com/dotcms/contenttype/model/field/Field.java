
package com.dotcms.contenttype.model.field;

import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.contenttype.model.field.Field.ClassNameAliasResolver;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value.Default;

@JsonTypeInfo(
        use = Id.CLASS,
        property = "clazz"
)
@JsonTypeIdResolver(value = ClassNameAliasResolver.class)
@JsonSubTypes({
        @Type(value = StoryBlockField.class),
        @Type(value = BinaryField.class),
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

    @Nullable
    public abstract Boolean searchable();

    @Nullable
    public abstract Boolean unique();

    @Nullable
    public abstract Boolean indexed();

    @Nullable
    public abstract Boolean listed();

    @Nullable
    public abstract Boolean readOnly();

    @Nullable
    public abstract Boolean forceIncludeInApi();

    @Nullable
    public abstract String owner();

    @Nullable
    public abstract String id();

    @Nullable
    public abstract String inode();

    @Nullable
    public abstract Date modDate();

    @Default
    public String name() {
        return variable();
    }

    public abstract String variable();

    @Nullable
    public abstract String relationType();

    @Nullable
    public abstract Boolean required();

    @Nullable
    public abstract Integer sortOrder();

    @Nullable
    public abstract String values();

    @Nullable
    public abstract String regexCheck();

    @Nullable
    public abstract String hint();

    @Nullable
    public abstract String defaultValue();

    @Nullable
    public abstract Boolean fixed();

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

    static class ClassNameAliasResolver extends ClassNameIdResolver {

        static TypeFactory typeFactory = TypeFactory.defaultInstance();

        public ClassNameAliasResolver() {
            super(typeFactory.constructType(new TypeReference<Field>() {}), typeFactory, ClientObjectMapper.defaultPolymorphicTypeValidator());
        }

        @Override
        public JavaType typeFromId(final DatabindContext context, final String id) throws IOException {
            final String packageName = Field.class.getPackage().getName();
            if( !id.contains(".") && !id.startsWith(packageName)){
                final String className = String.format("%s.Immutable%s",packageName,id);
                return super.typeFromId(context, className);
            }
            return super.typeFromId(context, id);
        }

    }

}
