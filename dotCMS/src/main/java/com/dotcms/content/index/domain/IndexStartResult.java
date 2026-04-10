package com.dotcms.content.index.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableIndexStartResult.class)
@JsonDeserialize(as = ImmutableIndexStartResult.class)
public interface IndexStartResult {

    String timeStampOS();

    String timeStampES();

    static IndexStartResult empty() {
        return ImmutableIndexStartResult.builder()
                .timeStampOS("")
                .timeStampES("")
                .build();
    }

}
