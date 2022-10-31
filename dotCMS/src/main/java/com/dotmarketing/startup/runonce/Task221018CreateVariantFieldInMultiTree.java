package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;

import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.liferay.util.StringPool;
import java.sql.SQLException;
import java.util.Optional;

public class Task221018CreateVariantFieldInMultiTree extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return !hasAddRemoveField();
    }

    private boolean hasAddRemoveField() {
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        try {
            return databaseMetaData.hasColumn("multi_tree", "variant_id");
        } catch (SQLException e) {
            return false;
        }
    }

    public String getPostgresScript() {
        return hasAddRemoveField() ? StringPool.BLANK : getCreateFieldStatement() + ";" + addVariantIdIntoPrimaryKey() + ";";
    }


    public String getMSSQLScript(){
        return hasAddRemoveField() ? StringPool.BLANK : getCreateFieldStatement()  + ";" + addVariantIdIntoPrimaryKey() + ";";
    }


    private String getCreateFieldStatement() {
        final String dataBaseFieldType = DbConnectionFactory.isMsSql() ? "NVARCHAR" : "varchar";

        return String.format(
                "ALTER TABLE multi_tree ADD variant_id %s(255) NOT NULL",
                dataBaseFieldType);
    }

    private String addVariantIdIntoPrimaryKey() {
        final Optional<String> primaryKeyName = DotDatabaseMetaData.getPrimaryKeyName("multi_tree");
        final String dropStatement = String.format("ALTER TABLE multi_tree DROP CONSTRAINT %s", primaryKeyName.get());

        final String createStatement = String.format("ALTER TABLE multi_tree "
                + " ADD CONSTRAINT %s PRIMARY KEY (child, parent1, parent2, relation_type, personalization, variant_id)", primaryKeyName.get());
        return dropStatement + ";" + createStatement + ";";
    }
}
