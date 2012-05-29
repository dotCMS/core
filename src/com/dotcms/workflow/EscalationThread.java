package com.dotcms.workflow;

import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class EscalationThread extends Thread {
    private static final EscalationThread instance=new EscalationThread();
    
    public static EscalationThread getInstace() {
        return instance;
    }
    
    private EscalationThread() {}
    
    @Override
    public void run() {
        if(Config.getBooleanProperty("ESCALATION_ENABLE",false)) {
            int interval=Config.getIntProperty("ESCALATION_CHECK_INTERVAL_SECS",600);
            WorkflowAPI wapi=APILocator.getWorkflowAPI();
            while(!Thread.interrupted()) {
                try {
                    try {
                        HibernateUtil.startTransaction();
                        List<WorkflowTask> tasks=wapi.findExpiredTasks();
                        for (WorkflowTask task : tasks) {
                            String stepId=task.getStatus();
                            WorkflowStep step=wapi.findStep(stepId);
                            String actionId=step.getEscalationAction();
                            WorkflowAction action=wapi.findAction(actionId, APILocator.getUserAPI().getSystemUser());
                            
                            // perform action
                        }
                        HibernateUtil.commitTransaction();
                    }
                    catch(Exception ex) {
                        try {
                            HibernateUtil.rollbackTransaction();
                        } catch (DotHibernateException e) {}
                        Logger.warn(this, ex.getMessage(), ex);
                    }
                    
                    Thread.sleep(interval*1000L);
                }
                catch(InterruptedException ex) {
                    return;
                }
            }
        }
    }
}
