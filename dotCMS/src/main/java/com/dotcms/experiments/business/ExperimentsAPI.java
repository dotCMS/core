package com.dotcms.experiments.business;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.experiments.business.result.ExperimentResults;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.Rule;
import com.liferay.portal.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Interface to interact with {@link Experiment}s. This includes operations like CRUD, and
 * starting, stopping an experiment.
 * This API needs License.
 */

public interface ExperimentsAPI {

    String PRIMARY_GOAL = "primary";
    String EXPERIMENTS_MAX_DURATION_KEY = "EXPERIMENTS_MAX_DURATION";
    String EXPERIMENTS_DEFAULT_DURATION_KEY = "EXPERIMENTS_DEFAULT_DURATION";
    String EXPERIMENTS_MIN_DURATION_KEY = "EXPERIMENTS_MIN_DURATION";
    String EXPERIMENTS_LOOKBACK_WINDOW_KEY = "EXPERIMENTS_LOOKBACK_WINDOW";

    enum Health {
        OK, NOT_CONFIGURED, CONFIGURATION_ERROR
    }

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
     * Deletes the Experiment matching the provided id, validate that the Experiment
     * is in DRAFT or SCHEDULED state to be deleted
     */
    void delete(String id, User user) throws DotDataException, DotSecurityException;

    /**
     * Deletes the Experiment matching the provided id, and just Validate Permission
     */
    void forceDelete(final String id, final User user) throws DotDataException, DotSecurityException;

    /**
     * Returns experiments based on the provided filters in {@link ExperimentFilter}
     * <p>
     * Available filters are :
     * <li>pageId - Id of the parent page of the {@link com.dotcms.experiments.model.Experiment}
     * <li>name - Name of the experiment. Provided name can be partial
     * <li>statuses - List of {@link Status} to filter one. Zero to many.
     */
    List<Experiment> list(final ExperimentFilter filter, final User user) throws DotDataException;

    /**
     * Starts an {@link Experiment}. In order to start an Experiment it needs to:
     * <li>Have a {@link Status#DRAFT} status
     * <li>Have at least one Variant
     * <li>Have a primary goal set
     * <p>
     * The following considerations regarding {@link Experiment#scheduling()} are also taking place
     * when starting an Experiment:
     * <li>If no {@link Scheduling#startDate()} is provided, set it to now()
     * <li>If no {@link Scheduling#endDate()} is provided, set it to four weeks
     * <li>Unable to start if provided {@link Scheduling#startDate()} is in the past
     * <li>Unable to start if provided {@link Scheduling#endDate()} is in the past
     * <li>Unable to start if provided {@link Scheduling#endDate()} is not after provided {@link Scheduling#startDate()}
     * <li>Unable to start if difference between {@link Scheduling#endDate()} and {@link Scheduling#startDate()} is more than {@link ExperimentsAPI#EXPERIMENTS_MAX_DURATION_KEY}
     *
     * @return
     */
    Experiment start(final String experimentId, final User user)
            throws DotDataException, DotSecurityException;

    /**
     * Similar to #start, but it forces the start of the Experiment even if there is an Experiment
     * already running for the same page, which would then be stopped.
     */
    Experiment forceStart(final String experimentId, final User user, Scheduling scheduling)
            throws DotDataException, DotSecurityException;

    /**
     * Similar to #start when it is used with an Experiment with not null Scheduling,
     * but it forces the start of the Experiment even if there is an Experiment
     * already running for the same page, which would then be stopped.
     */
    Experiment forceScheduled(final String experimentId, final User user, final Scheduling scheduling)
            throws DotDataException, DotSecurityException;

    /**
     * Starts the SCHEDULED Experiment with the given id
     * @param experimentId the id
     * @param user the user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Experiment startScheduled(String experimentId, User user)
            throws DotDataException, DotSecurityException;

    /**
     * Ends an already started {@link Experiment}. The Experiment needs to be in either
     * {@link Status#RUNNING} or {@link  Status#SCHEDULED} status to be able to end it.
     */
    Experiment end(String experimentId, User user) throws DotDataException, DotSecurityException;

    /**
     * Adds a new {@link com.dotcms.variant.model.Variant} to the {@link Experiment} with the provided Id
     * @return the updated Experiment
     */
    Experiment addVariant(String experimentId, String variantName, User user)
            throws DotDataException, DotSecurityException;

    /**
     * Only starts Experiments in the SCHEDULED status whose startDate is in the past 
     * @param user
     * @throws DotDataException
     */
    void startScheduledToStartExperiments(User user) throws DotDataException;

