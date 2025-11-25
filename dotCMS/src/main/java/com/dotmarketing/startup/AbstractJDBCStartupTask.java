package com.dotmarketing.startup;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dotcms.util.CollectionsUtils.getMapValue;

/**
 * Derived classes should avoid use of transactions. MSSQL might have problems
 * to handle a mix of DDL+DML because of the snapshot isolation setting.
 * <p>
 * The cleaner way to avoid it is to set autocommit on the threadlocal
 * connection DbConnectionFactory.getConnection().setAutoCommit(true). So
 * DotConnect and HibernateUtil will not have problems with MSSQL.
 * 
 * @author Jason Tesser
 * @author Andres Olarte
 * @since 1.6.5a
 *
 */
public abstract class AbstractJDBCStartupTask implements StartupTask {

	/**
	 * By default statements should run in a single transaction.
	 * If you set to false every statement of the Script will be tokenized and
	 * executed within a separate transactions.
	 */
	protected boolean runInSingleTransaction = Boolean.TRUE;
	private boolean rebuildIndices = Boolean.TRUE;
	private boolean rebuildForeignKeys = Boolean.TRUE;
	private boolean rebuildPrimaryKeys = Boolean.TRUE;
	private boolean rebuildDefaultConstraints = Boolean.TRUE;
	private boolean rebuildCheckConstraints = Boolean.TRUE;

	protected CustomConstraintProcessor<PrimaryKeyHandler> primaryKeyProcessor = null;
	protected CustomConstraintProcessor<ForeignKeyHandler> foreignKeyProcessor = null;
	protected CustomConstraintProcessor<IndexHandler> indexProcessor = null;
	protected CustomConstraintProcessor<DefaultConstHandler> defaultConstProcessor = null;
	protected CustomConstraintProcessor<CheckConstHandler> checkConstProcessor = null;
	
	/**
	 * Contains the values that make up a Primary Key in a database. These keys
	 * can be composed of several column names.
	 * 
	 * @author Jason Tesser
	 * @version 1.6.5a
	 * @since Mar 22, 2012
	 *
	 */
    public class PrimaryKey {

		String tableName;
		String keyName;
		List<String> columnNames;

		@Override
		public String toString() {
			return "PrimaryKey [tableName=" + tableName + ", keyName=" + keyName + ", columnNames=" + columnNames + "]";
		}

	}

    /**
	 * Contains the values that make up a Foreign Key in a database. These keys
	 * can be composed of several column names.
	 * 
	 * @author Jason Tesser
	 * @version 1.6.5a
	 * @since Mar 22, 2012
	 *
	 */
	public class ForeignKey {

