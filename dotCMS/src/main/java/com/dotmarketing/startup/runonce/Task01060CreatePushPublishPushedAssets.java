package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

public class Task01060CreatePushPublishPushedAssets implements StartupTask {

	private void createPushedAssetsTable(DotConnect dc) throws SQLException, DotDataException {
		if(DbConnectionFactory.isMsSql()) {
			dc.executeStatement("create table publishing_pushed_assets(bundle_id varchar(36) NOT NULL, asset_id varchar(36) NOT NULL,asset_type varchar(255) NOT NULL,push_date DATETIME, environment_id varchar(36) NOT NULL, );");
			dc.executeStatement("CREATE INDEX idx_pushed_assets_1 ON publishing_pushed_assets (bundle_id);");
			dc.executeStatement("CREATE INDEX idx_pushed_assets_2 ON publishing_pushed_assets (environment_id);");
			dc.executeStatement("alter table publishing_bundle add force_push tinyint ;");
		}else if(DbConnectionFactory.isOracle()) {
			dc.executeStatement("create table publishing_pushed_assets(bundle_id varchar2(36) NOT NULL, asset_id varchar2(36) NOT NULL, asset_type varchar2(255) NOT NULL, push_date TIMESTAMP, environment_id varchar2(36) NOT NULL)");
			dc.executeStatement("CREATE INDEX idx_pushed_assets_1 ON publishing_pushed_assets (bundle_id)");
			dc.executeStatement("CREATE INDEX idx_pushed_assets_2 ON publishing_pushed_assets (environment_id)");
			dc.executeStatement("alter table publishing_bundle add force_push number(1,0)");
		}else if(DbConnectionFactory.isMySql()) {
			dc.executeStatement("create table publishing_pushed_assets(bundle_id varchar(36) NOT NULL,asset_id varchar(36) NOT NULL,asset_type varchar(255) NOT NULL, push_date DATETIME, environment_id varchar(36) NOT NULL);");
			dc.executeStatement("CREATE INDEX idx_pushed_assets_1 ON publishing_pushed_assets (bundle_id);");
			dc.executeStatement("CREATE INDEX idx_pushed_assets_2 ON publishing_pushed_assets (environment_id);");
			dc.executeStatement("alter table publishing_bundle add force_push varchar(1) ;");
		}else if(DbConnectionFactory.isPostgres()) {
			dc.executeStatement("create table publishing_pushed_assets(bundle_id varchar(36) NOT NULL,asset_id varchar(36) NOT NULL,asset_type varchar(255) NOT NULL, push_date TIMESTAMP, environment_id varchar(36) NOT NULL);");
			dc.executeStatement("CREATE INDEX idx_pushed_assets_1 ON publishing_pushed_assets (bundle_id);");
			dc.executeStatement("CREATE INDEX idx_pushed_assets_2 ON publishing_pushed_assets (environment_id);");
			dc.executeStatement("alter table publishing_bundle add force_push bool ;");
		}
	}

	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		try {
			DbConnectionFactory.getConnection().setAutoCommit(true);
		} catch (SQLException e) {
			throw new DotDataException(e.getMessage(), e);
		}
		try {
			DotConnect dc=new DotConnect();
			createPushedAssetsTable(dc);
		} catch (SQLException e) {
			throw new DotRuntimeException(e.getMessage(),e);
		}

	}

	@Override
	public boolean forceRun() {
		return true;
	}

}
