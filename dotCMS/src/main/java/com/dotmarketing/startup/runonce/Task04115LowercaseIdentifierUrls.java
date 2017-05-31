package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * This upgrade task will perform an update operation on almost all the records
 * of the {@code identifier} table. The values of the {@code parent_path} and
 * {@code asset_name} columns will be lower-cased in order to take advantage of
 * the performance improvements provided by database indexes. The following
 * considerations are taken into account:
 * <ul>
 * <li>Parent path called {@code "/System folder"} is not modified.</li>
 * <li>The queries are created so that <b>ONLY PATHS AND ASSET NAMES WITH MIXED
 * CASES ARE UPDATED</b>. This improves performance since paths and assets that
 * are already lower-case will not be processed.</li>
 * <li>Cases where the UPDATE throws an error will be logged as Error and they
 * will have to be fixed manually.</li>
 * </ul>
 * 
 * @author Jose Castro
 * @author Oscar Arrieta
 * @version 4.1.0
 * @since Apr 20, 2017
 *
 */
public class Task04115LowercaseIdentifierUrls implements StartupTask {

	private static final String SELECT_QUERY_POSTGRES = "SELECT id, parent_path, asset_name FROM identifier WHERE parent_path <> '/System folder' AND (parent_path <> LOWER(parent_path) OR asset_name <> LOWER(asset_name))";
	private static final String SELECT_QUERY_MYSQL = "SELECT id, parent_path, asset_name FROM identifier WHERE parent_path <> '/System folder' AND (CAST(asset_name AS BINARY) RLIKE '[A-Z]' OR CAST(parent_path AS BINARY) RLIKE '[A-Z]')";
	private static final String SELECT_QUERY_MSSQL = "SELECT id, parent_path, asset_name FROM identifier WHERE (HASHBYTES('SHA1', asset_name) <> HASHBYTES('SHA1', UPPER( asset_name)) AND HASHBYTES('SHA1', asset_name) <> HASHBYTES('SHA1', LOWER(asset_name))) OR (HASHBYTES('SHA1', parent_path) <> HASHBYTES('SHA1', UPPER( parent_path)) AND HASHBYTES('SHA1', parent_path) <> HASHBYTES('SHA1', LOWER(parent_path)) AND parent_path <> '/System folder')";
	private static final String SELECT_QUERY_ORACLE = "SELECT id, parent_path, asset_name FROM identifier WHERE parent_path <> '/System folder' AND (parent_path <> LOWER(parent_path) OR asset_name <> LOWER(asset_name))";

	private static final String UPDATE_QUERY_GENERIC = "UPDATE identifier SET parent_path = ?, asset_name = ? WHERE id = ?";

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

        //Disable trigger.
        try {
            final DotConnect dotConnect = new DotConnect();
            final List<String> statements = getDisableTriggerQuery();
            for (String statement : statements) {
                dotConnect.executeStatement(statement);
            }
        } catch (SQLException e) {
            throw new DotDataException(
                String.format("Error executing Task04115LowercaseIdentifierUrls, trying to DISABLE trigger: %s",
                    e.getMessage()), e);
        }

