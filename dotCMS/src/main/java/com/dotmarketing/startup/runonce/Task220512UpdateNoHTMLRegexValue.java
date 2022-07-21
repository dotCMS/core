package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.model.field.TextField;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.util.List;
import java.util.Map;

/**
 * Updates regex for NO HTML fields
 * @author nollymar
 */
public class Task220512UpdateNoHTMLRegexValue implements StartupTask {

    private static final String NEW_NOHTML_REGEX = "^[^<><|>]+$";
    static final String OLD_NOHTML_REGEX = "[^(<[.\n"
            + "]+>)]*";

    @Override
    public boolean forceRun() {

        try {
            final List<Map<String, Object>> results =
                    new DotConnect()
                    .setSQL("select * from field where regex_check= ? and field_type=?")
                            .addParam(OLD_NOHTML_REGEX)
                            .addParam(TextField.class.getCanonicalName())
                    .loadObjectResults();

            return null != results && results.size() > 0; // any result?? update the email
        } catch(Exception ex) {
            return true;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        new DotConnect().executeUpdate("update field set regex_check= ? where regex_check= ? and field_type=?",
                NEW_NOHTML_REGEX, OLD_NOHTML_REGEX, TextField.class.getCanonicalName());
    }
}
