package com.dotmarketing.startup.runonce;

import com.dotcms.util.content.json.PopulateContentletAsJSONUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.job.PopulateContentletAsJSONJob;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.io.IOException;
import java.sql.SQLException;

public class Task230320FixMissingContentletAsJSON implements StartupTask {

    @Override
    public boolean forceRun() {
        return PopulateContentletAsJSONUtil.canPersistContentAsJson();
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        Logger.info(this, "Running upgrade Task230320FixMissingContentletAsJSON");

        // First we fire the job to populate the missing contentlet as JSON fields for everything except Hosts, this
        // will execute a background stateful quartz job
        PopulateContentletAsJSONJob.fireJob("Host");

        try {
            // Now we populate the contentlet as JSON for Hosts, this will execute in the same thread
            new PopulateContentletAsJSONUtil().populateForAssetSubType("Host");
        } catch (SQLException | IOException e) {
            Logger.error(this, "Error populating Contentlet as JSON population column for Hosts", e);
            throw new DotDataException(e.getMessage(), e);
        }
    }

}