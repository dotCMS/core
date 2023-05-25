package com.dotmarketing.startup.runonce;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.startup.StartupTask;
import com.liferay.util.StringPool;
import java.sql.SQLException;
import java.util.stream.Collectors;

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
                dotConnect.executeStatement(getStatements());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getStatements() {
        return list(createVariantId(), removeVariantFromJsonField(), updateVariantToDefault()).stream()
                .collect(Collectors.joining(";\n"));
    }

    private String updateVariantToDefault() {
        return "UPDATE contentlet SET variant_id = 'DEFAULT'";
    }

    private String removeVariantFromJsonField() {
        return "UPDATE contentlet SET contentlet_as_json = contentlet_as_json - 'variantId'";
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
