package com.dotmarketing.portlets.contentlet.business;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.*;
import org.quartz.SimpleTrigger;

import com.dotcms.enterprise.HostAssetsJobProxy;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.SimpleScheduledTask;
import com.dotmarketing.quartz.job.HostCopyOptions;
import com.liferay.portal.model.User;

public class HostAPITest {
    
    @Test
    public void testDeleteHost() throws Exception {
        User user=APILocator.getUserAPI().getSystemUser();
        
        Host source=APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
        
        Host host=new Host();
        host.setHostname("copy"+System.currentTimeMillis()+".demo.dotcms.com");
        host.setDefault(false);
        host=APILocator.getHostAPI().save(host, user, false);
        String hostIdent=host.getIdentifier();
        String hostName=host.getHostname();
        
        HostCopyOptions options=new HostCopyOptions(true);
        
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("sourceHostId", source.getIdentifier());
        parameters.put("destinationHostId", host.getIdentifier());
        parameters.put("copyOptions", options);
        
        Calendar startTime = Calendar.getInstance();
        SimpleScheduledTask task = new SimpleScheduledTask("setup-host-" + host.getIdentifier(), "setup-host-group", "Setups host "
                + host.getIdentifier() + " from host " + source.getIdentifier(), HostAssetsJobProxy.class.getCanonicalName(), false,
                "setup-host-" + source.getIdentifier() + "-trigger", "setup-host-trigger-group", startTime.getTime(), null,
                SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT, 5, true, parameters, 0, 0);
        
        QuartzUtils.scheduleTask(task);
        
        // wait for the copy to be done
        while(QuartzUtils.getTaskProgress(task.getJobName(), task.getJobGroup())<100) {
            Thread.sleep(500);
        }
        Thread.sleep(600); // wait a bit for the index
        
        APILocator.getHostAPI().archive(host, user, false);
        APILocator.getHostAPI().deleteAndWait(host, user, false);
        
        Thread.sleep(600); // wait a bit for the index
        
        host = APILocator.getHostAPI().find(hostIdent, user, false);
        
        Assert.assertNull(host);
        
        host = APILocator.getHostAPI().findByName(hostName, user, false);
        
        Assert.assertNull(host);
    }
}
