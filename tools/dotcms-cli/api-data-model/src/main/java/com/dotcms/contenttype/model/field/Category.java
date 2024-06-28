package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableCategory.class)
@JsonDeserialize(as = ImmutableCategory.class)
public interface Category {

    @Nullable
    String categoryName();

    @Nullable
    String description();

    @Nullable
    String inode();

    @Nullable
    String key();

    @Nullable
    String keywords();

    @Nullable
    Integer sortOrder();

}
