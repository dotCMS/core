package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.*;
import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleFactoryImpl;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.*;
import com.dotcms.publishing.PublisherConfig.Operation;
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
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageDataGen;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Config;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.collection.Stream;
import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.*;

import static com.dotcms.publishing.PublisherAPIImplTest.getLanguageVariables;
import static com.dotcms.publishing.PublisherAPIImplTest.getLanguagesVariableDependencies;
import static com.dotcms.util.CollectionsUtils.*;
import static com.dotmarketing.business.ThemeAPI.THEME_PNG;
import static java.util.stream.Collectors.*;
import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class DependencyBundlerTest {

    private static  Map<String, List<ManifestItem>> excludeSystemFolder;

    public static final String EXCLUDE_SYSTEM_FOLDER_HOST = "Exclude System Folder/Host";
    private static String FILTER_EXCLUDE_REASON = "Exclude by filter";
    private static String FILTER_EXCLUDE_BY_OPERATION = "Exclude by Operation: ";
    private BundlerStatus status = null;

    private DependencyBundler bundler = null;

    private static FilterDescriptor filterDescriptorAllDependencies;
    private static FilterDescriptor filterDescriptorNotDependencies;
    private static FilterDescriptor filterDescriptorNotRelationship;
    private static FilterDescriptor filterDescriptorNotDependenciesRelationship;

    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        excludeSystemFolder = map(EXCLUDE_SYSTEM_FOLDER_HOST,
                list(APILocator.getFolderAPI().findSystemFolder()));

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
    public void initTest() throws IOException {
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

        return all.toArray();
    }

    private static Collection<TestData> createContentletWithThirdPartyTestCase()
            throws DotDataException, DotSecurityException {

        final Host host = createHostWithDependencies();
        final Language language = new LanguageDataGen().nextPersisted();

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


        final Map<ManifestItem, Collection<ManifestItem>> dependencies = map(
                content, list(host, language, contentType, relationship),
                contentletChild, list(language, contentletChild)
        );

        dependencies.putAll(contentTypeWithDependencies.dependenciesToAssert);
        dependencies.get(relationship).add(contentletChild);
        dependencies.get(relationship).add(contentType);

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

        final Map<ManifestItem, Collection<ManifestItem>> dependenciesWithFolder = map(
                contentWithFolder, list(folder, folder.getHost(), language, contentTypeWithFolder, relationshipWithFolder)
        );
        dependenciesWithFolder.putAll(contentTypeWithDependenciesWithFolder.dependenciesToAssert);
        dependenciesWithFolder.get(relationshipWithFolder).add(contentTypeWithFolder);

        return list(
                new TestData(content, dependencies, excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(contentWithFolder, dependenciesWithFolder, excludeSystemFolder, filterDescriptorAllDependencies)
        );
    }

    private static Collection<TestData> createRuleWithThirdPartyTestCase()
            throws DotDataException, DotSecurityException {

        final Host host = createHostWithDependencies();
        final Rule rule = new RuleDataGen().host(host).nextPersisted();

        final TestData htmlPageWithDependencies = createHTMLPageWithDependencies();
        final HTMLPageAsset htmlPageAsset = (HTMLPageAsset) htmlPageWithDependencies.assetsToAddInBundle;
        final Rule ruleWithPage = new RuleDataGen().page(htmlPageAsset).nextPersisted();

        return list(
                new TestData(rule, map(rule, list(rule, host)), excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(ruleWithPage, map(ruleWithPage, list(htmlPageAsset)), excludeSystemFolder, filterDescriptorAllDependencies)
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

        final Map<ManifestItem, Collection<ManifestItem>> dependencies = map(link, list(host, folder));

        return list(
                new TestData(link, dependencies, excludeSystemFolder, filterDescriptorAllDependencies)
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

        final Map<ManifestItem, Collection<ManifestItem>> dependencies = map(
                folder, list(host, folderContentType, contentType, subFolder, parentFolder));
        dependencies.putAll(contentTypeWithDependencies.dependenciesToAssert);
        dependencies.putAll(subFolderWithDependencies.dependenciesToAssert);

        return list(
                new TestData(folder, dependencies, excludeSystemFolder, filterDescriptorAllDependencies)
        );
    }

    private static TestData createHTMLPageWithDependencies() throws DotDataException {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final Host host = new SiteDataGen().nextPersisted();
        final ContentType contentTypeToPage = new ContentTypeDataGen().host(host).nextPersisted();
        final Container container = new ContainerDataGen().withContentType(contentTypeToPage, "").nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen().withContainer(container).next();
        final Template template = new TemplateDataGen().drawedBody(templateLayout).nextPersisted();

        final Contentlet htmlPageAsset = new HTMLPageDataGen(host, template).host(host).languageId(defaultLanguage.getId()).nextPersisted();
        final ContentType htmlPageAssetContentType = htmlPageAsset.getContentType();

        return new TestData(htmlPageAsset, map(
                htmlPageAsset, list(defaultLanguage, host, template, htmlPageAssetContentType),
                template, list(container),
                container, list(contentTypeToPage)
            ), null);
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


        final Map<ManifestItem, Collection<ManifestItem>> dependencies =
                map(containerWithContentType, list(host, contentType));
        dependencies.putAll(contentTypeWithDependencies.dependenciesToAssert);

        return list(
                new TestData(containerWithContentType, dependencies, excludeSystemFolder, filterDescriptorAllDependencies)
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

        final Map<ManifestItem, Collection<ManifestItem>> templateIncludes = map(
                template, list(host, container),
                container, list(contentType)
        );
        templateIncludes.putAll(contentTypeWithDependencies.dependenciesToAssert);

        return list(
                new TestData(template, templateIncludes, excludeSystemFolder, filterDescriptorAllDependencies)
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
                new TestData(contentType, map(contentType, list(host, systemWorkflowScheme)), excludeSystemFolder,
                        filterDescriptorAllDependencies),
                new TestData(contentTypeWithFolder, map(
                        contentTypeWithFolder, list(folderHost, contentTypeFolder, systemWorkflowScheme)
                    ), excludeSystemFolder, filterDescriptorAllDependencies)
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

        return new TestData(folder, map(
                folder, list(host, parentFolder, contentType, contentlet, link, subFolder),
                contentlet, list(language),
                subFolder, list(contentlet_2, folderContentType),
                contentType, list(systemWorkflowScheme),
                folderContentType, list(systemWorkflowScheme)
        ), null);
    }

    private static TestData createContentTypeWithDependencies() throws DotDataException, DotSecurityException {
        return createContentTypeWithDependencies(null);
    }

    private static TestData createContentTypeWithDependencies(Folder folder) throws DotDataException, DotSecurityException {
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

        return new TestData(contentType, map(
                contentType, list(host, workflowScheme, category, systemWorkflowScheme, relationship),
                contentTypeChild, list(host, contentTypeChild, systemWorkflowScheme),
                relationship, list(contentTypeChild)
        ), null);
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
        final Language language = new LanguageDataGen().nextPersisted();
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
                .languageId(language.getId())
                .host(host)
                .nextPersisted();

        final Contentlet contentletWithRelationship = new ContentletDataGen(contentTypeParent.id())
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

        final Language imageFileLanguage = new LanguageDataGen().nextPersisted();
        final Folder imageFolder = new FolderDataGen().site(host).nextPersisted();
        File tempFile = File.createTempFile("testFileAsset2-es", ".jpg");
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

        final Map<ManifestItem, Collection<ManifestItem>> contentletInclude = map(
                contentlet, list(host, contentType, language),
                contentType, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>> contentletWithFolderIncludes = map(
                contentletWithFolder, list(host, contentType, language, folder),
                contentType, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>> contentletWithRelationshipIncludes = map(
                contentletWithRelationship, list(host, relationship, contentTypeParent, language),
                relationship, list(contentTypeParent, contentTypeChild, contentletChild),
                contentletChild, list(language, contentTypeChild),
                contentTypeParent, list(systemWorkflowScheme),
                contentTypeChild, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>> contentWithCategoryIncludes = map(
                contentWithCategory, list(host, contentTypeWithCategory, language, category),
                contentTypeWithCategory, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>> htmlPageAssetIncludes = map(
                htmlPageAsset, list(host, defaultLanguage, template, htmlPageAssetContentType),
                template, list(container, defaultHost),
                contentTypeToPage, list(systemWorkflowScheme),
                container, list(contentTypeToPage)
        );

        final Map<ManifestItem, Collection<ManifestItem>>  contentletWithImageIncludes = map(
                contentletWithImage, list(host, contentTypeWithImageField, imageFileAsset, language),
                imageFileAsset, list(imageFolder, imageFileLanguage, imageFileAsset.getContentType())
        );

        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

        final Map<String, List<ManifestItem>> contentletWithFolderExcludes = map(FILTER_EXCLUDE_REASON,
                list(host, contentType, language, folder));

        final Map<String, List<ManifestItem>> contentletWithRelationshipExcludes = map(
                FILTER_EXCLUDE_REASON, list(host, relationship, contentTypeParent, language),
                EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder));

        final Map<String, List<ManifestItem>> contentWithCategoryExcludes = map(FILTER_EXCLUDE_REASON,
                list(host, contentTypeWithCategory, language, category), EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder));

        final Map<String, List<ManifestItem>> htmlPageAssetExcludes = map(FILTER_EXCLUDE_REASON,
                list(host, defaultLanguage, template, htmlPageAssetContentType), EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder));

        final Map<String, List<ManifestItem>>  contentletWithImageExcludes =  map(FILTER_EXCLUDE_REASON,
                list(host, contentTypeWithImageField, imageFileAsset, language), EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder));

        return list(
                new TestData(contentlet, contentletInclude, excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(contentlet, map(),
                        map(FILTER_EXCLUDE_REASON, list(host, contentType, language), EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)),
                        filterDescriptorNotDependencies),
                new TestData(contentlet, contentletInclude, excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(contentlet, map(),
                        map(FILTER_EXCLUDE_REASON, list(host, contentType, language), EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)),
                        filterDescriptorNotDependenciesRelationship),

                new TestData(contentletWithFolder, contentletWithFolderIncludes, excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(contentletWithFolder, map(), contentletWithFolderExcludes, filterDescriptorNotDependencies),
                new TestData(contentletWithFolder,contentletWithFolderIncludes, excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(contentletWithFolder, map(), contentletWithFolderExcludes, filterDescriptorNotDependenciesRelationship)    ,

                new TestData(contentletWithRelationship, contentletWithRelationshipIncludes, excludeSystemFolder,
                        filterDescriptorAllDependencies),
                new TestData(contentletWithRelationship, map(), contentletWithRelationshipExcludes, filterDescriptorNotDependencies),
                new TestData(contentletWithRelationship, map(contentletWithRelationship, list(host, contentTypeParent, language)),
                        map(FILTER_EXCLUDE_REASON, list(relationship), EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)),
                        filterDescriptorNotRelationship),
                new TestData(contentletWithRelationship, map(), contentletWithRelationshipExcludes,
                        filterDescriptorNotDependenciesRelationship),

                new TestData(contentWithCategory, contentWithCategoryIncludes, excludeSystemFolder,
                        filterDescriptorAllDependencies),
                new TestData(contentWithCategory, map(),
                        map(FILTER_EXCLUDE_REASON, list(host, contentTypeWithCategory, language, category),
                                EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)),
                        filterDescriptorNotDependencies),
                new TestData(contentWithCategory, contentWithCategoryIncludes, excludeSystemFolder,
                        filterDescriptorNotRelationship),
                new TestData(contentWithCategory, map(), contentWithCategoryExcludes, filterDescriptorNotDependenciesRelationship),

                new TestData(htmlPageAsset, htmlPageAssetIncludes, excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(htmlPageAsset, map(), htmlPageAssetExcludes, filterDescriptorNotDependencies),
                new TestData(htmlPageAsset, htmlPageAssetIncludes, excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(htmlPageAsset, map(), htmlPageAssetExcludes, filterDescriptorNotDependenciesRelationship),

                new TestData(contentletWithImage, contentletWithImageIncludes, excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(contentletWithImage, map(), contentletWithImageExcludes, filterDescriptorNotDependencies),
                new TestData(contentletWithImage, contentletWithImageIncludes, excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(contentletWithImage, map(), contentletWithImageExcludes, filterDescriptorNotDependenciesRelationship)
        );
    }

    private static Collection<TestData> createRuleTestCase() {
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule = new RuleDataGen().host(host).nextPersisted();

        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        final Rule ruleWithPage = new RuleDataGen().page(htmlPageAsset).nextPersisted();

        return list(
                new TestData(rule, map(rule, list(host)), excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(rule, map(), map(FILTER_EXCLUDE_REASON, list(host)), filterDescriptorNotDependencies),
                new TestData(rule, map(rule, list(host)), excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(rule, map(), map(FILTER_EXCLUDE_REASON, list(host)), filterDescriptorNotDependenciesRelationship),

                new TestData(ruleWithPage, map(ruleWithPage, list(htmlPageAsset)), excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(ruleWithPage, map(), map(FILTER_EXCLUDE_REASON, list(htmlPageAsset)), filterDescriptorNotDependencies),
                new TestData(ruleWithPage, map(ruleWithPage, list(htmlPageAsset)), excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(ruleWithPage, map(), map(FILTER_EXCLUDE_REASON, list(htmlPageAsset)), filterDescriptorNotDependenciesRelationship)
        );
    }

    private static Collection<TestData> createLanguageTestCase() {
        final Language language = new LanguageDataGen().nextPersisted();

        return list(
                new TestData(language, map(), excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(language, map(), filterDescriptorNotDependencies),
                new TestData(language, map(), excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(language, map(), filterDescriptorNotDependenciesRelationship)
        );

    }

    private static Collection<TestData> createWorkflowTestCase() {
        final WorkflowScheme workflowScheme = new WorkflowDataGen().nextPersisted();
        final WorkflowStep workflowStep = new WorkflowStepDataGen(workflowScheme.getId()).nextPersisted();
        final WorkflowAction workflowAction = new WorkflowActionDataGen(workflowScheme.getId(), workflowStep.getId())
                .nextPersisted();

        return list(
                new TestData(workflowScheme, map(), excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(workflowScheme, map(), filterDescriptorNotDependencies),
                new TestData(workflowScheme, map(), excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(workflowScheme, map(), filterDescriptorNotDependenciesRelationship)
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

        final Map<ManifestItem, Collection<ManifestItem>> hostWithContentIncludes = map(
                hostWithContent, list(contentType, contentlet),
                contentType, list(systemWorkflowScheme),
                contentlet, list(language)
        );

        final Map<ManifestItem, Collection<ManifestItem>> hostWithFolderInclude = map(
                hostWithFolder, list(folder),
                folder, list(folderContentType),
                folderContentType, list(systemWorkflowScheme)
        );

        return list(
                new TestData(host, map(), excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(host, map(), map(), filterDescriptorNotDependencies),
                new TestData(host, map(), excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(host, map(), filterDescriptorNotDependenciesRelationship),

                new TestData(hostWithTemplate, map(hostWithTemplate, list(template)),
                        excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(hostWithTemplate, map(), map(FILTER_EXCLUDE_REASON, list(template)),
                        filterDescriptorNotDependencies),
                new TestData(hostWithTemplate, map(hostWithTemplate, list(template)), excludeSystemFolder,
                        filterDescriptorNotRelationship),
                new TestData(hostWithTemplate, map(), map(FILTER_EXCLUDE_REASON, list(template)),
                        filterDescriptorNotDependenciesRelationship),

                new TestData(hostWithContainer, map(hostWithContainer, list(container)), excludeSystemFolder,
                        filterDescriptorAllDependencies),
                new TestData(hostWithContainer, map(), map(FILTER_EXCLUDE_REASON, list(container)),
                        filterDescriptorNotDependencies),
                new TestData(hostWithContainer, map(hostWithContainer, list(container)), excludeSystemFolder,
                        filterDescriptorNotRelationship),
                new TestData(hostWithContainer, map(), map(FILTER_EXCLUDE_REASON, list(container)),
                        filterDescriptorNotDependenciesRelationship),

                new TestData(hostWithContent, hostWithContentIncludes, excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(hostWithContent, map(), map(FILTER_EXCLUDE_REASON, list(contentType, contentlet)),
                        filterDescriptorNotDependencies),
                new TestData(hostWithContent, hostWithContentIncludes, excludeSystemFolder,
                        filterDescriptorNotRelationship),
                new TestData(hostWithContent, map(), map(FILTER_EXCLUDE_REASON, list(contentType, contentlet)),
                        filterDescriptorNotDependenciesRelationship),

                new TestData(hostWithFolder, hostWithFolderInclude, excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(hostWithFolder, map(), map(FILTER_EXCLUDE_REASON, list(folder)),
                        filterDescriptorNotDependencies),
                new TestData(hostWithFolder, hostWithFolderInclude, excludeSystemFolder,
                        filterDescriptorNotRelationship),
                new TestData(hostWithFolder, map(), map(FILTER_EXCLUDE_REASON, list(folder)),
                        filterDescriptorNotDependenciesRelationship),

                new TestData(hostWithRule, map(hostWithRule, list(rule)), excludeSystemFolder,
                        filterDescriptorAllDependencies),
                new TestData(hostWithRule, map(), map(FILTER_EXCLUDE_REASON, list(rule)),filterDescriptorNotDependencies),
                new TestData(hostWithRule, map(hostWithRule, list(rule)), excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(hostWithRule, map(), map(FILTER_EXCLUDE_REASON, list(rule)), filterDescriptorNotDependenciesRelationship)
        );
    }

    private static Collection<TestData> createLinkTestCase() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final Link link = new LinkDataGen(folder)
                .hostId(host.getIdentifier())
                .nextPersisted();

        return list(
                new TestData(link, map(link, list(host, folder)), excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(link, map(), map(FILTER_EXCLUDE_REASON, list(host, folder)), filterDescriptorNotDependencies),
                new TestData(link, map(link, list(host, folder)), excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(link, map(), map(FILTER_EXCLUDE_REASON, list(host, folder)), filterDescriptorNotDependenciesRelationship)
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

        final Map<ManifestItem, Collection<ManifestItem>> folderIncludes = map(
                folder, list(host, folderContentType),
                folderContentType, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>> folderWithParentIncludes = map(
                folderWithParent, list(host, folderContentType, parentFolder),
                folderContentType, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>> folderWithContentTypeIncludes = map(
                folderWithContentType, list(host, folderContentType, contentType),
                folderContentType, list(systemWorkflowScheme),
                contentType, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>> folderWithContentIncludes = map(
                folderWithContent, list(host, folderContentType, contentlet),
                contentlet, list(contentlet, language, contentlet.getContentType()),
                contentlet.getContentType(), list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>>  folderWithLinkIncludes = map(
                folderWithLink, list(host, folderContentType, link, systemWorkflowScheme),
                folderContentType, list(systemWorkflowScheme)
        );

        final Map<ManifestItem, Collection<ManifestItem>>  folderWithSubFolderIncludes = map(
                folderWithSubFolder, list(host, folderContentType, subFolder),
                folderContentType, list(systemWorkflowScheme),
                subFolder, list(contentlet_2),
                contentlet_2, list(language)
        );

        //Folder with sub folder
        return list(
                new TestData(folder, folderIncludes, excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(folder, map(), map(FILTER_EXCLUDE_REASON, list(host, folderContentType)),
                        filterDescriptorNotDependencies),
                new TestData(folder, folderIncludes, excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(folder, map(), map(FILTER_EXCLUDE_REASON, list(host, folderContentType)),
                        filterDescriptorNotDependenciesRelationship),

                //Dependency manager not add Parent Folder, the Parent Folder is added as dependency in FolderBundle
                new TestData(folderWithParent, folderWithParentIncludes, excludeSystemFolder,
                        filterDescriptorAllDependencies),
                new TestData(folderWithParent, map(), map(FILTER_EXCLUDE_REASON, list(host, folderContentType, parentFolder)),
                        filterDescriptorNotDependencies),
                new TestData(folderWithParent, folderWithParentIncludes, excludeSystemFolder,
                        filterDescriptorNotRelationship),
                new TestData(folderWithParent, map(), map(FILTER_EXCLUDE_REASON, list(host, folderContentType, parentFolder)),
                        filterDescriptorNotDependenciesRelationship),

                new TestData(folderWithContentType, folderWithContentTypeIncludes, excludeSystemFolder,
                        filterDescriptorAllDependencies),
                new TestData(folderWithContentType, map(),
                        map(FILTER_EXCLUDE_REASON, list(host, folderContentType, contentType)), filterDescriptorNotDependencies),
                new TestData(folderWithContentType, folderWithContentTypeIncludes, excludeSystemFolder,
                        filterDescriptorNotRelationship),
                new TestData(folderWithContentType, map(),
                        map(FILTER_EXCLUDE_REASON, list(host, folderContentType, contentType)),
                        filterDescriptorNotDependenciesRelationship),

                new TestData(folderWithContent, folderWithContentIncludes, excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(folderWithContent, map(), map(FILTER_EXCLUDE_REASON, list(host, folderContentType, contentlet)),
                        filterDescriptorNotDependencies),
                new TestData(folderWithContent, folderWithContentIncludes, excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(folderWithContent, map(), map(FILTER_EXCLUDE_REASON, list(host, folderContentType, contentlet)),
                        filterDescriptorNotDependenciesRelationship),

                new TestData(folderWithLink, folderWithLinkIncludes, excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(folderWithLink, map(), map(FILTER_EXCLUDE_REASON, list(host, folderContentType, link)),
                        filterDescriptorNotDependencies),
                new TestData(folderWithLink, folderWithLinkIncludes, excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(folderWithLink, map(), map(FILTER_EXCLUDE_REASON, list(host, folderContentType, link)),
                        filterDescriptorNotDependenciesRelationship),

                new TestData(folderWithSubFolder, folderWithSubFolderIncludes, excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(folderWithSubFolder, map(), map(FILTER_EXCLUDE_REASON, list(host, folderContentType, subFolder)),
                        filterDescriptorNotDependencies),
                new TestData(folderWithSubFolder,folderWithSubFolderIncludes, excludeSystemFolder,
                        filterDescriptorNotRelationship),
                new TestData(folderWithSubFolder, map(), map(FILTER_EXCLUDE_REASON, list(host, folderContentType, subFolder)),
                        filterDescriptorNotDependenciesRelationship)
        );
    }

    private static Collection<TestData> createContainerTestCase() throws DotDataException, DotSecurityException {
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

        final Map<ManifestItem, Collection<ManifestItem>> containerWithContentTypeIncludes = map(
                containerWithContentType, list(host, contentType),
                contentType, list(systemWorkflowScheme)
        );

        return list(
                new TestData(containerWithoutContentType, map(containerWithoutContentType, list(host)),
                        excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(containerWithoutContentType, map(), map(FILTER_EXCLUDE_REASON, list(host)),
                        filterDescriptorNotDependencies),
                new TestData(containerWithoutContentType, map(containerWithoutContentType, list(host)),
                        excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(containerWithoutContentType,  map(), map(FILTER_EXCLUDE_REASON, list(host)),
                        filterDescriptorNotDependenciesRelationship),

                new TestData(containerWithContentType,
                        containerWithContentTypeIncludes, excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(containerWithContentType, map(), map(FILTER_EXCLUDE_REASON, list(host, contentType)),
                        filterDescriptorNotDependencies),
                new TestData(containerWithContentType,containerWithContentTypeIncludes, excludeSystemFolder,
                        filterDescriptorNotRelationship),
                new TestData(containerWithContentType, map(), map(FILTER_EXCLUDE_REASON, list(host, contentType)),
                 filterDescriptorNotDependenciesRelationship)
        );
    }

    private static List<TestData> createTemplatesTestCase() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template advancedTemplateWithContainer = new TemplateDataGen().host(host).nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().host(host).nextPersisted();

        final Container container_1 = new ContainerDataGen()
                .site(host)
                .withContentType(contentType, "")
                .nextPersisted();

        final Container container_2 = new ContainerDataGen()
                .site(host)
                .withContentType(contentType, "")
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

        final Map<ManifestItem, Collection<ManifestItem>> contentTypeParentIncludes = map(
                templateWithTemplateLayout, list(host, container_1, container_2),
                container_1, list(contentType),
                container_2, list(contentType),
                contentType, list(systemWorkflowScheme)
        );

        return list(
                new TestData(advancedTemplateWithContainer, map(advancedTemplateWithContainer, list(host)),
                        excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(advancedTemplateWithContainer, map(),  map(FILTER_EXCLUDE_REASON, list(host)),
                        filterDescriptorNotDependencies),
                new TestData(advancedTemplateWithContainer, map(advancedTemplateWithContainer, list(host)),
                        excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(advancedTemplateWithContainer, map(),  map(FILTER_EXCLUDE_REASON, list(host)),
                        filterDescriptorNotDependenciesRelationship),

                new TestData(templateWithTemplateLayout, contentTypeParentIncludes, excludeSystemFolder,
                        filterDescriptorAllDependencies),
                new TestData(templateWithTemplateLayout, map(),  map(FILTER_EXCLUDE_REASON, list(host, container_1, container_2)),
                        filterDescriptorNotDependencies),
                new TestData(templateWithTemplateLayout, contentTypeParentIncludes, excludeSystemFolder,
                        filterDescriptorNotRelationship),
                new TestData(templateWithTemplateLayout, map(),  map(FILTER_EXCLUDE_REASON, list(host, container_1, container_2)),
                        filterDescriptorNotDependenciesRelationship)
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

        final Map<ManifestItem, Collection<ManifestItem>> contentTypeWithFolderIncludes = map(
                contentTypeWithFolder, list(folder, systemWorkflowScheme, folderHost));

        final Map<ManifestItem, Collection<ManifestItem>> contentTypeWithWorkflowIncludes = map(
                contentTypeWithWorkflow, list(host, systemWorkflowScheme, workflowScheme));

        final Map<ManifestItem, Collection<ManifestItem>> contentTypeWithCategoryIncludes = map(
                contentTypeWithCategory, list(host, systemWorkflowScheme, category));

        final Map<ManifestItem, Collection<ManifestItem>> contentTypeParentIncludes = map(
                contentTypeParent, list(host, systemWorkflowScheme, relationship),
                relationship, list(contentTypeChild)
        );

        list(host, systemWorkflowScheme, category);
        return list(
                new TestData(contentType,
                        map(contentType, list(host, systemWorkflowScheme)), excludeSystemFolder, filterDescriptorAllDependencies),

                new TestData(contentType, map(),
                        map(
                            FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme),
                            EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)), filterDescriptorNotDependencies),

                new TestData(contentType,
                        map(contentType, list(host, systemWorkflowScheme)),
                        excludeSystemFolder, filterDescriptorNotRelationship),

                new TestData(contentType, map(),
                        map(FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme),
                                EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)), filterDescriptorNotDependenciesRelationship),

                new TestData(contentTypeWithFolder, contentTypeWithFolderIncludes, excludeSystemFolder,
                        filterDescriptorAllDependencies),
                new TestData(contentTypeWithFolder, map(),
                        map(FILTER_EXCLUDE_REASON, list(folder, systemWorkflowScheme, folderHost)), filterDescriptorNotDependencies),
                new TestData(contentTypeWithFolder, contentTypeWithFolderIncludes, excludeSystemFolder,
                        filterDescriptorNotRelationship),
                new TestData(contentTypeWithFolder, map(),
                        map(FILTER_EXCLUDE_REASON, list(folder, systemWorkflowScheme, folderHost)),
                        filterDescriptorNotDependenciesRelationship),

                new TestData(contentTypeWithWorkflow, contentTypeWithWorkflowIncludes, excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(contentTypeWithWorkflow, map(), map(FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme, workflowScheme),
                        EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)), filterDescriptorNotDependencies),
                new TestData(contentTypeWithWorkflow, contentTypeWithWorkflowIncludes, excludeSystemFolder, filterDescriptorNotRelationship),
                new TestData(contentTypeWithWorkflow, map(), map(FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme, workflowScheme),
                        EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)), filterDescriptorNotDependenciesRelationship),

                new TestData(contentTypeWithCategory, contentTypeWithCategoryIncludes, excludeSystemFolder,
                        filterDescriptorAllDependencies),
                new TestData(contentTypeWithCategory, map(), map(FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme, category),
                        EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)), filterDescriptorNotDependencies),
                new TestData(contentTypeWithCategory, contentTypeWithCategoryIncludes, excludeSystemFolder,
                        filterDescriptorNotRelationship),
                new TestData(contentTypeWithCategory, map(), map(FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme, category),
                        EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)), filterDescriptorNotDependenciesRelationship),

                new TestData(contentTypeParent, contentTypeParentIncludes, excludeSystemFolder, filterDescriptorAllDependencies),
                new TestData(contentTypeParent, map(), map(FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme, relationship),
                        EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)), filterDescriptorNotDependencies),
                new TestData(contentTypeParent, map(contentTypeParent, list(host, systemWorkflowScheme)),
                        map(FILTER_EXCLUDE_REASON, list(relationship), EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)),
                        filterDescriptorNotRelationship),
                new TestData(contentTypeParent, map(), map(FILTER_EXCLUDE_REASON, list(host, systemWorkflowScheme, relationship),
                        EXCLUDE_SYSTEM_FOLDER_HOST, list(systemFolder)), filterDescriptorNotDependenciesRelationship)
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
     */
    @Test
    @UseDataProvider("assets")
    public void addAssetInBundle(final TestData testData)
            throws IOException, DotBundleException, DotDataException, DotSecurityException {

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
        APILocator.getPublisherAPI().addFilterDescriptor(testData.filterDescriptor);

        final PushPublisherConfig config = new PushPublisherConfig();
        final BundleOutput bundleOutput = new DirectoryBundleOutput(config);
        final Set<Object> dependencies = new HashSet<>();

        try (CSVManifestBuilder manifestBuilder = new CSVManifestBuilder()) {

            manifestBuilder.create();
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

            final TestManifestItemsMap manifestLines = testData.manifestLines();

            if (publisherFilter.isDependencies()) {
                PublisherAPIImplTest.addLanguageVariableManifestItem(
                        manifestLines,
                        true,
                        PublisherAPIImplTest.getLanguageVariables()
                );
            }

            PublisherAPIImplTest.assertManifestFile(manifestBuilder.getManifestFile(), manifestLines);
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
     */
    @Test
    @UseDataProvider("configs")
    public void excludeContenletChildAssetByModDate(ModDateTestData modDateTestData)
            throws DotBundleException, DotDataException, DotSecurityException, IOException {

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
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

            manifestBuilder.create();
            config.setManifestBuilder(manifestBuilder);

            bundler.setConfig(config);
            bundler.generate(bundleOutput, status);

            final TestManifestItemsMap manifestLines = new TestManifestItemsMap();
            manifestLines.add(contentParent, "Add directly by User");

            final List<Contentlet> languageVariables = PublisherAPIImplTest.getLanguageVariables();
            if (modDateTestData.operation == Operation.PUBLISH) {
                dependencies.addAll(getLanguagesVariableDependencies(true, false, false));

                dependencies.addAll(list(host, language, contentTypeParent, contentTypeChild));
                dependencies.add(APILocator.getWorkflowAPI().findSystemWorkflowScheme());
                dependencies.add(language);

                manifestLines.addDependencies(map(
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
                manifestLines.addExcludes(map(excludeByOperation,
                        list(host, language, contentTypeParent, contentTypeChild, relationship,
                                APILocator.getWorkflowAPI().findSystemWorkflowScheme())));

                final List<? extends Serializable> generalLangVarDependencies = list(
                        PublisherAPIImplTest.getLanguageVariablesContentType(),
                        APILocator.getWorkflowAPI().findSystemWorkflowScheme());

                Stream.concat(PublisherAPIImplTest.getLanguagesVariableDependencies(),
                        languageVariables, generalLangVarDependencies).forEach(asset ->
                        manifestLines.addExclude((ManifestItem) asset, excludeByOperation));
            }

            dependencies.add(contentParent);
            manifestLines.addExclude(APILocator.getFolderAPI().findSystemFolder(), EXCLUDE_SYSTEM_FOLDER_HOST);
            manifestLines.addExclude(APILocator.getHostAPI().findSystemHost(), EXCLUDE_SYSTEM_FOLDER_HOST);

            if (modDateTestData.isDownload || modDateTestData.isForcePush) {
                dependencies.add(contentletChild);
                manifestLines.addDependencies(map(relationship, list(contentletChild)));

                manifestLines.addDependencies(map(contentletChild, list(language)));
            } else if (modDateTestData.operation == Operation.PUBLISH) {
                manifestLines.addExclude(contentletChild, "Exclude by mod_date");

                manifestLines.addDependencies(map(contentletChild, list(language)));
            } else {
                manifestLines.addExclude(contentletChild, FILTER_EXCLUDE_BY_OPERATION + modDateTestData.operation);
            }

            manifestBuilder.close();
            PublisherAPIImplTest.assertManifestFile(manifestBuilder.getManifestFile(), manifestLines);
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

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptorAllDependencies);

        final Map<String, Object> relationShip = createRelationShip();

        final Host host = (Host) relationShip.get("host");
        final Language language = (Language) relationShip.get("language");
        final ContentType contentTypeParent =  (ContentType) relationShip.get("contentTypeParent");
        final ContentType contentTypeChild =  (ContentType) relationShip.get("contentTypeChild");

        final Contentlet contentletChild =  (Contentlet) relationShip.get("contentletChild");
        final Contentlet contentParent = (Contentlet) relationShip.get("contentParent");

        final Contentlet contentletChildAnotherLang = ContentletDataGen.checkout(contentletChild);
        final Language anotherLang = new LanguageDataGen().nextPersisted();
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
        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
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

            return map(
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
        final Language language = new LanguageDataGen().nextPersisted();

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

        return map(
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

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
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

        final Host systemHost = APILocator.getHostAPI().findSystemHost();

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

        return map(
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

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
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

        final Host systemHost = APILocator.getHostAPI().findSystemHost();

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

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
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

        final Host systemHost = APILocator.getHostAPI().findSystemHost();

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

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
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

        final Host systemHost = APILocator.getHostAPI().findSystemHost();

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

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
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

        final Host systemHost = APILocator.getHostAPI().findSystemHost();

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
            final int count = metaData.collection.apply(config).size();

            assertEquals(String.format("Expected %d not %d to %s: ", expectedCount, count, clazz.getSimpleName(),
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

        public TestData(
                final ManifestItem assetsToAddInBundle,
                final Map<ManifestItem, Collection<ManifestItem>> dependenciesToAssert,
                final FilterDescriptor filterDescriptor)  {
            this(assetsToAddInBundle, dependenciesToAssert, null, filterDescriptor);
        }

        public TestData(
                final ManifestItem assetsToAddInBundle,
                final Map<ManifestItem, Collection<ManifestItem>> dependenciesToAssert,
                final Map<String, List<ManifestItem>> excludes,
                final FilterDescriptor filterDescriptor)  {
            this.assetsToAddInBundle = assetsToAddInBundle;
            this.filterDescriptor = filterDescriptor;
            this.dependenciesToAssert = dependenciesToAssert;
            this.excludes = excludes;
        }

        public Collection<ManifestItem> dependencies(){
            return dependenciesToAssert.values().stream()
                    .flatMap(dependencies -> dependencies.stream())
                    .collect(Collectors.toSet());
        }

        public TestManifestItemsMap manifestLines() {
            final TestManifestItemsMap manifestItemsMap = new TestManifestItemsMap();
            final ManifestItem assetManifestItem = (ManifestItem) assetsToAddInBundle;
            manifestItemsMap.add(assetManifestItem, "Add directly by User");

            manifestItemsMap.addDependencies(dependenciesToAssert);

            if (excludes != null) {
                manifestItemsMap.addExcludes(excludes);
            }

            return manifestItemsMap;
        }

        @Override
        public String toString() {
            final String dependencies = dependenciesToAssert.values().stream()
                    .map(dependecy -> dependecy.getClass().getSimpleName())
                    .collect(joining(","));

            return "[assetsToAddInBundle=" + assetsToAddInBundle.getClass().getSimpleName() + "], " +
                    "[dependenciesToAssert=" +  dependencies + "], " +
                    "filterDescriptor=" + (filterDescriptor != null ? filterDescriptor.getFilters() : "null");
        }
    }

    class TestManifestBuilder implements ManifestBuilder {

        @Override
        public void create() throws IOException {

        }

        @Override
        public <T> void include(ManifestItem manifestItem, String reason) {

        }

        @Override
        public <T> void exclude(ManifestItem manifestItem, String reason) {

        }

        @Override
        public File getManifestFile() {
            return null;
        }

        @Override
        public void close() throws IOException {

        }
    }
}
