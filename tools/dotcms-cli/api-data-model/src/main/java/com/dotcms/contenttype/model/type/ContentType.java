package com.dotcms.contenttype.model.type;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldLayoutRow;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

@JsonTypeInfo(
        use = Id.CLASS,
        property = "clazz"

)
@JsonSubTypes({
        @Type(value = FileAssetContentType.class, name = "FileAssetContentType"),
        @Type(value = FormContentType.class, name = "FormContentType"),
        @Type(value = PageContentType.class, name = "PageContentType"),
        @Type(value = PersonaContentType.class, name = "PersonaContentType"),
        @Type(value = SimpleContentType.class, name = "SimpleContentType"),
        @Type(value = WidgetContentType.class, name = "WidgetContentType"),
        @Type(value = VanityUrlContentType.class, name = "VanityUrlContentType"),
        @Type(value = KeyValueContentType.class, name = "KeyValueContentType"),
        @Type(value = DotAssetContentType.class, name = "DotAssetContentType")
})
@JsonIgnoreProperties(value = { "systemActionMappings", "workflows", "nEntries", "sortOrder", "versionable", "multilingualable" })
public abstract class ContentType {

    @Nullable
    public abstract String id();

    @Nullable
    public abstract String inode();

    public abstract String name();

    public abstract String variable();

    public abstract Date modDate();

    @Nullable
    public abstract String publishDateVar();

    @Nullable
    public abstract String expireDateVar();

    public abstract boolean fixed();

    public abstract Date iDate();

    public abstract String host();

    public abstract String folder();

    @Nullable
    public abstract String icon();

    @Nullable
    public abstract String description();

    @Nullable
    public abstract Boolean defaultType();

    public abstract BaseContentType baseType();

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

}
