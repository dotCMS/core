package com.dotcms.publisher.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.dependencies.DependencyManager;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherAPIImplTest;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import java.util.Date;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import java.util.List;

/**
 * @author nollymar
 */
public class DependencyManagerTest {

    private static long languageId;
    private static User user;
    private static BundleAPI bundleAPI;
    private static ContentletAPI contentletAPI;
    private static FieldAPI contentTypeFieldAPI;
    private static RelationshipAPI relationshipAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        user = APILocator.systemUser();

        bundleAPI = APILocator.getBundleAPI();
        contentletAPI = APILocator.getContentletAPI();
        contentTypeFieldAPI = APILocator.getContentTypeFieldAPI();
        relationshipAPI = APILocator.getRelationshipAPI();

        createFilter();

    }

    /**
     * <b>Method to test:</b> {@link DependencyManager#setDependencies()} <p>
     * <b>Given Scenario:</b> A bundle contains a content with a self-join relationship <p>
     * <b>ExpectedResult:</b> The Dependency Manager should include all related content to the bundle
     */
    @Test
    public void test_dependencyManager_shouldIncludeSelfRelationships()
            throws DotSecurityException, DotBundleException, DotDataException {
        final PushPublisherConfig config = new PushPublisherConfig();
        final ContentType contentType = getContentTypeWithSelfJoinRelationship();
        final ContentletDataGen dataGen = new ContentletDataGen(contentType.id());

        //Creates parent content
        Contentlet blogContentParent = dataGen
                .languageId(languageId)
                .setProperty("title", "blogContentParent")
                .setProperty("urlTitle", "blogContentParent")
                .setProperty("author", "systemUser")
                .setProperty("sysPublishDate", new Date())
                .setProperty("body", "blogBody").next();

        //Creates child content
        final Contentlet blogContentChild = dataGen
                .languageId(languageId)
                .setProperty("title", "blogContentChild")
                .setProperty("urlTitle", "blogContentChild")
                .setProperty("author", "systemUser")
                .setProperty("sysPublishDate", new Date())
                .setProperty("body", "blogBody").nextPersisted();

        //Adds a new relationship between both contentlets
        final Relationship relationship = relationshipAPI.byContentType(contentType).get(0);

        ContentletRelationships contentletRelationships = new ContentletRelationships(
                blogContentParent);
        ContentletRelationshipRecords records = contentletRelationships.new ContentletRelationshipRecords(
                relationship, true);
        records.setRecords(Lists.newArrayList(blogContentChild));
        contentletRelationships.getRelationshipsRecords().add(records);
        blogContentParent = contentletAPI.checkin(blogContentParent, contentletRelationships, null, null, user, false);

        //Creates a bundle with just the child
        createBundle(config, blogContentChild);

        DependencyManager dependencyManager = new DependencyManager(user, config);
        dependencyManager.setDependencies();

        //The dependecy manager should include parent and child contentlets in the bundle
        validateDependencies(blogContentParent, blogContentChild, relationship, dependencyManager);

        //Creates a bundle with just the parent
        createBundle(config, blogContentParent);

        dependencyManager = new DependencyManager(user, config);
        dependencyManager.setDependencies();

        //The dependency manager should include parent and child contentlets in the bundle
        validateDependencies(blogContentParent, blogContentChild, relationship, dependencyManager);

    }

    /**
     * <b>Method to test:</b> {@link DependencyManager#setDependencies()} <p>
     * <b>Given Scenario:</b> A bundle contains a content with a relationship <p>
     * <b>ExpectedResult:</b> The Dependency Manager should include all related content to the bundle
     * @throws DotSecurityException
     * @throws DotBundleException
     * @throws DotDataException
     */
    @Test
    public void test_dependencyManager_shouldIncludeRelationships()
            throws DotSecurityException, DotBundleException, DotDataException {

        final PushPublisherConfig config = new PushPublisherConfig();

        final ContentType blogContentType = TestDataUtils.getBlogLikeContentType();
        final ContentType commentContentType = TestDataUtils.getCommentsLikeContentType();

        final Field field = FieldBuilder.builder(RelationshipField.class)
                .name(commentContentType.variable())
                .contentTypeId(blogContentType.id())
                .values(String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .relationType(commentContentType.variable()).required(false).build();

        contentTypeFieldAPI.save(field, user);

        //Creates parent content
        Contentlet blogContentParent = new ContentletDataGen(blogContentType.id())
                .languageId(languageId)
                .setProperty("title", "blogContentParent")
                .setProperty("urlTitle", "blogContentParent")
                .setProperty("author", "systemUser")
                .setProperty("sysPublishDate", new Date())
                .setProperty("body", "blogBody").next();

        //Creates child content
        final Contentlet commentContentChild = new ContentletDataGen(commentContentType.id())
                .languageId(languageId)
                .setProperty("title", "commentContentChild")
                .setProperty("urlTitle", "commentContentChild")
                .setProperty("author", "systemUser")
                .setProperty("sysPublishDate", new Date())
                .setProperty("body", "blogBody").nextPersisted();

        //Adds a new relationship between both contentlets
        final Relationship relationship = relationshipAPI.byContentType(blogContentType).get(0);

        ContentletRelationships contentletRelationships = new ContentletRelationships(
                blogContentParent);
        ContentletRelationshipRecords records = contentletRelationships.new ContentletRelationshipRecords(
                relationship, true);
        records.setRecords(Lists.newArrayList(commentContentChild));
        contentletRelationships.getRelationshipsRecords().add(records);
        blogContentParent = contentletAPI.checkin(blogContentParent, contentletRelationships, null, null, user, false);

        //Creates a bundle with just the child
        createBundle(config, commentContentChild);

        DependencyManager dependencyManager = new DependencyManager(user, config);
        dependencyManager.setDependencies();

        //The dependecy manager should include parent and child contentlets in the bundle
        validateDependencies(blogContentParent, commentContentChild, relationship, dependencyManager);

        //Creates a bundle with just the parent
        createBundle(config, blogContentParent);

        dependencyManager = new DependencyManager(user, config);
        dependencyManager.setDependencies();

        //The dependency manager should include parent and child contentlets in the bundle
        validateDependencies(blogContentParent, commentContentChild, relationship, dependencyManager);
   }

    /**
     * <b>Method to test:</b> {@link DependencyManager#setDependencies()} <p>
     * <b>Given Scenario:</b> A {@link ContentType} with a page as detail page that not exist<p>
     * <b>ExpectedResult:</b> Should not throw any exception
     * @throws DotSecurityException
     * @throws DotBundleException
     * @throws DotDataException
     */
    @Test
    public void test_Sending_Page_as_dependencies_with_PUSH_PUBLISHING_PUSH_ALL_FOLDER_PAGES()
            throws DotSecurityException, DotBundleException, DotDataException {

        Config.setProperty("PUSH_PUBLISHING_PUSH_ALL_FOLDER_PAGES", true);

        try {
            final PushPublisherConfig config = new PushPublisherConfig();

            final Host host = new SiteDataGen().nextPersisted();
            final ContentType contentTypeForContent = new ContentTypeDataGen().nextPersisted();

            final Container container = new ContainerDataGen()
                    .withContentType(contentTypeForContent, "Testing")
                    .nextPersisted();

            final Container container_2 = new ContainerDataGen()
                    .nextPersisted();

            final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                    .withContainer(container, ContainerUUID.UUID_START_VALUE)
                    .withContainer(container_2, ContainerUUID.UUID_START_VALUE)
                    .next();

            final Folder folderTheme = new FolderDataGen().nextPersisted();
            final Contentlet theme_1 = new ThemeDataGen()
                    .site(host)
                    .themesFolder(folderTheme)
                    .nextPersisted();

            final Template template_1 = new TemplateDataGen()
                    .drawedBody(templateLayout)
                    .host(host)
                    .theme(theme_1)
                    .nextPersisted();

            final Folder pages_folder = new FolderDataGen().nextPersisted();
            final HTMLPageAsset htmlPageAsset_1 = new HTMLPageDataGen(host, template_1)
                    .folder(pages_folder)
                    .nextPersisted();

            final Contentlet theme_2 = new ThemeDataGen()
                    .site(host)
                    .themesFolder(folderTheme)
                    .nextPersisted();

            final Template template_2 = new TemplateDataGen()
                    .drawedBody(templateLayout)
                    .host(host)
                    .theme(theme_2)
                    .nextPersisted();

            final HTMLPageAsset htmlPageAsset_2 = new HTMLPageDataGen(host, template_2)
                    .folder(pages_folder)
                    .nextPersisted();

            createBundle(config, htmlPageAsset_1);

            DependencyManager dependencyManager = new DependencyManager(DependencyManagerTest.user, config);
            dependencyManager.setDependencies();

            final List<Contentlet> languageVariables = PublisherAPIImplTest.getLanguageVariables();
            assertEquals(languageVariables.size() + 4, dependencyManager.getContents().size());
            assertTrue(dependencyManager.getContents().contains(htmlPageAsset_1.getIdentifier()));
            assertTrue(dependencyManager.getContents().contains(htmlPageAsset_2.getIdentifier()));
            assertTrue(dependencyManager.getContents().contains(theme_1.getIdentifier()));
            assertTrue(dependencyManager.getContents().contains(theme_2.getIdentifier()));

            assertEquals(2, dependencyManager.getTemplates().size());
            assertTrue(dependencyManager.getTemplates().contains(template_1.getIdentifier()));
            assertTrue(dependencyManager.getTemplates().contains(template_2.getIdentifier()));

            assertEquals(2, dependencyManager.getContainers().size());
            assertTrue(dependencyManager.getContainers().contains(container.getIdentifier()));
            assertTrue(dependencyManager.getContainers().contains(container_2.getIdentifier()));

            assertTrue(dependencyManager.getFolders().contains(theme_1.getFolder()));
            assertTrue(dependencyManager.getFolders().contains(theme_2.getFolder()));
        } finally {
            Config.setProperty("PUSH_PUBLISHING_PUSH_ALL_FOLDER_PAGES", false);
        }
    }

    private Contentlet createContentlet(
            final ContentType contentType,
            final Container container,
            final HTMLPageAsset htmlPageAsset) {

        final Contentlet contentlet = new ContentletDataGen(contentType.id()).nextPersisted();
        new MultiTreeDataGen()
                .setPage(htmlPageAsset)
                .setContainer(container)
                .setContentlet(contentlet)
                .nextPersisted();
        return contentlet;
    }
    
    /**
     * <b>Method to test:</b> {@link DependencyManager#setDependencies()} <p>
     * <b>Given Scenario:</b> A Page using a FileContainer and the FileContainer jus has the container.vtl file<p>
     * <b>ExpectedResult:</b> Should include the container.vtl file as dependencies
     * @throws DotSecurityException
     * @throws DotBundleException
     * @throws DotDataException
     */
    @Test
    public void test_Page_with_FileContainer_as_Dependencies()
            throws DotSecurityException, DotBundleException, DotDataException {

        final PushPublisherConfig config = new PushPublisherConfig();

        FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen()
                .ignoreDefaultContentTypes()
                .nextPersisted();

        fileAssetContainer = (FileAssetContainer) APILocator.getContainerAPI()
                .find(fileAssetContainer.getInode(), APILocator.systemUser(), true);

        final Host host = new SiteDataGen().nextPersisted();
        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(fileAssetContainer, ContainerUUID.UUID_START_VALUE)
                .next();

        final Template template = new TemplateDataGen()
                .drawedBody(templateLayout)
                .host(host)
                .nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();

        //Creates a bundle with just the child
        createBundle(config, htmlPageAsset);

        DependencyManager dependencyManager = new DependencyManager(user, config);
        dependencyManager.setDependencies();

//        assertEquals(2, dependencyManager.getContents().size());
        assertTrue(dependencyManager.getContents().contains(htmlPageAsset.getIdentifier()));

        final String path = fileAssetContainer.getPath();
        final Folder rootFolder = APILocator.getFolderAPI()
                .findFolderByPath(path, fileAssetContainer.getHost(), user, false);

        final List<FileAsset> fileAssetsByFolder = APILocator.getFileAssetAPI()
                .findFileAssetsByFolder(rootFolder, APILocator.systemUser(), false);

        for (final FileAsset fileAsset : fileAssetsByFolder) {
            assertTrue(dependencyManager.getContents().contains(fileAsset.getIdentifier()));
        }

    }

    /**
     * <b>Method to test:</b> {@link DependencyManager#setDependencies()} <p>
     * <b>Given Scenario:</b> A Page using a Template as a File Design<p>
     * <b>ExpectedResult:</b> Should include the template files as dependencies
     * @throws DotSecurityException
     * @throws DotBundleException
     * @throws DotDataException
     */
    @Test
    public void test_Page_with_FileTemplateDesign_as_Dependencies()
            throws DotSecurityException, DotBundleException, DotDataException {

        final Host host = new SiteDataGen().nextPersisted();
        final PushPublisherConfig config = new PushPublisherConfig();

        FileAssetTemplate fileAssetTemplate = new TemplateAsFileDataGen().designTemplate(true)
                .host(host).nextPersisted();

        fileAssetTemplate = FileAssetTemplate.class.cast(APILocator.getTemplateAPI()
                .findWorkingTemplate(fileAssetTemplate.getIdentifier(),user,false));

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, fileAssetTemplate).nextPersisted();

        //Creates a bundle with just the page
        createBundle(config, htmlPageAsset);

        DependencyManager dependencyManager = new DependencyManager(user, config);
        dependencyManager.setDependencies();

        final String path = fileAssetTemplate.getPath();
        final Folder rootFolder = APILocator.getFolderAPI()
                .findFolderByPath(path, fileAssetTemplate.getHost(), user, false);

        final List<FileAsset> fileAssetsByFolder = APILocator.getFileAssetAPI()
                .findFileAssetsByFolder(rootFolder, APILocator.systemUser(), false);

        for (final FileAsset fileAsset : fileAssetsByFolder) {
                assertTrue(
                        "fileAsset: " + fileAsset.getIdentifier() + "Contents: " + dependencyManager.getContents().toString(),
                        dependencyManager.getContents().contains(fileAsset.getIdentifier())
                );
        }

    }

    /**
     * <b>Method to test:</b> {@link DependencyManager#setDependencies()} <p>
     * <b>Given Scenario:</b> A Page using a Template as a File Advanced<p>
     * <b>ExpectedResult:</b> Should include the template files as dependencies
     * @throws DotSecurityException
     * @throws DotBundleException
     * @throws DotDataException
     */
    @Test
    public void test_Page_with_FileTemplateAdvanced_as_Dependencies()
            throws DotSecurityException, DotBundleException, DotDataException {

        final Host host = new SiteDataGen().nextPersisted();
        final PushPublisherConfig config = new PushPublisherConfig();

        FileAssetTemplate fileAssetTemplate = new TemplateAsFileDataGen().designTemplate(false)
                .host(host).nextPersisted();

        fileAssetTemplate = FileAssetTemplate.class.cast(APILocator.getTemplateAPI()
                .findWorkingTemplate(fileAssetTemplate.getIdentifier(),user,false));

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, fileAssetTemplate).nextPersisted();

        //Creates a bundle with just the page
        createBundle(config, htmlPageAsset);

        DependencyManager dependencyManager = new DependencyManager(user, config);
        dependencyManager.setDependencies();

        final String path = fileAssetTemplate.getPath();
        final Folder rootFolder = APILocator.getFolderAPI()
                .findFolderByPath(path, fileAssetTemplate.getHost(), user, false);

        final List<FileAsset> fileAssetsByFolder = APILocator.getFileAssetAPI()
                .findFileAssetsByFolder(rootFolder, APILocator.systemUser(), false);

        for (final FileAsset fileAsset : fileAssetsByFolder) {
            assertTrue("fileAsset: " + fileAsset.getIdentifier() + "Contents: " +dependencyManager.getContents().toString(),dependencyManager.getContents().contains(htmlPageAsset.getIdentifier()));
        }

    }

    private void createBundle(final PushPublisherConfig config, final Contentlet contentlet)
            throws DotDataException {
        createBundle(config,contentlet,"");
    }

    /**
     * Creates a bundle with one contentlet
     */
    private void createBundle(final PushPublisherConfig config, final Contentlet contentlet,final String filterKey)
            throws DotDataException {
        final String bundleName = "testDependencyManagerBundle" + System.currentTimeMillis();
        Bundle bundle = new Bundle(bundleName, new Date(), null, user.getUserId(),false,filterKey);
        bundleAPI.saveBundle(bundle);
        bundle = bundleAPI.getBundleByName(bundleName);

        final PublishQueueElement publishQueueElement = new PublishQueueElement();
        publishQueueElement.setId(1);
        publishQueueElement.setOperation(Operation.PUBLISH.ordinal());
        publishQueueElement.setAsset(contentlet.getInode());
        publishQueueElement.setEnteredDate(new Date());
        publishQueueElement.setPublishDate(new Date());
        publishQueueElement.setBundleId(bundle.getId());
        publishQueueElement.setType(PusheableAsset.CONTENTLET.getType());

        config.setAssets(Lists.newArrayList(publishQueueElement));
        config.setId(bundle.getId());
        config.setOperation(Operation.PUBLISH);
        config.setDownloading(true);
        config.setLuceneQueries(Lists.newArrayList("+identifier:" + contentlet.getIdentifier()));
    }

    /**
     * Validates the dependency manager includes relationship and both contentlets
     */
    private void validateDependencies(final Contentlet parentContent, final Contentlet childContent,
            final Relationship relationship, final DependencyManager dependencyManager)
            throws DotSecurityException, DotDataException {
        assertNotNull(dependencyManager.getRelationships());
        assertEquals(1, dependencyManager.getRelationships().size());
        assertEquals(relationship.getInode(),
                dependencyManager.getRelationships().iterator().next());
        assertNotNull(dependencyManager.getContents());


        final List<Contentlet> languageVariables = PublisherAPIImplTest.getLanguageVariables();
        assertEquals(languageVariables.size() + 2, dependencyManager.getContents().size());

        assertTrue(dependencyManager.getContents().contains(parentContent.getIdentifier())
                && dependencyManager.getContents().contains(childContent.getIdentifier()));
    }

    /**
     * Creates a content type with a self-join relationship
     */
    private ContentType getContentTypeWithSelfJoinRelationship()
            throws DotSecurityException, DotDataException {

        final ContentType contentType = TestDataUtils.getBlogLikeContentType();
        final Field field = FieldBuilder.builder(RelationshipField.class)
                .name(contentType.variable())
                .contentTypeId(contentType.id())
                .values(String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .relationType(contentType.variable()).required(false).build();

        contentTypeFieldAPI.save(field, user);
        return contentType;
    }
    
    private static void createFilter(){
        final Map<String,Object> filtersMap =
                ImmutableMap.of("dependencies",true,"relationships",true,"forcePush",false);
        final FilterDescriptor filterDescriptor =
                new FilterDescriptor("filterKey.yml","Filter Test Title",filtersMap,true,"Reviewer,dotcms.org.2789");
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);
    }

    private static void createShallowPushFilter(){
        final Map<String,Object> filtersMap =
                ImmutableMap.of("dependencies",false,"relationships",false,"forcePush",false);
        final FilterDescriptor filterDescriptor =
                new FilterDescriptor("ShallowPush.yml","Only Selected Items",filtersMap,false,"DOTCMS_BACK_END_USER");
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);
    }

    /**
     * <b>Method to test:</b> PushPublishigDependencyProcesor.tryToAdd(PusheableAsset, Object, String) <p>
     * <b>Given Scenario:</b> Push publish a page with the filter 'Only Selected Items' selected.<p>
     * <b>ExpectedResult:</b> The template should not be included in the dependencies.
     * @throws DotSecurityException
     * @throws DotBundleException
     * @throws DotDataException
     */
    @Test
    public void test_PP_page_should_not_contain_template_in_dependencies_when_filter_set() throws DotDataException, DotBundleException, DotSecurityException {
        final PushPublisherConfig config = new PushPublisherConfig();
        final Template template = new TemplateDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        //Create a bundle with filter 'Only Selected Items'
        final String filterKey = "ShallowPush.yml";
        if(!APILocator.getPublisherAPI().existsFilterDescriptor(filterKey)){
            createShallowPushFilter();
        }
        createBundle(config, htmlPageAsset, filterKey);
        DependencyManager dependencyManager = new DependencyManager(DependencyManagerTest.user, config);
        dependencyManager.setDependencies();

        assertFalse(APILocator.getPublisherAPI().getFilterDescriptorByKey("ShallowPush.yml").toString(),
                dependencyManager.getTemplates().contains(template.getIdentifier()));
    }

    /**
     * <b>Method to test:</b> PushPublishigDependencyProcesor.tryToAdd(PusheableAsset, Object, String) <p>
     * <b>Given Scenario:</b>  Custom templates be should considered part of the page. <p>
     * <b>ExpectedResult:</b> The layout should be included in the dependencies regardless of the selected filter.
     * @throws DotSecurityException
     * @throws DotBundleException
     * @throws DotDataException
     */
    @Test
    public void test_PP_page_should_contains_layout_in_dependencies() throws DotDataException, DotBundleException, DotSecurityException {
        final PushPublisherConfig config = new PushPublisherConfig();
        //Layout templates are identified by the prefix 'anonymous_layout_'
        final Template template = new TemplateDataGen().title(Template.ANONYMOUS_PREFIX+"shouldBeIncluded").nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        //Create a bundle with filter 'Only Selected Items'
        final String filterKey = "ShallowPush.yml";
        if(!APILocator.getPublisherAPI().existsFilterDescriptor(filterKey)){
            createShallowPushFilter();
        }
        createBundle(config, htmlPageAsset, filterKey);
        createBundle(config, htmlPageAsset, "ShallowPush.yml");
        DependencyManager dependencyManager = new DependencyManager(DependencyManagerTest.user, config);
        dependencyManager.setDependencies();

        assertTrue(dependencyManager.getTemplates().contains(template.getIdentifier()));
    }

    /**
     * <b>Method to test:</b> {@link com.dotcms.publisher.util.dependencies.PushPublishigDependencyProcesor#tryToAdd(PusheableAsset, Object, String)}<p>
     * <b>Given Scenario:</b>  A page that only exist in a non default lang should PP the template/layout if required <p>
     * <b>ExpectedResult:</b> The template/layout should be included in the dependencies
     * @throws DotSecurityException
     * @throws DotBundleException
     * @throws DotDataException
     */
    @Test
    public void test_PP_page_non_default_language_should_contains_template_layout_in_dependencies() throws DotDataException, DotBundleException, DotSecurityException {
        final PushPublisherConfig config = new PushPublisherConfig();
        //Layout templates are identified by the prefix 'anonymous_layout_'
        final Template template = new TemplateDataGen().title(Template.ANONYMOUS_PREFIX+"shouldBeIncluded"+System.currentTimeMillis()).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).languageId(language.getId()).nextPersisted();
        //Create a bundle with filter 'Only Selected Items'
        final String filterKey = "ShallowPush.yml";
        if(!APILocator.getPublisherAPI().existsFilterDescriptor(filterKey)){
            createShallowPushFilter();
        }
        createBundle(config, htmlPageAsset, filterKey);
        createBundle(config, htmlPageAsset, "ShallowPush.yml");
        DependencyManager dependencyManager = new DependencyManager(DependencyManagerTest.user, config);
        dependencyManager.setDependencies();

        assertTrue("Template isn't in the bundle",dependencyManager.getTemplates().contains(template.getIdentifier()));
    }

}
