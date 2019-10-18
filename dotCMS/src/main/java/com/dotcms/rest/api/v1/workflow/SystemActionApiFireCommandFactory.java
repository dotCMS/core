package com.dotcms.rest.api.v1.workflow;

import static com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction.ARCHIVE;
import static com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction.DELETE;
import static com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction.DESTROY;
import static com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction.EDIT;
import static com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction.NEW;
import static com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction.PUBLISH;
import static com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction.UNARCHIVE;
import static com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction.UNPUBLISH;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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

    private final Map<WorkflowAPI.SystemAction, Function<Tuple2<WorkflowAction, Boolean>, Boolean>> systemActionHasActionletHandlerMap =
            new ImmutableMap.Builder<WorkflowAPI.SystemAction, Function<Tuple2<WorkflowAction, Boolean>, Boolean>> ()
                    .put(NEW,       (final Tuple2<WorkflowAction, Boolean> params) -> this.workflowAPI.hasSaveActionlet(params._1))
                    .put(EDIT,      (final Tuple2<WorkflowAction, Boolean> params) -> this.workflowAPI.hasSaveActionlet(params._1))
                    .put(PUBLISH,   this::hasPublishValid)
                    .put(UNPUBLISH, (final Tuple2<WorkflowAction, Boolean> params) -> this.workflowAPI.hasUnpublishActionlet(params._1))
                    .put(ARCHIVE,   (final Tuple2<WorkflowAction, Boolean> params) -> this.workflowAPI.hasArchiveActionlet(params._1))
                    .put(UNARCHIVE, (final Tuple2<WorkflowAction, Boolean> params) -> this.workflowAPI.hasUnarchiveActionlet(params._1))
                    .put(DELETE,    (final Tuple2<WorkflowAction, Boolean> params) -> this.workflowAPI.hasDeleteActionlet(params._1))
                    .put(DESTROY,   (final Tuple2<WorkflowAction, Boolean> params) -> this.workflowAPI.hasDestroyActionlet(params._1))
                    .build();

    private final Map<WorkflowAPI.SystemAction, SystemActionApiFireCommand> commandMap = new ConcurrentHashMap<>();
    {

        final SystemActionApiFireCommand saveFireCommand = new SaveSystemActionApiFireCommandImpl();
        this.commandMap.put(NEW,       saveFireCommand);
        this.commandMap.put(EDIT,      saveFireCommand);
        this.commandMap.put(PUBLISH,   new PublishSystemActionApiFireCommandImpl());
        this.commandMap.put(UNPUBLISH, new UnpublishSystemActionApiFireCommandImpl());
        this.commandMap.put(ARCHIVE,   new ArchiveSystemActionApiFireCommandImpl());
        this.commandMap.put(UNARCHIVE, new UnArchiveSystemActionApiFireCommandImpl());
        this.commandMap.put(DELETE,    new DeleteSystemActionApiFireCommandImpl());
        this.commandMap.put(DESTROY,   new DestroySystemActionApiFireCommandImpl());
    }

    private boolean hasPublishValid (final Tuple2<WorkflowAction, Boolean> params) {

        final WorkflowAction action   = params._1;
        final boolean        needSave = params._2;

        return needSave? // if needs Save has to have save and publish actionlet
                this.workflowAPI.hasSaveActionlet(action) && this.workflowAPI.hasPublishActionlet(action):
                this.workflowAPI.hasPublishActionlet(action);
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
     * @param workflowAction    {@link WorkflowAction}
     * @param needSave         {@link Boolean} true if needs to save the contentlet (b/c it has body and changes to safe)
     * @param systemAction     {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}
     * @return Optional of SystemActionApiFireCommand
     */
    public Optional<SystemActionApiFireCommand> get(final WorkflowAction workflowAction, final boolean needSave,
                                                    final WorkflowAPI.SystemAction systemAction) {

        final boolean hasActionlet = this.systemActionHasActionletHandlerMap.containsKey(systemAction)?
                this.systemActionHasActionletHandlerMap.get(systemAction).apply(Tuple.of(workflowAction, needSave)):false;

        return hasActionlet?Optional.empty():Optional.ofNullable(this.commandMap.get(systemAction));
    }

    /**
     * Gets the SystemActionApiFireCommand associated to the {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}
     * This method returns just the command to call it as a fallback
     * @param systemAction {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}
     * @return Optional of SystemActionApiFireCommand
     */
    public Optional<SystemActionApiFireCommand> get(final WorkflowAPI.SystemAction systemAction) {

        return Optional.ofNullable(this.commandMap.get(systemAction));
    }

    /**
     * Implements a {@link SystemActionApiFireCommand} that does a checkin to cover the {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction#NEW} and {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction#EDIT} system action
     */
    private class SaveSystemActionApiFireCommandImpl implements SystemActionApiFireCommand {

        @Override
        public Contentlet fire(final Contentlet contentlet, final boolean needSave, final ContentletDependencies dependencies)
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

            final Contentlet checkoutContentlet = new SaveContentActionlet()
                    .checkout(contentlet, dependencies.getModUser());

            return  contentletAPI.checkin(checkoutContentlet, dependencies);
        }
    }

    ////////////////////////

    /**
     * Implements a {@link SystemActionApiFireCommand} that does an publish to cover the {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction#PUBLISH} system action
     */
    private class PublishSystemActionApiFireCommandImpl implements SystemActionApiFireCommand {

        @WrapInTransaction
        @Override
        public Contentlet fire(final Contentlet contentlet, final boolean needSave, final ContentletDependencies dependencies)
                throws DotDataException, DotSecurityException {

            Logger.info(this, "The contentlet : " + contentlet.getTitle()
                    + ", was fired by default action: " + dependencies.getWorkflowActionId() +
                    ", however this action has not any publish content actionlet, so the publish api call is being triggered as part of the request");

            final String actionId       = UtilMethods.isSet(dependencies.getWorkflowActionId())?
                    dependencies.getWorkflowActionId():contentlet.getActionId();
            final User   user           = dependencies.getModUser();

            if(UtilMethods.isSet(actionId)) {
                contentlet.setActionId(actionId);
            }

            if(UtilMethods.isSet(dependencies.getWorkflowActionComments())){
                contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, dependencies.getWorkflowActionComments());
            }

            if(UtilMethods.isSet(dependencies.getWorkflowAssignKey())){
                contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, dependencies.getWorkflowAssignKey());
            }

            if (needSave) {

                return firePublishWithSave(contentlet, dependencies, actionId, user);
            }

            Logger.info(this, "The contentlet : " + contentlet.getTitle()
                    + ", on the action id: " + actionId +
                    ", will do a publish");

            contentletAPI.publish(contentlet, user, dependencies.isRespectAnonymousPermissions());

            return contentlet;
        }

        private Contentlet firePublishWithSave(final Contentlet contentlet,
                                               final ContentletDependencies dependencies,
                                               final String actionId, final User user) throws DotDataException, DotSecurityException {

            final String disableWorkflow = Contentlet.DISABLE_WORKFLOW;
            final boolean hasPublishActionlet  = UtilMethods.isSet(actionId)?
                workflowAPI.findAction(actionId, user).hasPublishActionlet():false;

            // we do not want auto assign this checkin
            if (!hasPublishActionlet) {

                Logger.info(this, "The contentlet : " + contentlet.getTitle()
                        + ", on the action id: " + actionId +
                        ", does not have publish, and has changes, so a checkin will be fired without assign to any step");
                contentlet.setBoolProperty(disableWorkflow, true);
            }

            Logger.info(this, "The contentlet : " + contentlet.getTitle() + ", will do a checkin");
            final Contentlet checkoutContentlet = new SaveContentActionlet()
                    .checkout(contentlet, dependencies.getModUser());
            final Contentlet checkinContentlet = contentletAPI.checkin(checkoutContentlet,
                    user, dependencies.isRespectAnonymousPermissions());

            if (!hasPublishActionlet) {

                if (checkinContentlet.getMap().containsKey(disableWorkflow)) {
                    checkinContentlet.getMap().remove(disableWorkflow);
                }

                if (UtilMethods.isSet(actionId)) {
                    checkinContentlet.setActionId(actionId);
                }

                Logger.info(this, "The contentlet : " + contentlet.getTitle()
                        + ", on the action id: " + actionId +
                        ", was checkin but does not have publish, so a publish will be fired (could autoassign if needed)");

                contentletAPI.publish(checkinContentlet,
                        user, dependencies.isRespectAnonymousPermissions());
            }

            return checkinContentlet;
        }
    }

    //////////////////////////
    /**
     * Implements a {@link SystemActionApiFireCommand} that does an unpublish to cover the {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction#UNPUBLISH} system action
     */
    private class UnpublishSystemActionApiFireCommandImpl implements SystemActionApiFireCommand {

        @WrapInTransaction
        @Override
        public Contentlet fire(final Contentlet contentlet, final boolean needSave, final ContentletDependencies dependencies)
                throws DotDataException, DotSecurityException {

            Logger.info(this, "The contentlet : " + contentlet.getTitle()
                    + ", was fired by default action: " + dependencies.getWorkflowActionId() +
                    ", however this action has not any unpublish content actionlet, so the unpublish api call is being triggered as part of the request");

            final String actionId       = UtilMethods.isSet(dependencies.getWorkflowActionId())?
                    dependencies.getWorkflowActionId():contentlet.getActionId();
            final User   user           = dependencies.getModUser();

            if(UtilMethods.isSet(actionId)) {
                contentlet.setActionId(actionId);
            }

            if(UtilMethods.isSet(dependencies.getWorkflowActionComments())){
                contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, dependencies.getWorkflowActionComments());
            }

            if(UtilMethods.isSet(dependencies.getWorkflowAssignKey())){
                contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, dependencies.getWorkflowAssignKey());
            }

            Logger.info(this, "The contentlet : " + contentlet.getTitle()
                    + ", on the action id: " + actionId +
                    ", will do an unpublish");

            contentletAPI.unpublish(contentlet, user, dependencies.isRespectAnonymousPermissions());

            return contentlet;
        }
    }

    //////////////////////////
    /**
     * Implements a {@link SystemActionApiFireCommand} that does an archive to cover the {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction#ARCHIVE} system action
     */
    private class ArchiveSystemActionApiFireCommandImpl implements SystemActionApiFireCommand {

        @WrapInTransaction
        @Override
        public Contentlet fire(final Contentlet contentlet, final boolean needSave, final ContentletDependencies dependencies)
                throws DotDataException, DotSecurityException {

            Logger.info(this, "The contentlet : " + contentlet.getTitle()
                    + ", was fired by default action: " + dependencies.getWorkflowActionId() +
                    ", however this action has not any archive content actionlet, so the archive api call is being triggered as part of the request");

            final String actionId       = UtilMethods.isSet(dependencies.getWorkflowActionId())?
                    dependencies.getWorkflowActionId():contentlet.getActionId();
            final User   user           = dependencies.getModUser();

            if(UtilMethods.isSet(actionId)) {
                contentlet.setActionId(actionId);
            }

            if(UtilMethods.isSet(dependencies.getWorkflowActionComments())){
                contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, dependencies.getWorkflowActionComments());
            }

            if(UtilMethods.isSet(dependencies.getWorkflowAssignKey())){
                contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, dependencies.getWorkflowAssignKey());
            }

            Logger.info(this, "The contentlet : " + contentlet.getTitle()
                    + ", on the action id: " + actionId +
                    ", will do an archive");

            contentletAPI.archive(contentlet, user, dependencies.isRespectAnonymousPermissions());

            return contentlet;
        }
    }

    /////////////////

    /**
     * Implements a {@link SystemActionApiFireCommand} that does an unarchive to cover the {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction#UNARCHIVE} system action
     */
    private class UnArchiveSystemActionApiFireCommandImpl implements SystemActionApiFireCommand {

        @WrapInTransaction
        @Override
        public Contentlet fire(final Contentlet contentlet, final boolean needSave, final ContentletDependencies dependencies)
                throws DotDataException, DotSecurityException {

            Logger.info(this, "The contentlet : " + contentlet.getTitle()
                    + ", was fired by default action: " + dependencies.getWorkflowActionId() +
                    ", however this action has not any unarchive content actionlet, so the unarchive api call is being triggered as part of the request");

            final String actionId       = UtilMethods.isSet(dependencies.getWorkflowActionId())?
                    dependencies.getWorkflowActionId():contentlet.getActionId();
            final User   user           = dependencies.getModUser();

            if(UtilMethods.isSet(actionId)) {
                contentlet.setActionId(actionId);
            }

            if(UtilMethods.isSet(dependencies.getWorkflowActionComments())){
                contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, dependencies.getWorkflowActionComments());
            }

            if(UtilMethods.isSet(dependencies.getWorkflowAssignKey())){
                contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, dependencies.getWorkflowAssignKey());
            }

            Logger.info(this, "The contentlet : " + contentlet.getTitle()
                    + ", on the action id: " + actionId +
                    ", will do an unarchive");

            contentletAPI.unarchive(contentlet, user, dependencies.isRespectAnonymousPermissions());

            return contentlet;
        }
    }

    ///////////////////

    /**
     * Implements a {@link SystemActionApiFireCommand} that does a delete to cover the {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction#DELETE} system action
     */
    private class DeleteSystemActionApiFireCommandImpl implements SystemActionApiFireCommand {

        @WrapInTransaction
        @Override
        public Contentlet fire(final Contentlet contentlet, final boolean needSave, final ContentletDependencies dependencies)
                throws DotDataException, DotSecurityException {

            Logger.info(this, "The contentlet : " + contentlet.getTitle()
                    + ", was fired by default action: " + dependencies.getWorkflowActionId() +
                    ", however this action has not any delete content actionlet, so the delete api call is being triggered as part of the request");

            final String actionId       = UtilMethods.isSet(dependencies.getWorkflowActionId())?
                    dependencies.getWorkflowActionId():contentlet.getActionId();
            final User   user           = dependencies.getModUser();

            if(UtilMethods.isSet(actionId)) {
                contentlet.setActionId(actionId);
            }

            if(UtilMethods.isSet(dependencies.getWorkflowActionComments())){
                contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, dependencies.getWorkflowActionComments());
            }

            if(UtilMethods.isSet(dependencies.getWorkflowAssignKey())){
                contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, dependencies.getWorkflowAssignKey());
            }

            Logger.info(this, "The contentlet : " + contentlet.getTitle()
                    + ", on the action id: " + actionId +
                    ", will do an delete");

            contentletAPI.delete(contentlet, user, dependencies.isRespectAnonymousPermissions());

            return contentlet;
        }
    }

    //////////////////////////////

    /**
     * Implements a {@link SystemActionApiFireCommand} that does a destroy to cover the {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction#DESTROY} system action
     */
    private class DestroySystemActionApiFireCommandImpl implements SystemActionApiFireCommand {

        @WrapInTransaction
        @Override
        public Contentlet fire(final Contentlet contentlet, final boolean needSave, final ContentletDependencies dependencies)
                throws DotDataException, DotSecurityException {

            Logger.info(this, "The contentlet : " + contentlet.getTitle()
                    + ", was fired by default action: " + dependencies.getWorkflowActionId() +
                    ", however this action has not any destroy content actionlet, so the delete api call is being triggered as part of the request");

            final String actionId       = UtilMethods.isSet(dependencies.getWorkflowActionId())?
                    dependencies.getWorkflowActionId():contentlet.getActionId();
            final User   user           = dependencies.getModUser();

            if(UtilMethods.isSet(actionId)) {
                contentlet.setActionId(actionId);
            }

            if(UtilMethods.isSet(dependencies.getWorkflowActionComments())){
                contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, dependencies.getWorkflowActionComments());
            }

            if(UtilMethods.isSet(dependencies.getWorkflowAssignKey())){
                contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, dependencies.getWorkflowAssignKey());
            }

            Logger.info(this, "The contentlet : " + contentlet.getTitle()
                    + ", on the action id: " + actionId +
                    ", will do an destroy");

            contentletAPI.destroy(contentlet, user, dependencies.isRespectAnonymousPermissions());

            return contentlet;
        }
    }
}
