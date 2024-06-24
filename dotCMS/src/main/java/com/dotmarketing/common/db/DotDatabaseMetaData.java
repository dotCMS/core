package com.dotmarketing.common.db;

import com.dotcms.util.CloseUtils;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encapsulates database metada operations operations.
 * @author jsanca
 */
public class DotDatabaseMetaData {

    public static final String ALTER_TABLE = "ALTER TABLE ";
    public static final String DROP_FOREIGN_KEY = " DROP FOREIGN KEY ";
    public static final String DROP_INDEX = " DROP INDEX ";

    /**
     * Find the foreign key on the table, with the primary keys and columns assigned.
     *
     * Consider the following constraint:
     *
     * ALTER  TABLE my_table ADD CONSTRAINT fk_table_id   foreign key (third_table_id)   references third_table  (id);
     *
     * In this sentence, each parameter will be
     *
     * foreignKeyTableName   = my_table
     * primaryKeyTableName   = third_table
     * foreignKeyColumnNames = third_table_id (as much as you have)
     * primaryKeyColumnNames = id (as much as you have)
     *
     * @param foreignKeyTableName String
     * @param primaryKeyTableName String
     * @param foreignKeyColumnNames List
     * @param primaryKeyColumnNames List
     * @return The foreign keys associated to the table, null of does not exists.
     */
    public ForeignKey findForeignKeys(final String foreignKeyTableName,
                                           final String primaryKeyTableName,
                                           final List<String> foreignKeyColumnNames,
                                           final List<String> primaryKeyColumnNames) {

        return this.findForeignKeys(DbConnectionFactory.getConnection(),
                foreignKeyTableName, primaryKeyTableName,
                foreignKeyColumnNames, primaryKeyColumnNames);
    }

    /**
     * Find the foreign key on the table, with the primary keys and columns assigned.
     *
     * Consider the following constraint:
     *
     * ALTER  TABLE my_table ADD CONSTRAINT fk_table_id   foreign key (third_table_id)   references third_table  (id);
     *
     * In this sentence, each parameter will be
     *
     * foreignKeyTableName   = my_table
     * primaryKeyTableName   = third_table
     * foreignKeyColumnNames = third_table_id (as much as you have)
     * primaryKeyColumnNames = id (as much as you have)
     *
     * @param connection          {@link Connection}
     * @param foreignKeyTableName String
     * @param primaryKeyTableName String
     * @param foreignKeyColumnNames List
     * @param primaryKeyColumnNames List
     *
     * @return The foreign keys associated to the table, null of does not exists.
     */
    public ForeignKey findForeignKeys(final Connection connection,
                                      final String foreignKeyTableName,
                                      final String primaryKeyTableName,
                                      final List<String> foreignKeyColumnNames,
                                      final List<String> primaryKeyColumnNames) {

        ForeignKey foundForeignKey = null;
        final List<ForeignKey> foreignKeys = this.getForeignKeys(connection, foreignKeyTableName);

        Logger.info(this, "**Printing foreign keys");

        for (final ForeignKey foreignKey : foreignKeys) {
            System.out.println("foreignKey = " + foreignKey);

            if (primaryKeyTableName.equalsIgnoreCase(foreignKey.getPrimaryKeyTableName()) &&
                    foreignKeyTableName.equalsIgnoreCase(foreignKey.getForeignKeyTableName()) &&
                    checkIfMatchAllPrimaryKeys(primaryKeyColumnNames, foreignKey) &&
                    checkIfMatchAllForeighKeys(foreignKeyColumnNames, foreignKey)) {

                foundForeignKey = foreignKey;
                break;
            }
        }

        return foundForeignKey;
    }

    private boolean checkIfMatchAllPrimaryKeys(final List<String> primaryKeyColumnNames, final ForeignKey foreignKey) {

        boolean allMatch = false;
        if (primaryKeyColumnNames.size() == foreignKey.getPrimaryKeyColumnNames().size()) {

            for (String primaryKeyColumnName : primaryKeyColumnNames) {

                if (!(foreignKey.getPrimaryKeyColumnNames().contains(primaryKeyColumnName) ||
                        foreignKey.getPrimaryKeyColumnNames().contains(primaryKeyColumnName.toUpperCase()))) {
                    return false;
                }
            }

            allMatch = true;
        }

        return allMatch;
    }