		String PKTABLE_NAME;
		List<String> PKCOLUMN_NAMES;
		String FKTABLE_NAME;
		List<String> FKCOLUMN_NAMES;
		String FK_NAME;
		String INDEX_NAME;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ForeignKey) {
				ForeignKey k=(ForeignKey)obj;
				if (!k.PKTABLE_NAME.equalsIgnoreCase(PKTABLE_NAME)) {
					return false;
				}
				if (!k.FKTABLE_NAME.equalsIgnoreCase(FKTABLE_NAME)) {
					return false;
				}
				if (!k.FK_NAME.equalsIgnoreCase(FK_NAME)) {
					return false;
				}
				return true;
			}
			return false;
		}

		/**
		 * Adds a new value to the list of columns for the primary key.
		 * 
		 * @param columnName
		 *            - The name of the new column.
		 */
		public void addPrimaryColumnName(String columnName) {
			if (this.PKCOLUMN_NAMES == null) {
				this.PKCOLUMN_NAMES = new ArrayList<>();
			}
			this.PKCOLUMN_NAMES.add(columnName);
		}

		/**
		 * Adds a new value to the list of columns for the foreign key.
		 * 
		 * @param columnName
		 *            - The name of the new column.
		 */
		public void addForeignColumnName(String columnName) {
			if (this.FKCOLUMN_NAMES == null) {
				this.FKCOLUMN_NAMES = new ArrayList<>();
			}
			this.FKCOLUMN_NAMES.add(columnName);
		}

		@Override
		public String toString() {
			return "ForeignKey [PKTABLE_NAME=" + PKTABLE_NAME + ", PKCOLUMN_NAMES=" + PKCOLUMN_NAMES + ", FKTABLE_NAME="
					+ FKTABLE_NAME + ", FKCOLUMN_NAMES=" + FKCOLUMN_NAMES + ", FK_NAME=" + FK_NAME + ", INDEX_NAME="
					+ INDEX_NAME + "]";
		}

		public String fkName(){
			return FK_NAME;
		}

	}

	/**
	 * Contains the values that make up an Index in a database.
	 * 
	 * @author Jason Tesser
	 * @version 1.6.5a
	 * @since Mar 22, 2012
	 *
	 */
	public class Index {

		String tableName;
		String indexName;
		List<String> columnNames;
		Boolean unique;
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Index) {
				Index i=(Index)obj;
				if (i.tableName.equalsIgnoreCase(tableName) && i.indexName.equalsIgnoreCase(indexName)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return ((tableName!=null) ? tableName : "") + "."+  ((indexName!=null) ? indexName : "");
		}

	}

	/**
	 * Contains the values that make up a Constraint in a database. There are
	 * different types of constraints, such as, check constraint, default
	 * constraint, etc.
	 * 
	 * @author Jose Castro
	 * @version 3.7
	 * @since Nov 29, 2016
	 *
	 */
	public class Constraint {

		String tableName;
		String name;
		String columnName;
		Object value;
		ConstraintType constraintType;

		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof Constraint) {
				Constraint cons = (Constraint) obj;
				if (!cons.name.equalsIgnoreCase(name)) {
					return false;
				}
				if (!cons.tableName.equalsIgnoreCase(tableName)) {
					return false;
				}
				if (!cons.constraintType.equals(constraintType)) {
					return false;
				}
				return true;
			}
			return false;
		}

		@Override
		public String toString() {
			return "Constraint [tableName=" + tableName + ", name=" + name + ", columnName=" + columnName + ", value="
					+ value + ", constraintType=" + constraintType + "]";
		}

	}

	/**
	 * Simple enumeration for the different types of constraints in a database.
	 * 
	 * @author Jose Castro
	 * @version 3.7
	 * @since Nov 29, 2016
	 *
	 */
	protected enum ConstraintType {

		DEFAULT, CHECK

	}

	/**
	 * A simple enumeration representing the officially supported databases in
	 * the application.
	 * 
	 * @author Jose Castro
	 * @version 3.7
	 * @since Dec 9, 2016
	 *
	 */
	protected enum DbType {

		MYSQL("MySQL"), POSTGRESQL("PostgreSQL"), ORACLE("Oracle"), MSSQL("Microsoft SQL Server");

		private String dbType;

		/**
		 * Private class constructor.
		 * 
		 * @param dbType
		 */
		private DbType(final String dbType) {
			this.dbType = dbType;
		}

		/**
		 * Returns the String representation of this DbType.
		 * 
		 * @return The {@link DbType} object.
		 */
		public String getDbType() {
			return dbType;
		}

		/**
		 * Returns the correct Enum based on the String representation of the
		 * database.
		 * 
		 * @param type
		 *            - The name of the database - i.e., "Oracle", "Microsoft
		 *            SQL Server", etc.
		 * @return The {@link DbType} object.
		 */
		public static DbType getDbType(final String type) {
			switch (type) {
			case "MySQL":
				return DbType.MYSQL;
			case "Oracle":
				return DbType.ORACLE;
			case "Microsoft SQL Server":
				return DbType.MSSQL;
			default:
				return DbType.POSTGRESQL;
			}
		}

	}

	/**
	 * Constraint objects in a database - i.e. PKs, FKs, indices, constraints,
	 * etc. - might require a specific SQL query to be created when using the
	 * {@link AbstractJDBCStartupTask#getTablesToDropConstraints()} method. Some
	 * constraints need to be marked as unique, have no checks, be clustered or
	 * non-clustered, and so on. Therefore, a simple ADD query will not be
	 * sufficient in specific cases because they will lose important settings
	 * for some constraints.
	 * <p>
	 * This Custom Constraint Processor allows you to run a specific SQL query
	 * upon creating every constraint for any database. This will assist you in
	 * making sure that the constraints in your tables will remain as they were
	 * before running the upgrade. The handler classes that allow you to
	 * customize the SQL query are:
	 * <ul>
	 * <li>{@link PrimaryKeyHandler}</li>
	 * <li>{@link ForeignKeyHandler}</li>
	 * <li>{@link IndexHandler}</li>
	 * <li>{@link DefaultConstHandler}</li>
	 * <li>{@link CheckConstHandler}</li>
	 * </ul>
	 * 
	 * @author Jose Castro
	 * @version 3.7
	 * @since Dec 7, 2016
	 *
	 */
	public class CustomConstraintProcessor<T extends ConstraintHandler<?>> {

		private final Map<DbType, Map<String, T>> createHandlers = new HashMap<>();

		/**
		 * Allows you to add one or more constraint handlers containing specific
		 * SQL code that will be run for creating the constraints.
		 * 
		 * @param dbType
		 *            - The {@link DbType} that the custom SQL query will run
		 *            against.
		 * @param handlers
		 *            - The {@link ConstraintHandler} objects that will be
		 *            executed for a given constraint.
		 */
		public void addCreateHandlers(final DbType dbType, final @SuppressWarnings("unchecked") T... handlers) {
			if (dbType == null) {
				throw new IllegalStateException("The database type has not been specified.");
			}
			for (T handler : handlers) {
				addHandlerEntry(dbType, createHandlers, handler);
			}
		}

		/**
		 * Returns the list of create handlers that will be run for the
		 * specified database.
		 * 
		 * @return The list of {@link ConstraintHandler} objects.
		 */
		public List<T> getCreateHandlers(final DbType dbType) {
			if (!this.createHandlers.containsKey(dbType)) {
				return new ArrayList<>();
			}
			return new ArrayList<>(this.createHandlers.get(dbType).values());
		}

		/**
		 * Looks up the specified create handler by its name for a specific
		 * database. If not present, a {@code null} will be returned. Remember
		 * that the constraint name can also be some of the characters of the
		 * actual name. This allows you to run a SQL query for constraints whose
		 * name start with specific characters.
		 * 
		 * @param constraintName
		 *            - The name of the constraint (or the beginning of it) in
		 *            the database.
		 * @return The {@link ConstraintHandler} object associated to the
		 *         specified name.
		 */
		public T findCreateHandler(final DbType dbType, final String constraintName) {
			if (this.createHandlers.containsKey(dbType)) {
				Map<String, T> createOperations = this.createHandlers.get(dbType);
				return getMapValue(createOperations, constraintName, null);
			}
			return null;
		}

		/**
		 * Adds a handler to its respective handler map - "create" map or "drop"
		 * map - for a specific database.
		 * 
		 * @param dbType
		 *            - The {@link DbType} that the custom handler will run
		 *            against.
		 * @param handler
		 *            - The {@link ConstraintHandler} object with the custom SQL
		 *            query.
		 */
		private void addHandlerEntry(final DbType dbType, final Map<DbType, Map<String, T>> handlersMap, final T handler) {
			if (!handlersMap.containsKey(dbType)) {
				Map<String, T> handlerData = Map.of(handler.getConstraintName(), handler);
				handlersMap.put(dbType, handlerData);
			} else {
				Map<String, T> createOperations = handlersMap.get(dbType);
				createOperations.put(handler.getConstraintName(), handler);
			}
		}

	}

	/**
	 * Overwriting the
	 * {@link AbstractJDBCStartupTask#getTablesToDropConstraints()} method in an
	 * upgrade task forces dotCMS to drop all constraints in the specified list
	 * of tables before running the upgrade queries, and then adds them back.
	 * There are generic queries that allow the process to re-create the dropped
	 * objects in order to leave the database as it were before running the
	 * upgrade. However, not all constraint objects are created in the same way,
	 * i.e., using the same SQL statement to create them.
	 * <p>
	 * The Constraint Handler objects allow developers to specify the SQL query
	 * that will be run when restoring a previously dropped object. This causes
	 * dotCMS to use the custom query they set in the handler object instead of
	 * using the default "create" statement. For example, the
	 * {@code "IX_dist_journal"} uses an {@code ADD CONSTRAINT} statement to be
	 * re-generated instead of a {@code CREATE INDEX} statement.
	 * <p>
	 * Each class implementing this {@code ConstraintHandler} interface has the
	 * ability to implement its custom SQL statement as needed.
	 * 
	 * @author Jose Castro
	 * @version 3.7
	 * @since Dec 8, 2016
	 *
	 * @param <T>
	 *            The constraint that needs to be created, such as,
	 *            {@link PrimaryKey}, {@link ForeignKey}, {@link Index}, etc.
	 */
	public interface ConstraintHandler<T> {

		/**
		 * Returns the name of this database constraint - i.e., primary key,
		 * foreign key, constraint, etc. In order to allow more flexibility to
		 * the process, this constraint name can also be the initial sequence of
		 * characters that make up the constraint. This way, you can run the
		 * same custom SQL query for more than one constraint. This is
		 * particularly useful when running a SQL query for constraints that are
		 * generated automatically by a given database.
		 * 
		 * @return The name of the constraint (or the beginning of it).
		 */
		public String getConstraintName();

		/**
		 * Returns the appropriate SQL query using the information from the
		 * specified database constraint.
		 * 
		 * @param constraint
		 *            - The object containing all the information of the
		 *            constraint that will be created.
		 * @return The SQL query that will be executed.
		 */
		public String generateQuery(final T constraint);

	}

	/**
	 * Represents the primary key constraint that needs to be added in a special
	 * way. For example, a primary key might need to have the
	 * {@code WITH NOCHECK} or {@code CLUSTERED} keywords as part of the
	 * {@code ALTER TABLE} statement.
	 * 
	 * @author Jose Castro
	 * @version 3.7
	 * @since Dec 8, 2016
	 *
	 */
	public class PrimaryKeyHandler implements ConstraintHandler<PrimaryKey> {

		final private String constraintName;
		final private String customQuery;

		/**
		 * Creates a handler for creating primary key objects. In order to allow
		 * more flexibility to the process, this constraint name can also be the
		 * initial sequence of characters that make up the constraint. This way,
		 * you can run the same custom SQL query for more than one constraint.
		 * This is particularly useful when running a SQL query for constraints
		 * that are generated automatically by a given database.
		 * 
		 * @param constraintName
		 *            - The name of the constraint (or the beginning of it) that
		 *            needs to be created in a special way.
		 * @param customQuery
		 *            - The SQL statement that will be run. It might contain
		 *            placeholders to replace with the actual query data.
		 */
		public PrimaryKeyHandler(final String constraintName, final String customQuery) {
			this.constraintName = constraintName;
			this.customQuery = customQuery;
		}

		@Override
		public String getConstraintName() {
			return constraintName;
		}

		@Override
		public String generateQuery(final PrimaryKey constraint) {
			return String.format(customQuery, constraint.tableName, constraint.keyName,
					getColumnList(constraint.columnNames));
		}

	}

	/**
	 * Represents the foreign key constraint that needs to be added in a special
	 * way. For example, a foreign key might need to have the
	 * {@code ON DELETE CASCADE} at the end of the statement.
	 * 
	 * @author Jose Castro
	 * @version 3.7
	 * @since Dec 8, 2016
	 *
	 */
	public class ForeignKeyHandler implements ConstraintHandler<ForeignKey> {

		final private String constraintName;
		final private String customQuery;

		/**
		 * Creates a handler for creating foreign key objects. In order to allow
		 * more flexibility to the process, this constraint name can also be the
		 * initial sequence of characters that make up the constraint. This way,
		 * you can run the same custom SQL query for more than one constraint.
		 * This is particularly useful when running a SQL query for constraints
		 * that are generated automatically by a given database.
		 * 
		 * @param constraintName
		 *            - The name of the constraint (or the beginning of it) that
		 *            needs to be created in a special way.
		 * @param customQuery
		 *            - The SQL statement that will be run. It might contain
		 *            placeholders to replace with the actual query data.
		 */
		public ForeignKeyHandler(final String constraintName, final String customQuery) {
			this.constraintName = constraintName;
			this.customQuery = customQuery;
		}

		@Override
		public String getConstraintName() {
			return constraintName;
		}

		@Override
		public String generateQuery(final ForeignKey constraint) {
			return String.format(customQuery, constraint.FKTABLE_NAME, constraint.FK_NAME,
					getColumnList(constraint.FKCOLUMN_NAMES), constraint.PKTABLE_NAME,
					getColumnList(constraint.PKCOLUMN_NAMES));
		}

	}

	/**
	 * Represents the index constraint that needs to be added in a special way.
	 * 
	 * @author Jose Castro
	 * @version 3.7
	 * @since Dec 9, 2016
	 *
	 */
	public class IndexHandler implements ConstraintHandler<Index> {

		final private String constraintName;
		final private String customQuery;

		/**
		 * Creates a handler for creating index objects. In order to allow more
		 * flexibility to the process, this constraint name can also be the
		 * initial sequence of characters that make up the constraint. This way,
		 * you can run the same custom SQL query for more than one constraint.
		 * This is particularly useful when running a SQL query for constraints
		 * that are generated automatically by a given database.
		 * 
		 * @param constraintName
		 *            - The name of the constraint (or the beginning of it) that
		 *            needs to be created in a special way.
		 * @param customQuery
		 *            - The SQL statement that will be run. It might contain
		 *            placeholders to replace with the actual query data.
		 */
		public IndexHandler(final String constraintName, final String customQuery) {
			this.constraintName = constraintName;
			this.customQuery = customQuery;
		}

		@Override
		public String getConstraintName() {
			return constraintName;
		}

		@Override
		public String generateQuery(final Index constraint) {
			return String.format(customQuery, constraint.tableName, constraint.indexName,
					getColumnList(constraint.columnNames));
		}

	}

	/**
	 * Represents the default constraint that needs to be added in a special
	 * way.
	 * 
	 * @author Jose Castro
	 * @version 3.7
	 * @since Dec 12, 2016
	 *
	 */
	public class DefaultConstHandler implements ConstraintHandler<Constraint> {

		final private String constraintName;
		final private String customQuery;

		/**
		 * Creates a handler for creating default constraint objects. In order
		 * to allow more flexibility to the process, this constraint name can
		 * also be the initial sequence of characters that make up the
		 * constraint. This way, you can run the same custom SQL query for more
		 * than one constraint. This is particularly useful when running a SQL
		 * query for constraints that are generated automatically by a given
		 * database.
		 * 
		 * @param constraintName
		 *            - The name of the constraint (or the beginning of it) that
		 *            needs to be created in a special way.
		 * @param customQuery
		 *            - The SQL statement that will be run. It might contain
		 *            placeholders to replace with the actual query data.
		 */
		public DefaultConstHandler(final String constraintName, final String customQuery) {
			this.constraintName = constraintName;
			this.customQuery = customQuery;
		}

		@Override
		public String getConstraintName() {
			return constraintName;
		}

		@Override
		public String generateQuery(final Constraint constraint) {
			return String.format(customQuery, constraint.tableName, constraint.name, constraint.value,
					constraint.columnName);
		}

	}

	/**
	 * Represents the check constraint that needs to be added in a special way.
	 * 
	 * @author Jose Castro
	 * @version 3.7
	 * @since Dec 12, 2016
	 *
	 */
	public class CheckConstHandler implements ConstraintHandler<Constraint> {

		final private String constraintName;
		final private String customQuery;

		/**
		 * Creates a handler for creating check constraint objects. In order to
		 * allow more flexibility to the process, this constraint name can also
		 * be the initial sequence of characters that make up the constraint.
		 * This way, you can run the same custom SQL query for more than one
		 * constraint. This is particularly useful when running a SQL query for
		 * constraints that are generated automatically by a given database.
		 * 
		 * @param constraintName
		 *            - The name of the constraint (or the beginning of it) that
		 *            needs to be created in a special way.
		 * @param customQuery
		 *            - The SQL statement that will be run. It might contain
		 *            placeholders to replace with the actual query data.
		 */
		public CheckConstHandler(final String constraintName, final String customQuery) {
			this.constraintName = constraintName;
			this.customQuery = customQuery;
		}

		@Override
		public String getConstraintName() {
			return constraintName;
		}

		@Override
		public String generateQuery(final Constraint constraint) {
			return String.format(customQuery, constraint.tableName, constraint.name, constraint.value);
		}

	}

	@Override
	@WrapInTransaction
	public void executeUpgrade() throws DotDataException, DotRuntimeException{
		DotConnect dc = new DotConnect();
		Connection conn = null;
		List<PrimaryKey> primaryKeys=null;
		List<ForeignKey> foreignKeys=null;
		List<Index> indexes=null;
		List<Constraint> defaultConstraints = null;
		List<Constraint> checkConstraints = null;
		List<String> schemaList = new ArrayList<>();
		try {
			// Obtain the SQL Script in accordance with the database type
			if (DbConnectionFactory.isPostgres()) {
				schemaList = SQLUtil.tokenize(getPostgresScript());
			} else if (DbConnectionFactory.isMySql()) {
				schemaList = SQLUtil.tokenize(getMySQLScript());
			} else if (DbConnectionFactory.isOracle()) {
				schemaList = SQLUtil.tokenize(getOracleScript());
			} else if (DbConnectionFactory.isMsSql()) {
				schemaList = SQLUtil.tokenize(getMSSQLScript());
			}
			if (schemaList.isEmpty()) {
				return;
			}
			this.primaryKeyProcessor = getPrimaryKeyProcessor();
			this.foreignKeyProcessor = getForeignKeyProcessor();
			this.indexProcessor = getIndexProcessor();
			this.defaultConstProcessor = getDefaultConstraintProcessor();
			this.checkConstProcessor = getCheckConstraintProcessor();
		    conn = DbConnectionFactory.getDataSource().getConnection();
			conn.setAutoCommit(true);
			List<String> tables = getTablesToDropConstraints();
			if(tables!=null){
				boolean executeDrop = true;
				logTaskProgress("==> Retrieving foreign keys [Drop objects? " + executeDrop + "]");
                foreignKeys=getForeingKeys(conn, tables, executeDrop);
                logTaskProgress("==> Retrieving primary keys [Drop objects? " + executeDrop + "]");
                primaryKeys=getPrimaryKey(conn, tables, executeDrop);
                logTaskProgress("==> Retrieving indexes [Drop objects? " + executeDrop + "]");
                indexes=getIndexes(conn, tables, executeDrop);
                logTaskProgress("==> Retrieving default constraints [Drop objects? " + executeDrop + "]");
                defaultConstraints = getDefaultConstraints(conn, tables, executeDrop);
                logTaskProgress("==> Retrieving check constraints [Drop objects? " + executeDrop + "]");
                checkConstraints = getCheckConstraints(conn, tables, executeDrop);
                if(DbConnectionFactory.isMsSql()) {
                    // for mssql we pass again as we might have index dependencies
                    getPrimaryKey(conn, tables, executeDrop);
                }
            }
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
		finally {
		    try {
		    	if (conn != null) {
		    		conn.close();
		    	}
		    } catch(SQLException ex) {
		        throw new DotDataException(ex.getMessage(), ex);
		    }
		}
		try {
		    conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(false);
		    
			if(DbConnectionFactory.isMySql()){
				dc.executeStatement("SET " + DbConnectionFactory.getMySQLStorageEngine() + "=INNODB", conn);
			}else if(DbConnectionFactory.isMsSql()){
				dc.executeStatement("SET TRANSACTION ISOLATION LEVEL READ COMMITTED;", conn);
			}
			logTaskProgress("==> Executing upgrade script");
			for (String query : schemaList) {
                if(!runInSingleTransaction){
                    try {
                        HibernateUtil.startTransaction();
                        dc.executeStatement(query);
                    } catch (Exception e) {
                        Logger.error(this, "Unable to execute query : " + query);
                        HibernateUtil.rollbackTransaction();
                        continue;
                    }
                    HibernateUtil.closeAndCommitTransaction();
                } else {
                    try {
                        dc.executeStatement(query, conn);
                    } catch (SQLException e) {
                        Logger.fatal(this, "Unable to execute SQL upgrade", e);
                        throw new DotDataException(e.getMessage(), e);
                    }
                }
            }
			
			conn.commit();
		} catch (SQLException e) {
		    try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new DotDataException(e1.getMessage(), e1);
            }
			Logger.fatal(this, "Unable to execute SQL upgrade", e);
			throw new DotDataException(e.getMessage(), e);
		}
		finally {
		    try {
                conn.close();
            } catch (SQLException e) {
                throw new DotDataException(e.getMessage(), e);
            }
		}
		
		try {
		    conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);

			if (checkConstraints != null && rebuildCheckConstraints) {
				logTaskProgress("==> Restoring check constraints");
				for (Constraint constraint : checkConstraints) {
					try {
						createConstraint(conn, constraint);
					} catch (SQLException e) {
						Logger.error(this, "Can't create check constraint on table '" + constraint.tableName
								+ "' in column [" + constraint.columnName + "]. Message: " + e.getMessage(), e);
					}
				}
			}

			if (defaultConstraints != null && rebuildDefaultConstraints) {
				logTaskProgress("==> Restoring default constraints");
				for (Constraint constraint : defaultConstraints) {
					try {
						createConstraint(conn, constraint);
					} catch (SQLException e) {
						Logger.error(this, "Can't create default constraint on table '" + constraint.tableName
								+ "' in column [" + constraint.columnName + "]. Message: " + e.getMessage(), e);
					}
				}
			}

			if (primaryKeys!=null && rebuildPrimaryKeys) {
				logTaskProgress("==> Restoring primary keys");
				List<String> processedPks = new ArrayList<>();
				for (PrimaryKey key:primaryKeys) {
					try {
						if (!processedPks.contains(key.keyName)) {
							createPrimaryKey(conn, key);
							processedPks.add(key.keyName);
						}
					} catch (SQLException e) {
						Logger.error(this, "Can't create primary key on table '" + key.tableName + "' in columns ["
								+ getColumnList(key.columnNames) + "]. Message: " + e.getMessage(), e);
					}
				}
			}

			if (foreignKeys!=null && rebuildForeignKeys) {
				logTaskProgress("==> Restoring foreign keys");
				for (ForeignKey key:foreignKeys) {
					try {
						createConstraint(conn, key);
					} catch (SQLException e) {
						Logger.error(this, "Can't create foreign key on table '" + key.PKTABLE_NAME + "' in columns ["
								+ getColumnList(key.PKCOLUMN_NAMES) + "]. Message: " + e.getMessage(), e);
					}
				}
			}

			if (indexes!=null && rebuildIndices) {
				logTaskProgress("==> Restoring indices");
				idxfor: for (Index index:indexes) {
					try {
						for (PrimaryKey pk:primaryKeys) {
							if(index.tableName.equalsIgnoreCase(pk.tableName) && index.indexName.equalsIgnoreCase(pk.keyName)) {
								continue idxfor; 
							}
						}
						createIndex(conn, index);
					} catch (SQLException e) {
						Logger.error(this, "Can't create index on table '" + index.tableName + "' in columns ["
								+ getColumnList(index.columnNames) + "]. Message: " + e.getMessage(), e);
					}
				}
			}
    	} catch (SQLException e) {
            Logger.fatal(this, "Unable to execute SQL upgrade", e);
            throw new DotDataException(e.getMessage(), e);
        }
        finally {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new DotDataException(e.getMessage(), e);
            }
        } 
	}

	/**
	 * Drops the specified index associated to the given table.
	 * 
	 * @param conn
	 *            - The database connection object to access the dotCMS data.
	 * @param tableName
	 *            - The name of the table whose index will be dropped.
	 * @param indexName
	 *            - The name of the index that will be dropped.
	 * @throws SQLException
	 *             An error occurred when executing the SQL query.
	 */
	protected void executeDropIndex(Connection conn, String tableName, String indexName) throws SQLException{		
		PreparedStatement preparedStatement = null;
		String sql="";
		if(DbConnectionFactory.isOracle() || DbConnectionFactory.isPostgres()) {
			sql="DROP INDEX " + indexName; 
		} else if(DbConnectionFactory.isMsSql()) {
			sql="DROP INDEX " + tableName + "." + indexName; 
		} else {
			sql="ALTER TABLE " + tableName + " DROP INDEX " + indexName;
		}
		preparedStatement = conn.prepareStatement(sql);
		Logger.info(this, "Executing: "+sql);
		preparedStatement.execute();
		preparedStatement.close();
	}

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
	protected void executeDropConstraint(Connection conn, String tableName, String constraintName) throws SQLException {
	    String sql="";
		if(DbConnectionFactory.isMySql()) {
		    if(constraintName.indexOf("PRIMARY")>-1) {
		        sql="ALTER TABLE " + tableName + " DROP PRIMARY KEY ";
		    } else {
		        sql="ALTER TABLE " + tableName + " DROP INDEX " + constraintName;
		    }
		}  else {
		    sql="ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName;
		}
		PreparedStatement preparedStatement = conn.prepareStatement(sql);
		Logger.info(this, "Executing: " + sql);
		preparedStatement.execute();
		preparedStatement.close();
	}
	
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
	protected void executeDropForeignKeyMySql(Connection conn, String tableName, String constraintName) throws SQLException{
		try {
			PreparedStatement preparedStatement = conn.prepareStatement("ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName);
			Logger.info(this, "Executing: " + "ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName);
			preparedStatement.execute();
			preparedStatement.close();
		} catch (Exception e) {
			Logger.error(this, "Error executing: " + "ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName + " - NOT A FOREIGN KEY.");
		}
	}

	/**
	 * Takes a list of columns as <code>String</code> values and returns them as
	 * a <code>String</code> of comma-separated values.
	 * 
	 * @param columns
	 *            - The list of columns to process.
	 * @return A <code>String</code> of comma-separated values.
	 */
	protected String getColumnList(List<String> columns) {
		StringBuilder b=new StringBuilder();
		boolean first=true;
		for (String column:columns) {
			if (!first) {
				b.append(",");
			} else {
				first =false;
			}
			b.append(column);
		}
		return b.toString();
	}

	/**
	 * Creates the specified primary key in the database.
	 * 
	 * @param conn
	 *            - The database connection object to access the dotCMS data.
	 * @param key
	 *            - The information of the primary key that will be created.
	 * @throws SQLException
	 *             An error occurred when executing the SQL query.
	 */
	protected void createPrimaryKey(final Connection conn, final PrimaryKey key) throws SQLException {
		String sql = "ALTER TABLE " + key.tableName + " ADD CONSTRAINT " + key.keyName + " PRIMARY KEY ("
				+ getColumnList(key.columnNames) + ")";
		PrimaryKeyHandler handler = this.primaryKeyProcessor
				.findCreateHandler(DbType.getDbType(DbConnectionFactory.getDBType()), key.keyName);
		if (UtilMethods.isSet(handler)) {
			sql = handler.generateQuery(key);
		}
		Logger.info(this, "Executing: " +sql);
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.execute();
		stmt.close();
	}

	/**
	 * Creates the specified index in the database.
	 * 
	 * @param conn
	 *            - The database connection object to access the dotCMS data.
	 * @param key
	 *            - The information of the index that will be created.
	 * @throws SQLException
	 *             An error occurred when executing the SQL query.
	 */
	protected void createIndex(final Connection conn, final Index index) throws SQLException {
		String sql = "CREATE " + (index.unique ? "UNIQUE" : "") + " INDEX " + index.indexName + " ON " + index.tableName
				+ " (" + getColumnList(index.columnNames) + ")";
		IndexHandler handler = this.indexProcessor.findCreateHandler(DbType.getDbType(DbConnectionFactory.getDBType()),
				index.indexName);
		if (UtilMethods.isSet(handler)) {
			sql = handler.generateQuery(index);
		}
		Logger.info(this, "Executing: "+sql);
		PreparedStatement stmt=conn.prepareStatement(sql);
		try {
		    stmt.execute();
		}
		finally {
		    stmt.close();
		}
	}

	/**
	 * Creates the specified constraint in the database.
	 * 
	 * @param conn
	 *            - The database connection object to access the dotCMS data.
	 * @param constraint
	 *            - The information of the constraint that will be created.
	 * @throws SQLException
	 *             An error occurred when executing the SQL query.
	 */
	protected void createConstraint(final Connection conn, final Object constraint) throws SQLException {
		String sql = StringUtils.EMPTY;
		if (constraint instanceof ForeignKey) {
			final ForeignKey cons = ForeignKey.class.cast(constraint);
			sql = "ALTER TABLE " + cons.FKTABLE_NAME + " ADD CONSTRAINT " + cons.FK_NAME + " FOREIGN KEY ("
					+ getColumnList(cons.FKCOLUMN_NAMES) + ") REFERENCES " + cons.PKTABLE_NAME + " (" + getColumnList(cons.PKCOLUMN_NAMES) + ")";
			ForeignKeyHandler handler = this.foreignKeyProcessor
					.findCreateHandler(DbType.getDbType(DbConnectionFactory.getDBType()), cons.FK_NAME);
			if (UtilMethods.isSet(handler)) {
				sql = handler.generateQuery(cons);
			}
		} else if (constraint instanceof Constraint) {
			final Constraint cons = Constraint.class.cast(constraint);
			if (DbConnectionFactory.isMsSql()) {
				if (ConstraintType.DEFAULT.equals(cons.constraintType)) {
					sql = "ALTER TABLE " + cons.tableName + " ADD CONSTRAINT " + cons.name + " DEFAULT "
							+ cons.value + " FOR " + cons.columnName;
					DefaultConstHandler handler = this.defaultConstProcessor
							.findCreateHandler(DbType.getDbType(DbConnectionFactory.getDBType()), cons.name);
					if (UtilMethods.isSet(handler)) {
						sql = handler.generateQuery(cons);
					}
				} else {
					sql = "ALTER TABLE " + cons.tableName + " ADD CONSTRAINT " + cons.name + " CHECK " + cons.value;
					CheckConstHandler handler = this.checkConstProcessor
							.findCreateHandler(DbType.getDbType(DbConnectionFactory.getDBType()), cons.name);
					if (UtilMethods.isSet(handler)) {
						sql = handler.generateQuery(cons);
					}
				}
			}
		}
		final PreparedStatement preparedStatement = conn.prepareStatement(sql);
		Logger.info(this, "Executing: " + sql);
		preparedStatement.execute();
		preparedStatement.close();
	}

	/**
	 * Reads all the foreign keys associated to every table in the specified
	 * list. Additionally, the keys can be dropped.
	 * 
	 * @param conn
	 *            - The database connection object to access the dotCMS data.
	 * @param tables
	 *            - The list of database tables whose foreign keys will be
	 *            retrieved.
	 * @param executeDrop
	 *            - If <code>true</code>, the foreign keys will be dropped
	 *            before being returned. Otherwise, set to <code>false</code>.
	 * @return The list of foreign keys associated to the tables.
	 */
	protected List<ForeignKey> getForeingKeys(final Connection conn, final List<String> tables, final boolean executeDrop) {
		List<ForeignKey> ret=new ArrayList<>();
		Map<String, ForeignKey> fkMap = new HashMap<>();
		try {
			DatabaseMetaData dbmd = conn.getMetaData();
			
			for (String t : tables) {
				String schema = null;
				if (DbConnectionFactory.isOracle()) {
					t = t.toUpperCase();
					schema = dbmd.getUserName();
				}

				ResultSet rs = dbmd.getImportedKeys(conn.getCatalog(), schema,
						t);

				// Iterates over the foreign key columns
				while (rs.next()) {
					ForeignKey key=new ForeignKey();
					key.PKTABLE_NAME=rs.getString("PKTABLE_NAME");
					String pkColumnName = rs.getString("PKCOLUMN_NAME");
					key.addPrimaryColumnName(pkColumnName);
					key.FKTABLE_NAME=rs.getString("FKTABLE_NAME");
					String fkColumnName = rs.getString("FKCOLUMN_NAME");
					key.addForeignColumnName(fkColumnName);
					key.FK_NAME=rs.getString("FK_NAME");
					
					if (!ret.contains(key)) {
						ret.add(key);
						fkMap.put(key.FK_NAME, key);
					} else {
						if (DbConnectionFactory.isMsSql()) {
							// The FK has more than one column as part of the
							// constraint. Add the column to the same object
							// instead of duplicating an entry in the list.
							ForeignKey existingKey = fkMap.get(key.FK_NAME);
							existingKey.addPrimaryColumnName(pkColumnName);
							existingKey.addForeignColumnName(fkColumnName);
						}
					}

				}
			}
			if (executeDrop) {
				final Set<String> droppedForeignKeys = new HashSet<>();
				for (ForeignKey key:ret) {
					if (!droppedForeignKeys.contains(key.FK_NAME)) {
						if(DbConnectionFactory.isPostgres() ||
							DbConnectionFactory.isMsSql() ||
							DbConnectionFactory.isOracle()){

							 executeDropConstraint(conn, key.FKTABLE_NAME, key.FK_NAME);
							 
						} else if (DbConnectionFactory.isMySql()) {
							executeDropForeignKeyMySql(conn, key.FKTABLE_NAME, key.FK_NAME);

						}
						droppedForeignKeys.add(key.FK_NAME);
					}
				}
			}
		} catch (SQLException e) {
			Logger.error(this,
					"An error occurred when processing the foreign keys [drop = " + executeDrop + "]: " + e.getMessage(), e);
		}
		return ret;
	}

	/**
	 * Reads all the indexes associated to every table in the specified list.
	 * Additionally, the indexes can be dropped.
	 * 
	 * @param conn
	 *            - The database connection object to access the dotCMS data.
	 * @param tables
	 *            - The list of database tables whose indexes will be retrieved.
	 * @param executeDrop
	 *            - If <code>true</code>, the indexes will be dropped before
	 *            being returned. Otherwise, set to <code>false</code>.
	 * @return The list of indexes associated to the tables.
	 */
	protected List<Index> getIndexes(final Connection conn, final List<String> tables, final boolean executeDrop) {
		List<Index> ret=new ArrayList<>();
		try {
			DatabaseMetaData dbmd = conn.getMetaData();

			for (String t : tables) {
				String schema = null;

				if (DbConnectionFactory.isOracle()) {
					t = t.toUpperCase();
					schema = dbmd.getUserName();
				}

				ResultSet idxrs = dbmd.getIndexInfo(conn.getCatalog(), schema, t, false, false);
				Index i=null;
				String indexName = "";

				while (idxrs.next()) {
					if(indexName!=null && indexName.equals(idxrs.getString("INDEX_NAME"))) {
						i.columnNames.add(idxrs.getString("COLUMN_NAME"));
					} else {
						indexName = idxrs.getString("INDEX_NAME");
						i = new Index();
						i.indexName=indexName;
						i.tableName=t;
						i.columnNames=new ArrayList<>();
						i.columnNames.add(idxrs.getString("COLUMN_NAME"));
						i.unique = !(idxrs.getBoolean("NON_UNIQUE"));
						
						if(UtilMethods.isSet(indexName)) {
							
						    if(DbConnectionFactory.isOracle()) {
						        PreparedStatement smt=conn.prepareStatement("select constraint_name from user_constraints where INDEX_NAME=?");
						        smt.setString(1, indexName);
						        ResultSet rs=smt.executeQuery();
						        while(rs.next()) {
						            Index cons = new Index();
						            cons.indexName=rs.getString(1);
						            cons.tableName=i.tableName;
						            cons.columnNames=i.columnNames;
						            cons.unique=i.unique;
						            ret.add(cons);
						        }
						        rs.close();
						        smt.close();
						    }
						    
							ret.add(i);
						}
					}
				}
				
			}

			if(executeDrop) {
				for (Index index:ret) {
					try {
				    	if(index.unique) {
				    		if (DbConnectionFactory.isMsSql()) {
				    			try {
				    				// Try dropping the object as a constraint. If
									// it fails, try again by dropping it as an index
					    			executeDropConstraint(conn, index.tableName, index.indexName);
					    		} catch(Exception ex) {
									Logger.warn(this, "Drop constraint failed with '" + index.indexName + "': ["
											+ ex.getMessage() + "] . Try again by dropping it as an index...");
							        executeDropIndex(conn, index.tableName, index.indexName);
							    }
				    		} else {
				    			executeDropConstraint(conn, index.tableName, index.indexName);
				    		}
    					} else {
    						executeDropIndex(conn, index.tableName, index.indexName);
    					}
				    }
				    catch(Exception ex) {
						Logger.warn(this,
								"Drop index/constraint '" + index.indexName + "' failed on table '" + index.tableName + "'",
								ex);
				    }
				}
			}
		} catch (SQLException e) {
			Logger.error(this,
					"An error occurred when processing the indices [drop = " + executeDrop + "]: " + e.getMessage(), e);
		}

		return ret;
	}

	/**
	 * Reads all the primary keys associated to every table in the specified
	 * list. Additionally, the keys can be dropped.
	 * 
	 * @param connection
	 *            - The database connection object to access the dotCMS data.
	 * @param tablesWithKeys
	 *            - The list of database tables whose primary keys will be
	 *            retrieved.
	 * @param drop
	 *            - If <code>true</code>, the primary keys will be dropped
	 *            before being returned. Otherwise, set to <code>false</code>.
	 * @return The list of primary keys associated to the tables.
	 */
	protected List<PrimaryKey> getPrimaryKey(final Connection connection,
											 final List<String> tablesWithKeys, final boolean drop) {

		List<PrimaryKey> ret=new ArrayList<>();

		if (tablesWithKeys!=null) {
			try {
				for (String tableName: tablesWithKeys) {

					DatabaseMetaData metaData = connection.getMetaData();

					String schema=null;
					if(DbConnectionFactory.isOracle()) {

						tableName = tableName.toUpperCase();
						schema    = metaData.getUserName();
					}

					final ResultSet resultSet = metaData.getPrimaryKeys(connection.getCatalog(), schema, tableName);
					PrimaryKey key            = null;

					while (resultSet.next()) {
						if (key==null) {
							key = new PrimaryKey();
							key.keyName=resultSet.getString("PK_NAME");
							key.tableName=tableName;
							key.columnNames=new ArrayList<>();
						}
						key.columnNames.add(resultSet.getString("COLUMN_NAME"));
						
					}

					if(key!=null) {
						ret.add(key);
					}
			}
			
			if(drop) {
				final Set<String> droppedPrimaryKeys = new HashSet<>();
			    for(PrimaryKey idx : ret) {
			    	if (!droppedPrimaryKeys.contains(idx.keyName)) {
				        try {
				            executeDropConstraint(connection, idx.tableName, idx.keyName);
				            droppedPrimaryKeys.add(idx.keyName);
				        }
				        catch(Exception ex) {
							if (idx != null) {
								Logger.error(this,
										"Drop primary key '" + idx.keyName + "' failed on table '" + idx.tableName + "'", ex);
							}
				        }
			    	}
			    }
			}

			} catch (SQLException e) {
				Logger.error(AbstractJDBCStartupTask.class,
						"An error occurred when processing the primary keys [drop = " + drop + "]: " + e.getMessage(), e);
			}

		}
		return ret;
	}

	/**
	 * Reads all the default constraints associated to every table in the
	 * specified list. Additionally, the constraints can be dropped.
	 * 
	 * @param conn
	 *            - The database connection object to access the dotCMS data.
	 * @param tables
	 *            - The list of database tables whose default constraints will
	 *            be retrieved.
	 * @param executeDrop
	 *            - If <code>true</code>, the default constraints will be
	 *            dropped before being returned. Otherwise, set to
	 *            <code>false</code>.
	 * @return The list of default constraints associated to the tables.
	 */
	protected List<Constraint> getDefaultConstraints(final Connection conn, final List<String> tables,
			final boolean executeDrop) {
		final List<Constraint> defaultConstraints = new ArrayList<>();
		PreparedStatement statement = null;
		try {
			if (tables != null) {
				for (String table : tables) {
					if (DbConnectionFactory.isMsSql()) {
						statement = conn.prepareStatement(
								"SELECT d.name, definition, c.name, system_type_id FROM sys.tables t JOIN sys.default_constraints d "
										+ "ON d.parent_object_id = t.object_id JOIN sys.columns c ON c.object_id = t.object_id "
										+ "AND c.column_id = d.parent_column_id WHERE t.name = ?");
						statement.setString(1, table);
						final ResultSet results = statement.executeQuery();
						while (results.next()) {
							final Constraint cons = new Constraint();
							cons.constraintType = ConstraintType.DEFAULT;
							cons.name = results.getString(1);
							cons.value = results.getString(2);
							cons.columnName = results.getString(3);
							cons.tableName = table;
							defaultConstraints.add(cons);
						}
						results.close();
						statement.close();
					}
				}
			}
			if (executeDrop) {
				for (Constraint cons : defaultConstraints) {
					try {
						executeDropConstraint(conn, cons.tableName, cons.name);
					} catch (Exception ex) {
						Logger.warn(this,
								"Drop default constraint '" + cons.name + "' failed on table '" + cons.tableName + "'", ex);
					}
				}
			}
		} catch (SQLException e) {
			Logger.error(this, "An error occurred when processing the default constraints [drop = " + executeDrop + "]: "
					+ e.getMessage(), e);
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e1) {
					// Statement could not be closed. Continue
				}
			}
		}
		return defaultConstraints;
	}

	/**
	 * Reads all the check constraints to every table in the specified list.
	 * Additionally, the constraints can be dropped.
	 * 
	 * @param conn
	 *            - The database connection object to access the dotCMS data.
	 * @param tables
	 *            - The list of database tables whose check constraints will be
	 *            retrieved.
	 * @param executeDrop
	 *            - If <code>true</code>, the check constraints will be dropped
	 *            before being returned. Otherwise, set to <code>false</code>.
	 * @return The list of check constraints associated to the tables.
	 */
	protected List<Constraint> getCheckConstraints(final Connection conn, final List<String> tables,
			final boolean executeDrop) {
		final List<Constraint> checkConstraints = new ArrayList<>();
		PreparedStatement statement = null;
		try {
			if (tables != null) {
				for (String table : tables) {
					if (DbConnectionFactory.isMsSql()) {
						statement = conn.prepareStatement("SELECT t.name, cc.name, definition FROM sys.tables t "
								+ "JOIN sys.check_constraints cc ON cc.parent_object_id = t.object_id WHERE t.name = ?");
						statement.setString(1, table);
						final ResultSet results = statement.executeQuery();
						while (results.next()) {
							final Constraint cons = new Constraint();
							cons.constraintType = ConstraintType.CHECK;
							cons.tableName = results.getString(1);
							cons.name = results.getString(2);
							cons.value = results.getString(3);
							checkConstraints.add(cons);
						}
						results.close();
						statement.close();
					}
				}
			}
			if (executeDrop) {
				for (Constraint cons : checkConstraints) {
					try {
						executeDropConstraint(conn, cons.tableName, cons.name);
					} catch (Exception ex) {
						Logger.warn(this,
								"Drop check constraint '" + cons.name + "' failed on table '" + cons.tableName + "'", ex);
					}
				}
			}
		} catch (SQLException e) {
			Logger.error(this, "An error occurred when processing the check constraints [drop = " + executeDrop + "]: "
					+ e.getMessage(), e);
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e1) {
					// Statement could not be closed. Continue
				}
			}
		}
		return checkConstraints;
	}

	/**
	 * Specifies whether the primary keys need to be re-built after being
	 * dropped.
	 * 
	 * @param rebuildPrimaryKeys
	 *            - If set to <code>true</code>, the primary keys will be
	 *            re-created. Otherwise, set to <code>false</code>.
	 */
	protected void setRebuildPrimaryKeys(Boolean rebuildPrimaryKeys) {
		this.rebuildPrimaryKeys = rebuildPrimaryKeys;
	}

	/**
	 * Specifies whether the indices need to be re-built after being dropped.
	 * 
	 * @param rebuildIndices
	 *            - If set to <code>true</code>, the indices will be re-created.
	 *            Otherwise, set to <code>false</code>.
	 */
	protected void setRebuildIndices(Boolean rebuildIndices) {
		this.rebuildIndices = rebuildIndices;
	}

	/**
	 * Specifies whether the foreign keys need to be re-built after being
	 * dropped.
	 * 
	 * @param rebuildForeignKeys
	 *            - If set to <code>true</code>, the foreign keys will be
	 *            re-created. Otherwise, set to <code>false</code>.
	 */
	protected void setRebuildForeignKeys(Boolean rebuildForeignKeys) {
		this.rebuildForeignKeys = rebuildForeignKeys;
	}

	/**
	 * Utility method to display a message in the <code>dotcms.log</code> file
	 * in order to report the execution progress of the task.
	 * 
	 * @param msg
	 *            - The message that will be displayed in the log.
	 */
	protected void logTaskProgress(final String msg) {
		Logger.info(this, "======================================================================");
		Logger.info(this, msg);
		Logger.info(this, "======================================================================");
	}

	/**
	 * Returns the upgrade SQL query for PostgreSQL.
	 * 
	 * @return The SQL statement(s).
	 */
	public String getPostgresScript() {
	    return null;
	}

	/**
	 * Returns the upgrade SQL query for MySQL.
	 * 
	 * @return The SQL statement(s).
	 */
	public String getMySQLScript(){
        return null;
    }

	/**
	 * Returns the upgrade SQL query for Oracle.
	 * 
	 * @return The SQL statement(s).
	 */
	public String getOracleScript(){
        return null;
    }

	/**
	 * Returns the upgrade SQL query for Microsoft SQL Server.
	 * 
	 * @return The SQL statement(s).
	 */
	public String getMSSQLScript(){
        return null;
    }

	/**
	 * Returns the list of database tables whose keys, indexes, and constraints
	 * will be dropped in order to perform intensive changes in their columns.
	 * <p>
	 * <b>IMPORTANT: The order in which the tables are added to the list
	 * matters. The reason is that objects of a given table might need to be
	 * dropped first before another table. Read the log errors thoroughly when
	 * troubleshooting this.</b>
	 */
	protected List<String> getTablesToDropConstraints(){
	    return List.of();
	}

	/**
	 * Returns the Custom Constraint Processor object for a given set of primary
	 * key objects.
	 * 
	 * @return The {@link CustomConstraintProcessor} object.
	 */
	public CustomConstraintProcessor<PrimaryKeyHandler> getPrimaryKeyProcessor() {
		return new CustomConstraintProcessor<>();
	}

	/**
	 * Returns the Custom Constraint Processor object for a given set of foreign
	 * key objects.
	 * 
	 * @return The {@link CustomConstraintProcessor} object.
	 */
	public CustomConstraintProcessor<ForeignKeyHandler> getForeignKeyProcessor() {
		return new CustomConstraintProcessor<>();
	}

	/**
	 * Returns the Custom Constraint Processor object for a given set of index
	 * objects.
	 * 
	 * @return The {@link CustomConstraintProcessor} object.
	 */
	public CustomConstraintProcessor<IndexHandler> getIndexProcessor() {
		return new CustomConstraintProcessor<>();
	}

	/**
	 * Returns the Custom Constraint Processor object for a given set of default
	 * constraint objects.
	 * 
	 * @return The {@link CustomConstraintProcessor} object.
	 */
	public CustomConstraintProcessor<DefaultConstHandler> getDefaultConstraintProcessor() {
		return new CustomConstraintProcessor<>();
	}

	/**
	 * Returns the Custom Constraint Processor object for a given set of check
	 * constraint objects.
	 * 
	 * @return The {@link CustomConstraintProcessor} object.
	 */
	public CustomConstraintProcessor<CheckConstHandler> getCheckConstraintProcessor() {
		return new CustomConstraintProcessor<>();
	}

}
