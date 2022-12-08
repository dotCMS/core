package com.dotcms.experiments.business;

import static com.dotcms.experiments.model.AbstractExperiment.Status.DRAFT;
import static com.dotcms.experiments.model.AbstractExperiment.Status.ENDED;
import static com.dotcms.experiments.model.AbstractExperiment.Status.RUNNING;
import static com.dotcms.experiments.model.AbstractExperimentVariant.EXPERIMENT_VARIANT_NAME_PREFIX;
import static com.dotcms.experiments.model.AbstractExperimentVariant.EXPERIMENT_VARIANT_NAME_SUFFIX;

import static com.dotcms.util.CollectionsUtils.set;

import static com.dotcms.experiments.model.AbstractExperimentVariant.ORIGINAL_VARIANT;
import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;

import com.dotcms.analytics.metrics.MetricsUtil;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.AbstractTrafficProportion.Type;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.experiments.model.TargetingCondition;
import com.dotcms.experiments.model.TrafficProportion;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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
import com.liferay.portal.model.User;
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

public class ExperimentsAPIImpl implements ExperimentsAPI {

    final ExperimentsFactory factory = FactoryLocator.getExperimentsFactory();
    final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    final VariantAPI variantAPI = APILocator.getVariantAPI();
    final ShortyIdAPI shortyIdAPI = APILocator.getShortyAPI();
    final RulesAPI rulesAPI = APILocator.getRulesAPI();

    private final LicenseValiditySupplier licenseValiditySupplierSupplier =
            new LicenseValiditySupplier() {};

    private final Supplier<String> invalidLicenseMessageSupplier =
            ()->"Valid License is required";

    @Override
    @WrapInTransaction
    public Experiment save(final Experiment experiment, final User user) throws
            DotSecurityException, DotDataException {
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);