    private boolean checkIfMatchAllForeighKeys(final List<String> foreignKeyColumnNames, final ForeignKey foreignKey) {

        boolean allMatch = false;
        if (foreignKeyColumnNames.size() == foreignKey.getForeignKeyColumnNames().size()) {

            for (String foreignKeyColumnName : foreignKeyColumnNames) {

                if (!(foreignKey.getForeignKeyColumnNames().contains(foreignKeyColumnName) ||
                        foreignKey.getForeignKeyColumnNames().contains(foreignKeyColumnName.toUpperCase()))) {
                    return false;
                }
            }

            allMatch = true;
        }

        return allMatch;
    }

    /**
     * Reads all the foreign keys associated to every table in the specified
     * list.
     *
     * @param tables
     *            - The list of database tables whose foreign keys will be
     *            retrieved.
     *
     * @return The list of foreign keys associated to the tables.
     */

    public List<ForeignKey> getForeignKeys(final String... tables) {

        return getForeignKeys(DbConnectionFactory.getConnection(), tables);
    }

    /**
     * Reads all the foreign keys associated to every table in the specified
     * list.
     *
     * @param conn
     *            - The database connection object to access the dotCMS data.
     * @param tables
     *            - The list of database tables whose foreign keys will be
     *            retrieved.
     *
     * @return The list of foreign keys associated to the tables.
     */
    public List<ForeignKey> getForeignKeys(final Connection conn, final String... tables) {

        final List<ForeignKey> foreignKeys = new ArrayList<>();
        final Map<String, ForeignKey> foreignKeyMap = new HashMap<>();
        DatabaseMetaData databaseMetaData     = null;
        ResultSet        resultSet            = null;
        ForeignKey       foreignKey           = null;
        String           schema               = null;

        try {

            databaseMetaData = conn.getMetaData();

            for (String table : tables) {

                if (DbConnectionFactory.isOracle()) {
                    table = table.toUpperCase();
                    schema = databaseMetaData.getUserName();
                }

                resultSet = databaseMetaData.getImportedKeys
                        (conn.getCatalog(), schema, table);

                // Iterates over the foreign foreignKey columns
                while (resultSet.next()) {

                    foreignKey = new ForeignKey(resultSet.getString("PKTABLE_NAME"),
                            resultSet.getString("FKTABLE_NAME"),
                            resultSet.getString("FK_NAME")
                            );

                    final String pkColumnName = resultSet.getString("PKCOLUMN_NAME");
                    final String fkColumnName = resultSet.getString("FKCOLUMN_NAME");

                    foreignKey.addPrimaryColumnName(pkColumnName);
                    foreignKey.addForeignColumnName(fkColumnName);

                    if (!foreignKeys.contains(foreignKey)) {
                        foreignKeys.add(foreignKey);
                        foreignKeyMap.put(foreignKey.fkName(), foreignKey);
                    } else {
                        if (DbConnectionFactory.isMsSql()) {
                            // The FK has more than one column as part of the
                            // constraint. Add the column to the same object
                            // instead of duplicating an entry in the list.
                            ForeignKey existingKey = foreignKeyMap.get(foreignKey.fkName());
                            existingKey.addPrimaryColumnName(pkColumnName);
                            existingKey.addForeignColumnName(fkColumnName);
                        }
                    }

                }
            }
        } catch (SQLException e) {
            Logger.error(this,
                    "An error occurred when getting the the foreign keys: " + e.getMessage(), e);
        }

        return Collections.unmodifiableList(foreignKeys);
    } // getForeignKeys.

    /**
     * Drops a foreign key
     * @param foreignKey {@link ForeignKey}
     * @throws SQLException
     */
    public void dropForeignKey (final ForeignKey foreignKey) throws SQLException {

        this.dropForeignKey(DbConnectionFactory.getConnection(), foreignKey);
    } // dropForeignKey.

    /**
     * Drops a foreign key
     * @param conn {@link Connection}
     * @param foreignKey {@link ForeignKey}
     * @throws SQLException
     */
    public void dropForeignKey (final Connection conn, final ForeignKey foreignKey) throws SQLException {
        if(DbConnectionFactory.isPostgres() ||
                DbConnectionFactory.isMsSql() ||
                DbConnectionFactory.isOracle()) {

            this.executeDropConstraint(conn, foreignKey.getForeignKeyTableName(), foreignKey.getForeignKeyName());
        } else if (DbConnectionFactory.isMySql()) {
            executeDropForeignKeyMySql(conn, foreignKey.getForeignKeyTableName(), foreignKey.getForeignKeyName());
        }
    } // dropForeignKey.

