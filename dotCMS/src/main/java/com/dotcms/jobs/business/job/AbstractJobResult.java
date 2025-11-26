package com.dotcms.jobs.business.job;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * Abstract interface for an immutable JobResult class. This interface defines the structure for job
 * result information in the job processing system. The concrete implementation will be generated as
 * an immutable class named JobResult.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = JobResult.class)
@JsonDeserialize(as = JobResult.class)
public interface AbstractJobResult {

    Optional<com.dotcms.jobs.business.error.ErrorDetail> errorDetail();

    Optional<Map<String, Object>> metadata();

}
