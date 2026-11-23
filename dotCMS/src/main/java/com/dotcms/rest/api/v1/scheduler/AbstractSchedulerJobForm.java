package com.dotcms.rest.api.v1.scheduler;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * A job in the Scheduler portlet's {@code User Job} group, as {@link SchedulerJobResource} accepts
 * it when the job is saved and returns it once saved.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = SchedulerJobForm.class)
@JsonDeserialize(as = SchedulerJobForm.class)
@Schema(description = "A scheduled job in the Scheduler portlet's User Job group")
public interface AbstractSchedulerJobForm {

    /** The job's name, unique within the User Job group. Taken from the path when a job is updated. */
    @Nullable
    @Schema(description = "The job's name, unique within the User Job group. When a job is "
            + "updated, the name in the path is used.", example = "Nightly import")
    String jobName();

    /** A free-text description of the job. */
    @Nullable
    @Schema(description = "A free-text description of the job", example = "Imports the product feed")
    String jobDescription();

    /** The fully qualified name of the Quartz job class to run. */
    @Nullable
    @Schema(description = "The fully qualified name of the job class to run. Users who are not CMS "
            + "Administrators can only choose a class from the list the Scheduler form offers.",
            example = "com.dotmarketing.quartz.job.ContentImportThread")
    String javaClass();

    /** The Quartz cron expression that says when the job runs. */
    @Nullable
    @Schema(description = "The cron expression that says when the job runs", example = "0 0 1 * * ?")
    String cronExpression();

    /** The first date and time the job may run, as {@code yyyy-MM-dd HH:mm}; none means now. */
    @Nullable
    @Schema(description = "The first date and time the job may run, as yyyy-MM-dd HH:mm. "
            + "Leave it out to start now.", example = "2026-12-01 01:00")
    String startDate();

    /** The last date and time the job may run, as {@code yyyy-MM-dd HH:mm}; needs a start date. */
    @Nullable
    @Schema(description = "The last date and time the job may run, as yyyy-MM-dd HH:mm. "
            + "It needs a start date and must be later than it and than now.",
            example = "2027-12-01 01:00")
    String endDate();

    /** The name and value parameters handed to the job when it runs. */
    @Value.Default
    @Schema(description = "The name and value parameters handed to the job when it runs",
            example = "{\"structure\": \"Product\", \"path\": \"/imports/products.csv\"}")
    default Map<String, String> properties() {
        return Map.of();
    }
}