    /**
     * Drops the specified constraint associated to the given table.
     *
     * @param conn
     *            - The database connection object to access the dotCMS data.
     * @param tableName
     *            - The name of the table whose constraint will be dropped.
     * @param constraintName
     *            - The name of the constraint that will be dropped.
     * @throws SQLException
     *             An error occurred when executing the SQL query.
     */
    public void executeDropConstraint(final Connection conn, final String tableName, final String constraintName) throws SQLException {

        String sql = StringPool.BLANK;
        PreparedStatement preparedStatement = null;

        if(DbConnectionFactory.isMySql()) {
            sql = (constraintName.indexOf("PRIMARY")>-1)?
                ALTER_TABLE + tableName + " DROP PRIMARY KEY ":
                ALTER_TABLE + tableName + " DROP INDEX " + constraintName;
        }  else {
            sql = ALTER_TABLE + tableName + " DROP CONSTRAINT " + constraintName;
        }

        try {
            preparedStatement = conn.prepareStatement(sql);
            Logger.info(this, "Executing: " + sql);
            preparedStatement.execute();
        }  catch (SQLException e) {
            Logger.error(this, "Error executing: " + sql);
            throw e;
        } finally {
            CloseUtils.closeQuietly(preparedStatement);
        }
    } // executeDropConstraint.


    /**
     * Drops the specified foreign key associated to the given table <b>in a
     * MySQL database</b>.
     *
     * @param conn
     *            - The database connection object to access the dotCMS data.
     * @param tableName
     *            - The name of the table whose foreign key will be dropped.
     * @param constraintName
     *            - The name of the foreign key that will be dropped.
     * @throws SQLException
     *             An error occurred when executing the SQL query.
     */
    public void executeDropForeignKeyMySql(final Connection conn, final String tableName, final String constraintName) throws SQLException {

        PreparedStatement preparedStatement = null;
        try {

            preparedStatement = conn.prepareStatement(ALTER_TABLE + tableName + DROP_FOREIGN_KEY + constraintName);
            Logger.info(this, "Executing: " + ALTER_TABLE + tableName + DROP_FOREIGN_KEY + constraintName);
            preparedStatement.execute();
        } catch (SQLException e) {
            Logger.error(this, "Error executing: " + ALTER_TABLE + tableName + DROP_FOREIGN_KEY + constraintName + " - NOT A FOREIGN KEY.");
            throw e;
        } finally {
            CloseUtils.closeQuietly(preparedStatement);
        }
    } // executeDropForeignKeyMySql.

    /**
     * Returns true if the table already exists
     * @param connection {@link Connection}
     * @param tableName  {@link String}
     * @return Boolean true if exists, otherwise false
     * @throws SQLException
     */
    public boolean tableExists(final Connection connection, final String tableName) throws SQLException {

        boolean exists                        = false;
        DatabaseMetaData databaseMetaData     = null;
        ResultSet        resultSet            = null;
        String           schema               = null;
        String           table                = tableName;

        try {

            databaseMetaData = connection.getMetaData();

            if (DbConnectionFactory.isOracle()) {
                table = table.toUpperCase();
                schema = databaseMetaData.getUserName();
            }

            resultSet = databaseMetaData.getTables
                    (connection.getCatalog(), schema, table, null);

            // Iterates over the foreign foreignKey columns
            exists = resultSet.next();
        } catch (SQLException e) {
            Logger.error(this,
                    "An error occurred when getting the the table: " + e.getMessage(), e);
            throw e;
        }

        return exists;
    }

    public static ResultSet getColumnsMetaData(final Connection connection, final String tableName) throws SQLException {
        final DatabaseMetaData databaseMetaData = connection.getMetaData();
        String schema = null;
        String table = tableName;

        if (DbConnectionFactory.isOracle()) {
            table = table.toUpperCase();
            schema = databaseMetaData.getUserName();
        }

        return databaseMetaData.getColumns
                (connection.getCatalog(), schema, table, null);
    }


