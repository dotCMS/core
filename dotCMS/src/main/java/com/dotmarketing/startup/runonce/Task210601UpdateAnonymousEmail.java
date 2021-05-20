package com.dotmarketing.startup.runonce;

import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

import java.util.List;
import java.util.Map;

/**
 * Updates the anonymous email to avoid the "anonymous@dotcmsfakeemail.org"
 * @author jsanca
 */
public class Task210601UpdateAnonymousEmail implements StartupTask {

    protected static final String OLD_ANONYMOUS_EMAIL = "anonymous@dotcmsfakeemail.org";

    @Override
    public boolean forceRun() {

        try {
            final List<Map<String, Object>> results =
                    new DotConnect()
                    .setSQL("select * from user_ where emailaddress = ?")
                    .addParam(OLD_ANONYMOUS_EMAIL)
                    .loadObjectResults();

            return null != results && results.size() > 0; // any result?? update the email
        } catch(Exception ex) {
            return true;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        new DotConnect().executeUpdate("UPDATE user_ SET emailaddress = ? where emailaddress = ?",
                UserAPI.CMS_ANON_USER_EMAIL, OLD_ANONYMOUS_EMAIL);
    }
}