    /**
     * Validates a {@link Scheduling} by the following:
     *
     * <li>Provided {@link Scheduling#startDate()} needs to be now or in the future
     * <li>Provided {@link Scheduling#endDate()} needs to be in the future
     * <li>Provided {@link Scheduling#endDate()} needs to be after provided {@link Scheduling#startDate()}
     * <li>Difference between provided {@link Scheduling#endDate()} and {@link Scheduling#startDate()} needs to be less or equal than {@link ExperimentsAPI#EXPERIMENTS_MAX_DURATION_KEY}
     */
    Scheduling validateScheduling(final Scheduling scheduling);

    /**
     * Deletes a {@link com.dotcms.variant.model.Variant} from the {@link Experiment} with the given Id
     * @return the updated Experiment
     */
    Experiment deleteVariant(String experimentId, String variantName, User user)
            throws DotDataException, DotSecurityException;

    /**
     * Edits the description of the {@link com.dotcms.variant.model.Variant} with the given name
     * from the {@link Experiment} with the given Id
     * @return the updated Experiment
     */
    @WrapInTransaction
    Experiment editVariantDescription(String experimentId, String variantName,
            String newDescription, User user)
            throws DotDataException, DotSecurityException;

    /**
     * Deletes the {@link com.dotcms.experiments.model.TargetingCondition} with the given id from
     * the {@link Experiment} with the given id
     */

    Experiment deleteTargetingCondition(String experimentId, String conditionId, User user)
            throws DotDataException, DotSecurityException;

    /**
     * Return a list of the current RUNNING Experiments.
     *
     * @return
     */
    List<Experiment> getRunningExperiments() throws DotDataException;

    /**
     * Return a list of the current RUNNING Experiments on the specific {@link Host}.
     *
     * @return
     */
    List<Experiment> getRunningExperiments(final Host host) throws DotDataException;

    /**
     * Return a {@link Experiment}'s {@link Rule}
     *
     * @param experiment
     * @return
     */
    Optional<Rule> getRule(final Experiment experiment)
            throws DotDataException, DotSecurityException;

    /**
     * Return true if Any {@link Experiment} is running right now and Experiment are enabled.
     * Otherwise return false.
     *
     * @return
     *
     * @see ConfigExperimentUtil#isExperimentEnabled()
     *
     */
    boolean isAnyExperimentRunning(final Host host) throws DotDataException;


    /**
     * Return the Experiment partial or total result.
     *
     * @param experiment
     * @param user
     * @return
     */
    ExperimentResults getResults(Experiment experiment, User user)
            throws DotDataException, DotSecurityException;

    /*
     * Ends finalized {@link com.dotcms.experiments.model.Experiment}s
     * <p>
     *     A finalized Experiment is an Experiment that is in the {@link com.dotcms.experiments.model.Experiment.Status#RUNNING}
     *     state and whose {@link  Scheduling#endDate()} is in the past
     */
    void endFinalizedExperiments(final User user) throws DotDataException;

    /**
     * Promotes a Variant to the default one for the given Experiment
     */
    Experiment promoteVariant(String experimentId, String variantName, User user)
            throws DotDataException, DotSecurityException;

    /*
     * Cancels a Scheduled or RUNNING {@link com.dotcms.experiments.model.Experiment}.
     * By Canceling an Experiment:
     *
     * - If the current Status is Scheduled then it comes back to DRAFT and its future execution will not take place
     * - If it is in RUNNING then it just comes back to DRAFT.
     *
     * In order to be canceled, the Experiment needs to be in the
     * {@link com.dotcms.experiments.model.Experiment.Status#SCHEDULED} state.
     */
    Experiment cancel(String experimentId, User user) throws DotDataException, DotSecurityException;

    /**
     * Return the RUNNING Experiments for the given page if it has any,
     * Otherwise return an empty Optional
     *
     * @param pageId
     * @return
     * @throws DotDataException
     */
    Optional<Experiment> getRunningExperimentPerPage(final String pageId) throws DotDataException;

    /**
     * Return the collection of experiments that are active on this Page. This includes all the experiments
     * currently active on the Page. It means all experiments with the status DRAFT, SCHEDULED or
     * RUNNING Experiment on this Page
     *
     * @param pageIdentifier to Filter the Experiments.
     *
     * @return
     * @throws DotDataException
     */
    Collection<Experiment> listActive(final String pageIdentifier) throws DotDataException;

    int getExperimentsLookbackWindow();

}
