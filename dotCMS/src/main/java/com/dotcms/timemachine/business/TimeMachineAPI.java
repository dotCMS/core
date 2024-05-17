package com.dotcms.timemachine.business;

import java.io.File;
import java.util.Date;
import java.util.List;

import com.dotcms.publishing.PublishStatus;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.quartz.ScheduledTask;

public interface TimeMachineAPI {

	public List<Date> getAvailableTimeMachineForSite(Host host) throws DotDataException;

    public ScheduledTask getQuartzJob();

    public List<String> getAvailableLangForTimeMachine(Host host, Date date);

    public List<Host> getHostsWithTimeMachine();

    List<PublishStatus> startTimeMachine(List<Host> hosts, List<Language> langs, boolean incremental);

    void setQuartzJobConfig(String cronExp, List<Host> hosts, boolean allhost, List<Language> langs, boolean incremental);

    void removeQuartzJob() throws DotRuntimeException;

    /**
     * Prunes all Time Machine backup folders that are older than the value specified
     * in the PRUNE_TIMEMACHINE_OLDER_THAN_DAYS configuration property.
     *
     * @return A List containing all the files that were deleted.
     */
    List<File> removeOldTimeMachineBackupsFiles();

    /**
     * Return all the Time Machine folder on the Time Machine path
     *
     * Every time the {@link com.dotmarketing.quartz.job.TimeMachineJob} runs, it creates a folder inside the
     * Time Machine path (one for each language). Inside this new folder, it stores all the files produced during
     * the Time Machine process.
     *
     * These files are necessary to render all the pages on the selected sites.
     *
     * This method return all the folder that exists on The Time Machine Path.
     *
     * The Time Machine Path is set using the TIMEMACHINE_PATH property.
     *
     * @return all the Time Machine folder on the Time Machine path
     */
    List<File> getAvailableTimeMachineFolder();
	
}
