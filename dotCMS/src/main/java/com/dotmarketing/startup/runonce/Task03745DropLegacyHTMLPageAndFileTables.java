package com.dotmarketing.startup.runonce;

import java.sql.SQLException;
import java.util.*;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

public class Task03745DropLegacyHTMLPageAndFileTables implements StartupTask {
	
	private List<String> tablesToDrop = Arrays.asList("htmlpage_version_info","htmlpage", "fileasset_version_info", "file_asset");

	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        DotConnect dc=new DotConnect();
        for(String table : tablesToDrop){
        	try {
				dc.executeStatement("drop table " + table);
			} catch (SQLException e) {
				throw new DotRuntimeException(e.getMessage(),e);
			}
        }
		
	}

}
