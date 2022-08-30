package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Optional;

/**
 * Interface to interact with {@link Experiment}s. This includes operations like CRUD, and
 * starting, stopping an experiment.
 * This API needs License.
 */

public interface ExperimentsAPI {

    Experiment save(final Experiment experiment, final User user) throws DotSecurityException, DotDataException;

    Optional<Experiment> find(final String id, final User user) throws DotDataException, DotSecurityException;

    Experiment archive(final String id, final User user)
            throws DotDataException, DotSecurityException;

    void delete(String id, User user) throws DotDataException, DotSecurityException;

    List<Experiment> list(final ExperimentFilter filter, final User user) throws DotDataException;
}
