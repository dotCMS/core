package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

public interface ExperimentsAPI {

    Experiment save(final Experiment experiment, final User user) throws DotSecurityException, DotDataException;

    Experiment archive(final Experiment experiment, final User user);

    void delete(final Experiment experiment, final User user);
}
