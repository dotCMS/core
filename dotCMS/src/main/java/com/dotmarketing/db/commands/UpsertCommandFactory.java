package com.dotmarketing.db.commands;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory class for the UpsertCommand
 * @author andrecurione
 */
public class UpsertCommandFactory {

    /**
     * Map that contains Upsert Command implementations
     */
    private static Map<DbType, UpsertCommand> commandMap = new ConcurrentHashMap<>();

    static {
        commandMap.put(DbType.POSTGRESQL, new PostgreUpsertCommand());
        commandMap.put(DbType.MYSQL, new MySQLUpsertCommand());
        commandMap.put(DbType.MSSQL, new MSSQLUpsertCommand());
        commandMap.put(DbType.ORACLE, new OracleUpsertCommand());
    }

    //Hide the constructor
    private UpsertCommandFactory() { }

    public static UpsertCommand getUpsertCommand() {
        return commandMap.get(DbType.getDbType(DbConnectionFactory.getDBType()));
    }
}
