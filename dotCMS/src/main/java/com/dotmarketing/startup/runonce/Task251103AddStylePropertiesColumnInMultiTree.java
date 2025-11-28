package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;

/**
 * Adds the {@code style_properties} column to the {@code multi_tree} table. This JSONB/JSON column
 * stores style configuration properties for content placement within containers on pages, allowing
 * for flexible styling of individual content instances.
 *
 * @author Dario Daza
 * @since Nov 3rd, 2025
 */
public class Task251103AddStylePropertiesColumnInMultiTree implements StartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        addStyleProperties();
    }

    /**
     * Adds the style_properties column to the multi_tree table.
     */
    private void addStyleProperties() {
        try {
            new DotConnect().executeStatement(
                    "ALTER TABLE multi_tree ADD COLUMN IF NOT EXISTS style_properties JSONB");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(), e);
        }
    }
}
