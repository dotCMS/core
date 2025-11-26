package com.dotcms.jobs.business.job;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = JobPaginatedResult.class)
@JsonDeserialize(as = JobPaginatedResult.class)
public interface AbstractJobPaginatedResult extends JobPaginatedResultContract<Job> {

    List<Job> jobs();

}
