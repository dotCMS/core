package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;

/**
 * Task to create the variant_id column in the contentlet table, Also it removes the variantId attribute
 * from the contentlet_as_json column and sets the variant_id column to the default value 'DEFAULT'
 */
public class Task230523CreateVariantFieldInContentlet implements StartupTask {

    @Override
    public boolean forceRun() {
        return !hasVariantIdColumn();
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        if (forceRun()) {
            final DotConnect dotConnect = new DotConnect();
            try {
                Logger.info(this, "Adding the 'variant_id' column to the 'contentlet' table");
                dotConnect.executeStatement(createVariantId());
                Logger.info(this, "Removing the 'variantId' property from the 'contentlet_as_json' value");
                dotConnect.executeStatement("UPDATE contentlet SET contentlet_as_json = contentlet_as_json - 'variantId' WHERE contentlet_as_json IS NOT NULL");
            } catch (SQLException e) {
                throw new DotRuntimeException(e);
            }
        }
    }

    public String createVariantId() throws  DotRuntimeException {
        final String dataBaseFieldType = DbConnectionFactory.isMsSql() ? "NVARCHAR" : "varchar";

        return String.format(
                "ALTER TABLE contentlet ADD variant_id %s(255) NOT NULL default 'DEFAULT'",
                dataBaseFieldType);
    }

    private boolean hasVariantIdColumn() {
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        try {
            return databaseMetaData.hasColumn("contentlet", "variant_id");
        } catch (SQLException e) {
            return false;
        }
    }

}
