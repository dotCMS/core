package com.dotmarketing.startup.runonce;

import com.dotcms.util.content.json.PopulateContentletAsJSONUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import org.apache.commons.lang.time.StopWatch;

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

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            PopulateContentletAsJSONUtil.getInstance().populateForAssetSubType("Host");
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        } catch (IOException e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }

        stopWatch.stop();
        Logger.info(this, "Contentlet as JSON migration task DONE, duration:" +
                DateUtil.millisToSeconds(stopWatch.getTime()) + " seconds");
    }

}
