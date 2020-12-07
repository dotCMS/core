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
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;

import java.util.*;

import org.junit.BeforeClass;
import org.junit.Test;
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

        //assertEquals(2, dependencyManager.getContents().size());
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
        final String bundleName = "testDependencyManagerBundle" + System.currentTimeMillis();
        Bundle bundle = new Bundle(bundleName, new Date(), null, user.getUserId());
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
            final Relationship relationship, final DependencyManager dependencyManager) {
        assertNotNull(dependencyManager.getRelationships());
        assertEquals(1, dependencyManager.getRelationships().size());
        assertEquals(relationship.getInode(),
                dependencyManager.getRelationships().iterator().next());
        assertNotNull(dependencyManager.getContents());
        //assertEquals(2, dependencyManager.getContents().size());
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
    
    
    /**
     * This Tests to make sure that
     * @throws Exception
     */
    
    @Test
    public void test_dependency_manager_setHTMLPagesDependencies() throws Exception {
        
        PushPublisherConfig config = new PushPublisherConfig();
        config.setOperation(Operation.PUBLISH);
        
        final Set<String> idents = new HashSet<>();
        idents.add("nope");
        idents.add("break");
        idents.add(null);
        
        DependencyManager manager = new DependencyManager(APILocator.systemUser(), config);
        
        try {
            manager.setHTMLPagesDependencies(idents, null);
        }
        catch(Exception e) {
            assertTrue("Unable to set HTML Page Dependencies", false);
        }
        
        assert(manager !=null && manager.getContents().isEmpty());
        
        
        
        
        
    }


    private static void createFilter(){
        final Map<String,Object> filtersMap =
                ImmutableMap.of("dependencies",true,"relationships",true,"forcePush",false);
        final FilterDescriptor filterDescriptor =
                new FilterDescriptor("filterKey.yml","Filter Test Title",filtersMap,true,"Reviewer,dotcms.org.2789");
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);
    }




}
