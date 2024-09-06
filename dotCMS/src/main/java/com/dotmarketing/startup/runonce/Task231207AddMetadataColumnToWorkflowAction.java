package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * Adds the {@code metadata} column to the {@code workflow_action} table. This JSONB column is meant
 * to store any sort of additional configuration properties for a Workflow Action that may not be
 * strictly related to their core functionality.
 *
 * @author Jose Castro
 * @since Oct 10th, 2023
 */
public class Task231207AddMetadataColumnToWorkflowAction implements StartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final DotConnect dc = new DotConnect().setSQL("ALTER TABLE workflow_action ADD COLUMN IF NOT EXISTS metadata JSONB NULL");
        dc.loadResult();
    }

}
