package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;

import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class Task221018CreateVariantFieldInMultiTree extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return !hasAddRemoveField() || !primaryKeyContainsVariantId();
    }

    private boolean hasAddRemoveField() {
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        try {
            return databaseMetaData.hasColumn("multi_tree", "variant_id");
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean primaryKeyContainsVariantId() {
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
        final List<String> primaryKeysFields = databaseMetaData.getPrimaryKeysFields("multi_tree");

        return primaryKeysFields.contains("variant_id");
    }

    public String getPostgresScript() {
        return !forceRun() ? StringPool.BLANK : getStatements();
    }

    @NotNull
    private String getStatements() {
        final String createFieldStatement = getCreateFieldStatement();
        final String addVariantIdIntoPrimaryKey = addVariantIdIntoPrimaryKey();
        final String separator = UtilMethods.isSet(createFieldStatement) && UtilMethods.isSet(addVariantIdIntoPrimaryKey)
                ? ";" : StringPool.BLANK;

        return createFieldStatement + separator + addVariantIdIntoPrimaryKey + separator;
    }


    public String getMSSQLScript(){
        return !forceRun() ? StringPool.BLANK : getStatements();
    }


    private String getCreateFieldStatement() {
        if (!hasAddRemoveField()) {
            final String dataBaseFieldType = DbConnectionFactory.isMsSql() ? "NVARCHAR" : "varchar";

            return String.format(
                    "ALTER TABLE multi_tree ADD variant_id %s(255) NOT NULL default 'DEFAULT'",
                    dataBaseFieldType);
        } else {
            return StringPool.BLANK;
        }
    }

    private String addVariantIdIntoPrimaryKey() {
        if (!primaryKeyContainsVariantId()) {
            final Optional<String> primaryKeyName = DotDatabaseMetaData.getPrimaryKeyName(
                    "multi_tree");
            final String dropStatement = String.format("ALTER TABLE multi_tree DROP CONSTRAINT %s",
                    primaryKeyName.get());

            final String createStatement = String.format("ALTER TABLE multi_tree "
                            + " ADD CONSTRAINT %s PRIMARY KEY (child, parent1, parent2, relation_type, personalization, variant_id)",
                    primaryKeyName.get());
            return dropStatement + ";" + createStatement + ";";
        } else {
            return StringPool.BLANK;
        }
    }
}
