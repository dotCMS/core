package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.startup.StartupTask;

/**
 * This task updates the page detail of the structures where there is no url map pattern.
 * Issue https://my.dotcms.com/ticket/dotcms-55/ item #1
 * 
 * @author Erick Gonzalez
 * @version 1.0
 * @since 7-8-2015
 *
 */
public class Task03135FixStructurePageDetail extends AbstractJDBCStartupTask {
	
	private static final String SQL_UPDATE_STRUCTURE_BY_URL_MAP = "UPDATE structure set page_detail = null where url_map_pattern is null and page_detail is not null";

	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public String getPostgresScript() {
		return SQL_UPDATE_STRUCTURE_BY_URL_MAP;
	}

	@Override
	public String getMySQLScript() {
		return SQL_UPDATE_STRUCTURE_BY_URL_MAP;
	}

	@Override
	public String getOracleScript() {
		return SQL_UPDATE_STRUCTURE_BY_URL_MAP;
	}

	@Override
	public String getMSSQLScript() {
		return SQL_UPDATE_STRUCTURE_BY_URL_MAP;
	}

	@Override
	public String getH2Script() {
		return SQL_UPDATE_STRUCTURE_BY_URL_MAP;
	}

	protected List getTablesToDropConstraints() {
		return null;
	}

}