    /**
     * Returns the columns list of the table as a list
     * @param connection {@link Connection}
     * @param tableName  {@link String}
     * @return
     */
    public Set<String> getColumnNames (final Connection connection, final String tableName) throws SQLException {

        final Set<String> columns = new LinkedHashSet<>();
        ResultSet        resultSet            = null;

        try {

            resultSet = this.getColumnsMetaData(connection, tableName);

            // Iterates over the foreign foreignKey columns
            while (resultSet.next()) {

                columns.add(resultSet.getString("COLUMN_NAME"));

            }
        } catch (SQLException e) {
            Logger.error(this,
                    "An error occurred when getting the the column names: " + e.getMessage(), e);
            throw e;
        }

        return Collections.unmodifiableSet(columns);
    }


    /**
     * Drop the primary keys associated to a table, additionally returns the list of pk removed
     *
     * @param connection
     *            - The database connection object to access the dotCMS data.
     * @param tableName
     *            - database table name whose primary keys will be
     *            retrieved.
     *
     * @return The list of primary keys associated to the tables.
     */
    public PrimaryKey dropPrimaryKey(final Connection connection,
                                                 String tableName)  throws SQLException{

        PrimaryKey primaryKey = null;
        if (tableName!=null) {
            try {

                final DatabaseMetaData metaData = connection.getMetaData();

                String schema=null;
                if(DbConnectionFactory.isOracle()) {

                    tableName = tableName.toUpperCase();
                    schema    = metaData.getUserName();
                }

                System.out.println("metaData: " + metaData);
                final ResultSet resultSet = metaData.getPrimaryKeys(connection.getCatalog(), schema, tableName);
                String keyName            = null;
                final List<String> columnNames = new ArrayList<>();

                while (resultSet.next()) {
                    keyName = resultSet.getString("PK_NAME");
                    System.out.println("keyName: " + keyName);
                    columnNames.add(resultSet.getString("COLUMN_NAME"));
                }

                if (null != keyName) {
                    primaryKey = new PrimaryKey(tableName, keyName, columnNames);

                    try {
                        executeDropConstraint(connection, primaryKey.getTableName(), primaryKey.getKeyName());
                    } catch (Exception ex) {
                        if (primaryKey != null) {
                            Logger.error(this,
                                    "Drop primary key '" + primaryKey.getKeyName() + "' failed on table '" + primaryKey.getTableName() + "'", ex);
                        }
                    }
                }
            } catch (SQLException e) {
                Logger.error(AbstractJDBCStartupTask.class,
                        "An error occurred when processing the primary keys : " + e.getMessage(), e);
                throw e;
            }
        }

        return primaryKey;
    }

    /**
     * Drops the column depending on the db
     * @param connection {@link Connection}
     * @param tableName {@link String}
     * @param columnName {@link String}
     * @throws SQLException
     */ 
    public void dropColumn(final Connection connection, final String tableName, final String columnName) throws SQLException {

        if (DbConnectionFactory.isMySql()) {
            // Drop column constraints
            this.dropColumnMySQLDependencies(connection, tableName, columnName);
        } else if (DbConnectionFactory.isMsSql()) {
            this.dropColumnMSSQLDependencies(connection, tableName, columnName);
        }

        // Drop the column:
        connection.createStatement().executeUpdate("ALTER TABLE " + tableName + " DROP COLUMN " + columnName); // Drop the column
    }

    /**
     * Drop the table
     * @param connection
     * @param systemTable
     * @throws SQLException
     */
    public void dropTable(final Connection connection, final String systemTable) throws SQLException {

        new DotConnect().executeStatement("DROP TABLE " + systemTable, connection);
    }

    private void dropColumnMySQLDependencies(final Connection connection, final String tableName, final String columnName) throws SQLException {

        try (final ResultSet resultSet = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
                .executeQuery("select CONSTRAINT_NAME from INFORMATION_SCHEMA.KEY_COLUMN_USAGE where CONSTRAINT_SCHEMA = SCHEMA() and TABLE_NAME = '" +
                        tableName + "' and COLUMN_NAME = '" + columnName + "'")) {

            while (resultSet.next()) {
                final String constraintName = resultSet.getString("CONSTRAINT_NAME");
                this.executeDropConstraint(connection, tableName, constraintName);
            }
        }

        // Drop column indexes
        try (final ResultSet resultSet = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
                .executeQuery("SHOW INDEX FROM " + tableName + " where column_name = '" + columnName + "'")) {

            while (resultSet.next()) {
                final String keyName = resultSet.getString("Key_name");
                this.dropIndex(tableName, keyName);
            }
        }
    }

