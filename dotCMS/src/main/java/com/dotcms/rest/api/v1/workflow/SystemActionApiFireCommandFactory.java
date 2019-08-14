package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction.*;

/**
 * This Factory provides the {@link SystemActionApiFireCommand} for a {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}
 * @author jsanca
 */
public class SystemActionApiFireCommandFactory {

    private static class SingletonHolder {
        private static final SystemActionApiFireCommandFactory INSTANCE = new SystemActionApiFireCommandFactory();
    }

    public static SystemActionApiFireCommandFactory getInstance() {
        return SystemActionApiFireCommandFactory.SingletonHolder.INSTANCE;
    }

    private final ContentletAPI  contentletAPI  = APILocator.getContentletAPI();
    private final WorkflowAPI    workflowAPI    = APILocator.getWorkflowAPI();

    private final Map<WorkflowAPI.SystemAction, Function<WorkflowAction, Boolean>> systemActionHasActionletHandlerMap =
            new ImmutableMap.Builder<WorkflowAPI.SystemAction, Function<WorkflowAction, Boolean>>()
                    .put(NEW,       this.workflowAPI::hasSaveActionlet)
                    .put(EDIT,      this.workflowAPI::hasSaveActionlet)
                    .put(PUBLISH,   this.workflowAPI::hasPublishActionlet)
                    .put(UNPUBLISH, this.workflowAPI::hasUnpublishActionlet)
                    .put(ARCHIVE,   this.workflowAPI::hasArchiveActionlet)
                    .put(UNARCHIVE, this.workflowAPI::hasUnarchiveActionlet)
                    .put(DELETE,    this.workflowAPI::hasDeleteActionlet)
                    .put(DESTROY,   this.workflowAPI::hasDestroyActionlet)
                    .build();

    private final Map<WorkflowAPI.SystemAction, SystemActionApiFireCommand> commandMap = new ConcurrentHashMap<>();
    {

        final SystemActionApiFireCommand saveFireCommand = new SaveSystemActionApiFireCommandImpl();
        this.commandMap.put(NEW,  saveFireCommand);
        /*this.commandMap.put(EDIT, saveFireCommand);
        this.commandMap.put(PUBLISH,   new PublishSystemActionApiFireCommandImpl());
        this.commandMap.put(UNPUBLISH, new UnPublishSystemActionApiFireCommandImpl());
        this.commandMap.put(ARCHIVE,   new ArchiveSystemActionApiFireCommandImpl());
        this.commandMap.put(UNARCHIVE, new UnArchiveSystemActionApiFireCommandImpl());
        this.commandMap.put(DELETE,    new DeleteSystemActionApiFireCommandImpl());
        this.commandMap.put(DESTROY,   new DestroySystemActionApiFireCommandImpl());*/
    }

    /**
     * Adds a new command
     * @param systemAction {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}
     * @param command {@link SystemActionApiFireCommand}
     */
    public void subscribe (final WorkflowAPI.SystemAction systemAction, final SystemActionApiFireCommand command) {

        if (null != systemAction && null != command) {

            this.commandMap.put(systemAction, command);
        }
    }


    /**
     * If the workflow action has an actionlet that satisfied the system action returns an empty {@link Optional} empty.
     * But if the workflow action does not have any actionlet associated to the system action, returns a SystemActionApiFireCommand
     * in order to do the api call and executes the workflow action, for instance:
     *
     * If the system action is NEW or EDIT and the workflow action does not have a Save Content action, the command will do the checkin and fire the workflow action in a workflow.
     * Otherwise returns an empty optional, in that case the workflow fire should be run normally.
     *
     * @param workflowAction {@link WorkflowAction}
     * @param systemAction   {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}
     * @return Optional of SystemActionApiFireCommand
     */
    public Optional<SystemActionApiFireCommand> get(final WorkflowAction workflowAction, final WorkflowAPI.SystemAction systemAction) {

        final boolean hasActionlet = this.systemActionHasActionletHandlerMap.containsKey(systemAction)?
                this.systemActionHasActionletHandlerMap.get(systemAction).apply(workflowAction):false;

        return hasActionlet?Optional.empty():Optional.ofNullable(this.commandMap.get(systemAction));
    }

    /**
     * Implements a {@link SystemActionApiFireCommand} that does a checkin to cover the NEW and EDIT system action
     */
    private class SaveSystemActionApiFireCommandImpl implements SystemActionApiFireCommand {

        @Override
        public Contentlet fire(final Contentlet contentlet, final ContentletDependencies dependencies)
                throws DotDataException, DotSecurityException {

            Logger.info(this, "The contentlet : " + contentlet.getTitle()
                    + ", was fired by default action: " + dependencies.getWorkflowActionId() +
                    ", however this action has not any save content actionlet, so the checkin is being triggered as part of the request");

            if(UtilMethods.isSet(dependencies.getWorkflowActionId())){
                contentlet.setActionId(dependencies.getWorkflowActionId());
            }

            if(UtilMethods.isSet(dependencies.getWorkflowActionComments())){
                contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, dependencies.getWorkflowActionComments());
            }

            if(UtilMethods.isSet(dependencies.getWorkflowAssignKey())){
                contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, dependencies.getWorkflowAssignKey());
            }

            return contentletAPI.checkin(contentlet, dependencies);
        }
    }
}
