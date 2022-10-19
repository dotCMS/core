package com.dotcms.contenttype.model.type;

import com.dotcms.contenttype.model.field.Field;
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
        @Type(value = FormContentType.class),
        @Type(value = PageContentType.class),
        @Type(value = PersonaContentType.class),
        @Type(value = SimpleContentType.class),
        @Type(value = WidgetContentType.class),
        @Type(value = VanityUrlContentType.class),
        @Type(value = KeyValueContentType.class),
        @Type(value = DotAssetContentType.class)
})
public abstract class ContentType {

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

    public abstract int sortOrder();

    @Nullable
    public abstract String description();

    @Nullable
    public abstract Boolean defaultType();

    public abstract BaseContentType baseType();

    @Nullable
    public abstract Boolean versionable();

    @Nullable
    public abstract Boolean system();

    @Nullable
    public abstract String owner();

    @Nullable
    public abstract Boolean multilingualable();

    public abstract List<Field> fields();

}
