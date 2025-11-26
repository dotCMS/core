package com.dotcms.jobs.business.queue;

import com.dotcms.jobs.business.job.JobState;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * Interface representing the parameters for querying job states.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = JobStateQueryParameters.class)
@JsonDeserialize(as = JobStateQueryParameters.class)
public interface AbstractJobStateQueryParameters {

    /**
     * Gets the name of the queue.
     *
     * @return an Optional containing the queue name, or an empty Optional if not specified.
     */
    Optional<String> queueName();

    /**
     * Gets the start date for the query.
     *
     * @return an Optional containing the start date, or an empty Optional if not specified.
     */
    Optional<LocalDateTime> startDate();

    /**
     * Gets the end date for the query.
     *
     * @return an Optional containing the end date, or an empty Optional if not specified.
     */
    Optional<LocalDateTime> endDate();

    /**
     * Gets the page number for pagination.
     *
     * @return the page number.
     */
    int page();

    /**
     * Gets the page size for pagination.
     *
     * @return the page size.
     */
    int pageSize();

    /**
     * Gets the column name to filter dates.
     *
     * @return an Optional containing the filter date column name, or an empty Optional if not
     * specified.
     */
    Optional<String> filterDateColumn();

    /**
     * Gets the column name to order the results by.
     *
     * @return the order by column name.
     */
    String orderByColumn();

    /**
     * Gets the states to filter the jobs by.
     *
     * @return an array of JobState values.
     */
    JobState[] states();

}
