package com.dotcms.integritycheckers;

import static com.dotcms.content.business.json.ContentletJsonAPI.SAVE_CONTENTLET_AS_JSON;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import io.vavr.Tuple2;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.vavr.Tuple;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Host integrity checker which tests the main functionality of this integrity checker.
 */
@RunWith(DataProviderRunner.class)
public class HostIntegrityCheckerTest extends IntegrationTestBase {
    private HostIntegrityChecker integrityChecker;
    private User user;
    private HostAPI hostAPI;
    private String endpointId;
    private String testHost;

    @Before
    public void setup() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        integrityChecker = new HostIntegrityChecker();
        user = APILocator.getUserAPI().getSystemUser();
        hostAPI = APILocator.getHostAPI();
        endpointId = UUID.randomUUID().toString();
        testHost = "test-host-" + System.currentTimeMillis() + ".dotcms.com";
    }

    /**
     * Evaluates that integrity type is actually {@link IntegrityType}.HOSTS.
     */
    @Test
    public void test_getIntegrityType() {
        Assert.assertSame(IntegrityType.HOSTS, integrityChecker.getIntegrityType());
    }

    /**
     * Tests that hosts CSV file is generated with introduced host.
     */
    @Test
    public void test_generateCSVFile() throws Exception {
        final String endpointFolder = prepareResources();
        final Host host = addHost(testHost);
        final File generatedCsv = integrityChecker.generateCSVFile(endpointFolder);
        Assert.assertNotNull(generatedCsv);
        final CsvReader hosts = new CsvReader(
                ConfigUtils.getIntegrityPath() +
                        File.separator +
                        endpointId +
                        File.separator +
                        integrityChecker.getIntegrityType().getDataToCheckCSVName(),
                '|',
                StandardCharsets.UTF_8);
        boolean hostFound = false;
        while(hosts.readRecord()) {
            if (testHost.equals(HostIntegrityChecker.getStringIfNotBlank("text1", hosts.get(5)))) {
                hostFound = true;
                break;
            }
        }
        Assert.assertTrue(hostFound);

        HibernateUtil.startTransaction();
        removeHost(new DotConnect(), host.getInode());
        HibernateUtil.closeAndCommitTransaction();
    }

    /**
     * Tests that after a hosts CSV file is generated conflicts are detected and that they are persisted to
     * designated table,
     */
    @Test
    @UseDataProvider("getUseJsonTestCases")
    public void test_generateIntegrityResults(final Boolean useJson) throws Exception {

        final boolean defaultValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, useJson);
        try {
            final String endpointFolder = prepareResources();
            final Host host = addHost(testHost);
            final File generatedCsv = integrityChecker.generateCSVFile(endpointFolder);
            Assert.assertNotNull(generatedCsv);
            final Host dup = addDup();

            Assert.assertTrue(integrityChecker.generateIntegrityResults(endpointId));
            final DotConnect dc = new DotConnect();
            Assert.assertEquals(1L,
                    dc.getRecordCount("hosts_ir", "WHERE host = '" + host.getName() + "'")
                            .longValue());


            HibernateUtil.startTransaction();
            removeHost(dc, host.getInode());
            removeHost(dc, dup.getInode());
            HibernateUtil.closeAndCommitTransaction();
        }finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }
    }

    /**
     * Tests that after conflicts are detected a fix is applied in favor of remote host.
     * This means that remote is persisted and local is deleted.
     */
    @Test
    @UseDataProvider("getUseJsonTestCases")
    public void test_executeFix(final Boolean useJson) throws Exception {
        final boolean defaultValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, useJson);
        try {
            final String endpointFolder = prepareResources();

            HibernateUtil.startTransaction();
            final Host host = addHost(testHost);
            integrityChecker.generateCSVFile(endpointFolder);
            final Host dup = addDup();
            integrityChecker.generateIntegrityResults(endpointId);

            final DotConnect dc = new DotConnect();
            Assert.assertEquals(1L,
                    dc.getRecordCount("hosts_ir", "WHERE host = '" + host.getName() + "'")
                            .longValue());

            // Some hack in order to simulate remote integrity
            removeRemoteResults(dc);
            HibernateUtil.closeAndCommitTransaction();

            integrityChecker.executeFix(endpointId);
            assertHostsFix(dc);
            assertHostHasContentletAsJson(dc, host.getInode());

            HibernateUtil.startTransaction();
            removeHost(dc, host.getInode());
            removeHost(dc, dup.getInode());
            HibernateUtil.closeAndCommitTransaction();
        }finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }
    }

    @NotNull
    private String prepareResources() {
        final String assetRealPath = Config.getStringProperty("ASSET_REAL_PATH", "test-resources");
        final String endpointFolder = assetRealPath + "/integrity/" + endpointId;
        final File outputFolder = new File(endpointFolder);
        if (!outputFolder.exists()) {
            Assert.assertTrue(outputFolder.mkdirs());
        }
        return endpointFolder;
    }

    private List<Map<String, Object>> getConflicts(DotConnect dc) throws DotDataException {
        return dc.setSQL("SELECT remote_working_inode, local_working_inode, remote_live_inode, local_live_inode," +
                        " remote_identifier, local_identifier, language_id, host" +
                        " FROM hosts_ir" +
                        " WHERE endpoint_id = ?")
                .addParam(endpointId)
                .loadResults();
    }

    private void assertHost(DotConnect dc,
                            Map<String, Object> result,
                            String table,
                            String column,
                            String value,
                            long expected)
            throws Exception {
        Assert.assertEquals(
                expected,
                dc.getRecordCount(table,"WHERE " + column + " = '" + result.get(value) + "'").longValue());
    }

    private void assertHostFix(DotConnect dc, Map<String, Object> result) throws Exception {
        assertHost(dc, result, "contentlet", "inode", "remote_working_inode", 1L);
        assertHost(dc, result, "contentlet_version_info", "working_inode", "remote_working_inode", 1L);
        assertHost(dc, result, "contentlet", "inode", "local_working_inode", 0L);
        assertHost(dc, result, "contentlet_version_info", "working_inode", "local_working_inode", 0L);
    }

    private void assertHostsFix(DotConnect dc) throws Exception {
        final List<Map<String, Object>> results = getConflicts(dc);
        results.forEach(result -> {
            try {
                assertHostFix(dc, result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void assertHostHasContentletAsJson(DotConnect dc, String inode) throws Exception {
        Assert.assertEquals(
                1L,
                dc.getRecordCount("contentlet", "WHERE inode = '" + inode + "' AND contentlet_as_json IS NOT NULL").longValue());
    }

    @NotNull
    private Host addDup() throws Exception {
        final Host dup = addHost(testHost);
        final DotConnect dc = new DotConnect();
        dc.setSQL("UPDATE contentlet SET text1 = ? WHERE inode = ?")
                .addParam(testHost)
                .addParam(dup.getInode());
        return dup;
    }

    private Host addHost(final String name) throws DotDataException, DotSecurityException {
        final Host host = new Host();
        host.setHostname(name);
        host.setDefault(false);
        HibernateUtil.startTransaction();
        final Host saved = hostAPI.save(host, user, false);
        HibernateUtil.closeAndCommitTransaction();
        return saved;
    }

    private void removeHost(DotConnect dc, String inode) throws Exception {
        dc.executeStatement("DELETE FROM contentlet_version_info WHERE working_inode = '" + inode + "'");
        dc.executeStatement("DELETE FROM contentlet WHERE inode = '" + inode + "'");
    }

    private void removeRemoteResults(DotConnect dc) throws Exception {
        final List<Map<String, Object>> results = getConflicts(dc);
        results.forEach(result -> {
            try {
                removeHost(dc, (String) result.get("remote_working_inode"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Make sure test are executed under a contentlet stored as json and contentlet stored in column scenarios
     * @return
     * @throws Exception
     */
    @DataProvider
    public static Object[] getUseJsonTestCases() throws Exception {
        return new Object[]{
                true,
                false
        };
    }

    /**
     * Method to test: {@link HostIntegrityChecker#executeFix(String)}
     * When: Tests that after conflicts are detected a fix is applied in favor of remote host.
     * Should: Columns Asset_subtype, owner and create_date should be populated
     * @throws Exception
     */
    @Test
    public void test_executeFix_identifierColumnsNotNull() throws Exception {

        //Create new Site
        final Host newSite = new SiteDataGen().nextPersisted();

        //Introduce Conflict, will return new identifier of the site
        final String remoteIdentifier = introduceConflict(newSite ,endpointId);

        integrityChecker.executeFix(endpointId);

        try{
            //Query to check that the columns were populated, using the remoteIdentifier since
            //it's the new Id of the site.
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("SELECT asset_subtype, owner, create_date FROM identifier WHERE id = ?");
            dotConnect.addParam(remoteIdentifier);
            final List<Map<String, Object>> results = dotConnect.loadObjectResults();

            boolean assetSubtypeNotNull = results.stream()
                    .anyMatch(result -> result.containsKey("asset_subtype") && result.get("asset_subtype") != null);

            Assert.assertTrue("Asset_SubType is null", assetSubtypeNotNull);

            boolean createDateNotNull = results.stream()
                    .anyMatch(result -> result.containsKey("create_date") && result.get("create_date") != null);

            Assert.assertTrue("Create Date is null", createDateNotNull);

            boolean ownerNotNull = results.stream()
                    .anyMatch(result -> result.containsKey("owner") && result.get("owner") != null);

            Assert.assertTrue("Owner is null", ownerNotNull);
        } catch (DotDataException e) {
            Logger.error(this, e);
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    @WrapInTransaction
    private String introduceConflict(final Host site, final String endpointId) throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("INSERT INTO hosts_ir \n"
                + "(local_identifier, remote_identifier, endpoint_id, local_working_inode, remote_working_inode, language_id, host)\n"
                + "VALUES(?, ?, ?, ?, ?, ?, ?)");

        final String remoteIdentifier = UUID.randomUUID().toString();
        final String remoteWorkingInode = UUID.randomUUID().toString();

        dotConnect.addParam(site.getIdentifier());
        dotConnect.addParam(remoteIdentifier);
        dotConnect.addParam(endpointId);
        dotConnect.addParam(site.getInode());
        dotConnect.addParam(remoteWorkingInode);
        dotConnect.addParam(site.getLanguageId());
        dotConnect.addParam(site.getHostname());
        dotConnect.loadResult();
        return remoteIdentifier;
    }

}
