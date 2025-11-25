package com.dotcms.ai.workflow;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * The AsyncWorkflowRunner interface provides methods for running workflows asynchronously.
 * It extends both Runnable and Serializable interfaces.
 *
 * This interface is designed to be implemented by classes that need to perform
 * long-running tasks in a separate thread and need to be serialized to a persistent storage.
 */
interface AsyncWorkflowRunner extends Runnable, Serializable {

    /**
     * Returns the time at which the workflow should be run.
     *
     * @return the time at which the workflow should be run.
     */
    long getRunAt();

    /**
     * Returns the identifier of the workflow.
     *
     * @return the identifier of the workflow.
     */
    String getIdentifier();

    /**
     * Returns the language of the workflow.
     *
     * @return the language of the workflow.
     */
    long getLanguage();

    /**
     * Method to be implemented by classes that run the workflow.
     */
    void runInternal();

    /**
     * Method to run the workflow. Throws a DotRuntimeException if not implemented.
     */
    default void run() {
        runInternal();
    }

    /**
     * Saves the contentlet after the workflow has been run.
     *
     * @param workingContentlet the contentlet to be saved.
     * @param user the user who is saving the contentlet.
     */
    default void saveContentlet(final Contentlet workingContentlet, final User user) {
        try {
            workingContentlet.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);
            workingContentlet.setProperty(Contentlet.SKIP_RELATIONSHIPS_VALIDATION, Boolean.TRUE);
            workingContentlet.setProperty(Contentlet.DONT_VALIDATE_ME, Boolean.TRUE);

            final boolean isPublished = APILocator.getVersionableAPI().isLive(workingContentlet);
            final Contentlet checkedContentlet = APILocator.getContentletAPI().checkin(workingContentlet, user, false);
            if (isPublished) {
                checkedContentlet.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);
                checkedContentlet.setProperty(Contentlet.SKIP_RELATIONSHIPS_VALIDATION, Boolean.TRUE);
                checkedContentlet.setProperty(Contentlet.DONT_VALIDATE_ME, Boolean.TRUE);
                if (validateContentlet(checkedContentlet,user)) {
                    APILocator.getContentletAPI().publish(checkedContentlet, user, false);
                }
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Checks out the latest version of the contentlet.
     *
     * @param identifier the identifier of the contentlet.
     * @param language the language of the contentlet.
     * @param user the user who is checking out the contentlet.
     * @return the latest version of the contentlet.
     */
    default Contentlet checkoutLatest(final String identifier, final long language, User user) {
        final Contentlet latest = getLatest(identifier, language, user);
        return Try.of(() -> APILocator
                        .getContentletAPI()
                        .checkout(latest.getInode(), user, false))
                .getOrElseThrow(DotRuntimeException::new);
    }

    /**
     * Gets the latest version of the contentlet.
     *
     * @param identifier the identifier of the contentlet.
     * @param language the language of the contentlet.
     * @param user the user who is getting the contentlet.
     * @return the latest version of the contentlet.
     */
    default Contentlet getLatest(final String identifier, final long language, final User user) {
        final Optional<ContentletVersionInfo> info = APILocator
                .getVersionableAPI()
                .getContentletVersionInfo(identifier, language);
        if (info.isEmpty() || UtilMethods.isEmpty(() -> info.get().getWorkingInode())) {
            throw new DotRuntimeException(
                    "unable to find content version info for id:"
                            + identifier
                            + " lang:"
                            + language);
        }

        return Try.of(() -> APILocator
                        .getContentletAPI()
                        .find(info.get().getWorkingInode(), user, false))
                .getOrElseThrow(DotRuntimeException::new);
    }

    /**
     * Handles any exceptions that occur during the execution of the workflow.
     * It logs the error, sends a system message to the user, and rethrows the exception as a DotRuntimeException.
     *
     * @param e the exception that occurred.
     * @param user the user who is running the workflow.
     * @throws DotRuntimeException if an exception occurs during the execution of the workflow.
     */
    default void handleError(final Exception e, final User user) {
        final String errorMsg = String.format("Error: %s", ExceptionUtil.getErrorMessage(e));
        final SystemMessageBuilder message = new SystemMessageBuilder()
                .setMessage(errorMsg)
                .setLife(5000)
                .setType(MessageType.SIMPLE_MESSAGE)
                .setSeverity(MessageSeverity.ERROR);
        SystemMessageEventUtil.getInstance().pushMessage(message.create(), List.of(user.getUserId()));
        Logger.error(this.getClass(), errorMsg, e);
        throw new DotRuntimeException(e);
    }

    private boolean validateContentlet(final Contentlet contentlet, final User user){
        try {
            final ContentletRelationships relationships = APILocator.getContentletAPI().getAllRelationships(contentlet);
            final List<Category> categories = APILocator
                    .getCategoryAPI()
                    .getParents(contentlet, user, false);
            APILocator.getContentletAPI().validateContentlet(contentlet, relationships, categories);
        } catch(Exception ve) {
            return false;
        }

        return true;
    }

}
