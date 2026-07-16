package com.dotcms.integritycheckers;

import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.com.csvreader.CsvWriter;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Base class for all the integrity checkers implementation
 *
 * @author Rogelio Blanco
 * @version 1.0
 * @since 06-10-2015
 *
 */
public abstract class AbstractIntegrityChecker implements IntegrityChecker {

    /**
     * Creates temporary TABLE for integrity checking purposes.
     *
     * @param endpointId
     * @return temporal table name
     */
    protected String getTempTableName(final String endpointId) {
        return getTempTableName(endpointId, StringUtils.EMPTY);
    }

    /**
     * Creates temporary TABLE for integrity checking purposes.
     *
     * @param endpointId
     * @param type
     * @return temporal table name
     */
	protected String getTempTableName(String endpointId, String type) {
		return getTempTableName(endpointId, getIntegrityType(), type);
	}

    /**
     * Creates temporary TABLE for integrity checking purposes.
     *
     * @param endpointId
     * @param type
     * @param suffixName
     * @return temporal table name
     */
    protected String getTempTableName(String endpointId, IntegrityType type, String suffixName) {

    	if (!UtilMethods.isSet(endpointId)) {
            return null;
        }

        final String endpointIdforDB = endpointId.replace("-", "");
        String resultsTableName = type.name().toLowerCase() + "_temp"+suffixName.toLowerCase()+"_" + endpointIdforDB;

        if (DbConnectionFactory.isOracle()) {
            resultsTableName = resultsTableName.substring(0, 29);
        } else if (DbConnectionFactory.isMsSql()) {
            resultsTableName = "#" + resultsTableName;
        }

        return resultsTableName;
    }

    /**
     * @see IntegrityChecker
     */
    @Override
    public String[] getTempTableNames(String endpointId) {
    	return new String[]{ getTempTableName(endpointId) };
    }

    /**
     * @see IntegrityChecker
     */
    @Override
    public void discardConflicts(final String endpointId) throws DotDataException {
        if (getIntegrityType().hasResultsTable()) {
            discardConflicts(endpointId, getIntegrityType());
        } else {
            Logger.warn(this, "Results table not defined for integrity type = ["
                    + getIntegrityType() + "], endpointId = [" + endpointId +"]");
        }
    }

    /**
     * Helper to discard conflicts from an specific integrity type and endpoint
     * id
     * 
     * @param endpointId
     * @param type
     * @throws DotDataException
     */
    private void discardConflicts(final String endpointId, IntegrityType type)
            throws DotDataException {

        final String resultsTableName = type.getResultsTableName();
        DotConnect dc = new DotConnect();
        dc.setSQL("delete from " + resultsTableName + " where endpoint_id = ?")
        .addParam(endpointId)
        .loadResult();

    }

    /**
     * Creates CSV file with Contentlet information from End Point server
     * depending on the structure type.
     *
     * @param outputFile
     *            - The file containing the list of pages.
     * @param structureTypeId
     *            - The type of content type {@link Structure}.
     * @return a {@link File} with the page information.
     * @throws DotDataException
     *             An error occurred when querying the database.
     * @throws IOException
     *             An error occurred when writing to the file.
     */
    protected File generateContentletsCSVFile(final String outputFile, final int structureTypeId)
            throws DotDataException, IOException {
        File csvFile;
        CsvWriter writer = null;

        try {
            csvFile = new File(outputFile);
            writer = new CsvWriter(new FileWriter(csvFile, true), '|');

            // Query the new content pages
            final String query = new StringBuilder("SELECT DISTINCT c.identifier, ")
                    .append("cvi.working_inode, cvi.live_inode, i.parent_path, i.asset_name, i.host_inode, c.language_id ")
                    .append("FROM contentlet_version_info cvi ")
                    .append("INNER JOIN contentlet c ON (c.identifier = cvi.identifier AND c.language_id = cvi.lang) ")
                    .append("INNER JOIN structure s ON (s.inode = c.structure_inode AND s.structuretype = ")
                    .append(structureTypeId).append(") ")
                    .append("INNER JOIN identifier i ON (i.id = c.identifier)").toString();

            final Connection conn = DbConnectionFactory.getConnection();
            try (final PreparedStatement statement = conn.prepareStatement(query)) {
                try (final ResultSet rs = statement.executeQuery()) {
                    int count = 0;

                    while (rs.next()) {
                        writer.write(rs.getString("working_inode"));
                        writer.write(rs.getString("live_inode"));
                        writer.write(rs.getString("identifier"));
                        writer.write(rs.getString("parent_path"));
                        writer.write(rs.getString("asset_name"));
                        writer.write(rs.getString("host_inode"));
                        writer.write(rs.getString("language_id"));
                        writer.endRecord();

                        count++;

                        if (count == 1000) {
                            writer.flush();
                            count = 0;
                        }
                    }
                }
            } catch (final SQLException e) {
                throw new DotDataException(String.format("An error occurred when generating the CSV file for " +
                        "Contentlets for Content Type ID '%s' to file '%s': %s", structureTypeId, outputFile, e
                        .getMessage()), e);
            }
        } finally {
            // Close writer
            if (writer != null) {
                writer.close();
            }
        }

        return csvFile;
    }

