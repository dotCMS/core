package com.dotmarketing.quartz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.quartz.impl.jdbcjobstore.LockException;
import org.quartz.impl.jdbcjobstore.Util;

public class MySQLLockSemaphore extends org.quartz.impl.jdbcjobstore.DBSemaphore{
	
	public static final String SELECT_FOR_LOCK = "SELECT * FROM "
        + TABLE_PREFIX_SUBST.toLowerCase() + TABLE_LOCKS.toLowerCase() + " WHERE " + COL_LOCK_NAME.toLowerCase()
        + " = ? FOR UPDATE";
	
    public MySQLLockSemaphore() {
        super(DEFAULT_TABLE_PREFIX.toLowerCase(), null, SELECT_FOR_LOCK);
    }
    public MySQLLockSemaphore(String tablePrefix) {
        super(tablePrefix.toLowerCase(), null, SELECT_FOR_LOCK);
    }
    
    public MySQLLockSemaphore(String tablePrefix, String selectWithLockSQL) {
        super(tablePrefix.toLowerCase(), selectWithLockSQL.toLowerCase(), SELECT_FOR_LOCK);
    }

	@Override
	 /**
     * Execute the SQL select for update that will lock the proper database row.
     */
    protected void executeSQL(Connection conn, String lockName, String expandedSQL) throws LockException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(expandedSQL.toLowerCase());
            ps.setString(1, lockName);
            
            if (getLog().isDebugEnabled()) {
                getLog().debug(
                    "Lock '" + lockName + "' is being obtained: " + 
                    Thread.currentThread().getName());
            }
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new SQLException(Util.rtp(
                    "No row exists in table " + TABLE_PREFIX_SUBST + 
                    TABLE_LOCKS + " for lock named: " + lockName, getTablePrefix()));
            }
        } catch (SQLException sqle) {
            //Exception src =
            // (Exception)getThreadLocksObtainer().get(lockName);
            //if(src != null)
            //  src.printStackTrace();
            //else
            //  System.err.println("--- ***************** NO OBTAINER!");

            if (getLog().isDebugEnabled()) {
                getLog().debug(
                    "Lock '" + lockName + "' was not obtained by: " + 
                    Thread.currentThread().getName());
            }
            
            throw new LockException("Failure obtaining db row lock: "
                    + sqle.getMessage(), sqle);
        } finally {
            if (rs != null) { 
                try {
                    rs.close();
                } catch (Exception ignore) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    protected String getSelectWithLockSQL() {
        return getSQL().toLowerCase();
    }

    public void setSelectWithLockSQL(String selectWithLockSQL) {
        setSQL(selectWithLockSQL.toLowerCase());
    }

}
