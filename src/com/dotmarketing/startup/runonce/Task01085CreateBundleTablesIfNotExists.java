package com.dotmarketing.startup.runonce;

import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

public class Task01085CreateBundleTablesIfNotExists extends AbstractJDBCStartupTask {


    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return "";
    }

    @Override
    public String getMySQLScript() {
        return "";
    }

    @Override
    public String getOracleScript() {
    	DotConnect dc = new DotConnect();
        dc.setSQL( "select 1 from user_tables where table_name = 'PUBLISHING_BUNDLE'" );
        StringBuilder query = new StringBuilder();

        try {
			List<Map<String, Object>> res = dc.loadObjectResults();
			if(res.isEmpty()) {
				query.append("create table publishing_bundle(id varchar2(36) NOT NULL  primary key,name varchar2(255) NOT NULL,publish_date TIMESTAMP,expire_date TIMESTAMP,owner varchar2(100));\n");
				query.append("create table publishing_bundle_environment(id varchar2(36) NOT NULL primary key,bundle_id varchar2(36) NOT NULL,environment_id varchar2(36) NOT NULL);\n");
				query.append("alter table publishing_bundle_environment add constraint FK_bundle_id foreign key (bundle_id) references publishing_bundle(id);\n");
				query.append("alter table publishing_bundle_environment add constraint FK_environment_id foreign key (environment_id) references publishing_environment(id);\n");
				query.append("create table publishing_pushed_assets(bundle_id varchar2(36) NOT NULL,asset_id varchar2(36) NOT NULL,asset_type varchar2(255) NOT NULL,push_date TIMESTAMP,environment_id varchar2(36) NOT NULL);\n");
				query.append("CREATE INDEX idx_pushed_assets_1 ON publishing_pushed_assets (bundle_id);\n");
				query.append("CREATE INDEX idx_pushed_assets_2 ON publishing_pushed_assets (environment_id);\n");
				query.append("alter table publishing_bundle add force_push number(1,0) ;\n");
				query.append("CREATE INDEX idx_pub_qa_1 ON publishing_queue_audit (status);\n");
			}
		} catch (DotDataException e) {
			Logger.error(this, "Unable to verify if table PUBLISHING_BUNDLE exists", e);
		}

        return query.toString();
    }

    @Override
    public String getMSSQLScript() {
        return  "";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
