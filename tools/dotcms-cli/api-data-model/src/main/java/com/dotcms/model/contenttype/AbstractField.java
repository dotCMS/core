package com.dotcms.model.contenttype;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Date;
import javax.annotation.Nullable;
import org.immutables.value.Value;

//@JsonTypeInfo(
  //      use = Id.CLASS,
  //      include = JsonTypeInfo.As.PROPERTY,
  //      property = "clazz"
//)
@ValueType
@Value.Immutable
@JsonDeserialize(as = Field.class)
public interface AbstractField {

    String clazz();

    boolean searchable();

    boolean unique();

    boolean indexed();

    boolean listed();

    boolean readOnly();

    boolean forceIncludeInApi();

    @Nullable
    String owner();

    @Nullable
    String id();

    @Nullable
    String inode();

    Date modDate();

    String name();

    @Nullable
    String relationType();

    boolean required();

    String variable();

    int sortOrder();

    @Nullable
    String values();

    @Nullable
    String regexCheck();

    @Nullable
    String hint();

    @Nullable
    String defaultValue();

    boolean fixed();

}
