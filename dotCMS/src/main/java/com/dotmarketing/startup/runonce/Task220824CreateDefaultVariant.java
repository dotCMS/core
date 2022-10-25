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
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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
                    .setSQL("SELECT * FROM variant WHERE name = ?")
                    .addParam(VariantAPI.DEFAULT_VARIANT.name())
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
    }

    private void createDefaultVariant(DotConnect dotConnect) throws DotDataException {
        if (!defaultVariantExists(dotConnect)) {
            dotConnect.setSQL("INSERT INTO variant (name, description, archived) VALUES (?, ?, ?)")
                    .addParam(VariantAPI.DEFAULT_VARIANT.name())
                    .addParam(VariantAPI.DEFAULT_VARIANT.name())
                    .addParam(ConversionUtils.toBooleanFromDb(false))
                    .loadResult();
        }
    }

    private boolean defaultVariantExists(DotConnect dotConnect) throws DotDataException {
        return !dotConnect.setSQL("SELECT * FROM variant WHERE name = ?")
                .addParam(VariantAPI.DEFAULT_VARIANT.name())
                .loadResults()
                .isEmpty();
    }
}
