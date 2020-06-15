package com.dotcms.workflow;

import static com.dotmarketing.db.HibernateUtil.closeSessionSilently;
import static com.dotmarketing.db.HibernateUtil.rollbackTransaction;
import java.util.List;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import com.dotcms.business.CloseDB;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

public class EscalationThread extends DotStatefulJob {



    final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
    final String wfActionAssign = Config.getStringProperty("ESCALATION_DEFAULT_ASSIGN", StringPool.BLANK);
    final String wfActionComments = Config.getStringProperty("ESCALATION_DEFAULT_COMMENT", "Task time out");

    @CloseDB
    public void run(final JobExecutionContext jobContext) throws JobExecutionException {

        if (LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level) {
            try {
                final List<WorkflowTask> tasks = workflowAPI.findExpiredTasks();

                for (final WorkflowTask task : tasks) {
                    esclateTask(task);
                }
            } catch (Exception ex) {
                Logger.warn(this, ex.getMessage(), ex);
            } 
        }
    }

    @WrapInTransaction
    private void esclateTask(final WorkflowTask task) throws DotDataException, DotSecurityException {


        final String stepId = task.getStatus();
        final WorkflowStep step = workflowAPI.findStep(stepId);
        final String actionId = step.getEscalationAction();
        final WorkflowAction action = workflowAPI.findAction(actionId, APILocator.getUserAPI().getSystemUser());

        Logger.info(this,
                        "Task '" + task.getTitle() + "' " + "on contentlet id '" + task.getWebasset() + "' " + "timeout on step '"
                                        + step.getName() + "' " + "excecuting escalation action '" + action.getName() + "'");

        // find contentlet for the given language
        final Contentlet contentletByDefaultLanguage = APILocator.getContentletAPI().findContentletByIdentifier(
                        task.getWebasset(), false, task.getLanguageId(), APILocator.getUserAPI().getSystemUser(), false);

        // No need to escalate if the contentlet already is in the Action Escalated.
        if (UtilMethods.isSet(actionId) && !actionId.equals(contentletByDefaultLanguage.getActionId())) {

            final String inode = contentletByDefaultLanguage.getInode();
            final Contentlet contentlet =
                            APILocator.getContentletAPI().find(inode, APILocator.getUserAPI().getSystemUser(), false);



            APILocator.getWorkflowAPI().fireContentWorkflow(contentlet,
                            new ContentletDependencies.Builder().respectAnonymousPermissions(false)
                                            .modUser(APILocator.getUserAPI().getSystemUser()).workflowActionId(action.getId())
                                            .workflowActionComments(wfActionComments).workflowAssignKey(wfActionAssign).build());
        }


    }



}