    /**
     * Checks the existence of content conflicts in a specific endpoint by using
     * the structure type. When there is conflicts there is an step that inserts
     * the results into the result table of the integrity checker.
     * 
     * <p>
     * NOTE: This is a generic method that can be use when you need to do an
     * integrity check for a contentlet
     * </p>
     *
     * @param endpointId
     *            - The ID of the endpoint where conflicts will be detected.
     * @param structureType
     *            - Contentlet structure type, located at {@link Structure}
     *            class
     * @throws IOException
     *             An error occurred when reading the file containing the page
     *             data.
     * @throws SQLException
     *             There's a syntax error in a SQL statement.
     * @throws DotDataException
     *             An error occurred when interacting with the database.
     */
    protected void processContentletIntegrityByStructureType(final String endpointId,
            final int structureType) throws IOException, SQLException, DotDataException {
        CsvReader contentFile = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator
                + endpointId + File.separator + getIntegrityType().getDataToCheckCSVName(), '|',
                Charset.forName("UTF-8"));

        DotConnect dc = new DotConnect();
        final String tempTableName = getTempTableName(endpointId);

		final String INSERT_TEMP_TABLE = "INSERT INTO " + tempTableName + " (working_inode, live_inode, identifier, full_path_lc, host_identifier, language_id) VALUES(?,?,?,?,?,?)";
		boolean hasResultsToCheck = false;
		while (contentFile.readRecord()) {
			hasResultsToCheck = true;
			String contentIdentifier = null;
			try {
				contentIdentifier = getStringIfNotBlank("identifier",
						contentFile.get(2));
				final String workingInode = getStringIfNotBlank(
						"working_inode", contentFile.get(0));
				final String liveInode = contentFile.get(1);
				final String contentParentPath = getStringIfNotBlank(
						"parent_path", contentFile.get(3));
				final String contentName = getStringIfNotBlank("asset_name",
						contentFile.get(4));
				final String contentHostIdentifier = getStringIfNotBlank(
						"host_identifier", contentFile.get(5));
				final String contentLanguage = getStringIfNotBlank(
						"language_id", contentFile.get(6));
				dc.setSQL(INSERT_TEMP_TABLE);
				dc.addParam(workingInode);
				dc.addParam(liveInode);
				dc.addParam(contentIdentifier);
                dc.addParam((contentParentPath + contentName).toLowerCase());
				dc.addParam(contentHostIdentifier);
				dc.addParam(Long.valueOf(contentLanguage));
				dc.loadResult();
			} catch (final DotDataException e) {
				contentFile.close();
				final String assetId = UtilMethods.isSet(contentIdentifier) ? contentIdentifier
						: "";
                throw new DotDataException(String.format("An error occurred when generating temp table for asset '%s':" +
                        " %s", assetId, e.getMessage()), e);
            }
        }
        contentFile.close();
        if (!hasResultsToCheck) {
        	return;
        }

        // Compare the data from the CSV to the local database data and see if
        // we have conflicts.
        String selectSQL = new StringBuilder("SELECT DISTINCT ")
                .append("li.asset_name as ")
                .append(getIntegrityType().getFirstDisplayColumnLabel())
                .append(", lcvi.working_inode as local_working_inode, lcvi.live_inode as local_live_inode, ")
                .append("t.working_inode as remote_working_inode, t.live_inode as remote_live_inode, ")
                .append("lc.identifier as local_identifier, t.identifier as remote_identifier, lc.language_id ")
                .append("FROM  identifier li ")
                .append("INNER JOIN contentlet lc ON (lc.identifier = li.id and li.asset_type = 'contentlet') ")
                .append("INNER JOIN contentlet_version_info lcvi ON (lc.identifier = lcvi.identifier) ")
                .append("INNER JOIN structure ls ON (lc.structure_inode = ls.inode and ls.structuretype = ")
                .append(structureType).append(") ").append("INNER JOIN ").append(tempTableName)
                .append(" t ON (li.full_path_lc = t.full_path_lc ")
                .append("AND li.host_inode = host_identifier AND lc.identifier <> t.identifier ")
                .append("AND lc.language_id = t.language_id)").toString();

        if (DbConnectionFactory.isOracle()) {
            selectSQL = selectSQL.replaceAll(" as ", " ");
        }

        dc.setSQL(selectSQL);

        final List<Map<String, Object>> results = dc.loadObjectResults();

