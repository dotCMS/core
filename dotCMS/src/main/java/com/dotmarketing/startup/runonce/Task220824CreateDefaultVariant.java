package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.ConversionUtils;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Create a new variant_id field in the contentlet_version_info table and create the DEFAULT variant
 */
public class Task220824CreateDefaultVariant implements StartupTask  {

    @Override
    @WrapInTransaction
    public boolean forceRun() {
        try{
            final ArrayList results = new DotConnect()
                    .setSQL("SELECT * FROM variant WHERE id = ?")
                    .addParam(VariantAPI.DEFAULT_VARIANT.identifier())
                    .loadResults();
            return results.isEmpty();
        } catch (DotDataException e) {
            return Boolean.FALSE;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final DotConnect dotConnect = new DotConnect();

        createDefaultVariant(dotConnect);
        createVariantDataBaseField(dotConnect);

    }

    private void createDefaultVariant(DotConnect dotConnect) throws DotDataException {
        if (!defaultVariantExists(dotConnect)) {
            dotConnect.setSQL("INSERT INTO variant (id, name, archived) VALUES (?, ?, ?)")
                    .addParam(VariantAPI.DEFAULT_VARIANT.identifier())
                    .addParam(VariantAPI.DEFAULT_VARIANT.name())
                    .addParam(ConversionUtils.toBooleanFromDb(false))
                    .loadResult();
        }
    }

    private boolean defaultVariantExists(DotConnect dotConnect) throws DotDataException {
        return !dotConnect.setSQL("SELECT * FROM variant WHERE id = ?")
                .addParam(VariantAPI.DEFAULT_VARIANT.identifier())
                .loadResults()
                .isEmpty();
    }

    private void createVariantDataBaseField(DotConnect dotConnect) throws DotDataException {
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        try {
            if (!databaseMetaData.hasColumn("contentlet_version_info", "variant_id")) {
                final String dataBaseFieldType = DbConnectionFactory.isMsSql() ? "NVARCHAR" : "varchar";

                final String alterTableQuery = String.format(
                        "ALTER TABLE contentlet_version_info ADD variant_id %s(255) default 'DEFAULT'",
                        dataBaseFieldType);

                dotConnect
                        .setSQL(alterTableQuery)
                        .loadResult();

                dotConnect.setSQL("UPDATE contentlet_version_info SET variant_id = 'DEFAULT'")
                        .loadResult();
            }
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }
}
