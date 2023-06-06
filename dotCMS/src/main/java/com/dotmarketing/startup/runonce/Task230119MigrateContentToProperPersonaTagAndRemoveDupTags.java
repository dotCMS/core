package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import io.vavr.control.Try;

/**
 * Persona tags can have duplicates, being the correct tag something like `hipster` and the incorrect duplicate
 * tag something like `hipster:persona`.
 * <p>
 * This task takes care of:
 * <ul>
 *     <li>Inserting new relationships between the correct tag and the content related to the wrong duplicate tag
 *     <li>Deleting bad relationships between content and the wrong duplicate tag
 *     <li>Deleting the duplicate tags
 * </ul>
 */
public class Task230119MigrateContentToProperPersonaTagAndRemoveDupTags extends AbstractJDBCStartupTask  {

    final String DELETE_BAD_RELS_POSTGRES = "DELETE FROM tag_inode ti USING tag t WHERE t.tag_id = ti.tag_id\n"
            + "AND t.tagname LIKE '%:persona';";
    final String DELETE_BAD_RELS_MSSQL = "DELETE ti FROM tag_inode ti JOIN tag ON ti.tag_id = tag.tag_id\n"
            + "WHERE tag.tagname LIKE '%:persona';";
    final String SQL = "INSERT INTO tag_inode\n"
            + "SELECT tag.tag_id, aux.inode, aux.field_var_name, aux.mod_date\n"
            + "FROM (SELECT tag_inode.*, tag.tagname\n"
            + "      FROM tag_inode\n"
            + "               JOIN tag ON tag_inode.tag_id = tag.tag_id\n"
            + "      WHERE tagname LIKE '%:persona') aux,\n"
            + "     tag\n"
            + "WHERE tag.tagname = REPLACE(aux.tagname, ':persona', '');\n"
            + "\n"
            + (DbConnectionFactory.isPostgres() ? DELETE_BAD_RELS_POSTGRES : DELETE_BAD_RELS_MSSQL)
            + "\n"
            + "DELETE FROM tag WHERE tag.tagname LIKE '%:persona';";




    @Override
    public boolean forceRun() {
        return Try.of(()->!new DotConnect().setSQL("SELECT * FROM tag WHERE tagname LIKE '%:persona'")
                .loadResults().isEmpty()).getOrElse(false);
    }

    @Override
    public String getMSSQLScript() {
        return SQL;
    }

    @Override
    public String getPostgresScript() {
        return SQL;
    }

}