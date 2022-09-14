package com.dotcms.experiments.business;

import static com.dotcms.experiments.model.AbstractExperiment.Status.DRAFT;
import static com.dotcms.experiments.model.AbstractExperiment.Status.ENDED;

import com.dotcms.analytics.metrics.MetricsUtil;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.LicenseValiditySupplier;
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
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ExperimentsAPIImpl implements ExperimentsAPI {

    final ExperimentsFactory factory = FactoryLocator.getExperimentsFactory();
    final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    final ContentletAPI contentletAPI = APILocator.getContentletAPI();

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

        final Experiment experimentToSave = builder.build();

        return factory.save(experimentToSave);
    }

    @CloseDBIfOpened
    @Override
    public Optional<Experiment> find(final String id, final User user)
            throws DotDataException, DotSecurityException {
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);
        DotPreconditions.checkArgument(UtilMethods.isSet(id), "Experiment Id is required");

        final Optional<Experiment> experiment =  factory.find(id);

        if(experiment.isPresent()) {
            validatePermissions(user, experiment.get(),
                    "You don't have permission to get the Experiment. "
                            + "Experiment Id: " + experiment.get().id());
        }

        return experiment;
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
        return factory.save(archived);

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

        final Experiment running = experimentToStart.withStatus(Status.RUNNING);
        return factory.save(running);

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
        return factory.save(ended);
    }

    private Scheduling startNowScheduling(final Experiment experiment) {
        // Setting "now" with an additional minute to avoid failing validation
        final Instant now = Instant.now().plus(1, ChronoUnit.MINUTES);
        return Scheduling.builder().startDate(now)
                .endDate(now.plus(EXPERIMENT_MAX_DURATION.get(), ChronoUnit.DAYS))
                .build();
    }

    private boolean hasAtLeastOneVariant(final Experiment experiment) {
        return !experiment.trafficProportion().variantsPercentagesMap().keySet().isEmpty();
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
                    .plus(EXPERIMENT_MAX_DURATION.get(), ChronoUnit.DAYS));
        } else if(scheduling.startDate().isEmpty() && scheduling.endDate().isPresent()) {
            DotPreconditions.checkState(scheduling.endDate().get().isAfter(NOW),
                    "Invalid Scheduling. End date is in the past");
            DotPreconditions.checkState(
                    Instant.now().plus(EXPERIMENT_MAX_DURATION.get(), ChronoUnit.DAYS)
                            .isAfter(scheduling.endDate().get()),
                    "Experiment duration must be less than "
                            + EXPERIMENT_MAX_DURATION.get() +" days. ");

            toReturn = scheduling.withStartDate(Instant.now());
        } else {
            DotPreconditions.checkState(scheduling.startDate().get().isAfter(NOW),
                    "Invalid Scheduling. Start date is in the past");

            DotPreconditions.checkState(scheduling.endDate().get().isAfter(NOW),
                    "Invalid Scheduling. End date is in the past");

            DotPreconditions.checkState(scheduling.endDate().get().isAfter(scheduling.startDate().get()),
                    "Invalid Scheduling. End date must be after the start date");

            DotPreconditions.checkState(Duration.between(scheduling.startDate().get(),
                            scheduling.endDate().get()).toDays() <= EXPERIMENT_MAX_DURATION.get(),
                    "Experiment duration must be less than "
                            + EXPERIMENT_MAX_DURATION.get() +" days. ");
        }
        return toReturn;
    }

    private boolean hasValidLicense(){
        return (licenseValiditySupplierSupplier.hasValidLicense());
    }
}
