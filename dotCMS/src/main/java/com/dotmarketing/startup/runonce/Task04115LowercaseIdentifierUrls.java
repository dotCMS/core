package com.dotmarketing.startup.runonce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

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
 * </ul>
 * 
 * @author Jose Castro
 * @version 4.1.0
 * @since Apr 20, 2017
 *
 */
public class Task04115LowercaseIdentifierUrls implements StartupTask {

	private static final String SELECT_QUERY_POSTGRES = "SELECT id, parent_path, asset_name FROM identifier WHERE parent_path <> '/System folder' AND (parent_path <> LOWER(parent_path) OR asset_name <> LOWER(asset_name))";
	private static final String SELECT_QUERY_MYSQL = "SELECT id, parent_path, asset_name FROM identifier WHERE parent_path <> '/System folder' AND (CAST(asset_name AS BINARY) RLIKE '[A-Z]' OR CAST(parent_path AS BINARY) RLIKE '[A-Z]')";
	private static final String SELECT_QUERY_MSSQL = "SELECT id, parent_path, asset_name FROM identifier WHERE (HASHBYTES('SHA1', asset_name) <> HASHBYTES('SHA1', UPPER( asset_name)) AND HASHBYTES('SHA1', asset_name) <> HASHBYTES('SHA1', LOWER(asset_name))) OR (HASHBYTES('SHA1', parent_path) <> HASHBYTES('SHA1', UPPER( parent_path)) AND HASHBYTES('SHA1', parent_path) <> HASHBYTES('SHA1', LOWER(parent_path)) AND parent_path <> '/System folder')";
	private static final String SELECT_QUERY_ORACLE = "SELECT id, parent_path, asset_name FROM identifier WHERE parent_path <> '/System folder' AND (parent_path <> LOWER(parent_path) OR asset_name <> LOWER(asset_name));";

	private static final String UPDATE_QUERY_GENERIC = "UPDATE identifier SET parent_path = ?, asset_name = ? WHERE id = ?";

	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		Connection conn = DbConnectionFactory.getConnection();
		Logger.info(this, "============= Lower-casing Identifier URLs =============");
		try (PreparedStatement selectPs = conn.prepareStatement(getSelectQuery())) {
			final int BATCH_SIZE = 100;
			try (ResultSet identifiers = selectPs.executeQuery();) {
				PreparedStatement updatePs = conn.prepareStatement(UPDATE_QUERY_GENERIC);
				for (int counter = 1; identifiers.next(); counter++) {
					final String identifier = identifiers.getString(1);
					final String originalParentPath = identifiers.getString(2);
					final String originalAssetName = identifiers.getString(3);
					// Double-check that '/System folder' must not be modified
					if ("/system folder".equalsIgnoreCase(originalParentPath)) {
						continue;
					}
					final String parentPathLowercase = originalParentPath.toLowerCase();
					final String assetNameLowercase = originalAssetName.toLowerCase();
					updatePs.setString(1, parentPathLowercase);
					updatePs.setString(2, assetNameLowercase);
					updatePs.setString(3, identifier);
					updatePs.addBatch();
					Logger.info(this, counter + ". " + originalParentPath + originalAssetName);
					if (counter % BATCH_SIZE == 0) {
						// Insert records in batches
						updatePs.executeBatch();
					}
				}
				updatePs.executeBatch();
				Logger.info(this, "========================================================");
			}
		} catch (SQLException e) {
			throw new DotDataException(
					String.format("Error executing Task04115LowercaseIdentifierUrls: %s", e.getMessage()), e);
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

}
