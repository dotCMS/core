package com.dotcms.workflow;

import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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
            final int interval=Config.getIntProperty("ESCALATION_CHECK_INTERVAL_SECS",600);
            final String wfActionAssign=Config.getStringProperty("ESCALATION_DEFAULT_ASSIGN","");
            final String wfActionComments=Config.getStringProperty("ESCALATION_DEFAULT_COMMENT","Task time out");
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
                            
                            Logger.info(this, "Task '"+task.getTitle()+"' " +
                                    "on contentlet id '"+task.getWebasset()+"' "+
                            		"timeout on step '"+step.getName()+"' " +
                            		"excecuting escalation action '"+action.getName()+"'");
                            
                            // find contentlet for default language
                            Contentlet def=APILocator.getContentletAPI().findContentletByIdentifier(task.getWebasset(), false, 
                                    APILocator.getLanguageAPI().getDefaultLanguage().getId(), 
                                    APILocator.getUserAPI().getSystemUser(), false);
                            String inode=def.getInode();
                            
                            Contentlet c;

                            // if the worflow requires a checkin
                            if(action.requiresCheckout()){
                                c = APILocator.getContentletAPI().checkout(inode, APILocator.getUserAPI().getSystemUser(), false);
                                c.setStringProperty("wfActionId", action.getId());
                                c.setStringProperty("wfActionComments", wfActionComments);
                                c.setStringProperty("wfActionAssign", wfActionAssign);
                                
                                c = APILocator.getContentletAPI().checkin(c, APILocator.getUserAPI().getSystemUser(), false);
                            }
                            
                            // if the worflow requires a checkin
                            else{
                                c = APILocator.getContentletAPI().find(inode, APILocator.getUserAPI().getSystemUser(), false);
                                c.setStringProperty("wfActionId", action.getId());
                                c.setStringProperty("wfActionComments", wfActionComments);
                                c.setStringProperty("wfActionAssign", wfActionAssign);
                                wapi.fireWorkflowNoCheckin(c);
                            }
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
