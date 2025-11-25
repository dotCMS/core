package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;


public class Task05395RemoveEndpointIdForeignKeyInIntegrityResolverTablesIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to Test: {@link Task05395RemoveEndpointIdForeignKeyInIntegrityResolverTables#executeUpgrade()}
     * When: Run the Upgrade Task
     * Should: Should remove the foreign key to all the Integrity Resolver tables and rename the column endpoint_id
     * The Integrity Resolver tables are:
     * - cms_roles_ir
     * - folders_ir
     * - structures_ir
     * - htmlpages_ir
     * - fileassets_ir
     *
     * After remove the constraint you should be allow to insert register in this table without has any register in publishing_end_point
     */
    @Test
    public void constraintShouldNotExists() throws DotDataException, SQLException {

        final String endpointId = insertPublishingEndPoint();
        insertFolderIntegrityResolver(endpointId);
        insertPageIntegrityResolver(endpointId);
        insertFileAssetIntegrityResolver(endpointId);
        insertRolesIntegrityResolver(endpointId);
        insertStructuresIntegrityResolver(endpointId);

        final Task05395RemoveEndpointIdForeignKeyInIntegrityResolverTables task05390RemoveEndpointIdForeignKeyInIntegrityResolverTables =
                new Task05395RemoveEndpointIdForeignKeyInIntegrityResolverTables();

        task05390RemoveEndpointIdForeignKeyInIntegrityResolverTables.executeUpgrade();

        insertFolderIntegrityResolver("anyIP");
        insertPageIntegrityResolver("anyIP");
        insertFileAssetIntegrityResolver("anyIP");
        insertRolesIntegrityResolver("anyIP");
        insertStructuresIntegrityResolver("anyIP");

        checkColumnsSize();
    }

    private void checkColumnsSize() throws SQLException {
        final List<String> tables = list(
                "folders_ir",
                "structures_ir",
                "htmlpages_ir",
                "fileassets_ir",
                "cms_roles_ir"
        );

        for (String table : tables) {
            checkColumnSize(table);
        }
    }

    private void checkColumnSize(final String tableName) throws SQLException {
        final Connection connection = DbConnectionFactory.getConnection();
        final ResultSet resultSet = DotDatabaseMetaData.getColumnsMetaData(connection, tableName);

        while (resultSet.next()) {

            final String columnName = resultSet.getString("COLUMN_NAME");

            if (columnName.equals("endpoint_Id")) {
                final int columnSize = resultSet.getInt("COLUMN_SIZE");
                assertEquals(columnSize, 40);
            }

        }
    }

    private String insertPublishingEndPoint() throws DotDataException {

        DotConnect dc = new DotConnect();
        dc.setSQL("INSERT INTO publishing_end_point " +
                "(id, group_id, server_name, address, port, protocol, enabled, auth_key, sending) " +
                "values(?,?,?,?,?,?,?,?,?)"
        );

        final String id = String.valueOf(System.currentTimeMillis());

        dc.addParam(id);
        dc.addParam("group_id");
        dc.addParam("server_name");
        dc.addParam("address");
        dc.addParam("port");
        dc.addParam("protocol");
        dc.addParam(true);
        dc.addParam("auth_key");
        dc.addParam(true);

        dc.loadResult();

        return id;
    }

    private void insertFolderIntegrityResolver(final String remoteIP) throws DotDataException {

        DotConnect dc = new DotConnect();
        dc.setSQL(
                "INSERT INTO folders_ir (local_inode, remote_inode, local_identifier, remote_identifier, endpoint_id) values(?,?,?,?,?)"
        );

        dc.addParam("localInode" + System.currentTimeMillis());
        dc.addParam("remoteInode" + System.currentTimeMillis());
        dc.addParam("localIdentifier" + System.currentTimeMillis());
        dc.addParam("remoteIdentifier" + System.currentTimeMillis());
        dc.addParam(remoteIP);

        dc.loadResult();
    }

    private void insertPageIntegrityResolver(final String remoteIP) throws DotDataException {

        DotConnect dc = new DotConnect();
        dc.setSQL("INSERT INTO htmlpages_ir " +
                "(local_working_inode, remote_working_inode, local_live_inode, remote_live_inode, local_identifier, remote_identifier, html_page, endpoint_id, language_id) " +
                "values(?,?,?,?,?,?,?,?,?)");

        dc.addParam("local_working_inode" + System.currentTimeMillis());
        dc.addParam("remote_working_inode" + System.currentTimeMillis());
        dc.addParam("local_live_inode" + System.currentTimeMillis());
        dc.addParam("remote_live_inode" + System.currentTimeMillis());
        dc.addParam("local_identifier" + System.currentTimeMillis());
        dc.addParam("remote_identifier" + System.currentTimeMillis());
        dc.addParam("html_page" + System.currentTimeMillis());
        dc.addParam(remoteIP);
        dc.addParam(Long.valueOf("1"));

        dc.loadResult();
    }

    private void insertFileAssetIntegrityResolver(final String remoteIP) throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("INSERT INTO fileassets_ir " +
                "(local_working_inode, remote_working_inode, local_live_inode, remote_live_inode, local_identifier, remote_identifier, file_name, endpoint_id, language_id) " +
                "values(?,?,?,?,?,?,?,?,?)");

        dotConnect.addParam("local_working_inode" + System.currentTimeMillis());
        dotConnect.addParam("remote_working_inode" + System.currentTimeMillis());
        dotConnect.addParam("local_live_inode" + System.currentTimeMillis());
        dotConnect.addParam("remote_live_inode" + System.currentTimeMillis());
        dotConnect.addParam("local_identifier" + System.currentTimeMillis());
        dotConnect.addParam("remote_identifier" + System.currentTimeMillis());
        dotConnect.addParam("file_name" + System.currentTimeMillis());
        dotConnect.addParam(remoteIP);
        dotConnect.addParam(Long.valueOf(1));

        dotConnect.loadResult();
    }

    private void insertRolesIntegrityResolver(final String remoteIP) throws DotDataException {

        DotConnect dc = new DotConnect();
        dc.setSQL(
                "INSERT INTO cms_roles_ir (name, role_key, local_role_id, remote_role_id, local_role_fqn, remote_role_fqn, endpoint_id) values(?,?,?,?,?,?,?)"
        );

        dc.addParam("name" + System.currentTimeMillis());
        dc.addParam("role_key" + System.currentTimeMillis());
        dc.addParam("local_role_id" + System.currentTimeMillis());
        dc.addParam("remote_role_id" + System.currentTimeMillis());
        dc.addParam("local_role_fqn" + System.currentTimeMillis());
        dc.addParam("remote_role_fqn" + System.currentTimeMillis());
        dc.addParam(remoteIP);

        dc.loadResult();
    }

    private void insertStructuresIntegrityResolver(final String remoteIP) throws DotDataException {

        DotConnect dc = new DotConnect();
        dc.setSQL("INSERT INTO structures_ir (local_inode, remote_inode, endpoint_id) values(?,?,?)");

        dc.addParam("local_inode" + System.currentTimeMillis());
        dc.addParam("remote_inode" + System.currentTimeMillis() );
        dc.addParam(remoteIP);

        dc.loadResult();
    }
}
