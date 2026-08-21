package com.dotcms.rest.api.v1.content.bulkrefresh;

import com.dotcms.jobs.business.job.JobState;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * What {@code GET /api/v1/content/_bulkrefresh/{jobId}} reports.
 *
 * <p>Deliberately narrower than {@link com.dotcms.jobs.business.job.JobView}. That view serializes
 * {@code parameters()}, which for this queue is the entire submitted inode list plus the submitter's
 * id — up to 500 UUIDs. A client polls this endpoint every 1.5 seconds for as long as the run lasts,
 * so returning the whole selection on every response sends the caller's own input back to it a couple
 * of hundred times, and hands any reader of one response the full selection and who submitted it.
 *
 * <p>{@link #result()} carries the processor's metadata map as-is, so the counters sit directly on it
 * rather than under a nested key — the same flattening
 * {@code OptionalJobResultSerializer} produces for {@code JobView}, kept identical so clients do not
 * have to care which of the two they are reading. The job's {@code errorDetail} is not surfaced;
 * {@link #state()} is what tells a caller the run failed.
 *
 * @author dotCMS
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = BulkRefreshStatusView.class)
@JsonDeserialize(as = BulkRefreshStatusView.class)
public interface AbstractBulkRefreshStatusView {

    /** The job's id, echoed so a response is self-describing. */
    String id();

    /** Where the run has got to. Terminal states are what tell a client to stop polling. */
    JobState state();

    /** 0.0–1.0. The only progress signal available while the run is in flight. */
    float progress();

    /** Counters, and the per-item records when the job was submitted asking for them. */
    @JsonInclude(Include.NON_ABSENT)
    Optional<Map<String, Object>> result();
}
