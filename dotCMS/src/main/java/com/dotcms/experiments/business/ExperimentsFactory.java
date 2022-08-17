package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import java.util.Optional;

public interface ExperimentsFactory {
    Experiment save(final Experiment experiment) throws DotDataException;

    Experiment archive(final Experiment experiment);

    void delete(final Experiment experiment);

    Optional<Experiment> find(String id) throws DotDataException;
}
