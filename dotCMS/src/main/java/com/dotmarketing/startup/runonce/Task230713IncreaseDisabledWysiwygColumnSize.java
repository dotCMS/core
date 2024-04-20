package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.util.Map;

/**
 * This Upgrade Task increases the size of the {@code contentlet.disabled_wysiwyg} column to 1000.
 * <p>This column stores the velocity variable names -- and their respective separation characters -- of the WYSIWYG and
 * Text Area fields whose view mode has been changed. For example:
 * <ul>
 *     <li>View Modes for WYSIWYG: {@code WYSIWYG}, {@code CODE}, and {@code PLAIN}.</li>
 *     <li>View Modes for Text Area: {@code Toggle Editor} and "normal" editor.</li>
 * </ul>
 * In case customers have a Content Type with several of those fields, dotCMS will hit a database problem due to the
 * column not being able to hold a String longer than the original 255 limit. This upgrade Task fixes such a situation.
 * </p>
 *
 * @author Jose Castro
 * @since Jul 13th, 2023
 */
public class Task230713IncreaseDisabledWysiwygColumnSize implements StartupTask {

    @Override
    public boolean forceRun() {
        return !(new DotDatabaseMetaData().isColumnLengthExpected("contentlet",
                "disabled_wysiwyg", "1000"));
    }

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        new DotConnect().setSQL("ALTER TABLE contentlet ALTER COLUMN disabled_wysiwyg TYPE VARCHAR(1000)").loadResult();
    }

}
