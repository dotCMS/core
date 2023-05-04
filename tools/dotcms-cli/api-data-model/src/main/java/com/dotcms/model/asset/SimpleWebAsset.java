package com.dotcms.model.asset;

import java.time.Instant;
import javax.annotation.Nullable;

public interface SimpleWebAsset {

    @Nullable
    String site();

    String path();

    String name();

    @Nullable
    Instant modDate();

    @Nullable
    String identifier();

    @Nullable
    String inode();

}
