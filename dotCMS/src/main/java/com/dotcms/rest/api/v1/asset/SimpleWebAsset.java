package com.dotcms.rest.api.v1.asset;

import java.time.Instant;
import javax.annotation.Nullable;

public interface SimpleWebAsset {

    String path();
    String name();
    Instant modDate();
    String identifier();
    String inode();

}
