package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

public class Task01096CreateContainerStructuresTable implements StartupTask {

	@Override
	public boolean forceRun() {
		return true;
	}


	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {

		try {
			DbConnectionFactory.getConnection().setAutoCommit(true);
			DotConnect dc=new DotConnect();

			try {
				dc.executeStatement("drop table container_structures");
			} catch (SQLException e) {
				Logger.info(getClass(), "container_structures table does not exist. Will be created.");
			}

			String createTable = "Create table container_structures" +
					"(id varchar(36) NOT NULL  primary key," +
					"container_id varchar(36) NOT NULL," +
					"structure_id varchar(36) NOT NULL, "
					+ "code text)";

			if(DbConnectionFactory.isOracle()) {
				createTable=createTable.replaceAll("varchar\\(", "varchar2\\(");
				createTable=createTable.replaceAll("text", "nclob");
			} else if(DbConnectionFactory.isMySql()) {
				createTable=createTable.replaceAll("text", "longtext");
			}

			dc.executeStatement(createTable);

		} catch (SQLException e) {
			throw new DotDataException(e.getMessage(), e);
		}

	}

}
