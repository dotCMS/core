package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;

public interface ExperimentsFactory {
    Experiment save(final Experiment experiment);

    Experiment archive(final Experiment experiment);

    void delete(final Experiment experiment);
}
