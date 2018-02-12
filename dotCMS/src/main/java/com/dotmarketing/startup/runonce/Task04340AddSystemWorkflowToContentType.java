

package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;

import java.util.List;
import java.util.Map;

/**
 * This upgrade task associated the system workflow to all contenttypes.
 *
 * @author jsanca
 * @version 5.0
 *
 */
public class Task04340AddSystemWorkflowToContentType implements StartupTask {

    public static final String SYSTEM_WORKFLOW_ID          = "d61a59e1-a49c-46f2-a929-db2b4bfa88b2";
    protected static String SELECT_CONTENT_TYPES           = "select inode, name from structure";
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

        for (final Map<String, Object> contentResult : contentResults) {

            final String contentTypeId = (String)contentResult.get("inode");

            Logger.info(this, "Adding the system workflow to the content type "
                    + contentTypeId + ", name " + contentResult.get("name"));

            new DotConnect().setSQL(INSERT_SCHEME_FOR_CONTENT_TYPE)
                    .addParam(UUIDGenerator.generateUuid())
                    .addParam(contentTypeId)
                    .addParam(SYSTEM_WORKFLOW_ID)
                    .loadResult();
        }

    } // executeUpgrade.

}
