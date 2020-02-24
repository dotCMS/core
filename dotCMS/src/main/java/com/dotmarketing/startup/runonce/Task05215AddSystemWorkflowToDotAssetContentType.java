package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;

/**
 * This upgrade task associated the system workflow to DotAsset Content Type.
 *
 * @author jsanca
 * @version 5.0
 */
public class Task05215AddSystemWorkflowToDotAssetContentType implements StartupTask {

    public static final String SYSTEM_WORKFLOW_ID = WorkflowAPI.SYSTEM_WORKFLOW_ID;
    protected static String INSERT_SCHEME_FOR_CONTENT_TYPE = "insert into workflow_scheme_x_structure (id, scheme_id, structure_id) values ( ?, ?, ?)";

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    @CloseDBIfOpened
    public void executeUpgrade() throws DotDataException {

        Logger.info(this, "Adding the system workflow to the DotAsset content type ");

        new DotConnect().setSQL(INSERT_SCHEME_FOR_CONTENT_TYPE)
                .addParam(UUIDGenerator.generateUuid())
                .addParam(SYSTEM_WORKFLOW_ID)
                .addParam(Task05210CreateDefaultDotAsset.DOTASSET_VARIABLE_INODE)
                .loadResult();
    } // executeUpgrade.

}

