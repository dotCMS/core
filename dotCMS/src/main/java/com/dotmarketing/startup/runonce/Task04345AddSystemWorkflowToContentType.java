package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This upgrade task associated the system workflow to all contenttypes.
 *
 * @author jsanca
 * @version 5.0
 *
 */
public class Task04345AddSystemWorkflowToContentType implements StartupTask {

    public static final String SYSTEM_WORKFLOW_ID          = WorkflowAPI.SYSTEM_WORKFLOW_ID;
    protected static String SELECT_CONTENT_TYPES           = "select inode, name from structure";
    protected static String SELECT_CONTENT_TYPES_WITH_MANDATORY =
            "select structure.inode as inode from structure, workflow_scheme_x_structure, workflow_scheme " +
            "where structure.inode = workflow_scheme_x_structure.structure_id and " +
            "      workflow_scheme_x_structure.scheme_id = workflow_scheme.id and " +
            "      workflow_scheme.mandatory = ?";
    protected static String INSERT_SCHEME_FOR_CONTENT_TYPE = "insert into workflow_scheme_x_structure (id, scheme_id, structure_id) values ( ?, ?, ?)";

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    @CloseDBIfOpened
    public void executeUpgrade() throws DotDataException {

        Logger.info(this, "Running the upgrade for the system workflow to content type");

        final List<Map<String, Object>> contentResults =
                (List<Map<String, Object>>)new DotConnect()
                        .setSQL(SELECT_CONTENT_TYPES)
                        .loadResults();

        final Set<String> contentResultsWithMandatory =
                        new DotConnect()
                        .setSQL(SELECT_CONTENT_TYPES_WITH_MANDATORY)
                        .addParam(Boolean.TRUE)
                        .loadObjectResults().stream()
                        .map(stringObjectMap -> (String)stringObjectMap.get("inode"))
                        .collect(Collectors.toSet());

        for (final Map<String, Object> contentResult : contentResults) {

            final String contentTypeId = (String)contentResult.get("inode");

            if (!contentResultsWithMandatory.contains(contentTypeId)) {

                Logger.info(this, "Adding the system workflow to the content type "
                        + contentTypeId + ", name " + contentResult.get("name"));

                new DotConnect().setSQL(INSERT_SCHEME_FOR_CONTENT_TYPE)
                        .addParam(UUIDGenerator.generateUuid())
                        .addParam(SYSTEM_WORKFLOW_ID)
                        .addParam(contentTypeId)
                        .loadResult();
            }
        }


    } // executeUpgrade.

}

