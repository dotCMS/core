package com.dotcms.integritycheckers;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
/**
 * Simple test to validate the ContentFileAssetIntegrityChecker
 */
public class ContentFileAssetIntegrityCheckerTest extends IntegrationTestBase implements IntegrityCheckerTest {


    @Before
    public void setup() throws Exception {
        //Setting web app environment
        setUpEnvironment();
    }

    /**
     * Creates a new file asset and publishes it
     * @param file
     * @param folder
     * @return
     * @throws IOException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    FileAsset newFileAsset(final File file, final Folder folder)
            throws IOException, DotDataException, DotSecurityException {
        final Contentlet persisted = new FileAssetDataGen(file)
                .languageId(1)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        ContentletDataGen.publish(persisted);
        return APILocator.getFileAssetAPI().fromContentlet(persisted);
    }

    /**
     * Simplest way to simulate a conflict is to introduce a conflict directly into the  fileassets_ir table
     * @param contentlet
     * @param endpointId
     * @return
     * @throws DotDataException
     */
    @WrapInTransaction
    Tuple2<String, String> introduceConflict(final FileAsset contentlet, final String endpointId)
            throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("INSERT INTO fileassets_ir \n"
                + "(file_name, local_working_inode, remote_working_inode, local_identifier, remote_identifier, endpoint_id, language_id)\n"
                + "VALUES(?, ?, ?, ?, ?, ?, ?)");

        final String remoteIdentifier = UUID.randomUUID().toString();
        final String remoteWorkingInode = UUID.randomUUID().toString();

        dotConnect.addParam(contentlet.getFileName());
        dotConnect.addParam(contentlet.getInode());
        dotConnect.addParam(remoteWorkingInode);
        //
        dotConnect.addParam(contentlet.getIdentifier());
        dotConnect.addParam(remoteIdentifier);
        dotConnect.addParam(endpointId);
        dotConnect.addParam(contentlet.getLanguageId());
        dotConnect.loadResult();
        return Tuple.of(remoteIdentifier, remoteWorkingInode);
    }


    /**
     * Given Scenario: A file asset is created and published  the we introduce a conflict directly into the  fileassets_ir table
     * Expected Behavior: The integrity checker should be able to solve the conflict and generate the contentlet as json
     * @throws Exception
     */
    @Test
    public void TestFixConflictGeneratesContentletAsJson() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final File file = FileUtil.createTemporaryFile("test", ".txt", "this is a test!");
        final FileAsset fileAsset = newFileAsset(file, folder);

        final ContentFileAssetIntegrityChecker integrityChecker = new ContentFileAssetIntegrityChecker();
        final Tuple2<String, String> remoteIdentifierAndInode = introduceConflict(fileAsset,
                endpointId.get());

        final String remoteIdentifier = remoteIdentifierAndInode._1();
        final String remoteWorkingInode = remoteIdentifierAndInode._2();

        Logger.debug(this, "remoteIdentifier: " + remoteIdentifier + " remoteWorkingInode: "
                + remoteWorkingInode);

        integrityChecker.executeFix(endpointId.get());
        Assert.assertTrue(validateFix(remoteIdentifier));
    }

    /**
     * Method to test: {@link ContentFileAssetIntegrityChecker#executeFix(String)}
     * When: Tests that after conflicts are detected a fix is applied in favor of remote fileAsset.
     * Should: Columns Asset_subtype, owner and create_date should be populated
     * @throws Exception
     */
    @Test
    public void test_execute_identifierColumnsNotNull() throws Exception {
        final Host host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Folder folder = new FolderDataGen().name("testFolder"+UUID.randomUUID().toString()).site(host).nextPersisted();
        final Contentlet contentlet = FileAssetDataGen.createFileAsset(folder, "text1FileAsset"+UUID.randomUUID().toString(), ".txt");
        final FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(contentlet);
        final ContentFileAssetIntegrityChecker integrityChecker = new ContentFileAssetIntegrityChecker();
        final Tuple2<String, String> remoteIdentifierAndInode = introduceConflict(fileAsset ,endpointId.get());
        final String remoteIdentifier = remoteIdentifierAndInode._1();
        final String remoteWorkingInode = remoteIdentifierAndInode._2();

        integrityChecker.executeFix(endpointId.get());

        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("SELECT asset_subtype, owner, create_date FROM identifier WHERE id = ?");
            dotConnect.addParam(remoteIdentifier);
            final Connection connection = DbConnectionFactory.getConnection();
            List<Map<String, Object>> results = dotConnect.loadObjectResults(connection);

            final boolean assetSubtypeNotNull = results.stream()
                    .anyMatch(result -> result.containsKey("asset_subtype") && result.get("asset_subtype") != null);

            Assert.assertTrue("Asset_SubType is null", assetSubtypeNotNull);

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
}
