package com.dotmarketing.startup.runonce;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.tasks.FixTask00090RecreateMissingFoldersInParentPath;
import com.dotmarketing.startup.StartupTask;

/**
 * Upgrade task that sets the system_folder identifier to SYSTEM_FOLDER and updates all folders to use the identifier as
 * the inode;
 */
public class Task250604UpdateFolderInodes implements StartupTask {

    @Override
    public boolean forceRun() {

        return APILocator.getFolderAPI().folderIdsNeedFixing();
    }

    /**
     * Executes the upgrade task, creating the necessary tables and indexes for the Job Queue.
     *
     * @throws DotDataException    if a data access error occurs.
     * @throws DotRuntimeException if a runtime error occurs.
     */
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        //Running Update Assets Inconsistencies before fixing folder IDs
        final FixTask00090RecreateMissingFoldersInParentPath task = new FixTask00090RecreateMissingFoldersInParentPath();

        if (task.shouldRun()){
            task.executeFix();
        }

        //Updating folder IDs
        APILocator.getFolderAPI().fixFolderIds();
    }
}
