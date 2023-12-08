package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

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
public class Task231207AddInodeAndIdentifierLeftIndexes implements StartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        new DotConnect().setSQL("CREATE INDEX if not exists inode_inode_leading_idx ON inode(inode text_pattern_ops);").loadResult();

        new DotConnect().setSQL("CREATE INDEX if not exists identifier_id_leading_idx ON identifier(id text_pattern_ops);").loadResult();

    }

}
