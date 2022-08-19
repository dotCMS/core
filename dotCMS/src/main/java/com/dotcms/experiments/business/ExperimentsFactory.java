package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import java.util.Optional;

public interface ExperimentsFactory {
    Experiment save(final Experiment experiment) throws DotDataException;

    void delete(final Experiment experiment) throws DotDataException;

    Optional<Experiment> find(String id) throws DotDataException;
}
