package com.dotmarketing.startup.runalways;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

public class Task00002ClusterInitialize implements StartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        ClusterFactory.initialize();
    }

}
