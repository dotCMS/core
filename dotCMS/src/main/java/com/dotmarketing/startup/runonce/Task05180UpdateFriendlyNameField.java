package com.dotmarketing.startup.runonce;

import static com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.FRIENDLY_NAME_FIELD;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

/**
 * @author nollymar
 */
public class Task05180UpdateFriendlyNameField implements StartupTask {

    private final String UPDATE_FIELD = "UPDATE field set velocity_var_name='" + FRIENDLY_NAME_FIELD
            + "' where velocity_var_name='friendlyname'";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            DotConnect dc = new DotConnect();
            dc.setSQL(UPDATE_FIELD);
            dc.loadResult();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
    }
}
