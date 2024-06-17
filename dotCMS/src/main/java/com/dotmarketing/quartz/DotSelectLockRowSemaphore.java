package com.dotmarketing.quartz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.quartz.impl.jdbcjobstore.DBSemaphore;
import org.quartz.impl.jdbcjobstore.LockException;
import org.quartz.impl.jdbcjobstore.Util;
import org.quartz.impl.jdbcjobstore.TablePrefixAware;
import org.quartz.impl.jdbcjobstore.Constants;

public class DotSelectLockRowSemaphore extends DBSemaphore {

    public static final String SELECT_FOR_LOCK = "SELECT * FROM "
            + TABLE_PREFIX_SUBST + TABLE_LOCKS + " WHERE " + COL_LOCK_NAME
            + " = ? FOR UPDATE";
    public static final String INSERT_LOCK = "INSERT INTO "
            + TABLE_PREFIX_SUBST + TABLE_LOCKS + " (" + COL_LOCK_NAME + ") VALUES (?)";

    public DotSelectLockRowSemaphore() {
        super(DEFAULT_TABLE_PREFIX, null, SELECT_FOR_LOCK, INSERT_LOCK);
    }

    public DotSelectLockRowSemaphore(String tablePrefix) {
        super(tablePrefix, null, SELECT_FOR_LOCK, INSERT_LOCK);
    }

    public DotSelectLockRowSemaphore(String tablePrefix, String schedName) {
        super(tablePrefix, schedName, SELECT_FOR_LOCK, INSERT_LOCK);
    }

    public DotSelectLockRowSemaphore(String tablePrefix, String schedName, String selectWithLockSQL, String insertSQL) {
        super(tablePrefix, schedName, selectWithLockSQL, insertSQL);
    }

    @Override
    protected void executeSQL(Connection conn, String lockName, String theExpandedSQL, String theExpandedInsertSQL)
            throws LockException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(theExpandedSQL);
            ps.setString(1, lockName);

            if (getLog().isDebugEnabled()) {
                getLog().debug(
                        "Lock '" + lockName + "' is being obtained: " +
                                Thread.currentThread().getName());
            }
            rs = ps.executeQuery();
            if (!rs.next()) {
                // Insert the lock row if it does not exist
                ps = conn.prepareStatement(theExpandedInsertSQL);
                ps.setString(1, lockName);
                ps.executeUpdate();

                ps = conn.prepareStatement(theExpandedSQL);
                ps.setString(1, lockName);
                rs = ps.executeQuery();
                if (!rs.next()) {
                    throw new SQLException(Util.rtp(
                            "No row exists in table " + TABLE_PREFIX_SUBST +
                                    TABLE_LOCKS + " for lock named: " + lockName, getTablePrefix(), getSchedName()));
                }
            }
        } catch (SQLException sqle) {
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
        return getSQL();
    }

    public void setSelectWithLockSQL(String selectWithLockSQL) {
        setSQL(selectWithLockSQL);
    }
}