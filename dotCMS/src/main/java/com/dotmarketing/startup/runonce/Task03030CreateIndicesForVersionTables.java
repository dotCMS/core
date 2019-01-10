package com.dotmarketing.startup.runonce;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

public class Task03030CreateIndicesForVersionTables extends AbstractJDBCStartupTask {

	String createIndices = "create index idx_contentlet_vi_version_ts on contentlet_version_info(version_ts);\n"+
    		"create index idx_container_vi_version_ts on container_version_info(version_ts);\n"+
    		"create index idx_template_vi_version_ts on template_version_info(version_ts);\n"+
    		"create index idx_htmlpage_vi_version_ts on htmlpage_version_info(version_ts);\n"+
    		"create index idx_fileasset_vi_version_ts on fileasset_version_info(version_ts);\n"+
    		"create index idx_link_vi_version_ts on link_version_info(version_ts);\n";

    @Override
    public boolean forceRun() {

       return !this.existsIndexOnTable(DbConnectionFactory.getConnection(), "contentlet_version_info", "idx_contentlet_vi_version_ts") &&
               !this.existsIndexOnTable(DbConnectionFactory.getConnection(), "container_version_info", "idx_container_vi_version_ts") &&
               !this.existsIndexOnTable(DbConnectionFactory.getConnection(), "template_version_info", "idx_template_vi_version_ts") &&
               !this.existsIndexOnTable(DbConnectionFactory.getConnection(), "htmlpage_version_info", "idx_htmlpage_vi_version_ts") &&
               !this.existsIndexOnTable(DbConnectionFactory.getConnection(), "fileasset_version_info", "idx_fileasset_vi_version_ts") &&
               !this.existsIndexOnTable(DbConnectionFactory.getConnection(), "link_version_info", "idx_link_vi_version_ts");
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
