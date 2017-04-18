package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * Add the environmentId and publisher columns to the publishing_pushed_assets
 * @author Oswaldo Gallango
 *
 */
public class Task04110AddColumnsPublishingPushedAssetsTable implements StartupTask {

	private void updatePublishingPushedAssets(DotConnect dc) throws SQLException, DotDataException {
		if(DbConnectionFactory.isMsSql()) {
			dc.executeStatement("alter table publishing_pushed_assets add endpoint_ids nvarchar(max);");
			dc.executeStatement("alter table publishing_pushed_assets add publisher nvarchar(255);");
			
		}else if(DbConnectionFactory.isOracle()) {
			dc.executeStatement("alter table publishing_pushed_assets add endpoint_ids nclob;");
			dc.executeStatement("alter table publishing_pushed_assets add publisher varchar2(255);");
			
		}else if(DbConnectionFactory.isMySql()) {
			dc.executeStatement("alter table publishing_pushed_assets add endpoint_ids longtext;");
			dc.executeStatement("alter table publishing_pushed_assets add publisher varchar(255);");
			
		}else  {
			dc.executeStatement("alter table publishing_pushed_assets add endpoint_ids text;");
			dc.executeStatement("alter table publishing_pushed_assets add publisher varchar(255);");
			
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
			updatePublishingPushedAssets(dc);
		} catch (SQLException e) {
			throw new DotRuntimeException(e.getMessage(),e);
		}

	}

	@Override
	public boolean forceRun() {
		return true;
	}

}