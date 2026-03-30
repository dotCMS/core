package com.dotcms.content.index.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableInitIndexInfo.class)
@JsonDeserialize(as = ImmutableInitIndexInfo.class)
public interface InitIndexInfo {

    String timeStampOS();

    String timeStampES();

    static InitIndexInfo empty() {
        return ImmutableInitIndexInfo.builder()
                .timeStampOS("")
                .timeStampES("")
                .build();
    }

}