    private void dropColumnMSSQLDependencies(final Connection connection, final String tableName, final String columnName) throws SQLException {

        final List<String>  constraints = getColumnConstraintsMSSQL(connection, tableName, columnName);
        if (UtilMethods.isSet(constraints)) {

            for (final String constraintName : constraints) {
                this.executeDropConstraint(connection, tableName, constraintName);
            }
        }
    }

    private List<String> getColumnConstraintsMSSQL (final Connection connection, final String tableName, final String columnName) throws SQLException {

        final List<String>  constraints = new ArrayList<>();

        try (final Statement statement = connection.createStatement()) {
            try (final ResultSet resultSet = statement.executeQuery("SELECT default_constraints.name FROM sys.all_columns INNER JOIN sys.tables\n" +
                    "        ON all_columns.object_id = tables.object_id\n" +
                    "        INNER JOIN sys.schemas\n" +
                    "        ON tables.schema_id = schemas.schema_id\n" +
                    "        INNER JOIN sys.default_constraints\n" +
                    "        ON all_columns.default_object_id = default_constraints.object_id\n" +
                    "WHERE tables.name = '" + tableName + "' AND all_columns.name = '" + columnName + "'")) {

                while (resultSet.next()) {

                    constraints.add(resultSet.getString(1));
                }
            }
        }

        return constraints;
    }

    public void dropIndex(final String tableName, final String indexName) throws SQLException {
        dropIndex(DbConnectionFactory.getConnection(), tableName, indexName);
    }

    public void dropIndex(final Connection connection,
            final String tableName, final String indexName) throws SQLException {
        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());

        if(dbType == DbType.MYSQL){
            new DotConnect().executeStatement(String.format("%s %s %s %s",ALTER_TABLE,tableName,DROP_INDEX,indexName), connection);
            return;
        }

        if(dbType == DbType.MSSQL){
            new DotConnect().executeStatement(String.format("%s %s.%s",DROP_INDEX, tableName, indexName), connection);
            return;
        }

