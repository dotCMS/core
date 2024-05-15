package com.dotcms.experiments.business;

import static com.dotcms.experiments.business.ExperimentsCache.CACHED_EXPERIMENTS_KEY;
import static com.dotcms.experiments.model.AbstractExperiment.Status.ARCHIVED;
import static com.dotcms.experiments.model.AbstractExperiment.Status.DRAFT;
import static com.dotcms.experiments.model.AbstractExperiment.Status.ENDED;
import static com.dotcms.experiments.model.AbstractExperiment.Status.RUNNING;
import static com.dotcms.experiments.model.AbstractExperiment.Status.SCHEDULED;
import static com.dotcms.experiments.model.AbstractExperimentVariant.EXPERIMENT_VARIANT_NAME_PREFIX;
import static com.dotcms.experiments.model.AbstractExperimentVariant.EXPERIMENT_VARIANT_NAME_SUFFIX;

import static com.dotcms.util.CollectionsUtils.set;

import static com.dotcms.experiments.model.AbstractExperimentVariant.ORIGINAL_VARIANT;
import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;
import static com.dotmarketing.util.DateUtil.isTimeReach;

import com.dotcms.analytics.bayesian.BayesianAPI;
import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.BayesianResult;
import com.dotcms.analytics.helper.AnalyticsHelper;

import com.dotcms.analytics.helper.BayesianHelper;
import com.dotcms.analytics.metrics.EventType;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.analytics.metrics.MetricsUtil;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.business.event.ContentletDeletedEvent;
import com.dotcms.cube.CubeJSClient;
import com.dotcms.cube.CubeJSClientFactory;
import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.CubeJSResultSet;
import com.dotcms.cube.CubeJSResultSet.ResultSetItem;
import com.dotcms.cube.filters.SimpleFilter;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.experiments.business.result.*;
import com.dotcms.exception.NotAllowedException;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Goal;
import com.dotcms.experiments.model.AbstractTrafficProportion.Type;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Experiment.Builder;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.GoalFactory;
import com.dotcms.experiments.model.Goals;
import com.dotcms.experiments.model.RunningIds;
import com.dotcms.experiments.model.RunningIds.RunningId;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.experiments.model.TargetingCondition;
import com.dotcms.experiments.model.TrafficProportion;

