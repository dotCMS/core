package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

/**
 * Migrates OS index names in the {@code indicies} table from the legacy {@code os::} prefix format
 * to the new {@code .os} suffix format.
 *
 * <p>Before: {@code os::cluster_e0f4fa027f.working_20260527221825}</p>
 * <p>After:  {@code cluster_e0f4fa027f.working_20260527221825.os}</p>
 *
 * <p>The {@code os::} prefix was replaced with a {@code .os} suffix to avoid collision issues and
 * improve readability. The suffix approach is collision-free because logical index names always end
 * in a numeric timestamp ({@code _YYYYMMDDHHMMSS}) — they can never naturally end in {@code .os}.
 * The DB storage form is stripped before being returned to any caller; only {@code VersionedIndicesAPIImpl}
 * manages this marker.</p>
 *
 * @since May 28th, 2026
 */
public class Task260528MigrateOsIndiciesSuffix implements StartupTask {

    @Override
    public boolean forceRun() {
        try {
            final int count = new DotConnect()
                    .setSQL("SELECT COUNT(*) AS c FROM indicies WHERE index_name LIKE 'os::%'")
                    .getInt("c");
            return count > 0;
        } catch (final Exception e) {
            Logger.error(this, "Could not check for legacy os:: indicies rows: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Migrating os:: prefixed index names to .os suffix in indicies table");

        new DotConnect()
                .setSQL("UPDATE indicies SET index_name = SUBSTRING(index_name, 5) || '.os' WHERE index_name LIKE 'os::%'")
                .loadResult();

        Logger.info(this, "Migrated OS index names from os:: prefix to .os suffix in indicies table");
    }

}
