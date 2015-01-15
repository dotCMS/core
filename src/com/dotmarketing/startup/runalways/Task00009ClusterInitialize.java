package com.dotmarketing.startup.runalways;

import java.sql.Connection;
import java.sql.SQLException;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

public class Task00009ClusterInitialize implements StartupTask {

    @Override
    public boolean forceRun() {
    		Connection con = null;
		try {
			con = DbConnectionFactory.getDataSource("jdbc/dotCMSPool").getConnection();
		} catch (SQLException e1) {
			Logger.error(Task00009ClusterInitialize.class,e1.getMessage(),e1);
			return false;
		}
	    	try {
	    		
	    		DotConnect dc=new DotConnect();
	    		dc.setSQL("select * from sitelic");
	    		dc.loadResult(con);
	    		return true;
	    	}catch(Exception ex) {
	    		Logger.warn(this, "Autowire Cluster is not initializing as this looks like the system is upgrading or the sitelic table cannot be found");
	    		return false;
	    	}finally {
	    		try {
	    			con.close();
	    		} catch (Exception e) {
	    			Logger.error(this, e.getMessage(), e);
	    		}
	    	}
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        ClusterFactory.initialize();
    }

}
