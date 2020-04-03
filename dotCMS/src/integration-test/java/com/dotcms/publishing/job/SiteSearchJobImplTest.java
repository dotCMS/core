package com.dotcms.publishing.job;

import static com.dotcms.rendering.velocity.directive.ParseContainer.getDotParserContainerUUID;
import static com.dotmarketing.util.Constants.USER_AGENT_DOTCMS_SITESEARCH;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.IndexType;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResults;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.sitesearch.business.SiteSearchAuditAPI;
import com.dotmarketing.sitesearch.model.SiteSearchAudit;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UUIDUtil;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SiteSearchJobImplTest extends IntegrationTestBase {

    static ESIndexAPI esIndexAPI;
    static long defaultLang;
    static SiteSearchAPI siteSearchAPI;
    static SiteSearchAuditAPI siteSearchAuditAPI;
    static ContentletAPI contentletAPI;
    static HostAPI hostAPI;

    private static String contentGenericId;
    private static Host site;
    private static Template template;
    private static Folder folder;
    private static User systemUser;
    private static Container container;

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        systemUser = APILocator.systemUser();

        esIndexAPI = APILocator.getESIndexAPI();
        defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        siteSearchAPI = APILocator.getSiteSearchAPI();
        siteSearchAuditAPI = APILocator.getSiteSearchAuditAPI();
        contentletAPI = APILocator.getContentletAPI();
        hostAPI = APILocator.getHostAPI();

        final List<Host> allHosts = hostAPI.findAll(systemUser, false);
        for (final Host host : allHosts) {
            if (host.isSystemHost() || host.getHostname().startsWith("demo")) {
                continue;
            }
            hostAPI.archive(host, systemUser, false);
            hostAPI.delete(host, systemUser, false);
        }

        site = new SiteDataGen().nextPersisted();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        final ContentType contentGenericType = contentTypeAPI.find("webPageContent");
        contentGenericId = contentGenericType.id();

        final String nameTitle = "myTestContainer" + System.currentTimeMillis();
        container = new ContainerDataGen()
                .withContentType(contentGenericType, "$!{body}")
                .friendlyName(nameTitle)
                .title(nameTitle)
                .nextPersisted();

        PublishFactory.publishAsset(container, systemUser, false, false);
        template = new TemplateDataGen().withContainer(container.getIdentifier()).nextPersisted();

        folder = new FolderDataGen().site(site).nextPersisted();

        PublishFactory.publishAsset(template, systemUser, false, false);

        //Create Contentlet in English
        final Contentlet contentlet1 = new ContentletDataGen(contentGenericId)
                .languageId(defaultLang)
                .folder(folder)
                .host(site)
                .setProperty("title", "content1")
                .setProperty("body", "content1")
                .nextPersisted();

        contentlet1.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentlet1.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentlet1.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet1, systemUser, false);

        final String pageName = "our-page";

        final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder, template).languageId(1)
                .pageURL(pageName)
                .friendlyName(pageName)
                .title(pageName).nextPersisted();
        final String uuid = UUIDGenerator.generateUuid();
        final MultiTree multiTree = new MultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier(), contentlet1.getIdentifier() ,uuid,0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        HTMLPageDataGen.publish(pageEnglishVersion);

    }

    @Test
    public void Test_Non_Incremental_Create_Default_Index_Run_Non_Incrementally_Expect_New_Index_Keep_Alias_And_Default()
            throws DotPublishingException, JobExecutionException, DotDataException, IOException, DotSecurityException {

        final List<String> indicesBeforeTest = siteSearchAPI.listIndices();
        for(final String index:indicesBeforeTest) {
            esIndexAPI.delete(index);
        }
        final String jobId = UUIDUtil.uuid();
        final String alias = "any-alias-"+System.currentTimeMillis();
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(SiteSearchJobImpl.RUN_NOW, Boolean.TRUE.toString());
        //Even though we are requesting it to be incremental it'll run as non incremental because it is a run now type of job.
        jobDataMap.put(SiteSearchJobImpl.INCREMENTAL, Boolean.TRUE.toString());
        jobDataMap.put(SiteSearchJobImpl.INDEX_ALIAS, alias);
        jobDataMap.put(SiteSearchJobImpl.JOB_ID, jobId);
        jobDataMap.put(SiteSearchJobImpl.QUARTZ_JOB_NAME, SiteSearchJobImpl.RUNNING_ONCE_JOB_NAME);
        jobDataMap.put(SiteSearchJobImpl.INCLUDE_EXCLUDE, "all");
        jobDataMap.put(SiteSearchJobImpl.LANG_TO_INDEX, new String[]{Long.toString(defaultLang)});
        jobDataMap.put(SiteSearchJobImpl.INDEX_HOST, site.getIdentifier());

        final JobDetail jobDetail = Mockito.mock(JobDetail.class);
        Mockito.when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        final JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context.getJobDetail()).thenReturn(jobDetail);
        Mockito.when(context.getFireTime()).thenReturn(new Date());
        final SiteSearchJobImpl impl = new SiteSearchJobImpl();
        impl.run(context);

        final List<String> indicesAfterTest = siteSearchAPI.listIndices();
        Assert.assertFalse(indicesAfterTest.isEmpty());
        final String newIndexName = indicesAfterTest.get(0);

        Assert.assertEquals("New index is expected have same Alias", alias, esIndexAPI.getIndexAlias(newIndexName));
        final SiteSearchResults search = siteSearchAPI.search(newIndexName, "*",0, 10);
        Assert.assertTrue(search.getTotalResults() >= 1);
        final List<SiteSearchAudit> recentAudits = siteSearchAuditAPI.findRecentAudits(jobId, 0, 1);
        Assert.assertFalse(recentAudits.isEmpty());
        final SiteSearchAudit siteSearchAudit = recentAudits.get(0);
        Assert.assertEquals(1, siteSearchAudit.getPagesCount());
        Assert.assertFalse(siteSearchAudit.isIncremental()); //did not run incrementally since it was launched as a run-now.
        Assert.assertTrue(siteSearchAPI.isDefaultIndex(newIndexName));
    }


    @Test
    public void Test_Non_Incremental_Create_Default_Index_Create_Second_Index_Run_Non_Incrementally_Expect_Non_Default_New_Index()
            throws DotPublishingException, JobExecutionException, DotDataException, IOException, DotSecurityException {

        List<SiteSearchAudit> recentAudits;
        final SiteSearchJobImpl impl = new SiteSearchJobImpl();

        final List<String> indicesBeforeTest = siteSearchAPI.listIndices();
        for(final String index:indicesBeforeTest) {
            esIndexAPI.delete(index);
        }

        final long timeMillis1 = System.currentTimeMillis();
        final String defaultAlias = IndexType.SITE_SEARCH.getPrefix() + "_alias_" + timeMillis1;
        final String defaultIndexName = IndexType.SITE_SEARCH.getPrefix() + "_" + timeMillis1;
        siteSearchAPI.createSiteSearchIndex(defaultIndexName, defaultAlias, 1);
        siteSearchAPI.activateIndex(defaultIndexName); //Make it default.

        final String jobId1 = UUIDUtil.uuid();

        final JobDataMap jobDataMap1 = new JobDataMap();
        jobDataMap1.put(SiteSearchJobImpl.RUN_NOW, Boolean.TRUE.toString());
        jobDataMap1.put(SiteSearchJobImpl.INCREMENTAL, Boolean.FALSE.toString());
        jobDataMap1.put(SiteSearchJobImpl.INDEX_ALIAS, defaultAlias);
        jobDataMap1.put(SiteSearchJobImpl.JOB_ID, jobId1);
        jobDataMap1.put(SiteSearchJobImpl.QUARTZ_JOB_NAME, SiteSearchJobImpl.RUNNING_ONCE_JOB_NAME);
        jobDataMap1.put(SiteSearchJobImpl.INCLUDE_EXCLUDE, "all");
        jobDataMap1.put(SiteSearchJobImpl.LANG_TO_INDEX, new String[]{Long.toString(defaultLang)});
        jobDataMap1.put(SiteSearchJobImpl.INDEX_HOST, site.getIdentifier());

        final JobDetail jobDetail1 = Mockito.mock(JobDetail.class);
        Mockito.when(jobDetail1.getJobDataMap()).thenReturn(jobDataMap1);
        final JobExecutionContext context1 = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context1.getJobDetail()).thenReturn(jobDetail1);
        Mockito.when(context1.getFireTime()).thenReturn(new Date());

        impl.run(context1);


        final List <String> indices = siteSearchAPI.listIndices();
        Assert.assertFalse(indices.contains(defaultIndexName));

        recentAudits = siteSearchAuditAPI.findRecentAudits(jobId1, 0, 1);

        //Since non-incremental runs will use its own brand new index
        //This is the only trustworthy way to retrieve the index name that was created.
        Assert.assertFalse(recentAudits.isEmpty());
        final String newDefaultIndexName = recentAudits.get(0).getIndexName();
        Assert.assertTrue( siteSearchAPI.isDefaultIndex(newDefaultIndexName));

        //Second index
        final long timeMillis2 = System.currentTimeMillis();
        final String newIndexAlias = IndexType.SITE_SEARCH.getPrefix() + "_alias_" +timeMillis2;
        final String newIndexName =  IndexType.SITE_SEARCH.getPrefix() + "_" +timeMillis2;
        siteSearchAPI.createSiteSearchIndex(newIndexName, newIndexAlias, 1);

        final String jobId2 = UUIDUtil.uuid();

        final JobDataMap jobDataMap2 = new JobDataMap();
        jobDataMap2.put(SiteSearchJobImpl.RUN_NOW, Boolean.TRUE.toString());
        jobDataMap2.put(SiteSearchJobImpl.INCREMENTAL, Boolean.FALSE.toString());
        jobDataMap2.put(SiteSearchJobImpl.INDEX_ALIAS, newIndexAlias);
        jobDataMap2.put(SiteSearchJobImpl.JOB_ID, jobId2);
        jobDataMap2.put(SiteSearchJobImpl.QUARTZ_JOB_NAME, SiteSearchJobImpl.RUNNING_ONCE_JOB_NAME);
        jobDataMap2.put(SiteSearchJobImpl.INCLUDE_EXCLUDE, "all");
        jobDataMap2.put(SiteSearchJobImpl.LANG_TO_INDEX, new String[]{Long.toString(defaultLang)});
        jobDataMap2.put(SiteSearchJobImpl.INDEX_HOST, site.getIdentifier());

        final JobDetail jobDetail2 = Mockito.mock(JobDetail.class);
        Mockito.when(jobDetail2.getJobDataMap()).thenReturn(jobDataMap2);
        final JobExecutionContext context2 = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context2.getJobDetail()).thenReturn(jobDetail2);
        Mockito.when(context2.getFireTime()).thenReturn(new Date());

        impl.run(context2);
        //Second run will create the another index. But the first one should remain as default.

        //Check the original index is still the default
        Assert.assertTrue(siteSearchAPI.isDefaultIndex(newDefaultIndexName));

        recentAudits = siteSearchAuditAPI.findRecentAudits(jobId2, 0, 1);
        Assert.assertFalse(recentAudits.isEmpty());
        final SiteSearchAudit siteSearchAudit = recentAudits.get(0);

        final SiteSearchResults search = siteSearchAPI.search(siteSearchAudit.getIndexName(), "*",0, 10);
        Assert.assertTrue(search.getTotalResults() >= 1);

    }

    @Test
    public void Test_Incremental_Job_Test_Pages_Are_Found_Create_And_Publish_New_Page_Test_Changes_Are_Picked_Unpublish_Then_Verify_Page_Is_Gone()
    throws DotPublishingException, JobExecutionException, DotDataException, IOException, DotSecurityException {

        List<SiteSearchAudit> recentAudits;

        final String jobName = "Any-Scheduled-Job-" + System.currentTimeMillis();

        final List<String> indicesBeforeTest = siteSearchAPI.listIndices();
        for(final String index:indicesBeforeTest) {
            esIndexAPI.delete(index);
        }

        final long timeMillis1 = System.currentTimeMillis();
        final String newIndexAlias = IndexType.SITE_SEARCH.getPrefix() + "_alias_" + timeMillis1;
        final String jobId = UUIDUtil.uuid();
        // As an incremental scheduled type of job.
        // Every single run has to be tied to the same job id.

        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(SiteSearchJobImpl.RUN_NOW, Boolean.FALSE.toString());
        jobDataMap.put(SiteSearchJobImpl.INCREMENTAL, Boolean.TRUE.toString());
        jobDataMap.put(SiteSearchJobImpl.INDEX_ALIAS, newIndexAlias);
        jobDataMap.put(SiteSearchJobImpl.JOB_ID, jobId);
        jobDataMap.put(SiteSearchJobImpl.QUARTZ_JOB_NAME, jobName);
        jobDataMap.put(SiteSearchJobImpl.INCLUDE_EXCLUDE, "all");
        jobDataMap.put(SiteSearchJobImpl.LANG_TO_INDEX, new String[]{Long.toString(defaultLang)});
        jobDataMap.put(SiteSearchJobImpl.INDEX_HOST, site.getIdentifier());

        final JobDetail jobDetail1 = Mockito.mock(JobDetail.class);
        Mockito.when(jobDetail1.getJobDataMap()).thenReturn(jobDataMap);
        final JobExecutionContext context1 = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context1.getJobDetail()).thenReturn(jobDetail1);
        Mockito.when(context1.getFireTime()).thenReturn(new Date());

        final SiteSearchJobImpl firstRunJob = new SiteSearchJobImpl();
        firstRunJob.run(context1);

        String generatedBundleId1 = firstRunJob.getBundleId();
        Assert.assertNotNull(generatedBundleId1);

        recentAudits = siteSearchAuditAPI.findRecentAudits(jobId, 0, 1);
        Assert.assertFalse(recentAudits.isEmpty());
        final SiteSearchAudit siteSearchAudit = recentAudits.get(0);

        final SiteSearchResults search1 = siteSearchAPI.search(siteSearchAudit.getIndexName(), "our-page*",0, 10);
        Assert.assertTrue(search1.getTotalResults() >= 1);

        final File runOnceBundleRoot = BundlerUtil.getBundleRoot(generatedBundleId1, false);
        Assert.assertTrue(runOnceBundleRoot.exists());

        // At this point we just finished dumping our content into a brand new index through a run once type of job.
        // From now on we will have to create an incremental job and continuously feed it.
        // The first time it'll have to create the whole thing since it's the first incremental run.

        final String incrementalJobBundleId = StringUtils.camelCaseLower(jobName);

        final JobDetail jobDetail2 = Mockito.mock(JobDetail.class);
        Mockito.when(jobDetail2.getJobDataMap()).thenReturn(jobDataMap);
        final JobExecutionContext context2 = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context2.getJobDetail()).thenReturn(jobDetail2);
        Mockito.when(context2.getFireTime()).thenReturn(new Date());

        final SiteSearchJobImpl secondRunJob = new SiteSearchJobImpl();
        secondRunJob.run(context2);

        generatedBundleId1 = secondRunJob.getBundleId();
        Assert.assertEquals(incrementalJobBundleId, generatedBundleId1);

        final File incrementalBundleRoot1 = BundlerUtil.getBundleRoot(generatedBundleId1, false);
        Assert.assertTrue(incrementalBundleRoot1.exists());

        final List<File> firstRunFiles = FileUtil.listFilesRecursively(incrementalBundleRoot1);
        Assert.assertFalse(firstRunFiles.isEmpty());

        recentAudits = siteSearchAuditAPI.findRecentAudits(jobId, 0, 1);
        Assert.assertFalse(recentAudits.isEmpty());
        final SiteSearchAudit siteSearchAudit2 = recentAudits.get(0);

        final SiteSearchResults search2 = siteSearchAPI.search(siteSearchAudit2.getIndexName(), "our-page*",0, 10);
        Assert.assertTrue(search2.getTotalResults() >= 1);

        //Create Contentlet in English
        final Contentlet contentlet1 = new ContentletDataGen(contentGenericId)
                .languageId(defaultLang)
                .folder(folder)
                .host(site)
                .setProperty("title", "content1")
                .setProperty("body", "content1")
                .nextPersisted();

        contentlet1.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentlet1.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentlet1.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet1, systemUser, false);

        final String pageName = RandomStringUtils.randomAlphabetic(20);

        final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder, template).languageId(defaultLang)
                .pageURL(pageName)
                .friendlyName(pageName)
                .title(pageName).nextPersisted();
        final String uuid = UUIDGenerator.generateUuid();
        final MultiTree multiTree = new MultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier(), contentlet1.getIdentifier() ,uuid,0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);
        HTMLPageDataGen.publish(pageEnglishVersion);

        //Next Run should run completely incrementally
        final JobExecutionContext incrementalContext2 = Mockito.mock(JobExecutionContext.class);
        Mockito.when(incrementalContext2.getJobDetail()).thenReturn(jobDetail2);
        Mockito.when(incrementalContext2.getFireTime()).thenReturn(new Date());

        final SiteSearchJobImpl thirdRunJob = new SiteSearchJobImpl();
        thirdRunJob.run(incrementalContext2);

        recentAudits = siteSearchAuditAPI.findRecentAudits(jobId, 0, 1);
        Assert.assertFalse(recentAudits.isEmpty());
        final SiteSearchAudit siteSearchAudit3 = recentAudits.get(0);

        //Now we make sure the page is now part of Site-search search results.
        final SiteSearchResults search3 = siteSearchAPI.search(siteSearchAudit3.getIndexName(), pageName,0, 10);
        Assert.assertEquals(1, search3.getTotalResults());

        final String generatedBundleId2 = thirdRunJob.getBundleId();

        Assert.assertEquals(generatedBundleId1, generatedBundleId2);

        final File incrementalBundleRoot2 = BundlerUtil.getBundleRoot(generatedBundleId2, false);
        Assert.assertTrue(incrementalBundleRoot1.exists());

        final List<File> secondRunFiles = FileUtil.listFilesRecursively(incrementalBundleRoot2);

        secondRunFiles.removeAll(firstRunFiles);

        final List<File> secondRunFilteredFiles = secondRunFiles.stream().filter(file -> !file.isDirectory())
                .collect(Collectors.toList());

        //This tests that only the files related to the new-page created by this test are the `new files` listed.
        Assert.assertTrue(secondRunFilteredFiles.stream().allMatch(file -> file.getName().contains(pageName)));

        //And now for my last trick I'm gonna un-publish the page that I just verified appears in the search-results.
        //Then re-run the job and make sure it's gone.

        DateUtil.sleep(6000L); //This sleep is freaking important. It allows a wider time difference between the last start-date. And the file asset last modified timestamp.

        HTMLPageDataGen.unpublish(pageEnglishVersion);
        //Next Run should run completely incrementally
        final JobExecutionContext incrementalContext3 = Mockito.mock(JobExecutionContext.class);
        Mockito.when(incrementalContext3.getJobDetail()).thenReturn(jobDetail2);
        Mockito.when(incrementalContext3.getFireTime()).thenReturn(new Date());

        final SiteSearchJobImpl fourthRunJob = new SiteSearchJobImpl();
        fourthRunJob.run(incrementalContext3);

        recentAudits = siteSearchAuditAPI.findRecentAudits(jobId, 0, 1);
        Assert.assertFalse(recentAudits.isEmpty());
        final SiteSearchAudit siteSearchAudit4 = recentAudits.get(0);
        //Now we make sure the page is Not part of Site-search search results.
        final SiteSearchResults search4 = siteSearchAPI.search(siteSearchAudit4.getIndexName(), pageName,0, 10);
        Assert.assertEquals(0, search4.getTotalResults());
        //Ta da!!!

    }

    /**
     * Given sceneario: Multi-language content referenced from a page. Create a Site-Search run including all the languages of the content and
     * check the two versions made it into the resulting index.
     */

    @Test
    public void test_MultilangContent_IndexingAllLanguages()
            throws DotPublishingException, JobExecutionException, DotDataException, IOException, DotSecurityException, WebAssetException {

        boolean defaultContentToDefaultLangOriginalValue =
                Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false);

        try {

            Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", true);

            final Host site = new SiteDataGen().nextPersisted();
            Language lang1 = APILocator.getLanguageAPI().getDefaultLanguage();
            Language lang2 = new LanguageDataGen().nextPersisted();
            folder = new FolderDataGen().site(site).nextPersisted();

            Contentlet contentletLang1 = TestDataUtils
                    .getEmployeeContent(true, lang1.getId(), null, site);

            contentletLang1 = contentletAPI.find(contentletLang1.getInode(), systemUser, false);
            contentletLang1.setStringProperty("firstName", "catherine");
            contentletLang1 = contentletAPI.checkin(contentletLang1, systemUser, false);

            ContentletDataGen.publish(contentletLang1);

            Contentlet contentletLang2 = contentletAPI
                    .find(contentletLang1.getInode(), systemUser, false);
            contentletLang2.setInode("");
            contentletLang2.setLanguageId(lang2.getId());
            contentletLang2.setStringProperty("firstName", "catalina");
            contentletLang2 = contentletAPI.checkin(contentletLang2, systemUser, false);

            ContentletDataGen.publish(contentletLang2);

            final Container container = new ContainerDataGen().withContentType(contentletLang1
                    .getContentType(), "$!{firstName}").nextPersisted();

            ContainerDataGen.publish(container);

            final String uuid = UUIDGenerator.generateUuid();

            final Template template = new TemplateDataGen()
                    .withContainer(container.getIdentifier(), uuid)
                    .nextPersisted();

            TemplateDataGen.publish(template);

            HTMLPageAsset page = new HTMLPageDataGen(folder, template).languageId(lang1.getId())
                    .nextPersisted();

            HTMLPageDataGen.publish(page);

            final MultiTree multiTree = new MultiTree(page.getIdentifier(),
                    container.getIdentifier(),
                    contentletLang1.getIdentifier(), getDotParserContainerUUID(uuid), 0);

            APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

            final String html = APILocator.getHTMLPageAssetAPI().getHTML(page.getURI(), site, true,
                    contentletLang1.getIdentifier(), APILocator.systemUser(),
                    contentletLang1.getLanguageId(), USER_AGENT_DOTCMS_SITESEARCH);

            Assert.assertFalse(html.isEmpty());

            final List<String> indicesBeforeTest = siteSearchAPI.listIndices();
            for (final String index : indicesBeforeTest) {
                esIndexAPI.delete(index);
            }
            final String jobId = UUIDUtil.uuid();
            final String alias = "any-alias-" + System.currentTimeMillis();
            final JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(SiteSearchJobImpl.RUN_NOW, Boolean.TRUE.toString());
            jobDataMap.put(SiteSearchJobImpl.INCREMENTAL, Boolean.FALSE.toString());
            jobDataMap.put(SiteSearchJobImpl.INDEX_ALIAS, alias);
            jobDataMap.put(SiteSearchJobImpl.JOB_ID, jobId);
            jobDataMap.put(SiteSearchJobImpl.QUARTZ_JOB_NAME,
                    SiteSearchJobImpl.RUNNING_ONCE_JOB_NAME);
            jobDataMap.put(SiteSearchJobImpl.INCLUDE_EXCLUDE, "all");
            jobDataMap
                    .put(SiteSearchJobImpl.LANG_TO_INDEX, new String[]{Long.toString(lang1.getId()),
                            Long.toString(lang2.getId())});
            jobDataMap.put(SiteSearchJobImpl.INDEX_HOST, site.getIdentifier());

            final JobDetail jobDetail = Mockito.mock(JobDetail.class);
            Mockito.when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
            final JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
            Mockito.when(context.getJobDetail()).thenReturn(jobDetail);
            Mockito.when(context.getFireTime()).thenReturn(new Date());
            final SiteSearchJobImpl impl = new SiteSearchJobImpl();
            impl.run(context);

            final List<String> indicesAfterTest = siteSearchAPI.listIndices();
            Assert.assertFalse(indicesAfterTest.isEmpty());
            final String newIndexName = indicesAfterTest.get(0);

            SiteSearchResults searchResults = siteSearchAPI.search(newIndexName, "catalina", 0, 10);
            Assert.assertTrue(searchResults.getTotalResults() >= 1);

            searchResults = siteSearchAPI.search(newIndexName, "catherine", 0, 10);
            Assert.assertTrue(searchResults.getTotalResults() >= 1);

        } finally {
            Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE",
                    defaultContentToDefaultLangOriginalValue);
        }
    }


}
