package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.sql.SQLException;

/**
 * Adds the nullable {@code default_base_type} column to the {@code folder} table. This column
 * records the folder's Content Drive upload-mode preference (a {@code BaseContentType} name such
 * as {@code DOTASSET}/{@code FILEASSET}, or {@code null} for "no preference"). Additive and
 * backward compatible: existing folders read back {@code null}. PostgreSQL-only (the sole supported
 * database). Idempotent via {@link #forceRun()}.
 *
 * @since Jul 20th, 2026
 */
public class Task260720AddDefaultBaseTypeToFolderTable implements StartupTask {

    private static final String ADD_COLUMN_SQL =
            "ALTER TABLE folder ADD COLUMN default_base_type varchar(36) null";

    @Override
    public boolean forceRun() {
        try {
            return !new DotDatabaseMetaData().hasColumn("folder", "default_base_type");
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final DotConnect dc = new DotConnect();
        try {
            dc.executeStatement(ADD_COLUMN_SQL);
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }
}
