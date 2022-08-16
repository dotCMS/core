package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.liferay.portal.model.User;

public interface ExperimentsAPI {

    Experiment save(final Experiment experiment, final User user);

    Experiment archive(final Experiment experiment, final User user);

    void delete(final Experiment experiment, final User user);
}
