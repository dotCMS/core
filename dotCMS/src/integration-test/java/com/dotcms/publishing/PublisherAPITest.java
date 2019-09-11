package com.dotcms.publishing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.factory.PublishingEndPointFactory;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.felix.framework.OSGIUtil;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by Oscar Arrieta on 7/7/17.
 */
public class PublisherAPITest extends IntegrationTestBase {

    private static HostAPI hostAPI;
    private static FolderAPI folderAPI;
    private static UserAPI userAPI;
    private static RoleAPI roleAPI;
    private static EnvironmentAPI environmentAPI;
    private static PublishingEndPointAPI publisherEndPointAPI;
    private static PublisherAPI publisherAPI;
    private static LanguageAPI languageAPI;
    private static PublishAuditAPI publishAuditAPI;
    private static BundleAPI bundleAPI;

    private static User systemUser;
    private static User adminUser;

    private static Host site;

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        hostAPI = APILocator.getHostAPI();
        folderAPI = APILocator.getFolderAPI();
        userAPI = APILocator.getUserAPI();
        roleAPI = APILocator.getRoleAPI();
        environmentAPI = APILocator.getEnvironmentAPI();
        publisherEndPointAPI = APILocator.getPublisherEndPointAPI();
        publisherAPI = APILocator.getPublisherAPI();
        languageAPI = APILocator.getLanguageAPI();
        publishAuditAPI = PublishAuditAPI.getInstance();
        bundleAPI = APILocator.getBundleAPI();

        systemUser = userAPI.getSystemUser();
        adminUser = APILocator.getUserAPI()
                .loadByUserByEmail("admin@dotcms.com", systemUser, false);

