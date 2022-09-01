package com.dotcms.experiments.business;

import com.dotcms.experiments.model.AbstractExperiment.Status;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * Comprises the different parameters available to filter {@link com.dotcms.experiments.model.Experiment}s
 * when retrieving then.
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
public interface AbstractExperimentFilter extends Serializable {
    Optional<String> pageId();
    Optional<String> name();
    Optional<List<Status>> statuses();
}
