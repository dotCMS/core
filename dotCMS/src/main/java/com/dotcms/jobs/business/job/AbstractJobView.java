package com.dotcms.jobs.business.job;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = JobView.class)
@JsonDeserialize(as = JobView.class)
public interface AbstractJobView extends JobContract {

    @Override
    @JsonSerialize(using = OptionalJobResultSerializer.class)
    Optional<JobResult> result();

}
