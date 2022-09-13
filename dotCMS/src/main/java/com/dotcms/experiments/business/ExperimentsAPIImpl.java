package com.dotcms.experiments.business;

import com.dotcms.analytics.metrics.MetricsUtil;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.time.Instant;
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

        if(persistedExperiment.isPresent()) {
            validatePermissions(user, persistedExperiment.get(),
                    "You don't have permission to archive the Experiment. "
                            + "Experiment Id: " + persistedExperiment.get().id());

            if(persistedExperiment.get().status()!= Status.ENDED) {
                throw new DotStateException("Only ended experiments can be archived");
            }

            final Experiment archived = persistedExperiment.get().withStatus(Status.ARCHIVED);
            return factory.save(archived);
        } else {
            throw new NotFoundInDbException("Experiment with provided id not found");
        }

    }

    @Override
    @WrapInTransaction
    public void delete(final String id, final User user)
            throws DotDataException, DotSecurityException {
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);
        DotPreconditions.checkArgument(UtilMethods.isSet(id), "id must be provided.");

        final Optional<Experiment> persistedExperiment =  find(id, user);

        if(persistedExperiment.isPresent()) {
            validatePermissions(user, persistedExperiment.get(),
                    "You don't have permission to delete the Experiment. "
                            + "Experiment Id: " + persistedExperiment.get().id());

            if(persistedExperiment.get().status()!= Status.DRAFT) {
                throw new DotStateException("Only draft experiments can be deleted");
            }

            factory.delete(persistedExperiment.get());
        } else {
            throw new NotFoundInDbException("Experiment with provided id not found");
        }
    }

    @Override
    @CloseDBIfOpened
    public List<Experiment> list(ExperimentFilter filter, User user) throws DotDataException {
        DotPreconditions.isTrue(hasValidLicense(), InvalidLicenseException.class,
                invalidLicenseMessageSupplier);
        return factory.list(filter);
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

    private boolean hasValidLicense(){
        return (licenseValiditySupplierSupplier.hasValidLicense());
    }
}