        Connection conn = DbConnectionFactory.getConnection();
        Logger.info(this, "============= Lower-casing Identifier URLs =============");
        try (PreparedStatement selectPs = conn.prepareStatement(getSelectQuery())) {
            //final int BATCH_SIZE = 100;
            try (ResultSet identifiers = selectPs.executeQuery()) {
                int counter = 1;
                int errors = 1;
                while (identifiers.next()) {
                    final String identifier = identifiers.getString(1);
                    final String originalParentPath = identifiers.getString(2);
                    final String originalAssetName = identifiers.getString(3);
                    // Double-check that '/System folder' must not be modified
                    if ("/system folder".equalsIgnoreCase(originalParentPath)) {
                        continue;
                    }
                    final String parentPathLowercase = originalParentPath.toLowerCase();
                    final String assetNameLowercase = originalAssetName.toLowerCase();

                    DotConnect dotConnect = new DotConnect();
                    dotConnect.setSQL(UPDATE_QUERY_GENERIC);
                    dotConnect.addParam(parentPathLowercase);
                    dotConnect.addParam(assetNameLowercase);
                    dotConnect.addParam(identifier);

                    Logger.info(this, counter + ". " + originalParentPath + originalAssetName);
                    try {
                        dotConnect.loadResult();

                    } catch (Exception e) {
                        Logger.error(this,
                            errors + ". Error trying to UPDATE identifier with id: " + identifier
                                + ", parent_path: " + originalParentPath
                                + ", asset_name: " + originalAssetName);
                        errors++;
                    }

                    counter++;
                }
                Logger.info(this, "========================================================");

            } catch (Exception e) { //ResultSet identifiers = selectPs.executeQuery()
                Logger
                    .error(this, String.format("Error executing Task04115LowercaseIdentifierUrls: %s", e.getMessage()),
                        e);
                throw new DotDataException(
                    String.format("Error executing Task04115LowercaseIdentifierUrls: %s", e.getMessage()), e);
            }
        } catch (Exception e) { //conn.prepareStatement(getSelectQuery())
            Logger
                .error(this, String.format("Error executing Task04115LowercaseIdentifierUrls: %s", e.getMessage()),
                    e);
            throw new DotDataException(
                String.format("Error executing Task04115LowercaseIdentifierUrls: %s", e.getMessage()), e);
        } finally {
            //Enable trigger.
            try {
                final DotConnect dotConnect = new DotConnect();
                final List<String> statements = getEnableTriggerQuery();
                for (String statement : statements) {
                    dotConnect.executeStatement(statement);
                }
            } catch (SQLException e) {
                Logger.error(this, String
                    .format("Error executing Task04115LowercaseIdentifierUrls, trying to ENABLE trigger: %s",
                        e.getMessage()), e);
                throw new DotDataException(
                    String.format("Error executing Task04115LowercaseIdentifierUrls, trying to ENABLE trigger: %s",
                        e.getMessage()), e);
            }
        }
    }

	/**
	 * Returns the appropriate <code>SELECT</code> query based on the current
	 * database.
	 * 
	 * @return Returns the correct query for MySQL, PostgreSQL, MSSQL, and
	 *         Oracle.
	 */
	private String getSelectQuery() {
		return DbConnectionFactory.isMsSql() ? SELECT_QUERY_MSSQL
				: DbConnectionFactory.isMySql() ? SELECT_QUERY_MYSQL
						: DbConnectionFactory.isOracle() ? SELECT_QUERY_ORACLE
								: DbConnectionFactory.isPostgres() ? SELECT_QUERY_POSTGRES : "";
	}

	private List<String> getDisableTriggerQuery() {
		return DbConnectionFactory.isMsSql() ? SQLUtil.tokenize(MS_SQL_DISABLE_TRIGGER)
			: DbConnectionFactory.isMySql() ? Collections.singletonList("DROP TRIGGER IF EXISTS check_parent_path_when_update")
				: DbConnectionFactory.isOracle() ? Collections.singletonList("ALTER TRIGGER identifier_parent_path_check DISABLE")
					: DbConnectionFactory.isPostgres() ? Collections.singletonList("ALTER TABLE identifier DISABLE TRIGGER identifier_parent_path_trigger")
						: Collections.singletonList("");
	}

	private List<String> getEnableTriggerQuery() {
		return DbConnectionFactory.isMsSql() ? Collections.singletonList("ALTER TABLE identifier ENABLE TRIGGER check_identifier_parent_path")
			: DbConnectionFactory.isMySql() ? SQLUtil.tokenize(MY_SQL_CREATE_TRIGGER)
				: DbConnectionFactory.isOracle() ? Collections.singletonList("ALTER TRIGGER identifier_parent_path_check ENABLE")
					: DbConnectionFactory.isPostgres() ? Collections.singletonList("ALTER TABLE identifier ENABLE TRIGGER identifier_parent_path_trigger")
                        : Collections.singletonList("");
	}

	private final String MS_SQL_DISABLE_TRIGGER = "SET TRANSACTION ISOLATION LEVEL READ COMMITTED;\n"
		+ "ALTER TABLE identifier DISABLE TRIGGER check_identifier_parent_path;";

	private final String MY_SQL_CREATE_TRIGGER = "DROP TRIGGER IF EXISTS check_parent_path_when_update;\n"
        + "CREATE TRIGGER check_parent_path_when_update  BEFORE UPDATE\n"
        + "on identifier\n"
        + "FOR EACH ROW\n"
        + "BEGIN\n"
        + "DECLARE idCount INT;\n"
        + "DECLARE canUpdate boolean default false;\n"
        + " IF @disable_trigger IS NULL THEN\n"
        + "   select count(id)into idCount from identifier where asset_type='folder' and CONCAT(parent_path,asset_name,'/')= NEW.parent_path and host_inode = NEW.host_inode and id <> NEW.id;\n"
        + "   IF(idCount > 0 OR NEW.parent_path = '/' OR NEW.parent_path = '/System folder') THEN\n"
        + "     SET canUpdate := TRUE;\n"
        + "   END IF;\n"
        + "   IF(canUpdate = FALSE) THEN\n"
        + "     delete from Cannot_update_for_this_path_does_not_exist_for_the_given_host;\n"
        + "   END IF;\n"
        + " END IF;\n"
        + "END";

}
