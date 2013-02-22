package com.dotmarketing.quartz.job;

import java.util.List;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;


public class TimeMachineJob implements Job, StatefulJob {

    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        JobDataMap dataMap = ctx.getJobDetail().getJobDataMap();
        Boolean allhosts=(Boolean) dataMap.get("allhosts");
        List<Host> hosts=(List<Host>) dataMap.get("hosts");
        List<Language> langs=(List<Language>) dataMap.get("langs");
        Boolean incremental= (dataMap.get("incremental") != null) ? (Boolean) dataMap.get("incremental") : false;
        try {
            if(allhosts) 
                hosts=APILocator.getHostAPI().findAll(APILocator.getUserAPI().getSystemUser(), false);
            
            APILocator.getTimeMachineAPI().startTimeMachine(hosts, langs,incremental);
        }
        catch(Exception ex) {
            throw new JobExecutionException(ex);
        }
    }
    
}
