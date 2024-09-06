package com.dotcms.integritycheckers;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
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

    @WrapInTransaction
    private String introduceConflict(final Folder folder, final String endpointId) throws DotDataException {
        final DotConnect dotConnect = new DotConnect();

        dotConnect.setSQL("INSERT INTO folders_ir " +
                "(local_identifier, remote_identifier, endpoint_id, local_inode, remote_inode) " +
                "values(?,?,?,?,?)");

        final String remoteIdentifier = UUID.randomUUID().toString();
        final String remoteWorkingInode = UUID.randomUUID().toString();

        dotConnect.addParam(folder.getIdentifier());
        dotConnect.addParam(remoteIdentifier);
        dotConnect.addParam(endpointId);
        dotConnect.addParam(folder.getInode());
        dotConnect.addParam(remoteWorkingInode);
        dotConnect.loadResult();
        return remoteIdentifier;
    }
}
