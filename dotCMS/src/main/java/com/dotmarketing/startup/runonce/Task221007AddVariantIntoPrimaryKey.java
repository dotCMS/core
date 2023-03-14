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
import java.util.Optional;

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
        return getScript();
    }


    public String getMSSQLScript(){
        return getScript();
    }

    public String getScript() {
        final StringBuffer buffer = new StringBuffer();

        final Optional<String> contentletVersionInfoOptional = DotDatabaseMetaData.getPrimaryKeyName(
                "contentlet_version_info");

        if (contentletVersionInfoOptional.isPresent()) {
            buffer.append(String.format("ALTER TABLE contentlet_version_info DROP CONSTRAINT %s;", contentletVersionInfoOptional.get()));
        }

        final String updatePrimaryKeyStatement = String.format(
                "ALTER TABLE contentlet_version_info ADD CONSTRAINT %s PRIMARY KEY (identifier, lang, variant_id);",
                contentletVersionInfoOptional.orElse("contentlet_version_info_pkey")
        );

        buffer.append(updatePrimaryKeyStatement);
        return buffer.toString();
    }
}
