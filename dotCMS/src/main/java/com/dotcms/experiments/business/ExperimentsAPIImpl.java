package com.dotcms.experiments.business;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

public class ExperimentsAPIImpl implements ExperimentsAPI {

    final ExperimentsFactory factory = FactoryLocator.getExperimentsFactory();
    final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    final ContentletAPI contentletAPI = APILocator.getContentletAPI();

    @Override
    @WrapInTransaction
    public Experiment save(final Experiment experiment, final User user) throws
            DotSecurityException, DotDataException {

        final Contentlet pageAsContent = contentletAPI
                .findContentletByIdentifierAnyLanguage(experiment.pageId(), false);

        DotPreconditions.isTrue(pageAsContent!=null
                && UtilMethods.isSet(pageAsContent.getIdentifier()),
                DotStateException.class, ()->"Invalid Page provided");

        if(!permissionAPI.doesUserHavePermission(pageAsContent, PermissionLevel.EDIT.getType(), user)) {
            Logger.error(this, "You don't have permission to save the Experiment.");
            throw new DotSecurityException("You don't have permission to save the Experiment.");
        }

        Experiment.Builder builder = Experiment.builder().from(experiment);

        if(experiment.id().isEmpty()) {
            builder.id(UUIDGenerator.generateUuid());
        }

        builder.modDate(Instant.now());
        builder.lastModifiedBy(user.getUserId());

        final Experiment experimentToSave = builder.build();

        return factory.save(experimentToSave);
    }

    @Override
    public Optional<Experiment> find(final String id, final User user)
            throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(id)) {
            return Optional.empty();
        }

        final Optional<Experiment> experiment =  factory.find(id);

        if(experiment.isPresent()) {
            final Contentlet pageAsContent = contentletAPI
                    .findContentletByIdentifierAnyLanguage(experiment.get().pageId(), false);

            if (!permissionAPI.doesUserHavePermission(pageAsContent, PermissionLevel.EDIT.getType(),
                    user)) {
                Logger.error(this, "You don't have permission to get the Experiment.");
                throw new DotSecurityException("You don't have permission to get the Experiment.");
            }
        }

        return experiment;
    }

    @Override
    public Experiment archive(final String id, final User user)
            throws DotDataException, DotSecurityException {
        DotPreconditions.checkArgument(UtilMethods.isSet(id), "id must be provided.");

        final Optional<Experiment> persistedExperiment =  factory.find(id);

        if(persistedExperiment.isPresent()) {
            final Contentlet pageAsContent = contentletAPI
                    .findContentletByIdentifierAnyLanguage(persistedExperiment.get().pageId(), false);

            if (!permissionAPI.doesUserHavePermission(pageAsContent, PermissionLevel.EDIT.getType(),
                    user)) {
                Logger.error(this, "You don't have permission to get the Experiment.");
                throw new DotSecurityException("You don't have permission to get the Experiment.");
            }

            if(persistedExperiment.get().status()!= Status.ENDED) {
                throw new DotStateException("Only ended experiments can be archived");
            }

            final Experiment archived = persistedExperiment.get().withArchived(true);
            return factory.save(archived);
        } else {
            throw new NotFoundInDbException("Experiment with provided id not found");
        }

    }

    @Override
    public void delete(final String id, final User user)
            throws DotDataException, DotSecurityException {
        DotPreconditions.checkArgument(UtilMethods.isSet(id), "id must be provided.");

        final Optional<Experiment> persistedExperiment =  factory.find(id);

        if(persistedExperiment.isPresent()) {
            final Contentlet pageAsContent = contentletAPI
                    .findContentletByIdentifierAnyLanguage(persistedExperiment.get().pageId(), false);

            if (!permissionAPI.doesUserHavePermission(pageAsContent, PermissionLevel.EDIT.getType(),
                    user)) {
                Logger.error(this, "You don't have permission to get the Experiment.");
                throw new DotSecurityException("You don't have permission to get the Experiment.");
            }

            if(persistedExperiment.get().status()!= Status.DRAFT) {
                throw new DotStateException("Only draft experiments can be deleted");
            }

            factory.delete(persistedExperiment.get());
        } else {
            throw new NotFoundInDbException("Experiment with provided id not found");
        }
    }
}
