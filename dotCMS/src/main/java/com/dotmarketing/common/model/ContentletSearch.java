package com.dotmarketing.common.model;

import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface ContentletSearch {

    @Nullable
    String id();

    @Nullable
    String inode();

    @Nullable
    String identifier();

    @Nullable
    String index();

    @Value.Default
    default float score() {
        return 0.0f;
    }
}