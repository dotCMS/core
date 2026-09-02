package com.dotcms.content.index.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableIndexStartResult.class)
@JsonDeserialize(as = ImmutableIndexStartResult.class)
public interface IndexStartResult {

    String indexSuffixOS();

    String indexSuffixES();

    static IndexStartResult empty() {
        return ImmutableIndexStartResult.builder()
                .indexSuffixOS("")
                .indexSuffixES("")
                .build();
    }

}
