package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeTransformer;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.*;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.DependencyManager;
import com.dotcms.publishing.*;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
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
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static com.dotcms.publishing.PublisherAPIImplTest.getLanguagesVariableDependencies;
import static com.dotcms.util.CollectionsUtils.*;
import static java.util.stream.Collectors.*;
import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;
import static org.mockito.Matchers.booleanThat;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class DependencyBundlerTest {

    private File bundleRoot = null;
    private BundlerStatus status = null;

    private DependencyBundler bundler = null;

    private static FilterDescriptor filterDescriptorAllDependencies;
    private static FilterDescriptor filterDescriptorNotDependencies;
    private static FilterDescriptor filterDescriptorNotRelationship;
    private static FilterDescriptor filterDescriptorNotDependenciesRelationship;

    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        filterDescriptorAllDependencies = new FileDescriptorDataGen().next();
        filterDescriptorNotDependencies = new FileDescriptorDataGen()
                .dependencies(false)
                .next();

        filterDescriptorNotRelationship = new FileDescriptorDataGen()
                .relationships(false)
                .next();

        filterDescriptorNotDependenciesRelationship = new FileDescriptorDataGen()
                .relationships(false)
                .dependencies(false)
                .next();
    }

    @Before
    public void initTest() throws IOException {
        bundleRoot = FileUtil.createTemporaryDirectory("DependencyBundlerTest_");
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

        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

        final List<Object> dependencies = list(host, language, contentType, contentletChild, systemFolder, contentTypeChild);
        dependencies.addAll(contentTypeWithDependencies.dependenciesToAssert);

        final TestData folderWithDependencies = createFolderWithDependencies();
        final Folder folder = (Folder) folderWithDependencies.assetsToAddInBundle ;

        final Contentlet contentWithFolder = new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .host(folder.getHost())
                .folder(folder)
                .nextPersisted();

        final List<Object> dependenciesWithFolder = list(folder, folder.getHost(), language, contentType);
        dependenciesWithFolder.addAll(contentTypeWithDependencies.dependenciesToAssert);

        return list(
                new TestData(content, dependencies, filterDescriptorAllDependencies),
                new TestData(contentWithFolder, dependenciesWithFolder, filterDescriptorAllDependencies)
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
                new TestData(rule, list(rule, host), filterDescriptorAllDependencies),
                new TestData(ruleWithPage, list(htmlPageAsset), filterDescriptorAllDependencies)
        );
    }

    private static Collection<TestData> createLinkWithThirdPartyTestCase()
            throws DotDataException, DotSecurityException {

        final Host host = createHostWithDependencies();

        final TestData folderWithDependencies = createFolderWithDependencies(null, host);
        final Folder folder = (Folder) folderWithDependencies.assetsToAddInBundle;

        final Link link = new LinkDataGen(folder)
                .hostId(host.getIdentifier())
                .nextPersisted();

        final List<Object> dependencies = list(host, folder);
        //dependencies.addAll(contentTypeWithDependencies.dependenciesToAssert);

        return list(
                new TestData(link, dependencies, filterDescriptorAllDependencies)
        );
    }

    private static Collection<TestData> createFolderWithThirdPartyTestCase()
            throws DotDataException, DotSecurityException {

        final Host host = createHostWithDependencies();

        final TestData parentFolderDependencies = createFolderWithDependencies(null, host);
        final Folder parentFolder = (Folder) parentFolderDependencies.assetsToAddInBundle;

        final Folder folder = new FolderDataGen()
                .parent(parentFolder)
                .site(host)
                .nextPersisted();

        final TestData contentTypeWithDependencies = createContentTypeWithDependencies(folder);
        final ContentType contentType = (ContentType) contentTypeWithDependencies.assetsToAddInBundle;

        final TestData subFolderWithDependencies = createFolderWithDependencies(folder);
        final Folder subFolder = (Folder) subFolderWithDependencies.assetsToAddInBundle;

        final Structure folderStructure = CacheLocator.getContentTypeCache()
                .getStructureByInode(folder.getDefaultFileType());

        final ContentType folderContentType = new StructureTransformer(folderStructure).from();

        final Host systemHost = APILocator.getHostAPI().findSystemHost();
        final List<Object> dependencies = list(host, folderContentType, systemHost);
        dependencies.addAll(contentTypeWithDependencies.dependenciesToAssert);

        return list(
                new TestData(folder, dependencies, filterDescriptorAllDependencies)
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
        final Host systemHost = APILocator.getHostAPI().findSystemHost();

        return new TestData(htmlPageAsset, list(defaultLanguage, host, contentTypeToPage, container, template,
                htmlPageAssetContentType, systemHost), null);
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


        final List<Object> dependencies = list(host, contentType);
        dependencies.addAll(contentTypeWithDependencies.dependenciesToAssert);

        return list(
                new TestData(containerWithContentType, dependencies, filterDescriptorAllDependencies)
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

        final List<Object> dependencies = list(host, contentType, container);
        dependencies.addAll(contentTypeWithDependencies.dependenciesToAssert);

        return list(
                new TestData(template, dependencies, filterDescriptorAllDependencies)
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
                .host(folderHost)
                .folder(contentTypeFolder)
                .nextPersisted();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();
        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

        return list(
                new TestData(contentType, list(host, systemWorkflowScheme, systemFolder), filterDescriptorAllDependencies),
                new TestData(contentTypeWithFolder, list(folderHost, contentTypeFolder, systemWorkflowScheme),
                        filterDescriptorAllDependencies)
        );
    }

    private static TestData createFolderWithDependencies() throws DotDataException, DotSecurityException {
        return createFolderWithDependencies(null);
    }

    private static TestData createFolderWithDependencies(final Folder folder) throws DotDataException, DotSecurityException {
        return createFolderWithDependencies(folder, null);
    }

    private static TestData createFolderWithDependencies(final Folder parentFolderParam, final Host hostParam)
            throws DotDataException, DotSecurityException {
        final Host host = hostParam == null ? new SiteDataGen().nextPersisted() : hostParam;

        final Folder parentFolder = parentFolderParam == null ? new FolderDataGen().site(host).nextPersisted() : parentFolderParam;
        final Folder folder = new FolderDataGen()
                .parent(parentFolder)
                .site(host)
                .nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .folder(folder)
                .nextPersisted();

        final File image = new File(Thread.currentThread().getContextClassLoader().getResource("images/test.jpg").getFile());
        final Contentlet contentlet = new FileAssetDataGen(folder, image)
                .host(host)
                .nextPersisted();
        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

        final Link link = new LinkDataGen(folder).nextPersisted();

        final Folder subFolder = new FolderDataGen()
                .parent(folder)
                .nextPersisted();

        final Contentlet contentlet_2 = new FileAssetDataGen(subFolder, image)
                .host(host)
                .nextPersisted();

        final Host systemHost = APILocator.getHostAPI().findSystemHost();
        final Structure folderStructure = CacheLocator.getContentTypeCache()
                .getStructureByInode(folder.getDefaultFileType());

        final ContentType folderContentType = new StructureTransformer(folderStructure).from();

        final Folder systemFolder = APILocator.getFolderAPI()
                .find(folderContentType.folder(), APILocator.systemUser(), false);

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();

        return new TestData(folder, list(host, parentFolder, contentType, contentlet, language, link, subFolder,
                contentlet_2, systemHost, folderContentType, systemFolder, systemWorkflowScheme), null);
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

        new FieldRelationshipDataGen()
                .child(contentTypeChild)
                .parent(contentType)
                .nextPersisted();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();
        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

        return new TestData(contentType, list(host, workflowScheme, category, contentTypeChild,
                systemFolder, systemWorkflowScheme), null);
    }


    private static Host createHostWithDependencies(){
        final Host host = new SiteDataGen().nextPersisted();

        new FolderDataGen().site(host).nextPersisted();
        final ContentType anotherContentType = new ContentTypeDataGen().host(host).nextPersisted();
        new ContentletDataGen(anotherContentType.id()).host(host).nextPersisted();
        new RuleDataGen().host(host).nextPersisted();

        return host;
    }

    private static Collection<TestData> createContentTestCase() throws DotDataException, DotSecurityException {
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

        final Language languageChild = new LanguageDataGen().nextPersisted();
        final Contentlet contentletChild =  new ContentletDataGen(contentTypeChild.id())
                .languageId(languageChild.getId())
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

        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

        final Host systemHost = APILocator.systemHost();
        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();
        return list(
                new TestData(contentlet, list(host, contentType, language, systemFolder, systemWorkflowScheme), filterDescriptorAllDependencies),
                new TestData(contentlet, list(), filterDescriptorNotDependencies),
                new TestData(contentlet, list(), filterDescriptorNotRelationship),
                new TestData(contentlet, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(contentletWithFolder, list(host, contentType, language, folder, systemWorkflowScheme, systemFolder),
                        filterDescriptorAllDependencies),
                new TestData(contentletWithFolder, list(), filterDescriptorNotDependencies),
                new TestData(contentletWithFolder, list(), filterDescriptorNotRelationship),
                new TestData(contentletWithFolder, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(contentletWithRelationship,
                        list(host, contentTypeParent, language, contentletChild, contentTypeChild, languageChild,
                                systemFolder, systemWorkflowScheme), filterDescriptorAllDependencies),
                new TestData(contentletWithRelationship, list(), filterDescriptorNotDependencies),
                new TestData(contentletWithRelationship, list(), filterDescriptorNotRelationship),
                new TestData(contentletWithRelationship, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(contentWithCategory, list(host, contentTypeWithCategory, language, category, systemFolder, systemWorkflowScheme),
                        filterDescriptorAllDependencies),
                new TestData(contentWithCategory, list(), filterDescriptorNotDependencies),
                new TestData(contentWithCategory, list(), filterDescriptorNotRelationship),
                new TestData(contentWithCategory, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(htmlPageAsset, list(host, defaultHost, contentTypeToPage, defaultLanguage,
                        container, template, htmlPageAssetContentType, systemFolder, systemWorkflowScheme, systemHost),
                        filterDescriptorAllDependencies),
                new TestData(htmlPageAsset, list(), filterDescriptorNotDependencies),
                new TestData(htmlPageAsset, list(), filterDescriptorNotRelationship),
                new TestData(htmlPageAsset, list(), filterDescriptorNotDependenciesRelationship)
        );
    }

    private static Collection<TestData> createRuleTestCase() {
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule = new RuleDataGen().host(host).nextPersisted();

        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        final Rule ruleWithPage = new RuleDataGen().page(htmlPageAsset).nextPersisted();

        return list(
                new TestData(rule, list(host), filterDescriptorAllDependencies),
                new TestData(rule, list(), filterDescriptorNotDependencies),
                new TestData(rule, list(), filterDescriptorNotRelationship),
                new TestData(rule, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(ruleWithPage, list(htmlPageAsset), filterDescriptorAllDependencies),
                new TestData(ruleWithPage, list(), filterDescriptorNotDependencies),
                new TestData(ruleWithPage, list(), filterDescriptorNotRelationship),
                new TestData(ruleWithPage, list(), filterDescriptorNotDependenciesRelationship)
        );
    }

    private static Collection<TestData> createLanguageTestCase() {
        final Language language = new LanguageDataGen().nextPersisted();

        return list(
                new TestData(language, list(), filterDescriptorAllDependencies),
                new TestData(language, list(), filterDescriptorNotDependencies),
                new TestData(language, list(), filterDescriptorNotRelationship),
                new TestData(language, list(), filterDescriptorNotDependenciesRelationship)
        );

    }

    private static Collection<TestData> createWorkflowTestCase() {
        final WorkflowScheme workflowScheme = new WorkflowDataGen().nextPersisted();
        final WorkflowStep workflowStep = new WorkflowStepDataGen(workflowScheme.getId()).nextPersisted();
        final WorkflowAction workflowAction = new WorkflowActionDataGen(workflowScheme.getId(), workflowStep.getId())
                .nextPersisted();

        return list(
                new TestData(workflowScheme, list(), filterDescriptorAllDependencies),
                new TestData(workflowScheme, list(), filterDescriptorNotDependencies),
                new TestData(workflowScheme, list(), filterDescriptorNotRelationship),
                new TestData(workflowScheme, list(), filterDescriptorNotDependenciesRelationship)
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

        final Host systemHost = APILocator.getHostAPI().findSystemHost();

        return list(
                new TestData(host, list(), filterDescriptorAllDependencies),
                new TestData(host, list(), filterDescriptorNotDependencies),
                new TestData(host, list(), filterDescriptorNotRelationship),
                new TestData(host, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(hostWithTemplate, list(template), filterDescriptorAllDependencies),
                new TestData(hostWithTemplate, list(), filterDescriptorNotDependencies),
                new TestData(hostWithTemplate, list(), filterDescriptorNotRelationship),
                new TestData(hostWithTemplate, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(hostWithContainer, list(container), filterDescriptorAllDependencies),
                new TestData(hostWithContainer, list(), filterDescriptorNotDependencies),
                new TestData(hostWithContainer, list(), filterDescriptorNotRelationship),
                new TestData(hostWithContainer, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(hostWithContent, list(contentType, contentlet, systemWorkflowScheme,systemFolder, language),
                        filterDescriptorAllDependencies),
                new TestData(hostWithContent, list(), filterDescriptorNotDependencies),
                new TestData(hostWithContent, list(), filterDescriptorNotRelationship),
                new TestData(hostWithContent, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(hostWithFolder, list(folder, folderContentType, systemHost, systemFolder, systemWorkflowScheme),
                        filterDescriptorAllDependencies),
                new TestData(hostWithFolder, list(), filterDescriptorNotDependencies),
                new TestData(hostWithFolder, list(), filterDescriptorNotRelationship),
                new TestData(hostWithFolder, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(hostWithRule, list(rule), filterDescriptorAllDependencies),
                new TestData(hostWithFolder, list(), filterDescriptorNotDependencies),
                new TestData(hostWithFolder, list(), filterDescriptorNotRelationship),
                new TestData(hostWithFolder, list(), filterDescriptorNotDependenciesRelationship)
        );
    }

    private static Collection<TestData> createLinkTestCase() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final Link link = new LinkDataGen(folder)
                .hostId(host.getIdentifier())
                .nextPersisted();


        return list(
                new TestData(link, list(host, folder), filterDescriptorAllDependencies),
                new TestData(link, list(), filterDescriptorNotDependencies),
                new TestData(link, list(), filterDescriptorNotRelationship),
                new TestData(link, list(), filterDescriptorNotDependenciesRelationship)
        );
    }

    private static Collection<TestData> createFolderTestCase() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final Folder parentFolder = new FolderDataGen().site(host).nextPersisted();
        final Folder folderWithParent = new FolderDataGen()
                .parent(parentFolder)
                .site(host)
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

        final Host systemHost = APILocator.getHostAPI().findSystemHost();
        final Structure folderStructure = CacheLocator.getContentTypeCache()
                .getStructureByInode(folder.getDefaultFileType());

        final ContentType folderContentType = new StructureTransformer(folderStructure).from();

        final Folder systemFolder = APILocator.getFolderAPI()
                .find(folderContentType.folder(), APILocator.systemUser(), false);

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();

        //Folder with sub folder
        return list(
                new TestData(folder, list(host, folderContentType, systemHost, systemFolder, systemWorkflowScheme),
                        filterDescriptorAllDependencies),
                new TestData(folder, list(), filterDescriptorNotDependencies),
                new TestData(folder, list(), filterDescriptorNotRelationship),
                new TestData(folder, list(), filterDescriptorNotDependenciesRelationship),

                //Dependency manager not add Parent Folder, the Parent Folder is added as dependency in FolderBundle
                new TestData(folderWithParent, list(host, folderContentType, systemHost, systemFolder, systemWorkflowScheme),
                        filterDescriptorAllDependencies),
                new TestData(folderWithParent, list(), filterDescriptorNotDependencies),
                new TestData(folderWithParent, list(), filterDescriptorNotRelationship),
                new TestData(folderWithParent, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(folderWithContentType, list(host, folderContentType, systemHost, systemFolder, contentType, systemWorkflowScheme),
                        filterDescriptorAllDependencies),
                new TestData(folderWithContentType, list(), filterDescriptorNotDependencies),
                new TestData(folderWithContentType, list(), filterDescriptorNotRelationship),
                new TestData(folderWithContentType, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(folderWithContent, list(host, folderContentType, systemHost, systemFolder, contentlet,
                        systemWorkflowScheme, language), filterDescriptorAllDependencies),
                new TestData(folderWithContent, list(), filterDescriptorNotDependencies),
                new TestData(folderWithContent, list(), filterDescriptorNotRelationship),
                new TestData(folderWithContent, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(folderWithLink, list(host, folderContentType, systemHost, systemFolder, link,
                        systemWorkflowScheme), filterDescriptorAllDependencies),
                new TestData(folderWithLink, list(), filterDescriptorNotDependencies),
                new TestData(folderWithLink, list(), filterDescriptorNotRelationship),
                new TestData(folderWithLink, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(folderWithSubFolder, list(host, folderContentType, systemHost, systemFolder,
                        subFolder, contentlet_2, systemWorkflowScheme, language),
                        filterDescriptorAllDependencies),
                new TestData(folderWithSubFolder, list(), filterDescriptorNotDependencies),
                new TestData(folderWithSubFolder, list(), filterDescriptorNotRelationship),
                new TestData(folderWithSubFolder, list(), filterDescriptorNotDependenciesRelationship)
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

        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();
        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();

        Logger.info(DependencyManager.class, "Container " + containerWithoutContentType.getIdentifier() + " host" + host.getIdentifier());
        return list(
                new TestData(containerWithoutContentType, list(host), filterDescriptorAllDependencies),
                new TestData(containerWithoutContentType, list(), filterDescriptorNotDependencies),
                new TestData(containerWithoutContentType, list(), filterDescriptorNotRelationship),
                new TestData(containerWithoutContentType, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(containerWithContentType, list(host, contentType, systemWorkflowScheme, systemFolder), filterDescriptorAllDependencies),
                new TestData(containerWithContentType, list(), filterDescriptorNotDependencies),
                new TestData(containerWithContentType, list(), filterDescriptorNotRelationship),
                new TestData(containerWithContentType, list(), filterDescriptorNotDependenciesRelationship)
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
        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

        return list(
                new TestData(advancedTemplateWithContainer, list(host), filterDescriptorAllDependencies),
                new TestData(advancedTemplateWithContainer, list(), filterDescriptorNotDependencies),
                new TestData(advancedTemplateWithContainer, list(), filterDescriptorNotRelationship),
                new TestData(advancedTemplateWithContainer, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(templateWithTemplateLayout, list(host, container_1, container_2, contentType, systemWorkflowScheme, systemFolder),
                        filterDescriptorAllDependencies),
                new TestData(templateWithTemplateLayout, list(), filterDescriptorNotDependencies),
                new TestData(templateWithTemplateLayout, list(), filterDescriptorNotRelationship),
                new TestData(templateWithTemplateLayout, list(), filterDescriptorNotDependenciesRelationship)
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

        new FieldRelationshipDataGen()
                .child(contentTypeChild)
                .parent(contentTypeParent)
                .nextPersisted();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();
        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

        return list(
                new TestData(contentType, list(host, systemWorkflowScheme, systemFolder), filterDescriptorAllDependencies),
                new TestData(contentType, list(), filterDescriptorNotDependencies),
                new TestData(contentType, list(), filterDescriptorNotRelationship),
                new TestData(contentType, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(contentTypeWithFolder, list(folder, systemWorkflowScheme, folderHost), filterDescriptorAllDependencies),
                new TestData(contentTypeWithFolder, list(), filterDescriptorNotDependencies),
                new TestData(contentTypeWithFolder, list(), filterDescriptorNotRelationship),
                new TestData(contentTypeWithFolder, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(contentTypeWithWorkflow, list(host, systemWorkflowScheme, workflowScheme, systemFolder), filterDescriptorAllDependencies),
                new TestData(contentTypeWithWorkflow, list(), filterDescriptorNotDependencies),
                new TestData(contentTypeWithWorkflow, list(), filterDescriptorNotRelationship),
                new TestData(contentTypeWithWorkflow, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(contentTypeWithCategory, list(host, systemWorkflowScheme, category, systemFolder), filterDescriptorAllDependencies),
                new TestData(contentTypeWithCategory, list(), filterDescriptorNotDependencies),
                new TestData(contentTypeWithCategory, list(), filterDescriptorNotRelationship),
                new TestData(contentTypeWithCategory, list(), filterDescriptorNotDependenciesRelationship),

                new TestData(contentTypeParent, list(host, systemWorkflowScheme, contentTypeChild, systemFolder), filterDescriptorAllDependencies),
                new TestData(contentTypeParent, list(), filterDescriptorNotDependencies),
                new TestData(contentTypeParent, list(), filterDescriptorNotRelationship),
                new TestData(contentTypeParent, list(), filterDescriptorNotDependenciesRelationship)
        );
    }


    /**
     * Method to Test: {@link DependencyBundler#generate(File, BundlerStatus)}
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

        new BundleDataGen()
                .pushPublisherConfig(config)
                .addAssets(set(testData.assetsToAddInBundle))
                .filter(testData.filterDescriptor)
                .nextPersisted();

        final Set<Object> languagesVariableDependencies = getLanguagesVariableDependencies(
                true, false, false);

        final Set<Object> dependencies = new HashSet<>();

        if (filterDescriptorAllDependencies == testData.filterDescriptor) {
            dependencies.addAll(testData.dependenciesToAssert);
            dependencies.addAll(languagesVariableDependencies);
        }

        bundler.setConfig(config);
        bundler.generate(bundleRoot, status);

        assertAll(config, dependencies, testData.filterDescriptor);
    }

    @Test
    public void addLanguageVariableTestCaseInBundle() throws DotSecurityException, DotDataException, DotBundleException {
        Contentlet contentlet = null;

        try {
            final PushPublisherConfig config = new PushPublisherConfig();

            PublisherAPIImplTest.createLanguageVariableIfNeeded();

            final FilterDescriptor filterDescriptor = new FileDescriptorDataGen().nextPersisted();
            new BundleDataGen()
                    .pushPublisherConfig(config)
                    .filter(filterDescriptor)
                    .nextPersisted();

            bundler.setConfig(config);
            bundler.generate(bundleRoot, status);

            final Collection<Object> dependencies = getLanguagesVariableDependencies(
                    true, false, false);
            assertAll(config, dependencies, filterDescriptor);
        } finally {
            if (contentlet != null) {
                ContentletDataGen.archive(contentlet);
                ContentletDataGen.remove(contentlet);
            }
        }

    }

    private void assertAll(final PushPublisherConfig config, final Collection<Object> dependenciesToAssert, final FilterDescriptor filterDescriptor) {
        AssignableFromMap<Integer> counts = new AssignableFromMap<>();

        for (Object asset : dependenciesToAssert) {

            Class assetClass = (Contentlet.class == asset.getClass()) && ((Contentlet) asset).isHost() ? Host.class : asset.getClass();
            final BundleDataGen.MetaData metaData = BundleDataGen.howAddInBundle.get(assetClass);

            final String assetId = metaData.dataToAdd.apply(asset);
            assertTrue(String.format("Not Contain %s in %s", assetId, asset.getClass()),
                    metaData.collection.apply(config).contains(assetId));

            final Class key = BundleDataGen.howAddInBundle.getKey(asset.getClass());
            counts.addOrUpdate(key, 1, (Integer value) -> value + 1);
        }

        if (filterDescriptorAllDependencies == filterDescriptor) {
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
    }

    private static class TestData {
        Object assetsToAddInBundle;
        Collection<Object> dependenciesToAssert;
        FilterDescriptor filterDescriptor;

        public TestData(
                final Object assetsToAddInBundle,
                final Collection<Object> dependenciesToAssert,
                final FilterDescriptor filterDescriptor)  {
            this.assetsToAddInBundle = assetsToAddInBundle;
            this.filterDescriptor = filterDescriptor;

            this.dependenciesToAssert = new HashSet<>();
            this.dependenciesToAssert.addAll(dependenciesToAssert);
            this.dependenciesToAssert.add(assetsToAddInBundle);
        }

        @Override
        public String toString() {
            final String dependencies = dependenciesToAssert.stream()
                    .map(dependecy -> dependecy.getClass().getSimpleName())
                    .collect(joining(","));

            return "[assetsToAddInBundle=" + assetsToAddInBundle.getClass().getSimpleName() + "], " +
                    "[dependenciesToAssert=" +  dependencies + "], " +
                    "filterDescriptor=" + filterDescriptor.getFilters();
        }
    }
}
