package com.dotmarketing.startup.runonce;

import java.sql.Connection;
import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

public class Task01090AddWorkflowSchemeUniqueNameContraint implements StartupTask{

	final private String WORKFLOW_SCHEME_CONSTRAINT = "alter table workflow_scheme add constraint unique_workflow_scheme_name unique (name)";
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		Connection conn = null;
		DotConnect dc = new DotConnect();
		  try {
			conn = DbConnectionFactory.getConnection();
			dc.executeStatement(WORKFLOW_SCHEME_CONSTRAINT, conn);
			 
		} catch (SQLException e) {
			Logger.error(this, e.getMessage()+". Create different schemes with the same name is not allowed. Please change the workflow scheme names duplicates.",e);
		}
	}

	public boolean forceRun() {
		return true;
	}
}
