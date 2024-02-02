package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * Adds the {@code metadata} column to the {@code structure} table. This JSONB column is meant
 * to store any sort of additional configuration properties for a Content Type that may not be
 * strictly related to their core functionality or for temporary behavior.
 *
 * @author Jose Castro
 * @since Jan 11th, 2024
 */
public class Task240112AddMetadataColumnToStructureTable implements StartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final DotConnect dc = new DotConnect().setSQL("ALTER TABLE structure ADD COLUMN IF NOT EXISTS metadata JSONB NULL");
        dc.loadResult();
    }

}
