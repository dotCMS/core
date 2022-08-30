package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import java.util.List;
import java.util.Optional;

/**
 * Interaction with the persistence layer for CRUD and other operations with {@link Experiment}s
 */

public interface ExperimentsFactory {
    Experiment save(final Experiment experiment) throws DotDataException;

    void delete(final Experiment experiment) throws DotDataException;

    Optional<Experiment> find(String id) throws DotDataException;

    List<Experiment> list(final ExperimentFilter filter) throws DotDataException;
}
