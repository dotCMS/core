package com.dotcms.integritycheckers;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.WrapInTransaction;
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
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.dotcms.content.business.json.ContentletJsonAPI.SAVE_CONTENTLET_AS_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Host integrity checker which tests the main functionality of this integrity checker.
 */
@RunWith(DataProviderRunner.class)
public class HostIntegrityCheckerTest extends IntegrationTestBase {

    private HostIntegrityChecker integrityChecker;
    private User user;
    private HostAPI hostAPI;
    private String endpointId;
    private String testSiteName;

    @Before
    public void setup() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        integrityChecker = new HostIntegrityChecker();
        user = APILocator.getUserAPI().getSystemUser();
        hostAPI = APILocator.getHostAPI();
        endpointId = UUID.randomUUID().toString();
        testSiteName = "test-host-" + System.currentTimeMillis() + ".dotcms.com";
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
        final Host host = addSite(testSiteName);
        final File generatedCsv = integrityChecker.generateCSVFile(endpointFolder);
        assertNotNull(generatedCsv);
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
            if (testSiteName.equals(HostIntegrityChecker.getStringIfNotBlank("text1", hosts.get(5)))) {
                hostFound = true;
                break;
            }
        }
        assertTrue(hostFound);

        HibernateUtil.startTransaction();
        removeHost(new DotConnect(), host.getInode());
        HibernateUtil.closeAndCommitTransaction();
    }

    /**
     * Tests that after a hosts CSV file is generated conflicts are detected and that they are persisted to
     * designated table,
     */
    @Test
    public void test_generateIntegrityResults() throws Exception {
        final String endpointFolder = prepareResources();
        final Host testSite = addSite(testSiteName);
        final File generatedCsv = integrityChecker.generateCSVFile(endpointFolder);
        Host duplicateSite = addDup();
        duplicateSite = this.updateDuplicateSite(duplicateSite);
        assertNotNull(generatedCsv);

        assertTrue("", integrityChecker.generateIntegrityResults(endpointId));
        final DotConnect dc = new DotConnect();
        assertEquals("", 1L,
                dc.getRecordCount("hosts_ir", "WHERE host = '" + testSite.getName() + "'")
                        .longValue());

        HibernateUtil.startTransaction();
        removeHost(dc, testSite.getInode());
        removeHost(dc, duplicateSite.getInode());
        HibernateUtil.closeAndCommitTransaction();
    }

    /**
     * Tests that after conflicts are detected a fix is applied in favor of remote host.
     * This means that remote is persisted and local is deleted.
     */
    @Test
    public void test_executeFix() throws Exception {
        final String endpointFolder = prepareResources();

        HibernateUtil.startTransaction();
        final Host host = addSite(testSiteName);
        integrityChecker.generateCSVFile(endpointFolder);
        Host duplicateSite = addDup();
        duplicateSite = this.updateDuplicateSite(duplicateSite);
        integrityChecker.generateIntegrityResults(endpointId);

        final DotConnect dc = new DotConnect();
        assertEquals(1L,
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
        removeHost(dc, duplicateSite.getInode());
        HibernateUtil.closeAndCommitTransaction();
    }

    @NotNull
    private String prepareResources() {
        final String assetRealPath = Config.getStringProperty("ASSET_REAL_PATH", "test-resources");
        final String endpointFolder = assetRealPath + "/integrity/" + endpointId;
        final File outputFolder = new File(endpointFolder);
        if (!outputFolder.exists()) {
            assertTrue(outputFolder.mkdirs());
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
        assertEquals(
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
        assertEquals(
                1L,
                dc.getRecordCount("contentlet", "WHERE inode = '" + inode + "' AND contentlet_as_json IS NOT NULL").longValue());
    }

    /**
     * Adds a duplicate site (host) with the name "updated-test-host" and saves it to the database.
     * This method is used to create a site that will later be updated to create a conflict. But,
     * before being able to do that, we need to create with a different name to avoid validation
     * errors.
     *
     * @return the created and saved Host object
     *
     * @throws Exception if there is an error accessing the data layer or saving the host
     */
    private Host addDup() throws Exception {
        return addSite("updated-" + testSiteName);
    }

    /**
     * Updates the duplicate site (host) by changing its hostname back to the expected test site
     * name. This will then create the expected integrity conflict. The important part here is that:
     * <ul>
     *     <li>We need to FORCE the elasticsearch indexation.</li>
     *     <li>DO NOT validate the updated Site so that the conflict can be introduced</li>
     *     <li>Set the {@code forceExecution} property to {@code true} so that the Site name can be
     *     updated.</li>
     * </ul>
     *
     * @param duplicateSite the host to be updated
     *
     * @return the updated Host object
     *
     * @throws DotDataException     if there is an error accessing the data layer
     * @throws DotSecurityException if the operation is not permitted due to security restrictions
     */
    private Host updateDuplicateSite(final Host duplicateSite) throws DotDataException, DotSecurityException {
        duplicateSite.setHostname(testSiteName);
        duplicateSite.setDefault(false);
        duplicateSite.setIndexPolicy(IndexPolicy.FORCE);
        duplicateSite.setProperty(Contentlet.DONT_VALIDATE_ME, true);
        duplicateSite.setProperty("forceExecution", true);
        duplicateSite.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        HibernateUtil.startTransaction();
        final Host saved = hostAPI.save(duplicateSite, user, false);
        HibernateUtil.closeAndCommitTransaction();
        return saved;
    }

    /**
     * Adds a new site (host) with the specified name and saves it to the database. The created
     * host will not be set as the default.
     *
     * @param name the name of the host to be created
     *
     * @return the created and saved Host object
     *
     * @throws DotDataException     if there is an error accessing the data layer
     * @throws DotSecurityException if the operation is not permitted due to security restrictions
     */
    private Host addSite(final String name) throws DotDataException, DotSecurityException {
        final Host site = new Host();
        site.setHostname(name);
        site.setDefault(false);
        site.setIndexPolicy(IndexPolicy.FORCE);
        site.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        HibernateUtil.startTransaction();
        final Host saved = hostAPI.save(site, user, false);
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

            assertTrue("Asset_SubType is null", assetSubtypeNotNull);

            boolean createDateNotNull = results.stream()
                    .anyMatch(result -> result.containsKey("create_date") && result.get("create_date") != null);

            assertTrue("Create Date is null", createDateNotNull);

            boolean ownerNotNull = results.stream()
                    .anyMatch(result -> result.containsKey("owner") && result.get("owner") != null);

            assertTrue("Owner is null", ownerNotNull);
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
