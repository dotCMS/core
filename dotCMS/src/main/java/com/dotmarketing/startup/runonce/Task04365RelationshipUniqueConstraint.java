package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.util.Collections;
import java.util.List;

public class Task04365RelationshipUniqueConstraint extends AbstractJDBCStartupTask {


    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return Collections.emptyList();
    }

    @Override
    public String getH2Script() {
        StringBuilder builder = new StringBuilder()
            //Delete duplicates
            .append("DELETE R1 ")
            .append("FROM relationship AS R1, relationship AS R2 ")
            .append("WHERE R1.inode < R2.inode ")
            .append("AND R1.relation_type_value = R2.relation_type_value; ")

            //Create Unique Key
            .append("ALTER TABLE relationship ADD CONSTRAINT unique_relation_type_value UNIQUE (relation_type_value); ");
        return builder.toString();
    }

    @Override
    public String getPostgresScript() {
        StringBuilder builder = new StringBuilder()
            //Delete duplicates
            .append("DELETE FROM relationship R1 ")
            .append("USING relationship R2 ")
            .append("WHERE R1.inode < R2.inode ")
            .append("AND R1.relation_type_value = R2.relation_type_value; ")

            //Create Unique Key
            .append("ALTER TABLE relationship ADD CONSTRAINT unique_relation_type_value UNIQUE (relation_type_value); ");
        return builder.toString();
    }

    @Override
    public String getMySQLScript() {
        StringBuilder builder = new StringBuilder()
            //Delete duplicates
            .append("DELETE R1 ")
            .append("FROM relationship AS R1, relationship AS R2 ")
            .append("WHERE R1.inode < R2.inode ")
            .append("AND R1.relation_type_value = R2.relation_type_value; ")

            //Create Unique Key
            .append("ALTER TABLE relationship ADD CONSTRAINT unique_relation_type_value UNIQUE (relation_type_value); ");
        return builder.toString();
    }

    @Override
    public String getMSSQLScript() {
        StringBuilder builder = new StringBuilder()
            //Delete duplicates
            .append("DELETE R1 FROM ( ")
            .append("  Select *, ROW_NUMBER() over (Partition by relation_type_value order by relation_type_value) as rowNumber ")
            .append("  From relationship ) R1  ")
            .append("WHERE R1.rowNumber > 1; ")

            //Create Unique Key
            .append("ALTER TABLE relationship ADD CONSTRAINT unique_relation_type_value UNIQUE (relation_type_value); ");
        return builder.toString();
    }

    @Override
    public String getOracleScript() {
        this.runInSingleTransaction = false;
        StringBuilder builder = new StringBuilder()
            //Delete duplicates
            .append("DELETE FROM relationship R1 ")
            .append("WHERE R1.ROWID > ANY ( ")
            .append("  SELECT R2.ROWID ")
            .append("  FROM relationship R2 ")
            .append("  WHERE R1.relation_type_value = R2.relation_type_value ); ")

            //Create Unique Key
            .append("ALTER TABLE relationship ADD CONSTRAINT unique_relation_type_value UNIQUE (relation_type_value); ");
        return builder.toString();
    }
}
