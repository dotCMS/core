package com.dotmarketing.startup.runalways;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

public class Task00008CreateDefaultWorkflowScheme implements StartupTask {


    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        
        try {
            CacheLocator.getWorkFlowCache().clearCache();
            APILocator.getWorkflowAPI().createDefaultScheme();
        } catch (Exception e) {
            // let's don't make much noise here because of the upgrade
            // from 1.9.3 where those workflow tables aren't created yet
            Logger.warn(this.getClass(),e.getMessage());
        }
        
    }

    public boolean forceRun() {
        WorkflowScheme scheme  = null;
        try{
            scheme = APILocator.getWorkflowAPI().findDefaultScheme();
        }
        catch(Exception e){
            
        }
        return (scheme==null);
    }

}
