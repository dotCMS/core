package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.business.StoryBlockAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.ImmutableStoryBlockField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.EnvironmentDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FieldRelationshipDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FilterDescriptorDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.PushPublishingEndPointDataGen;
import com.dotcms.datagen.PushedAssetDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TemplateLayoutDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.datagen.WorkflowStepDataGen;
import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleFactoryImpl;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.ManifestItemsMapTest;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotcms.publishing.PublisherAPIImplTest;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.PublisherFilter;
import com.dotcms.publishing.manifest.CSVManifestBuilder;
import com.dotcms.publishing.manifest.ManifestBuilder;
import com.dotcms.publishing.manifest.ManifestItem;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.UniqueLanguageDataGen;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Config;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.collection.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static com.dotcms.publishing.PublisherAPIImplTest.getLanguagesVariableDependencies;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.set;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * This Integration Test verifies that the information added to Bundles for Push Publishing will contain the expected
 * data in terms of data dependencies, exclusions, Publishing Filters, and so on.
 *
 * @author Freddy Rodriguez
 * @since Feb 9th, 2021
 */
@RunWith(DataProviderRunner.class)
public class DependencyBundlerTest {

    private static  Map<String, List<ManifestItem>> excludeSystemFolder;
    private static  Map<String, List<ManifestItem>> excludeSystemFolderAndSystemHost;

    public static final String EXCLUDE_SYSTEM_FOLDER_HOST = "Excluded System Folder/Host/Container/Template";
    private static final String FILTER_EXCLUDE_REASON = "Excluded by filter";
    private static final String FILTER_EXCLUDE_BY_OPERATION = "Excluded by Operation: ";
    private BundlerStatus status = null;

    private DependencyBundler bundler = null;

    private static FilterDescriptor filterDescriptorAllDependencies;
    private static FilterDescriptor filterDescriptorNotDependencies;
    private static FilterDescriptor filterDescriptorNotRelationship;
    private static FilterDescriptor filterDescriptorNotDependenciesRelationship;

    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        final Map<String, List<ManifestItem>> excludeSystemFolderMap = new HashMap<>();
        excludeSystemFolderMap.put(EXCLUDE_SYSTEM_FOLDER_HOST,
                list(APILocator.getFolderAPI().findSystemFolder()));
        excludeSystemFolder = excludeSystemFolderMap;

        final Map<String, List<ManifestItem>> excludeSystemFolderAndSystemHostMap = new HashMap<>();
        excludeSystemFolderAndSystemHostMap.put(
                EXCLUDE_SYSTEM_FOLDER_HOST, list(APILocator.getFolderAPI().findSystemFolder(),
                        APILocator.getHostAPI().findSystemHost()));
        excludeSystemFolderAndSystemHost = excludeSystemFolderAndSystemHostMap;

        filterDescriptorAllDependencies = new FilterDescriptorDataGen().next();
        filterDescriptorNotDependencies = new FilterDescriptorDataGen()
                .dependencies(false)
                .next();

        filterDescriptorNotRelationship = new FilterDescriptorDataGen()
                .relationships(false)
                .next();

