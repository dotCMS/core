package com.dotmarketing.common.model;

import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface ContentletSearch {

    @Nullable
    String getId();

    @Nullable
    String getInode();

    @Nullable
    String getIdentifier();

    @Nullable
    String getIndex();

    @Value.Default
    default float getScore() {
        return 0.0f;
    }
}
