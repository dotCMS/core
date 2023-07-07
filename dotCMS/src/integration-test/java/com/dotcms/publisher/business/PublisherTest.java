package com.dotcms.publisher.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.enterprise.publishing.PublishDateUpdater;
import com.dotcms.enterprise.publishing.remote.bundler.ContentBundler;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.receiver.BundlePublisher;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.PublisherAPIImplTest;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.io.IOException;
import java.util.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Test the PushedAssetsAPI
 */
@RunWith(DataProviderRunner.class)
public class PublisherTest extends IntegrationTestBase {


    private static final String TEST_DESCRIPTION = "testDescription";
    private static final String TEST_TITLE = "testTitle";

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        createFilter();

        when(Config.CONTEXT.getAttribute(Globals.MESSAGES_KEY))
                .thenReturn(new MultiMessageResources( MultiMessageResourcesFactory.createFactory(),""));

    }

    /**
     * This method test the deletePushedAssetsByEnvironment method
     * 1) Creates a Folder and page
     * 2) Push the Page
     * 3) Remote remove of the folder
     * 4) Adds a new page to the folder
     * 5) Push the page
     */
    @Test
    public void test_Create_folderpage_Push_page_Remove_folderpush_Create_page_Push_page_SameBundle_shouldbe_created()
            throws Exception {

        User adminUser              = null;
        PPBean ppBean               = null;
        FolderPage folderPage       = null;
        PushResult    pushResult    = null;

        try {

            adminUser     = APILocator.getUserAPI().loadByUserByEmail("admin@dotcms.com", APILocator.getUserAPI().getSystemUser(), false);
            folderPage    = this.createFolderPage("testFolderPublisher"+System.currentTimeMillis(), adminUser);
            ppBean        = this.createPushPublishEnv(adminUser);
            this.assertPPBean(ppBean);
            pushResult    = this.pushFolderPage("folderPage1Test", folderPage, adminUser, ppBean);
            this.assertPushFolderPage(folderPage, pushResult);
            pushResult    = this.removePushFolder("removeFolderPage1Test", folderPage, adminUser, ppBean);
            this.assertRemoveFolder(folderPage, pushResult);
            folderPage    = this.createNewPage(folderPage, adminUser);
            pushResult    = this.pushFolderPage("folderPage2Test", folderPage, adminUser, ppBean);
            this.assertPushFolderPage(folderPage, pushResult);
        } finally {

            if (null != ppBean) {
                PublisherTestUtil.cleanBundleEndpointEnv(null, ppBean.endPoint, ppBean.environment);
            }
        }
    }

    /**
     * Test the push of a content with a unique field
     * Method to test: {@link com.dotcms.publisher.receiver.BundlePublisher#process(PublishStatus)}
     * When:
     * - Create a ContentType with a unique field
     * - Create a {@link Contentlet} with a value for the unique field and add it to a bundle
     * - Delete the {@link Contentlet} from the previous step
     * - Create a new {@link Contentlet} with a different value for the unique field
     * - Push publish the bundle
     * Should: The {@link Contentlet} from the bundle with the unique field should replace
     * the {@link Contentlet} version with a different value for the unique field
     */
    @Test
    public void testPushContentWithUniqueField() throws Exception {

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        ContentType testContentType = null;
        PPBean ppBean = null;
        Contentlet resultContentlet = null;

        final String uniqueValue = "\"A+” Student";
        final String replaceValue = "Replaced value";

        try {

            // Create test content type
            testContentType = createContentType("Test Content Type", systemUser, true);

            // Create test environment and endpoint
            final User adminUser = APILocator.getUserAPI().loadByUserByEmail("admin@dotcms.com",
                    systemUser, false);
            ppBean = createPushPublishEnv(adminUser);
            assertPPBean(ppBean);

            // Generate bundle
            final Contentlet contentlet = new ContentletDataGen(testContentType.id())
                    .setProperty(TEST_TITLE, uniqueValue)
                    .setProperty(TEST_DESCRIPTION, replaceValue).nextPersisted();
            final Map<String, Object> bundleData = generateContentBundle(
                    "unique-content-test-1", contentlet, adminUser, ppBean);
            assertNotNull(bundleData);
            assertNotNull(bundleData.get(PublisherTestUtil.FILE));

            // Test content to be replaced using unique field match
            APILocator.getContentletAPI().destroy(contentlet, adminUser, false );

            final Contentlet contentToReplace = new ContentletDataGen(testContentType.id())
                    .setProperty(TEST_TITLE, uniqueValue)
                    .setProperty(TEST_DESCRIPTION, "Other value").nextPersisted();

            // Publish bundle
            final PublisherConfig publisherConfig = publishContentBundle(
                    (File) bundleData.get(PublisherTestUtil.FILE), ppBean.endPoint);
            assertNotNull(publisherConfig);
            assertEquals(((File) bundleData.get(PublisherTestUtil.FILE)).getName(),
                    publisherConfig.getId());

            // Check result content
            resultContentlet = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(contentToReplace.getIdentifier());
            assertNotNull(resultContentlet);
            assertEquals(replaceValue, resultContentlet.getStringProperty(TEST_DESCRIPTION));

        } finally {

            deleteTestPPData(systemUser, testContentType, ppBean, resultContentlet);

        }

    }

    /**
     * Test the push of a multi-language content, with an archived version
     * Method to test: {@link com.dotcms.publisher.receiver.BundlePublisher#process(PublishStatus)}
     * When:
     * - Create a ContentType with title and description fields
     * - Create a {@link Contentlet} with the default language and publish it
     * - Create a {@link Contentlet} with a different language and archive it
     * - Push publish the content
     * Should: The {@link Contentlet} version with the default language should be in live state
     * and the {@link Contentlet} version with a different language should be archived
     */
    @Test
    public void testPushArchivedAndMultiLanguageContent() throws Exception {

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        ContentType testContentType = null;
        PPBean ppBean = null;
        Contentlet resultContentlet = null;

        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final Language spanishLanguage = TestDataUtils.getSpanishLanguage();

        final String defaultLangTitle = "Default Title";
        final String defaultLangDescription = "Default Description";
        final String spanishLangTitle = "Spanish Title";
        final String spanishLangDescription = "Spanish Description";

        try {

            // Create test content type
            testContentType = createContentType("Test Content Type", systemUser, false);

            // Create test environment and endpoint
            final User adminUser = APILocator.getUserAPI().loadByUserByEmail("admin@dotcms.com",
                    systemUser, false);
            ppBean = createPushPublishEnv(adminUser);
            assertPPBean(ppBean);

            // Generate bundle
            final Contentlet contentlet = new ContentletDataGen(testContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setProperty(TEST_TITLE, defaultLangTitle)
                    .setProperty(TEST_DESCRIPTION, defaultLangDescription).nextPersisted();
            ContentletDataGen.publish(contentlet);

            final Contentlet spanishCotentlet = ContentletDataGen.checkout(contentlet);
            spanishCotentlet.setLanguageId(spanishLanguage.getId());
            spanishCotentlet.setProperty(TEST_TITLE, spanishLangTitle);
            spanishCotentlet.setProperty(TEST_DESCRIPTION, spanishLangDescription);
            ContentletDataGen.checkin(spanishCotentlet);
            ContentletDataGen.archive(spanishCotentlet);

            final Map<String, Object> bundleData = generateContentBundle(
                    "archived-multi-language-content-test-1", contentlet, adminUser, ppBean);
            assertNotNull(bundleData);
            assertNotNull(bundleData.get(PublisherTestUtil.FILE));

            final String contentIdentifier = contentlet.getIdentifier();
            APILocator.getContentletAPI().destroy(contentlet, adminUser, false );

            // Publish bundle
            final PublisherConfig publisherConfig = publishContentBundle(
                    (File) bundleData.get(PublisherTestUtil.FILE), ppBean.endPoint);
            assertNotNull(publisherConfig);
            assertEquals(((File) bundleData.get(PublisherTestUtil.FILE)).getName(),
                    publisherConfig.getId());

            // Check result content
            resultContentlet = APILocator.getContentletAPI()
                    .findContentletByIdentifier(contentIdentifier,
                            false, spanishLanguage.getId(), adminUser, false);
            assertNotNull(resultContentlet);
            assertEquals(spanishLangTitle, resultContentlet.getStringProperty(TEST_TITLE));
            assertTrue(resultContentlet.isArchived());

            resultContentlet = APILocator.getContentletAPI()
                    .findContentletByIdentifier(contentIdentifier,
                            true, defaultLanguage.getId(), adminUser, false);
            assertNotNull(resultContentlet);
            assertEquals(defaultLangTitle, resultContentlet.getStringProperty(TEST_TITLE));

        } finally {

            deleteTestPPData(systemUser, testContentType, ppBean, resultContentlet);

        }
    }

    /**
     * Method to test: {@link com.dotcms.enterprise.publishing.PublishDateUpdater#updatePublishExpireDates(Date)}
     * When:
     * - Create a ContentType with publish date field
     * - Create a {@link Contentlet} withput a publish date set
     * Should: The {@link Contentlet} shouldn't be returned by the query for auto published content
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void dontAutoPublishContentWithoutPublishDate() throws DotDataException, DotSecurityException {

        // create a content type with a publish date field
        final Field publishField = new FieldDataGen().defaultValue(null)
                .type(DateTimeField.class).next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(publishField)
                .publishDateFieldVarName(publishField.variable())
                .nextPersisted();

        Contentlet contentlet = null;

        try {

            // create a contentlet without a publish date
            contentlet = new ContentletDataGen(contentType)
                    .nextPersisted();

            // query content to be auto published
            final Date versionTs = APILocator.getVersionableAPI()
                    .getVersionInfo(contentlet.getIdentifier()).getVersionTs();
            final Date triggerDate = Date.from(versionTs.toInstant().plusSeconds(60));

            final String queryContentToPublish = PublishDateUpdater.getPublishLuceneQuery(
                    triggerDate, CollectionsUtils.list(contentType.variable()));
            final List<Contentlet> contentToPublish = APILocator.getContentletAPI().search(
                    queryContentToPublish, 0, 0, null, APILocator.systemUser(), false);

            // check that the content was not included in the auto publish query results
            assertTrue(contentToPublish.isEmpty());

        } finally {

            ContentletDataGen.remove(contentlet);
            ContentTypeDataGen.remove(contentType);

        }
    }

    private FolderPage createNewPage (final  FolderPage folderPage, final User user) throws Exception {

        final HTMLPageAsset page = PublisherTestUtil.createPage(folderPage.folder, user);

        return new FolderPage(folderPage.folder, page);
    }

    private void assertPPBean(final PPBean ppBean) {
        assertNotNull(ppBean);
        assertNotNull(ppBean.environment);
        assertNotNull(ppBean.endPoint);
    }

    private void assertPushFolderPage(final FolderPage folderPage, final PushResult pushResult) throws Exception {

        final User sysuser = APILocator.getUserAPI().getSystemUser();
        final Host host    = APILocator.getHostAPI().findDefaultHost(sysuser, false);
        final PublishStatus status                  = pushResult.publishStatus;
        final Optional<BundlerStatus> bundlerStatus = PublisherTestUtil.getBundleStatus(status.getBundlerStatuses(), ContentBundler.class);

        assertTrue(bundlerStatus.isPresent());
        assertTrue("We should have 1 page on: " + folderPage.folder, bundlerStatus.get().getCount() != 0);

        File tarGzipFile = new File(pushResult.bundlePath + ".tar.gz");
        assertTrue(tarGzipFile.exists());

        PublisherAPIImplTest.extractTarArchive(tarGzipFile, new File(pushResult.bundlePath));

        assertTrue(PublisherTestUtil.existsFolder(pushResult.bundlePath, folderPage.folder));
        assertTrue(PublisherTestUtil.existsPage(pushResult.bundlePath, host, folderPage.folder, folderPage.page));
    }

    private void assertRemoveFolder(final FolderPage folderPage, final PushResult pushResult) throws Exception {
        File tarGzipFile = new File(pushResult.bundlePath + ".tar.gz");
        PublisherAPIImplTest.extractTarArchive(tarGzipFile, new File(pushResult.bundlePath));
        assertTrue(PublisherTestUtil.existsFolder(pushResult.bundlePath, folderPage.folder));
    }



    private PPBean createPushPublishEnv(final User user) throws DotSecurityException, DotDataException {

        final Environment environment     = PublisherTestUtil.createEnvironment(user);
        final PublishingEndPoint endPoint = PublisherTestUtil.createEndpoint(environment);

        return new PPBean(environment, endPoint);
    }

    private PushResult pushFolderPage(final String bundleName, final FolderPage folderPage, final User user, final PPBean ppBean)
            throws DotDataException, DotPublisherException, InstantiationException, IllegalAccessException, DotSecurityException {

        final PublisherAPI publisherAPI = PublisherAPI.getInstance();
        final Bundle bundle             = PublisherTestUtil.createBundle(bundleName, user, ppBean.environment);
        final String assetRealPath      = Config.getStringProperty("ASSET_REAL_PATH", "test-resources");
        final File tempDir              = new File(assetRealPath + "/bundles/" + System.currentTimeMillis());

        //Creating temp bundle dir
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        final List<PublishQueueElement> assets = PublisherTestUtil.getAssets(bundle, folderPage.folder);

        publisherAPI.addContentsToPublish(
                Arrays.asList(folderPage.page.getIdentifier()),
                bundle.getId(), new Date(), user);

        return
            new PushResult(bundle, PublisherTestUtil.push (assets, bundle, user), assetRealPath + "/bundles/" + bundle.getId());
    }



    private FolderPage createFolderPage (final String folderName, final User user) throws Exception {

        final Folder folder      = PublisherTestUtil.createFolder(folderName);
        final HTMLPageAsset page = PublisherTestUtil.createPage(folder, user);

        return new FolderPage(folder, page);
    }

    private PushResult removePushFolder(final String bundleName, final FolderPage folderPage, final User user, final PPBean ppBean)
            throws DotDataException, DotPublisherException, InstantiationException, IllegalAccessException, DotSecurityException {

        final PublisherAPI publisherAPI = PublisherAPI.getInstance();
        final Bundle bundle             = PublisherTestUtil.createBundle(bundleName, user, ppBean.environment);
        final String assetRealPath      = Config.getStringProperty("ASSET_REAL_PATH", "test-resources");

        final List<PublishQueueElement> assets = PublisherTestUtil.getAssets(bundle, folderPage.folder);

        publisherAPI.addContentsToUnpublish(
                Arrays.asList(folderPage.folder.getIdentifier()),
                bundle.getId(), new Date(), user);

        return
                new PushResult(bundle, PublisherTestUtil.remoteRemove (assets, bundle, user), assetRealPath + "/bundles/" + bundle.getId());
    }

    private ContentType createContentType(final String contentTypeName,
            final User user, final boolean uniqueTitle)
            throws DotSecurityException, DotDataException {

        final long time = System.currentTimeMillis();

        ContentType contentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(contentTypeName + time)
                .description("description" + time).variable("velocityVarNameTesting" + time)
                .owner(user.getUserId()).build();

        contentType = APILocator.getContentTypeAPI(user).save(contentType);

        final Field textField = FieldBuilder.builder(TextField.class).name(TEST_TITLE)
                .variable(TEST_TITLE).contentTypeId(contentType.id()).required(true)
                .listed(true).unique(uniqueTitle).indexed(true).sortOrder(1).readOnly(false)
                .fixed(false).searchable(true).dataType(DataTypes.TEXT).build();

        APILocator.getContentTypeFieldAPI().save(textField, user);

        final Field descField = FieldBuilder.builder(TextField.class).name(TEST_DESCRIPTION)
                .variable(TEST_DESCRIPTION).contentTypeId(contentType.id()).required(false)
                .listed(false).unique(false).indexed(false).sortOrder(2).readOnly(false)
                .fixed(false).searchable(false).dataType(DataTypes.TEXT).build();

        APILocator.getContentTypeFieldAPI().save(descField, user);

        return contentType;

    }

    private Map<String, Object> generateContentBundle(final String bundleName,
            final Contentlet contentlet,
            final User user, final PPBean ppBean)
            throws DotDataException, DotPublisherException, DotPublishingException, DotBundleException, InstantiationException, IOException, IllegalAccessException {

        final PublisherAPI publisherAPI = PublisherAPI.getInstance();
        final Bundle bundle = PublisherTestUtil.createBundle(bundleName, user, ppBean.environment);

        publisherAPI.saveBundleAssets(Arrays.asList(contentlet.getIdentifier()),
                bundle.getId(), user);

        return PublisherTestUtil.generateBundle(bundle.getId(), Operation.PUBLISH);

    }

    private PublisherConfig publishContentBundle(final File bundleFile,
            final PublishingEndPoint endpoint)
            throws DotPublisherException, DotPublishingException {

        final String fileName = bundleFile.getName();
        final String bundleFolder = fileName.substring(0, fileName.indexOf(".tar.gz"));

        final PublishAuditStatus status = PublishAuditAPI
                .getInstance()
                .updateAuditTable(endpoint.getId(), endpoint.getGroupId(), bundleFolder, true);

        final PublisherConfig publisherConfig = new PublisherConfig();
        publisherConfig.setId(fileName);
        publisherConfig.setEndpoint(endpoint.getId());
        publisherConfig.setGroupId(endpoint.getGroupId());
        publisherConfig.setPublishAuditStatus(status);

        final BundlePublisher bundlePublisher = new BundlePublisher();
        bundlePublisher.init(publisherConfig);
        return bundlePublisher.process(null);

    }

    private void deleteTestPPData(User systemUser, ContentType testContentType, PPBean ppBean, Contentlet resultContentlet) throws DotDataException, DotSecurityException {
        if (UtilMethods.isSet(resultContentlet)) {
            APILocator.getContentletAPI().destroy(resultContentlet, systemUser, false );
        }

        if (UtilMethods.isSet(ppBean)) {
            PublisherTestUtil.cleanBundleEndpointEnv(null, ppBean.endPoint, ppBean.environment);
        }

        if (UtilMethods.isSet(testContentType) && UtilMethods.isSet(testContentType.id())) {
            APILocator.getContentTypeAPI(systemUser).delete(testContentType);
        }
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup()
            throws DotDataException, DotSecurityException, InterruptedException, ExecutionException, AlreadyExistException {


    }

    class PushResult {

        final String bundlePath;
        final Bundle bundle;
        final PublishStatus publishStatus;

        public PushResult(final Bundle bundle, final PublishStatus publishStatus, final String bundlePath) {
            this.bundle        = bundle;
            this.publishStatus = publishStatus;
            this.bundlePath    = bundlePath;
        }
    }

    class PPBean {

        final Environment environment;
        final PublishingEndPoint endPoint;

        public PPBean(Environment environment, PublishingEndPoint endPoint) {
            this.environment = environment;
            this.endPoint    = endPoint;
        }
    }

    class FolderPage {

        final Folder folder;
        final HTMLPageAsset page;

        public FolderPage(final Folder folder, final HTMLPageAsset page) {
            this.folder = folder;
            this.page = page;
        }
    }

    private static void createFilter() {
        final Map<String, Object> filtersMap =
                ImmutableMap.of("dependencies", true, "relationships", true);
        final FilterDescriptor filterDescriptor =
                new FilterDescriptor("filterTestAPI.yml", "Filter Test Title", filtersMap, true,
                        "Reviewer,dotcms.org.2789");
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);
    }
}