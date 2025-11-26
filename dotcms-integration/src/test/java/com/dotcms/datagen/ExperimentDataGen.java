package com.dotcms.datagen;

import com.dotcms.analytics.metrics.AbstractCondition.Operator;
import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Experiment.Builder;
import com.dotcms.experiments.model.GoalFactory;
import com.dotcms.experiments.model.Goals;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.experiments.model.TargetingCondition;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.bytebuddy.utility.RandomString; 

public class ExperimentDataGen  extends AbstractDataGen<Experiment> {

    final Lazy<RandomString> randomString = Lazy.of(() -> new RandomString());
    private String name;
    private String description;
    private HTMLPageAsset experimentPage;
    private List<Variant> variants = new ArrayList<>();
    private Float trafficAllocation = 100f;
    private User user = APILocator.systemUser();
    private List<TargetingCondition> targetingConditions = new ArrayList<>();
    private Scheduling scheduling;

    private Status status = Status.DRAFT;

    private Goals goal;

    public ExperimentDataGen name(final String name){
        this.name = name;
        return this;
    }

    public ExperimentDataGen description(final String description){
        this.description = description;
        return this;
    }

    public ExperimentDataGen page(final HTMLPageAsset page){
        this.experimentPage = page;
        return this;
    }

    public ExperimentDataGen addVariant(final String description){
        this.variants.add(Variant.builder().name(description).description(Optional.of(description)).build());
        return this;
    }

    public ExperimentDataGen trafficAllocation(final float trafficAllocation){
        this.trafficAllocation = trafficAllocation;
        return this;
    }

    public ExperimentDataGen addTargetingConditions(final TargetingCondition targetingCondition) {
        this.targetingConditions.add(targetingCondition);
        return this;
    }

    public ExperimentDataGen status(final Status status) {
        this.status = status;
        return this;
    }

    @Override
    public Experiment next() {
        final String innerName = UtilMethods.isSet(name) ? name : getRandomName();
        final String innerDescription = UtilMethods.isSet(description) ? description : getRandomDescription();

        final HTMLPageAsset page = UtilMethods.isSet(experimentPage) ? experimentPage : createPage();

        final Goals innerGoal = UtilMethods.isSet(goal) ? goal : createDefaultGoal(page);

        final Builder experimentBuilder = Experiment.builder()
                .name(innerName)
                .description(innerDescription)
                .createdBy(user.getUserId())
                .lastModifiedBy(user.getUserId())
                .pageId(page.getIdentifier())
                .goals(innerGoal)
                .trafficAllocation(trafficAllocation)
                .status(status);

        return UtilMethods.isSet(scheduling) ? experimentBuilder.scheduling(scheduling).build() :
                experimentBuilder.build();
    }

    private static Goals createDefaultGoal(HTMLPageAsset page) {
        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(Condition.builder()
                        .parameter("url")
                        .value(page.getPageUrl())
                        .operator(Operator.EQUALS)
                        .build())
                .build();

        return Goals.builder().primary(GoalFactory.create(metric)).build();
    }

    private HTMLPageAsset createPage() {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();
        return APILocator.getHTMLPageAssetAPI().fromContentlet(HTMLPageDataGen.publish(page));
    }

    private String getRandomName() {
        return "ExperimentName-" + randomString.get().nextString();
    }

    private String getRandomDescription() {
        return "ExperimentDescription-" + randomString.get().nextString();
    }

    @Override
    public Experiment persist(final Experiment experiment) {
        try {
            final ExperimentsAPI experimentsAPI = APILocator.getExperimentsAPI();
            Experiment experimentSaved = experimentsAPI.save(experiment, APILocator.systemUser());

            if (UtilMethods.isSet(targetingConditions)) {
                final Experiment experimentWithTargeting = Experiment.builder()
                    .from(experimentSaved)
                    .targetingConditions(targetingConditions)
                    .build();
                experimentSaved = experimentsAPI.save(experimentWithTargeting, user);
            }

            if (!UtilMethods.isSet(variants)) {
                variants.add(new VariantDataGen().nextPersisted());
            }

            for (Variant variant : variants) {
                experimentsAPI.addVariant(experimentSaved.getIdentifier(), variant.name(), user);
            }

            return experimentsAPI.find(experimentSaved.id().get(), APILocator.systemUser()).get();
        } catch (DotSecurityException | DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    public static Experiment start(final Experiment runningExperiment) {
        try {
            return APILocator.getExperimentsAPI().start(runningExperiment.getIdentifier(), APILocator.systemUser());
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public static void end(final Experiment runningExperiment) {
        try {
            APILocator.getExperimentsAPI().end(runningExperiment.getIdentifier(), APILocator.systemUser());
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public ExperimentDataGen scheduling(final Scheduling scheduling) {
        this.scheduling = scheduling;
        return this;
    }

    public ExperimentDataGen addGoal(final Goals goal) {
        this.goal = goal;
        return this;
    }

    public Experiment nextPersistedAndStart() {
        final Experiment experiment = nextPersisted();

        try {
            return APILocator.getExperimentsAPI().start(experiment.id().get(), APILocator.systemUser());
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }
}
