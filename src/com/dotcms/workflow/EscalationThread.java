package com.dotcms.workflow;

import java.util.List;

import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class EscalationThread extends DotStatefulJob {

    public void run(JobExecutionContext jobContext) throws JobExecutionException {

        WorkflowAPI wapi=APILocator.getWorkflowAPI();

        final String wfActionAssign=Config.getStringProperty("ESCALATION_DEFAULT_ASSIGN","");
        final String wfActionComments=Config.getStringProperty("ESCALATION_DEFAULT_COMMENT", "Task time out");

        if(LicenseUtil.getLevel()>=200) {
            try {
                HibernateUtil.startTransaction();
                List<WorkflowTask> tasks = wapi.findExpiredTasks();
                for (WorkflowTask task : tasks) {
                    String stepId = task.getStatus();
                    WorkflowStep step = wapi.findStep(stepId);
                    String actionId = step.getEscalationAction();
                    WorkflowAction action = wapi.findAction(actionId, APILocator.getUserAPI().getSystemUser());

                    Logger.info(this, "Task '" + task.getTitle() + "' " +
                            "on contentlet id '" + task.getWebasset() + "' " +
                            "timeout on step '" + step.getName() + "' " +
                            "excecuting escalation action '" + action.getName() + "'");

                    // find contentlet for default language
                    Contentlet def = APILocator.getContentletAPI().findContentletByIdentifier(task.getWebasset(), false,
                            APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            APILocator.getUserAPI().getSystemUser(), false);

                    //No need to escalate if the contentlet already is in the Action Escalated.
                    if(UtilMethods.isSet(actionId) && !actionId.equals(def.getStringProperty("wfActionId"))){
                        String inode = def.getInode();

                        // if the worflow requires a checkin
                        if (action.requiresCheckout()) {
                            Contentlet c = APILocator.getContentletAPI().checkout(inode, APILocator.getUserAPI().getSystemUser(), false);
                            c.setStringProperty("wfActionId", action.getId());
                            c.setStringProperty("wfActionComments", wfActionComments);
                            c.setStringProperty("wfActionAssign", wfActionAssign);

                            APILocator.getContentletAPI().checkin(c, APILocator.getUserAPI().getSystemUser(), false);
                        }

                        // if the worflow requires a checkin
                        else {
                            Contentlet c = APILocator.getContentletAPI().find(inode, APILocator.getUserAPI().getSystemUser(), false);
                            c.setStringProperty("wfActionId", action.getId());
                            c.setStringProperty("wfActionComments", wfActionComments);
                            c.setStringProperty("wfActionAssign", wfActionAssign);
                            wapi.fireWorkflowNoCheckin(c, APILocator.getUserAPI().getSystemUser());
                        }
                    }
                }
                HibernateUtil.commitTransaction();

            } catch (Exception ex) {
                Logger.warn(this, ex.getMessage(), ex);

                try {
                    HibernateUtil.rollbackTransaction();
                } catch (DotHibernateException e) {}
            }
        }
    }
}