        new DotConnect().executeStatement(String.format("%s %s",DROP_INDEX, indexName), connection);

    }

    public List<String> getConstraints(final String tableName) throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        if (DbConnectionFactory.isPostgres()) {
            return getConstraintsPostgres(tableName, dotConnect);
        }

        if (DbConnectionFactory.isOracle()) {
            return getConstraintsOracleSQL(tableName, dotConnect);
        }

        if (DbConnectionFactory.isMsSql()) {
            return getConstraintsMSSQL(tableName, dotConnect);
        }

        if (DbConnectionFactory.isMySql()) {
            return getConstraintsMySQL(tableName, dotConnect);
        }

        throw new DotDataException("Unknown database type.");
    }

    public List<String> getIndices(final String tableName) throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        if (DbConnectionFactory.isMsSql()) {
            return getIndicesMSSQL(tableName, dotConnect);
        }
        throw new UnsupportedOperationException("This Operation isn't supported for the current database type");
    }

    /**
     * Returns the list of Indices for a given database table.
     *
     * @param conn              The current database {@link Connection} object, retrievable via the
     *                          {@link DbConnectionFactory#getConnection()}.
     * @param schema            The schema name for the database. If not necessary, it can be {@code null}.
     * @param table             The table name.
     * @param uniqueIndicesOnly Indicates if only unique indices should be returned.
     * @return A {@link ResultSet} object containing the list of indices for the given table.
     * @throws SQLException If an error occurs while retrieving the indices.
     */
    public ResultSet getIndices(final Connection conn, final String schema, final String table, final boolean uniqueIndicesOnly) throws SQLException {
        final DatabaseMetaData dbMetadata = conn.getMetaData();
        return dbMetadata.getIndexInfo(conn.getCatalog(), schema, table, uniqueIndicesOnly, false);
    }

    /**
     * Indicates if a column exists on a given table
     * @param tableName
     * @param columnName
     * @return - boolean indicating if the column exists on the referenced table
     * @throws SQLException
     */
    public boolean hasColumn(final String tableName, final String columnName) throws SQLException {
       return this.getColumnNames(DbConnectionFactory.getConnection(), tableName).stream()
                .anyMatch(dbcolumn -> dbcolumn.equalsIgnoreCase(columnName));
    }

    private static String POSTGRES_CONSTRAINT_SQL =
            " SELECT con.*\n"
            + "       FROM pg_catalog.pg_constraint con\n"
            + "            INNER JOIN pg_catalog.pg_class rel\n"
            + "                       ON rel.oid = con.conrelid\n"
            + "            INNER JOIN pg_catalog.pg_namespace nsp\n"
            + "                       ON nsp.oid = connamespace\n"
            + "       WHERE  nsp.nspname = 'public' \n"
            + "             AND rel.relname = '%s' ";

    private List<String> getConstraintsPostgres(final String tableName, final DotConnect dotConnect) throws DotDataException {
        dotConnect.setSQL(String.format(POSTGRES_CONSTRAINT_SQL, tableName));
        final List<Map> constraintsMeta = dotConnect.loadResults();
        return constraintsMeta.stream().map(map -> map.get("conname").toString()).collect(Collectors.toList());
    }

    private static final String MYSQL_CONSTRAINT_SQL =
            " SELECT column_name, constraint_name \n"
            + " FROM information_schema.KEY_COLUMN_USAGE\n"
            + " WHERE TABLE_NAME = 'workflow_task'";

    private List<String> getConstraintsMySQL(final String tableName, final DotConnect dotConnect) throws DotDataException {
        dotConnect.setSQL(String.format(MYSQL_CONSTRAINT_SQL, tableName));
        final List<Map> constraintsMeta = dotConnect.loadResults();
        return constraintsMeta.stream().map(map -> map.get("constraint_name").toString()).collect(Collectors.toList());
    }

    private static final String ORACLE_CONSTRAINT_SQL = "SELECT * FROM ALL_CONSTRAINTS WHERE table_name = '%s' ";

    private List<String> getConstraintsOracleSQL(final String tableName, final DotConnect dotConnect) throws DotDataException {
        dotConnect.setSQL(String.format(ORACLE_CONSTRAINT_SQL, tableName.toUpperCase()));
        final List<Map> constraintsMeta = dotConnect.loadResults();
        return constraintsMeta.stream().map(map -> map.get("constraint_name").toString()).collect(Collectors.toList());
    }

    private static final String MS_SQL_INDEX = "select  t.[name] as table_view, \n"
            + "    case when t.[type] = 'U' then 'Table'\n"
            + "        when t.[type] = 'V' then 'View'\n"
            + "        end as [object_type],\n"
            + "    i.index_id,\n"
            + "    case when i.is_primary_key = 1 then 'Primary key'\n"
            + "        when i.is_unique = 1 then 'Unique'\n"
            + "        else 'Not unique' end as [type],\n"
            + "    i.[name] as index_name,\n"
            + "    substring(column_names, 1, len(column_names)-1) as [columns],\n"
            + "    case when i.[type] = 1 then 'Clustered index'\n"
            + "        when i.[type] = 2 then 'Nonclustered unique index'\n"
            + "        when i.[type] = 3 then 'XML index'\n"
            + "        when i.[type] = 4 then 'Spatial index'\n"
            + "        when i.[type] = 5 then 'Clustered columnstore index'\n"
            + "        when i.[type] = 6 then 'Nonclustered columnstore index'\n"
            + "        when i.[type] = 7 then 'Nonclustered hash index'\n"
            + "        end as index_type\n"
            + "from sys.objects t\n"
            + "    inner join sys.indexes i\n"
            + "        on t.object_id = i.object_id\n"
            + "    cross apply (select col.[name] + ', '\n"
            + "                    from sys.index_columns ic\n"
            + "                        inner join sys.columns col\n"
            + "                            on ic.object_id = col.object_id\n"
            + "                            and ic.column_id = col.column_id\n"
            + "                    where ic.object_id = t.object_id\n"
            + "                        and ic.index_id = i.index_id\n"
            + "                            order by col.column_id\n"
            + "                            for xml path ('') ) D (column_names)\n"
            + "where t.is_ms_shipped <> 1\n"
            + "and index_id > 0 and  t.[name] = '%s'\n"
            + "order by schema_name(t.schema_id) + '.' + t.[name], i.index_id";


    private static final String MS_SQL_CONSTRAINTS = "\n"
            + "SELECT default_constraints.name FROM sys.all_columns INNER JOIN sys.tables\n"
            + "  ON all_columns.object_id = tables.object_id\n"
            + "  INNER JOIN sys.schemas\n"
            + "  ON tables.schema_id = schemas.schema_id\n"
            + "  INNER JOIN sys.default_constraints \n"
            + "  ON all_columns.default_object_id = default_constraints.object_id \n"
            + " WHERE tables.name = '%s'";

    private List<String> getConstraintsMSSQL(final String tableName, final DotConnect dotConnect) throws DotDataException {
        dotConnect.setSQL(String.format(MS_SQL_CONSTRAINTS,tableName));
        final List<Map> constraintsMeta = dotConnect.loadResults();
        return constraintsMeta.stream().map(map -> map.get("name").toString()).collect(Collectors.toList());
    }

    private List<String> getIndicesMSSQL(final String tableName, final DotConnect dotConnect) throws DotDataException {
        dotConnect.setSQL(String.format(MS_SQL_INDEX, tableName));
        final List<Map> indexMeta = dotConnect.loadResults();
        return indexMeta.stream().map(map -> map.get("index_name").toString()).collect(Collectors.toList());
    }


    /**
     * Return a list of the fields that make up the table primary key
     *
     * @param tableName
     * @return
     */
    public static List<String> getPrimaryKeysFields(final String tableName)  {
        try {
            final Connection connection = DbConnectionFactory.getConnection();
            final ResultSet pkColumns = connection.getMetaData()
                    .getPrimaryKeys(null, null, tableName);

            List<String> primaryKeysFields = new ArrayList();

            while (pkColumns.next()) {
                primaryKeysFields.add(pkColumns.getString("COLUMN_NAME"));
            }

            return primaryKeysFields;
        }catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

    public static void dropPrimaryKey(final String tableName){
        final Optional<String> constraintNameOptional = getPrimaryKeyName(tableName);

        if (constraintNameOptional.isPresent()) {
            try {
                new DotConnect().executeStatement(
                        String.format("ALTER TABLE %s DROP CONSTRAINT %s", tableName,
                                constraintNameOptional.get()));

            } catch (Exception e) {
                throw new DotRuntimeException(e);
            }
        }
    }

    public static Optional<String> getPrimaryKeyName(final String tableName)  {
        try {
            final ArrayList arrayList = new DotConnect()
                    .setSQL("SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS T "
                            + "WHERE table_name = ? "
                            + "AND constraint_type = 'PRIMARY KEY'")
                    .addParam(tableName)
                    .loadResults();

            return arrayList.isEmpty() ? Optional.empty() :
                    Optional.of((((Map) arrayList.get(0))).get("constraint_name").toString());
        } catch (DotDataException e) {
            return Optional.empty();
        }
    }

    /**
     * Get the length of a modified column in a specified table.
     *
     * @param tableName
     *            - The name of the table to be queried.
     * @param columnName
     *            - The name of the column that wants to be queried.
     * @return Map<String, String>
     *            - the length of the field and if is nullable
     *
     * @throws SQLException
     *             An error occurred when executing the SQL query.
     */
    public Map<String, String> getModifiedColumnLength ( final String tableName, final String columnName) throws SQLException, DotDataException {
        final DotConnect dotConnect = new DotConnect();

        //the same query works for both supported sql versions
        if(DbConnectionFactory.isPostgres() || DbConnectionFactory.isMsSql() ) {
            return getColPropertiesQuery(dotConnect, tableName, columnName);
        }

        throw new DotDataException("Unknown database type.");
    } // getModifiedColumnLength.

    private Map<String, String> getColPropertiesQuery(DotConnect dotConnect, String tableName, String columnName) throws DotDataException {
        final String query = "select character_maximum_length as field_length, is_nullable as nullable_value " +
                "from information_schema.columns " +
                "where table_name = '"+tableName+"' and column_name='"+columnName+"'";
        dotConnect.setSQL(query);
        return (Map<String, String>)dotConnect.loadResults().get(0);
    }

    /**
     * Checks if the length of a specific column in a table is already what is expected.
     * Note: since this is going to be used in UT, when using it you need to negate the result,
     * since the UT should be run when the column length is not what is expected.
     *
     * @param tableName
     * @param columnName
     * @param expectedLength
     * @return boolean - true if the column length is equals to the value expected.
     */
    public boolean isColumnLengthExpected(final String tableName, final String columnName, final String expectedLength){
        boolean isColumnLengthExpected = false;
        try {
            final Map<String, String> columnData = getModifiedColumnLength(tableName,columnName);
            isColumnLengthExpected = columnData.get("field_length").equals(expectedLength);
        } catch (Exception e) {
            Logger.error(this.getClass(),"An error occurred when trying to pull the field_length");
        }
        return isColumnLengthExpected;
    }
} // E:O:F:DotDatabaseMetaData.

