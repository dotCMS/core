package com.dotcms.workflow;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.APILocator;


import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
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
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.List;
import static com.dotmarketing.db.HibernateUtil.*;

public class EscalationThread extends DotStatefulJob {

    public void run(final JobExecutionContext jobContext) throws JobExecutionException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final String wfActionAssign   = Config.getStringProperty("ESCALATION_DEFAULT_ASSIGN", StringPool.BLANK);
        final String wfActionComments = Config.getStringProperty("ESCALATION_DEFAULT_COMMENT", "Task time out");

        if (LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level) {

            try {

                startTransaction();
                final List<WorkflowTask> tasks = workflowAPI.findExpiredTasks();

                for (final WorkflowTask task : tasks) {

                    final String stepId         = task.getStatus();
                    final WorkflowStep step     = workflowAPI.findStep(stepId);
                    final String actionId       = step.getEscalationAction();
                    final WorkflowAction action = workflowAPI.findAction(actionId, APILocator.getUserAPI().getSystemUser());

                    Logger.info(this, "Task '" + task.getTitle() + "' " +
                            "on contentlet id '" + task.getWebasset() + "' " +
                            "timeout on step '" + step.getName() + "' " +
                            "excecuting escalation action '" + action.getName() + "'");

                    // find contentlet for default language
                    final Contentlet contentletByDefaultLanguage =
                            APILocator.getContentletAPI().findContentletByIdentifier(task.getWebasset(), false,
                                task.getLanguageId(),
                                APILocator.getUserAPI().getSystemUser(), false);

                    //No need to escalate if the contentlet already is in the Action Escalated.
                    if(UtilMethods.isSet(actionId) && !actionId.equals
                            (contentletByDefaultLanguage.getActionId())) {

                        final String inode          = contentletByDefaultLanguage.getInode();
                        final Contentlet contentlet = APILocator.getContentletAPI().find
                                (inode, APILocator.getUserAPI().getSystemUser(), false);
                        
                        
                        
                        APILocator.getWorkflowAPI().fireContentWorkflow(contentlet,
                                new ContentletDependencies.Builder()
                                        .respectAnonymousPermissions(false)
                                        .modUser(APILocator.getUserAPI().getSystemUser())
                                        .workflowActionId(action.getId())
                                        .workflowActionComments(wfActionComments)
                                        .workflowAssignKey(wfActionAssign).build());
                    }
                }

                commitTransaction();
            } catch (Exception ex) {
                Logger.warn(this, ex.getMessage(), ex);

                try {
                    rollbackTransaction();
                } catch (DotHibernateException e) {}
            } finally {

                closeSessionSilently();
            }
        }
    } // run.
} // E:O:F:EscalationThread.
