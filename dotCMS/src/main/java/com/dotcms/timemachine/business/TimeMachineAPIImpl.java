package com.dotcms.timemachine.business;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.enterprise.publishing.timemachine.TimeMachineConfig;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.output.DirectoryBundlerOutput;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.quartz.job.TimeMachineJob;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class TimeMachineAPIImpl implements TimeMachineAPI {

    @Override
    public List<PublishStatus> startTimeMachine(final List<Host> hosts,
                                                final List<Language> languages,
                                                final boolean incremental)  {

        final List<PublishStatus> publishStatusList = new ArrayList<PublishStatus>(languages.size()); // todo: could it be immutable?
        final Date currentDate = new Date();

        for(Language language : languages) {

            try {

                TimeMachineConfig timeMachineConfig = new TimeMachineConfig();
                timeMachineConfig.setUser(APILocator.getUserAPI().getSystemUser());
                timeMachineConfig.setHosts(hosts);
                timeMachineConfig.setLanguage(language.getId());
                timeMachineConfig.setDestinationBundle("tm_" + currentDate.getTime());
                timeMachineConfig.setIncremental(incremental);
                if(incremental) {
                	timeMachineConfig.setId("timeMachineBundle_incremental_" + language.getId());
                } else {
                	timeMachineConfig.setId("timeMachineBundle_" +currentDate.getTime() + "_" + language.getId());
                }

                final DirectoryBundlerOutput directoryPublisherOutput = new DirectoryBundlerOutput(timeMachineConfig);
                publishStatusList.add(APILocator.getPublisherAPI().publish(timeMachineConfig, directoryPublisherOutput));
                directoryPublisherOutput.close();
            }
            catch(Exception ex) {
                Logger.error(this, ex.getMessage(), ex);
            }
        }
        return publishStatusList;
    }
	
	@Override
	public List<Host> getHostsWithTimeMachine() {
	    final Set<Host> hostSet = new HashSet<Host>();
        final File timeMachinePath = new File(ConfigUtils.getTimeMachinePath());

        for ( File file : timeMachinePath.listFiles()) {
            if ( file.isDirectory() && file.getName().startsWith("tm_")) {

                final File hostDir = new File(file.getAbsolutePath() + File.separator + "live");
                if ( hostDir.exists() && hostDir.isDirectory() ) {
                    for(String hostname : hostDir.list()) {
                        try {
                            final Host host = APILocator.getHostAPI().findByName(hostname, APILocator.getUserAPI().getSystemUser(), false);
                            if(host!=null && UtilMethods.isSet(host.getIdentifier())) {
                                hostSet.add(host);
                            }
                        } catch(Exception ex) {
                            Logger.warn(this, ex.getMessage(),ex);
                        }
                    }
                }
            }
        }
        return new ArrayList<Host>(hostSet);
	}

	@Override
	public List<Date> getAvailableTimeMachineForSite(final Host host) throws DotDataException {

		final List<Date> list = new ArrayList<Date>();
		final File timeMachinePath = new File(ConfigUtils.getTimeMachinePath());

		for ( File file : timeMachinePath.listFiles()) {
			if ( file.isDirectory() && file.getName().startsWith("tm_")) {

				final File hostDir = new File(file.getAbsolutePath() + File.separator + "live" + File.separator + host.getHostname());
				if ( hostDir.exists() && hostDir.isDirectory() ) {
					try {
					    final Date date=new Date(Long.parseLong(file.getName().substring(3)));
						list.add(date);
					} catch (Throwable t) {
						Logger.error(this, "bundle seems a time machine bundle but it is not! " + file.getName());
					}
				}
			}
		}

		return list;
	}
	
	@Override
	public ScheduledTask getQuartzJob() {
	    try {
	        List<ScheduledTask> sched = QuartzUtils.getScheduledTasks("timemachine");
	        if(sched.size()==0) {
	            return null;
	        }
	        else {
	            return sched.get(0);
	        }
	    }
	    catch(Exception ex) {
	        throw new RuntimeException(ex);
	    }
	}


    @Override
    public List<String> getAvailableLangForTimeMachine(final Host host, final Date date) {

        final File hostPath = new File(ConfigUtils.getTimeMachinePath()+File.separator+
                                 "tm_"+date.getTime()+File.separator+
                                 "live"+File.separator+host.getHostname());
        return (hostPath.exists())?
             Arrays.asList(hostPath.list(new TimeMachineFileNameFilter())):
             null;
    }
    
    @Override
    public void removeQuartzJob() throws DotRuntimeException {
        try {
            if(getQuartzJob()!=null) {
                QuartzUtils.pauseJob("timemachine","timemachine");
                QuartzUtils.removeTaskRuntimeValues("timemachine","timemachine");
                QuartzUtils.removeJob("timemachine","timemachine");
            }
        }
        catch(Exception ex) {
            throw new DotRuntimeException(
                    "error while removing timemachine quartz job",ex);
        }
    }

    @Override
    public void setQuartzJobConfig(String cronExp, List<Host> hosts, boolean allhost, List<Language> langs, boolean incremental) {
        Map<String,Object> config=new HashMap<String,Object>();
        config.put("CRON_EXPRESSION",cronExp);
        config.put("hosts", hosts);
        config.put("langs", langs);
        config.put("allhosts", allhost);
        config.put("incremental", incremental);
        ScheduledTask task=getQuartzJob();
        if(task!=null) {
            // delete the old one
            removeQuartzJob();
        }
        // create it
        task = new CronScheduledTask("timemachine", "timemachine", "Time Machine", 
                TimeMachineJob.class.getName(), new Date(), null, 1, config, cronExp);
        try {
            QuartzUtils.scheduleTask(task);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    private class TimeMachineFileNameFilter implements FilenameFilter{
    	

		@Override
		public boolean accept(File dir, String name) {
			int lang = 0;
			try{
				lang = Integer.parseInt(name);
				
			}
			catch(Exception e){
				Logger.debug(this.getClass(), "not a language directory");
			}

			return lang > 0;
		}
    }

}
