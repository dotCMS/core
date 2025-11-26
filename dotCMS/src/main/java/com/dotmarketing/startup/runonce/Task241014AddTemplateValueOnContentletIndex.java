package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * This upgrade task adds an index on the template value of a contentlet. This
 * GREATLY speeds up queries.
 */
public class Task241014AddTemplateValueOnContentletIndex implements StartupTask {

    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        new DotConnect().setSQL("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_contentlet_template_value ON contentlet((contentlet_as_json->'fields'->'template'->>'value'))").loadResult();

    }
}
