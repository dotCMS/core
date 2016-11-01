package com.dotmarketing.startup.runonce;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

public class Task01080CreateModDateForMissingObjects implements StartupTask {

	private void addModDate(DotConnect dc) throws SQLException, DotDataException {
		if(DbConnectionFactory.isMsSql()) {
			dc.executeStatement("alter table folder add mod_date datetime null ;");
			dc.executeStatement("alter table structure add mod_date datetime null ;");
			dc.executeStatement("alter table category add mod_date datetime null ;");

			// verify if columns exists
			dc.setSQL("SELECT * FROM sys.columns WHERE Name = 'mod_date' AND OBJECT_ID = OBJECT_ID('workflow_scheme')");
			List<Map<String, Object>> results = dc.loadObjectResults();

			if(results.isEmpty()) {
				dc.executeStatement("alter table workflow_scheme add mod_date datetime null ;");
			}

			dc.executeStatement("update folder set mod_date = getdate(); ");
			dc.executeStatement("update structure set mod_date = getdate(); ");
			dc.executeStatement("update category set mod_date = getdate(); ");
			dc.executeStatement("update workflow_scheme set mod_date = getdate(); ");
		}else if(DbConnectionFactory.isOracle()) {
			dc.executeStatement("alter table folder add mod_date timestamp");
			dc.executeStatement("alter table structure add mod_date timestamp");
			dc.executeStatement("alter table category add mod_date timestamp");

			// verify if columns exists
			dc.setSQL("SELECT * FROM USER_TAB_COLUMNS where table_name = 'WORKFLOW_SCHEME' and column_name = 'MOD_DATE'");
			List<Map<String, Object>> results = dc.loadObjectResults();

			if(results.isEmpty()) {
				dc.executeStatement("alter table workflow_scheme add mod_date timestamp");
			}

			dc.executeStatement("update folder set mod_date = sysdate");
			dc.executeStatement("update structure set mod_date = sysdate");
			dc.executeStatement("update category set mod_date = sysdate");
			dc.executeStatement("update workflow_scheme set mod_date = sysdate");
		}else if(DbConnectionFactory.isMySql()) {
			dc.executeStatement("alter table folder add mod_date datetime");
			dc.executeStatement("alter table structure add mod_date datetime");
			dc.executeStatement("alter table category add mod_date datetime");

			// verify if columns exists
			dc.setSQL("select column_name from information_schema.columns where table_schema = ? and table_name='workflow_scheme' and column_name='mod_date'");
			dc.addParam(DbConnectionFactory.getConnection().getCatalog());
			List<Map<String, Object>> results = dc.loadObjectResults();

			if(results.isEmpty()) {
				dc.executeStatement("alter table workflow_scheme add mod_date datetime");
			}

			dc.executeStatement("update folder set mod_date = now();");
			dc.executeStatement("update structure set mod_date = now();");
			dc.executeStatement("update category set mod_date = now();");
			dc.executeStatement("update workflow_scheme set mod_date = now();");
		}else if(DbConnectionFactory.isPostgres()) {
			dc.executeStatement("alter table folder add mod_date timestamp");
			dc.executeStatement("alter table structure add mod_date timestamp");
			dc.executeStatement("alter table category add mod_date timestamp");

			// verify if columns exists
			dc.setSQL("select column_name from information_schema.columns where table_name='workflow_scheme' and column_name='mod_date'");
			List<Map<String, Object>> results = dc.loadObjectResults();

			if(results.isEmpty()) {
				dc.executeStatement("alter table workflow_scheme add mod_date timestamp");
			}

			dc.executeStatement("update folder set mod_date = now();");
			dc.executeStatement("update structure set mod_date = now();");
			dc.executeStatement("update category set mod_date = now();");
			dc.executeStatement("update workflow_scheme set mod_date = now();");

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
			addModDate(dc);
		} catch (SQLException e) {
			throw new DotRuntimeException(e.getMessage(),e);
		}

	}

	@Override
	public boolean forceRun() {
		return true;
	}

}
