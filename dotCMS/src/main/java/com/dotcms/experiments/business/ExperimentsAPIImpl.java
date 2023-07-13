package com.dotcms.experiments.business;

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

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.bayesian.BayesianAPI;
import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.BayesianResult;
import com.dotcms.analytics.helper.AnalyticsHelper;

import com.dotcms.analytics.helper.BayesianHelper;
import com.dotcms.analytics.metrics.AbstractCondition.Operator;
import com.dotcms.analytics.metrics.EventType;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.analytics.metrics.MetricsUtil;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.cube.CubeJSClient;
import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.CubeJSResultSet;
import com.dotcms.cube.CubeJSResultSet.ResultSetItem;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.experiments.business.result.BrowserSession;
import com.dotcms.experiments.business.result.Event;
import com.dotcms.experiments.business.result.ExperimentAnalyzerUtil;
import com.dotcms.experiments.business.result.ExperimentResults;
import com.dotcms.experiments.business.result.ExperimentResultsQueryFactory;
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

import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.WebAPILocator;
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
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import graphql.VisibleForTesting;
import io.vavr.control.Try;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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

    private final LicenseValiditySupplier licenseValiditySupplierSupplier =
            new LicenseValiditySupplier() {};

    private final Supplier<String> invalidLicenseMessageSupplier =
            ()->"Valid License is required";

    private final AnalyticsHelper analyticsHelper;

    @VisibleForTesting
    public ExperimentsAPIImpl(final AnalyticsHelper analyticsHelper) {
        this.analyticsHelper = analyticsHelper;
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

        final Contentlet pageAsContent = contentletAPI
                .findContentletByIdentifierAnyLanguage(experiment.pageId(), DEFAULT_VARIANT.name());

        DotPreconditions.isTrue(pageAsContent!=null
                && UtilMethods.isSet(pageAsContent.getIdentifier()),
                DotStateException.class, ()->"Invalid Page provided");

        if(!permissionAPI.doesUserHavePermission(pageAsContent, PermissionLevel.EDIT.getType(), user)) {
            Logger.error(this, "You don't have permission to save the Experiment."
                    + " Experiment name: " + experiment.name() + ". Page Id: " + experiment.pageId());
            throw new DotSecurityException("You don't have permission to save the Experiment.");
        }

        Experiment.Builder builder = Experiment.builder().from(experiment);

        if(experiment.id().isEmpty()) {
            builder.id(UUIDGenerator.generateUuid());
        }

        builder.modDate(Instant.now());
        builder.lastModifiedBy(user.getUserId());
        
        if(experiment.goals().isPresent()) {
            final Goals goals = experiment.goals().orElseThrow();
            MetricsUtil.INSTANCE.validateGoals(goals);

            addConditionIfIsNeed(goals,
                    APILocator.getHTMLPageAssetAPI().fromContentlet(pageAsContent), builder);
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

        return savedExperiment.get();
    }

    private static boolean isExistingSchedulingChanging(Experiment experimentToSave,
            Optional<Experiment> existingExperiment) {
        return !existingExperiment.get().scheduling().
                equals(experimentToSave.scheduling());
    }

    private void addConditionIfIsNeed(final Goals goals, final HTMLPageAsset page,
            final Builder builder) {

        if (goals.primary().getMetric().type() == MetricType.BOUNCE_RATE &&
                !hasCondition(goals, "url")) {
            addUrlCondition(page, builder, goals);
        }
    }

    private void addUrlCondition(final HTMLPageAsset page, final Builder builder, final Goals goals) {

        final com.dotcms.analytics.metrics.Condition refererCondition = createConditionWithUrlValue(
                page, "url");

        final Goals newGoal = createNewGoals(goals, refererCondition);
        builder.goals(newGoal);
    }

    @NotNull
    private static Goals createNewGoals(final Goals oldGoals,
            final com.dotcms.analytics.metrics.Condition newConditionToAdd) {
        final Metric newMetric = Metric.builder().from(oldGoals.primary().getMetric())
                .addConditions(newConditionToAdd).build();
        final Goal newGoal = GoalFactory.create(newMetric);
        return Goals.builder().from(oldGoals).primary(newGoal).build();
    }

    private boolean hasCondition(final Goals goals, final String conditionName){
        return goals.primary().getMetric().conditions()
                .stream()
                .anyMatch(condition ->conditionName .equals(condition.parameter()));
    }
    private com.dotcms.analytics.metrics.Condition createConditionWithUrlValue(
            final HTMLPageAsset page,
            final String conditionName) {

            return com.dotcms.analytics.metrics.Condition.builder()
                    .parameter(conditionName)
                    .operator(Operator.REGEX)
                    .value(ExperimentUrlPatternCalculator.INSTANCE.calculateUrlRegexPattern(page))
                    .build();

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
            validatePermissions(user, experiment.get(),
                    "You don't have permission to get the Experiment. "
                            + "Experiment Id: " + experiment.get().id());

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

        validatePermissions(user, persistedExperiment.get(),
                "You don't have permission to archive the Experiment. "
                        + "Experiment Id: " + persistedExperiment.get().id());

        DotPreconditions.isTrue(persistedExperiment.get().status()==ENDED,
                ()-> "Only ENDED experiments can be archived",
                DotStateException.class);

        final Experiment archived = persistedExperiment.get().withStatus(ARCHIVED);
        return save(archived, user);
    }

    @Override
    @WrapInTransaction
    public void delete(final String id, final User user)
            throws DotDataException, DotSecurityException {
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);
        DotPreconditions.checkArgument(UtilMethods.isSet(id), "id must be provided.");

        final Optional<Experiment> persistedExperiment =  find(id, user);

        DotPreconditions.isTrue(persistedExperiment.isPresent(),()-> "Experiment with provided id not found",
                DoesNotExistException.class);

        validatePermissions(user, persistedExperiment.get(),
                "You don't have permission to delete the Experiment. "
                        + "Experiment Id: " + persistedExperiment.get().id());

        if(persistedExperiment.get().status() != DRAFT &&
                persistedExperiment.get().status() != Status.SCHEDULED) {
            throw new DotStateException("Only DRAFT or SCHEDULED experiments can be deleted");
        }

        factory.delete(persistedExperiment.get());
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
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);
        DotPreconditions.checkArgument(UtilMethods.isSet(experimentId), "experiment Id must be provided.");

        final Experiment persistedExperiment =  find(experimentId, user).orElseThrow(
                ()-> new IllegalArgumentException("Experiment with provided id not found")
        );

        validatePermissions(user, persistedExperiment,
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

        final List<Experiment> runningExperimentsOnPage = list(ExperimentFilter.builder()
                .pageId(persistedExperiment.pageId())
                .statuses(Set.of(RUNNING))
                .build(), user);

        if(!runningExperimentsOnPage.isEmpty()) {
            final boolean meantToRunNow = persistedExperiment.scheduling().isEmpty();

            if(meantToRunNow) {
                throw new DotStateException("There is a running Experiment on the same page. Name: "
                        + runningExperimentsOnPage.get(0).name());
            }

            final Experiment runningExperiment = runningExperimentsOnPage.get(0);
            DotPreconditions.isTrue(runningExperiment.scheduling().orElseThrow().endDate()
                    .orElseThrow().isBefore(persistedExperiment.scheduling().orElseThrow().startDate().orElseThrow()),
                    ()-> "Scheduling conflict: The same page can't be included in different experiments with overlapping schedules. "
                            + "Overlapping with Experiment: "
                            + runningExperiment.name(),
                    DotStateException.class);
        }

        Experiment toReturn;

        if(emptyScheduling(persistedExperiment)) {
            final Scheduling scheduling = startNowScheduling(persistedExperiment);
            final Experiment experimentToSave = persistedExperiment.withScheduling(scheduling).withStatus(RUNNING);
            validateNoConflictsWithScheduledExperiments(experimentToSave, user);
            toReturn = innerStart(experimentToSave, user);
        } else {
            Scheduling scheduling = persistedExperiment.scheduling().get();
            final Experiment experimentToSave = persistedExperiment.withScheduling(scheduling).withStatus(SCHEDULED);
            validateNoConflictsWithScheduledExperiments(experimentToSave, user);
            toReturn = save(experimentToSave.withScheduling(scheduling).withStatus(SCHEDULED), user);
        }

        return toReturn;
    }

    private void publishExperimentPage(final Experiment experiment, final User user)
            throws DotDataException, DotSecurityException {
        final HTMLPageAsset htmlPageAsset = APILocator.getHTMLPageAssetAPI().fromContentlet(contentletAPI
                .findContentletByIdentifierAnyLanguage(experiment.pageId(), false));

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

    @Override
    public Experiment startScheduled(String experimentId, User user)
            throws DotDataException, DotSecurityException {
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);
        DotPreconditions.checkArgument(UtilMethods.isSet(experimentId), "experiment Id must be provided.");

        final Experiment persistedExperiment =  find(experimentId, user).orElseThrow(
                ()-> new IllegalArgumentException("Experiment with provided id not found")
        );

        validatePermissions(user, persistedExperiment,
                "You don't have permission to start the Experiment. "
                        + "Experiment Id: " + persistedExperiment.id());

        DotPreconditions.isTrue(persistedExperiment.status() == Status.SCHEDULED,()-> "Cannot start an already started Experiment.",
                DotStateException.class);

        return innerStart(persistedExperiment, user);
    }

    private Experiment innerStart(final Experiment persistedExperiment, final User user)
            throws DotSecurityException, DotDataException {

        final Experiment experimentToSave = Experiment.builder().from(persistedExperiment)
                .runningIds(getRunningIds(persistedExperiment))
                .status(RUNNING)
                .build();

        Experiment running = save(experimentToSave, user);
        cacheRunningExperiments();
        publishExperimentPage(running, user);
        publishContentOnExperimentVariants(user, running);

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
        DotPreconditions.isTrue(runningExperiment.status().equals(RUNNING),
                "Experiment needs to be RUNNING");

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
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);
        DotPreconditions.checkArgument(UtilMethods.isSet(experimentId), "experiment Id must be provided.");

        final Optional<Experiment> persistedExperimentOpt =  find(experimentId, user);

        DotPreconditions.isTrue(persistedExperimentOpt.isPresent(),()-> "Experiment with provided id not found",
                DoesNotExistException.class);

        final Experiment experimentFromFactory = persistedExperimentOpt.get();
        validatePermissions(user, experimentFromFactory,
                "You don't have permission to archive the Experiment. "
                        + "Experiment Id: " + persistedExperimentOpt.get().id());

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

        cacheRunningExperiments();

        return saved;
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

        return save(updatedExperiment, user);
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
        final Experiment fromDB = save(withUpdatedTraffic, user);
        variantAPI.archive(toDelete.name());
        variantAPI.delete(toDelete.name());
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
        final List<Experiment> cached = experimentsCache.getList(ExperimentsCache.CACHED_EXPERIMENTS_KEY);
        if (Objects.nonNull(cached)) {
            return cached;
        }

        return cacheRunningExperiments();
    }

    @Override
    public Optional<Rule> getRule(final Experiment experiment)
            throws DotDataException, DotSecurityException {

        final List<Rule> rules = APILocator.getRulesAPI()
                .getAllRulesByParent(experiment, APILocator.systemUser(), false);
        return UtilMethods.isSet(rules) ? Optional.of(rules.get(0)) : Optional.empty();
    }

    @Override
    public boolean isAnyExperimentRunning() throws DotDataException {
        return ConfigExperimentUtil.INSTANCE.isExperimentEnabled() &&
                !APILocator.getExperimentsAPI().getRunningExperiments().isEmpty();
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

        final List<BrowserSession> events = getEvents(experiment, user);
        final ExperimentResults experimentResults = ExperimentAnalyzerUtil.INSTANCE.getExperimentResult(
            experiment, events);

        experimentResults.setBayesianResult(calcBayesian(experimentResults, null));

        return experimentResults;
    }

    @CloseDBIfOpened
    private List<Experiment> cacheRunningExperiments() throws DotDataException {
        final List<Experiment> experiments = FactoryLocator
            .getExperimentsFactory()
            .list(ExperimentFilter.builder().statuses(set(Status.RUNNING)).build());
        experimentsCache.putList(ExperimentsCache.CACHED_EXPERIMENTS_KEY, experiments);
        return experiments;
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

    @Override
    public List<BrowserSession> getEvents(final Experiment experiment, final User user) throws DotDataException {
        try{
            final AnalyticsApp analyticsApp = resolveAnalyticsApp(user);

            final CubeJSClient cubeClient = new CubeJSClient(
                    analyticsApp.getAnalyticsProperties().analyticsReadUrl());

            final CubeJSQuery cubeJSQuery = ExperimentResultsQueryFactory.INSTANCE
                    .create(experiment);

            final CubeJSResultSet cubeJSResultSet = cubeClient.sendWithPagination(cubeJSQuery);

            String previousLookBackWindow = null;
            final List<Event> currentEvents = new ArrayList<>();
            final List<BrowserSession> sessions = new ArrayList<>();

            for (final ResultSetItem resultSetItem : cubeJSResultSet) {
                final String currentLookBackWindow = resultSetItem.get("Events.lookBackWindow")
                        .map(Object::toString)
                        .orElse(StringPool.BLANK);

                if (!currentLookBackWindow.equals(previousLookBackWindow)) {
                    if (!currentEvents.isEmpty()) {
                        sessions.add(new BrowserSession(previousLookBackWindow, new ArrayList<>(currentEvents)));
                        currentEvents.clear();
                    }
                }

                currentEvents.add(new Event(resultSetItem.getAll(),
                            EventType.get(resultSetItem.get("Events.eventType")
                                    .map(Object::toString)
                                    .orElseThrow(() -> new IllegalStateException("Type into Event is expected")))
                ));

                previousLookBackWindow = currentLookBackWindow;
            }

            if (!currentEvents.isEmpty()) {
                sessions.add(new BrowserSession(previousLookBackWindow, new ArrayList<>(currentEvents)));
            }

            return sessions;
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private AnalyticsApp resolveAnalyticsApp(final User user) throws DotDataException, DotSecurityException {
        final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHost();
        try {
            return analyticsHelper.appFromHost(currentHost);
        } catch (final IllegalStateException e) {
            throw new DotDataException(
                Try.of(() ->
                    LanguageUtil.get(
                        user,
                        "analytics.app.not.configured",
                        AnalyticsHelper.extractMissingAnalyticsProps(e)))
                    .getOrElse(String.format("Analytics App not found for host: %s", currentHost.getHostname())));
        }
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

        final Experiment persistedExperiment = find(experimentId, user)
                .orElseThrow(()->new DoesNotExistException("Experiment with provided id not found"));

        DotPreconditions.isTrue(persistedExperiment.status().equals(Status.RUNNING) ||
                persistedExperiment.status().equals(ENDED),
                ()->"Experiment must be running or ended to promote a variant",
                DotStateException.class);

        DotPreconditions.isTrue(variantName!= null &&
                        variantName.contains(shortyIdAPI.shortify(experimentId)), ()->"Invalid Variant provided",
                IllegalArgumentException.class);

        final Variant variantToPromote = variantAPI.get(variantName)
                .orElseThrow(()->new DoesNotExistException("Provided Variant not found"));

        final TreeSet<ExperimentVariant> variantsAfterPromotion =
                persistedExperiment.trafficProportion()
                        .variants().stream().map((variant) -> {
                            if (variant.id().equals(variantName)) {
                                Try.run(()->variantAPI.promote(variantToPromote, user))
                                        .getOrElseThrow(()-> new DotRuntimeException("Unable to promote variant. Variant name: " + variantName));
                                return variant.withPromoted(true);
                            } else {
                                return variant.withPromoted(false);
                            }
                        }).collect(Collectors.toCollection(TreeSet::new));

        final TrafficProportion trafficProportion = persistedExperiment.trafficProportion()
                .withVariants(variantsAfterPromotion);
        Experiment withUpdatedVariants = persistedExperiment.withTrafficProportion(trafficProportion);

        withUpdatedVariants = save(withUpdatedVariants, user);

        if(withUpdatedVariants.status()==RUNNING) {
            withUpdatedVariants = end(withUpdatedVariants.id().orElseThrow(), user);
        }

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
        validatePermissions(user, experimentFromFactory,
                "You don't have permission to cancel the Experiment. "
                        + "Experiment Id: " + persistedExperimentOpt.get().id());

        DotPreconditions.isTrue(experimentFromFactory.status()== SCHEDULED, ()->
                "Only SCHEDULED experiments can be canceled", DotStateException.class);

        DotPreconditions.isTrue(persistedExperimentOpt.get().scheduling().isPresent(),
                ()-> "Scheduling not valid.", DotStateException.class);

        return save(experimentFromFactory.withStatus(DRAFT), user);
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

    private Scheduling startNowScheduling(final Experiment experiment) {
        // Setting "now" with an additional minute to avoid failing validation
        final Instant now = Instant.now().plus(1, ChronoUnit.MINUTES);
        return Scheduling.builder().startDate(now)
                .endDate(now.plus(EXPERIMENTS_MAX_DURATION.get(), ChronoUnit.DAYS))
                .build();
    }

    private boolean hasAtLeastOneVariant(final Experiment experiment) {
        return experiment.trafficProportion().variants().size()>1;
    }

    private void validatePermissions(final User user, final Experiment persistedExperiment,
            final String errorMessage)
            throws DotDataException, DotSecurityException {
        PermissionableProxy parentPage = new PermissionableProxy();
        parentPage.setIdentifier(persistedExperiment.pageId());
        parentPage.setType("htmlpage");

        if (!permissionAPI.doesUserHavePermission(parentPage, PermissionLevel.EDIT.getType(),
                user)) {
            Logger.error(this, errorMessage);
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
                    .plus(EXPERIMENTS_MAX_DURATION.get(), ChronoUnit.DAYS));
        } else if(scheduling.startDate().isEmpty() && scheduling.endDate().isPresent()) {
            DotPreconditions.checkState(scheduling.endDate().get().isAfter(NOW),
                    "Invalid Scheduling. End date is in the past");

            final Instant startDate = scheduling.endDate().get().minus(EXPERIMENTS_MAX_DURATION.get(),
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

    private boolean hasValidLicense(){
        return (licenseValiditySupplierSupplier.hasValidLicense());
    }

}
