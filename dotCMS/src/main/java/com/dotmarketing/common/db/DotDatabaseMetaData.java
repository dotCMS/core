package com.dotmarketing.common.db;

import com.dotcms.util.CloseUtils;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;

import java.sql.*;
import java.util.*;

/**
 * Encapsulates database metada operations operations.
 * @author jsanca
 */
public class DotDatabaseMetaData {

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

        for (ForeignKey foreignKey : foreignKeys) {

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

                if (!foreignKey.getPrimaryKeyColumnNames().contains(primaryKeyColumnName)) {
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

                if (!foreignKey.getForeignKeyColumnNames().contains(foreignKeyColumnName)) {
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
                "ALTER TABLE " + tableName + " DROP PRIMARY KEY ":
                "ALTER TABLE " + tableName + " DROP INDEX " + constraintName;
        }  else {
            sql = "ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName;
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

            preparedStatement = conn.prepareStatement("ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName);
            Logger.info(this, "Executing: " + "ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName);
            preparedStatement.execute();
        } catch (SQLException e) {
            Logger.error(this, "Error executing: " + "ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName + " - NOT A FOREIGN KEY.");
            throw e;
        } finally {
            CloseUtils.closeQuietly(preparedStatement);
        }
    } // executeDropForeignKeyMySql.
} // E:O:F:DotDatabaseMetaData.

