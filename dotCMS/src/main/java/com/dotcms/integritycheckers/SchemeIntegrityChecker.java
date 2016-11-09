package com.dotcms.integritycheckers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.com.csvreader.CsvWriter;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowCache;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.UtilMethods;

/**
 * Scheme integrity checker implementation.
 * 
 * @author Rogelio Blanco
 * @version 1.0
 * @since 06-10-2015
 * 
 */
public class SchemeIntegrityChecker extends AbstractIntegrityChecker {

    @Override
    public final IntegrityType getIntegrityType() {
        return IntegrityType.SCHEMES;
    }

    @Override
    public File generateCSVFile(final String outputPath) throws DotDataException, IOException {
        final String outputFile = outputPath + File.separator
                + getIntegrityType().getDataToCheckCSVName();

        File csvFile = null;
        CsvWriter writer = null;

        try {
            csvFile = new File(outputFile);
            writer = new CsvWriter(new FileWriter(csvFile, true), '|');
            Connection conn = DbConnectionFactory.getConnection();
            try (PreparedStatement statement = conn
                    .prepareStatement("select id, name from workflow_scheme ")) {
                try (ResultSet rs = statement.executeQuery()) {
                    int count = 0;

                    while (rs.next()) {
                        writer.write(rs.getString("id"));
                        writer.write(rs.getString("name"));
                        writer.endRecord();
                        count++;

                        if (count == 1000) {
                            writer.flush();
                            count = 0;
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DotDataException(e.getMessage(), e);
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        return csvFile;
    }

    @Override
    public boolean generateIntegrityResults(final String endpointId) throws Exception {
        try {
            CsvReader schemes = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator
                    + endpointId + File.separator + getIntegrityType().getDataToCheckCSVName(),
                    '|', Charset.forName("UTF-8"));
            boolean tempCreated = false;
            DotConnect dc = new DotConnect();
            String tempTableName = getTempTableName(endpointId);

            String tempKeyword = DbConnectionFactory.getTempKeyword();

            String createTempTable = "create " + tempKeyword + " table " + tempTableName
                    + " (inode varchar(36) not null, name varchar(255) not null, "
                    + " primary key (inode) )";

            if (DbConnectionFactory.isOracle()) {
                createTempTable = createTempTable.replaceAll("varchar\\(", "varchar2\\(");
            }

            final String INSERT_TEMP_TABLE = "insert into " + tempTableName + " (inode, name) values(?,?)";

            while (schemes.readRecord()) {

                if (!tempCreated) {
                    dc.executeStatement(createTempTable);
                    tempCreated = true;
                }

                String schemeInode = null;
                try {
					schemeInode = getStringIfNotBlank("inode", schemes.get(0));
					final String name = getStringIfNotBlank("name",
							schemes.get(1));
					dc.setSQL(INSERT_TEMP_TABLE);
					dc.addParam(schemeInode);
					dc.addParam(name);
					dc.loadResult();
				} catch (DotDataException e) {
					schemes.close();
					final String assetId = UtilMethods.isSet(schemeInode) ? schemeInode
							: "";
					throw new DotDataException(
							"An error occured when generating temp table for asset: "
									+ assetId, e);
				}
            }

            schemes.close();
            if (!tempCreated) {
            	return false;
            }

            // compare the data from the CSV to the local db data and see if we
            // have conflicts
            dc.setSQL("select s.name, s.id as local_inode, wt.inode as remote_inode from workflow_scheme s "
                    + "join " + tempTableName + " wt on s.name = wt.name and s.id <> wt.inode");

            List<Map<String, Object>> results = dc.loadObjectResults();

            if (!results.isEmpty()) {
                // if we have conflicts, lets create a table out of them
                final String INSERT_INTO_RESULTS_TABLE = "insert into "
                        + getIntegrityType().getResultsTableName() 
                        + " (name, local_inode, remote_inode, endpoint_id)"
                        + " select s.name, s.id as local_inode, wt.inode as remote_inode , '"
                        + endpointId + "' from workflow_scheme s " + "join " + tempTableName
                        + " wt on s.name = wt.name and s.id <> wt.inode";

                dc.executeStatement(INSERT_INTO_RESULTS_TABLE);

            }

            return !results.isEmpty();
        } catch (Exception e) {
            throw new Exception("Error running the Workflow Schemes Integrity Check", e);
        }
    }

    @Override
    public void executeFix(final String serverId) throws DotDataException, DotSecurityException {
        DotConnect dc = new DotConnect();

        // TODO: Remove this line when everything works
        // String resultsTableName = getResultsTableName(IntegrityType.SCHEMES);

        try {

            // Delete the schemes cache
            dc.setSQL("SELECT local_inode, remote_inode FROM "
                    + getIntegrityType().getResultsTableName() + " WHERE endpoint_id = ?");
            dc.addParam(serverId);
            List<Map<String, Object>> results = dc.loadObjectResults();
            for (Map<String, Object> result : results) {

                String oldWorkflowId = (String) result.get("local_inode");
                String newWorkflowId = (String) result.get("remote_inode");

                WorkflowCache workflowCache = CacheLocator.getWorkFlowCache();
                // Verify if the workflow is the default one
                WorkflowScheme defaultScheme = workflowCache.getDefaultScheme();
                if (defaultScheme != null && defaultScheme.getId().equals(oldWorkflowId)) {
                    CacheLocator.getCacheAdministrator().remove(workflowCache.defaultKey,
                            workflowCache.getPrimaryGroup());
                } else {
                    // Clear the cache
                    WorkflowScheme oldScheme = APILocator.getWorkflowAPI()
                            .findScheme(oldWorkflowId);
                    List<WorkflowStep> steps = APILocator.getWorkflowAPI().findSteps(oldScheme);

                    for (int i = 0; i < steps.size(); i++) {
                        WorkflowStep workflowStep = steps.get(i);
                        workflowCache.remove(workflowStep);
                    }

                    WorkflowScheme scheme = workflowCache.getScheme(oldWorkflowId);
                    workflowCache.remove(scheme);
                }

                // THIS IS THE NEW CODE

                // 1) Insert dummy temp row on WORKFLOW_SCHEME table

                if (DbConnectionFactory.isOracle()) {
                    dc.executeStatement("insert into workflow_scheme (id, name, description, archived, mandatory, default_scheme, entry_action_id, mod_date) values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_DESC', '"
                            + DbConnectionFactory.getDBFalse()
                            + "', '"
                            + DbConnectionFactory.getDBFalse()
                            + "', '"
                            + DbConnectionFactory.getDBFalse()
                            + "', '', to_date('1900-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'))");
                } else if (DbConnectionFactory.isPostgres()) {
                    dc.executeStatement("insert into workflow_scheme (id, name, description, archived, mandatory, default_scheme, entry_action_id, mod_date) values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_DESC', "
                            + DbConnectionFactory.getDBFalse()
                            + ", "
                            + DbConnectionFactory.getDBFalse()
                            + ", "
                            + DbConnectionFactory.getDBFalse() + ", '', '1900-01-01 00:00:00.00')");
                } else {
                    dc.executeStatement("insert into workflow_scheme (id, name, description, archived, mandatory, default_scheme, entry_action_id, mod_date) values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_DESC', '"
                            + DbConnectionFactory.getDBFalse()
                            + "', '"
                            + DbConnectionFactory.getDBFalse()
                            + "', '"
                            + DbConnectionFactory.getDBFalse() + "', '', '1900-01-01 00:00:00.00')");
                }

                // 2) Update references to the new dummies temps

                // update foreign tables references to TEMP
                dc.executeStatement("update workflow_step set scheme_id = 'TEMP_INODE' where scheme_id = '"
                        + oldWorkflowId + "'");
                dc.executeStatement("update workflow_scheme_x_structure set scheme_id = 'TEMP_INODE' where scheme_id = '"
                        + oldWorkflowId + "'");

                // 3) delete old WORKFLOW_SCHEME row
                // lets save old scheme columns values first
                dc.setSQL("select * from workflow_scheme where id = ?");
                dc.addParam(oldWorkflowId);
                Map<String, Object> oldFolderRow = dc.loadObjectResults().get(0);
                String name = (String) oldFolderRow.get("name");
                String desc = (String) oldFolderRow.get("description");
                Boolean archived = DbConnectionFactory.isDBTrue(oldFolderRow.get("archived")
                        .toString());
                Boolean mandatory = DbConnectionFactory.isDBTrue(oldFolderRow.get("mandatory")
                        .toString());
                Boolean isDefaultScheme = DbConnectionFactory.isDBTrue(oldFolderRow.get(
                        "default_scheme").toString());
                String entryActionId = (String) oldFolderRow.get("entry_action_id");
                Date modDate = (Date) oldFolderRow.get("mod_date");

                dc.executeStatement("delete from workflow_scheme where id = '" + oldWorkflowId
                        + "'");

                // 4) insert real new WORKFLOW_SCHEME row
                dc.setSQL("insert into workflow_scheme (id, name, description, archived, mandatory, default_scheme, entry_action_id, mod_date) values (?, ?, ?, ?, ?, ?, ?, ?) ");
                dc.addParam(newWorkflowId);
                dc.addParam(name);
                dc.addParam(desc);
                dc.addParam(archived);
                dc.addParam(mandatory);
                dc.addParam(isDefaultScheme);
                dc.addParam(entryActionId);
                dc.addParam(modDate);
                dc.loadResult();

                // 5) update foreign tables references to the new real row
                dc.executeStatement("update workflow_step set scheme_id = '" + newWorkflowId
                        + "' where scheme_id = 'TEMP_INODE'");
                dc.executeStatement("update workflow_scheme_x_structure set scheme_id = '"
                        + newWorkflowId + "' where scheme_id = 'TEMP_INODE'");

                // 6) delete dummy temp
                dc.executeStatement("delete from workflow_scheme where id = 'TEMP_INODE'");
            }
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }
}
