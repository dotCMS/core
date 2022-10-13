package com.dotcms.model.contenttype;


import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = ContentType.class)
public interface AbstractContentType {

    String clazz();

    String id();

    @Nullable
    String inode();

    String name();

    String variable();

    Date modDate();

    @Nullable
    String publishDateVar();

    @Nullable
    String expireDateVar();

    boolean fixed();

    Date iDate();

    String host();

    String folder();

    @Nullable
    String icon();

    int sortOrder();

    @Nullable
    String description();

    @Nullable
    Boolean defaultType();

    BaseContentType baseType();

    @Nullable
    Boolean versionable();

    @Nullable
    Boolean system();

    @Nullable
    String owner();

    @Nullable
    Boolean multilingualable();

    List<Field> fields();

}
