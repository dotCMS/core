package com.dotmarketing.portlets.workflows.action;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;

/**
 * Decides whether a user may write to a workflow task.
 *
 * <p>A workflow task carries no permissions of its own: the rows live on the contentlet the task
 * was opened for. So every question about a task is answered by asking about that contentlet.
 * Front-end roles are not counted, because these are back-end screens.</p>
 *
 * <p>The bar is not the same for every command. Adding a comment or attaching a file asks for READ
 * — the same bar {@code POST /api/v1/workflow/{contentletId}/comments} applies, and the legacy
 * task screen offers the comment box to everyone who can open it, so asking for more would take
 * away something that works today. Removing an attachment asks for EDIT: it destroys someone
 * else's work, there is no REST endpoint whose bar it could inherit, and READ there would let a
 * reader delete an editor's file.</p>
 */
public final class WorkflowTaskAccess {

    private WorkflowTaskAccess() {
        throw new AssertionError("WorkflowTaskAccess is a utility class");
    }

    /**
     * Tells whether the given user may write to the given workflow task, for the commands whose
     * bar is reading the content: commenting on the task and attaching a file to it.
     *
     * <p>Anything that stops the answer from being a clear yes is a no: a missing task or user, a
     * contentlet that cannot be resolved, or a lookup that fails. A task whose contentlet is gone
     * cannot be authorized against anything, so it is refused rather than waved through.</p>
     *
     * @param task the {@link WorkflowTask} being written to
     * @param user the {@link User} making the request; {@code null} is refused
     * @return {@code true} when the user may read the task's contentlet
     */
    public static boolean canAccess(final WorkflowTask task, final User user) {
        return null != resolveContentlet(task, user);
    }

    /**
     * Tells whether the given user may remove something from the given workflow task, which asks
     * for EDIT on its contentlet rather than READ.
     *
     * @param task the {@link WorkflowTask} being written to
     * @param user the {@link User} making the request; {@code null} is refused
     * @return {@code true} when the user may edit the task's contentlet
     */
    public static boolean canEdit(final WorkflowTask task, final User user) {
        final Contentlet contentlet = resolveContentlet(task, user);
        if (null == contentlet) {
            return false;
        }
        try {
            if (APILocator.getPermissionAPI().doesUserHavePermission(contentlet,
                    PermissionAPI.PERMISSION_EDIT, user,
                    Constants.DONT_RESPECT_FRONT_END_ROLES)) {
                return true;
            }
            SecurityLogger.logInfo(WorkflowTaskAccess.class,
                    String.format("User '%s' may read but not edit the contentlet of workflow "
                            + "task '%s'", user.getUserId(), task.getId()));
            return false;
        } catch (final Exception e) {
            Logger.warn(WorkflowTaskAccess.class,
                    String.format("Unable to resolve edit access on workflow task '%s'",
                            task.getId()), e);
            SecurityLogger.logInfo(WorkflowTaskAccess.class,
                    String.format("Refusing workflow task '%s' for user '%s': edit access could "
                            + "not be resolved (%s)", task.getId(), user.getUserId(), e));
            return false;
        }
    }

    /**
     * Reads the contentlet the task belongs to, as the calling user, so that resolving it is
     * itself the READ check.
     *
     * @param task the {@link WorkflowTask} whose contentlet is wanted
     * @param user the {@link User} making the request
     * @return the {@link Contentlet}, or {@code null} when the user may not read it, the task
     *         names none, or the lookup could not be answered
     */
    private static Contentlet resolveContentlet(final WorkflowTask task, final User user) {
        if (null == task || null == user) {
            return null;
        }
        if (!InodeUtils.isSet(task.getWebasset())) {
            SecurityLogger.logInfo(WorkflowTaskAccess.class,
                    String.format("Refusing workflow task '%s' for user '%s': the task names no "
                            + "contentlet", task.getId(), user.getUserId()));
            return null;
        }
        try {
            final Contentlet contentlet = APILocator.getContentletAPI()
                    .findContentletByIdentifier(task.getWebasset(), false, task.getLanguageId(),
                            user, Constants.DONT_RESPECT_FRONT_END_ROLES);
            if (null != contentlet && InodeUtils.isSet(contentlet.getInode())) {
                return contentlet;
            }
            SecurityLogger.logInfo(WorkflowTaskAccess.class,
                    String.format("Refusing workflow task '%s' for user '%s': its contentlet '%s' "
                                    + "could not be resolved", task.getId(), user.getUserId(),
                            task.getWebasset()));
            return null;
        } catch (final DotSecurityException e) {
            SecurityLogger.logInfo(WorkflowTaskAccess.class,
                    String.format("User '%s' is not allowed to write to workflow task '%s'",
                            user.getUserId(), task.getId()));
            return null;
        } catch (final Exception e) {
            // Resolving the contentlet failed rather than came back negative. Refuse, but say in
            // the audit trail that this was a failure to decide, not a decision, so a database or
            // index blip is not read later as a permissions problem.
            Logger.warn(WorkflowTaskAccess.class,
                    String.format("Unable to resolve the contentlet of workflow task '%s'",
                            task.getId()), e);
            SecurityLogger.logInfo(WorkflowTaskAccess.class,
                    String.format("Refusing workflow task '%s' for user '%s': access could not be "
                            + "resolved (%s)", task.getId(), user.getUserId(), e));
            return null;
        }
    }
}
