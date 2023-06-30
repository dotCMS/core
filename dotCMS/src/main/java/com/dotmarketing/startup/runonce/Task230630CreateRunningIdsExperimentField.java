package com.dotmarketing.startup.runonce;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * {@link StartupTask} to create a running_ids field in the experiment table
 */
public class Task230630CreateRunningIdsExperimentField implements StartupTask  {

    @Override
    public boolean forceRun() {
        return false;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

    }
}
