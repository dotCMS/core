package com.dotcms.workflow.helper;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * System Action Mapping handler to merge the remotes mapping with the existing ones (the locals)
 * Works for content type and schemes
 */
public class SystemActionMappingsHandlerMerger {

    private final WorkflowAPI workflowAPI;

    public SystemActionMappingsHandlerMerger(final WorkflowAPI workflowAPI) {
        this.workflowAPI = workflowAPI;
    }

    /**
     * Merge the system action for the local scheme.
     * @param contentType            {@link ContentType}
     * @param remoteSystemActionMappings  {@link List}
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @WrapInTransaction
    public void mergeSystemActions(final ContentType contentType,
                                   final List<SystemActionWorkflowActionMapping> remoteSystemActionMappings)
            throws DotDataException, DotSecurityException {

        if (UtilMethods.isSet(remoteSystemActionMappings)) {

            final List<SystemActionWorkflowActionMapping> localSystemActionMappings =
                    workflowAPI.findSystemActionsByContentType(contentType, APILocator.systemUser());
            final Set<String> remoteSystemMappingIdentifiers = new HashSet<>();
            // Saves the mappings coming from remote.
            this.saveRemoteSystemActionMappings(contentType,
                    remoteSystemActionMappings, remoteSystemMappingIdentifiers);

            // now remove the local mappings that are not on the bundle
            this.deleteUnusedPreviousMappings(localSystemActionMappings, remoteSystemMappingIdentifiers);
        }
    }

    private void saveRemoteSystemActionMappings(final ContentType localContentType,
                                                final List<SystemActionWorkflowActionMapping> remoteSystemActionMappings,
                                                final Set<String> remoteSystemMappingIdentifiers)
            throws DotDataException {

        for (final SystemActionWorkflowActionMapping remoteMapping : remoteSystemActionMappings) {

            if (remoteMapping.isOwnerContentType()) {

                final ContentType remoteContentType = (ContentType)remoteMapping.getOwner();

                if (remoteContentType.variable().equals(localContentType.variable())) {

                    final WorkflowAPI.SystemAction systemAction   = remoteMapping.getSystemAction();
                    if (UtilMethods.isSet(remoteMapping.getWorkflowAction()) && UtilMethods.isSet(remoteMapping.getWorkflowAction().getId())) {

                        final Optional<WorkflowAction> workflowAction = this
                                .findWorkflowAction(remoteMapping.getWorkflowAction().getId());
                        if (workflowAction.isPresent()) {

                            try {
                                
                                final SystemActionWorkflowActionMapping localSavedMapping =
                                        workflowAPI.mapSystemActionToWorkflowActionForContentType(
                                                systemAction, workflowAction.get(),
                                                localContentType);

                                remoteSystemMappingIdentifiers.add(null != localSavedMapping ?
                                        localSavedMapping.getIdentifier()
                                        : remoteMapping.getIdentifier());
                                Logger.info(this, "The mapping for systemAction: " + systemAction
                                        + ", workflowAction: " + workflowAction
                                        + ", and content type: " + localContentType.variable()
                                        + " has been saved");
                            } catch (DotDataException | IllegalArgumentException e) { // we just catch but do not propagate the exception in order to not break the transaction

                                Logger.error(this, "The mapping for systemAction: " + systemAction
                                        + ", workflowAction: " + workflowAction
                                        + ", and content type: " + localContentType.variable()
                                        + " could not be saved, msg: " + e.getMessage(), e);
                            }
                        } else {

                            Logger.warn(this, "The mapping for systemAction: " + systemAction
                                    + ", workflowAction: " + workflowAction
                                    + ", and content type: " + localContentType.variable()
                                    + " was not saved because the workflow action does not exists locally");
                        }
                    } else {

                        Logger.info(this, "The workflow on the " + remoteContentType
                                + " is null or the id is null");
                    }
                } else {

                    Logger.info(this, "The remote mapping: " + remoteMapping.getIdentifier() +
                            ", is own by different content type: " + localContentType.variable() + ", instead of: " +
                            remoteContentType.variable());
                }
            } else {

                Logger.info(this, "The remote mapping: " + remoteMapping.getIdentifier() +
                        ", the owner is not an scheme");
            }
        }
    }

    /**
     * Merge the system action for the local scheme.
     * @param scheme                      {@link WorkflowScheme}
     * @param remoteSystemActionMappings  {@link List}
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @WrapInTransaction
    public void mergeSystemActions(final WorkflowScheme scheme,
                                   final List<SystemActionWorkflowActionMapping> remoteSystemActionMappings)
            throws DotDataException, DotSecurityException {

        if (UtilMethods.isSet(remoteSystemActionMappings)) {

            final List<SystemActionWorkflowActionMapping> localSystemActionMappings =
                    this.workflowAPI.findSystemActionsByScheme(scheme, APILocator.systemUser());
            final Set<String> remoteSystemMappingIdentifiers = new HashSet<>();

            // Saves the mappings coming from remote.
            this.saveRemoteSystemActionMappings(scheme,
                    remoteSystemActionMappings, remoteSystemMappingIdentifiers);

            // now remove the local mappings that are not on the bundle
            this.deleteUnusedPreviousMappings(localSystemActionMappings, remoteSystemMappingIdentifiers);
        }
    }

    private void saveRemoteSystemActionMappings(final WorkflowScheme localScheme,
                                                final List<SystemActionWorkflowActionMapping> remoteSystemActionMappings,
                                                final Set<String> remoteSystemMappingIdentifiers)
            throws DotDataException {

        for (final SystemActionWorkflowActionMapping remoteMapping : remoteSystemActionMappings) {

            if (remoteMapping.isOwnerScheme()) {

                final WorkflowScheme mappingScheme = (WorkflowScheme)remoteMapping.getOwner();

                if (mappingScheme.equals(localScheme)) {

                    final WorkflowAPI.SystemAction systemAction   = remoteMapping.getSystemAction();
                    if (UtilMethods.isSet(remoteMapping.getWorkflowAction()) && UtilMethods.isSet(remoteMapping.getWorkflowAction().getId())) {
                        final Optional<WorkflowAction> workflowAction = this
                                .findWorkflowAction(remoteMapping.getWorkflowAction().getId());
                        if (workflowAction.isPresent()) {

                            try {

                                final SystemActionWorkflowActionMapping localSavedMapping =
                                        workflowAPI
                                                .mapSystemActionToWorkflowActionForWorkflowScheme(
                                                        systemAction, workflowAction.get(),
                                                        localScheme);

                                remoteSystemMappingIdentifiers.add(null != localSavedMapping ?
                                        localSavedMapping.getIdentifier()
                                        : remoteMapping.getIdentifier());
                                Logger.info(this, "The mapping for systemAction: " + systemAction
                                        + ", workflowAction: " + workflowAction
                                        + ", and scheme: " + mappingScheme.getId()
                                        + " has been saved");
                            } catch (DotDataException | IllegalArgumentException  e) { // we just catch but do not propagate the exception in order to not break the transaction

                                Logger.error(this, "The mapping for systemAction: " + systemAction
                                    + ", workflowAction: " + workflowAction
                                    + ", and scheme: " + mappingScheme.getId()
                                    + " could not be saved, msg: " + e.getMessage(), e);
                            }
                        } else {

                            Logger.warn(this, "The mapping for systemAction: " + systemAction
                                    + ", workflowAction: " + workflowAction
                                    + ", and scheme: " + mappingScheme.getId()
                                    + " was not saved because the workflow action does not exists locally");
                        }
                    } else {

                        Logger.info(this, "The workflow on the " + remoteMapping
                                + " is null or the id is null");
                    }
                } else {

                    Logger.info(this, "The remote mapping: " + remoteMapping.getIdentifier() +
                            ", is own by different scheme: " + localScheme.getId() + ", instead of: " +
                            mappingScheme.getId());
                }
            } else {

                Logger.info(this, "The remote mapping: " + remoteMapping.getIdentifier() +
                        ", the owner is not an scheme");
            }
        }
    }

    private void deleteUnusedPreviousMappings (final List<SystemActionWorkflowActionMapping> localSystemActionMappings,
                                               final Set<String> remoteSystemMappingIdentifiers) throws DotDataException {
        if (UtilMethods.isSet(localSystemActionMappings)) {
            for (final SystemActionWorkflowActionMapping previousLocalMappings : localSystemActionMappings) {

                // if the previous id db, is not on the latest saved it means it is not on the bundle and must be removed.
                if (!remoteSystemMappingIdentifiers.contains(previousLocalMappings.getIdentifier())) {

                    workflowAPI.deleteSystemAction(previousLocalMappings);
                }
            }
        }
    }

    private Optional<WorkflowAction> findWorkflowAction (final String workflowId) {

        try {
            return Optional.ofNullable(this.workflowAPI.findAction(workflowId, APILocator.systemUser()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
