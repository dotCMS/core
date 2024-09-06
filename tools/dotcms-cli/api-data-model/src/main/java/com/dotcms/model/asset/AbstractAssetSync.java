package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = AssetSync.class)
public interface AbstractAssetSync {

    @Value.Default
    default boolean markedForPush(){return false;}

    @Value.Default
    default boolean markedForDelete(){return false;}

    @Value.Default
    default PushType pushType() {return PushType.UNKNOWN;}

    enum PushType {
        NEW, MODIFIED, UNKNOWN
    }

}
