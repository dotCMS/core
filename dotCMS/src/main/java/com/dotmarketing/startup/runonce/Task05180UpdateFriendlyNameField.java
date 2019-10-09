package com.dotmarketing.startup.runonce;

import static com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.FRIENDLY_NAME_FIELD;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;

/**
 * @author nollymar
 */
public class Task05180UpdateFriendlyNameField implements StartupTask {

    private final String UPDATE_FIELD = "update field set velocity_var_name='" + FRIENDLY_NAME_FIELD
            + "' where velocity_var_name='friendlyname' and structure_inode in (select inode from structure where structuretype="
            + BaseContentType.HTMLPAGE.getType() + ")";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL(UPDATE_FIELD);
            dotConnect.loadResult();
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(), e);
        }
    }
}