        //Create test data
        site = new SiteDataGen().nextPersisted();
        new FolderDataGen().site(site).nextPersisted();
        new FolderDataGen().site(site).nextPersisted();
        new FolderDataGen().site(site).nextPersisted();
    }

    /**
     * This test will:
     * ---Setup sender environment.
     * ---Get a random folder from the default host.
     * ---Push publish the folder. (It will fail cause receiver doesn't exist)
     * ---Get the date of a file.
     * ---Push publish retry. (Retry #1)
     * ---Check that the file dates
     *
     * https://github.com/dotCMS/core/issues/12038
     */
    @Test
    public void test_publish_fail_retry() {

        Environment environment = null;
        Bundle bundle = null;

        try {
            /* Setup sender environment. */
            final String publishEnvironmentName = "Publish Environment";

            environment = new Environment();
            environment.setName(publishEnvironmentName);
            environment.setPushToAll(false);

            // Find the roles of the admin user.
            Role role = roleAPI.loadRoleByKey(adminUser.getUserId());

            //Create the permissions for the environment
            List<Permission> permissions = new ArrayList<>();
            Permission p = new Permission(environment.getId(), role.getId(),
                    PermissionAPI.PERMISSION_USE);
            permissions.add(p);

            // Create a environment.
            environmentAPI.saveEnvironment(environment, permissions);

            // Now we need to create the end point.
            final String protocol = "http";
            final PublishingEndPointFactory factory = new PublishingEndPointFactory();
            PublishingEndPoint endpoint = factory.getPublishingEndPoint(protocol);
            endpoint.setServerName(new StringBuilder("Publish Endpoint"));
            endpoint.setAddress("127.0.0.1");
            endpoint.setPort("8765");
            endpoint.setProtocol(protocol);
            endpoint.setAuthKey(new StringBuilder(PublicEncryptionFactory.encryptString("1111")));
            endpoint.setEnabled(true);
            endpoint.setSending(false);
            endpoint.setGroupId(environment.getId());

            // Save the endpoint.
            publisherEndPointAPI.saveEndPoint(endpoint);

            /* Get a random folder from the default host. */
            final List<Folder> folderList = folderAPI
                    .findFoldersByHost(site, systemUser, false);
            final Folder folder = folderList.get(0);
            Logger.info(this, "Using folder: " + folder.getName());

            /* Push publish the folder. */
            // Set up Bundle.
            bundle = new Bundle(null, new Date(), null, adminUser.getUserId(), false);
            bundleAPI.saveBundle(bundle, Lists.newArrayList(environment));

            // Set up PublishAuditHistory.
            PublishAuditHistory historyPojo = new PublishAuditHistory();
            Map<String, String> assetsAsMap = Maps.newHashMap();
            assetsAsMap.put(folder.getInode(), PusheableAsset.FOLDER.getType());
            historyPojo.setAssets(assetsAsMap);

            // Set up PublishAuditStatus.
            PublishAuditStatus status = new PublishAuditStatus(bundle.getId());
            status.setStatusPojo(historyPojo);

            // Insert in Audit table.
            publishAuditAPI.insertPublishAuditStatus(status);

            // Set up config Object.
            PublishQueueElement publishQueueElement = new PublishQueueElement();
            publishQueueElement.setId(1);
            publishQueueElement.setOperation(1);
            publishQueueElement.setAsset(folder.getInode());
            publishQueueElement.setEnteredDate(new Date());
            publishQueueElement.setPublishDate(new Date());
            publishQueueElement.setBundleId(bundle.getId());
            publishQueueElement.setType(PusheableAsset.FOLDER.getType());

            PushPublisherConfig publisherConfig = new PushPublisherConfig();
            publisherConfig.setId(bundle.getId());
            publisherConfig.setOperation(Operation.PUBLISH);
            publisherConfig.setLanguage(languageAPI.getDefaultLanguage().getId());
            publisherConfig.setAssets(Lists.newArrayList(publishQueueElement));
            publisherConfig.setLuceneQueries(Lists.newArrayList());
            publisherConfig.setUser(systemUser);
            // Set yesterday's date in order bundle.xml has different info and file be modified.
            publisherConfig.setStartDate(new Date(new Date().getTime() - 24*3600*1000));
            publisherConfig.setPublishers(Lists.newArrayList(PushPublisher.class));

            // Push Publish.
            publisherAPI.publish(publisherConfig);
            /* Get the date of a file. */
            final String bundlePath =
                    ConfigUtils.getBundlePath() + File.separator + publisherConfig.getName();
            final String bundleTarGzPath = bundlePath + ".tar.gz";
            final String bundleXMLPath = bundlePath + File.separator + "bundle.xml";

            final File bundleFolder = new File(bundlePath);
            assertTrue("Bundle Folder Exists", bundleFolder.exists());
            final File bundleTarGz = new File(bundleTarGzPath);
            assertTrue("Bundle Tar Gz Exists", bundleTarGz.exists());
            final long bundleTarGzFirstDate = bundleTarGz.lastModified();
            final File bundleXML = new File(bundleXMLPath);
            assertTrue("bundle.xml exists", bundleXML.exists());
            final long bundleXMLFirstDate = bundleXML.lastModified();

            final Map<String, Long> firstFileDates = getFileDatesByFolder(bundleFolder,
                    getNoBundleXMLFileFilter());

            // Let's wait 2 seconds between runs so we have different millis in files.
            Logger.info(this, "Waiting 2 seconds before 2nd PP run");
            Thread.sleep(2000);

            /* Push publish retry. */
            // As we are using the same config and bundle,
            // the PublishAuditStatus.getStatusPojo().getNumTries() should be > 0.
            publisherConfig = new PushPublisherConfig();
            publisherConfig.setId(bundle.getId());
            publisherConfig.setOperation(Operation.PUBLISH);
            publisherConfig.setLanguage(languageAPI.getDefaultLanguage().getId());
            publisherConfig.setAssets(Lists.newArrayList(publishQueueElement));
            publisherConfig.setLuceneQueries(Lists.newArrayList());
            publisherConfig.setUser(systemUser);
            // Set today's date in order bundle.xml has different info and file be modified.
            publisherConfig.setStartDate(new Date());
            publisherConfig.setPublishers(Lists.newArrayList(PushPublisher.class));

            publisherAPI.publish(publisherConfig);

            /* Check the file dates */
            final long bundleTarGzSecondDate = bundleTarGz.lastModified();
            // Tar Gz File should have the same date.
            assertEquals("Tar Gz File should have the same date",
                    bundleTarGzFirstDate, bundleTarGzSecondDate);
            final long bundleXMLSecondDate = bundleXML.lastModified();
            // bundle.xml file should be updated each PP process, so dates shouldn't be the same.
            assertNotEquals("bundle.xml file should be updated each PP process",
                    bundleXMLFirstDate, bundleXMLSecondDate);

            final Map<String, Long> secondFileDates = getFileDatesByFolder(bundleFolder,
                    getNoBundleXMLFileFilter());

            // We want to check bundle folder contains same file and they were not modified.
            for (String filePath : secondFileDates.keySet()) {
                Logger.info(this, "Checking file: " + filePath);
                assertTrue("Check bundle folder contains same file " + filePath,
                        firstFileDates.containsKey(filePath));
                assertEquals("Check dates were not modified" + filePath,
                        firstFileDates.get(filePath), secondFileDates.get(filePath));
            }

        } catch (DotDataException
                | DotSecurityException
                | DotPublisherException
                | DotPublishingException
                | InterruptedException
                | FileNotFoundException e) {
            fail(e.getMessage());
        } finally {
            try {
                /* Cleaning environment. */
                // Delete the env.
                if (environment != null) {
                    environmentAPI.deleteEnvironment(environment.getId());
                }
                // Delete the bundle.
                if (bundle != null) {
                    bundleAPI.deleteBundle(bundle.getId());
                }
            } catch (DotDataException e) {
                fail(e.getMessage());
            }
        }
    }

    /**
     * Util method to get getAbsolutePath and lastModified from all the files inside a Folder.
     *
     * @param folder Directory that you want to get the files.
     * @param fileFilter {@link FileFilter} that you want to use to filter. If the given {@code
     * fileFilter} is {@code null} then all pathnames are accepted
     * @return Returns a {@link Map} with Key:File.getAbsolutePath and Value:File.lastModified
     */
    private Map<String, Long> getFileDatesByFolder(final File folder, FileFilter fileFilter)
            throws FileNotFoundException {
        Map<String, Long> fileDates = Maps.newHashMap();

        for (File file : FileUtil.listFilesRecursively(folder, fileFilter)) {
            fileDates.put(file.getAbsolutePath(), new Long(file.lastModified()));
        }

        return fileDates;
    }

    /**
     * {@link FileFilter} that will filter all the files that end with bundle.xml.
     */
    private FileFilter getNoBundleXMLFileFilter() {
        return file -> !file.getAbsolutePath().endsWith("bundle.xml");
    }

}
