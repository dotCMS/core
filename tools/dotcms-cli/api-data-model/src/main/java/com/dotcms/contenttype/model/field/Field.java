
package com.dotcms.contenttype.model.field;

import com.dotcms.api.provider.ClientObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

@JsonTypeInfo(
        use = Id.CLASS,
        property = "clazz"
)
@JsonTypeIdResolver(value = Field.ClassNameAliasResolver.class)
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
        @Type(value = JSONField.class),
})
@JsonInclude(Include.NON_DEFAULT)
@Value.Style(passAnnotations = {JsonInclude.class})
public abstract class Field {

    @Value.Default
    public Boolean searchable() {
        return false;
    }

    @Value.Default
    public Boolean unique() {
        return false;
    }

    @Value.Default
    public Boolean indexed() {
        return false;
    }

    @Value.Default
    public Boolean listed() {
        return false;
    }

    @Value.Default
    public Boolean readOnly() {
        return false;
    }

    @Value.Default
    public Boolean forceIncludeInApi() {
        return false;
    }

    @Nullable
    public abstract String owner();

    @Nullable
    public abstract String id();

    @Nullable
    public abstract String inode();

    /**
     * The modDate attribute is marked as auxiliary to exclude it from the equals, hashCode, and
     * toString methods. This ensures that two instances of Field can be considered equal even if
     * their modDate values differ. This decision was made because under certain circumstances, the
     * modDate value is set using the current date.
     */
    @Value.Auxiliary
    @Nullable
    public abstract Date modDate();

    @Default
    public String name() {
        return variable();
    }

    public abstract String variable();

    @Nullable
    public abstract String relationType();

    @Value.Default
    public Boolean required() {
        return false;
    }

    @Value.Default
    public Integer sortOrder() {
        return 0;
    }

    @Nullable
    public abstract String values();

    @Nullable
    public abstract String regexCheck();

    @Nullable
    public abstract String hint();

    @Nullable
    public abstract String defaultValue();

    @Value.Default
    public Boolean fixed() {
        return false;
    }

    public abstract DataTypes dataType();

    @JsonIgnore
    @Nullable
    public abstract String contentTypeId();

    @Nullable
    public abstract String fieldType();

    @Nullable
    public abstract String fieldTypeLabel();

    @Value.Default
    public List<FieldVariable> fieldVariables() {
        return Collections.emptyList();
    }

    /**
     * The iDate attribute is marked as auxiliary to exclude it from the equals, hashCode, and
     * toString methods. This ensures that two instances of Field can be considered equal even if
     * their iDate values differ. This decision was made because under certain circumstances, the
     * iDate value is set using the current date.
     */
    @Value.Auxiliary
    @Nullable
    public abstract Date iDate();

    @Value.Default
    public List<ContentTypeFieldProperties> fieldContentTypeProperties() {
        return Collections.emptyList();
    }

    public static class ClassNameAliasResolver extends ClassNameIdResolver {

        static final String IMMUTABLE = "Immutable";

        static TypeFactory typeFactory = TypeFactory.defaultInstance();

        public ClassNameAliasResolver() {
            super(typeFactory.constructType(new TypeReference<Field>() {}), typeFactory, ClientObjectMapper.defaultPolymorphicTypeValidator());
        }

        @Override
        public String idFromValue(Object value) {
            final String simpleName = value.getClass().getSimpleName();
            if(simpleName.startsWith(IMMUTABLE)){
               return simpleName.replace(IMMUTABLE,"");
            }
            return super.idFromValue(value);
        }

        @Override
        public JavaType typeFromId(final DatabindContext context, final String id) throws IOException {
            final String packageName = Field.class.getPackageName();
            if( !id.contains(".") && !id.startsWith(packageName)){
                final String className = String.format("%s.Immutable%s", packageName,id);
                return super.typeFromId(context, className);
            }
            return super.typeFromId(context, id);
        }

    }

}
