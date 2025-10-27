package com.dotcms.enterprise.publishing.remote;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publisher.util.dependencies.DependencyManager;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dotcms.rendering.velocity.directive.ParseContainer.getDotParserContainerUUID;

/**
 * This class is for testing the bundle that is gonna generate the system when using PP.
 * It calls all the bundlers that are called when doing PP, so the bundle generated
 * is exactly the same that when doing PP from the UI.
 *
 * If there is an issue related to PP with the sender (bundler) you can create the test here.
 */
public class PushPublishBundleGeneratorTest extends IntegrationTestBase {

    private static User systemUser = APILocator.systemUser();
    static String defaultFilterKey = "Intelligent.yml";

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        createFilter();

        createFilterDescriptor(defaultFilterKey,true,true,false,null,null,null,null,true);
    }

    /**
     * Creates and saves bundle
     * @param bundleName
     * @return a bundle
     * @throws DotDataException
     */
    static Bundle createBundle (final String bundleName, final boolean forcePush, final String filterKey)
            throws DotDataException {

        final BundleAPI bundleAPI         = APILocator.getBundleAPI();
        final Bundle     bundle1           = new Bundle(bundleName, null, null, systemUser.getUserId(),forcePush,filterKey);

        bundleAPI.saveBundle(bundle1);
        return bundle1;
    }

    /**
     * Creates a Filter and adds it to map of filters
     *
     * @param key
     * @param dependencies
     * @param relationships
     * @param forcePush
     * @param excludeClasses
     * @param excludeDependencyClasses
     * @param excludeQuery
     * @param excludeDependencyQuery
     * @param defaultFilter
     */
    private static void createFilterDescriptor(final String key, final boolean dependencies,//CREATE BUILDER PARA EVITAR METODO CON TANTOS PARAMS
            final boolean relationships, final boolean forcePush,
            final List<Object> excludeClasses, final List<Object> excludeDependencyClasses,
            final String excludeQuery, final String excludeDependencyQuery, final boolean defaultFilter){
        final Map<String,Object> filtersMap = new HashMap<>();
        if(UtilMethods.isSet(dependencies)) {
            filtersMap.put("dependencies", dependencies);
        }
        if(UtilMethods.isSet(relationships)) {
            filtersMap.put("relationships", relationships);
        }
        if(UtilMethods.isSet(forcePush)) {
            filtersMap.put("forcePush", forcePush);
        }
        if(UtilMethods.isSet(excludeClasses)) {
            filtersMap.put("excludeClasses", excludeClasses);
        }
        if(UtilMethods.isSet(excludeDependencyClasses)) {
            filtersMap.put("excludeDependencyClasses", excludeDependencyClasses);
        }
        if(UtilMethods.isSet(excludeQuery)) {
            filtersMap.put("excludeQuery", excludeQuery);
        }
        if(UtilMethods.isSet(excludeDependencyQuery)) {
            filtersMap.put("excludeDependencyQuery", excludeDependencyQuery);
        }

        APILocator.getPublisherAPI().addFilterDescriptor(new FilterDescriptor(key,key,filtersMap,defaultFilter,"DOTCMS_BACK_END_USER"));
    }

    /**
     * Generate a bundle and returns a PushPublisherConfig to retrieve the list of objects that make up
     * the bundle, organized by their types.
     */
    private static PushPublisherConfig generateBundle ( final String bundleId, final PushPublisherConfig.Operation operation )
            throws DotPublisherException, DotDataException, DotPublishingException, IllegalAccessException, InstantiationException, DotBundleException, IOException {

        final PushPublisherConfig pconf = new PushPublisherConfig();
        final PublisherAPI pubAPI = PublisherAPI.getInstance();

        final List<PublishQueueElement> tempBundleContents = pubAPI
                .getQueueElementsByBundleId(bundleId);
        final List<PublishQueueElement> assetsToPublish = new ArrayList<>();

        for (final PublishQueueElement queueElement : tempBundleContents) {
            assetsToPublish.add(queueElement);
        }

        pconf.setDownloading(true);
        pconf.setOperation(operation);

        pconf.setAssets(assetsToPublish);
        //Queries creation
        pconf.setLuceneQueries(PublisherUtil.prepareQueries(tempBundleContents));
        pconf.setId(bundleId);
        pconf.setUser(APILocator.getUserAPI().getSystemUser());

        //BUNDLERS

        final List<Class<IBundler>> bundlers = new ArrayList<>();
        final List<IBundler> confBundlers = new ArrayList<>();

        final Publisher publisher = new PushPublisher();
        publisher.init(pconf);
        //Add the bundles for this publisher
        for (final Class<IBundler> clazz : publisher.getBundlers()) {
            if (!bundlers.contains(clazz)) {
                bundlers.add(clazz);
            }
        }

        final File bundleRoot = BundlerUtil.getBundleRoot(pconf);

        final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(pconf, bundleRoot);

        // Run bundlers
        BundlerUtil.writeBundleMetaInfo(pconf, directoryBundleOutput);
        for (final Class<IBundler> aClass : bundlers) {

            final IBundler bundler = aClass.newInstance();
            confBundlers.add(bundler);
            bundler.setConfig(pconf);
            bundler.setPublisher(publisher);
            final BundlerStatus bundlerStatus = new BundlerStatus(bundler.getClass().getName());
            //Generate the bundler
            Logger.info(PushPublishBundleGeneratorTest.class, "Start of Bundler: " + aClass.getSimpleName());
            bundler.generate(directoryBundleOutput, bundlerStatus);
            Logger.info(PushPublishBundleGeneratorTest.class, "End of Bundler: " + aClass.getSimpleName());
        }

        pconf.setBundlers(confBundlers);

        return pconf;
    }

    /**
     * This test is for the filter excludeDependencyClasses.
     *
     * This test creates a Content Type and a content, and generates the bundle using the default Filter (this one does not have any exclude filter set),
     * so the content and the content type (as a dependency) will be added.
     * After this using the same Content Type and content generates another bundle but using a new created filter that exclude ContentTypes to be pushed
     * as a dependency, so in the bundle the content will be added but the content type will not.
     */
    @Test
    public void testGenerateBundleWithFilter_ExcludeDependencyClasses_ContentTypeWorkflow_NotAdded()
            throws DotDataException, IllegalAccessException, DotBundleException, DotPublishingException, InstantiationException, DotPublisherException, IOException {
        //Create ContentType
        final ContentType contentType = TestDataUtils.getWikiLikeContentType();
        //Create Content
        final Contentlet content = TestDataUtils.getWikiContent(true,APILocator.getLanguageAPI().getDefaultLanguage().getId(),contentType.id());

        //Create bundle with DefaultFilter
        final Bundle bundleWithDefaultFilter = createBundle("TestBundle"+System.currentTimeMillis(),false,defaultFilterKey);
        //Add assets to the bundle
        PublisherAPI.getInstance().saveBundleAssets(Arrays.asList(content.getIdentifier()),bundleWithDefaultFilter.getId(),
                systemUser);
        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        final PushPublisherConfig listOfAssetsWithDefaultFilter = generateBundle(bundleWithDefaultFilter.getId(), Operation.PUBLISH);
        Assert.assertNotNull(listOfAssetsWithDefaultFilter);
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getStructures().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getStructures().contains(contentType.id()));
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getContentlets().contains(content.getIdentifier()));

        //Create filter
        final String filterKey = "TestFilterWithExcludeDependencyClasses.yml";
        final List<Object> excludeDependencyClassesList = new ArrayList<>();
        excludeDependencyClassesList.add("ContentType");
        excludeDependencyClassesList.add("Workflow");
        createFilterDescriptor(filterKey,true,true,false,
                null,excludeDependencyClassesList,null,null,false);
        //Create bundle with New filter
        final Bundle bundleWithNewFilter = createBundle("TestBundle"+System.currentTimeMillis(),false,filterKey);
        //Add assets to the bundle
        PublisherAPI.getInstance().saveBundleAssets(Arrays.asList(content.getIdentifier()),bundleWithNewFilter.getId(),
                systemUser);
        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        final PushPublisherConfig listOfAssetsWithNewFilter = generateBundle(bundleWithNewFilter.getId(), Operation.PUBLISH);
        Assert.assertNotNull(listOfAssetsWithNewFilter);
        Assert.assertTrue(listOfAssetsWithNewFilter.getStructures().isEmpty());
        Assert.assertTrue(listOfAssetsWithNewFilter.getContentlets().contains(content.getIdentifier()));
    }

    /**
     * This test is for the filter excludeClasses.
     *
     * This test creates a Template and a Container, and generates the bundle using the default Filter (this one does not have any exclude filter set),
     * so the Template and the Container will be added (both assets added manually).
     * After this using the same Template and Container generates another bundle but using a new created filter that exclude Template and Container to be pushed,
     * so in the bundle neither the Template nor the Container will be added.
     */
    @Test
    public void testGenerateBundleWithFilter_ExcludeClasses_TemplateContainer_NotAdded()
            throws DotDataException, IllegalAccessException, DotBundleException, DotPublishingException, InstantiationException, DotPublisherException, IOException {
        //Create Container
        final Container container = new ContainerDataGen().nextPersisted();
        //Create Template
        final Template template = new TemplateDataGen().nextPersisted();

        //Create bundle with DefaultFilter
        final Bundle bundleWithDefaultFilter = createBundle("TestBundle"+System.currentTimeMillis(),false,defaultFilterKey);
        //Add assets to the bundle
        PublisherAPI.getInstance().saveBundleAssets(Arrays.asList(template.getIdentifier(),container.getIdentifier()),bundleWithDefaultFilter.getId(),
                systemUser);
        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        final PushPublisherConfig listOfAssetsWithDefaultFilter = generateBundle(bundleWithDefaultFilter.getId(), Operation.PUBLISH);
        Assert.assertNotNull(listOfAssetsWithDefaultFilter);
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getTemplates().isEmpty());
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getContainers().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getTemplates().contains(template.getIdentifier()));
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getContainers().contains(container.getIdentifier()));

        //Create filter
        final String filterKey = "TestFilterWithExcludeClasses.yml";
        final List<Object> excludeClassesList = new ArrayList<>();
        excludeClassesList.add("Template");
        excludeClassesList.add("Containers");
        createFilterDescriptor(filterKey,true,true,false,
                excludeClassesList,null,null,null,false);
        //Create bundle with New filter
        final Bundle bundleWithNewFilter = createBundle("TestBundle"+System.currentTimeMillis(),false,filterKey);
        //Add assets to the bundle
        PublisherAPI.getInstance().saveBundleAssets(Arrays.asList(template.getIdentifier(),container.getIdentifier()),bundleWithNewFilter.getId(),
                systemUser);
        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        final PushPublisherConfig listOfAssetsWithNewFilter = generateBundle(bundleWithNewFilter.getId(), Operation.PUBLISH);
        Assert.assertNotNull(listOfAssetsWithNewFilter);
        Assert.assertTrue(listOfAssetsWithNewFilter.getTemplates().isEmpty());
        Assert.assertTrue(listOfAssetsWithNewFilter.getContainers().isEmpty());
    }

    /**
     * This test is for the filter excludeDependencyQuery
     *
     * This test creates 2 Content Types with a Relationship field, creates 2 content one of each Content Type and Relates them,
     * generates the bundle using the default Filter (this one does not have any exclude filter set),
     * so the both ContentTypes and content will be added (Content Types and child content as a dependency since only pushing the parent content).
     * After this using the same assets generates another bundle but using a new created filter that exclude content from the child Content Type to be pushed,
     * so in the bundle will be the parent and child Content Type and the parent content.
     */
    @Test
    public void testGenerateBundleWithFilter_ExcludeDependencyQuery_ContentFromChildContentType_NotAdded()
            throws DotDataException, IllegalAccessException, DotBundleException, DotPublishingException, InstantiationException, DotPublisherException, IOException, DotSecurityException {
        //Create child Content Type
        final ContentType childContentType = TestDataUtils.getNewsLikeContentType();
        //Create parent Content Type
        final ContentType parentContentType = TestDataUtils.getWikiLikeContentType();
        //Relate Content Types
        Field newField = FieldBuilder.builder(RelationshipField.class).name("chilCT_parentCT" + System.currentTimeMillis())
                .contentTypeId(parentContentType.id()).values(String.valueOf(
                        RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .relationType(childContentType.variable()).build();
        newField = APILocator.getContentTypeFieldAPI().save(newField, systemUser);
        //Create child content
        final Contentlet childContentlet = TestDataUtils.getNewsContent(true,APILocator.getLanguageAPI().getDefaultLanguage().getId(),childContentType.id());
        //Create parent content
        Contentlet parentContentlet = TestDataUtils.getWikiContent(false,APILocator.getLanguageAPI().getDefaultLanguage().getId(),parentContentType.id());
        //Relate contents
        final Relationship relationship = APILocator.getRelationshipAPI().getRelationshipFromField(newField,systemUser);
        parentContentlet = APILocator.getContentletAPI().checkin(parentContentlet,
                Map.of(relationship, CollectionsUtils.list(childContentlet)), systemUser,
                false);

        //Create bundle with DefaultFilter
        final Bundle bundleWithDefaultFilter = createBundle("TestBundle"+System.currentTimeMillis(),false,defaultFilterKey);
        //Add assets to the bundle
        PublisherAPI.getInstance().saveBundleAssets(Arrays.asList(parentContentlet.getIdentifier()),bundleWithDefaultFilter.getId(),
                systemUser);
        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        final PushPublisherConfig listOfAssetsWithDefaultFilter = generateBundle(bundleWithDefaultFilter.getId(), Operation.PUBLISH);
        Assert.assertNotNull(listOfAssetsWithDefaultFilter);
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getStructures().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getStructures().contains(parentContentType.id()));
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getStructures().contains(childContentType.id()));
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getContentlets().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getContentlets().contains(parentContentlet.getIdentifier()));
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getContentlets().contains(childContentlet.getIdentifier()));
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getRelationships().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getRelationships().contains(relationship.getInode()));

        //Create filter
        final String filterKey = "TestFilterWithExcludeDependencyQuery.yml";
        final String excludeDependencyQuery = "+contentType:"+childContentType.variable();
        createFilterDescriptor(filterKey,true,true,false,
                null,null,null,excludeDependencyQuery,false);
        //Create bundle with New filter
        final Bundle bundleWithNewFilter = createBundle("TestBundle"+System.currentTimeMillis(),false,filterKey);
        //Add assets to the bundle
        PublisherAPI.getInstance().saveBundleAssets(Arrays.asList(parentContentlet.getIdentifier()),bundleWithNewFilter.getId(),
                systemUser);
        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        final PushPublisherConfig listOfAssetsWithNewFilter = generateBundle(bundleWithNewFilter.getId(), Operation.PUBLISH);
        Assert.assertNotNull(listOfAssetsWithNewFilter);
        Assert.assertFalse(listOfAssetsWithNewFilter.getStructures().isEmpty());
        Assert.assertTrue(listOfAssetsWithNewFilter.getStructures().contains(parentContentType.id()));
        Assert.assertTrue(listOfAssetsWithNewFilter.getStructures().contains(childContentType.id()));
        Assert.assertFalse(listOfAssetsWithNewFilter.getContentlets().isEmpty());
        Assert.assertTrue(listOfAssetsWithNewFilter.getContentlets().contains(parentContentlet.getIdentifier()));
        Assert.assertFalse(listOfAssetsWithNewFilter.getContentlets().contains(childContentlet.getIdentifier()));
        Assert.assertFalse(listOfAssetsWithNewFilter.getRelationships().isEmpty());
        Assert.assertTrue(listOfAssetsWithNewFilter.getRelationships().contains(relationship.getInode()));
    }

    /**
     * This test is for the filter excludeQuery
     *
     * This test creates 2 Content Types (one Widget and one Content), creates one content of each one,
     * generates the bundle using the default Filter (this one does not have any exclude filter set),
     * and tries to push each content, so the both ContentTypes and content will be added.
     * After this using the same assets generates another bundle but using a new created filter that exclude content from the baseType Widget to be pushed,
     * so in the bundle will be only the content and the ContentType of the baseType Content.
     */
    @Test
    public void testGenerateBundleWithFilter_ExcludeQuery_BaseTypeWidget_NotAdded()
            throws DotDataException, IllegalAccessException, DotBundleException, DotPublishingException, InstantiationException, DotPublisherException, IOException, DotSecurityException {
        //Create Widget ContentType
        final ContentType widgetContentType = TestDataUtils.getWidgetLikeContentType();
        //Create Content ContentType
        final ContentType contentContentType = TestDataUtils.getWikiLikeContentType();
        //Create Widget contentlet
        final Contentlet widgetContentlet = TestDataUtils.getWidgetContent(true,APILocator.getLanguageAPI().getDefaultLanguage().getId(),widgetContentType.id());
        //Create Content contentlet
        final Contentlet contentContentlet = TestDataUtils.getWikiContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),contentContentType.id());

        //Create bundle with DefaultFilter
        final Bundle bundleWithDefaultFilter = createBundle("TestBundle"+System.currentTimeMillis(),false,defaultFilterKey);
        //Add assets to the bundle
        PublisherAPI.getInstance().saveBundleAssets(Arrays.asList(contentContentlet.getIdentifier(),widgetContentlet.getIdentifier()),bundleWithDefaultFilter.getId(),
                systemUser);
        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        final PushPublisherConfig listOfAssetsWithDefaultFilter = generateBundle(bundleWithDefaultFilter.getId(), Operation.PUBLISH);
        Assert.assertNotNull(listOfAssetsWithDefaultFilter);
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getStructures().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getStructures().contains(contentContentType.id()));
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getStructures().contains(widgetContentType.id()));
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getContentlets().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getContentlets().contains(contentContentlet.getIdentifier()));
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getContentlets().contains(widgetContentlet.getIdentifier()));

        //Create filter
        final String filterKey = "TestFilterWithExcludeQuery.yml";
        final String excludeQuery = "+baseType:"+ BaseContentType.WIDGET.getType();
        createFilterDescriptor(filterKey,true,true,false,
                null,null,excludeQuery,null,false);
        //Create bundle with New filter
        final Bundle bundleWithNewFilter = createBundle("TestBundle"+System.currentTimeMillis(),false,filterKey);
        //Add assets to the bundle
        PublisherAPI.getInstance().saveBundleAssets(Arrays.asList(contentContentlet.getIdentifier(),widgetContentlet.getIdentifier()),bundleWithNewFilter.getId(),
                systemUser);
        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        final PushPublisherConfig listOfAssetsWithNewFilter = generateBundle(bundleWithNewFilter.getId(), Operation.PUBLISH);
        Assert.assertNotNull(listOfAssetsWithNewFilter);
        Assert.assertFalse(listOfAssetsWithNewFilter.getStructures().isEmpty());
        Assert.assertTrue(listOfAssetsWithNewFilter.getStructures().contains(contentContentType.id()));
        Assert.assertFalse(listOfAssetsWithNewFilter.getStructures().contains(widgetContentType.id()));
        Assert.assertFalse(listOfAssetsWithNewFilter.getContentlets().isEmpty());
        Assert.assertTrue(listOfAssetsWithNewFilter.getContentlets().contains(contentContentlet.getIdentifier()));
        Assert.assertFalse(listOfAssetsWithNewFilter.getContentlets().contains(widgetContentlet.getIdentifier()));
    }

    /**
     * This test is for the filter dependencies.
     *
     * This test creates a Template, a Container, a Folder, a Page, a ContentType and a contentlet
     * and generates the bundle using the default Filter (this one does not have any exclude filter set),
     * by pushing the Page (by dependency it will pull everything else Contentlet, ContentType, Container, Template, Folder).
     * After this using the same assets generates another bundle but using a new created filter that exclude any dependency to be pushed,
     * so in the bundle there is nothing else but the Page.
     */
    @Test
    public void testGenerateBundleWithFilter_DependenciesFalse_TemplateContainerFolderContenttypeContentlet_NotAdded()
            throws DotDataException, IllegalAccessException, DotBundleException, DotPublishingException, InstantiationException, DotPublisherException, IOException {
        //Create Content Type
        final ContentType contentType = TestDataUtils.getWikiLikeContentType();
        //Create Container
        final Container container = new ContainerDataGen().withContentType(contentType,"!{title}").nextPersisted();
        //Create Template
        final String uuid = UUIDGenerator.generateUuid();
        final Template template = new TemplateDataGen().withContainer(container.getIdentifier(),uuid).nextPersisted();
        //Create contentlet
        final Contentlet contentlet = TestDataUtils.getWikiContent(true,APILocator.getLanguageAPI().getDefaultLanguage().getId(),contentType.id());
        //Create Folder
        final Folder folder = new FolderDataGen().nextPersisted();
        //Create Page
        final HTMLPageAsset page = new HTMLPageDataGen(folder,template).languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .nextPersisted();
        HTMLPageDataGen.publish(page);
        //Add Contentlet to Page
        final MultiTree multiTree = new MultiTree(page.getIdentifier(),
                container.getIdentifier(),
                contentlet.getIdentifier(), getDotParserContainerUUID(uuid), 0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        //Create bundle with DefaultFilter
        final Bundle bundleWithDefaultFilter = createBundle("TestBundle"+System.currentTimeMillis(),false,defaultFilterKey);
        //Add assets to the bundle
        PublisherAPI.getInstance().saveBundleAssets(Arrays.asList(page.getIdentifier()),bundleWithDefaultFilter.getId(),
                systemUser);
        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        final PushPublisherConfig listOfAssetsWithDefaultFilter = generateBundle(bundleWithDefaultFilter.getId(), Operation.PUBLISH);
        Assert.assertNotNull(listOfAssetsWithDefaultFilter);
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getStructures().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getStructures().contains(contentType.id()));
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getStructures().contains(page.getContentType().id()));
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getTemplates().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getTemplates().contains(template.getIdentifier()));
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getContainers().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getContainers().contains(container.getIdentifier()));
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getContentlets().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getContentlets().contains(contentlet.getIdentifier()));
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getContentlets().contains(page.getIdentifier()));
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getFolders().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getFolders().contains(folder.getInode()));


        //Create filter
        final String filterKey = "TestFilterWithDependenciesFalse.yml";
        createFilterDescriptor(filterKey,false,true,false,
                null,null,null,null,false);

        //Create bundle with New filter
        final Bundle bundleWithNewFilter = createBundle("TestBundle"+System.currentTimeMillis(),false,filterKey);
        //Add assets to the bundle
        PublisherAPI.getInstance().saveBundleAssets(Arrays.asList(page.getIdentifier()),bundleWithNewFilter.getId(),
                systemUser);
        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        final PushPublisherConfig listOfAssetsWithNewFilter = generateBundle(bundleWithNewFilter.getId(), Operation.PUBLISH);
        Assert.assertNotNull(listOfAssetsWithNewFilter);
        Assert.assertTrue(listOfAssetsWithNewFilter.getStructures().isEmpty());
        Assert.assertTrue(listOfAssetsWithNewFilter.getTemplates().isEmpty());
        Assert.assertTrue(listOfAssetsWithNewFilter.getContainers().isEmpty());
        Assert.assertFalse(listOfAssetsWithNewFilter.getContentlets().isEmpty());
        Assert.assertTrue(listOfAssetsWithNewFilter.getContentlets().contains(page.getIdentifier()));
        Assert.assertTrue(listOfAssetsWithNewFilter.getFolders().isEmpty());
    }

    /**
     * This test is for the filter relationships
     *
     * This test creates 2 Content Types with a Relationship field, creates 2 content one of each Content Type and Relates them,
     * generates the bundle using the default Filter (this one does not have any exclude filter set),
     * so the both ContentTypes and content will be added (Content Types and child content as a dependency since only pushing the parent content).
     * After this using the same assets generates another bundle but using a new created filter that exclude relationships to be pushed,
     * so in the bundle will be the parent and child Content Type and the parent content.
     */
    @Test
    public void testGenerateBundleWithFilter_RelationshipsFalse_ChildcontentletChildcontentyypeRelationship_NotAdded()
            throws DotDataException, IllegalAccessException, DotBundleException, DotPublishingException, InstantiationException, DotPublisherException, IOException, DotSecurityException {
        //Create child Content Type
        final ContentType childContentType = TestDataUtils.getNewsLikeContentType();
        //Create parent Content Type
        final ContentType parentContentType = TestDataUtils.getWikiLikeContentType();
        //Relate Content Types
        Field newField = FieldBuilder.builder(RelationshipField.class).name("chilCT_parentCT" + System.currentTimeMillis())
                .contentTypeId(parentContentType.id()).values(String.valueOf(
                        RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .relationType(childContentType.variable()).build();
        newField = APILocator.getContentTypeFieldAPI().save(newField, systemUser);
        //Create child content
        final Contentlet childContentlet = TestDataUtils.getNewsContent(true,APILocator.getLanguageAPI().getDefaultLanguage().getId(),childContentType.id());
        //Create parent content
        Contentlet parentContentlet = TestDataUtils.getWikiContent(false,APILocator.getLanguageAPI().getDefaultLanguage().getId(),parentContentType.id());
        //Relate contents
        final Relationship relationship = APILocator.getRelationshipAPI().getRelationshipFromField(newField,systemUser);
        parentContentlet = APILocator.getContentletAPI().checkin(parentContentlet,
                Map.of(relationship, CollectionsUtils.list(childContentlet)), systemUser,
                false);

        //Create bundle with DefaultFilter
        final Bundle bundleWithDefaultFilter = createBundle("TestBundle"+System.currentTimeMillis(),false,defaultFilterKey);
        //Add assets to the bundle
        PublisherAPI.getInstance().saveBundleAssets(Arrays.asList(parentContentlet.getIdentifier()),bundleWithDefaultFilter.getId(),
                systemUser);
        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        final PushPublisherConfig listOfAssetsWithDefaultFilter = generateBundle(bundleWithDefaultFilter.getId(), Operation.PUBLISH);
        Assert.assertNotNull(listOfAssetsWithDefaultFilter);
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getStructures().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getStructures().contains(parentContentType.id()));
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getStructures().contains(childContentType.id()));
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getContentlets().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getContentlets().contains(parentContentlet.getIdentifier()));
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getContentlets().contains(childContentlet.getIdentifier()));
        Assert.assertFalse(listOfAssetsWithDefaultFilter.getRelationships().isEmpty());
        Assert.assertTrue(listOfAssetsWithDefaultFilter.getRelationships().contains(relationship.getInode()));

        //Create filter
        final String filterKey = "TestFilterWithRelationshipsFalse.yml";
        createFilterDescriptor(filterKey,true,false,false,
                null,null,null,null,false);
        //Create bundle with New filter
        final Bundle bundleWithNewFilter = createBundle("TestBundle"+System.currentTimeMillis(),false,filterKey);
        //Add assets to the bundle
        PublisherAPI.getInstance().saveBundleAssets(Arrays.asList(parentContentlet.getIdentifier()),bundleWithNewFilter.getId(),
                systemUser);
        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        final PushPublisherConfig listOfAssetsWithNewFilter = generateBundle(bundleWithNewFilter.getId(), Operation.PUBLISH);
        Assert.assertNotNull(listOfAssetsWithNewFilter);
        Assert.assertFalse(listOfAssetsWithNewFilter.getStructures().isEmpty());
        Assert.assertTrue(listOfAssetsWithNewFilter.getStructures().contains(parentContentType.id()));
        Assert.assertTrue(listOfAssetsWithNewFilter.getStructures().contains(childContentType.id()));
        Assert.assertFalse(listOfAssetsWithNewFilter.getContentlets().isEmpty());
        Assert.assertTrue(listOfAssetsWithNewFilter.getContentlets().contains(parentContentlet.getIdentifier()));
        Assert.assertFalse(listOfAssetsWithNewFilter.getContentlets().contains(childContentlet.getIdentifier()));
        Assert.assertFalse(listOfAssetsWithNewFilter.getRelationships().isEmpty());
    }

    /**
     * Method to test: {@link DependencyManager#setDependencies()}
     * Given Scenario: If a page has a Template that was deleted, we should be able to generate the bundle without issues.
     * ExpectedResult: The bundle should be generated without issues and the page added to the bundle.
     */
    @Test
    public void Test_GenerateBundle_PageWithTemplateDeleted_PageSuccessfullyAddedToBundle() throws Exception {
        //Create Template
        final Template template = new TemplateDataGen().nextPersisted();
        //Create Folder
        final Folder folder = new FolderDataGen().nextPersisted();
        //Create Page
        final HTMLPageAsset pageAsset = new HTMLPageDataGen(folder, template).nextPersisted();
        Assert.assertEquals(template.getIdentifier(),pageAsset.getTemplateId());
        //Delete Template
        final boolean isTemplateDeleted = APILocator.getTemplateAPI().delete(template,systemUser,false);
        Assert.assertTrue(isTemplateDeleted);
        //Create bundle
        final Bundle bundle = createBundle("TestBundle"+System.currentTimeMillis(),false,"");
        //Add assets to the bundle
        PublisherAPI.getInstance().saveBundleAssets(Arrays.asList(pageAsset.getIdentifier()),bundle.getId(),
                systemUser);
        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        final PushPublisherConfig listOfAssetsInBundle = generateBundle(bundle.getId(), Operation.PUBLISH);
        Assert.assertNotNull(listOfAssetsInBundle);
        Assert.assertTrue(listOfAssetsInBundle.getContentlets().contains(pageAsset.getIdentifier()));

    }

    /**
     * Given Scenario: If a file asset has zero files and CONTENT_ALLOW_ZERO_LENGTH_FILES set to true,
     *                  we should be able to generate the bundle without issues.
     * ExpectedResult: The bundle should be generated without issues and the page added to the bundle.
     */
    @Test
    public void Test_GenerateBundle_FileAssetZeroBytes_ContentAllowZeroLengthFilesSetToTrue_Success() throws Exception {
        final boolean allowZeroLengthFilesDefault = Config.getBooleanProperty("CONTENT_ALLOW_ZERO_LENGTH_FILES", true);
        try {
            //Set Property to True
            Config.setProperty("CONTENT_ALLOW_ZERO_LENGTH_FILES", true);
            //Create Folder
            final Folder folder = new FolderDataGen().nextPersisted();
            // Create File with zero length
            final File file = File.createTempFile("testing-file", ".vtl");
            FileUtil.write(file, "");
            // Create File Asset with that file
            final Contentlet fileAssetShown = new FileAssetDataGen(folder, file).nextPersisted();
            ContentletDataGen.publish(fileAssetShown);

            //Create bundle
            final Bundle bundle = createBundle("TestBundle" + System.currentTimeMillis(), false,
                    "");
            //Add assets to the bundle
            PublisherAPI.getInstance()
                    .saveBundleAssets(Arrays.asList(fileAssetShown.getIdentifier()), bundle.getId(),
                            systemUser);
            //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
            final PushPublisherConfig listOfAssetsInBundle = generateBundle(bundle.getId(),
                    Operation.PUBLISH);
            Assert.assertNotNull(listOfAssetsInBundle);
            Assert.assertTrue(
                    listOfAssetsInBundle.getContentlets().contains(fileAssetShown.getIdentifier()));
        } finally {
            //Set Property to Default Value
            Config.setProperty("CONTENT_ALLOW_ZERO_LENGTH_FILES", allowZeroLengthFilesDefault);
        }

    }

    /**
     * Given Scenario: If a file asset has zero bytes and CONTENT_ALLOW_ZERO_LENGTH_FILES set to false,
     *                    when you try to generate a bundle with that file asset it fails.
     * ExpectedResult: DotBundleException should be thrown when trying to generate the bundle with a file with zero bytes.
     */
    @Test (expected = DotBundleException.class)
    public void Test_GenerateBundle_FileAssetZeroBytes_ContentAllowZeroLengthFilesSetToFalse_DotBundleException() throws Exception {
        final boolean allowZeroLengthFilesDefault = Config.getBooleanProperty("CONTENT_ALLOW_ZERO_LENGTH_FILES", true);
        try {
            //Create Folder
            final Folder folder = new FolderDataGen().nextPersisted();
            // Create File with zero length
            final File file = File.createTempFile("testing-file", ".vtl");
            FileUtil.write(file, "");
            // Create File Asset with that file
            final Contentlet fileAssetShown = new FileAssetDataGen(folder, file).nextPersisted();
            ContentletDataGen.publish(fileAssetShown);

            //Set Property to False
            Config.setProperty("CONTENT_ALLOW_ZERO_LENGTH_FILES", false);
            //Create bundle
            final Bundle bundle = createBundle("TestBundle" + System.currentTimeMillis(), false,
                    "");
            //Add assets to the bundle
            PublisherAPI.getInstance()
                    .saveBundleAssets(Arrays.asList(fileAssetShown.getIdentifier()), bundle.getId(),
                            systemUser);
            //Generate Bundle
            generateBundle(bundle.getId(), Operation.PUBLISH);
        } finally {
            //Set Property to Default Value
            Config.setProperty("CONTENT_ALLOW_ZERO_LENGTH_FILES", allowZeroLengthFilesDefault);
        }

    }

    /**
     * Given Scenario: If a content is explicitly added to a bundle, it should be added to the bundle regardless the last push_date,
     *                  even though that forcePush is set to false.
     *                  Create a Content and Add it to a bundle.
     *                  Insert a pushed asset to mimic that the content was already pushed
     *                  Generate the bundle, after running all the bundlers, the content must be on the generated bundle.
     * ExpectedResult: Content Explicitly Added to the Bundle Must Be In the Generated Bundle Regardless Push_Date
     */
    @Test
    public void testGenerateBundle_FilterDependenciesAndForcePushFalse_ContentExplicitlyAddedMustBePushedRegardlessPushDate()
            throws DotDataException, IllegalAccessException, DotBundleException, DotPublishingException, InstantiationException, DotPublisherException, IOException {
        //Create Content Type
        final ContentType contentType = TestDataUtils.getWikiLikeContentType();
        //Create contentlet
        final Contentlet contentlet = TestDataUtils.getWikiContent(true,APILocator.getLanguageAPI().getDefaultLanguage().getId(),contentType.id());

        //Create filter
        final String filterKey = "TestFilterDependenciesAndForcePushFalse.yml";
        createFilterDescriptor(filterKey,false,false,false,
                null,null,null,null,false);
        //Create bundle with New filter
        final Bundle bundleWithNewFilter = createBundle("TestBundle"+System.currentTimeMillis(),false,filterKey);
        //Add assets to the bundle
        PublisherAPI.getInstance().saveBundleAssets(Arrays.asList(contentlet.getIdentifier()),bundleWithNewFilter.getId(),
                systemUser);

        //Insert the pushed asset to mimic that the asset was already pushed
        final PushedAsset
                assetToPush =
                new PushedAsset(bundleWithNewFilter.getId(), contentlet.getIdentifier(), contentlet.getType(), new Date(), "", "", "");

        APILocator.getPushedAssetsAPI().savePushedAsset(assetToPush);

        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        final PushPublisherConfig listOfAssetsWithNewFilter = generateBundle(bundleWithNewFilter.getId(), Operation.PUBLISH);
        Assert.assertNotNull(listOfAssetsWithNewFilter);
        //Must Contains the contentlet, even though the last push_date of it is after the last mod_date, because was added explicitly
        Assert.assertFalse(listOfAssetsWithNewFilter.getContentlets().isEmpty());
        Assert.assertTrue(listOfAssetsWithNewFilter.getContentlets().contains(contentlet.getIdentifier()));
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
