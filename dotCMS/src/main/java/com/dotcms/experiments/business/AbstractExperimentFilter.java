package com.dotcms.experiments.business;

import com.dotcms.experiments.model.AbstractExperiment.Status;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import org.immutables.value.Value;

/**
 * Comprises the different parameters available to filter {@link com.dotcms.experiments.model.Experiment}s
 * when retrieving then.
 * <p>
 * Available filters are :
 * <li>pageId - Id of the parent page of the {@link com.dotcms.experiments.model.Experiment}
 * <li>name - Name of the experiment. Provided name can be partial
 * <li>statuses - List of {@link Status} to filter one. Zero to many.
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
public interface AbstractExperimentFilter extends Serializable {
    Optional<String> pageId();
    Optional<String> name();
    Optional<Set<Status>> statuses();
}
