package com.dotmarketing.portlets.workflows.action;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;

/**
 * Decides whether a user may write to a workflow task.
 *
 * <p>A workflow task carries no permissions of its own: the rows live on the contentlet the task
 * was opened for. So the question "may this user comment on, or attach a file to, this task" is
 * answered by asking whether they may read that contentlet, which is the same bar
 * {@code POST /api/v1/workflow/{contentletId}/comments} applies. Front-end roles are not counted,
 * because these are back-end screens.</p>
 *
 * <p>READ rather than EDIT is deliberate. The legacy task screen offers the comment box and the
 * attach and remove file buttons to everyone who can open it, so a user who can only read a
 * contentlet comments on its task today; asking for EDIT would take that away.</p>
 */
public final class WorkflowTaskAccess {

    private WorkflowTaskAccess() {
        throw new AssertionError("WorkflowTaskAccess is a utility class");
    }

    /**
     * Tells whether the given user may write to the given workflow task.
     *
     * <p>Anything that stops the answer from being a clear yes is a no: a missing task or user, a
     * contentlet that cannot be resolved, or a lookup that fails. A task whose contentlet is gone
     * cannot be authorized against anything, so it is refused rather than waved through.</p>
     *
     * @param task the {@link WorkflowTask} being written to
     * @param user the {@link User} making the request; {@code null} is refused
     * @return {@code true} when the user may read the task's contentlet, and so may write to the
     *         task
     */
    public static boolean canAccess(final WorkflowTask task, final User user) {
        if (null == task || null == user) {
            return false;
        }
        if (!InodeUtils.isSet(task.getWebasset())) {
            SecurityLogger.logInfo(WorkflowTaskAccess.class,
                    String.format("Refusing workflow task '%s' for user '%s': the task names no "
                            + "contentlet", task.getId(), user.getUserId()));
            return false;
        }
        try {
            final Contentlet contentlet = APILocator.getContentletAPI()
                    .findContentletByIdentifier(task.getWebasset(), false, task.getLanguageId(),
                            user, false);
            if (null != contentlet && InodeUtils.isSet(contentlet.getInode())) {
                return true;
            }
            SecurityLogger.logInfo(WorkflowTaskAccess.class,
                    String.format("Refusing workflow task '%s' for user '%s': its contentlet '%s' "
                                    + "could not be resolved", task.getId(), user.getUserId(),
                            task.getWebasset()));
            return false;
        } catch (final DotSecurityException e) {
            SecurityLogger.logInfo(WorkflowTaskAccess.class,
                    String.format("User '%s' is not allowed to write to workflow task '%s'",
                            user.getUserId(), task.getId()));
            return false;
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
            return false;
        }
    }
}