        final Contentlet pageAsContent = contentletAPI
                .findContentletByIdentifierAnyLanguage(experiment.pageId(), false);

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
            MetricsUtil.INSTANCE.validateGoals(experiment.goals().get());
        }

        if(experiment.targetingConditions().isPresent()) {
            saveTargetingConditions(experiment, user);
        }

        final Experiment experimentToSave = builder.build();

        factory.save(experimentToSave);

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
            : createCondition(experimentRule, targetingCondition);

        condition.setOperator(targetingCondition.operator());
        condition.setConditionletId(targetingCondition.conditionKey());
        condition.setValues(new ArrayList<>());
        targetingCondition.values().forEach(condition::addValue);

        condition.checkValid();

        Try.run(()->rulesAPI.saveCondition(condition, user, false))
                .getOrElseThrow(()->new DotStateException("Error saving Condition: "
                        + condition.getConditionletId()));
    }

    private Condition createCondition(final Rule experimentRule,
            final TargetingCondition targetingCondition) {
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

        final Experiment archived = persistedExperiment.get().withStatus(Status.ARCHIVED);
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

        final Experiment experimentFromFactory = persistedExperiment;
        validatePermissions(user, experimentFromFactory,
                "You don't have permission to start the Experiment. "
                        + "Experiment Id: " + persistedExperiment.id());

        DotPreconditions.isTrue(experimentFromFactory.status()!=Status.RUNNING ||
                        experimentFromFactory.status() == Status.SCHEDULED,()-> "Cannot start an already started Experiment.",
                DotStateException.class);

        DotPreconditions.isTrue(experimentFromFactory.status()== DRAFT
                ,()-> "Only DRAFT experiments can be started",
                DotStateException.class);

        DotPreconditions.checkState(hasAtLeastOneVariant(experimentFromFactory), "The Experiment needs at "
                + "least one Page Variant in order to be started.");

        DotPreconditions.checkState(experimentFromFactory.goals().isPresent(), "The Experiment needs to "
                + "have the Goal set.");

        final Experiment experimentToStart;

        if(experimentFromFactory.scheduling().isEmpty()) {
            final Scheduling scheduling = startNowScheduling(experimentFromFactory);
            experimentToStart = experimentFromFactory.withScheduling(scheduling);
        } else {
            Scheduling scheduling = validateScheduling(experimentFromFactory.scheduling().get());
            experimentToStart = experimentFromFactory.withScheduling(scheduling);
        }

        Experiment running = experimentToStart.withStatus(Status.RUNNING);
        running = save(running, user);

        publishContentOnExperimentVariants(user, running);

        return running;

    }

    private void publishContentOnExperimentVariants(final User user,
            final Experiment runningExperiment)
            throws DotDataException, DotSecurityException {
        DotPreconditions.isTrue(runningExperiment.status().equals(RUNNING),
                "Experiment needs to be RUNNING");

        final List<Contentlet> contentByVariants = contentletAPI.getAllContentByVariants(user, false,
                runningExperiment.trafficProportion().variants().stream()
                        .map(ExperimentVariant::id).filter((id) -> !id.equals(DEFAULT_VARIANT.name()))
                        .toArray(String[]::new));

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

        DotPreconditions.isTrue(experimentFromFactory.status()==Status.RUNNING,()->
                        "Only RUNNING experiments can be ended", DotStateException.class);

        DotPreconditions.isTrue(experimentFromFactory.status()!= ENDED,
                ()-> "Cannot end an already ended Experiment.", DotStateException.class);

        DotPreconditions.isTrue(persistedExperimentOpt.get().scheduling().isPresent(),
                ()-> "Scheduling not valid.", DotStateException.class);

        final Scheduling endedScheduling = Scheduling.builder().from(persistedExperimentOpt.get()
                .scheduling().get()).endDate(Instant.now().plus(1, ChronoUnit.MINUTES))
                .build();

        final Experiment ended = persistedExperimentOpt.get().withStatus(ENDED)
                .withScheduling(endedScheduling);
        return save(ended, user);
    }

    @WrapInTransaction
    @Override
    public Experiment addVariant(final String experimentId, final String variantDescription,
            final User user)
            throws DotDataException, DotSecurityException {

        final Experiment persistedExperiment = find(experimentId, user)
                .orElseThrow(()->new DoesNotExistException("Experiment with provided id not found"));

        ExperimentVariant experimentVariant = createExperimentVariant(
                persistedExperiment, variantDescription);

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

    private ExperimentVariant createExperimentVariant(final Experiment experiment, final String variantDescription)
            throws DotDataException {

        final String experimentId = experiment.getIdentifier();
        final String variantName = getVariantName(experimentId);

        variantAPI.save(Variant.builder().name(variantName)
                .description(Optional.of(variantDescription)).build());

        APILocator.getMultiTreeAPI().copyVariantForPage(experiment.pageId(),
                DEFAULT_VARIANT.name(), variantName);

        final Contentlet pageContentlet = contentletAPI
                .findContentletByIdentifierAnyLanguage(experiment.pageId(), false);

        final HTMLPageAsset page = APILocator.getHTMLPageAssetAPI().fromContentlet(pageContentlet);

        final ExperimentVariant experimentVariant = ExperimentVariant.builder().id(variantName)
                .description(variantDescription).weight(0)
                .url(page.getURI()+"?variantName="+variantName)
                .build();
        return experimentVariant;
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
        final Experiment persistedExperiment = find(experimentId, user)
                .orElseThrow(()->new DoesNotExistException("Experiment with provided id not found"));

        DotPreconditions.isTrue(variantName!= null &&
                variantName.contains(shortyIdAPI.shortify(experimentId)), ()->"Invalid Variant provided",
                IllegalArgumentException.class);

        final Variant toDelete = variantAPI.get(variantName)
                .orElseThrow(()->new DoesNotExistException("Provided Variant not found"));

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

    @CloseDBIfOpened
    @Override
    public List<Experiment> getRunningExperiments() throws DotDataException {
        return FactoryLocator.getExperimentsFactory().list(
                ExperimentFilter.builder().statuses(set(Status.RUNNING)).build()
        );
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
        return !APILocator.getExperimentsAPI().getRunningExperiments().isEmpty();
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
        Scheduling toReturn = scheduling;
        final Instant NOW = Instant.now();

        if(scheduling.startDate().isPresent() && scheduling.endDate().isEmpty()) {
            DotPreconditions.checkState(scheduling.startDate().get().isAfter(NOW),
                    "Invalid Scheduling. Start date is in the past");

            toReturn = scheduling.withEndDate(scheduling.startDate().get()
                    .plus(EXPERIMENTS_MAX_DURATION.get(), ChronoUnit.DAYS));
        } else if(scheduling.startDate().isEmpty() && scheduling.endDate().isPresent()) {
            DotPreconditions.checkState(scheduling.endDate().get().isAfter(NOW),
                    "Invalid Scheduling. End date is in the past");
            DotPreconditions.checkState(
                    Instant.now().plus(EXPERIMENTS_MAX_DURATION.get(), ChronoUnit.DAYS)
                            .isAfter(scheduling.endDate().get()),
                    "Experiment duration must be less than "
                            + EXPERIMENTS_MAX_DURATION.get() +" days. ");

            toReturn = scheduling.withStartDate(Instant.now());
        } else {
            DotPreconditions.checkState(scheduling.startDate().get().isAfter(NOW),
                    "Invalid Scheduling. Start date is in the past");

            DotPreconditions.checkState(scheduling.endDate().get().isAfter(NOW),
                    "Invalid Scheduling. End date is in the past");

            DotPreconditions.checkState(scheduling.endDate().get().isAfter(scheduling.startDate().get()),
                    "Invalid Scheduling. End date must be after the start date");

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
