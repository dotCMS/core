package com.dotcms.jobs.business.api.events;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.immutables.value.Value;

/**
 * Class to hold a watcher and its filter predicate.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = JobWatcher.class)
@JsonDeserialize(as = JobWatcher.class)
public interface AbstractJobWatcher {

    /**
     * Returns a Consumer that performs an operation on a Job instance.
     *
     * @return a Consumer of Job that defines what to do with a Job instance.
     */
    Consumer<com.dotcms.jobs.business.job.Job> watcher();

    /**
     * Returns a predicate that can be used to filter jobs based on custom criteria.
     *
     * @return a Predicate object to filter Job instances
     */
    Predicate<com.dotcms.jobs.business.job.Job> filter();

}
