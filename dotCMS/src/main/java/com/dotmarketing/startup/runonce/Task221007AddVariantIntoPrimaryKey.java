package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Task221007AddVariantIntoPrimaryKey extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        final List<String> primaryKeysFields = DotDatabaseMetaData.getPrimaryKeysFields("contentlet_version_info");
        return primaryKeysFields.size() != 3 ||
                (!primaryKeysFields.contains("identifier") ||
                !primaryKeysFields.contains("variant_id") ||
                !primaryKeysFields.contains("lang"));
    }

    public String getPostgresScript() {
        return getScript("contentlet_version_info_pkey");
    }


    public String getMSSQLScript(){

        try {
            final ArrayList arrayList = new DotConnect()
                    .setSQL("SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS T "
                            + "WHERE table_name = 'contentlet_version_info' "
                            + "AND constraint_type = 'PRIMARY KEY'")
                    .loadResults();

            final String constraintName = (((Map) arrayList.get(0))).get("constraint_name").toString();
            return getScript(constraintName);
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    public String getScript(final String name) {
        final String dropStatement = String.format("ALTER TABLE contentlet_version_info DROP CONSTRAINT %s", name);

        final String createStatement = String.format("ALTER TABLE contentlet_version_info "
                + " ADD CONSTRAINT %s PRIMARY KEY (identifier, lang, variant_id)", name);
        return dropStatement + ";" + createStatement + ";";
    }
}