        filterDescriptorNotDependenciesRelationship = new FilterDescriptorDataGen()
                .relationships(false)
                .dependencies(false)
                .next();
    }

    @Before
    public void initTest() {
        status = mock(BundlerStatus.class);
        bundler = new DependencyBundler();
    }

    @DataProvider(format = "%m: %p[0]")
    public static Object[] assets() throws Exception {
        prepare();

        final ArrayList<Object> all = new ArrayList<>();
        all.addAll(createContentTypeTestCase());
        all.addAll(createTemplatesTestCase());
        all.addAll(createContainerTestCase());
        all.addAll(createFolderTestCase());
        all.addAll(createHostTestCase());
        all.addAll(createLinkTestCase());
        all.addAll(createWorkflowTestCase());
        all.addAll(createLanguageTestCase());
        all.addAll(createRuleTestCase());
        all.addAll(createContentTestCase());
        all.addAll(createContentTypeWithThirdPartyTestCase());
        all.addAll(createTemplateWithThirdPartyTestCase());
        all.addAll(createContainerWithThirdPartyTestCase());
        all.addAll(createFolderWithThirdPartyTestCase());
        all.addAll(createLinkWithThirdPartyTestCase());
        all.addAll(createRuleWithThirdPartyTestCase());
        all.addAll(createContentletWithThirdPartyTestCase());
        all.addAll(createContentletWithBlockEditorField());

        return all.toArray();
    }

    /**
     * Creates a Contentlet with a Story Block field, which has some text inside a {@code <p>} tag and a referenced
     * Contentlet as well. All Contentlets referenced inside a Story Block field must be pushed to the receiving
     * instance.
     *
     * @return The list of {@link TestData} objects used to feed the Data Provider.
     *
     * @throws DotDataException     An error occurred when interacting with the data source
     * @throws DotSecurityException The specified User does not have the required permissions to execute a given
     * operation.
     */
    private static Collection<?> createContentletWithBlockEditorField() throws DotDataException, DotSecurityException {
        final StoryBlockAPI storyBlockAPI = APILocator.getStoryBlockAPI();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final Host site = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().host(site).nextPersisted();
        final ContentType referencedContentType = new ContentTypeDataGen().host(site).nextPersisted();
        final Field storyBlockField =
                new FieldDataGen().name("Story Block").type(ImmutableStoryBlockField.class).contentTypeId(contentType.id()).nextPersisted();

        final Contentlet referencedContentlet =
                new ContentletDataGen(referencedContentType.id()).languageId(language.getId()).host(site).nextPersisted();
        Contentlet mainContentlet =
                new ContentletDataGen(contentType.id()).languageId(language.getId()).host(site).nextPersisted();
        final String dummyStoryBlock = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"," +
         "\"attrs\":{\"textAlign\":\"left\"}," + "\"content\":[{\"type\":\"text\",\"text\":\"this is paragraph\"}]}]}";
        mainContentlet.setProperty(storyBlockField.variable(), dummyStoryBlock);
        mainContentlet.setInode("");
        mainContentlet = contentletAPI.checkin(mainContentlet, APILocator.systemUser(), false);

        final Object storyBlockValue = mainContentlet.get(storyBlockField.variable());
        final Object updatedStoryBlockValue = storyBlockAPI.addContentlet(storyBlockValue, referencedContentlet);
        mainContentlet.setProperty(storyBlockField.variable(), updatedStoryBlockValue);
        mainContentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
        mainContentlet.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        mainContentlet.setInode("");
        contentletAPI.checkin(mainContentlet, APILocator.systemUser(), false);

        final Map<ManifestItem, Collection<ManifestItem>> dependencies = new HashMap<>();

        dependencies.put(mainContentlet, list(site, language, contentType, referencedContentlet));
        dependencies.put(referencedContentlet, list(language, referencedContentType));

        return list(new TestData(mainContentlet, dependencies, excludeSystemFolderAndSystemHost,
                filterDescriptorAllDependencies, "Content with Story Block and another content within"));
    }

    private static Collection<TestData> createContentletWithThirdPartyTestCase()
            throws DotDataException, DotSecurityException {

        final Host host = createHostWithDependencies();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final TestData contentTypeWithDependencies = createContentTypeWithDependencies();
        final ContentType contentType = (ContentType) contentTypeWithDependencies.assetsToAddInBundle;

        final List<Relationship> relationships = APILocator.getRelationshipAPI().byContentType(contentType);
        final Relationship relationship = relationships.get(0);

        final ContentType contentTypeChild = new StructureTransformer(relationship.getChildStructure()).from();

        final Contentlet contentletChild =  new ContentletDataGen(contentTypeChild.id())
                .languageId(language.getId())
                .host(host)
                .nextPersisted();

        final Contentlet content = new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .host(host)
                .setProperty(contentType.variable(), list(contentletChild))
                .nextPersisted();


        final Map<ManifestItem, Collection<ManifestItem>> dependencies = new HashMap<>();
        dependencies.put(content, list(host, language, contentType, relationship));
        dependencies.put(contentletChild, list(language, contentTypeChild));

        dependencies.putAll(contentTypeWithDependencies.dependenciesToAssert);
        dependencies.get(relationship).add(contentletChild);
        dependencies.get(relationship).add(contentType);
        dependencies.get(relationship).add(contentTypeChild);

        final TestData folderWithDependencies = createFolderWithDependencies();
        final Folder folder = (Folder) folderWithDependencies.assetsToAddInBundle ;

        final TestData contentTypeWithDependenciesWithFolder = createContentTypeWithDependencies();
        final ContentType contentTypeWithFolder = (ContentType) contentTypeWithDependenciesWithFolder.assetsToAddInBundle;

        final Contentlet contentWithFolder = new ContentletDataGen(contentTypeWithFolder.id())
                .languageId(language.getId())
                .host(folder.getHost())
                .folder(folder)
                .nextPersisted();

        final List<Relationship> relationshipsWithFolder = APILocator.getRelationshipAPI().byContentType(contentTypeWithFolder);
        final Relationship relationshipWithFolder = relationshipsWithFolder.get(0);

        final Map<ManifestItem, Collection<ManifestItem>> dependenciesWithFolder = new HashMap<>();

        dependenciesWithFolder.put(
                contentWithFolder, list(folder, folder.getHost(), language, contentTypeWithFolder, relationshipWithFolder)
        );
        dependenciesWithFolder.putAll(contentTypeWithDependenciesWithFolder.dependenciesToAssert);
        dependenciesWithFolder.get(relationshipWithFolder).add(contentTypeWithFolder);

        return list(
                new TestData(content, dependencies, excludeSystemFolder, filterDescriptorAllDependencies, "Content with Third Party Dependencies"),
                new TestData(contentWithFolder, dependenciesWithFolder, excludeSystemFolder, filterDescriptorAllDependencies, "Content with folder and Third Party Dependencies")
        );
    }

    private static Collection<TestData> createRuleWithThirdPartyTestCase()
            throws DotDataException {

        final Host host = createHostWithDependencies();
        final Rule rule = new RuleDataGen().host(host).nextPersisted();

        final TestData htmlPageWithDependencies = createHTMLPageWithDependencies();
        final HTMLPageAsset htmlPageAsset = (HTMLPageAsset) htmlPageWithDependencies.assetsToAddInBundle;
        final Rule ruleWithPage = new RuleDataGen().page(htmlPageAsset).nextPersisted();

        return list(
                new TestData(rule, Map.of(rule, list(rule, host)), new HashMap<>(), filterDescriptorAllDependencies, "Rule with third party"),
                new TestData(ruleWithPage, Map.of(ruleWithPage, list(htmlPageAsset)), new HashMap<>(), filterDescriptorAllDependencies, "Rule with page and third party")
        );
    }

    private static Collection<TestData> createLinkWithThirdPartyTestCase()
            throws DotDataException, DotSecurityException {

        final Host host = createHostWithDependencies();

        final TestData folderWithDependencies = createFolderWithDependencies(host);
        final Folder folder = (Folder) folderWithDependencies.assetsToAddInBundle;

        final Link link = new LinkDataGen(folder)
                .hostId(host.getIdentifier())
                .nextPersisted();

        final Map<ManifestItem, Collection<ManifestItem>> dependencies = Map.of(link, list(host, folder));

        return list(
                new TestData(link, dependencies, new HashMap<>(), filterDescriptorAllDependencies, "Link with third Party")
        );
    }

    private static Collection<TestData> createFolderWithThirdPartyTestCase()
            throws DotDataException, DotSecurityException {

        final Host host = createHostWithDependencies();

        final TestData parentFolderDependencies = createFolderWithDependencies(host);
        final Folder parentFolder = (Folder) parentFolderDependencies.assetsToAddInBundle;

        final Folder folder = new FolderDataGen()
                .site(host)
                .parent(parentFolder)
                .nextPersisted();

        final TestData contentTypeWithDependencies = createContentTypeWithDependencies(folder);
        final ContentType contentType = (ContentType) contentTypeWithDependencies.assetsToAddInBundle;

        final TestData subFolderWithDependencies = createFolderWithDependencies(folder);
        final Folder subFolder = (Folder) subFolderWithDependencies.assetsToAddInBundle;

        final Structure folderStructure = CacheLocator.getContentTypeCache()
                .getStructureByInode(folder.getDefaultFileType());

        final ContentType folderContentType = new StructureTransformer(folderStructure).from();

        final Map<ManifestItem, Collection<ManifestItem>> dependencies = new HashMap<>();

        dependencies.put(
                folder, list(host, folderContentType, contentType, subFolder, parentFolder));
        dependencies.putAll(contentTypeWithDependencies.dependenciesToAssert);
        dependencies.putAll(subFolderWithDependencies.dependenciesToAssert);

        return list(
                new TestData(folder, dependencies, excludeSystemFolderAndSystemHost, filterDescriptorAllDependencies, "Folder with third party")
        );
    }

    private static TestData createHTMLPageWithDependencies() {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final Host host = new SiteDataGen().nextPersisted();
        final ContentType contentTypeToPage = new ContentTypeDataGen().host(host).nextPersisted();
        final Container container = new ContainerDataGen().withContentType(contentTypeToPage, "").nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container).next();
        final Template template = new TemplateDataGen().drawedBody(templateLayout).nextPersisted();

        final Contentlet htmlPageAsset = new HTMLPageDataGen(host, template).host(host).languageId(defaultLanguage.getId()).nextPersisted();
        final ContentType htmlPageAssetContentType = htmlPageAsset.getContentType();

        return new TestData(htmlPageAsset, Map.of(
                htmlPageAsset, list(defaultLanguage, host, template, htmlPageAssetContentType),
                template, list(container),
                container, list(contentTypeToPage)
            ), null, "Page with dependencies");
    }

    private static Collection<TestData> createContainerWithThirdPartyTestCase()
            throws DotDataException, DotSecurityException {

        final Host host = createHostWithDependencies();

        final TestData contentTypeWithDependencies = createContentTypeWithDependencies();
        final ContentType contentType = (ContentType) contentTypeWithDependencies.assetsToAddInBundle;

        final Container containerWithContentType = new ContainerDataGen()
                .site(host)
                .withContentType(contentType, "")
                .nextPersisted();


        final Map<ManifestItem, Collection<ManifestItem>> dependencies = new HashMap<>();
        dependencies.put(containerWithContentType, list(host, contentType));
        dependencies.putAll(contentTypeWithDependencies.dependenciesToAssert);

        return list(
                new TestData(containerWithContentType, dependencies, excludeSystemFolder, filterDescriptorAllDependencies, "Container with Third party dependencies")
        );
    }

    private static Collection<TestData> createTemplateWithThirdPartyTestCase()
            throws DotDataException, DotSecurityException {

        final Host host = createHostWithDependencies();

        final TestData contentTypeWithDependencies = createContentTypeWithDependencies();
        final ContentType contentType = (ContentType) contentTypeWithDependencies.assetsToAddInBundle;

        final Container container = new ContainerDataGen()
                .site(host)
                .withContentType(contentType, "")
                .nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(container)
                .next();

        final Template template = new TemplateDataGen()
                .host(host)
                .drawedBody(templateLayout)
                .nextPersisted();

        final Map<ManifestItem, Collection<ManifestItem>> templateIncludes = new HashMap<>();

        templateIncludes.put(template, list(host, container));
        templateIncludes.put(container, list(contentType));
        templateIncludes.putAll(contentTypeWithDependencies.dependenciesToAssert);

        return list(
                new TestData(template, templateIncludes, excludeSystemFolder, filterDescriptorAllDependencies, "Template with third party dependencies")
        );
    }

    private static Collection<TestData> createContentTypeWithThirdPartyTestCase()
            throws DotDataException, DotSecurityException {

        final Host host = createHostWithDependencies();

        final ContentType contentType = new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Folder contentTypeFolder = (Folder) createFolderWithDependencies().assetsToAddInBundle;
        final Host folderHost = contentTypeFolder.getHost();
        final ContentType contentTypeWithFolder = new ContentTypeDataGen()
                .folder(contentTypeFolder)
                .nextPersisted();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();

        return list(
                new TestData(contentType, Map.of(contentType, list(host, systemWorkflowScheme)), excludeSystemFolder,
                        filterDescriptorAllDependencies, "Content Type with third party dependencies"),
                new TestData(contentTypeWithFolder, Map.of(
                        contentTypeWithFolder, list(folderHost, contentTypeFolder, systemWorkflowScheme)
                    ), new HashMap<>(), filterDescriptorAllDependencies, "Content Type with folder and third party dependencies")
        );
    }

    private static TestData createFolderWithDependencies() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder parentFolder = new FolderDataGen().site(host).nextPersisted();

        return createFolderWithDependencies(parentFolder);
    }

    private static TestData createFolderWithDependencies(final Host host) throws DotDataException, DotSecurityException {
        final Folder parentFolder = new FolderDataGen().site(host).nextPersisted();
        return createFolderWithDependencies(parentFolder);
    }

    private static TestData createFolderWithDependencies(final Folder parentFolder)
            throws DotDataException, DotSecurityException {

        final Host host = parentFolder.getHost();

        final Folder folder = new FolderDataGen()
                .site(host)
                .parent(parentFolder)
                .nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .folder(folder)
                .nextPersisted();

        final File image = new File(Thread.currentThread().getContextClassLoader().getResource("images/test.jpg").getFile());
        final Contentlet contentlet = new FileAssetDataGen(folder, image)
                .host(host)
                .setProperty("title", "contentletTitle")
                .setProperty("fileName", "contentletfileName")
                .nextPersisted();
        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

        final Link link = new LinkDataGen(folder).nextPersisted();

        final Folder subFolder = new FolderDataGen()
                .site(host)
                .parent(folder)
                .nextPersisted();

        final Contentlet contentlet_2 = new FileAssetDataGen(subFolder, image)
                .setProperty("title", "contentlet_2Title")
                .setProperty("fileName", "contentlet_2fileName")
                .host(host)
                .nextPersisted();

        final Structure folderStructure = CacheLocator.getContentTypeCache()
                .getStructureByInode(folder.getDefaultFileType());

        final ContentType folderContentType = new StructureTransformer(folderStructure).from();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();

        return new TestData(folder, Map.of(
                folder, list(host, parentFolder, contentType, contentlet, link, subFolder),
                contentlet, list(language),
                contentlet_2, list(APILocator.getLanguageAPI().getLanguage(contentlet_2.getLanguageId())),
                subFolder, list(contentlet_2, folderContentType),
                contentType, list(systemWorkflowScheme),
                folderContentType, list(systemWorkflowScheme)
        ), null, "Folder with third dependencies");
    }

    private static TestData createContentTypeWithDependencies() throws DotDataException, DotSecurityException {
        return createContentTypeWithDependencies(null);
    }

    private static TestData createContentTypeWithDependencies(Folder folder) throws DotDataException {
        final Host host = folder != null ? folder.getHost() : new SiteDataGen().nextPersisted();
        final WorkflowScheme workflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();
        final Category category = new CategoryDataGen().nextPersisted();

        ContentType contentType = null;
        final ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                .workflowId(workflowScheme.getId())
                .addCategory(category);

        if (folder == null) {
            contentType = contentTypeDataGen.host(host).nextPersisted();
        } else {
            contentType = contentTypeDataGen.folder(folder).nextPersisted();
        }

        final ContentType contentTypeChild =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Relationship relationship = new FieldRelationshipDataGen()
                .child(contentTypeChild)
                .parent(contentType)
                .nextPersisted();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();

        return new TestData(contentType, Map.of(
                contentType, list(host, workflowScheme, category, systemWorkflowScheme, relationship),
                contentTypeChild, list(host, systemWorkflowScheme),
                relationship, list(contentTypeChild)
        ), null, "Content Type with dependencies");
    }

    private static Host createHostWithDependencies(){
        final Host host = new SiteDataGen().nextPersisted();

        new FolderDataGen().site(host).nextPersisted();
        final ContentType anotherContentType = new ContentTypeDataGen().host(host).nextPersisted();
        new ContentletDataGen(anotherContentType.id()).host(host).nextPersisted();
        new RuleDataGen().host(host).nextPersisted();

        return host;
    }

    private static Collection<TestData> createContentTestCase()
            throws DotDataException, DotSecurityException, IOException {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().host(host).nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .host(host)
                .nextPersisted();

        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final Contentlet contentletWithFolder = new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .host(host)
                .folder(folder)
                .nextPersisted();

        final ContentType contentTypeParent =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final ContentType contentTypeChild =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Relationship relationship = new FieldRelationshipDataGen()
                .child(contentTypeChild)
                .parent(contentTypeParent)
                .nextPersisted();

        final Contentlet contentletChild =  new ContentletDataGen(contentTypeChild.id())
                .setPolicy(IndexPolicy.WAIT_FOR)
                .languageId(language.getId())
                .host(host)
                .nextPersisted();

        final Contentlet contentletWithRelationship = new ContentletDataGen(contentTypeParent.id())
                .setPolicy(IndexPolicy.WAIT_FOR)
                .languageId(language.getId())
                .host(host)
                .setProperty(contentTypeParent.variable(), list(contentletChild))
                .nextPersisted();

        final Category category = new CategoryDataGen().nextPersisted();
        final ContentType contentTypeWithCategory = new ContentTypeDataGen()
                .addCategory(category)
                .host(host)
                .nextPersisted();

        Contentlet contentWithCategory = new ContentletDataGen(contentTypeWithCategory.id())
                .languageId(language.getId())
                .host(host)
                .next();
        contentWithCategory = APILocator.getContentletAPI().checkin(contentWithCategory, APILocator.systemUser(), false,
                list(category));


        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final ContentType contentTypeToPage = new ContentTypeDataGen().host(host).nextPersisted();
        final Container container = new ContainerDataGen().withContentType(contentTypeToPage, "").nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container).next();
        final Template template = new TemplateDataGen().drawedBody(templateLayout).nextPersisted();

        final Contentlet htmlPageAsset = new HTMLPageDataGen(host, template).host(host).languageId(defaultLanguage.getId()).nextPersisted();
        final ContentType htmlPageAssetContentType = htmlPageAsset.getContentType();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();

        final Language imageFileLanguage = new UniqueLanguageDataGen().nextPersisted();
        final Folder imageFolder = new FolderDataGen().site(host).nextPersisted();
        File tempFile = File.createTempFile("contentWithImageBundleTest", ".jpg");
        URL url = FocalPointAPITest.class.getResource("/images/test.jpg");
        File testImage = new File(url.getFile());
        FileUtils.copyFile(testImage, tempFile);

        final Contentlet imageFileAsset = new FileAssetDataGen(tempFile)
                .host(host)
                .languageId(imageFileLanguage.getId())
                .folder(imageFolder).nextPersisted();

        final Field imageField = new FieldDataGen().type(ImageField.class).next();
        final ContentType contentTypeWithImageField = new ContentTypeDataGen().host(host).field(imageField).nextPersisted();
        final Contentlet contentletWithImage = new ContentletDataGen(contentTypeWithImageField)
                .host(host)
                .setProperty(imageField.variable(), imageFileAsset.getIdentifier())
                .languageId(language.getId())
                .nextPersisted();

        final Map<ManifestItem, Collection<ManifestItem>> contentletInclude = Map.of(
                contentlet, list(host, contentType, language),
                contentType, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>> contentletWithFolderIncludes = Map.of(
                contentletWithFolder, list(host, contentType, language, folder),
                contentType, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>> contentletWithRelationshipIncludes = Map.of(
                contentletWithRelationship, list(host, relationship, contentTypeParent, language),
                relationship, list(contentTypeParent, contentTypeChild, contentletChild),
                contentletChild, list(language, contentTypeChild),
                contentTypeParent, list(systemWorkflowScheme),
                contentTypeChild, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>> contentWithCategoryIncludes = Map.of(
                contentWithCategory, list(host, contentTypeWithCategory, language, category),
                contentTypeWithCategory, list(systemWorkflowScheme, category)
        );

        final Map<ManifestItem, Collection<ManifestItem>> htmlPageAssetIncludes = Map.of(
                htmlPageAsset, list(host, defaultLanguage, template, htmlPageAssetContentType),
                template, list(container, defaultHost),
                contentTypeToPage, list(systemWorkflowScheme),
                htmlPageAssetContentType, list(systemWorkflowScheme),
                container, list(contentTypeToPage)
        );

        final Map<ManifestItem, Collection<ManifestItem>>  contentletWithImageIncludes = Map.of(
                contentletWithImage, list(host, contentTypeWithImageField, imageFileAsset, language),
                imageFileAsset, list(imageFolder, imageFileLanguage, imageFileAsset.getContentType()),
                contentTypeWithImageField, list(APILocator.getWorkflowAPI().findSystemWorkflowScheme()),
                imageFileAsset.getContentType(), list(APILocator.getWorkflowAPI().findSystemWorkflowScheme())
        );

        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

        final Map<String, List<ManifestItem>> contentletWithFolderExcludes = Map.of(FILTER_EXCLUDE_REASON,
                list(host, contentType, language, folder));

        final Map<String, List<ManifestItem>> contentletWithRelationshipExcludes = Map.of(
                FILTER_EXCLUDE_REASON, list(host, contentTypeParent, language, contentTypeChild),
                EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder));

        final Map<String, List<ManifestItem>> contentWithCategoryExcludes = Map.of(FILTER_EXCLUDE_REASON,
                list(host, contentTypeWithCategory, language, category), EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder));

        final Map<String, List<ManifestItem>> htmlPageAssetExcludes = Map.of(FILTER_EXCLUDE_REASON,
                list(host, defaultLanguage, template, htmlPageAssetContentType), EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder));

        final Map<String, List<ManifestItem>>  contentletWithImageExcludes =  Map.of(FILTER_EXCLUDE_REASON,
                list(host, contentTypeWithImageField, imageFileAsset, language), EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder));

        final Host systemHost = APILocator.getHostAPI().findSystemHost();
        final ContentType systemHostContentType = new ContentTypeDataGen().host(systemHost).nextPersisted();

        final Contentlet systemHostContentlet_1 = new ContentletDataGen(systemHostContentType.id())
                .languageId(language.getId())
                .host(systemHost)
                .nextPersisted();

        final Contentlet systemHostContentlet_2 = new ContentletDataGen(systemHostContentType.id())
                .languageId(language.getId())
                .host(systemHost)
                .nextPersisted();

        final Map<ManifestItem, Collection<ManifestItem>> systemHostContentletInclude = Map.of(
                systemHostContentlet_1, list(systemHostContentType, language),
                systemHostContentType, list(systemWorkflowScheme)
        );

        return list(
                new TestData(contentlet, contentletInclude, excludeSystemFolder, filterDescriptorAllDependencies, "Contentlet with filterDescriptorAllDependencies"),
                new TestData(contentlet, new HashMap<>(),
                        Map.of(FILTER_EXCLUDE_REASON, list(host, contentType, language), EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)),
                        filterDescriptorNotDependencies, "Contentlet with filterDescriptorNotDependencies"),
                new TestData(contentlet, contentletInclude, excludeSystemFolder, filterDescriptorNotRelationship, "Contentlet with filterDescriptorNotRelationship"),
                new TestData(contentlet, new HashMap<>(),
                        Map.of(FILTER_EXCLUDE_REASON, list(host, contentType, language), EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)),
                        filterDescriptorNotDependenciesRelationship, "Contentlet with filterDescriptorNotDependenciesRelationship"),

                new TestData(contentletWithFolder, contentletWithFolderIncludes, excludeSystemFolder, filterDescriptorAllDependencies, "Contentlet with folder and filterDescriptorAllDependencies"),
                new TestData(contentletWithFolder, new HashMap<>(), contentletWithFolderExcludes, filterDescriptorNotDependencies, "Contentlet with folder and filterDescriptorNotDependencies"),
                new TestData(contentletWithFolder,contentletWithFolderIncludes, excludeSystemFolder, filterDescriptorNotRelationship, "Contentlet folder and with filterDescriptorNotRelationship"),
                new TestData(contentletWithFolder, new HashMap<>(), contentletWithFolderExcludes, filterDescriptorNotDependenciesRelationship, "Contentlet folder and with filterDescriptorNotDependenciesRelationship"),

                new TestData(contentletWithRelationship, contentletWithRelationshipIncludes, excludeSystemFolder,
                        filterDescriptorAllDependencies, "Contentlet with Relationship and filterDescriptorAllDependencies"),
                new TestData(contentletWithRelationship, Map.of(contentTypeParent, list(relationship), contentletWithRelationship, list(relationship)), contentletWithRelationshipExcludes,
                        Map.of(contentTypeChild.getManifestInfo().id(),
                                list(getDependencyReason(relationship))),
                        filterDescriptorNotDependencies,
                        "Contentlet with Relationship and filterDescriptorNotDependencies"),
                new TestData(contentletWithRelationship, Map.of(contentletWithRelationship, list(host, contentTypeParent, language, relationship), contentTypeParent, list(systemWorkflowScheme, relationship), relationship, list(contentTypeChild)),
                        Map.of(FILTER_EXCLUDE_REASON, list(), EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)),
                        filterDescriptorNotRelationship, "Contentlet with Relationship and filterDescriptorNotRelationship"),
                new TestData(contentletWithRelationship, Map.of(contentletWithRelationship, list(relationship), contentTypeParent, list(relationship)), contentletWithRelationshipExcludes,
                        Map.of(contentTypeChild.getManifestInfo().id(),
                                list(getDependencyReason(relationship))),
                        filterDescriptorNotDependenciesRelationship,
                        "Contentlet with Relationship and filterDescriptorNotDependenciesRelationship"),

                new TestData(contentWithCategory, contentWithCategoryIncludes, excludeSystemFolder,
                        filterDescriptorAllDependencies, "Contentlet with Category and filterDescriptorAllDependencies"),
                new TestData(contentWithCategory, new HashMap<>(),
                        Map.of(FILTER_EXCLUDE_REASON, list(host, contentTypeWithCategory, language, category),
                                EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)),
                        filterDescriptorNotDependencies, "Contentlet with Category and filterDescriptorNotDependencies"),
                new TestData(contentWithCategory, contentWithCategoryIncludes, excludeSystemFolder,
                        filterDescriptorNotRelationship, "Contentlet with Category and filterDescriptorNotRelationship"),
                new TestData(contentWithCategory, new HashMap<>(), contentWithCategoryExcludes, filterDescriptorNotDependenciesRelationship, "Contentlet with Category and filterDescriptorNotDependenciesRelationship"),

                new TestData(htmlPageAsset, htmlPageAssetIncludes, excludeSystemFolderAndSystemHost, filterDescriptorAllDependencies, "Page with filterDescriptorAllDependencies"),
                new TestData(htmlPageAsset, new HashMap<>(), htmlPageAssetExcludes, filterDescriptorNotDependencies, "Page with filterDescriptorNotDependencies"),
                new TestData(htmlPageAsset, htmlPageAssetIncludes, excludeSystemFolderAndSystemHost, filterDescriptorNotRelationship, "Page with filterDescriptorNotRelationship"),
                new TestData(htmlPageAsset, new HashMap<>(), htmlPageAssetExcludes, filterDescriptorNotDependenciesRelationship, "Page with filterDescriptorNotDependenciesRelationship"),

                new TestData(contentletWithImage, contentletWithImageIncludes, excludeSystemFolder, filterDescriptorAllDependencies, "Contentlet with Image and filterDescriptorAllDependencies"),
                new TestData(contentletWithImage, new HashMap<>(), contentletWithImageExcludes, filterDescriptorNotDependencies, "Contentlet with Image and filterDescriptorNotDependencies"),
                new TestData(contentletWithImage, contentletWithImageIncludes, excludeSystemFolder, filterDescriptorNotRelationship, "Contentlet with Image and filterDescriptorNotRelationship"),

                new TestData(contentletWithImage, Collections.emptyMap(), contentletWithImageExcludes, filterDescriptorNotDependenciesRelationship, "Contentlet with Image and filterDescriptorNotDependenciesRelationship"),

                new TestData(systemHostContentlet_1, systemHostContentletInclude,
                        Map.of(EXCLUDE_SYSTEM_FOLDER_HOST, list(systemHost)), filterDescriptorAllDependencies,
                        "Contentlet In System Host"),


                new TestData(contentletWithImage, new HashMap<>(), contentletWithImageExcludes, filterDescriptorNotDependenciesRelationship, "Contentlet with Image and filterDescriptorNotDependenciesRelationship")
        );
    }

    private static Collection<TestData> createRuleTestCase() {
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule = new RuleDataGen().host(host).nextPersisted();

        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        final Rule ruleWithPage = new RuleDataGen().page(htmlPageAsset).nextPersisted();

        return list(
                new TestData(rule, Map.of(rule, list(host)), new HashMap<>(), filterDescriptorAllDependencies, "Rule with filterDescriptorAllDependencies"),
                new TestData(rule, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host)), filterDescriptorNotDependencies, "Rule with filterDescriptorNotDependencies"),
                new TestData(rule, Map.of(rule, list(host)), new HashMap<>(), filterDescriptorNotRelationship, "Rule with filterDescriptorNotRelationship"),
                new TestData(rule, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host)), filterDescriptorNotDependenciesRelationship, "Page with filterDescriptorNotDependenciesRelationship"),

                new TestData(ruleWithPage, Map.of(ruleWithPage, list(htmlPageAsset)), new HashMap<>(), filterDescriptorAllDependencies, "Rule with page and filterDescriptorAllDependencies"),
                new TestData(ruleWithPage, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(htmlPageAsset)), filterDescriptorNotDependencies, "Rule with page and filterDescriptorNotDependencies"),
                new TestData(ruleWithPage, Map.of(ruleWithPage, list(htmlPageAsset)), new HashMap<>(), filterDescriptorNotRelationship, "Rule with page and filterDescriptorNotRelationship"),
                new TestData(ruleWithPage, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(htmlPageAsset)), filterDescriptorNotDependenciesRelationship, "Rule with page and filterDescriptorNotDependenciesRelationship")

        );

    }

    private static Collection<TestData> createLanguageTestCase() {
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        return list(
                new TestData(language, new HashMap<>(), new HashMap<>(), filterDescriptorAllDependencies, "Language with filterDescriptorAllDependencies"),
                new TestData(language, new HashMap<>(), filterDescriptorNotDependencies, "Language with filterDescriptorNotDependencies"),
                new TestData(language, new HashMap<>(), new HashMap<>(), filterDescriptorNotRelationship, "Language with filterDescriptorNotRelationship"),
                new TestData(language, new HashMap<>(), filterDescriptorNotDependenciesRelationship, "Language with filterDescriptorNotDependenciesRelationship")
        );

    }

    private static Collection<TestData> createWorkflowTestCase() {
        final WorkflowScheme workflowScheme = new WorkflowDataGen().nextPersisted();
        final WorkflowStep workflowStep = new WorkflowStepDataGen(workflowScheme.getId()).nextPersisted();

        return list(
                new TestData(workflowScheme, new HashMap<>(), new HashMap<>(), filterDescriptorAllDependencies, "WorkflowScheme with filterDescriptorAllDependencies"),
                new TestData(workflowScheme, new HashMap<>(), filterDescriptorNotDependencies, "WorkflowScheme with filterDescriptorNotDependencies"),
                new TestData(workflowScheme, new HashMap<>(), new HashMap<>(), filterDescriptorNotRelationship, "WorkflowScheme with filterDescriptorNotRelationship"),
                new TestData(workflowScheme, new HashMap<>(), filterDescriptorNotDependenciesRelationship, "WorkflowScheme with filterDescriptorNotDependenciesRelationship")
        );
    }

    private static Collection<TestData> createHostTestCase() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();

        final Host hostWithTemplate = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(hostWithTemplate).nextPersisted();

        final Host hostWithContainer = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen()
                .site(hostWithContainer)
                .clearContentTypes()
                .nextPersisted();

        final Host hostWithFolder = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(hostWithFolder).nextPersisted();

        final Host hostWithContent = new SiteDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().host(hostWithContent).nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).host(hostWithContent).nextPersisted();
        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

        final Host hostWithRule = new SiteDataGen().nextPersisted();
        final Rule rule = new RuleDataGen().host(hostWithRule).nextPersisted();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();
        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

        final Structure folderStructure = CacheLocator.getContentTypeCache().getStructureByInode(systemFolder.getDefaultFileType());
        final ContentType folderContentType = new StructureTransformer(folderStructure).from();

        final Map<ManifestItem, Collection<ManifestItem>> hostWithContentIncludes = Map.of(
                hostWithContent, list(contentType, contentlet),
                contentType, list(systemWorkflowScheme),
                contentlet, list(language, contentType)
        );

        final Map<ManifestItem, Collection<ManifestItem>> hostWithFolderInclude = Map.of(
                hostWithFolder, list(folder),
                folder, list(folderContentType),
                folderContentType, list(systemWorkflowScheme)
        );

        return list(
                new TestData(host, new HashMap<>(), new HashMap<>(), filterDescriptorAllDependencies, "Host with filterDescriptorAllDependencies"),
                new TestData(host, new HashMap<>(), new HashMap<>(), filterDescriptorNotDependencies, "Host with filterDescriptorNotDependencies"),
                new TestData(host, new HashMap<>(), new HashMap<>(), filterDescriptorNotRelationship, "Host with filterDescriptorNotRelationship"),
                new TestData(host, new HashMap<>(), filterDescriptorNotDependenciesRelationship, "Host with filterDescriptorNotDependenciesRelationship"),

                new TestData(hostWithTemplate, Map.of(hostWithTemplate, list(template)),
                        new HashMap<>(), filterDescriptorAllDependencies, "Host with template and filterDescriptorAllDependencies"),
                new TestData(hostWithTemplate, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(template)),
                        filterDescriptorNotDependencies, "Host with template and  filterDescriptorNotDependencies"),
                new TestData(hostWithTemplate, Map.of(hostWithTemplate, list(template)), new HashMap<>(),
                        filterDescriptorNotRelationship, "Host with template and  filterDescriptorNotRelationship"),
                new TestData(hostWithTemplate, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(template)),
                        filterDescriptorNotDependenciesRelationship, "Host with template and  filterDescriptorNotDependenciesRelationship"),

                new TestData(hostWithContainer, Map.of(hostWithContainer, list(container)), new HashMap<>(),
                        filterDescriptorAllDependencies, "Host with Container and filterDescriptorAllDependencies"),
                new TestData(hostWithContainer, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(container)),
                        filterDescriptorNotDependencies, "Host with Container and filterDescriptorNotDependencies"),
                new TestData(hostWithContainer, Map.of(hostWithContainer, list(container)), new HashMap<>(),
                        filterDescriptorNotRelationship, "Host with Container and filterDescriptorNotRelationship"),
                new TestData(hostWithContainer, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(container)),
                        filterDescriptorNotDependenciesRelationship, "Host with Container and filterDescriptorNotDependenciesRelationship"),

                new TestData(hostWithContent, hostWithContentIncludes, excludeSystemFolder, filterDescriptorAllDependencies, "Host with Content and filterDescriptorAllDependencies"),
                new TestData(hostWithContent, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(contentType, contentlet)),
                        filterDescriptorNotDependencies, "Host with Content andfilterDescriptorNotDependencies"),
                new TestData(hostWithContent, hostWithContentIncludes, excludeSystemFolder,
                        filterDescriptorNotRelationship, "Host with Content andfilterDescriptorNotRelationship"),
                new TestData(hostWithContent, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(contentType, contentlet)),
                        filterDescriptorNotDependenciesRelationship, "Host with Content andfilterDescriptorNotDependenciesRelationship"),

                new TestData(hostWithFolder, hostWithFolderInclude, excludeSystemFolderAndSystemHost, filterDescriptorAllDependencies, "Host with Folder and filterDescriptorAllDependencies"),
                new TestData(hostWithFolder, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(folder)),
                        filterDescriptorNotDependencies, "Host with Folder and filterDescriptorNotDependencies"),
                new TestData(hostWithFolder, hostWithFolderInclude, excludeSystemFolderAndSystemHost,
                        filterDescriptorNotRelationship, "Host with Folder and filterDescriptorNotRelationship"),
                new TestData(hostWithFolder, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(folder)),
                        filterDescriptorNotDependenciesRelationship, "Host with Folder and filterDescriptorNotDependenciesRelationship"),

                new TestData(hostWithRule, Map.of(hostWithRule, list(rule)), new HashMap<>(),
                        filterDescriptorAllDependencies, "Host with Rule and filterDescriptorAllDependencies"),
                new TestData(hostWithRule, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(rule)),filterDescriptorNotDependencies, "Host with Rule and filterDescriptorNotDependencies"),
                new TestData(hostWithRule, Map.of(hostWithRule, list(rule)), new HashMap<>(), filterDescriptorNotRelationship, "Host with Rule and filterDescriptorNotRelationship"),
                new TestData(hostWithRule, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(rule)), filterDescriptorNotDependenciesRelationship, "Host with Rule and filterDescriptorNotDependenciesRelationship")
        );
    }

    private static Collection<TestData> createLinkTestCase() {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final Link link = new LinkDataGen(folder)
                .hostId(host.getIdentifier())
                .nextPersisted();

        return list(
                new TestData(link, Map.of(link, list(host, folder)), new HashMap<>(), filterDescriptorAllDependencies, "Link with filterDescriptorAllDependencies"),
                new TestData(link, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, folder)), filterDescriptorNotDependencies, "Link with filterDescriptorNotDependencies"),
                new TestData(link, Map.of(link, list(host, folder)), new HashMap<>(), filterDescriptorNotRelationship, "Link with filterDescriptorNotRelationship"),
                new TestData(link, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, folder)), filterDescriptorNotDependenciesRelationship, "Link with filterDescriptorNotDependenciesRelationship")
        );
    }

    private static Collection<TestData> createFolderTestCase() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final Folder parentFolder = new FolderDataGen().site(host).nextPersisted();
        final Folder folderWithParent = new FolderDataGen()
                .site(host)
                .parent(parentFolder)
                .nextPersisted();

        final Folder folderWithContentType = new FolderDataGen().site(host).nextPersisted();
        final ContentType contentType = new ContentTypeDataGen()
                .folder(folderWithContentType)
                .nextPersisted();

        final Folder folderWithContent = new FolderDataGen().site(host).nextPersisted();

        final File image = new File(Thread.currentThread().getContextClassLoader().getResource("images/test.jpg").getFile());
        final Contentlet contentlet = new FileAssetDataGen(folderWithContent, image)
                .host(host)
                .nextPersisted();
        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());


        final Folder folderWithLink = new FolderDataGen().site(host).nextPersisted();
        final Link link = new LinkDataGen(folderWithLink).nextPersisted();

        final Folder folderWithSubFolder = new FolderDataGen().site(host).nextPersisted();
        final Folder subFolder = new FolderDataGen()
                .parent(folderWithSubFolder)
                .nextPersisted();

        final Contentlet contentlet_2 = new FileAssetDataGen(subFolder, image)
                .host(host)
                .nextPersisted();

        final Structure folderStructure = CacheLocator.getContentTypeCache()
                .getStructureByInode(folder.getDefaultFileType());

        final ContentType folderContentType = new StructureTransformer(folderStructure).from();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();

        final Map<ManifestItem, Collection<ManifestItem>> folderIncludes = Map.of(
                folder, list(host, folderContentType),
                folderContentType, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>> folderWithParentIncludes = Map.of(
                folderWithParent, list(host, folderContentType, parentFolder),
                folderContentType, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>> folderWithContentTypeIncludes = Map.of(
                folderWithContentType, list(host, folderContentType, contentType),
                folderContentType, list(systemWorkflowScheme),
                contentType, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>> folderWithContentIncludes = Map.of(
                folderWithContent, list(host, folderContentType, contentlet),
                contentlet, list(contentlet, language, contentlet.getContentType()),
                contentlet.getContentType(), list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>>  folderWithLinkIncludes = Map.of(
                folderWithLink, list(host, folderContentType, link, systemWorkflowScheme),
                folderContentType, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>>  folderWithSubFolderIncludes = Map.of(
                folderWithSubFolder, list(host, folderContentType, subFolder),
                folderContentType, list(systemWorkflowScheme),
                subFolder, list(contentlet_2),
                contentlet_2, list(language)
        );

        //Folder with sub folder
        return list(
                new TestData(folder, folderIncludes, excludeSystemFolderAndSystemHost, filterDescriptorAllDependencies, "Folder with filterDescriptorAllDependencies"),
                new TestData(folder, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, folderContentType)),
                        filterDescriptorNotDependencies, "Folder with filterDescriptorNotDependencies"),
                new TestData(folder, folderIncludes, excludeSystemFolderAndSystemHost, filterDescriptorNotRelationship, "Folder with filterDescriptorNotRelationship"),
                new TestData(folder, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, folderContentType)),
                        filterDescriptorNotDependenciesRelationship, "Folder with filterDescriptorNotDependenciesRelationship"),

                //Dependency manager not add Parent Folder, the Parent Folder is added as dependency in FolderBundle
                new TestData(folderWithParent, folderWithParentIncludes, excludeSystemFolderAndSystemHost,
                        filterDescriptorAllDependencies, "Folder with parent and filterDescriptorAllDependencies"),
                new TestData(folderWithParent, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, folderContentType, parentFolder)),
                        filterDescriptorNotDependencies, "Folder with parent and filterDescriptorNotDependencies"),
                new TestData(folderWithParent, folderWithParentIncludes, excludeSystemFolderAndSystemHost,
                        filterDescriptorNotRelationship, "Folder with parent and filterDescriptorNotRelationship"),
                new TestData(folderWithParent, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, folderContentType, parentFolder)),
                        filterDescriptorNotDependenciesRelationship, "Folder with parent and filterDescriptorNotDependenciesRelationship"),

                new TestData(folderWithContentType, folderWithContentTypeIncludes, excludeSystemFolderAndSystemHost,
                        filterDescriptorAllDependencies, "Folder with ContentType and filterDescriptorAllDependencies"),
                new TestData(folderWithContentType, new HashMap<>(),
                        Map.of(FILTER_EXCLUDE_REASON, list(host, folderContentType, contentType)), filterDescriptorNotDependencies, "Folder with ContentType and filterDescriptorNotDependencies"),
                new TestData(folderWithContentType, folderWithContentTypeIncludes, excludeSystemFolderAndSystemHost,
                        filterDescriptorNotRelationship, "Folder with ContentType and filterDescriptorNotRelationship"),
                new TestData(folderWithContentType, new HashMap<>(),
                        Map.of(FILTER_EXCLUDE_REASON, list(host, folderContentType, contentType)),
                        filterDescriptorNotDependenciesRelationship, "Folder with ContentType and filterDescriptorNotDependenciesRelationship"),

                new TestData(folderWithContent, folderWithContentIncludes, excludeSystemFolderAndSystemHost, filterDescriptorAllDependencies, "Folder with Content and filterDescriptorAllDependencies"),
                new TestData(folderWithContent, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, folderContentType, contentlet)),
                        filterDescriptorNotDependencies, "Folder with Content and filterDescriptorNotDependencies"),
                new TestData(folderWithContent, folderWithContentIncludes, excludeSystemFolderAndSystemHost, filterDescriptorNotRelationship, "Folder with Content and filterDescriptorNotRelationship"),
                new TestData(folderWithContent, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, folderContentType, contentlet)),
                        filterDescriptorNotDependenciesRelationship, "Folder with Content and filterDescriptorNotDependenciesRelationship"),

                new TestData(folderWithLink, folderWithLinkIncludes, excludeSystemFolderAndSystemHost, filterDescriptorAllDependencies, "Folder with Link and filterDescriptorAllDependencies"),
                new TestData(folderWithLink, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, folderContentType, link)),
                        filterDescriptorNotDependencies, "Folder with Link and filterDescriptorNotDependencies"),
                new TestData(folderWithLink, folderWithLinkIncludes, excludeSystemFolderAndSystemHost, filterDescriptorNotRelationship, "Folder with Link and filterDescriptorNotRelationship"),
                new TestData(folderWithLink, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, folderContentType, link)),
                        filterDescriptorNotDependenciesRelationship, "Folder with Link and filterDescriptorNotDependenciesRelationship"),

                new TestData(folderWithSubFolder, folderWithSubFolderIncludes, excludeSystemFolderAndSystemHost, filterDescriptorAllDependencies, "Folder with Subfolder and filterDescriptorAllDependencies"),
                new TestData(folderWithSubFolder, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, folderContentType, subFolder)),
                        filterDescriptorNotDependencies, "Folder with Subfolder and filterDescriptorNotDependencies"),
                new TestData(folderWithSubFolder,folderWithSubFolderIncludes, excludeSystemFolderAndSystemHost,
                        filterDescriptorNotRelationship, "Folder with Subfolder and filterDescriptorNotRelationship"),
                new TestData(folderWithSubFolder, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, folderContentType, subFolder)),
                        filterDescriptorNotDependenciesRelationship, "Folder with Subfolder and filterDescriptorNotDependenciesRelationship")
        );
    }

    private static Collection<TestData> createContainerTestCase() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().host(host).nextPersisted();
        final Container containerWithoutContentType = new ContainerDataGen()
                .site(host)
                .clearContentTypes()
                .nextPersisted();

        final Container containerWithContentType = new ContainerDataGen()
                .site(host)
                .withContentType(contentType, "")
                .nextPersisted();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();

        final Map<ManifestItem, Collection<ManifestItem>> containerWithContentTypeIncludes = Map.of(
                containerWithContentType, list(host, contentType),
                contentType, list(systemWorkflowScheme)
        );

        return list(
                new TestData(containerWithoutContentType, Map.of(containerWithoutContentType, list(host)),
                        new HashMap<>(), filterDescriptorAllDependencies, "Container without Contenttype and filterDescriptorAllDependencies"),
                new TestData(containerWithoutContentType, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host)),
                        filterDescriptorNotDependencies, "Container without Contenttype and der and filterDescriptorNotDependencies"),
                new TestData(containerWithoutContentType, Map.of(containerWithoutContentType, list(host)),
                        new HashMap<>(), filterDescriptorNotRelationship, "Container without Contenttype and filterDescriptorNotRelationship"),
                new TestData(containerWithoutContentType,  new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host)),
                        filterDescriptorNotDependenciesRelationship, "Container without Contenttype and filterDescriptorNotDependenciesRelationship"),

                new TestData(containerWithContentType,
                        containerWithContentTypeIncludes, excludeSystemFolder, filterDescriptorAllDependencies, "Container with Contenttype and filterDescriptorAllDependencies"),
                new TestData(containerWithContentType, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, contentType)),
                        filterDescriptorNotDependencies, "Container with Contenttype and filterDescriptorNotDependencies"),
                new TestData(containerWithContentType,containerWithContentTypeIncludes, excludeSystemFolder,
                        filterDescriptorNotRelationship, "Container with Contenttype and filterDescriptorNotRelationship"),
                new TestData(containerWithContentType, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, contentType)),
                 filterDescriptorNotDependenciesRelationship, "Container with Contenttype and filterDescriptorNotDependenciesRelationship")
        );
    }

    private static List<TestData> createTemplatesTestCase() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template advancedTemplateWithoutContainer = new TemplateDataGen().host(host).nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().host(host).nextPersisted();

        final Container container_1 = new ContainerDataGen()
                .site(host)
                .withContentType(contentType, "")
                .nextPersisted();

        final Container container_2 = new ContainerDataGen()
                .site(host)
                .withContentType(contentType, "")
                .nextPersisted();

        final Template advancedTemplateWithContainer = new TemplateDataGen()
                .host(host)
                .withContainer(container_1.getIdentifier())
                .withContainer(container_2.getIdentifier())
                .nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(container_1)
                .withContainer(container_2)
                .next();

        final Template templateWithTemplateLayout = new TemplateDataGen()
                .host(host)
                .drawedBody(templateLayout)
                .nextPersisted();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();

        final Map<ManifestItem, Collection<ManifestItem>> templateWithTemplateLayoutIncludes = Map.of(
                templateWithTemplateLayout, list(host, container_1, container_2),
                container_1, list(contentType),
                container_2, list(contentType),
                contentType, list(systemWorkflowScheme)
        );

        final Container systemContainer = APILocator.getContainerAPI().systemContainer();

        final TemplateLayout systemContainerTemplateLayout = new TemplateLayoutDataGen()
                .withContainer(systemContainer)
                .next();

        final Host systemTemplateHost = new SiteDataGen().nextPersisted();

        final Template templateSystemContainer = new TemplateDataGen()
                .host(systemTemplateHost)
                .drawedBody(systemContainerTemplateLayout)
                .nextPersisted();

        return list(
                new TestData(advancedTemplateWithoutContainer, Map.of(advancedTemplateWithoutContainer, list(host)),
                        new HashMap<>(), filterDescriptorAllDependencies, "Advanced Template without Container and filterDescriptorAllDependencies"),
                new TestData(advancedTemplateWithoutContainer, new HashMap<>(),  Map.of(FILTER_EXCLUDE_REASON, list(host)),
                        filterDescriptorNotDependencies, "Advanced Template without Container and filterDescriptorNotDependencies"),
                new TestData(advancedTemplateWithoutContainer, Map.of(advancedTemplateWithoutContainer, list(host)),
                        new HashMap<>(), filterDescriptorNotRelationship, "Advanced Template without Container and filterDescriptorNotRelationship"),
                new TestData(advancedTemplateWithoutContainer, new HashMap<>(),  Map.of(FILTER_EXCLUDE_REASON, list(host)),
                        filterDescriptorNotDependenciesRelationship, "Advanced Template without Container and filterDescriptorNotDependenciesRelationship"),

                new TestData(advancedTemplateWithContainer, Map.of(advancedTemplateWithContainer, list(host)),
                        new HashMap<>(), filterDescriptorAllDependencies, "Advanced Template with Container and filterDescriptorAllDependencies"),
                new TestData(advancedTemplateWithContainer, new HashMap<>(),  Map.of(FILTER_EXCLUDE_REASON, list(host)),
                        filterDescriptorNotDependencies, "Advanced Template with Container and filterDescriptorNotDependencies"),
                new TestData(advancedTemplateWithContainer, Map.of(advancedTemplateWithContainer, list(host)),
                        new HashMap<>(), filterDescriptorNotRelationship, "Advanced Template with Container and filterDescriptorNotRelationship"),
                new TestData(advancedTemplateWithContainer, new HashMap<>(),  Map.of(FILTER_EXCLUDE_REASON, list(host)),
                        filterDescriptorNotDependenciesRelationship, "Advanced Template with Container and filterDescriptorNotDependenciesRelationship"),

                new TestData(templateWithTemplateLayout, templateWithTemplateLayoutIncludes, excludeSystemFolder,
                        filterDescriptorAllDependencies, "Template with Template Layout and filterDescriptorAllDependencies"),
                new TestData(templateWithTemplateLayout, new HashMap<>(),  Map.of(FILTER_EXCLUDE_REASON, list(host, container_1, container_2)),
                        filterDescriptorNotDependencies, "Template with Template Layout and filterDescriptorNotDependencies"),
                new TestData(templateWithTemplateLayout, templateWithTemplateLayoutIncludes, excludeSystemFolder,
                        filterDescriptorNotRelationship, "Template with Template Layout and filterDescriptorNotRelationship"),

                new TestData(templateWithTemplateLayout, Collections.emptyMap(),  Map.of(FILTER_EXCLUDE_REASON, list(host, container_1, container_2)),
                        filterDescriptorNotDependenciesRelationship, "Template with Template Layout and filterDescriptorNotDependenciesRelationship"),
                new TestData(templateSystemContainer, Map.of(templateSystemContainer, list(systemTemplateHost)),
                        Map.of(EXCLUDE_SYSTEM_FOLDER_HOST, list(systemContainer)), filterDescriptorAllDependencies, "Template with System_Container"),


                new TestData(templateWithTemplateLayout, new HashMap<>(),  Map.of(FILTER_EXCLUDE_REASON, list(host, container_1, container_2)),
                        filterDescriptorNotDependenciesRelationship, "Template with Template Layout and filterDescriptorNotDependenciesRelationship")
        );

    }

    private static List<TestData> createContentTypeTestCase() throws DotDataException {

        final Host host = new SiteDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Host folderHost = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(folderHost).nextPersisted();
        final ContentType contentTypeWithFolder = new ContentTypeDataGen()
                .folder(folder)
                .nextPersisted();

        final WorkflowScheme workflowScheme = new WorkflowDataGen().nextPersisted();
        final ContentType contentTypeWithWorkflow = new ContentTypeDataGen()
                .host(host)
                .workflowId(workflowScheme.getId())
                .nextPersisted();

        final Category category = new CategoryDataGen().nextPersisted();
        final ContentType contentTypeWithCategory=  new ContentTypeDataGen()
                .host(host)
                .addCategory(category)
                .nextPersisted();

        final ContentType contentTypeParent =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final ContentType contentTypeChild =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Relationship relationship = new FieldRelationshipDataGen()
                .child(contentTypeChild)
                .parent(contentTypeParent)
                .nextPersisted();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();
        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

        final Map<ManifestItem, Collection<ManifestItem>> contentTypeWithFolderIncludes = Map.of(
                contentTypeWithFolder, list(folder, systemWorkflowScheme, folderHost));

        final Map<ManifestItem, Collection<ManifestItem>> contentTypeWithWorkflowIncludes = Map.of(
                contentTypeWithWorkflow, list(host, systemWorkflowScheme, workflowScheme));

        final Map<ManifestItem, Collection<ManifestItem>> contentTypeWithCategoryIncludes = Map.of(
                contentTypeWithCategory, list(host, systemWorkflowScheme, category));

        final Map<ManifestItem, Collection<ManifestItem>> contentTypeParentIncludes = Map.of(
                contentTypeParent, list(host, systemWorkflowScheme, relationship),
                relationship, list(contentTypeChild)
        );

        list(host, systemWorkflowScheme, category);
        return list(
                new TestData(contentType,
                        Map.of(contentType, list(host, systemWorkflowScheme)), excludeSystemFolder, filterDescriptorAllDependencies, "Contentype with filterDescriptorAllDependencies"),

                new TestData(contentType, new HashMap<>(),
                        Map.of(
                            FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme),
                            EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)), filterDescriptorNotDependencies, "Contentype with filterDescriptorNotDependencies"),

                new TestData(contentType,
                        Map.of(contentType, list(host, systemWorkflowScheme)),
                        excludeSystemFolder, filterDescriptorNotRelationship, "Contentype with and filterDescriptorNotRelationship"),

                new TestData(contentType, new HashMap<>(),
                        Map.of(FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme),
                                EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)), filterDescriptorNotDependenciesRelationship, "Contentype with filterDescriptorNotDependenciesRelationship"),

                new TestData(contentTypeWithFolder, contentTypeWithFolderIncludes, new HashMap<>(),
                        filterDescriptorAllDependencies, "Contentype with Folder and filterDescriptorAllDependencies"),
                new TestData(contentTypeWithFolder, new HashMap<>(),
                        Map.of(FILTER_EXCLUDE_REASON, list(folder, systemWorkflowScheme, folderHost)), filterDescriptorNotDependencies, "Contentype with Folder and filterDescriptorNotDependencies"),
                new TestData(contentTypeWithFolder, contentTypeWithFolderIncludes, new HashMap<>(),
                        filterDescriptorNotRelationship, "Contentype with Folder and filterDescriptorNotRelationship"),
               new TestData(contentTypeWithFolder, new HashMap<>(),
                       Map.of(FILTER_EXCLUDE_REASON, list(folder, systemWorkflowScheme, folderHost)),
                        filterDescriptorNotDependenciesRelationship, "Contentype with Folder and filterDescriptorNotDependenciesRelationship"),

                new TestData(contentTypeWithWorkflow, contentTypeWithWorkflowIncludes, excludeSystemFolder, filterDescriptorAllDependencies, "Contentype with Workflow and filterDescriptorAllDependencies"),
                new TestData(contentTypeWithWorkflow, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme, workflowScheme),
                        EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)), filterDescriptorNotDependencies, "Contentype with Workflow and filterDescriptorNotDependencies"),
                new TestData(contentTypeWithWorkflow, contentTypeWithWorkflowIncludes, excludeSystemFolder, filterDescriptorNotRelationship, "Contentype with Workflow and filterDescriptorNotRelationship"),
                new TestData(contentTypeWithWorkflow, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme, workflowScheme),
                        EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)), filterDescriptorNotDependenciesRelationship, "Contentype with Workflow and filterDescriptorNotDependenciesRelationship"),

                new TestData(contentTypeWithCategory, contentTypeWithCategoryIncludes, excludeSystemFolder,
                        filterDescriptorAllDependencies, "Contentype with Category and filterDescriptorAllDependencies"),
                new TestData(contentTypeWithCategory, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme, category),
                        EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)), filterDescriptorNotDependencies, "Contentype with Category and filterDescriptorNotDependencies"),
                new TestData(contentTypeWithCategory, contentTypeWithCategoryIncludes, excludeSystemFolder,
                        filterDescriptorNotRelationship, "Contentype with Category and filterDescriptorNotRelationship"),
                new TestData(contentTypeWithCategory, new HashMap<>(), Map.of(FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme, category),
                        EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)), filterDescriptorNotDependenciesRelationship, "Contentype with Category and filterDescriptorNotDependenciesRelationship"),

                new TestData(contentTypeParent, contentTypeParentIncludes, excludeSystemFolder, filterDescriptorAllDependencies, "Contentype with Relationship and filterDescriptorAllDependencies"),
                new TestData(contentTypeParent, Map.of(contentTypeParent, list(relationship)),
                        Map.of(FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme, contentTypeChild),
                            EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)),
                        Map.of(contentTypeChild.getManifestInfo().id(),
                                list(getDependencyReason(relationship))),
                        filterDescriptorNotDependencies,
                        "Contentype with Relationship and filterDescriptorNotDependencies"),
                new TestData(contentTypeParent, Map.of(contentTypeParent, list(host, systemWorkflowScheme, relationship), relationship, list(contentTypeChild)),
                        Map.of(FILTER_EXCLUDE_REASON, list(), EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)),
                        filterDescriptorNotRelationship, "Contentype with Relationship and filterDescriptorNotRelationship"),
                new TestData(contentTypeParent, Map.of(contentTypeParent, list(relationship)),
                        Map.of(FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme, contentTypeChild),
                            EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)),
                        Map.of(contentTypeChild.getManifestInfo().id(),
                                list(getDependencyReason(relationship))),
                        filterDescriptorNotDependenciesRelationship,
                        "Contentype with Relationship and filterDescriptorNotDependenciesRelationship")
        );
    }


    /**
     * Method to Test: {@link DependencyBundler#generate(BundleOutput, BundlerStatus)}
     * When: Case tested:
     * - Add a {@link ContentType} into a Bundle
     * - Add a {@link Container} into a Bundle
     * - Add a {@link Template} into a Bundle
     * - Add a {@link Folder} into a Bundle
     * - Add a {@link Host} into a Bundle
     * - Add a {@link Link} into a Bundle
     * - Add a {@link WorkflowScheme} into a Bundle
     * - Add a {@link Language} into a Bundle
     * - Add a {@link Rule} into a Bundle
     * Should:  Add all the dependencies ans created the Manifest file with all the register
     */
    @Test
    @UseDataProvider("assets")
    public void addAssetInBundle(final TestData testData)
            throws IOException, DotBundleException, DotDataException, DotSecurityException {

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
        APILocator.getPublisherAPI().addFilterDescriptor(testData.filterDescriptor);

        final PushPublisherConfig config = new PushPublisherConfig();
        final BundleOutput bundleOutput = new DirectoryBundleOutput(config);
        final Set<Object> dependencies = new HashSet<>();

        try (CSVManifestBuilder manifestBuilder = new CSVManifestBuilder()) {
            config.setManifestBuilder(manifestBuilder);

            new BundleDataGen()
                    .pushPublisherConfig(config)
                    .addAssets(set(testData.assetsToAddInBundle))
                    .filter(testData.filterDescriptor)
                    .nextPersisted();

            final Set<Object> languagesVariableDependencies = getLanguagesVariableDependencies(
                    true, false, false);

            final PublisherFilter publisherFilter = APILocator.getPublisherAPI()
                    .createPublisherFilter(config.getId());

            if (publisherFilter.isDependencies()) {
                dependencies.addAll(testData.dependencies());
                dependencies.addAll(languagesVariableDependencies);
            }

            dependencies.add(testData.assetsToAddInBundle);

            bundler.setConfig(config);
            bundler.generate(bundleOutput, status);

            manifestBuilder.close();

            final ManifestItemsMapTest manifestLines = testData.manifestLines();

            if (publisherFilter.isDependencies()) {
                PublisherAPIImplTest.addLanguageVariableManifestItem(
                        manifestLines,
                        true,
                        PublisherAPIImplTest.getLanguageVariables()
                );
            }

            PublisherAPIImplTest.assertManifestFile(manifestBuilder.getManifestFile(),
                    manifestLines, list("#Filter:"));
        }

        assertAll(config, dependencies);
    }

    @Test
    public void addLanguageVariableTestCaseInBundle()
            throws DotSecurityException, DotDataException, DotBundleException, IOException {

        Contentlet contentlet = null;

        try {
            final PushPublisherConfig config = new PushPublisherConfig();

            PublisherAPIImplTest.createLanguageVariableIfNeeded();

            final FilterDescriptor filterDescriptor = new FilterDescriptorDataGen().nextPersisted();
            new BundleDataGen()
                    .pushPublisherConfig(config)
                    .filter(filterDescriptor)
                    .nextPersisted();

            final BundleOutput bundleOutput = new DirectoryBundleOutput(config);

            try (ManifestBuilder manifestBuilder = new TestManifestBuilder()) {
                config.setManifestBuilder(manifestBuilder);
                bundler.setConfig(config);
                bundler.generate(bundleOutput, status);

                final Collection<Object> dependencies = getLanguagesVariableDependencies(
                        true, false, false);
                assertAll(config, dependencies);
            }
        } finally {
            if (contentlet != null) {
                ContentletDataGen.archive(contentlet);
                ContentletDataGen.remove(contentlet);
            }
        }

    }

    private static String getDependencyReason(final ManifestItem asset) {
        return String.format(
                "Dependency from: ID: %s Title: %s", asset.getManifestInfo().id(),
                asset.getManifestInfo().title());
    }

    @DataProvider(format = "%m: %p[0]")
    public static Object[] configs() throws Exception {
        return new ModDateTestData[] {
                new ModDateTestData(false, false, Operation.PUBLISH),
                new ModDateTestData(false, false, Operation.UNPUBLISH),
                new ModDateTestData(true, false, Operation.PUBLISH),
                new ModDateTestData(false, true, Operation.PUBLISH)
        };
    }

    /**
     * Method to Test: {@link DependencyBundler#generate(BundleOutput, BundlerStatus)}
     * When:
     * - Have a Relationship.
     * - Have a parent content related with a child contentlet.
     * - The child contentlet have a moddate before the last Push operation.
     * - Add the parent content into a bundle.
     * Should:
     * - Exclude the child contentlet in the bundle when isForPush is false, isDownload is false and Operation is PUBLISH
     * otherwise should include the content child and content child dependencies
     * - Create the Manifest File
     */
    @Test
    @UseDataProvider("configs")
    public void excludeContenletChildAssetByModDate(ModDateTestData modDateTestData)
            throws DotBundleException, DotDataException, DotSecurityException, IOException {

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptorAllDependencies);

        final Map<String, Object> relationShip = createRelationShip();

        final Host host = (Host) relationShip.get("host");
        final Language language = (Language) relationShip.get("language");
        final ContentType contentTypeParent =  (ContentType) relationShip.get("contentTypeParent");
        final ContentType contentTypeChild =  (ContentType) relationShip.get("contentTypeChild");
        final Relationship relationship = (Relationship) relationShip.get("relationship");

        final Contentlet contentletChild =  (Contentlet) relationShip.get("contentletChild");
        final Contentlet contentParent = (Contentlet) relationShip.get("contentParent");

        final Map<String, Object> pushContext = createPushContext(modDateTestData, contentParent);

        final Environment environment = (Environment) pushContext.get("environment");
        final PushPublishingEndPoint publishingEndPoint = (PushPublishingEndPoint) pushContext.get("publishingEndPoint");
        final PushPublisherConfig config = (PushPublisherConfig) pushContext.get("config");
        final Bundle bundle = (Bundle) pushContext.get("bundle");

        final BundleOutput bundleOutput = new DirectoryBundleOutput(config);

        final Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        yesterday.add(Calendar.HOUR, 2);

        createPushAsset(
                yesterday.getTime(),
                contentletChild.getIdentifier(),
                "content",
                environment,
                publishingEndPoint,
                bundle,
                Publisher.class);

        final Collection<Object> dependencies = new HashSet<>();

        try (CSVManifestBuilder manifestBuilder = new CSVManifestBuilder()) {
            config.setManifestBuilder(manifestBuilder);

            bundler.setConfig(config);
            bundler.generate(bundleOutput, status);

            final ManifestItemsMapTest manifestLines = new ManifestItemsMapTest();
            manifestLines.add(contentParent, "Added directly by User");

            final List<Contentlet> languageVariables = PublisherAPIImplTest.getLanguageVariables();
            if (modDateTestData.operation == Operation.PUBLISH) {
                dependencies.addAll(getLanguagesVariableDependencies(true, false, false));

                dependencies.addAll(list(host, language, contentTypeParent, contentTypeChild));
                dependencies.add(APILocator.getWorkflowAPI().findSystemWorkflowScheme());
                dependencies.add(language);

                manifestLines.addDependencies(Map.of(
                        contentParent, list(host, language, contentTypeParent, contentTypeChild, relationship),
                        relationship, list(contentTypeChild, contentTypeParent),
                        contentTypeParent, list(APILocator.getWorkflowAPI().findSystemWorkflowScheme()),
                        contentTypeChild, list(APILocator.getWorkflowAPI().findSystemWorkflowScheme())
                ));

                if (!languageVariables.isEmpty()) {
                    PublisherAPIImplTest.addLanguageVariableManifestItem(
                            manifestLines,
                            true,
                            languageVariables
                    );
                }


            } else {
                final String excludeByOperation = FILTER_EXCLUDE_BY_OPERATION + modDateTestData.operation;
                final List<ManifestItem> parentExcludeList = list(
                        host, language, contentTypeParent, relationship);
                final List<ManifestItem> childExcludeList = list(
                        language, contentTypeChild);
                manifestLines.addExcludes(
                        Map.of(excludeByOperation, parentExcludeList), contentParent);
                manifestLines.addExcludes(
                        Map.of(excludeByOperation, childExcludeList), contentletChild);

                final List<? extends Serializable> generalLangVarDependencies = list(
                        PublisherAPIImplTest.getLanguageVariablesContentType(),
                        APILocator.getWorkflowAPI().findSystemWorkflowScheme());

                Stream.concat(PublisherAPIImplTest.getLanguagesVariableDependencies(),
                        languageVariables, generalLangVarDependencies).forEach(asset -> {
                    final String dependencyReason = asset instanceof Contentlet &&
                            !languageVariables.isEmpty() && languageVariables.contains((Contentlet) asset) ?
                            "Added Automatically by dotCMS" :
                            getDependencyReason(!languageVariables.isEmpty() ?
                                    languageVariables.get(0) : (ManifestItem) asset);
                    manifestLines.addExclude((ManifestItem) asset, dependencyReason, excludeByOperation);
                });

                if (!languageVariables.isEmpty()) {
                    manifestLines.addExclude(APILocator.getFolderAPI().findSystemFolder(),
                            getDependencyReason(languageVariables.get(0)),
                            EXCLUDE_SYSTEM_FOLDER_HOST);
                    manifestLines.addExclude(APILocator.getHostAPI().findSystemHost(),
                            getDependencyReason(languageVariables.get(0).getContentType()),
                            EXCLUDE_SYSTEM_FOLDER_HOST);
                    manifestLines.addExclude(
                            APILocator.getWorkflowAPI().findSystemWorkflowScheme(),
                            getDependencyReason(languageVariables.get(0).getContentType()),
                            excludeByOperation);
                }
            }

            dependencies.add(contentParent);
            manifestLines.addExclude(APILocator.getFolderAPI().findSystemFolder(), EXCLUDE_SYSTEM_FOLDER_HOST);
            manifestLines.addExclude(APILocator.getHostAPI().findSystemHost(), EXCLUDE_SYSTEM_FOLDER_HOST);

            if (modDateTestData.isDownload || modDateTestData.isForcePush) {
                dependencies.add(contentletChild);
                manifestLines.addDependencies(Map.of(relationship, list(contentletChild)));

                manifestLines.addDependencies(Map.of(contentletChild, list(language, contentTypeChild)));
            } else if (modDateTestData.operation == Operation.PUBLISH) {
                manifestLines.addExclude(contentletChild,
                        getDependencyReason(relationship),"Excluded by mod_date");

                manifestLines.addDependencies(Map.of(contentletChild, list(language, contentTypeChild)));
            } else {
                manifestLines.addExclude(contentletChild,
                        getDependencyReason(relationship),
                        FILTER_EXCLUDE_BY_OPERATION + modDateTestData.operation);
            }

            manifestBuilder.close();
            PublisherAPIImplTest.assertManifestFile(manifestBuilder.getManifestFile(), manifestLines,
                    list("#Filter:"));
        }

         assertAll(config, dependencies);
    }

    /**
     * Method to Test: {@link DependencyBundler#generate(BundleOutput, BundlerStatus)}
     * When:
     * - Have a Relationship.
     * - Have a parent content related with a child contentlet.
     * - The child contentlet have two versions in different languages, one of them after the last
     * Push operation and the another one before it.
     * - Add the parent content into a bundle.
     * Should:
     * - The child contentlet should be include all the time that the operation is equals to PUBLISH
     */
    @Test
    @UseDataProvider("configs")
    public void includeContenletChildWithSeveralVersionAssetByModDate(ModDateTestData modDateTestData)
            throws DotBundleException, DotDataException, DotSecurityException, IOException {

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptorAllDependencies);

        final Map<String, Object> relationShip = createRelationShip();

        final Host host = (Host) relationShip.get("host");
        final Language language = (Language) relationShip.get("language");
        final ContentType contentTypeParent =  (ContentType) relationShip.get("contentTypeParent");
        final ContentType contentTypeChild =  (ContentType) relationShip.get("contentTypeChild");

        final Contentlet contentletChild =  (Contentlet) relationShip.get("contentletChild");
        final Contentlet contentParent = (Contentlet) relationShip.get("contentParent");

        final Contentlet contentletChildAnotherLang = ContentletDataGen.checkout(contentletChild);
        final Language anotherLang = new UniqueLanguageDataGen().nextPersisted();
        contentletChildAnotherLang.setLanguageId(anotherLang.getId());
        ContentletDataGen.checkin(contentletChildAnotherLang);

        final Map<String, Object> pushContext = createPushContext(modDateTestData, contentParent);

        final Environment environment = (Environment) pushContext.get("environment");
        final PushPublishingEndPoint publishingEndPoint = (PushPublishingEndPoint) pushContext.get("publishingEndPoint");
        final PushPublisherConfig config = (PushPublisherConfig) pushContext.get("config");
        final Bundle bundle = (Bundle) pushContext.get("bundle");

        final BundleOutput bundleOutput = new DirectoryBundleOutput(config);

        final Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        yesterday.add(Calendar.HOUR, 2);

        createPushAsset(
                yesterday.getTime(),
                contentletChild.getIdentifier(),
                "content",
                environment,
                publishingEndPoint,
                bundle,
                Publisher.class);

        try (ManifestBuilder manifestBuilder = new TestManifestBuilder()) {
            config.setManifestBuilder(manifestBuilder);

            bundler.setConfig(config);
            bundler.generate(bundleOutput, status);
        }

        final Collection<Object> dependencies = new HashSet<>();

        if (modDateTestData.operation == Operation.PUBLISH) {
            dependencies.addAll(getLanguagesVariableDependencies(
                    true, false, false));

            dependencies.addAll(list(host, language, contentTypeParent, contentTypeChild));
            dependencies.add(APILocator.getWorkflowAPI().findSystemWorkflowScheme());
            dependencies.add(language);
            dependencies.add(anotherLang);

            dependencies.add(contentletChild);
        }

        dependencies.add(contentParent);

        assertAll(config, dependencies);
    }

    /**
     * Method to Test: {@link DependencyBundler#generate(BundleOutput, BundlerStatus)}
     * When:
     * - Have a Relationship.
     * - Have a parent content related with a child contentlet.
     * - The child contentlet have a moddate before the last Push operation.
     * - Add the child content into a bundle.
     * Should:
     * - Include the child contentlet in the bundle
     */
    @Test
    @UseDataProvider("configs")
    public void notExcludeContenletChildAssetByModDate(ModDateTestData modDateTestData)
            throws DotBundleException, DotDataException, DotSecurityException, IOException {
        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptorAllDependencies);

        final Map<String, Object> relationShipMap = createRelationShip();

        final Host host = (Host) relationShipMap.get("host");
        final Language language = (Language) relationShipMap.get("language");
        final ContentType contentTypeParent =  (ContentType) relationShipMap.get("contentTypeParent");
        final ContentType contentTypeChild =  (ContentType) relationShipMap.get("contentTypeChild");

        final Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        final Contentlet contentletChild =  (Contentlet) relationShipMap.get("contentletChild");
        final Contentlet contentParent = (Contentlet) relationShipMap.get("contentParent");

        final Map<String, Object> pushContext = createPushContext(modDateTestData, contentletChild);

        final Environment environment = (Environment) pushContext.get("environment");
        final PushPublishingEndPoint publishingEndPoint = (PushPublishingEndPoint) pushContext.get("publishingEndPoint");
        final PushPublisherConfig config = (PushPublisherConfig) pushContext.get("config");
        final Bundle bundle = (Bundle) pushContext.get("bundle");

        yesterday.add(Calendar.HOUR, 2);
        createPushAsset(
                yesterday.getTime(),
                contentletChild.getIdentifier(),
                "content",
                environment,
                publishingEndPoint,
                bundle, Publisher.class);

        final BundleOutput bundleOutput = new DirectoryBundleOutput(config);

        try (ManifestBuilder manifestBuilder = new TestManifestBuilder()) {
            config.setManifestBuilder(manifestBuilder);
            bundler.setConfig(config);
            bundler.generate(bundleOutput, status);
        }

        final Collection<Object> dependencies = new HashSet<>();

        if (modDateTestData.operation == Operation.PUBLISH) {
            dependencies.addAll(getLanguagesVariableDependencies(
                    true, false, false));
            dependencies.addAll(list(host, language, contentTypeParent, contentTypeChild,
                    contentParent));

            dependencies.add(APILocator.getWorkflowAPI().findSystemWorkflowScheme());
            dependencies.add(language);
        }

        dependencies.add(contentletChild);
        assertAll(config, dependencies);
    }

    private Map<String, Object> createPushContext(final ModDateTestData modDateTestData,
            final Object... assets){

        final Environment environment = new EnvironmentDataGen().nextPersisted();

        final PushPublishingEndPoint publishingEndPoint = new PushPublishingEndPointDataGen()
                .environment(environment)
                .nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setDownloading(modDateTestData.isDownload);
        config.setOperation(modDateTestData.operation);

        final Bundle bundle = new BundleDataGen()
                .pushPublisherConfig(config)
                .filter(filterDescriptorAllDependencies)
                .downloading(modDateTestData.isDownload)
                .addAssets(Arrays.asList(assets))
                .operation(modDateTestData.operation)
                .forcePush(modDateTestData.isForcePush)
                .nextPersisted();

        try {
            final BundleFactoryImpl bundleFactory = new BundleFactoryImpl();
            bundleFactory.saveBundleEnvironment(bundle, environment);

            return Map.of(
                "environment", environment,
                "publishingEndPoint", publishingEndPoint,
                "config", config,
                "bundle", bundle,
                "bundleFactory", bundleFactory
            );
        }catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    private Map<String, Object> createRelationShip() {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final ContentType contentTypeParent =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final ContentType contentTypeChild =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Relationship relationship = new FieldRelationshipDataGen()
                .child(contentTypeChild)
                .parent(contentTypeParent)
                .nextPersisted();

        final Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        final Contentlet contentletChild =  new ContentletDataGen(contentTypeChild.id())
                .languageId(language.getId())
                .host(host)
                .modeDate(yesterday.getTime())
                .nextPersisted();

        final Contentlet contentParent = new ContentletDataGen(contentTypeParent.id())
                .languageId(language.getId())
                .host(host)
                .setProperty(contentTypeParent.variable(), list(contentletChild))
                .nextPersisted();

        return Map.of(
            "host", host,
            "language", language,
            "contentTypeParent", contentTypeParent,
            "contentTypeChild", contentTypeChild,
            "relationship", relationship,
            "contentletChild", contentletChild,
            "contentParent", contentParent
        );
    }

    private void createPushAsset(final Date pushDate,
            final String assetId,
            final String assetType,
            final Environment environment,
            final PushPublishingEndPoint publishingEndPoint,
            final Bundle bundle, Class<Publisher> publisherClass) {

        new PushedAssetDataGen()
            .assetId(assetId)
            .assetType(assetType)
            .bundle(bundle)
            .publishingEndPoint(publishingEndPoint)
            .environment(environment)
            .pushDate(pushDate)
            .publisher(publisherClass)
            .nextPersisted();
    }


    /**
     * Method to Test: {@link DependencyBundler#generate(BundleOutput, BundlerStatus)}
     * When:
     * - Have a Page with all its dependencies: Template, Containers, Host.
     * - Template, Containers, Host have a moddate before the last Push operation.
     * - Add the page into a bundle.
     * Should: Exclude all the page's dependencies
     */
    @Test
    @UseDataProvider("configs")
    public void excludeHTMLDependenciesByModDate(ModDateTestData modDateTestData)
            throws DotBundleException, DotDataException, DotSecurityException, IOException {

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptorAllDependencies);

        final Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        final Map<String, Object> pageAndDependencies = pageWithDependencies(yesterday.getTime());

        final Host host = (Host) pageAndDependencies.get("host");
        final Container container = (Container) pageAndDependencies.get("container");
        final Template template = (Template) pageAndDependencies.get("template");
        final HTMLPageAsset htmlPageAsset = (HTMLPageAsset) pageAndDependencies.get("htmlPageAsset");

        final Map<String, Object> pushContext = createPushContext(modDateTestData, htmlPageAsset);

        final Environment environment = (Environment) pushContext.get("environment");
        final PushPublishingEndPoint publishingEndPoint = (PushPublishingEndPoint) pushContext.get("publishingEndPoint");
        final PushPublisherConfig config = (PushPublisherConfig) pushContext.get("config");
        final Bundle bundle = (Bundle) pushContext.get("bundle");

        final BundleOutput bundleOutput = new DirectoryBundleOutput(config);

        yesterday.add(Calendar.HOUR, 2);
        createPushAsset(
            yesterday.getTime(),
            host.getIdentifier(),
            "host",
            environment,
            publishingEndPoint,
            bundle, Publisher.class);

        createPushAsset(
            yesterday.getTime(),
            template.getIdentifier(),
            "template",
            environment,
            publishingEndPoint,
            bundle, Publisher.class);

        createPushAsset(
            yesterday.getTime(),
            container.getIdentifier(),
            "container",
            environment,
            publishingEndPoint,
            bundle, Publisher.class);

        try (ManifestBuilder manifestBuilder = new TestManifestBuilder()) {
            config.setManifestBuilder(manifestBuilder);
            bundler.setConfig(config);
            bundler.generate(bundleOutput, status);
        }

        final Collection<Object> dependencies = new HashSet<>();

        if (modDateTestData.operation == Operation.PUBLISH) {
            dependencies.addAll(getLanguagesVariableDependencies(
                    true, false, false));

            final ContentType pageContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(htmlPageAsset.getStructureInode());
            dependencies.add(pageContentType);

            dependencies.add(APILocator.getWorkflowAPI().findSystemWorkflowScheme());
            dependencies.add(APILocator.getLanguageAPI().getDefaultLanguage());
        }

        if (modDateTestData.isDownload || modDateTestData.isForcePush) {
            dependencies.addAll(list(host, container, template));
        }

        dependencies.add(htmlPageAsset);
        assertAll(config, dependencies);
    }

    private Map<String, Object> pageWithDependencies(){
        return pageWithDependencies(null);
    }

    private Map<String, Object> pageWithDependencies(final Date modDateParam){
        final Date  modDate = modDateParam == null ? new Date() : modDateParam;

        final Host host = new SiteDataGen()
                .modDate(modDate)
                .nextPersisted();

        final Container container = new ContainerDataGen()
                .modDate(modDate)
                .site(host)
                .nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container).next();
        final Template template = new TemplateDataGen()
                .modDate(modDate)
                .drawedBody(templateLayout)
                .host(host)
                .nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .nextPersisted();

        return Map.of(
            "host", host,
            "container", container,
            "template", template,
            "htmlPageAsset", htmlPageAsset
        );
    }

    /**
     * Method to Test: {@link DependencyBundler#generate(BundleOutput, BundlerStatus)}
     * When:
     * - Have a Page with all its dependencies: Template, Containers, Host.
     * - Template, Containers, Host have a moddate before the last Push operation.
     * - Add the Template, Containers, Host and Page into a bundle.
     * Should: Should include all the pages's dependencies
     */
    @Test
    @UseDataProvider("configs")
    public void includeHTMLDependenciesNoMatterModDate(ModDateTestData modDateTestData)
            throws DotBundleException, DotDataException, DotSecurityException, IOException {

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptorAllDependencies);

        final Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        final Map<String, Object> pageAndDependencies = pageWithDependencies(yesterday.getTime());

        final Host host = (Host) pageAndDependencies.get("host");
        final Container container = (Container) pageAndDependencies.get("container");
        final Template template = (Template) pageAndDependencies.get("template");
        final HTMLPageAsset htmlPageAsset = (HTMLPageAsset) pageAndDependencies.get("htmlPageAsset");

        final Map<String, Object> pushContext = createPushContext(modDateTestData, host, container,
                template, htmlPageAsset);

        final Environment environment = (Environment) pushContext.get("environment");
        final PushPublishingEndPoint publishingEndPoint = (PushPublishingEndPoint) pushContext.get("publishingEndPoint");
        final PushPublisherConfig config = (PushPublisherConfig) pushContext.get("config");
        final Bundle bundle = (Bundle) pushContext.get("bundle");

        final BundleOutput bundleOutput = new DirectoryBundleOutput(config);

        yesterday.add(Calendar.HOUR, 2);
        createPushAsset(
                yesterday.getTime(),
                host.getIdentifier(),
                "host",
                environment,
                publishingEndPoint,
                bundle, Publisher.class);

        createPushAsset(
                yesterday.getTime(),
                template.getIdentifier(),
                "template",
                environment,
                publishingEndPoint,
                bundle, Publisher.class);

        createPushAsset(
                yesterday.getTime(),
                container.getIdentifier(),
                "container",
                environment,
                publishingEndPoint,
                bundle, Publisher.class);

        final BundleFactoryImpl bundleFactory = new BundleFactoryImpl();
        bundleFactory.saveBundleEnvironment(bundle, environment);

        try (ManifestBuilder manifestBuilder = new TestManifestBuilder()) {
            config.setManifestBuilder(manifestBuilder);
            bundler.setConfig(config);
            bundler.generate(bundleOutput, status);
        }

        final Collection<Object> dependencies = new HashSet<>();

        if (modDateTestData.operation == Operation.PUBLISH) {
            dependencies.addAll(getLanguagesVariableDependencies(
                    true, false, false));

            final ContentType pageContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(htmlPageAsset.getStructureInode());
            dependencies.add(pageContentType);

            dependencies.add(APILocator.getWorkflowAPI().findSystemWorkflowScheme());
            dependencies.add(APILocator.getLanguageAPI().getDefaultLanguage());
        }

        dependencies.addAll(list(host, container, template));
        dependencies.add(htmlPageAsset);
        assertAll(config, dependencies);
    }

    /**
     * Method to Test: {@link DependencyBundler#generate(BundleOutput, BundlerStatus)}
     * When:
     * - Have a Page with all its dependencies: Template, Containers, Host.
     * - Template have a moddate before the last Push operation.
     * - Add the Page into a bundle.
     * Should: Should exclude the template from the bundle but include all the templates's dependencies
     */
    @Test
    @UseDataProvider("configs")
    public void includeDependenciesEvenWhenAssetExcludeByModDate(ModDateTestData modDateTestData)
            throws DotBundleException, DotDataException, DotSecurityException, IOException {

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptorAllDependencies);

        final Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        final Map<String, Object> pageAndDependencies = pageWithDependencies(yesterday.getTime());

        final Host host = (Host) pageAndDependencies.get("host");
        final Container container = (Container) pageAndDependencies.get("container");
        final Template template = (Template) pageAndDependencies.get("template");
        final HTMLPageAsset htmlPageAsset = (HTMLPageAsset) pageAndDependencies.get("htmlPageAsset");

        final Map<String, Object> pushContext = createPushContext(modDateTestData, htmlPageAsset);

        final Environment environment = (Environment) pushContext.get("environment");
        final PushPublishingEndPoint publishingEndPoint = (PushPublishingEndPoint) pushContext.get("publishingEndPoint");
        final PushPublisherConfig config = (PushPublisherConfig) pushContext.get("config");
        final Bundle bundle = (Bundle) pushContext.get("bundle");

        final BundleOutput bundleOutput = new DirectoryBundleOutput(config);

        yesterday.add(Calendar.HOUR, 2);

        createPushAsset(
                yesterday.getTime(),
                template.getIdentifier(),
                "template",
                environment,
                publishingEndPoint,
                bundle, Publisher.class);

        final BundleFactoryImpl bundleFactory = new BundleFactoryImpl();
        bundleFactory.saveBundleEnvironment(bundle, environment);

        try (ManifestBuilder manifestBuilder = new TestManifestBuilder()) {
            config.setManifestBuilder(manifestBuilder);
            bundler.setConfig(config);
            bundler.generate(bundleOutput, status);
        }

        final Collection<Object> dependencies = new HashSet<>();

        if (modDateTestData.operation == Operation.PUBLISH) {
            dependencies.addAll(getLanguagesVariableDependencies(
                    true, false, false));

            final ContentType pageContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(htmlPageAsset.getStructureInode());
            dependencies.add(pageContentType);

            dependencies.add(APILocator.getWorkflowAPI().findSystemWorkflowScheme());
            dependencies.add(APILocator.getLanguageAPI().getDefaultLanguage());

            dependencies.addAll(list(host, container));
        }

        if (modDateTestData.isDownload || modDateTestData.isForcePush) {
            dependencies.add(template);
        }

        dependencies.add(htmlPageAsset);
        assertAll(config, dependencies);
    }

    /**
     * Method to Test: {@link DependencyBundler#generate(BundleOutput, BundlerStatus)}
     * When:
     * - Have a Page with all its dependencies: Template, Containers, Host.
     * - Template is exclude by Filter.
     * - Add the Page into a bundle.
     * Should: Should exclude the template and all the templates's dependencies from the bundle
     */
    @Test
    @UseDataProvider("configs")
    public void excludeDependenciesWhenAssetExcludeByFilter(ModDateTestData modDateTestData)
            throws DotBundleException, DotDataException, DotSecurityException, IOException {

        final FilterDescriptor filterDescriptor = new FilterDescriptorDataGen()
                .dependencies(true)
                .relationships(true)
                .excludeDependencyClasses(list("Template"))
                .nextPersisted();

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);

        final Map<String, Object> pageAndDependencies = pageWithDependencies();

        final Host host = (Host) pageAndDependencies.get("host");
        final Container container = (Container) pageAndDependencies.get("container");
        final Template template = (Template) pageAndDependencies.get("template");
        final HTMLPageAsset htmlPageAsset = (HTMLPageAsset) pageAndDependencies.get("htmlPageAsset");

        final Map<String, Object> pushContext = createPushContext(modDateTestData, htmlPageAsset);

        final Environment environment = (Environment) pushContext.get("environment");
        final PushPublishingEndPoint publishingEndPoint = (PushPublishingEndPoint) pushContext.get("publishingEndPoint");
        final PushPublisherConfig config = (PushPublisherConfig) pushContext.get("config");
        final Bundle bundle = (Bundle) pushContext.get("bundle");

        final BundleOutput bundleOutput = new DirectoryBundleOutput(config);

        final BundleFactoryImpl bundleFactory = new BundleFactoryImpl();
        bundleFactory.saveBundleEnvironment(bundle, environment);

        try (ManifestBuilder manifestBuilder = new TestManifestBuilder()) {
            config.setManifestBuilder(manifestBuilder);
            bundler.setConfig(config);
            bundler.generate(bundleOutput, status);
        }

        final Collection<Object> dependencies = new HashSet<>();

        if (modDateTestData.operation == Operation.PUBLISH) {
            dependencies.addAll(getLanguagesVariableDependencies(
                    true, false, false));

            final ContentType pageContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(htmlPageAsset.getStructureInode());
            dependencies.add(pageContentType);

            dependencies.add(APILocator.getWorkflowAPI().findSystemWorkflowScheme());
            dependencies.add(APILocator.getLanguageAPI().getDefaultLanguage());

            dependencies.addAll(list(host));
        }

        dependencies.add(htmlPageAsset);
        assertAll(config, dependencies);
    }

    /**
     * Method to Test: {@link DependencyBundler#generate(BundleOutput, BundlerStatus)}
     * When:  {@link HTMLPageAsset}'s template has a modDate before the last Push Publish for one environment,
     * but exists another environment
     * should: the {@link Template} should be include in the bundle
     */
    @Test
    public void includeTemplateUsingTwoEnvironment()
            throws DotBundleException, DotDataException, DotSecurityException, IOException {

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptorAllDependencies);

        final Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        final Host host = new SiteDataGen().nextPersisted();

        final Template template = new TemplateDataGen()
                .modDate(yesterday.getTime())
                .host(host)
                .nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setDownloading(false);
        config.setOperation(Operation.PUBLISH);

        final Environment environment_1 = new EnvironmentDataGen().nextPersisted();
        final Environment environment_2 = new EnvironmentDataGen().nextPersisted();

        final PushPublishingEndPoint publishingEndPoint_1 = new PushPublishingEndPointDataGen()
                .environment(environment_1)
                .nextPersisted();

        final PushPublishingEndPoint publishingEndPoint_2 = new PushPublishingEndPointDataGen()
                .environment(environment_2)
                .nextPersisted();

        final Bundle bundle = new BundleDataGen()
                .pushPublisherConfig(config)
                .filter(filterDescriptorAllDependencies)
                .downloading(false)
                .addAssets(set(htmlPageAsset))
                .operation(Operation.PUBLISH)
                .forcePush(false)
                .nextPersisted();

        final BundleOutput bundleOutput = new DirectoryBundleOutput(config);

        yesterday.add(Calendar.HOUR, 2);
        createPushAsset(
                yesterday.getTime(),
                template.getIdentifier(),
                "template",
                environment_1,
                publishingEndPoint_1,
                bundle, Publisher.class);

        final BundleFactoryImpl bundleFactory = new BundleFactoryImpl();
        bundleFactory.saveBundleEnvironment(bundle, environment_1);
        bundleFactory.saveBundleEnvironment(bundle, environment_2);

        try (ManifestBuilder manifestBuilder = new TestManifestBuilder()) {
            config.setManifestBuilder(manifestBuilder);
            bundler.setConfig(config);
            bundler.generate(bundleOutput, status);
        }

        final ContentType pageContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(htmlPageAsset.getStructureInode());

        final Collection<Object> dependencies = list(
                pageContentType, host, template, htmlPageAsset,
                APILocator.getWorkflowAPI().findSystemWorkflowScheme(),
                APILocator.getLanguageAPI().getDefaultLanguage()
        );

        dependencies.addAll(getLanguagesVariableDependencies(
                true, false, false));

        assertAll(config, dependencies);

        final List<PushedAsset> allPushedAssets = APILocator.getPushedAssetsAPI()
                .getPushedAssets(template.getIdentifier());
        final List<String> newBundlePushedAssets = allPushedAssets.stream()
                .filter(pushedAsset -> pushedAsset.getPushDate().getTime() != yesterday.getTimeInMillis())
                .map(pushedAsset -> pushedAsset.getEnvironmentId())
                .collect(toList());

        assertEquals(2, newBundlePushedAssets.size());
        assertTrue(newBundlePushedAssets.contains(environment_2.getId()));
        assertTrue(newBundlePushedAssets.contains(environment_1.getId()));

        final List<String> oldBundlePushedAssets = allPushedAssets.stream()
                .filter(pushedAsset -> pushedAsset.getPushDate().getTime() == yesterday.getTimeInMillis())
                .map(pushedAsset -> pushedAsset.getEnvironmentId())
                .collect(toList());

        assertEquals(1, oldBundlePushedAssets.size());
        assertTrue(oldBundlePushedAssets.contains(environment_1.getId()));
    }

    private void assertAll(final PushPublisherConfig config, final Collection<Object> dependenciesToAssert) {
        AssignableFromMap<Integer> counts = new AssignableFromMap<>();
        Set<String> alreadyCounts = new HashSet<>();

        for (Object asset : dependenciesToAssert) {
            if ( Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_STRUCTURES", false) &&
                    Relationship.class.isInstance(asset)) {
                continue;
            }

            final boolean justExactlyClass = Host.class == asset.getClass() || Folder.class == asset.getClass();
            final BundleDataGen.MetaData metaData = BundleDataGen.howAddInBundle.get(asset.getClass(), null, justExactlyClass);

            final String assetId = metaData.dataToAdd.apply(asset);

            if (!alreadyCounts.contains(assetId)) {
                assertTrue(String.format("Not Contain %s in %s Class %s", assetId, metaData.collection.apply(config), asset.getClass()),
                        metaData.collection.apply(config).contains(assetId));

                final Class key = BundleDataGen.howAddInBundle.getKey(asset.getClass());
                counts.addOrUpdate(key, 1, (Integer value) -> value + 1);
                alreadyCounts.add(assetId);
            }
        }

        for (Class clazz : BundleDataGen.howAddInBundle.keySet()) {
            final boolean justExactlyClass = Host.class == clazz || Folder.class == clazz;

            final Integer expectedCount = counts.get(clazz, 0, justExactlyClass);

            final BundleDataGen.MetaData metaData = BundleDataGen.howAddInBundle.get(clazz, null, justExactlyClass);
            final Integer count = metaData.collection.apply(config).size();

            assertEquals(String.format("Expected %d not %d to %s: %s", expectedCount, count, clazz.getSimpleName(),
                    metaData.collection.apply(config).stream()
                            .map(object -> object.toString())
                            .collect(joining(","))),
                    expectedCount, count);
        }
    }

    private static class ModDateTestData {

        /**
         * True if {@link Bundle#isForcePush()} have to be true in te test
         */
        private boolean isForcePush;
        /**
         * True if {@link PushPublisherConfig#isDownloading()} have to be true in the test
         */
        private boolean isDownload;
        /**
         * Value to set in {@link Bundle#setOperation(Integer)}
         */
        private Operation operation;

        public ModDateTestData(
                boolean isForcePush,
                boolean isDownload,
                Operation operation) {

            this.isForcePush = isForcePush;
            this.isDownload = isDownload;
            this.operation = operation;
        }
    }

    private static class TestData {
        ManifestItem assetsToAddInBundle;
        Map<ManifestItem, Collection<ManifestItem>> dependenciesToAssert;
        FilterDescriptor filterDescriptor;
        Map<String, List<ManifestItem>> excludes;
        Map<String, List<String>> evaluateReasons;
        String message;

        public TestData(
                final ManifestItem assetsToAddInBundle,
                final Map<ManifestItem, Collection<ManifestItem>> dependenciesToAssert,
                final FilterDescriptor filterDescriptor,
                final String message)  {
            this(assetsToAddInBundle, dependenciesToAssert, null, filterDescriptor, message);
        }

        public TestData(
                final ManifestItem assetsToAddInBundle,
                final Map<ManifestItem, Collection<ManifestItem>> dependenciesToAssert,
                final Map<String, List<ManifestItem>> excludes,
                final FilterDescriptor filterDescriptor,
                final String message) {
            this(assetsToAddInBundle, dependenciesToAssert, excludes,
                    null, filterDescriptor, message);
        }

        public TestData(
            final ManifestItem assetsToAddInBundle,
            final Map<ManifestItem, Collection<ManifestItem>> dependenciesToAssert,
            final Map<String, List<ManifestItem>> excludes,
            final Map<String, List<String>> evaluateReasons,
            final FilterDescriptor filterDescriptor,
            final String message)  {

            this.assetsToAddInBundle = assetsToAddInBundle;
            this.filterDescriptor = filterDescriptor;
            this.dependenciesToAssert = dependenciesToAssert;
            this.excludes = excludes;
            this.evaluateReasons = evaluateReasons;
            this.message = message;
        }

        public Collection<ManifestItem> dependencies(){
            return dependenciesToAssert.values().stream()
                    .flatMap(dependencies -> dependencies.stream())
                    .collect(Collectors.toSet());
        }

        public ManifestItemsMapTest manifestLines() {
            final ManifestItemsMapTest manifestItemsMap = new ManifestItemsMapTest();
            final ManifestItem assetManifestItem = (ManifestItem) assetsToAddInBundle;
            manifestItemsMap.add(assetManifestItem, "Added directly by User");

            manifestItemsMap.addDependencies(dependenciesToAssert);

            if (excludes != null) {
                if (evaluateReasons == null) {
                    manifestItemsMap.addExcludes(excludes, assetManifestItem);
                } else {
                    for (final Map.Entry<String, List<ManifestItem>> excludeEntry : excludes.entrySet()) {
                        final String excludeReason = excludeEntry.getKey();
                        final List<ManifestItem> excludeList = excludeEntry.getValue();
                        for (final ManifestItem excludeItem : excludeList) {
                            final String itemId = excludeItem.getManifestInfo().id();
                            final List<String> evaluateReasonList = evaluateReasons
                                    .getOrDefault(itemId,
                                            list(getDependencyReason(assetManifestItem)));
                            for (final String evaluateReason : evaluateReasonList) {
                                manifestItemsMap.addExclude(excludeItem,
                                        evaluateReason, excludeReason);
                            }
                        }
                    }
                }
            }

            return manifestItemsMap;
        }

        @Override
        public String toString() {
            return message;
        }
    }
}
