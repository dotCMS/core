package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task01330CreateIndicesForVersionTables extends AbstractJDBCStartupTask {

	String createIndices = "create index idx_contentlet_vi_version_ts on contentlet_version_info(version_ts);\n"+
    		"create index idx_container_vi_version_ts on container_version_info(version_ts);\n"+
    		"create index idx_template_vi_version_ts on template_version_info(version_ts);\n"+
    		"create index idx_htmlpage_vi_version_ts on htmlpage_version_info(version_ts);\n"+
    		"create index idx_fileasset_vi_version_ts on fileasset_version_info(version_ts);\n"+
    		"create index idx_link_vi_version_ts on link_version_info(version_ts);\n";

    @Override
    public boolean forceRun() {
       return true;
    }

    @Override
    public String getPostgresScript() {
        return createIndices;
    }

    @Override
    public String getMySQLScript() {
    	return createIndices;
    }

    @Override
    public String getOracleScript() {
    	return createIndices;
    }

    @Override
    public String getMSSQLScript() {
    	return createIndices;
    }

    @Override
    public String getH2Script() {
    	return createIndices;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
