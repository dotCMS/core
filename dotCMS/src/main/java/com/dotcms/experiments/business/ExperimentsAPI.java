package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import java.util.Optional;

/**
 * Interface to interact with {@link Experiment}s. This includes operations like CRUD, and
 * starting, stopping an experiment.
 * This API needs License.
 */

public interface ExperimentsAPI {

    /**
     * Save a new experiment when the Experiment doesn't have an id
     * Updates an existing experiment when the provided Experiment has an id and a matching Experiment
     * was found.
     */
    Experiment save(final Experiment experiment, final User user) throws DotSecurityException, DotDataException;

    /**
     * Returns an Optional with the Experiment matching the provided id
     * Returns Optional.empty() if not found
     */
    Optional<Experiment> find(final String id, final User user) throws DotDataException, DotSecurityException;

    /**
     * Updates the Experiment matching the provided id and sets it as 'archived'
     */
    Experiment archive(final String id, final User user)
            throws DotDataException, DotSecurityException;

    /**
     * Deletes the Experiment matching the provided id
     */
    void delete(String id, User user) throws DotDataException, DotSecurityException;

}
