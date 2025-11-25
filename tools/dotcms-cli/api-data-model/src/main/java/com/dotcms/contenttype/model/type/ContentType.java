package com.dotcms.contenttype.model.type;

import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldLayoutRow;
import com.dotcms.contenttype.model.type.ContentType.ClassNameAliasResolver;
import com.dotcms.contenttype.model.workflow.Workflow;
import com.dotcms.model.views.CommonViews;
import com.dotcms.model.views.CommonViews.ContentTypeInternalView;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    "nEntries",
    "sortOrder",
    "versionable",
    "multilingualable",
    "pagination",
    "layout"
})
@JsonInclude(Include.NON_DEFAULT)
@Value.Style(passAnnotations = {JsonInclude.class})
public abstract class ContentType {

    public static final String SYSTEM_HOST = "SYSTEM_HOST";
    public static final String SYSTEM_FOLDER = "SYSTEM_FOLDER";

    static final String TYPE = "ContentType";

    @JsonView(CommonViews.InternalView.class)
    @JsonProperty("dotCMSObjectType")
    @Value.Derived
    public String dotCMSObjectType() {
        return TYPE;
    }

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

    /**
     * The modDate attribute is marked as auxiliary to exclude it from the equals, hashCode, and
     * toString methods. This ensures that two instances of ContentType can be considered equal even
     * if their modDate values differ. This decision was made because under certain circumstances,
     * the modDate value is set using the current date.
     */
    @Value.Auxiliary
    @Nullable
    public abstract Date modDate();

    @Nullable
    public abstract String publishDateVar();

    @Nullable
    public abstract String expireDateVar();

    @Value.Default
    public Boolean fixed() {
        return false;
    }

    /**
     * The iDate attribute is marked as auxiliary to exclude it from the equals, hashCode, and
     * toString methods. This ensures that two instances of ContentType can be considered equal even
     * if their iDate values differ. This decision was made because under certain circumstances, the
     * iDate value is set using the current date.
     */
    @Value.Auxiliary
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

    @Value.Default
    public Boolean defaultType() {
        return false;
    }

    @Value.Default
    public BaseContentType baseType() { return BaseContentType.CONTENT; }

    @Value.Default
    public Boolean system() {
        return false;
    }

    @Nullable
    public abstract String owner();

    public abstract List<Field> fields();

    @Nullable
    public abstract List<FieldLayoutRow> layout();

    @JsonAlias("detailPagePath")
    @JsonProperty("detailPage")
    @Nullable
    public abstract String detailPage();

    @Nullable
    public abstract String urlMapPattern();

    @Nullable
    @Value.Default
    public Map<String, ? extends Object> metadata() {
        return Collections.emptyMap();
    }

    @JsonView({ContentTypeInternalView.class})
    @Value.Default
    public List<Workflow> workflows() {
        return Collections.emptyList();
    }

    @JsonDeserialize(using = SystemActionMappingsDeserializer.class)
    @JsonInclude(Include.NON_NULL)
    @Nullable
    public abstract JsonNode systemActionMappings();

    /**
     * Class id resolver allows us using smaller ClassNames that eventually get mapped to the fully qualified class name
     */
    public static class ClassNameAliasResolver extends ClassNameIdResolver {

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
