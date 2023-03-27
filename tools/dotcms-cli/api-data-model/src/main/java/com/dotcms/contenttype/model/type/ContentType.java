package com.dotcms.contenttype.model.type;

import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldLayoutRow;
import com.dotcms.contenttype.model.type.ContentType.ClassNameAliasResolver;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
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
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

@JsonTypeInfo(use = Id.CLASS, property = "clazz")
@JsonTypeIdResolver(value = ClassNameAliasResolver.class)
@JsonSubTypes({
        @Type(value = FileAssetContentType.class),
        @Type(value = FormContentType.class),
        @Type(value = PageContentType.class),
        @Type(value = PersonaContentType.class),
        @Type(value = SimpleContentType.class),
        @Type(value = WidgetContentType.class),
        @Type(value = VanityUrlContentType.class),
        @Type(value = KeyValueContentType.class),
        @Type(value = DotAssetContentType.class)
})
@JsonIgnoreProperties(value = {
    "systemActionMappings",
    "workflows",
    "nEntries",
    "sortOrder",
    "versionable",
    "multilingualable",
    "pagination"
})
public abstract class ContentType {

    public static final String SYSTEM_HOST = "SYSTEM_HOST";
    public static final String SYSTEM_FOLDER = "SYSTEM_FOLDER";

    @Nullable
    public abstract String id();

    @Nullable
    @Value.Lazy
    public String inode() { return id(); }

    @Default
    public String name() {
        return variable();
    }

    @Nullable
    public abstract String variable();

    @Nullable
    public abstract Date modDate();

    @Nullable
    public abstract String publishDateVar();

    @Nullable
    public abstract String expireDateVar();

    @Nullable
    public abstract Boolean fixed();

    @Nullable
    public abstract Date iDate();

    @Value.Default
    public  String host() { return SYSTEM_HOST; }

    @Value.Default
    public String folder() { return SYSTEM_FOLDER; }

    @Nullable
    public abstract String siteName();

    @Nullable
    public abstract String canonicalSiteName();

    @Nullable
    public abstract String folderPath();

    @Nullable
    public abstract String canonicalFolderPath();

    @Nullable
    public abstract String icon();

    @Nullable
    public abstract String description();

    @Nullable
    public abstract Boolean defaultType();

    @Value.Default
    public BaseContentType baseType() { return BaseContentType.CONTENT; };

    @Nullable
    public abstract Boolean system();

    @Nullable
    public abstract String owner();

    public abstract List<Field> fields();

    @Nullable
    public abstract List<FieldLayoutRow> layout();

    @Nullable
    public abstract String detailPage();

    @Nullable
    public abstract String urlMapPattern();

    /**
     * Class id resolver allows us using smaller ClassNames that eventually get mapped to the fully qualified class name
     */
    static class ClassNameAliasResolver extends ClassNameIdResolver {

        static final String IMMUTABLE = "Immutable";

        static TypeFactory typeFactory = TypeFactory.defaultInstance();

        public ClassNameAliasResolver() {
            super(typeFactory.constructType(new TypeReference<ContentType>() {}), typeFactory, ClientObjectMapper.defaultPolymorphicTypeValidator());
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
           final String packageName = ContentType.class.getPackageName();
           if( !id.contains(".") && !id.startsWith(packageName)){
               final String className = String.format("%s.Immutable%s",packageName,id);
               return super.typeFromId(context, className);
           }
           return super.typeFromId(context, id);
        }

    }

}
