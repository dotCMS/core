package com.dotmarketing.startup.runonce;

import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * This UT is to update the column system for the Language Variable Content Type. This value needs to be false
 * since system content types are being hidden from the Content Search Portlet.
 */
public class Task240131UpdateLanguageVariableContentType implements StartupTask {
    @Override
    public boolean forceRun() {
        final String isSystem = new DotConnect().setSQL(
                        "select system from structure where velocity_var_name like ?")
                .addParam(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME).getString("system");

        return DbConnectionFactory.isDBTrue(isSystem);
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final DotConnect dc = new DotConnect();
        dc.setSQL("update structure set system = false where velocity_var_name = ?");
        dc.addParam(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);
        dc.loadResult();
    }
}
