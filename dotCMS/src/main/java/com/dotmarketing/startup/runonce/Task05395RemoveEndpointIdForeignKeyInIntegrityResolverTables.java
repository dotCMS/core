package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

import java.util.Map;

/**
 * Remove the foreign key with the publishing_end_point table to all the Integrity Resolver tables and rename the column
 * endpoint_id to remote IP, Also change the endpoint_id column size.
 *
 * The Integrity Resolver tables are:
 *
 * - cms_roles_ir
 * - folder_ir
 * - structures_ir
 * - htmlpages_ir
 * - fileassets_ir
 */
public class Task05395RemoveEndpointIdForeignKeyInIntegrityResolverTables implements StartupTask {
    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        final Map<String, String> tables = Map.of(
                "folders_ir", "fk_folder_ir_ep",
                "structures_ir", "fk_structure_ir_ep",
                "htmlpages_ir", "fk_page_ir_ep",
                "fileassets_ir", "fk_file_ir_ep",
                "cms_roles_ir", "fk_cms_roles_ir_ep"
        );

        for (Map.Entry<String, String> entry : tables.entrySet()) {
            try {
                dropConstraint(entry.getKey(), entry.getValue());
                alterColumn(entry.getKey());
            }catch (DotDataException e) {
                continue;
            }
        }

    }

    private void dropConstraint(final String tableName, final String constraintName) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL(String.format("ALTER TABLE %s DROP CONSTRAINT %s", tableName, constraintName));
        dc.loadResult();
    }

    private void alterColumn(final String tableName) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL(String.format("ALTER TABLE %s ALTER COLUMN endpoint_id TYPE VARCHAR (40)", tableName));
        dc.loadResult();
    }


}
