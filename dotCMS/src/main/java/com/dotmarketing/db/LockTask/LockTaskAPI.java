package com.dotmarketing.db.LockTask;

import java.sql.Connection;
import java.util.Date;
import java.util.Optional;
import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Try;

public class LockTaskAPI {

    
    private static class SQLHolder {
        static DbLockTaskSQL sql =new DbLockTaskSQL();

    }


    @VisibleForTesting
    protected LockTaskAPI(final String serverId) {
        this.myServerId = serverId;
        Try.run(() -> this.createLockTable());
    }



    private final String myServerId;

    private static class LockTaskAPIHolder {

        static LockTaskAPI lockTaskAPI = new LockTaskAPI(APILocator.getServerAPI().readServerId());

    }



    public static LockTaskAPI getInstance() {
        return LockTaskAPIHolder.lockTaskAPI;

    }

    /**
     * creates a distributed lock table if it does not exist
     * 
     * @throws DotDataException
     */
    private void createLockTable() throws DotDataException {
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()){

            new DotConnect().setSQL(SQLHolder.sql.CREATE_LOCK_TABLE).loadResult(conn);
            Logger.info(this.getClass(), "Creating DB Lock Table" );
        } catch (Exception e) {
            Logger.info(this.getClass(), "DB Lock Table Already Created:" + e);
        } 
    }

    /**
     * returns the current value of now() from the db - syntax is db specific
     * 
     * @return
     */
    private Date dbNow() {
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()){
           return (Date) new DotConnect().setSQL(SQLHolder.sql.SELECT_NOW).loadObjectResults(conn).get(0).get("nowsers");
        } catch (Exception e) {
           throw new DotRuntimeException(e);
        } 
    }


    /**
     * This method tries to aquire a lock on a specific task for the seconds specified. If a lock cannot
     * be aquired, it will return an empty Optional
     * 
     * @param taskToLock
     * @param secondsToHoldLock
     * @return
     */
    Optional<LockTask> lockTask(final String taskToLock, final int secondsToHoldLock) {

        final DotConnect db = new DotConnect();
        deleteOldLocks();
        Date future = new Date(dbNow().getTime() + (secondsToHoldLock * 1000));
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()){
            conn.setAutoCommit(false);
            db.setSQL(SQLHolder.sql.DELETE_MY_LOCK)
                .addParam(taskToLock)
                .addParam(myServerId)
                .loadResult(conn);
            db.setSQL(SQLHolder.sql.INSERT_LOCK)
                .addParam(taskToLock)
                .addParam(myServerId)
                .addParam(future)
                .loadResult(conn);
            conn.setAutoCommit(true);
            conn.commit();
            return Optional.of(new LockTask(
                            db.setSQL(SQLHolder.sql.SELECT_MY_LOCK).addParam(taskToLock).addParam(myServerId).loadObjectResults()));

        } catch (Exception e) {
            Logger.debug(this.getClass(),
                            "unable to obtain lock:" + taskToLock + "," + myServerId + "," + secondsToHoldLock, e);
        } 
        return Optional.empty();
    }

    
    /**
     * This method tries to aquire a lock on a specific task for the seconds specified. If a lock cannot
     * be aquired, it will return an empty Optional
     * 
     * @param taskToLock
     * @param secondsToHoldLock
     * @return
     */
    Optional<LockTask> waitForLock(final String taskToLock, final int secondsToHoldLock) {
        Optional<LockTask> task = lockTask(taskToLock,secondsToHoldLock);
        long message=0;
        while(!task.isPresent()) {
            if(message++ % 10 ==0) {
                Logger.info(this.getClass(), "Waiting for lock :" + taskToLock + " " + message + " seconds ");
            }
            Try.run(()->Thread.sleep(1000));
            task = lockTask(taskToLock,secondsToHoldLock);
        }
        return task;
        
    }
    
    
    
    
    
    /**
     * deletes expired locks
     */
    @CloseDBIfOpened
    private void deleteOldLocks() {

        Try.run(() -> new DotConnect().setSQL(SQLHolder.sql.DELETE_OLD_LOCKS).loadResult())
                        .getOrElseThrow(e -> new DotRuntimeException(e));


    }


    /**
     * This will unlock the given task if it is owned by this server
     * 
     * @param taskToUnLock
     * @return
     */
    boolean unlockTask(String taskToUnLock) {

        final DotConnect db = new DotConnect();

        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()){
            conn.setAutoCommit(false);
            db.setSQL(SQLHolder.sql.DELETE_MY_LOCK).addParam(taskToUnLock).addParam(myServerId).loadResult(conn);
            conn.commit();
            conn.setAutoCommit(true);
            return true;

        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), "unable to unlock Task:" + taskToUnLock + "," + myServerId, e);

        } 
        return false;
    }



}
