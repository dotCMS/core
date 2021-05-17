package com.dotmarketing.quartz.job;

import com.dotcms.content.elasticsearch.business.ElasticReadOnlyCommand;
import com.dotmarketing.util.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

/**
 * This job check if the Elastic server is running on ready only mode and will try to switch to write mode
 * @author jsanca
 */
public class EsReadOnlyMonitorJob implements StatefulJob {

    @Override
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {

        final ElasticReadOnlyCommand command = ElasticReadOnlyCommand.getInstance();
        Logger.debug(this, ()->"Running the EsReadOnlyMonitorJob...");
        command.executeCheck();
    }
}