import com.dotcms.metrics.timing.TimeMetric;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.LogicalOperator;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.Rule.FireOn;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import graphql.VisibleForTesting;
import io.vavr.control.Try;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class ExperimentsAPIImpl implements ExperimentsAPI {

    private static final int VARIANTS_NUMBER_MAX = 3;
    private static final List<Status> RESULTS_QUERY_VALID_STATUSES = List.of(RUNNING, ENDED);

    final ExperimentsFactory factory = FactoryLocator.getExperimentsFactory();
    final ExperimentsCache experimentsCache = CacheLocator.getExperimentsCache();
    final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    final VariantAPI variantAPI = APILocator.getVariantAPI();
    final ShortyIdAPI shortyIdAPI = APILocator.getShortyAPI();
    final RulesAPI rulesAPI = APILocator.getRulesAPI();
    final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
    final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
    final HTMLPageAssetAPI pageAssetAPI = APILocator.getHTMLPageAssetAPI();
    final BayesianAPI bayesianAPI = APILocator.getBayesianAPI();
    final CubeJSClientFactory cubeJSClientFactory = FactoryLocator.getCubeJSClientFactory();

    private final LicenseValiditySupplier licenseValiditySupplierSupplier =
            new LicenseValiditySupplier() {};

    private final Supplier<String> invalidLicenseMessageSupplier =
            ()->"Valid License is required";

    private final AnalyticsHelper analyticsHelper;

    @VisibleForTesting
    public ExperimentsAPIImpl(final AnalyticsHelper analyticsHelper) {
        this.analyticsHelper = analyticsHelper;

        APILocator.getLocalSystemEventsAPI().subscribe(ContentletDeletedEvent.class,
                (EventSubscriber<ContentletDeletedEvent>) event ->
                        checkAndDeleteExperiment(event.getContentlet(), event.getUser()));
    }

    public ExperimentsAPIImpl() {
        this(AnalyticsHelper.get());
    }

    @Override
    @WrapInTransaction
    public Experiment save(final Experiment experiment, final User user) throws
            DotSecurityException, DotDataException {
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);

        final HTMLPageAsset htmlPageAsset = getHtmlPageAsset(experiment);

        DotPreconditions.isTrue(htmlPageAsset != null
                && UtilMethods.isSet(htmlPageAsset.getIdentifier()),
                DotStateException.class, ()->"htmlPageAsset Page provided");

        validatePermissionToEdit(experiment, user, htmlPageAsset);

        Experiment.Builder builder = Experiment.builder().from(experiment);

        if(experiment.id().isEmpty()) {
            builder.id(UUIDGenerator.generateUuid());
        }

        builder.modDate(Instant.now());
        builder.lastModifiedBy(user.getUserId());
        
        if(experiment.goals().isPresent()) {
            final Goals goals = experiment.goals().orElseThrow();
            MetricsUtil.INSTANCE.validateGoals(goals);
        }

        if(experiment.targetingConditions().isPresent()) {
            saveTargetingConditions(experiment, user);
        }

        Experiment experimentToSave = builder.build();

        Optional<Experiment> existingExperiment = find(experimentToSave.id().get(), user);

        if(experimentToSave.status() == DRAFT && experimentToSave.scheduling().isPresent()
                && (existingExperiment.isEmpty() || isExistingSchedulingChanging(experimentToSave,
                existingExperiment))) {
            experimentToSave = experimentToSave.withScheduling(
                    Optional.of(validateScheduling(experimentToSave.scheduling().get())));
        }

        factory.save(experimentToSave);

        Logger.info(this, "Saving experiment with id: " + experimentToSave.id().get() + ", and status:"
        + experimentToSave.status());

        DotPreconditions.isTrue(experimentToSave.id().isPresent(), "Experiment doesn't have Id");

        Optional<Experiment> savedExperiment = find(experimentToSave.id().get(), user);

        DotPreconditions.isTrue(savedExperiment.isPresent(), "Saved Experiment not found");

        if(savedExperiment.get().trafficProportion().variants().stream().noneMatch((variant
                -> variant.description().equals(ORIGINAL_VARIANT)))) {
            savedExperiment = Optional.of(addVariant(savedExperiment.get().id().get(),
                    ORIGINAL_VARIANT, user));
        }

        final Experiment toReturn = savedExperiment.get();

        SecurityLogger.logInfo(this.getClass(), () -> String.format("Experiment '%s' [%s] has been saved by User" +
                " ID '%s'", toReturn.name(), toReturn.id(), user.getUserId()));

        return toReturn;
    }

    private void validatePermissionToEdit(Experiment experiment, User user, Contentlet pageAsContent)
            throws DotDataException, DotSecurityException {

        try {
            validateExperimentPagePermissions(user, experiment, PermissionLevel.EDIT,
                    "You don't have permission to Edit the Page");

            validateEditTemplateLayoutPermissions(user, experiment);
        } catch (DotSecurityException e) {
            Logger.error(this, "You don't have permission to save the Experiment."
                    + " Experiment name: " + experiment.name() + ". Page Id: " + experiment.pageId() +
                    "\n" + e.getMessage());
            throw new DotSecurityException("You don't have permission to save the Experiment.");
        }
    }

    private static boolean isExistingSchedulingChanging(Experiment experimentToSave,
            Optional<Experiment> existingExperiment) {
        return !existingExperiment.get().scheduling().
                equals(experimentToSave.scheduling());
    }

    private void saveTargetingConditions(final Experiment experiment, final User user)
            throws DotDataException, DotSecurityException {
        if(experiment.targetingConditions().isEmpty()) {
            return;
        }

        List<Rule> rules = Try.of(()->rulesAPI
                .getAllRulesByParent(experiment, user, false))
                .getOrElse(Collections::emptyList);

        Rule experimentRule;

        if(UtilMethods.isSet(rules)) {
            experimentRule = rules.get(0);
        } else {
            experimentRule = createRuleAndConditionGroup(experiment, user);
        }

        // transform and save TargetingConditions into conditions
        experiment.targetingConditions().get().forEach(targetingCondition -> {
            createAndSaveCondition(user, experimentRule, targetingCondition);
        });

    }

    private void createAndSaveCondition(User user, Rule experimentRule,
            TargetingCondition targetingCondition) {
        Condition condition = targetingCondition.id().isPresent()
            ? Try.of(()->rulesAPI.getConditionById(targetingCondition.id().get(), user, false))
                .getOrElseThrow(()->new IllegalArgumentException("Invalid targeting Condition Id provided. Id: " + targetingCondition.id().get()))
            : createCondition(experimentRule);

        condition.setOperator(targetingCondition.operator());
        condition.setConditionletId(targetingCondition.conditionKey());
        condition.setValues(new ArrayList<>());
        targetingCondition.values().forEach(condition::addValue);

        condition.checkValid();

        Try.run(()->rulesAPI.saveCondition(condition, user, false))
                .getOrElseThrow(()->new DotStateException("Error saving Condition: "
                        + condition.getConditionletId()));
    }

    private Condition createCondition(final Rule experimentRule) {
        final Condition condition = new Condition();
        condition.setConditionGroup(experimentRule.getGroups().get(0).getId());
        return condition;
    }

    private Rule createRuleAndConditionGroup(Experiment experiment, User user)
            throws DotDataException, DotSecurityException {
        DotPreconditions.isTrue(experiment.id().isPresent(), ()->"Error saving Experiment Targeting");

        final Rule experimentRule = new Rule();
        experimentRule.setParent(experiment.id().get());
        experimentRule.setName(experiment.name());
        experimentRule.setFireOn(FireOn.EVERY_PAGE);
        experimentRule.setEnabled(true);
        rulesAPI.saveRuleNoParentCheck(experimentRule, user, false);

        final ConditionGroup conditionGroup = new ConditionGroup();
        conditionGroup.setRuleId(experimentRule.getId());
        conditionGroup.setOperator(LogicalOperator.AND);
        rulesAPI.saveConditionGroup(conditionGroup, user, false);
        return experimentRule;
    }

    @CloseDBIfOpened
    @Override
    public Optional<Experiment> find(final String id, final User user)
            throws DotDataException, DotSecurityException {
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);
        DotPreconditions.checkArgument(UtilMethods.isSet(id), "Experiment Id is required");

        Optional<Experiment> experiment =  factory.find(id);

        if(experiment.isPresent()) {
            validatePageEditPermissions(user, experiment.get(),
                    "You don't have permission to get the Experiment. "
                            + "Experiment Id: " + experiment.get().id().get());

            experiment = Optional.of(addTargetingConditions(experiment.get(), user));
        }

        return experiment;
    }

    private Experiment addTargetingConditions(final Experiment experiment, final User user) {
        List<Rule> rules = Try.of(()->rulesAPI
                        .getAllRulesByParent(experiment, user, false))
                .getOrElse(Collections::emptyList);

        if(!UtilMethods.isSet(rules)) {
            return experiment;
        }

        final Rule experimentRule = rules.get(0);
        final List<TargetingCondition> targetingConditions = new ArrayList<>();
        experimentRule.getGroups().get(0).getConditions().forEach((condition -> {
            targetingConditions.add(TargetingCondition.builder()
                    .id(condition.getId())
                    .conditionKey(condition.getConditionletId())
                    .putAllValues(condition.getValues().stream().collect(Collectors.toMap(
                            ParameterModel::getKey, ParameterModel::getValue)))
                    .build());
        }));

        return experiment.withTargetingConditions(targetingConditions);
    }

    @Override
    @WrapInTransaction
    public Experiment archive(final String id, final User user)
            throws DotDataException, DotSecurityException {
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);
        DotPreconditions.checkArgument(UtilMethods.isSet(id), "id must be provided.");

        final Optional<Experiment> persistedExperiment =  find(id, user);

        DotPreconditions.isTrue(persistedExperiment.isPresent(),()-> "Experiment with provided id not found",
                DoesNotExistException.class);

        validatePageEditPermissions(user, persistedExperiment.get(),
                "You don't have permission to archive the Experiment. "
                        + "Experiment Id: " + persistedExperiment.get().id());

        if(persistedExperiment.get().status()==ARCHIVED) {
            return persistedExperiment.get();
        }

        DotPreconditions.isTrue(persistedExperiment.get().status()==ENDED,
                ()-> "Only ENDED experiments can be archived",
                DotStateException.class);

        final Experiment archived = persistedExperiment.get().withStatus(ARCHIVED);
        final Experiment afterSave = save(archived, user);

        SecurityLogger.logInfo(this.getClass(), () -> String.format("Experiment '%s' [%s] has been archived by User" +
                " ID '%s'", afterSave.name(), afterSave.id(), user.getUserId()));

        return afterSave;
    }

    @Override
    @WrapInTransaction
    public void delete(final String id, final User user)
            throws DotDataException, DotSecurityException {
        innerDelete(id, user, (experiment)-> {
            if(experiment.status() != DRAFT &&
                    experiment.status() != Status.SCHEDULED) {
                throw new DotStateException("Only DRAFT or SCHEDULED experiments can be deleted");
            }
        });

    }

    @Override
    @WrapInTransaction
    public void forceDelete(final String id, final User user)
            throws DotDataException, DotSecurityException {
        innerDelete(id, user, null);
    }

    @WrapInTransaction
    private void innerDelete(final String id, final User user, final Consumer<Experiment> extraValidation)
            throws DotDataException, DotSecurityException {
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);
        DotPreconditions.checkArgument(UtilMethods.isSet(id), "id must be provided.");

        final Optional<Experiment> persistedExperimentOptional =  find(id, user);

        DotPreconditions.isTrue(persistedExperimentOptional.isPresent(),()-> "Experiment with provided id not found",
                DoesNotExistException.class);

        final Experiment persistedExperiment = persistedExperimentOptional.get();

        validatePageEditPermissions(user, persistedExperiment,
                "You don't have permission to delete the Experiment. "
                        + "Experiment Id: " + persistedExperiment.id());

        if (extraValidation != null) {
            extraValidation.accept(persistedExperiment);
        }

        persistedExperiment.trafficProportion().variants().stream()
                .filter(variant -> !VariantAPI.DEFAULT_VARIANT.name().equals(variant.id()))
                .forEach(variant -> deleteVariant(variant));

        factory.delete(persistedExperiment);

        SecurityLogger.logInfo(this.getClass(), () -> String.format("Experiment '%s' [%s] has been deleted by User" +
                " ID '%s'", persistedExperiment.name(), persistedExperiment.id(), user.getUserId()));
    }

    private void deleteVariant(ExperimentVariant variant) {
        try {
            variantAPI.archive(variant.id());
            variantAPI.delete(variant.id());
        } catch (DotDataException e) {
            Logger.error(this, "Error deleting variant", e);
        }
    }

    @Override
    @CloseDBIfOpened
    public List<Experiment> list(ExperimentFilter filter, User user) throws DotDataException {
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);
        return factory.list(filter);
    }

    @Override
    @WrapInTransaction
    public Experiment start(String experimentId, User user)
            throws DotDataException, DotSecurityException {

        try {
            DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                    invalidLicenseMessageSupplier);
            DotPreconditions.checkArgument(UtilMethods.isSet(experimentId), "experiment Id must be provided.");

            final Experiment persistedExperiment = find(experimentId, user).orElseThrow(
                    () -> new IllegalArgumentException("Experiment with provided id not found")
            );


            validateExperimentPagePermissions(user, persistedExperiment, PermissionLevel.PUBLISH,
                    String.format("User %s doesn't have PUBLISH permission on the Experiment Page's. Experiment Id: %s",
                            user.getUserId(), persistedExperiment.id().get()));

            validatePublishTemplateLayoutPermissions(user, persistedExperiment);

            DotPreconditions.isTrue(persistedExperiment.status() != Status.RUNNING ||
                            persistedExperiment.status() != Status.SCHEDULED,
                    () -> "Cannot start an already started Experiment.",
                    DotStateException.class);

            DotPreconditions.isTrue(persistedExperiment.status() == DRAFT
                    , () -> "Only DRAFT experiments can be started",
                    DotStateException.class);

            DotPreconditions.checkState(hasAtLeastOneVariant(persistedExperiment),
                    "The Experiment needs at "
                            + "least one Page Variant in order to be started.");

            DotPreconditions.checkState(persistedExperiment.goals().isPresent(),
                    "The Experiment needs to "
                            + "have the Goal set.");

            Optional<Experiment> runningExperimentOnPage = getRunningExperimentsOnPage(
                    user, persistedExperiment);

            if (runningExperimentOnPage.isPresent()) {
                final boolean meantToRunNow = persistedExperiment.scheduling().isEmpty();

                if (meantToRunNow) {
                    throw new DotStateException(
                            "There is a running Experiment on the same page. Name: "
                                    + runningExperimentOnPage.get().name());
                }

                final Experiment runningExperiment = runningExperimentOnPage.get();
                DotPreconditions.isTrue(runningExperiment.scheduling().orElseThrow().endDate()
                                .orElseThrow().isBefore(
                                        persistedExperiment.scheduling().orElseThrow().startDate()
                                                .orElseThrow()),
                        () -> "Scheduling conflict: The same page can't be included in different experiments with overlapping schedules. "
                                + "Overlapping with Experiment: "
                                + runningExperiment.name(),
                        DotStateException.class);
            }

            Experiment toReturn;

            if (emptyScheduling(persistedExperiment)) {
                final Scheduling scheduling = startNowScheduling();
                final Experiment experimentToSave = persistedExperiment.withScheduling(scheduling)
                        .withStatus(RUNNING);
                validateNoConflictsWithScheduledExperiments(experimentToSave, user);
                toReturn = innerStart(experimentToSave, user, true);
            } else {
                Scheduling scheduling = persistedExperiment.scheduling().get();
                final Experiment experimentToSave = persistedExperiment.withScheduling(scheduling)
                        .withStatus(SCHEDULED);
                validateNoConflictsWithScheduledExperiments(experimentToSave, user);
                toReturn = save(experimentToSave.withScheduling(scheduling).withStatus(SCHEDULED).
                                withRunningIds(getRunningIds(experimentToSave)), user);
            }

            return toReturn;
        } catch (DotSecurityException e) {
            final String message = "You don't have permission to start the Experiment Id: " + experimentId;
            Logger.error(this, message + "\n" + e.getMessage());
            throw new DotSecurityException(message, e);
        }
    }

    @Override
    public Experiment forceStart(String experimentId, User user, Scheduling scheduling)
            throws DotDataException, DotSecurityException {
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);
        DotPreconditions.checkArgument(UtilMethods.isSet(experimentId), "experiment Id must be provided.");

        final Experiment persistedExperiment =  find(experimentId, user).orElseThrow(
                ()-> new IllegalArgumentException("Experiment with provided id not found")
        );

        validatePageEditPermissions(user, persistedExperiment,
                "You don't have permission to start the Experiment. "
                        + "Experiment Id: " + persistedExperiment.id());

        DotPreconditions.isTrue(persistedExperiment.status()!=Status.RUNNING ||
                        persistedExperiment.status() != Status.SCHEDULED,()-> "Cannot start an already started Experiment.",
                DotStateException.class);

        DotPreconditions.isTrue(persistedExperiment.status()== DRAFT
                ,()-> "Only DRAFT experiments can be started",
                DotStateException.class);

        DotPreconditions.checkState(hasAtLeastOneVariant(persistedExperiment), "The Experiment needs at "
                + "least one Page Variant in order to be started.");

        DotPreconditions.checkState(persistedExperiment.goals().isPresent(), "The Experiment needs to "
                + "have the Goal set.");

        final Optional<Experiment> runningExperimentOnPage = getRunningExperimentsOnPage(
                user, persistedExperiment);

        final Experiment experimentToSave = persistedExperiment.withStatus(RUNNING);

        if(runningExperimentOnPage.isPresent()) {
            endRunningExperimentIfNeeded(user, runningExperimentOnPage.get(), experimentToSave);
        }
        cancelScheduledExperimentsUponConflicts(experimentToSave, user);

        return innerStart(experimentToSave.withScheduling(scheduling), user, false);
    }

    @Override
    public Experiment forceScheduled(final String experimentId, final User user, final Scheduling scheduling)
            throws DotDataException, DotSecurityException {
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);
        DotPreconditions.checkArgument(UtilMethods.isSet(experimentId), "experiment Id must be provided.");

        final Experiment persistedExperiment =  find(experimentId, user).orElseThrow(
                ()-> new IllegalArgumentException("Experiment with provided id not found")
        );

        validatePageEditPermissions(user, persistedExperiment,
                "You don't have permission to start the Experiment. "
                        + "Experiment Id: " + persistedExperiment.id());

        DotPreconditions.isTrue(persistedExperiment.status()!=Status.RUNNING ||
                        persistedExperiment.status() != Status.SCHEDULED,()-> "Cannot start an already started Experiment.",
                DotStateException.class);

        DotPreconditions.isTrue(persistedExperiment.status()== DRAFT
                ,()-> "Only DRAFT experiments can be started",
                DotStateException.class);

        DotPreconditions.checkState(hasAtLeastOneVariant(persistedExperiment), "The Experiment needs at "
                + "least one Page Variant in order to be started.");

        DotPreconditions.checkState(persistedExperiment.goals().isPresent(), "The Experiment needs to "
                + "have the Goal set.");

        final Optional<Experiment> runningExperimentOnPage = getRunningExperimentsOnPage(
                user, persistedExperiment);

        final Experiment experimentToSave = persistedExperiment.withScheduling(scheduling).withStatus(SCHEDULED);

        if(runningExperimentOnPage.isPresent()) {
            endRunningExperimentIfNeeded(user, runningExperimentOnPage.get(), experimentToSave);
        }
        cancelScheduledExperimentsUponConflicts(experimentToSave, user);
        return save(experimentToSave.withScheduling(scheduling).withStatus(SCHEDULED), user);
    }

    private void endRunningExperimentIfNeeded(User user, Experiment runningExperimentOnPage,
            Experiment persistedExperiment) throws DotDataException, DotSecurityException {
        if(conflictingStartOrEndDate(runningExperimentOnPage.scheduling().orElseThrow(),
                persistedExperiment.scheduling().orElseThrow())) {
            end(runningExperimentOnPage.id().orElseThrow(), user);
        }
    }

    private Optional<Experiment> getRunningExperimentsOnPage(User user, Experiment persistedExperiment)
            throws DotDataException {
        final List<Experiment> runningExperimentsOnPage = list(ExperimentFilter.builder()
                .pageId(persistedExperiment.pageId())
                .statuses(Set.of(RUNNING))
                .build(), user);

        return runningExperimentsOnPage.isEmpty() ? Optional.empty() : Optional.of(runningExperimentsOnPage.get(0));
    }

    private void publishExperimentPage(final Experiment experiment, final User user)
            throws DotDataException, DotSecurityException {

        final HTMLPageAsset htmlPageAsset = APILocator.getHTMLPageAssetAPI().fromContentlet(contentletAPI
                .findContentletByIdentifierAnyLanguage(experiment.pageId(), DEFAULT_VARIANT.name()));

        if(htmlPageAsset.isLive()) {
            return;
        }

        final List relatedNotPublished = PublishFactory.getUnpublishedRelatedAssetsForPage(htmlPageAsset, new ArrayList(),
                true, user, false);
        relatedNotPublished.stream().filter(asset -> asset instanceof Contentlet).forEach(
                asset -> Contentlet.class.cast(asset)
                        .setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE));
        //Publish the page and the related content
        htmlPageAsset.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);
        try {
            PublishFactory.publishHTMLPage(htmlPageAsset, relatedNotPublished, user,
                    false);
        } catch(WebAssetException e) {
            throw new DotDataException(e);
        }
    }

    private static boolean emptyScheduling(Experiment persistedExperiment) {
        return persistedExperiment.scheduling().isEmpty() ||
                (persistedExperiment.scheduling().get().startDate()).isEmpty()
                        && persistedExperiment.scheduling().get().endDate().isEmpty();
    }

    private void validateNoConflictsWithScheduledExperiments(final Experiment experimentToCheck,
            final User user) throws DotDataException {

        final List<Experiment> scheduledExperimentsOnPage = list(ExperimentFilter.builder()
                .pageId(experimentToCheck.pageId())
                .statuses(Set.of(SCHEDULED))
                .build(), user);

        final boolean noConflicts = scheduledExperimentsOnPage.isEmpty() ||
                scheduledExperimentsOnPage.stream().allMatch(scheduledExperiment -> {
            final Scheduling scheduling = scheduledExperiment.scheduling().orElseThrow();
            final Scheduling schedulingToCheck = experimentToCheck.scheduling().orElseThrow();
            return schedulingToCheck.startDate().orElseThrow().isAfter(scheduling.endDate().orElseThrow()) ||
                    schedulingToCheck.endDate().orElseThrow().isBefore(scheduling.startDate().orElseThrow());
        });

        DotPreconditions.isTrue(noConflicts, ()-> "Scheduling conflict: The same page can't be included in different experiments with overlapping schedules. "
                        + "Overlapping with Experiment: "
                + scheduledExperimentsOnPage.get(0).name(),
                DotStateException.class);
    }

    /**
     * It checks whether there are conflicts between the scheduling of the experimentToCheck and already scheduled Experiments,
     * and if so, it will proceed to cancel the conflicting scheduled Experiments.
     * @param experimentToCheck the Experiment to check for conflicts
     * @param user the User
     * @throws DotDataException
     */
    private void cancelScheduledExperimentsUponConflicts(final Experiment experimentToCheck,
            final User user) throws DotDataException {

        final List<Experiment> scheduledExperimentsOnPage = list(ExperimentFilter.builder()
                .pageId(experimentToCheck.pageId())
                .statuses(Set.of(SCHEDULED))
                .build(), user);

        scheduledExperimentsOnPage.stream().filter(scheduledExperiment -> {
            final Scheduling scheduling = scheduledExperiment.scheduling().orElseThrow();
            final Scheduling schedulingToCheck = experimentToCheck.scheduling().orElseThrow();
            return conflictingStartOrEndDate(scheduling, schedulingToCheck);
        }).forEach(scheduledExperiment -> {
            try {
                cancel(scheduledExperiment.id().orElseThrow(), user);
            } catch (DotDataException | DotSecurityException e) {
                throw new DotStateException(e);
            }
        });
    }

    private static boolean conflictingStartOrEndDate(Scheduling scheduling, Scheduling schedulingToCheck) {
        return (schedulingToCheck.startDate().orElseThrow()
                .isAfter(scheduling.startDate().orElseThrow()) &&
                schedulingToCheck.startDate().orElseThrow()
                        .isBefore(scheduling.endDate().orElseThrow()))
                || (schedulingToCheck.endDate().orElseThrow()
                .isAfter(scheduling.startDate().orElseThrow()) &&
                schedulingToCheck.endDate().orElseThrow()
                        .isBefore(scheduling.endDate().orElseThrow()));
    }

    @Override
    public Experiment startScheduled(String experimentId, User user)
            throws DotDataException, DotSecurityException {
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);
        DotPreconditions.checkArgument(UtilMethods.isSet(experimentId), "experiment Id must be provided.");

        final Experiment persistedExperiment =  find(experimentId, user).orElseThrow(
                ()-> new IllegalArgumentException("Experiment with provided id not found")
        );

        validatePageEditPermissions(user, persistedExperiment,
                "You don't have permission to start the Experiment. "
                        + "Experiment Id: " + persistedExperiment.id());

        DotPreconditions.isTrue(persistedExperiment.status() == Status.SCHEDULED,()-> "Cannot start an already started Experiment.",
                DotStateException.class);

        final Experiment readyToStart = save(Experiment.builder().from(persistedExperiment)
                        .status(RUNNING).build(), user);

        return innerStart(readyToStart, user, false);
    }

    private Experiment innerStart(final Experiment persistedExperiment, final User user,
            final boolean generateNewRunId)
            throws DotSecurityException, DotDataException {

        final Experiment experimentToSave = generateNewRunId
                ? Experiment.builder().from(persistedExperiment).runningIds(getRunningIds(persistedExperiment)).build()
                : persistedExperiment;

        final Experiment running = save(experimentToSave, user);
        cleanRunningExperimentsCache();
        publishExperimentPage(running, user);
        publishContentOnExperimentVariants(user, running);

        SecurityLogger.logInfo(this.getClass(), () -> String.format("Experiment '%s' [%s] has been started by User" +
                " ID '%s'", running.name(), running.id(), user.getUserId()));

        return running;
    }

    private RunningIds getRunningIds(final Experiment persistedExperiment) {
        final RunningIds runningIds = persistedExperiment.runningIds();

        final Optional<RunningId> currentRunningId = runningIds.getAll().stream()
                .filter((id) -> id.endDate() == null)
                .limit(1)
                .findFirst();

        if (currentRunningId.isPresent()) {
            currentRunningId.get().setEndDate(Instant.now());
        }

        runningIds.add(RunningIds.RunningId.create());

        return runningIds;
    }


    private void publishContentOnExperimentVariants(final User user,
            final Experiment runningExperiment)
            throws DotDataException, DotSecurityException {

        final List<Contentlet> contentByVariants = contentletAPI.getAllContentByVariants(user, false,
                runningExperiment.trafficProportion().variants().stream()
                        .map(ExperimentVariant::id).filter((id) -> !id.equals(DEFAULT_VARIANT.name()))
                        .toArray(String[]::new)).stream()
                        .filter((contentlet -> Try.of(contentlet::isWorking)
                                .getOrElse(false))).collect(Collectors.toList());

        contentletAPI.publish(contentByVariants, user, false);
    }

    @Override
    @WrapInTransaction
    public Experiment end(String experimentId, User user)
            throws DotDataException, DotSecurityException {
        try {
            DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                    invalidLicenseMessageSupplier);
            DotPreconditions.checkArgument(UtilMethods.isSet(experimentId), "experiment Id must be provided.");

            final Optional<Experiment> persistedExperimentOpt =  find(experimentId, user);

            DotPreconditions.isTrue(persistedExperimentOpt.isPresent(),()-> "Experiment with provided id not found",
                    DoesNotExistException.class);

            final Experiment experimentFromFactory = persistedExperimentOpt.get();

            validatePagePublishPermissions(user, experimentFromFactory);
            validatePublishTemplateLayoutPermissions(user, experimentFromFactory);

            DotPreconditions.isTrue(experimentFromFactory.status()==Status.RUNNING, ()->
                            "Only RUNNING experiments can be ended", DotStateException.class);

            DotPreconditions.isTrue(persistedExperimentOpt.get().scheduling().isPresent(),
                    ()-> "Scheduling not valid.", DotStateException.class);

            final Scheduling endedScheduling = Scheduling.builder().from(persistedExperimentOpt.get()
                    .scheduling().get()).endDate(Instant.now().plus(1, ChronoUnit.MINUTES))
                    .build();

            final Experiment ended = persistedExperimentOpt.get().withStatus(ENDED)
                    .withScheduling(endedScheduling);
            final Experiment saved = save(ended, user);

            archivedAllVariants(saved);

            cleanRunningExperimentsCache();

            SecurityLogger.logInfo(this.getClass(), () -> String.format("Experiment '%s' [%s] has been ended by User" +
                    " ID '%s'", saved.name(), saved.id(), user.getUserId()));

            return saved;
        } catch (DotSecurityException e) {
            final String message = "You don't have permission to end the Experiment Id: " + experimentId;
            Logger.error(this, message + "\n" + e.getMessage());
            throw new DotSecurityException(message, e);
        }
    }

    private void archivedAllVariants(Experiment saved) throws DotDataException {
        for (ExperimentVariant variant : saved.trafficProportion().variants()) {

            if (!variant.id().equals(DEFAULT_VARIANT.name())) {
                variantAPI.archive(variant.id());
            }
        }
    }

    @WrapInTransaction
    @Override
    public Experiment addVariant(final String experimentId, final String variantDescription,
            final User user)
            throws DotDataException, DotSecurityException {

        final Experiment persistedExperiment = find(experimentId, user)
                .orElseThrow(()->new DoesNotExistException("Experiment with provided id not found"));

        ExperimentVariant experimentVariant = createExperimentVariant(
                persistedExperiment, variantDescription, user);

        final TrafficProportion trafficProportion = persistedExperiment.trafficProportion();

        final TreeSet<ExperimentVariant> variants = new TreeSet<>();
        variants.addAll(trafficProportion.variants());
        variants.add(experimentVariant);

        TreeSet<ExperimentVariant> weightedVariants = trafficProportion.type() == Type.SPLIT_EVENLY
                ? redistributeWeights(variants)
                : variants;

        final TrafficProportion weightedTrafficProportion = trafficProportion
                .withVariants(weightedVariants);

        final Experiment updatedExperiment = persistedExperiment
                .withTrafficProportion(weightedTrafficProportion);

        if (!DEFAULT_VARIANT.name().equals(experimentVariant.id())) {
            copyMultiTrees(persistedExperiment, experimentVariant);
        }

        return save(updatedExperiment, user);
    }

    private void copyMultiTrees(final Experiment persistedExperiment,
            final ExperimentVariant experimentVariant)
                throws DotDataException {

        final HTMLPageAsset experimentPage = getHtmlPageAsset(persistedExperiment);
        final List<MultiTree> multiTreesByVariant = multiTreeAPI.getMultiTreesByVariant(
                experimentPage.getIdentifier(), DEFAULT_VARIANT.name());

        multiTreeAPI.copyMultiTree(experimentPage.getIdentifier(), multiTreesByVariant,
                experimentVariant.id());
    }

    private ExperimentVariant createExperimentVariant(final Experiment experiment,
            final String variantDescription, final User user)
            throws DotDataException {

        final String experimentId = experiment.getIdentifier();
        String variantName = null;

        if(variantDescription.equals(ORIGINAL_VARIANT)) {
            DotPreconditions.isTrue(
                    experiment.trafficProportion().variants().stream().noneMatch((variant) ->
                            variant.description().equals(ORIGINAL_VARIANT)),
                    "Original Variant already created");
            variantName = DEFAULT_VARIANT.name();
        } else {
            variantName = getVariantName(experimentId);
            variantAPI.save(Variant.builder().name(variantName)
                    .description(Optional.of(variantDescription)).build());
        }

        final Contentlet pageContentlet = contentletAPI
        .findContentletByIdentifierAnyLanguage(experiment.pageId(), false);

        final HTMLPageAsset page = pageAssetAPI.fromContentlet(pageContentlet);

        return ExperimentVariant.builder().id(variantName)
                .description(variantDescription).weight(0)
                .url(page.getURI()+"?variantName="+variantName)
                .build();
    }

    private void copyPageAndItsContentForVariant(final Experiment experiment,
            final String variantDescription, final User user,
            final String variantName, final Contentlet pageContentlet) throws DotDataException {
        if(variantDescription.equals(ORIGINAL_VARIANT)) {
            saveContentOnVariant(pageContentlet,
                    variantName, user);

            multiTreeAPI.getMultiTrees(experiment.pageId()).forEach(
                    (multiTree -> {
                        final Contentlet contentlet = Try.of(()->contentletAPI.
                                findContentletByIdentifierAnyLanguage(multiTree.getContentlet()))
                                .getOrElseThrow(()->new DotStateException("Unable to find content. Id:"
                                        + multiTree.getContentlet()));

                        saveContentOnVariant(contentlet, variantName, user);
                    })
            );
        }
    }

    private void saveContentOnVariant(final Contentlet contentlet, final String variantName,
            final User user) {

        final Optional<ContentletVersionInfo> versionInfo = Try.of(
                        ()->versionableAPI.getContentletVersionInfo(contentlet.getIdentifier(),
                                contentlet.getLanguageId()))
                .getOrElseThrow((e->new DotStateException("Unable to get the live version of the page", e)));

        final ContentletVersionInfo contentletVersionInfo = versionInfo.orElseThrow();

        final String inode = UtilMethods.isSet(contentletVersionInfo.getLiveInode())
                ? contentletVersionInfo.getLiveInode()
                : contentletVersionInfo.getWorkingInode();

        final Contentlet checkedoutContentlet = Try.of(() -> contentletAPI
                        .checkout(inode, user, false))
                .getOrElseThrow(
                        (e) -> new DotStateException("Unable to checkout Experiment's content. Inode:" + inode, e));

        checkedoutContentlet.setVariantId(variantName);
        Try.of(() -> contentletAPI.checkin(checkedoutContentlet, user, false))
                .getOrElseThrow(
                        (e) -> new DotStateException("Unable to checkin Experiment's content. Inode:" + inode, e));

    }

    private String getVariantName(final String experimentId) throws DotDataException {
        final String variantNameBase = EXPERIMENT_VARIANT_NAME_PREFIX + shortyIdAPI.shortify(
                experimentId)
                + EXPERIMENT_VARIANT_NAME_SUFFIX;

        final int nextAvailableIndex = getNextAvailableIndex(variantNameBase);

        final String variantName = variantNameBase + nextAvailableIndex;
        return variantName;
    }

    @Override
    @WrapInTransaction
    public Experiment deleteVariant(String experimentId, String variantName, User user)
            throws DotDataException, DotSecurityException {

        DotPreconditions.isTrue(!variantName.equals(DEFAULT_VARIANT.name()),
                ()->"Cannot delete Original Variant", NotAllowedException.class);

        final Experiment persistedExperiment = find(experimentId, user)
                .orElseThrow(()->new DoesNotExistException("Experiment with provided id not found"));

        DotPreconditions.isTrue(variantName!= null &&
                variantName.contains(shortyIdAPI.shortify(experimentId)), ()->"Invalid Variant provided",
                IllegalArgumentException.class);

        final Variant toDelete = variantAPI.get(variantName)
                .orElseThrow(()->new DoesNotExistException("Provided Variant not found"));

        final String variantDescription = toDelete.description()
                .orElseThrow(()->new DotStateException("Variant without description. Variant name: "
                                + toDelete.name()));

        final TreeSet<ExperimentVariant> updatedVariants =
                new TreeSet<>(persistedExperiment.trafficProportion()
                .variants().stream().filter((variant)->
                        !Objects.equals(variant.id(), toDelete.name())).collect(
                        Collectors.toSet()));

        final SortedSet<ExperimentVariant> weightedVariants = redistributeWeights(updatedVariants);
        final TrafficProportion weightedTraffic = persistedExperiment.trafficProportion()
                .withVariants(weightedVariants);
        final Experiment withUpdatedTraffic = persistedExperiment.withTrafficProportion(weightedTraffic);
        Experiment fromDB = save(withUpdatedTraffic, user);
        variantAPI.archive(toDelete.name());
        variantAPI.delete(toDelete.name());

        if(withUpdatedTraffic.trafficProportion().variants().size()==1
                && VariantAPI.DEFAULT_VARIANT.name()
                .equals(withUpdatedTraffic.trafficProportion().variants().first().id())) {
            final TrafficProportion currentTrafficProportion = withUpdatedTraffic.trafficProportion();
            final TrafficProportion splitEvenlyTrafficProportion = TrafficProportion.builder()
                    .from(currentTrafficProportion).type(Type.SPLIT_EVENLY).build();

            fromDB = save(withUpdatedTraffic.withTrafficProportion(splitEvenlyTrafficProportion),
                    user);
        }


        return fromDB;

    }

    @Override
    @WrapInTransaction
    public Experiment editVariantDescription(String experimentId, String variantName,
            String newDescription, final User user)
            throws DotDataException, DotSecurityException {
        final Experiment persistedExperiment = find(experimentId, user)
                .orElseThrow(()->new DoesNotExistException("Experiment with provided id not found"));

        DotPreconditions.isTrue(variantName!= null &&
                        variantName.contains(shortyIdAPI.shortify(experimentId)), ()->"Invalid Variant provided",
                IllegalArgumentException.class);

        final Variant toEdit = variantAPI.get(variantName)
                .orElseThrow(()->new DoesNotExistException("Provided Variant not found"));

        final String currentDescription = toEdit.description()
                .orElseThrow(()->new DotStateException("Variant without description. Variant name: "
                        + toEdit.name()));

        DotPreconditions.isTrue(!currentDescription.equals(ORIGINAL_VARIANT),
                ()->"Cannot update Original Variant", IllegalArgumentException.class);

        final TreeSet<ExperimentVariant> updatedVariants =
                persistedExperiment.trafficProportion()
                        .variants().stream().map((variant) -> {
                            if (variant.id().equals(variantName)) {
                                return variant.withDescription(newDescription);
                            } else {
                                return variant;
                            }
                        }).collect(Collectors.toCollection(TreeSet::new));

        final TrafficProportion trafficProportion = persistedExperiment.trafficProportion()
                .withVariants(updatedVariants);
        final Experiment withUpdatedTraffic = persistedExperiment.withTrafficProportion(trafficProportion);
        final Experiment fromDB = save(withUpdatedTraffic, user);

        final Optional<Variant> variant = variantAPI.get(variantName);
        variantAPI.update(variant.orElseThrow().withDescription(Optional.of(newDescription)));
        return fromDB;

    }

    @Override
    @WrapInTransaction
    public Experiment deleteTargetingCondition(String experimentId, String conditionId, User user)
            throws DotDataException, DotSecurityException {
        final Experiment persistedExperiment = find(experimentId, user)
                .orElseThrow(()->new DoesNotExistException("Experiment with provided id not found"));

        DotPreconditions.isTrue(persistedExperiment.id().isPresent(), "Invalid Experiment");

        DotPreconditions.isTrue(UtilMethods.isSet(conditionId), ()->"Invalid Variant provided",
                IllegalArgumentException.class);

        final Condition conditionToDelete = rulesAPI.getConditionById(conditionId, user, false);
        rulesAPI.deleteCondition(conditionToDelete, user, false);

        final Optional<Experiment> toReturn = find(persistedExperiment.id().get(), user);
        DotPreconditions.isTrue(toReturn.isPresent(), "Experiment not found");

        return toReturn.get();

    }

    @Override
    public List<Experiment> getRunningExperiments() throws DotDataException {
        final List<Experiment> cached = experimentsCache.getList(CACHED_EXPERIMENTS_KEY);
        if (Objects.nonNull(cached)) {
            return cached;
        }

        return cacheRunningExperiments();
    }

    @Override
    public List<Experiment> getRunningExperiments(final Host host) throws DotDataException {
        return getRunningExperiments().stream().filter(experiment -> {
            try {
                final HTMLPageAsset  htmlPageAsset = getHtmlPageAsset(experiment);
                return host.getIdentifier().equals(htmlPageAsset.getHost());
            } catch (DotStateException | DotDataException e) {
                return false;
            }

        }).collect(Collectors.toList());
    }

    @Override
    public Optional<Rule> getRule(final Experiment experiment)
            throws DotDataException, DotSecurityException {

        final List<Rule> rules = APILocator.getRulesAPI()
                .getAllRulesByParent(experiment, APILocator.systemUser(), false);
        return UtilMethods.isSet(rules) ? Optional.of(rules.get(0)) : Optional.empty();
    }

    @Override
    public boolean isAnyExperimentRunning(final Host host) throws DotDataException {
        return ConfigExperimentUtil.INSTANCE.isExperimentEnabled() &&
                !APILocator.getExperimentsAPI().getRunningExperiments(host).isEmpty();
    }

    /**
     * Return the Experiment partial or total result:
     * This method do the follow:
     *
     * <ul>
     *     <li>Hit the CubeJS Server to get the Experiment's data</li>
     *     <li>Analyze Experiment's data to get The Experiment's result according to the {@link com.dotcms.experiments.model.Goals}</li>
     * </ul>
     *
     * @param experiment
     * @param user
     * @return
     */
    @Override
    public ExperimentResults getResults(final Experiment experiment, final User user) throws DotDataException, DotSecurityException {
        final String experimentId = experiment.id()
            .orElseThrow(() -> new IllegalArgumentException("The Experiment must have an Identifier"));
        final Experiment experimentFromDataBase = find(experimentId, APILocator.systemUser())
                .orElseThrow(() -> new NotFoundException("Experiment not found: " + experimentId));

        DotPreconditions.isTrue(
            RESULTS_QUERY_VALID_STATUSES.contains(experimentFromDataBase.status()),
            "The Experiment must be RUNNING or ENDED to get results");

        final CubeJSResultSet totalSessions = getTotalSessions(experimentFromDataBase, user);
        final CubeJSResultSet summarize = getSummary(experimentFromDataBase, user);
        return getResults(experimentFromDataBase, totalSessions, summarize);
    }

    private ExperimentResults getResults(final Experiment experiment, final CubeJSResultSet totalSessions,
                                         final CubeJSResultSet summarize) {

        final Goals goals = experiment.goals()
                .orElseThrow(() -> new IllegalArgumentException("The Experiment must have a Goal"));

        final Goal primaryGoal = goals.primary();
        final SortedSet<ExperimentVariant> variants = experiment.trafficProportion().variants();

        final  ExperimentResults.Builder builder = new ExperimentResults.Builder(variants);
        builder.addPrimaryGoal(primaryGoal);
        builder.trafficProportion(experiment.trafficProportion());

        for (final ResultSetItem totalSession : totalSessions) {
            final String variantName = totalSession.get("Events.variant").orElseThrow().toString();
            final long total = Long.parseLong(totalSession.get("Events.totalSessions").orElseThrow().toString());
            final float convertionRate = getConvertionRate(totalSession);
            final long success = getSuccess(totalSession);

            builder.addTotalSession(variantName, total);

            final VariantResults.UniqueBySessionResume uniqueBySessionResume =
                    new VariantResults.UniqueBySessionResume(success, convertionRate);
            builder.goal(primaryGoal).uniqueBySession(variantName, uniqueBySessionResume);
        }

        for (final ResultSetItem resultSetItem : summarize) {
            final Map<String, Object> attributes = resultSetItem.getAll();

            long totalSessionsByDate = Long.parseLong(attributes.get("Events.totalSessions").toString());
            long success = getSuccess(resultSetItem);
            float convertionRate = getConvertionRate(resultSetItem);

            final String day = resultSetItem.get("Events.day").orElseThrow().toString().replace("T00:00:00.000", "");
            final String variantName = resultSetItem.get("Events.variant").orElseThrow().toString();

            final VariantResults.ResultResumeItem resultResumeItem = new VariantResults.ResultResumeItem(success,
                    totalSessionsByDate, convertionRate);

            builder.goal(primaryGoal).add(variantName, day, resultResumeItem);

        }

        final ExperimentResults experimentResults = builder.build();

        if (experimentResults.getSessions().getVariants().size() >= 2) {
            experimentResults.setBayesianResult(calcBayesian(experimentResults, null));
        }

        return experimentResults;
    }

    private static float getConvertionRate(final ResultSetItem resultSetItem) {
        final Map<String, Object> attributes = resultSetItem.getAll();

        final String convertionRateAttributeName = attributes.keySet().stream()
                .filter(attributeName -> attributeName.endsWith("ConvertionRate"))
                .limit(1)
                .findFirst()
                .orElseThrow();
        return  Float.parseFloat(resultSetItem.get(convertionRateAttributeName).orElseThrow().toString());
    }

    private static long getSuccess(final ResultSetItem resultSetItem) {
        final Map<String, Object> attributes = resultSetItem.getAll();

        final String successesAttributeName = attributes.keySet().stream()
                .filter(attributeName -> attributeName.endsWith("Successes"))
                .limit(1)
                .findFirst()
                .orElseThrow();
        return Long.parseLong(resultSetItem.get(successesAttributeName).orElseThrow().toString());
    }

    /**
     * Get the Running Experiment from Database and put them on the cache
     *
     * @return
     * @throws DotDataException
     */
    @CloseDBIfOpened
    private List<Experiment> cacheRunningExperiments() throws DotDataException {
        final List<Experiment> experiments = FactoryLocator
            .getExperimentsFactory()
            .list(ExperimentFilter.builder().statuses(set(Status.RUNNING)).build());
        experimentsCache.putList(CACHED_EXPERIMENTS_KEY, experiments);
        return experiments;
    }

    /**
     * Clean the Running Experiment List up from cache
     *
     * @return
     * @throws DotDataException
     */
    @CloseDBIfOpened
    private void cleanRunningExperimentsCache() throws DotDataException {
        experimentsCache.removeList(CACHED_EXPERIMENTS_KEY);
    }

    /**
     * Calculates Bayesian results based on {@link ExperimentResults} object gathered results from cube.
     *
     * @param experimentResults experiments results
     * @param goalName          goal name to get results from
     * @return {@link BayesianResult} bayesian results instance
     */
    private BayesianResult calcBayesian(final ExperimentResults experimentResults, final String goalName) {
        DotPreconditions.checkNotNull(experimentResults, "Experiment results should not be null");
        final int variantsNumber = experimentResults.getSessions().getVariants().size();
        DotPreconditions.checkArgument(variantsNumber >= 2, "At least two variants should be put to test");
        DotPreconditions.checkArgument(variantsNumber <= VARIANTS_NUMBER_MAX, "Currently more than variant is not supported");

        final String goal = StringUtils.defaultIfBlank(goalName, PRIMARY_GOAL);
        final BayesianInput bayesianInput = BayesianHelper.get().toBayesianInput(experimentResults, goal);

        return bayesianAPI.doBayesian(bayesianInput);
    }

    public CubeJSResultSet getSummary(final Experiment experiment,
                                             final User user) throws DotDataException, DotSecurityException {
        final CubeJSClient cubeClient = cubeJSClientFactory.create(user);
        final CubeJSQuery cubeJSQuery = ExperimentResultsQueryFactory.INSTANCE.createWithDayGranularity(experiment);
        return cubeClient.send(cubeJSQuery);
    }

    public CubeJSResultSet getTotalSessions(final Experiment experiment, final User user) throws DotDataException, DotSecurityException {
        final CubeJSClient cubeClient = cubeJSClientFactory.create(user);
        final CubeJSQuery cubeJSQuery = ExperimentResultsQueryFactory.INSTANCE.create(experiment);
        return cubeClient.send(cubeJSQuery);
    }

    @Override
    public void endFinalizedExperiments(final User user) throws DotDataException {
        final List<Experiment> finalizedExperiments = getRunningExperiments().stream()
                .filter(experiment -> experiment.scheduling().orElseThrow().endDate().isPresent())
                .filter(experiment -> {
                    final Instant endDate = experiment.scheduling().orElseThrow().endDate()
                            .orElseThrow();
                    return isTimeReach(endDate, ChronoUnit.MINUTES);
                })
                .collect(Collectors.toList());

        finalizedExperiments.forEach((experiment ->
                Try.of(()->end(experiment.id().orElseThrow(), user)).getOrElseThrow((e)->
                        new DotStateException("Unable to end Experiment. Cause:" + e))));
    }

    @Override
    public Experiment promoteVariant(String experimentId, String variantName, User user)
            throws DotDataException, DotSecurityException {

        try {
            final Experiment persistedExperiment = find(experimentId, user)
                    .orElseThrow(()->new DoesNotExistException("Experiment with provided id not found"));

            validatePagePublishPermissions(user, persistedExperiment);
            validatePublishTemplateLayoutPermissions(user, persistedExperiment);

            DotPreconditions.isTrue(persistedExperiment.status().equals(Status.RUNNING) ||
                    persistedExperiment.status().equals(ENDED),
                    ()->"Experiment must be running or ended to promote a variant",
                    DotStateException.class);

            DotPreconditions.isTrue(variantName!= null &&
                            variantName.contains(shortyIdAPI.shortify(experimentId)), ()->"Invalid Variant provided",
                    IllegalArgumentException.class);

            final Variant variantToPromote = variantAPI.get(variantName)
                    .orElseThrow(()->new DoesNotExistException("Provided Variant not found"));

            final Experiment withUpdatedVariants = getUpdatedVariants(user, persistedExperiment,
                    variantToPromote);

            Experiment savedExperiment = save(withUpdatedVariants, user);

            if(withUpdatedVariants.status()==RUNNING) {
                savedExperiment = end(withUpdatedVariants.id().orElseThrow(), user);
            }

            return savedExperiment;
        } catch (final DotSecurityException e) {
            final String message = "You don't have permission to promote a Variant. Experiment Id: "
                    + experimentId;
            Logger.error(this, message + "\n" + e.getMessage());
            throw new DotSecurityException(message, e);
        }
    }

    private void validateEditTemplateLayoutPermissions(final User user, final Experiment experiment)
            throws DotDataException, DotSecurityException {
        final String errorMessage = String.format(
                "User %s doesn't have EDIT permission for Template-Layouts on the Experiment Page's site. Experiment Id: %s",
                user.getUserId(), experiment.id().orElseGet(() -> "NO ID"));
        validateTemplateLayoutPermissions(user, experiment, PermissionLevel.EDIT, errorMessage);
    }
    private void validatePublishTemplateLayoutPermissions(final User user, final Experiment experiment)
            throws DotDataException, DotSecurityException {
        final String errorMessage = String.format(
                "User %s doesn't have PUBLISH permission for Template-Layouts on the Experiment Page's site. Experiment Id: %s",
                user.getUserId(), experiment.id().orElseThrow());

        validateTemplateLayoutPermissions(user, experiment, PermissionLevel.PUBLISH, errorMessage);
    }

    private void validateTemplateLayoutPermissions(final User user, final Experiment experiment,
            final PermissionLevel permissionLevel, final String errorMessage) throws DotDataException, DotSecurityException {

        final HTMLPageAsset htmlPageAsset = getHtmlPageAsset(experiment);

        if (!UtilMethods.isSet(htmlPageAsset)) {
             return;
        }

        final Host host = APILocator.getHostAPI().find(htmlPageAsset.getHost(), APILocator.systemUser(),
                false);

        if (!permissionAPI.doesUserHavePermissions(host.getIdentifier(),
                PermissionableType.TEMPLATE_LAYOUTS, permissionLevel.getType(), user)) {

            throw new DotSecurityException(errorMessage);
        }
    }

    private HTMLPageAsset getHtmlPageAsset(Experiment experiment) throws DotDataException {
        final Contentlet pageAsContent = contentletAPI
                .findContentletByIdentifierAnyLanguage(experiment.pageId(), DEFAULT_VARIANT.name(), true);

        final HTMLPageAsset htmlPageAsset = APILocator.getHTMLPageAssetAPI()
                .fromContentlet(pageAsContent);
        return htmlPageAsset;
    }

    private Experiment getUpdatedVariants(final User user, final Experiment persistedExperiment,
            final Variant variantToPromote) {

        final String variantName = variantToPromote.name();
        final User systemUser = APILocator.systemUser();

        final TreeSet<ExperimentVariant> variantsAfterPromotion =
                persistedExperiment.trafficProportion()
                        .variants().stream().map((variant) -> {
                            if (variant.id().equals(variantName)) {
                                Try.run(()-> variantAPI.promote(variantToPromote, systemUser))
                                        .getOrElseThrow(()-> new DotRuntimeException("Unable to promote variant. Variant name: " + variantName));
                                return variant.withPromoted(true);
                            } else {
                                return variant.withPromoted(false);
                            }
                        }).collect(Collectors.toCollection(TreeSet::new));

        final TrafficProportion trafficProportion = persistedExperiment.trafficProportion()
                .withVariants(variantsAfterPromotion);
        Experiment withUpdatedVariants = persistedExperiment.withTrafficProportion(trafficProportion);
        return withUpdatedVariants;
    }

    @Override
    public Experiment cancel(String experimentId, User user)
            throws DotDataException, DotSecurityException {
        DotPreconditions.checkArgument(UtilMethods.isSet(experimentId), "experiment Id must be provided.");

        final Optional<Experiment> persistedExperimentOpt =  find(experimentId, user);

        DotPreconditions.isTrue(persistedExperimentOpt.isPresent(),()-> "Experiment with provided id not found",
                DoesNotExistException.class);

        final Experiment experimentFromFactory = persistedExperimentOpt.get();
        validatePageEditPermissions(user, experimentFromFactory,
                "You don't have permission to cancel the Experiment. "
                        + "Experiment Id: " + persistedExperimentOpt.get().id());

        DotPreconditions.isTrue(canBeCanceled(experimentFromFactory), ()->
                "Only SCHEDULED/RUNNING experiments can be canceled", DotStateException.class);

        DotPreconditions.isTrue(persistedExperimentOpt.get().scheduling().isPresent(),
                ()-> "Scheduling not valid.", DotStateException.class);

        final Experiment experimentCanceled = experimentFromFactory
                .withStatus(DRAFT)
                .withScheduling(Scheduling.builder().build());

        final Experiment afterSave = save(experimentCanceled, user);

        SecurityLogger.logInfo(this.getClass(), () -> String.format("Experiment '%s' [%s] has been canceled by User" +
                " ID '%s'", afterSave.name(), afterSave.id(), user.getUserId()));

        return afterSave;
    }

    private static boolean canBeCanceled(final Experiment experimentFromFactory) {
        return experimentFromFactory.status() == SCHEDULED
                || experimentFromFactory.status() == RUNNING;
    }

    @Override
    public void startScheduledToStartExperiments(final User user) throws DotDataException {
        final List<Experiment> scheduledToStartExperiments = list(ExperimentFilter.builder()
                .statuses(CollectionsUtils.set(SCHEDULED)).build(), user).stream()
                .filter((experiment -> {
                    final Instant startDate = experiment.scheduling().get().startDate().orElseThrow();
                    return isTimeReach(startDate, ChronoUnit.MINUTES);
                }))
                .collect(Collectors.toList());

        scheduledToStartExperiments.forEach((experiment ->
                Try.of(()->startScheduled(experiment.id().orElseThrow(), user)).getOrElseThrow((e)->
                        new DotStateException("Unable to start Experiment. Cause:" + e))));
    }

    private TreeSet<ExperimentVariant> redistributeWeights(final Set<ExperimentVariant> variants) {

        final int count = variants.size();

        final float weightPerEach = 100f / count;

        Set<ExperimentVariant> weightedVariants = variants.stream()
                .map((variant)-> variant.withWeight(weightPerEach))
                .collect(Collectors.toSet());

        return new TreeSet<>(weightedVariants);
    }

    private int getNextAvailableIndex(final String variantNameBase)
            throws DotDataException {
        int variantIndex = 1;
        String variantNameToTry = variantNameBase
                + variantIndex;

        while(variantAPI.get(variantNameToTry).isPresent()) {
            variantNameToTry = variantNameBase
                    + (++variantIndex);
        }

        return variantIndex;
    }

    private Scheduling startNowScheduling() {
        // Setting "now" with an additional minute to avoid failing validation
        final Instant now = Instant.now().plus(1, ChronoUnit.MINUTES);
        return Scheduling.builder().startDate(now)
                .endDate(now.plus(EXPERIMENTS_DEFAULT_DURATION.get(), ChronoUnit.DAYS))
                .build();
    }

    private boolean hasAtLeastOneVariant(final Experiment experiment) {
        return experiment.trafficProportion().variants().size()>1;
    }

    private void validatePagePublishPermissions(final User user, final Experiment experiment)
            throws DotDataException, DotSecurityException {
        final String messageFormat = "User %s doesn't have permission to publish the Experiment's page. Experiment Id: %s";

        final String errorMessage = String.format(messageFormat, user.getUserId(), experiment.id().orElseThrow());
        validateExperimentPagePermissions(user, experiment, PermissionLevel.PUBLISH, errorMessage);
    }

    private void validatePageEditPermissions(final User user, final Experiment persistedExperiment,
            final String errorMessage)
            throws DotDataException, DotSecurityException {
        
        try {
            validateExperimentPagePermissions(user, persistedExperiment, PermissionLevel.EDIT,
                    errorMessage);
        } catch (DotSecurityException e) {
            Logger.error(this, errorMessage);
            throw e;
        }
    }

    private void validateExperimentPagePermissions(final User user,
            final Experiment persistedExperiment,
            final PermissionLevel permissionLevel,
            final String errorMessage)
            throws DotDataException, DotSecurityException {

        PermissionableProxy parentPage = new PermissionableProxy();
        parentPage.setIdentifier(persistedExperiment.pageId());
        parentPage.setType("htmlpage");

        if (!permissionAPI.doesUserHavePermission(parentPage, permissionLevel.getType(),
                user)) {
            throw new DotSecurityException(errorMessage);
        }
    }

    public Scheduling validateScheduling(final Scheduling scheduling) {
        if(scheduling==null || (scheduling.startDate().isEmpty() && scheduling.endDate().isEmpty())) {
            return scheduling;
        }

        Scheduling toReturn = scheduling;
        final Instant NOW = Instant.now().minus(1, ChronoUnit.MINUTES);

        if(scheduling.startDate().isPresent() && scheduling.endDate().isEmpty()) {
            DotPreconditions.checkState(scheduling.startDate().get().isAfter(NOW),
                    "Invalid Scheduling. Start date is in the past");

            toReturn = scheduling.withEndDate(scheduling.startDate().get()
                    .plus(EXPERIMENTS_DEFAULT_DURATION.get(), ChronoUnit.DAYS));
        } else if(scheduling.startDate().isEmpty() && scheduling.endDate().isPresent()) {
            DotPreconditions.checkState(scheduling.endDate().get().isAfter(NOW),
                    "Invalid Scheduling. End date is in the past");

            final Instant startDate = scheduling.endDate().get().minus(EXPERIMENTS_DEFAULT_DURATION.get(),
                    ChronoUnit.DAYS);

            toReturn = scheduling.withStartDate(startDate);
        } else {
            DotPreconditions.checkState(scheduling.startDate().get().isAfter(NOW),
                    "Invalid Scheduling. Start date is in the past");

            DotPreconditions.checkState(scheduling.endDate().get().isAfter(NOW),
                    "Invalid Scheduling. End date is in the past");

            DotPreconditions.checkState(scheduling.endDate().get().isAfter(scheduling.startDate().get()),
                    "Invalid Scheduling. End date must be after the start date");

            DotPreconditions.checkState(Duration.between(scheduling.startDate().get(),
                            scheduling.endDate().get()).toDays() >= EXPERIMENTS_MIN_DURATION.get(),
                    "Experiment duration must be at least "
                            + EXPERIMENTS_MIN_DURATION.get() +" days. ");

            DotPreconditions.checkState(Duration.between(scheduling.startDate().get(),
                            scheduling.endDate().get()).toDays() <= EXPERIMENTS_MAX_DURATION.get(),
                    "Experiment duration must be less than "
                            + EXPERIMENTS_MAX_DURATION.get() +" days. ");
        }
        return toReturn;
    }

    @Override
    public Optional<Experiment> getRunningExperimentPerPage(final String pageId) throws DotDataException {

        return getRunningExperiments().stream()
                .filter(experiment ->
                        experiment.pageId().equals(pageId)
                )
                .findFirst();
    }

    private boolean hasValidLicense(){
        return (licenseValiditySupplierSupplier.hasValidLicense());
    }

    private void checkAndDeleteExperiment(final Contentlet contentlet, final User user)  {

        try {

            if (!contentlet.isHTMLPage()) {
                return;
            }

            final List<Experiment> pageExperiments = APILocator.getExperimentsAPI().list(
                    ExperimentFilter.builder().pageId(contentlet.getIdentifier()).build(), user);

            for (final Experiment pageExperiment : pageExperiments) {
                try {
                    APILocator.getExperimentsAPI()
                            .forceDelete(pageExperiment.id().orElseThrow(), user);
                } catch (DotDataException | DotSecurityException e) {
                    final String message = String.format("Unable to delete experiment %s",
                            pageExperiment.id().orElseThrow());
                    throw new DotRuntimeException(message, e);
                }
            }
        } catch (DotDataException e) {
            final String message = String.format("Unable to delete experiment %s",
                    contentlet.getIdentifier());
            throw new DotRuntimeException(message, e);
        }
    }
}
