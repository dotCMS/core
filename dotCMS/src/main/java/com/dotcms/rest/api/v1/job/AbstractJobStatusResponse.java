package com.dotcms.rest.api.v1.job;

import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

@ValueType
@Immutable
@JsonDeserialize(as = JobStatusResponse.class)
public interface AbstractJobStatusResponse {
    String jobId();
    String statusUrl();
}
