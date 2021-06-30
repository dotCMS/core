package com.dotmarketing.startup.runonce;


import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;


/**
 * Add the environmentId and publisher columns to the publishing_pushed_assets
 * @author Oswaldo Gallango
 *
 */
public class Task04110AddColumnsPublishingPushedAssetsTable extends AbstractJDBCStartupTask {

	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public String getPostgresScript() {
		return "alter table publishing_pushed_assets add endpoint_ids text;"+
				"alter table publishing_pushed_assets add publisher text;";
	}

	@Override
	public String getMySQLScript() {
		return "alter table publishing_pushed_assets add endpoint_ids longtext;"+
				"alter table publishing_pushed_assets add publisher longtext;";
	}

	@Override
	public String getMSSQLScript() {
		return "alter table publishing_pushed_assets add endpoint_ids nvarchar(max);"+
				"alter table publishing_pushed_assets add publisher nvarchar(max);";
	}

	@Override
	public String getOracleScript() {
		return "alter table publishing_pushed_assets add endpoint_ids nclob;"+
				"alter table publishing_pushed_assets add publisher nclob;";
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

}