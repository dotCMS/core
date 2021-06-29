package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.util.Collections;
import java.util.List;

public class Task05150CreateIndicesForContentVersionInfoMSSQL extends AbstractJDBCStartupTask {
    @Override
    public String getPostgresScript() {
        return null;
    }

    @Override
    public String getMySQLScript() {
        return null;
    }

    @Override
    public String getOracleScript() {
        return null;
    }

    @Override
    public String getMSSQLScript() {
        return "create index cvi_identifier_index on contentlet_version_info (identifier);"
                + "create index cvi_working_inode_index on contentlet_version_info (working_inode);"
                + "create index cvi_live_inode_index on contentlet_version_info (live_inode);"
                + "create index cvi_lang_index on contentlet_version_info (lang);"
                + "create index cvi_locked_by_index on contentlet_version_info (locked_by);";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return Collections.emptyList();
    }

    @Override
    public boolean forceRun() {
        return true;
    }
}
