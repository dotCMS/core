package com.dotmarketing.startup.runalways;

import static com.dotcms.security.apps.AppsUtil.APPS_IMPORT_EXPORT_DEFAULT_PASSWORD;
import static com.liferay.util.StringPool.BLANK;

import com.dotcms.datagen.AppDescriptorDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppDescriptorHelper;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.AppsCache;
import com.dotcms.security.apps.AppsUtil;
import com.dotcms.security.apps.SecretsKeyStoreHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.startup.runalways.Task00050LoadAppsSecrets;
import com.dotmarketing.util.Config;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task00050LoadAppsSecretsTest {

    static AppsAPI api;
    static User admin;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        api = APILocator.getAppsAPI();
        admin = TestUserUtils.getAdminUser();

    }

    private void destroySecretsStore() {
        final String secretStorePath = SecretsKeyStoreHelper.getSecretStorePath();
        new File(secretStorePath).delete();
        final AppsCache appsCache = CacheLocator.getAppsCache();
        appsCache.clearCache();
    }

    private AppDescriptor genAppDescriptor()
            throws DotSecurityException, AlreadyExistException, DotDataException, IOException {

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("p1", false, true)
                .stringParam("p2", false, true)
                .stringParam("p3", false, true)
                .withName("system-app-example")
                .withDescription("system-app-demo")
                .withExtraParameters(false);
        final File file = dataGen.nextPersistedDescriptor();
        return api.createAppDescriptor(file, admin);
    }

    private Path createSecretsAndExportThem(final AppDescriptor descriptor, final Key key,
            final Set<Host> sites)
            throws IOException, DotDataException, DotSecurityException, AlreadyExistException {

        final AppsAPI api = APILocator.getAppsAPI();

        final AppSecrets.Builder builder1 = new AppSecrets.Builder();
        final String appKey = descriptor.getKey();
        final AppSecrets secrets = builder1.withKey(appKey)
                .withHiddenSecret("p1", "secret-1")
                .withHiddenSecret("p2", "secret-2")
                .withHiddenSecret("p3", "secret-3")
                .build();
        final Map<String, Set<String>> appKeysBySite = new HashMap<>();
        for (final Host site : sites) {
            //Save it
            api.saveSecrets(secrets, site, admin);
            appKeysBySite.put(site.getIdentifier(), ImmutableSet.of(appKey));
        }
        return api.exportSecrets(key, false, appKeysBySite, admin);

    }

    /**
     * Given scenario: Happy path scenario We generate a file then we remove the secret store and we
     * run the import task Expected Result: No exceptions are expected. We expect the secret store
     * to have been regenerated then we verify the new secret exist.
     */
    @Test
    public void Test_UpgradeTask()
            throws DotDataException, DotSecurityException, AlreadyExistException, IOException {
        final Host site = new SiteDataGen().nextPersisted();
        final AppDescriptor descriptor = genAppDescriptor();
        destroySecretsStore();
        final Key key = AppsUtil.generateKey(AppsUtil.loadPass(null));
        final File exportFile = createSecretsAndExportThem(descriptor, key, ImmutableSet.of(site))
                .toFile();

        final Path serverDir = AppDescriptorHelper.getServerDirectory();

        final File fileToImport = new File(serverDir.toString(), "dotSecrets-import.xxx");
        //Task will match any file matching this name followed by any extension
        exportFile.renameTo(fileToImport);
        destroySecretsStore();

        final Task00050LoadAppsSecrets task = new Task00050LoadAppsSecrets();
        Assert.assertTrue(task.forceRun());
        task.executeUpgrade();
        Assert.assertEquals(1, task.getImportCount());
        final AppsAPI api = APILocator.getAppsAPI();
        final Optional<AppSecrets> secrets = api.getSecrets(descriptor.getKey(), site, admin);
        Assert.assertTrue(secrets.isPresent());
        //finally test file got removed.
        Assert.assertFalse(fileToImport.exists());
    }

    /**
     * Given scenario: We generate an export file that has a secret associated to a site. Then Site
     * gets removed to simulate a scenario on which we import a secret for a non-exiting site on a
     * receiver node Expected Result: The import operation must fail.
     */
    @Test(expected = DotDataException.class)
    public void Test_UpgradeTask_Expect_Failure_Due_To_Invalid_Site()
            throws DotDataException, DotSecurityException, AlreadyExistException, IOException {
        final Host site = new SiteDataGen().nextPersisted();
        final AppDescriptor descriptor = genAppDescriptor();
        destroySecretsStore();
        final Key key = AppsUtil.generateKey(AppsUtil.loadPass(null));
        final File exportFile = createSecretsAndExportThem(descriptor, key, ImmutableSet.of(site))
                .toFile();

        final HostAPI hostAPI = APILocator.getHostAPI();
        hostAPI.archive(site, admin, false);
        hostAPI.delete(site, admin, false);

        final Path serverDir = AppDescriptorHelper.getServerDirectory();

        final File fileToImport = new File(serverDir.toString(), "dotSecrets-import.xxx");
        //Task will match any file matching this name followed by any extension
        exportFile.renameTo(fileToImport);
        destroySecretsStore();

        final Task00050LoadAppsSecrets task = new Task00050LoadAppsSecrets();
        Assert.assertTrue(task.forceRun());
        task.executeUpgrade();
        Assert.assertEquals(1, task.getImportCount());
        final AppsAPI api = APILocator.getAppsAPI();
        final Optional<AppSecrets> secrets = api.getSecrets(descriptor.getKey(), site, admin);
        Assert.assertTrue(secrets.isPresent());
        //finally test file got removed.
        Assert.assertFalse(fileToImport.exists());
    }

    /**
     * Given scenario: We generate an export file that has a secret associated to an app-descriptor.
     * Then we remove the app-descriptor and attempt a reimport Expected Result: The import must
     * fail due to the no longer existing descriptor.
     */
    @Test(expected = DotDataException.class)
    public void Test_UpgradeTask_Expect_Failure_Due_To_Invalid_Descriptor()
            throws DotDataException, DotSecurityException, AlreadyExistException, IOException {
        final Host site = new SiteDataGen().nextPersisted();
        final AppDescriptor descriptor = genAppDescriptor();
        destroySecretsStore();
        final Key key = AppsUtil.generateKey(AppsUtil.loadPass(null));
        final File exportFile = createSecretsAndExportThem(descriptor, key, ImmutableSet.of(site))
                .toFile();

        api.removeApp(descriptor.getKey(), admin, true);

        final Path serverDir = AppDescriptorHelper.getServerDirectory();

        final File fileToImport = new File(serverDir.toString(), "dotSecrets-import.xxx");
        //Task will match any file matching this name followed by any extension
        exportFile.renameTo(fileToImport);
        destroySecretsStore();

        final Task00050LoadAppsSecrets task = new Task00050LoadAppsSecrets();
        Assert.assertTrue(task.forceRun());
        task.executeUpgrade();
        Assert.assertEquals(1, task.getImportCount());
        final AppsAPI api = APILocator.getAppsAPI();
        final Optional<AppSecrets> secrets = api.getSecrets(descriptor.getKey(), site, admin);
        Assert.assertTrue(secrets.isPresent());
        //finally test file got removed.
        Assert.assertFalse(fileToImport.exists());
    }

    /**
     * Given scenario: We remove the default password property
     * Expected Results: The Task must not run.
     */
    @Test
    public void Test_UpgradeTask_Should_Not_Run_When_No_DefaultPassword_isSet(){
        final String stringProperty = Config.getStringProperty(APPS_IMPORT_EXPORT_DEFAULT_PASSWORD);
       try{
           Config.setProperty(APPS_IMPORT_EXPORT_DEFAULT_PASSWORD, BLANK);
           final Task00050LoadAppsSecrets task = new Task00050LoadAppsSecrets();
           Assert.assertFalse(task.forceRun());
       }finally {
           Config.setProperty(APPS_IMPORT_EXPORT_DEFAULT_PASSWORD, stringProperty);
       }
    }

}
