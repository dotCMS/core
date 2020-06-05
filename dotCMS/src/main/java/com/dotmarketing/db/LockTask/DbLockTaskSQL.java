package com.dotmarketing.db.LockTask;

import com.dotmarketing.db.DbConnectionFactory;

public class DbLockTaskSQL {




    final String CREATE_LOCK_TABLE =
                    "CREATE TABLE db_lock_task( "
                    + "task_id varchar(255) NOT NULL,"
                    + "server_id varchar(255) NOT NULL, "
                    + "locked_until timestamp NOT NULL "
                    + "PRIMARY KEY (task_id)"
                    + ")";




    final String SELECT_LOCK =
                    "select *, " + DbConnectionFactory.getDBDateTimeFunction() + " as nowsers from db_lock_task where task_id=? and locked_until >  " + DbConnectionFactory.getDBDateTimeFunction();


    final String SELECT_MY_LOCK =
                    "select *, " + DbConnectionFactory.getDBDateTimeFunction() + " as nowsers from db_lock_task where task_id=? and server_id=? and locked_until >  " + DbConnectionFactory.getDBDateTimeFunction();


    

    final String SELECT_NOW =
                    "select " + DbConnectionFactory.getDBDateTimeFunction() + " as nowsers";


    final String DELETE_MY_LOCK = "delete from db_lock_task where task_id=? and server_id=?";
    
    final String INSERT_LOCK = "insert into db_lock_task (task_id, server_id, locked_until) values (?,?,?)";
    
    final String DELETE_OLD_LOCKS = "delete from db_lock_task where locked_until <  " + DbConnectionFactory.getDBDateTimeFunction();
    
    
    

}
