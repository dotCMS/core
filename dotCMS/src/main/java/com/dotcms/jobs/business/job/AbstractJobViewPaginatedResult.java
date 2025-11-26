package com.dotcms.jobs.business.job;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = JobViewPaginatedResult.class)
@JsonDeserialize(as = JobViewPaginatedResult.class)
public interface AbstractJobViewPaginatedResult extends JobPaginatedResultContract<JobView> {

    List<JobView> jobs();

}
