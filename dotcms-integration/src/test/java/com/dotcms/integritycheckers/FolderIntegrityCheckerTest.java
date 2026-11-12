package com.dotcms.integritycheckers;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FolderIntegrityCheckerTest extends IntegrationTestBase {

    private User user;
    private HostAPI hostAPI;
    private String endpointId;
    private String testHost;

    private FolderIntegrityChecker integrityChecker;

    @Before
    public void setup() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        integrityChecker = new  FolderIntegrityChecker();
        user = APILocator.getUserAPI().getSystemUser();
        hostAPI = APILocator.getHostAPI();
        endpointId = UUID.randomUUID().toString();
        testHost = "test-host-" + System.currentTimeMillis() + ".dotcms.com";
    }

    /**
     * Method to test: {@link HostIntegrityChecker#executeFix(String)}
     * When: Tests that after conflicts are detected a fix is applied in favor of remote folder.
     * Should: Columns owner and create_date should be populated
     * @throws Exception
     */
    @Test
    public void test_execute_identifierColumnsNotNull() throws Exception {

        final Host host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Folder newFolder = new FolderDataGen().name("testFolder"+UUID.randomUUID().toString()).site(host).nextPersisted();
        final FolderAPI folderAPI = APILocator.getFolderAPI();

        //manually introduced the owner because it's not being set in the dataGen
        final Identifier identifier = APILocator.getIdentifierAPI().find(newFolder.getIdentifier());
        identifier.setOwner("folder's owner");
        IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
        identifierAPI.save(identifier);

        final String remoteIdentifier = introduceConflict(newFolder ,endpointId);

        integrityChecker.executeFix(endpointId);

        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("SELECT owner, create_date FROM identifier WHERE id = ?");
            dotConnect.addParam(remoteIdentifier);
            final Connection connection = DbConnectionFactory.getConnection();
            final List<Map<String, Object>> results = dotConnect.loadObjectResults(connection);

            final boolean createDateNotNull = results.stream()
                    .anyMatch(result -> result.containsKey("create_date") && result.get("create_date") != null);

            Assert.assertTrue("Create Date is null", createDateNotNull);

            final boolean ownerNotNull = results.stream()
                    .anyMatch(result -> result.containsKey("owner") && result.get("owner") != null);

            Assert.assertTrue("Owner is null", ownerNotNull);

        } catch (DotDataException e) {
            Logger.error(this, e);
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    /**
     * Method to test: {@link FolderIntegrityChecker#executeFix(String)}
     * When: A folder conflict carries a remote inode that contains a single quote, and a content type
     * and a permission point at the folder
     * Should: Move the folder, the content type's folder and the permission to the remote inode,
     * stored exactly as received
     * @throws Exception
     */
    @Test
    public void test_executeFix_storesRemoteInodeAsData() throws Exception {
        final Host host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Folder folder = new FolderDataGen().name("testFolder" + UUID.randomUUID()).site(host).nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().host(host).folder(folder).nextPersisted();
        final Role role = new RoleDataGen().nextPersisted();
        APILocator.getPermissionAPI().save(new Permission(folder.getPermissionId(), role.getId(),
                PermissionAPI.PERMISSION_READ), folder, user, false);
        final String remoteInode = "f'" + UUID.randomUUID().toString().replace("-", "").substring(0, 30);
        final String remoteIdentifier = introduceConflict(folder, endpointId, remoteInode);

        try {
            new IntegrityUtil().fixConflicts(endpointId, IntegrityType.FOLDERS);

            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("SELECT inode, identifier FROM folder WHERE inode IN (?, ?)")
                    .addParam(remoteInode)
                    .addParam(folder.getInode())
                    .loadObjectResults();
            Assert.assertEquals(1, rows.size());
            Assert.assertEquals(remoteInode, rows.get(0).get("inode"));
            Assert.assertEquals(remoteIdentifier, rows.get(0).get("identifier"));

            Assert.assertEquals(remoteInode, new DotConnect()
                    .setSQL("SELECT folder FROM structure WHERE inode = ?")
                    .addParam(contentType.inode())
                    .loadObjectResults().get(0).get("folder"));
            Assert.assertEquals(1, new DotConnect()
                    .setSQL("SELECT inode_id FROM permission WHERE inode_id = ? AND roleid = ?")
                    .addParam(remoteInode)
                    .addParam(role.getId())
                    .loadObjectResults().size());
        } finally {
            integrityChecker.discardConflicts(endpointId);
            new DotConnect().setSQL("DELETE FROM permission WHERE roleid = ?").addParam(role.getId()).loadResult();
            Try.run(() -> ContentTypeDataGen.remove(contentType));
            new DotConnect().setSQL("DELETE FROM folder WHERE inode = ?").addParam(remoteInode).loadResult();
            new DotConnect().setSQL("DELETE FROM identifier WHERE id = ?").addParam(remoteIdentifier).loadResult();
            DbConnectionFactory.closeSilently();
        }
    }

    /**
     * Method to test: {@link FolderIntegrityChecker#executeFix(String)}
     * When: A statement after the folder lookup fails, here because the remote inode already belongs
     * to another folder
     * Should: Report the failure instead of skipping the folder, and leave the original folder row
     * and the permission pointing at it in place
     * @throws Exception
     */
    @Test
    public void test_executeFix_failingStatementKeepsOriginalFolder() throws Exception {
        final Host host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Folder folder = new FolderDataGen().name("testFolder" + UUID.randomUUID()).site(host).nextPersisted();
        final Folder otherFolder = new FolderDataGen().name("testFolder" + UUID.randomUUID()).site(host).nextPersisted();
        final Role role = new RoleDataGen().nextPersisted();
        APILocator.getPermissionAPI().save(new Permission(folder.getPermissionId(), role.getId(),
                PermissionAPI.PERMISSION_READ), folder, user, false);
        introduceConflict(folder, endpointId, otherFolder.getInode());

        try {
            Assert.assertThrows(DotDataException.class,
                    () -> new IntegrityUtil().fixConflicts(endpointId, IntegrityType.FOLDERS));

            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("SELECT identifier FROM folder WHERE inode = ?")
                    .addParam(folder.getInode())
                    .loadObjectResults();
            Assert.assertEquals(1, rows.size());
            Assert.assertEquals(folder.getIdentifier(), rows.get(0).get("identifier"));
            Assert.assertEquals(1, new DotConnect()
                    .setSQL("SELECT inode_id FROM permission WHERE inode_id = ? AND roleid = ?")
                    .addParam(folder.getInode())
                    .addParam(role.getId())
                    .loadObjectResults().size());
        } finally {
            integrityChecker.discardConflicts(endpointId);
            new DotConnect().setSQL("DELETE FROM permission WHERE roleid = ?").addParam(role.getId()).loadResult();
            Try.run(() -> FolderDataGen.remove(folder));
            Try.run(() -> FolderDataGen.remove(otherFolder));
            DbConnectionFactory.closeSilently();
        }
    }

    /**
     * Method to test: {@link AbstractIntegrityChecker#getTempTableName(String, IntegrityType, String)}
     * When: The end point id holds only letters, digits, dashes and underscores
     * Should: Build the name from the integrity type, the suffix and the id without its dashes
     */
    @Test
    public void test_getTempTableName_buildsNameFromAllowedCharacters() {
        Assert.assertEquals("folders_templocal_aB3_x9",
                integrityChecker.getTempTableName("aB3-_x9", IntegrityType.FOLDERS, "Local"));
        Assert.assertEquals("folders_temp_0f1e2d3c4b5a",
                integrityChecker.getTempTableName("0f1e2d3c-4b5a", IntegrityType.FOLDERS, ""));
    }

    /**
     * Method to test: {@link AbstractIntegrityChecker#getTempTableName(String, IntegrityType, String)}
     * When: The end point id holds a quote, a dot, a space or another character outside the allowed set
     * Should: Reject the id before a name is built
     */
    @Test
    public void test_getTempTableName_rejectsOtherCharacters() {
        for (final String endpointId : List.of("abc'def", "abc.def", "abc def", "abc;def", "abc\"def")) {
            Assert.assertThrows(endpointId, IllegalArgumentException.class,
                    () -> integrityChecker.getTempTableName(endpointId, IntegrityType.FOLDERS, ""));
        }
    }

    /**
     * Method to test: {@link AbstractIntegrityChecker#getTempTableName(String, IntegrityType, String)}
     * When: The end point id is missing, empty or blank
     * Should: Build no name at all, so nothing reaches the SQL
     */
    @Test
    public void test_getTempTableName_buildsNoNameWithoutAnId() {
        Assert.assertNull(integrityChecker.getTempTableName("", IntegrityType.FOLDERS, ""));
        Assert.assertNull(integrityChecker.getTempTableName("  ", IntegrityType.FOLDERS, ""));
        Assert.assertNull(integrityChecker.getTempTableName(null, IntegrityType.FOLDERS, ""));
    }

    private String introduceConflict(final Folder folder, final String endpointId) throws DotDataException {
        return introduceConflict(folder, endpointId, UUID.randomUUID().toString());
    }

    @WrapInTransaction
    private String introduceConflict(final Folder folder, final String endpointId,
            final String remoteWorkingInode) throws DotDataException {
        final DotConnect dotConnect = new DotConnect();

        dotConnect.setSQL("INSERT INTO folders_ir " +
                "(local_identifier, remote_identifier, endpoint_id, local_inode, remote_inode) " +
                "values(?,?,?,?,?)");

        final String remoteIdentifier = UUID.randomUUID().toString();

        dotConnect.addParam(folder.getIdentifier());
        dotConnect.addParam(remoteIdentifier);
        dotConnect.addParam(endpointId);
        dotConnect.addParam(folder.getInode());
        dotConnect.addParam(remoteWorkingInode);
        dotConnect.loadResult();
        return remoteIdentifier;
    }
}
