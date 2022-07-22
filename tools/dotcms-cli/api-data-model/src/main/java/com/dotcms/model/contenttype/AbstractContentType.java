package com.dotcms.model.contenttype;


import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Date;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = ContentType.class)
public interface AbstractContentType {

    String id();

    String name();

    String variable();

    Date modDate();

    @Nullable
    String publishDateVar();

    @Nullable
    String expireDateVar();

    boolean fixed();

    String host();

    int sortOrder();
}
