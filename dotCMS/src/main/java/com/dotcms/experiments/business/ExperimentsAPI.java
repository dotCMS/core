package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import java.util.Optional;

public interface ExperimentsAPI {

    Experiment save(final Experiment experiment, final User user) throws DotSecurityException, DotDataException;

    Optional<Experiment> find(final String id, final User user) throws DotDataException, DotSecurityException;

    Experiment archive(final Experiment experiment, final User user);

    void delete(final Experiment experiment, final User user);

}
