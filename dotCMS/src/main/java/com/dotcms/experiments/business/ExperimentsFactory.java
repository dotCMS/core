package com.dotcms.experiments.business;

import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Interaction with the persistence layer for CRUD and other operations with {@link Experiment}s
 */

public interface ExperimentsFactory {
    /**
     * Saves the provided Experiment in the persistence layer
     */
    void save(final Experiment experiment) throws DotDataException;

    /**
     * Deletes the provided Experiment from the persistence layer
     */
    void delete(final Experiment experiment) throws DotDataException;

    /**
     * Returns, if found, an Optional with the Experiment with the requested id.
     * Returns Optional.empty() if not found
     */
    Optional<Experiment> find(String id) throws DotDataException;

    /**
     * Returns a list of experiments after applying the provided filters in
     * {@link ExperimentFilter}
     */
    List<Experiment> list(final ExperimentFilter filter) throws DotDataException;

    /**
     * Return the collection of experiments that are active on this Page. This includes all the experiments
     * currently active on this Page.
     */

    /**
     * Return the collection of experiments that are RUNNING, DRAFT or SCHEDULED on the Page
     * @param pageIdentifier to Filter the Experiments.
     *
     * @return
     * @throws DotDataException
     */
    Collection<Experiment> listActive(final String pageIdentifier) throws DotDataException;

    /**
     * Returns the number of experiments whose {@code status} is in {@code statuses}.
     * Issues a SQL {@code COUNT} query — no experiment rows are loaded into memory.
     *
     * @param statuses the set of statuses to count
     * @return the count of matching experiments
     * @throws DotDataException on persistence errors
     */
    int countByStatuses(Set<Status> statuses) throws DotDataException;
}
