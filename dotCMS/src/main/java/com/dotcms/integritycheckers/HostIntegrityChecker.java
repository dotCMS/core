package com.dotcms.integritycheckers;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.com.csvreader.CsvWriter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Host integrity checker implementation
 *
 * @author Victor Alfaro
 */
public class HostIntegrityChecker extends AbstractIntegrityChecker {
    private static final String INTEGER_KEYWORD = getIntegerKeyword();

    /**
     * Gets the integrity type for this particular integrity checker.
     *
     * @return {@link IntegrityType}.HOSTS
     */
    @Override
    public final IntegrityType getIntegrityType() {
        return IntegrityType.HOSTS;
    }

    /**
     * Generates a CSV file based on results returned by a query fetching duplicated contentt.
     *
     * @param outputPath
     *            location to store cvs files; for example outputPath =
     *            ConfigUtils.getIntegrityPath() + File.separator + endpointId;
     * @return the {@link File} representation of where the results are stored
     * @throws DotDataException
     * @throws IOException
     */
    @Override
    public File generateCSVFile(final String outputPath) throws DotDataException, IOException {
        final String outputFile = getOutputFilePath(outputPath);

        File csvFile;
        CsvWriter writer = null;

        try {
            csvFile = new File(outputFile);
            Logger.info(this, String.format("Generating Host integrity check CSV file: %s", csvFile.getAbsoluteFile()));
            writer = new CsvWriter(new FileWriter(csvFile, true), '|');

            final String hostField = resolveHostField();
            final Connection conn = DbConnectionFactory.getConnection();
            try (final PreparedStatement statement = getCsvQuery(conn, hostField)) {
                try (final ResultSet rs = statement.executeQuery()) {
                    int count = 0;

                    while (rs.next()) {
                        writer.write(rs.getString("inode"));
                        writer.write(rs.getString("identifier"));
                        writer.write(rs.getString("working_inode"));
                        writer.write(rs.getString("live_inode"));
                        writer.write(String.valueOf(rs.getLong("language_id")));
                        writer.write(rs.getString(hostField));
                        writer.endRecord();
                        count++;

                        if (count == 1000) {
                            writer.flush();
                            count = 0;
                        }
                    }
                }
            } catch (final SQLException e) {
                throw new DotDataException(
                        String.format(
                                "An error occurred when generating the CSV file for Hosts to '%s': %s",
                                outputFile,
                                e.getMessage()),
                        e);
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
     * Based on previously generated CSV file, add results to a temporary page to discover possible conflicts,
     * When detected, conflicts are stored in hosts-ir table.
     * @param endpointId
     *            Server identifier were we need generate conflicts report
     * @return true when conflicts are detected, otherwise false
     * @throws Exception
     */
    @Override
    public boolean generateIntegrityResults(final String endpointId) throws Exception {
        try {
            final CsvReader hosts = new CsvReader(
                    ConfigUtils.getIntegrityPath() +
                            File.separator +
                            endpointId +
                            File.separator +
                            getIntegrityType().getDataToCheckCSVName(),
                    '|',
                    StandardCharsets.UTF_8);
            boolean tempCreated = false;
            final String tempTableName = getTempTableName(endpointId);
            // let's create a temp table AND insert all the records coming from the CSV file
            final String tempKeyword = DbConnectionFactory.getTempKeyword();
            final boolean isOracle = DbConnectionFactory.isOracle();
            final StringBuilder createBuilder = new StringBuilder("CREATE ")
                    .append(tempKeyword)
                    .append(" TABLE ")
                    .append(tempTableName)
                    .append(" (inode VARCHAR(36),")
                    .append(" identifier VARCHAR(36) not null,")
                    .append(" working_inode VARCHAR(36) not null,")
                    .append(" live_inode VARCHAR(36),")
                    .append(" language_id ").append(INTEGER_KEYWORD).append(",")
                    .append(" host VARCHAR(255),")
                    .append(" PRIMARY KEY (inode))");
            if (isOracle) {
                createBuilder.append(" ON COMMIT PRESERVE ROWS ");
            }
            final String createTempTable = isOracle
                    ? createBuilder.toString().replaceAll("VARCHAR\\(", "VARCHAR2\\(")
                    : createBuilder.toString();
            final String insertTempTable = "INSERT INTO " +
                    tempTableName +
                    " (inode, identifier, working_inode, live_inode, language_id, host)" +
                    " VALUES(?, ?, ?, ?, ?, ?)";
            final String hostField = resolveHostField();
            final DotConnect dc = new DotConnect();

            while(hosts.readRecord()) {
                if (!tempCreated) {
                    dc.executeStatement(createTempTable);
                    tempCreated = true;
                }

                String identifier = null;
                try {
                    final String inode = getStringIfNotBlank("inode", hosts.get(0));
                    identifier = getStringIfNotBlank("identifier", hosts.get(1));
                    final String workingInode = getStringIfNotBlank("working_inode", hosts.get(2));
                    final String liveInode = StringUtils.defaultIfBlank(hosts.get(3), null);
                    final Long languageId = Long.valueOf(getStringIfNotBlank("language_id", hosts.get(4)));
                    final String host = getStringIfNotBlank(hostField, hosts.get(5));
                    dc.setSQL(insertTempTable)
                            .addParam(inode)
                            .addParam(identifier)
                            .addParam(workingInode)
                            .addParam(liveInode)
                            .addParam(languageId)
                            .addParam(host)
                            .loadResult();
                } catch (final DotDataException e) {
                    hosts.close();
                    final String hostId = StringUtils.defaultIfBlank(identifier, StringUtils.EMPTY);
                    throw new DotDataException(
                            String.format(
                                    "An error occurred when generating temp table for host '%s': %s",
                                    hostId,
                                    e.getMessage()),
                            e);
                }
            }

            hosts.close();
            if (!tempCreated) {
                return false;
            }

            // compare the data from the CSV to the local db data AND see if we
            // have conflicts
            final String conflictSql = " FROM identifier i" +
                    " JOIN contentlet c ON i.id = c.identifier" +
                    " JOIN contentlet_version_info cvi ON (c.identifier = cvi.identifier" +
                    " AND c.language_id = cvi.lang" +
                    " AND c.inode = cvi.working_inode)" +
                    " JOIN structure s ON c.structure_inode = s.inode" +
                    " JOIN " + tempTableName + " ht ON (c." + hostField + " = ht.host" +
                    " AND c.language_id = cvi.lang)" +
                    " WHERE i.asset_type = 'contentlet'" +
                    " AND i.asset_subtype = 'Host'" +
                    " AND i.host_inode = ?" +
                    " AND (c.identifier <> ht.identifier)" +
                    " AND s.name = 'Host'";
            dc.setSQL("SELECT DISTINCT 1" + conflictSql).addParam(Host.SYSTEM_HOST);
            final List<Map<String, Object>> results = dc.loadObjectResults();

            if (!results.isEmpty()) {
                // if we have conflicts, lets create a table out of them
                final String insertStmt = "INSERT INTO " + getIntegrityType().getResultsTableName() +
                        " (local_identifier, remote_identifier, endpoint_id, local_working_inode, local_live_inode," +
                        " remote_working_inode, remote_live_inode, language_id, host)" +
                        " SELECT DISTINCT c.identifier as local_identifier, ht.identifier as remote_identifier," +
                        " '" + endpointId + "', cvi.working_inode as local_working_inode," +
                        " cvi.live_inode as local_live_inode, ht.working_inode as remote_working_inode," +
                        " ht.live_inode as remote_live_inode, c.language_id, c." + hostField + " as host" +
                        conflictSql;
                dc.setSQL(insertStmt)
                        .addParam(Host.SYSTEM_HOST)
                        .loadResult();
            }

            return dc.getRecordCount(
                    getIntegrityType().getResultsTableName(),
                    "WHERE endpoint_id = '"+ endpointId + "'") > 0;
        } catch (final Exception e) {
            throw new Exception(
                    String.format(
                            "Error running the Hosts Integrity Check for Endpoint '%s': %s",
                            endpointId,
                            e.getMessage()),
                    e);
        }
    }

    /**
     * Fixes hosts inconsistencies for a given server id.
     * Fixing a host consist updating its inode AND identifier with the ones received from the other end.
     *
     * @param endpointId endpoint identifier
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    public void executeFix(final String endpointId) throws DotDataException, DotSecurityException {
        // remove from the index all the content under each conflicted host
        final DotConnect dc = new DotConnect().setSQL(
                        "SELECT remote_working_inode, local_working_inode, remote_live_inode, local_live_inode," +
                        " remote_identifier, local_identifier, language_id, host" +
                        " FROM " + getIntegrityType().getResultsTableName() +
                        " WHERE endpoint_id = ?")
                .addParam(endpointId);

        final List<Map<String, Object>> results = dc.loadObjectResults();
        final Map<String, Integer> versionCount = new HashMap<>();
        results.forEach(result -> {
            final String oldIdentifier = (String) result.get("local_identifier");
            final Integer existent = versionCount.get(oldIdentifier);
            final Integer counter = existent == null ? 1 : existent + 1;
            versionCount.put(oldIdentifier, counter);
        });

        for (final Map<String, Object> result : results) {
            final String oldHostIdentifier = (String) result.get("local_identifier");
            final int counter = versionCount.get(oldHostIdentifier);
            boolean isLastConflict = counter == 1;

            fixHostConflicts(result, isLastConflict);
            if (!isLastConflict) {
                // Decrease version counter if greater than 1
                versionCount.put(oldHostIdentifier, counter - 1);
            }
        }
    }

    /**
     * Perform actual fix in real tables based on hosts_ir table record.
     *
     * @param row map representing a row from hosts_ir table
     * @param isLastConflict flag telling  if this is the last conflict
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void fixHostConflicts(final Map<String, Object> row,
                                  final boolean isLastConflict)
            throws DotDataException, DotSecurityException {
        final String localHostIdentifier = (String) row.get("local_identifier");
        final String remoteHostIdentifier = (String) row.get("remote_identifier");
        final String localWorkingInode = (String) row.get("local_working_inode");
        final String localLiveInode = (String) row.get("local_live_inode");
        final String remoteWorkingInode = (String) row.get("remote_working_inode");
        final String remoteLiveInode = (String) row.get("remote_live_inode");
        final Long languageId = DbConnectionFactory.isOracle() || DbConnectionFactory.isMsSql()
                ? new Long(((BigDecimal) row.get("language_id")).toPlainString())
                : (Long) row.get("language_id");
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final Contentlet existingWorkingHost = contentletAPI.find(localWorkingInode, systemUser, false);

        Contentlet existingLiveHost = null;
        try {
            existingLiveHost = contentletAPI.find(localLiveInode, systemUser, false);
        } catch (DotHibernateException e) { /* No Live Version */ }

        final DotConnect dc = new DotConnect();
        dc.setSQL("SELECT id FROM identifier WHERE id = ?");
        dc.addParam(remoteHostIdentifier);
        final List<Map<String, Object>> results = dc.loadObjectResults();

        // If not existing, add the new Identifier with a temporary asset
        // name. We need to have a dummy asset name because there is a
        // constraint that limit us to use the final one
        if (results == null || results.isEmpty()) {
            final String temporalAssetName = "TEMP_" + UUID.randomUUID();
            dc.setSQL("INSERT INTO identifier (id, parent_path, asset_name, host_inode, asset_type, syspublish_date, sysexpire_date)" +
                    " SELECT ?, parent_path, '" + temporalAssetName + "', host_inode, asset_type, syspublish_date, sysexpire_date" +
                    " FROM identifier WHERE id = ?");
            dc.addParam(remoteHostIdentifier);
            dc.addParam(localHostIdentifier);
            dc.loadResult();
        }

        if (dc.getRecordCount("inode", "WHERE inode = '" + remoteWorkingInode + "'") == 0) {
            // Insert the new Inodes records so they can be used in the contentlet
            dc.setSQL("INSERT INTO inode(inode, owner, idate, type)" +
                    " SELECT ?, owner, idate, type FROM inode i WHERE i.inode = ?");
            dc.addParam(remoteWorkingInode);
            dc.addParam(localWorkingInode);
            dc.loadResult();
        }

        if (!remoteWorkingInode.equals(remoteLiveInode)
                && UtilMethods.isSet(remoteLiveInode)
                && UtilMethods.isSet(localLiveInode)
                && dc.getRecordCount("inode", "WHERE i.inode = '" + remoteLiveInode + "'") == 0) {
            dc.setSQL("INSERT INTO inode(inode, owner, idate, type)" +
                    " SELECT ?, owner, idate, type FROM inode i WHERE i.inode = ?");
            dc.addParam(remoteLiveInode);
            dc.addParam(localLiveInode);
            dc.loadResult();
        }

        // Insert the new working Contentlet (Host) record with the new Inode
        insertHostForInode(
                localHostIdentifier,
                remoteHostIdentifier,
                remoteWorkingInode,
                languageId,
                dc,
                "cvi.working_inode");

        if (!remoteWorkingInode.equals(remoteLiveInode)
                && UtilMethods.isSet(remoteLiveInode)
                && UtilMethods.isSet(localLiveInode)) {
            insertHostForInode(
                    localHostIdentifier,
                    remoteHostIdentifier,
                    remoteLiveInode,
                    languageId,
                    dc,
                    "cvi.live_inode");
        }

        insertHostVersionInfo(
                localLiveInode,
                localWorkingInode,
                remoteLiveInode,
                remoteWorkingInode,
                localHostIdentifier,
                remoteHostIdentifier,
                languageId,
                dc);

        // Update other workflow task with new Identifier
        dc.setSQL("UPDATE workflow_task SET webasset = ? WHERE webasset = ? AND language_id = ?");
        dc.addParam(remoteHostIdentifier);
        dc.addParam(localHostIdentifier);
        dc.addParam(languageId);
        dc.loadResult();

        // Remove the live_inode references from Contentlet_version_info
        dc.setSQL("DELETE FROM contentlet_version_info WHERE identifier = ? AND working_inode = ? AND lang = ? ");
        dc.addParam(localHostIdentifier);
        dc.addParam(localWorkingInode);
        dc.addParam(languageId);
        dc.loadResult();

        // Remove the conflicting version of the Contentlet record
        dc.setSQL("DELETE FROM contentlet WHERE identifier = ? AND inode = ? AND language_id = ?");
        dc.addParam(localHostIdentifier);
        dc.addParam(localWorkingInode);
        dc.addParam(languageId);
        dc.loadResult();

        final boolean liveInodesDefinedAndDiffFromLocal = UtilMethods.isSet(localLiveInode)
                && UtilMethods.isSet(remoteLiveInode)
                && !localLiveInode.equals(localWorkingInode);
        if (liveInodesDefinedAndDiffFromLocal) {
            // Remove the conflicting version of the Contentlet record
            dc.setSQL("DELETE FROM contentlet WHERE identifier = ? AND inode = ? AND language_id = ?")
                    .addParam(localHostIdentifier)
                    .addParam(localLiveInode)
                    .addParam(languageId)
                    .loadResult();
        }

        // Update other Contentlet languages with new Identifier
        dc.setSQL("UPDATE contentlet SET identifier = ? WHERE identifier = ? AND language_id = ?")
                .addParam(remoteHostIdentifier)
                .addParam(localHostIdentifier)
                .addParam(languageId)
                .loadResult();

        // Update previous version of the Contentlet_version_info with
        // new Identifier
        dc.setSQL("UPDATE contentlet_version_info SET identifier = ? WHERE identifier = ? AND lang = ?")
                .addParam(remoteHostIdentifier)
                .addParam(localHostIdentifier)
                .addParam(languageId)
                .loadResult();

        if (isLastConflict) {
            // Remove the old Identifier record
            dc.setSQL("DELETE FROM identifier WHERE id = ?")
                    .addParam(localHostIdentifier)
                    .loadResult();
        }

        // Remove the old Inode record
        dc.setSQL("DELETE FROM inode WHERE inode = ?")
                .addParam(localWorkingInode)
                .loadResult();

        if (liveInodesDefinedAndDiffFromLocal) {
            // Remove the old Inode record
            dc.setSQL("DELETE FROM inode WHERE inode = ?");
            dc.addParam(localLiveInode);
            dc.loadResult();
        }

        // Create new contentlet using the current information, this is for the working contentlet
        generateNewContentlet(existingWorkingHost, remoteHostIdentifier, remoteWorkingInode);

        if (UtilMethods.isSet(localLiveInode) && !localLiveInode.equals(localWorkingInode)) {
            // Create new contentlet using the current information, this is for the live contentlet.
            // This is needed it when live and working are different
            generateNewContentlet(existingLiveHost, remoteHostIdentifier, remoteLiveInode);
        }

        // Remove the Lucene index for the old page
        cleanIndex(existingWorkingHost, existingLiveHost);
    }

    /**
     * Prepares duplicates detection query to run prior to creating CSV file.
     *
     * @param conn database connection to be reused
     * @param hostField column name used to retrieved the host name
     * @return a ready to use {@link PreparedStatement}
     * @throws SQLException
     */
    private PreparedStatement getCsvQuery(final Connection conn, final String hostField) throws SQLException {
        final String sql =
                "SELECT c.inode, c.identifier, cvi.working_inode, cvi.live_inode, c.language_id, " + hostField +
                        " FROM contentlet c" +
                        " JOIN identifier i ON c.identifier = i.id" +
                        " JOIN structure s ON c.structure_inode = s.inode" +
                        " JOIN contentlet_version_info cvi ON (c.identifier = cvi.identifier" +
                        " AND c.language_id = cvi.lang)" +
                        " WHERE i.asset_type = 'contentlet'" +
                        " AND i.asset_subtype = 'Host'" +
                        " AND s.name = 'Host'" +
                        " AND c.identifier <> ?";
        final PreparedStatement statement = conn.prepareStatement(sql);
        statement.setString(1, Host.SYSTEM_HOST);
        return statement;
    }

    /**
     * Create a new contentlet from the old one. This method basically copy all
     * the information from the old contentlet and paste it in the new
     * contentlet, after that process is over we need to add the new contentlet
     * to lucene index.
     *
     * @param existingContentlet
     *            contains the information that we need to copy to the new
     *            contentlet
     * @param newContentletIdentifier
     *            identifier for the new contentlet
     * @param remoteInode
     *            inode for the new contentlet
     * @return new generated contentlet
     * @throws DotContentletStateException
     * @throws DotRuntimeException
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    private Contentlet generateNewContentlet(final Contentlet existingContentlet,
                                             final String newContentletIdentifier,
                                             final String remoteInode)
            throws DotContentletStateException, DotRuntimeException, DotSecurityException, DotDataException {

        final Contentlet newContentlet = new Contentlet();
        newContentlet.setContentTypeId(existingContentlet.getContentTypeId());
        APILocator.getContentletAPI().copyProperties(newContentlet, existingContentlet.getMap());
        newContentlet.setIdentifier(newContentletIdentifier);
        newContentlet.setInode(remoteInode);

        // Add new contentlet to lucene index
        APILocator.getContentletIndexAPI().addContentToIndex(newContentlet);

        return newContentlet;
    }

    /**
     * Clean index from lucene
     *
     * @param existingWorkingContentlet
     * @param existingLiveContentlet
     * @throws DotHibernateException
     */
    private void cleanIndex(final Contentlet existingWorkingContentlet,
                            final Contentlet existingLiveContentlet) throws DotDataException {
        final ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();
        indexAPI.removeContentFromIndex(existingWorkingContentlet);

        if (UtilMethods.isSet(existingLiveContentlet)
                && !existingWorkingContentlet.getInode().equals(existingLiveContentlet.getInode())) {
            indexAPI.removeContentFromIndex(existingLiveContentlet);
        }
    }

    /**
     * Convenience method that generates an SQL insert statement for a large contenetlet table from join between
     * contentlet and contentlet_version_info tables
     *
     * @param cviInodeColumn value to
     * @return {@link String} representing the SQL statement
     */
    private String generateHostInsertSql(final String cviInodeColumn) {
        final String insertHostSql = String.format(
                "INSERT INTO contentlet(inode, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, disabled_wysiwyg, identifier, language_id, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25)" +
                " SELECT ?, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, disabled_wysiwyg, ?, ?, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25" +
                " FROM contentlet c INNER JOIN contentlet_version_info cvi on (c.inode = %s AND c.language_id = cvi.lang) WHERE c.identifier = ? and c.language_id = ?",
                cviInodeColumn);
        return DbConnectionFactory.isMySql() ? insertHostSql.replaceAll("\"", "`") : insertHostSql;
    }

    /**
     * Performs an insert statement on contentlet table using previous {@code generateHostInsertSql} method using
     * provided columns and filter values.
     *
     * @param oldHostIdentifier old host identifier
     * @param newHostIdentifier new host identifier
     * @param inode value to be used as inode
     * @param languageId value to be used as language identifier
     * @param dc db connection
     * @param cviInodeColumn contentlet version info column name
     * @throws DotDataException
     */
    private void insertHostForInode(final String oldHostIdentifier,
                                    final String newHostIdentifier,
                                    final String inode,
                                    final Long languageId,
                                    final DotConnect dc,
                                    final String cviInodeColumn) throws DotDataException {
        dc.setSQL(generateHostInsertSql(cviInodeColumn))
                .addParam(inode)
                .addParam(newHostIdentifier)
                .addParam(languageId)
                .addParam(oldHostIdentifier)
                .addParam(languageId)
                .loadResult();
    }

    /**
     * Performs an insert statement on contentlet_version_info table from returned results of a query using provided
     * columns and filter values.
     *
     * @param localLiveInode local live inode
     * @param localWorkingInode local working inode
     * @param remoteLiveInode remote live inode
     * @param remoteWorkingInode remote working inode
     * @param oldHostIdentifier old host identifier
     * @param newHostIdentifier new host identifier
     * @param languageId language identifier
     * @param dc db connection
     * @throws DotDataException
     */
    private void insertHostVersionInfo(final String localLiveInode,
                                       final String localWorkingInode,
                                       final String remoteLiveInode,
                                       final String remoteWorkingInode,
                                       final String oldHostIdentifier,
                                       final String newHostIdentifier,
                                       final Long languageId,
                                       final DotConnect dc) throws DotDataException {
        final String liveInodeValue;
        if (UtilMethods.isSet(localLiveInode) && UtilMethods.isSet(remoteLiveInode)) {
            liveInodeValue = "?";
        } else if (UtilMethods.isSet(localLiveInode) && !localWorkingInode.equals(localLiveInode)){
            liveInodeValue = "live_inode";
        } else {
            liveInodeValue = "null";
        }

        if (dc.getRecordCount(
                "contentlet_version_info",
                "WHERE identifier = '" + remoteWorkingInode + "' AND lang = " + languageId) == 0) {
            dc.setSQL(
                    String.format(
                            "INSERT INTO contentlet_version_info" +
                            " (identifier, lang, working_inode, live_inode, deleted, locked_by, locked_on, version_ts)" +
                            " SELECT ?, ?, ?, %s, deleted, locked_by, locked_on, version_ts" +
                            " FROM contentlet_version_info" +
                            " WHERE identifier = ?" +
                            " AND working_inode = ?" +
                            " AND lang = ?",
                            liveInodeValue))
                    .addParam(newHostIdentifier)
                    .addParam(languageId)
                    .addParam(remoteWorkingInode);
            if ("?".equals(liveInodeValue)) {
                dc.addParam(remoteLiveInode);
            }
            dc.addParam(oldHostIdentifier)
                    .addParam(localWorkingInode)
                    .addParam(languageId)
                    .loadResult();
        }
    }

    /**
     * Resolves which contenlet table column to use as the hostname.
     *
     * @return column name to be used as host name
     */
    private String resolveHostField() {
        return new DotConnect()
                .setSQL("select f.field_contentlet" +
                        " from field f" +
                        " JOIN structure s on s.inode = f.structure_inode" +
                        " where s.velocity_var_name = 'Host'" +
                        " AND f.velocity_var_name = 'hostName'")
                .getString("field_contentlet");
    }
}