        // If we have conflicts, lets create a table out of them.
        if (!results.isEmpty()) {

            String insertSQL = new StringBuilder("INSERT INTO ")
                    .append(getIntegrityType().getResultsTableName())
                    .append(" (" + getIntegrityType().getFirstDisplayColumnLabel() + ", local_working_inode, local_live_inode, remote_working_inode, remote_live_inode, ") 
                    .append("local_identifier, remote_identifier, endpoint_id, language_id)")
                    .append(" select DISTINCT li.full_path_lc ")
                    .append(" as ")
                    .append(getIntegrityType().getFirstDisplayColumnLabel())
                    .append(", ")
                    .append("lcvi.working_inode as local_working_inode, lcvi.live_inode as local_live_inode, ")
                    .append("t.working_inode as remote_working_inode, t.live_inode as remote_live_inode, ")
                    .append("lc.identifier as local_identifier, t.identifier as remote_identifier, ")
                    .append("'")
                    .append(endpointId)
                    .append("', t.language_id as language_id ")
                    .append("FROM identifier li ")
                    .append("INNER JOIN contentlet lc ON (lc.identifier = li.id and li.asset_type = 'contentlet') ")
                    .append("INNER JOIN contentlet_version_info lcvi ON (lc.identifier = lcvi.identifier and lc.language_id = lcvi.lang) ")
                    .append("INNER JOIN structure ls ON (lc.structure_inode = ls.inode and ls.structuretype = ")
                    .append(structureType)
                    .append(") INNER JOIN ")
                    .append(tempTableName)
                    .append(" t ON (li.full_path_lc = t.full_path_lc ")
                    .append("AND li.host_inode = host_identifier AND lc.identifier <> t.identifier AND lc.language_id = t.language_id )")
                    .toString();

            if (DbConnectionFactory.isOracle()) {
                insertSQL = insertSQL.replaceAll(" as ", " ");
            }

            dc.executeStatement(insertSQL);
        }
    }

    /**
     * @see IntegrityChecker
     */
    @Override
    public boolean doesIntegrityConflictsDataExist(final String endpointId) throws Exception {
        boolean exists = false;

        if (getIntegrityType().hasResultsTable()) {
            DotConnect dc = new DotConnect();
            dc.setSQL("SELECT 1 FROM " + getIntegrityType().getResultsTableName()
                    + " where endpoint_id = ?");
            dc.addParam(endpointId);

            if (dc.loadObjectResults().isEmpty()) {
                Logger.warn(this, "Results table [" + getIntegrityType() + "] reported no conflicts for endpoint ["
                        + endpointId + "]");
            } else {
                // At least one conflict found
                exists = true;
            }
        }

        return exists;
    }


    protected void createContentletTemporaryTable(final String endpointId) throws SQLException {
        DotConnect dc = new DotConnect();

        // Create a temporary table and insert all the records coming from
        // the CSV file.
        final String tempTableName = getTempTableName(endpointId);
        final String tempKeyword = DbConnectionFactory.getTempKeyword();
        final String integerKeyword = getIntegerKeyword();

        String createTempTableStr = "create " + tempKeyword
                + " table " + tempTableName + " ( "
                + " working_inode varchar(36) not null, "
                + " live_inode varchar(36), "
                + " identifier varchar(36) not null, "
                + " full_path_lc varchar(510), "
                + " host_identifier varchar(36) not null, "
                + " language_id " + integerKeyword + " not null, "
                + " primary key (working_inode, language_id) "
                + " ) "
                + (DbConnectionFactory.isOracle() ? " ON COMMIT PRESERVE ROWS " : "");

        if (DbConnectionFactory.isOracle()) {
            createTempTableStr = createTempTableStr.replaceAll("varchar\\(", "varchar2\\(");
        }

        dc.executeStatement(createTempTableStr);
    }

    /**
     * Returns the correct numeric data type for the language ID, depending on
     * the current database.
     *
     * @return The database-dependent keyword to represent the language ID.
     */
    protected static String getIntegerKeyword() {
        String keyword = null;
        if (DbConnectionFactory.isPostgres()) {
            keyword = "int8";
        } else if (DbConnectionFactory.isMySql()) {
            keyword = "bigint";
        } else if (DbConnectionFactory.isMsSql()) {
            keyword = "numeric(19,0)";
        } else if (DbConnectionFactory.isOracle()) {
            keyword = "number(19,0)";
        }
        return keyword;
    }

	/**
	 * A simple utility method that throws an exception if the {@code value}
	 * parameter is either null or an empty String. This alerts the user when
	 * the data to be checked has an incorrect value.
	 * <p>
	 * This is useful when columns of a DB table DO NOT allow null/empty values.
	 * Only Oracle considers that a null or an empty String are the same,
	 * whereas the other DBs don't. Therefore, a column value with an empty
	 * String can be saved, even if it's not valid.
	 * 
	 * @param columnName
	 *            - The name of the DB column that forbids null/empty values.
	 * @param value
	 *            - The value that is supposed to be saved into the DB column.
	 * @return The value itself if it's not null or empty.
	 * @throws DotDataException
	 *             If the value of the column is null or empty.
	 */
	public static String getStringIfNotBlank(String columnName, String value)
			throws DotDataException {
		if (StringUtils.isBlank(value)) {
			throw new DotDataException("The value of the column '" + columnName
					+ "' cannot be null/empty");
		}
		return value;
	}

    /**
     * Common method which resolves the output file path used for each integrity checker.
     *
     * @param outputPath base output path
     * @return Strin representation of the resolved path
     */
    protected String getOutputFilePath(final String outputPath) {
        return outputPath + File.separator + getIntegrityType().getDataToCheckCSVName();
    }

}
