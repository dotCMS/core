package com.dotcms.publisher.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import java.util.Date;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


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
     * <b>Given Scenario:</b> A {@link ContentType} with a page as detail page<p>
     * <b>ExpectedResult:</b> Should include the detail page as dependencies
     * @throws DotSecurityException
     * @throws DotBundleException
     * @throws DotDataException
     */
    @Test
    public void test_Content_Type_with_detail_Page()
            throws DotSecurityException, DotBundleException, DotDataException {

        final PushPublisherConfig config = new PushPublisherConfig();
        final User systemUser = APILocator.systemUser();
        final Host host = new SiteDataGen().nextPersisted();

        final String baseUrl = String.format("/test%s", System.currentTimeMillis());

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
        final Contentlet theme = new ThemeDataGen()
                .site(host)
                .themesFolder(folderTheme)
                .nextPersisted();

        final Template template = new TemplateDataGen()
                .drawedBody(templateLayout)
                .host(host)
                .theme(theme)
                .nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();

        final Contentlet contentlet = createContentlet(contentTypeForContent, container, htmlPageAsset);
        final ContentType contentType = new ContentTypeDataGen().user(systemUser)
                .host(host)
                .detailPage(htmlPageAsset.getIdentifier())
                .urlMapPattern(String.format("%s/{text}", baseUrl))
                .nextPersisted();

        //Creates a bundle with just the child
        createBundle(config, contentType);

        DependencyManager dependencyManager = new DependencyManager(DependencyManagerTest.user, config);
        dependencyManager.setDependencies();

        assertEquals(4, dependencyManager.getContentTypes().size());
        assertTrue(dependencyManager.getContentTypes().contains(contentType.id()));
        assertTrue(dependencyManager.getContentTypes().contains(contentlet.getContentType().id()));
        assertTrue(dependencyManager.getContentTypes().contains(theme.getContentType().id()));
        assertTrue(dependencyManager.getContentTypes().contains(htmlPageAsset.getContentType().id()));

//        assertEquals(3, dependencyManager.getContents().size());
        assertTrue(dependencyManager.getContents().contains(htmlPageAsset.getIdentifier()));
        assertTrue(dependencyManager.getContents().contains(contentlet.getIdentifier()));
        assertTrue(dependencyManager.getContents().contains(theme.getIdentifier()));

        assertEquals(1, dependencyManager.getTemplates().size());
        assertTrue(dependencyManager.getTemplates().contains(template.getIdentifier()));

        assertEquals(2, dependencyManager.getContainers().size());
        assertTrue(dependencyManager.getContainers().contains(container.getIdentifier()));
        assertTrue(dependencyManager.getContainers().contains(container_2.getIdentifier()));

        assertEquals(3, dependencyManager.getFolders().size());
        assertTrue(dependencyManager.getFolders().contains(theme.getFolder()));
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
    public void test_Content_Type_with_not_exists_detail_Page()
            throws DotSecurityException, DotBundleException, DotDataException {

        final PushPublisherConfig config = new PushPublisherConfig();
        final User systemUser = APILocator.systemUser();
        final Host host = new SiteDataGen().nextPersisted();

        final String baseUrl = String.format("/test%s", System.currentTimeMillis());

        final ContentType contentType = new ContentTypeDataGen().user(systemUser)
                .host(host)
                .detailPage("not_exists")
                .urlMapPattern(String.format("%s/{text}", baseUrl))
                .nextPersisted();

        //Creates a bundle with just the child
        createBundle(config, contentType);

        DependencyManager dependencyManager = new DependencyManager(DependencyManagerTest.user, config);
        dependencyManager.setDependencies();

        assertEquals(1, dependencyManager.getContentTypes().size());
        assertTrue(dependencyManager.getContentTypes().contains(contentType.id()));

//        assertEquals(0, dependencyManager.getContents().size());

        assertEquals(0, dependencyManager.getTemplates().size());

        assertEquals(0, dependencyManager.getContainers().size());
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

//            assertEquals(4, dependencyManager.getContents().size());
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
     * Creates a bundle with one contentlet
     */
    private void createBundle(final PushPublisherConfig config, final Contentlet contentlet)
            throws DotDataException {

        createBundle(config, PusheableAsset.CONTENTLET, contentlet.getInode(), contentlet.getIdentifier());
    }

    private void createBundle(final PushPublisherConfig config, final ContentType contentType)
            throws DotDataException {
        createBundle(config, PusheableAsset.CONTENT_TYPE, contentType.inode(), contentType.id());
    }

    private void createBundle(
            final PushPublisherConfig config,
            final PusheableAsset pusheableAsset,
            final String inode,
            final String id)

            throws DotDataException {

        final String bundleName = "testDependencyManagerBundle" + System.currentTimeMillis();
        Bundle bundle = new Bundle(bundleName, new Date(), null, user.getUserId());
        bundleAPI.saveBundle(bundle);
        bundle = bundleAPI.getBundleByName(bundleName);

        final PublishQueueElement publishQueueElement = new PublishQueueElement();
        publishQueueElement.setId(1);
        publishQueueElement.setOperation(Operation.PUBLISH.ordinal());
        publishQueueElement.setAsset(inode);
        publishQueueElement.setEnteredDate(new Date());
        publishQueueElement.setPublishDate(new Date());
        publishQueueElement.setBundleId(bundle.getId());
        publishQueueElement.setType(pusheableAsset.getType());

        config.setAssets(Lists.newArrayList(publishQueueElement));
        config.setId(bundle.getId());
        config.setOperation(Operation.PUBLISH);
        config.setDownloading(true);
        config.setLuceneQueries(Lists.newArrayList("+identifier:" + id));
    }

    /**
     * Validates the dependency manager includes relationship and both contentlets
     */
    private void validateDependencies(final Contentlet parentContent, final Contentlet childContent,
            final Relationship relationship, final DependencyManager dependencyManager) {
        assertNotNull(dependencyManager.getRelationships());
        assertEquals(1, dependencyManager.getRelationships().size());
        assertEquals(relationship.getInode(),
                dependencyManager.getRelationships().iterator().next());
        assertNotNull(dependencyManager.getContents());
//        assertEquals(2, dependencyManager.getContents().size());
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

}
