package com.dotcms.publishing;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.*;
import com.dotcms.enterprise.publishing.remote.bundler.ContainerBundler;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.repackage.com.google.common.collect.Sets;
import com.dotcms.test.util.FileTestUtil;
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
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.*;
import static org.jgroups.util.Util.assertEquals;

@RunWith(DataProviderRunner.class)
public class PublisherAPIImplTest {

    private static Contentlet languageVariableCreated;
    private static Set<Object> languageVariableDependencies;

    public static class PushPublisherMock extends PushPublisher {
        @Override
        public PublisherConfig process ( final PublishStatus status ) throws DotPublishingException {
            return this.config;
        }
    }

    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @AfterClass
    public static void removeLanguageVariable(){
        ContentletDataGen.remove(languageVariableCreated);
    }

    @BeforeClass
    public static void createLanguageVariableIfNeeded() throws DotSecurityException, DotDataException {
        final User systemUser = APILocator.systemUser();
        final List<Contentlet> langVariables = getLanguageVariables();

        final ContentType languageVariableContentType =
                APILocator.getContentTypeAPI(systemUser).find(LanguageVariableAPI.LANGUAGEVARIABLE);

        if (langVariables.isEmpty()) {
            final Language language = new com.dotmarketing.portlets.languagesmanager.business.LanguageDataGen().nextPersisted();

            final Host host = new SiteDataGen().nextPersisted();
            languageVariableCreated = new ContentletDataGen(languageVariableContentType.id())
                    .setProperty("key", "teset_key")
                    .setProperty("value", "value_test")
                    .languageId(language.getId())
                    .host(host)
                    .nextPersisted();
        }

        languageVariableDependencies = getLanguagesVariableDependencies();
    }


    /**
     * Method to Test: {@link PublisherAPIImpl#publish(PublisherConfig)}
     * When: Add a {@link Container} in a bundle
     * Should:
     * - The file should be create in:
     * For Live Version: <bundle_root_path>/live/<container_host_name>/<container_id>.container.xml
     * For Working: <bundle_root_path>/working/<container_host_name>/<container_id>.container.xml
     *
     * If the Container has live and working version then to files will be created
     */
    @DataProvider
    public static Object[] publishers() throws Exception {
        prepare();

        final List<TestAsset> assets = list(
                getContentTypeWithHost(),
                getTemplateWithDependencies(),
                getContainerWithDependencies(),
                getFolderWithDependencies(),
                getHostWithDependencies(),
                getLinkWithDependencies(),
                getWorkflowWithDependencies(),
                getLanguageWithDependencies(),
                getRuleWithDependencies()
        );
        final List<Class<? extends Publisher>> publishers = list(
                GenerateBundlePublisher.class,
                PushPublisherMock.class
        );

        createLanguageVariableIfNeeded();
        addLanguageVariableDependencies(assets);

        final List<TestCase> cases = new ArrayList<>();

        for (final Class<? extends Publisher> publisher : publishers) {
            for (TestAsset asset : assets) {
                cases.add(new TestCase(publisher, set(asset)));
            }
        }

        return cases.toArray();
    }

    private static TestAsset getRuleWithDependencies() {
        final Host host = new SiteDataGen().nextPersisted();

        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        final Rule ruleWithPage = new RuleDataGen().page(htmlPageAsset).nextPersisted();

        return new TestAsset(ruleWithPage, set(), "/bundlers-test/rule/rule.rule.xml", false);
    }

    private static TestAsset getLanguageWithDependencies() {
        final Language language = new LanguageDataGen().nextPersisted();

        return new TestAsset(language, set(), "/bundlers-test/language/language.language.xml");
    }

    private static TestAsset getHostWithDependencies() {
        try {
            final Host host = new SiteDataGen().nextPersisted();

            final Template template = new TemplateDataGen().host(host).nextPersisted();

            final ContentType containerContentType = new ContentTypeDataGen().host(host).nextPersisted();
            final Container container = new ContainerDataGen()
                    .site(host)
                    .withContentType(containerContentType, "")
                    .nextPersisted();

            final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();
            final Folder folder = new FolderDataGen().site(host).nextPersisted();

            final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

            final Structure folderStructure = CacheLocator.getContentTypeCache().getStructureByInode(systemFolder.getDefaultFileType());
            final ContentType folderContentType = new StructureTransformer(folderStructure).from();

            final ContentType contentType = new ContentTypeDataGen()
                    .host(host)
                    .nextPersisted();
            final Contentlet contentlet = new ContentletDataGen(contentType.id()).host(host).nextPersisted();
            final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

            final Rule rule = new RuleDataGen().host(host).nextPersisted();


            return new TestAsset(host,
                    set(template, container, containerContentType, systemWorkflowScheme, folder, folderContentType,
                            contentType, contentlet, language, rule), "/bundlers-test/host/host.host.xml");
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    private static TestAsset getLinkWithDependencies() {

        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final Link link = new LinkDataGen(folder)
                .hostId(host.getIdentifier())
                .nextPersisted();


        return new TestAsset(link, set(host, folder), "/bundlers-test/link/link.link.xml");
    }

    private static TestAsset getWorkflowWithDependencies() {

        final WorkflowScheme workflowScheme = new WorkflowDataGen().nextPersisted();
        final WorkflowStep workflowStep = new WorkflowStepDataGen(workflowScheme.getId()).nextPersisted();
        final WorkflowAction workflowAction = new WorkflowActionDataGen(workflowScheme.getId(), workflowStep.getId())
                .nextPersisted();


        return new TestAsset(workflowScheme, set(), "/bundlers-test/workflow/workflow_with_steps_and_action.workflow.xml");
    }

    private static TestAsset getFolderWithDependencies() throws DotDataException, DotSecurityException {

        final Host host = new SiteDataGen().nextPersisted();
        final Folder parentFolder = new FolderDataGen().site(host).nextPersisted();

        final Folder folderWithDependencies = new FolderDataGen()
                .site(host)
                .parent(parentFolder)
                .nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .folder(folderWithDependencies)
                .nextPersisted();

        final File image = new File(Thread.currentThread().getContextClassLoader().getResource("images/test.jpg").getFile());
        final Contentlet contentlet = new FileAssetDataGen(folderWithDependencies, image)
                .host(host)
                .nextPersisted();

        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

        final Link link = new LinkDataGen(folderWithDependencies).hostId(host.getIdentifier()).nextPersisted();

        final Folder subFolder = new FolderDataGen()
                .parent(folderWithDependencies)
                .nextPersisted();

        final Contentlet contentlet_2 = new FileAssetDataGen(subFolder, image)
                .host(host)
                .nextPersisted();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();
        final Structure folderStructure = CacheLocator.getContentTypeCache()
                .getStructureByInode(parentFolder.getDefaultFileType());

        final ContentType folderContentType = new StructureTransformer(folderStructure).from();

        return new TestAsset(folderWithDependencies,
                set(host, parentFolder, folderContentType, systemWorkflowScheme,
                        contentType, contentlet, language, link, subFolder, contentlet_2),
                "/bundlers-test/folder/folder.folder.xml");
    }

    private static TestAsset getTemplateWithDependencies() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();
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

        return new TestAsset(templateWithTemplateLayout,
                set(host, container_1, container_2, contentType, systemWorkflowScheme),
                "/bundlers-test/template/template.template.xml");
    }

    private static TestAsset getContentTypeWithHost() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final WorkflowScheme workflowScheme = new WorkflowDataGen().nextPersisted();

        final Category category = new CategoryDataGen().nextPersisted();

        final ContentType contentTypeChild =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        ContentType contentType = new ContentTypeDataGen()
                .host(host)
                .workflowId(workflowScheme.getId())
                .addCategory(category)
                .nextPersisted();

        final Relationship relationship = new FieldRelationshipDataGen()
                .child(contentTypeChild)
                .parent(contentType)
                .nextPersisted();

        contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find(contentType.variable());

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();

            return new TestAsset(contentType,
                set(host, workflowScheme, systemWorkflowScheme, contentTypeChild, relationship, category),
                "/bundlers-test/content_types/content_types_with_category_and_relationship.contentType.json");
    }

    private static TestAsset getContainerWithDependencies() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().host(host).nextPersisted();

        final Container containerWithContentType = new ContainerDataGen()
                .site(host)
                .withContentType(contentType, "")
                .nextPersisted();

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();

        return new TestAsset(containerWithContentType,
                set(host, contentType, systemWorkflowScheme),
                "/bundlers-test/container/container.containers.container.xml");
    }

    /**
     * Method to Test: {@link PublisherAPIImpl#publish(PublisherConfig)}
     * When: Add different assets into a bundle
     * Should: Create all the files
     */
    @Test
    @UseDataProvider("publishers")
    public void publish(final TestCase testCase) throws DotPublishingException, DotSecurityException, IOException, DotDataException {
        final Class<? extends Publisher> publisher = testCase.publisher;
        final Set<TestAsset> testAssets = testCase.assets;
        final Set<Object> assetsToAddInBundle = new HashSet<>();
        final Set<Object> dependencies = new HashSet<>();

        for (final TestAsset testAsset : testAssets) {
            assetsToAddInBundle.add(testAsset.asset);
            dependencies.addAll(testAsset.expectedInBundle);
        }

        final FilterDescriptor filterDescriptor = new FileDescriptorDataGen().nextPersisted();

        final PublisherAPIImpl publisherAPI = new PublisherAPIImpl();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setPublishers(list(publisher));
        config.setOperation(PublisherConfig.Operation.PUBLISH);

        config.setLuceneQueries(list());

        new BundleDataGen()
                .pushPublisherConfig(config)
                .addAssets(list(assetsToAddInBundle))
                .filter(filterDescriptor)
                .nextPersisted();

        publisherAPI.publish(config);

        final File bundleRoot = BundlerUtil.getBundleRoot(config);

        final Collection<File> filesExpected = new HashSet<>();

        for (final TestAsset testAsset : testAssets) {
            final Collection<File> files = FileTestUtil.assertBundleFile(bundleRoot, testAsset.asset, testAsset.fileExpectedPath);
            filesExpected.addAll(files);
        }

        for (Object assetToAssert : dependencies) {
            final Collection<File> files = FileTestUtil.assertBundleFile(bundleRoot, assetToAssert);
            filesExpected.addAll(files);
        }

        final String messagesPath = bundleRoot.getAbsolutePath() + File.separator + "messages";

        final String systemHostPath = bundleRoot.getAbsolutePath()
                + "/working/System Host/855a2d72-f2f3-4169-8b04-ac5157c4380c.contentType.json";

        final List<File> files = FileUtil.listFilesRecursively(bundleRoot).stream()
                .filter(file -> file.isFile())
                .filter(file -> !file.getAbsolutePath().equals(systemHostPath))
                .filter(file -> !file.getParentFile().getAbsolutePath().equals(messagesPath))
                .collect(Collectors.toList());

        //All the dependencies plus, the asset and the bundle xml
        long numberFilesExpected = filesExpected.size() + 1;
        final int numberFiles = files.size();

        final String filesExpectedPath = filesExpected.stream().map(file -> file.getAbsolutePath()).collect(Collectors.joining());
        final String filesPath = files.stream().map(file -> file.getAbsolutePath()).collect(Collectors.joining());

        assertEquals(String.format("Expected %d but get %d in %s\nExpected %s\nExisting %s\n",
                    numberFilesExpected, numberFiles, bundleRoot, filesExpectedPath, filesPath),
                numberFilesExpected, numberFiles);
    }

    private static class TestCase {
        Class<? extends Publisher> publisher;
        Set<TestAsset> assets;

        public TestCase(
                final Class<? extends Publisher> publisher,
                Set<TestAsset> assets) {

            this.publisher = publisher;
            this.assets = assets;
        }
    }

    public static List<Contentlet> getLanguageVariables() throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();
        final String langVarsQuery = "+contentType:" + LanguageVariableAPI.LANGUAGEVARIABLE;
        final List<Contentlet> langVariables = APILocator.getContentletAPI().search(langVarsQuery, 0, -1,
                StringPool.BLANK, systemUser, false);
        return langVariables;
    }

    private static void addLanguageVariableDependencies(final Collection<TestAsset> assets) throws DotSecurityException, DotDataException {

        for (final TestAsset asset : assets) {
            List<Object> languageVariablesDependencies = null;

            if (asset.addLanguageVariableAsDependencies) {
                final Host systemHost = APILocator.getHostAPI().findSystemHost();
                final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

                languageVariablesDependencies = languageVariableDependencies.stream()
                        .filter(dependency -> {
                            if (Host.class.isInstance(dependency)){
                                return !((Host) dependency).getIdentifier().equals(systemHost.getIdentifier());
                            } else  if (Folder.class.isInstance(dependency)){
                                return !((Folder) dependency).getIdentifier().equals(systemFolder.getIdentifier());
                            } else {
                                return true;
                            }
                        })
                        .collect(Collectors.toList());

            } else {
                final Host systemHost = APILocator.getHostAPI().findSystemHost();

                languageVariablesDependencies = languageVariableDependencies.stream()
                        .filter(dependency -> Host.class.isInstance(dependency))
                        .map(dependency -> (Host) dependency)
                        .filter(host -> !host.getIdentifier().equals(systemHost.getIdentifier()))
                        .collect(Collectors.toList());
            }

            if (languageVariableDependencies != null){
                asset.expectedInBundle.addAll(languageVariablesDependencies);
            }
        }
    }


    public static Set<Object> getLanguagesVariableDependencies() throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();
        final List<Contentlet> languageVariables = getLanguageVariables();
        Set<Object> dependencies = new HashSet<>();

        for (final Contentlet langVariable : languageVariables) {
            final Host host = APILocator.getHostAPI().find(langVariable.getHost(), systemUser, false);

            final Language language = APILocator.getLanguageAPI().getLanguage(langVariable.getLanguageId());
            dependencies.add(langVariable);
            dependencies.add(host);
            dependencies.add(language);
        }

        if (!languageVariables.isEmpty()) {
            final ContentType languageVariableContentType =
                    APILocator.getContentTypeAPI(systemUser).find(LanguageVariableAPI.LANGUAGEVARIABLE);

            dependencies.add(languageVariableContentType);

            final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();
            dependencies.add(systemWorkflowScheme);

            final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();
            dependencies.add(systemFolder);

            final Host systemHost = APILocator.getHostAPI().findSystemHost();
            dependencies.add(systemHost);
        }

        return dependencies;
    }

    private static class TestAsset {
        Object asset;
        Set<Object> expectedInBundle;
        String fileExpectedPath;
        boolean addLanguageVariableAsDependencies;

        public TestAsset(Object asset, Set<Object> expectedInBundle, String fileExpectedPath){
            this(asset, expectedInBundle, fileExpectedPath, true);
        }

        public TestAsset(Object asset, Set<Object> expectedInBundle, String fileExpectedPath,
                         boolean addLanguageVariableAsDependencies) {
            this.asset = asset;
            this.expectedInBundle = expectedInBundle;
            this.fileExpectedPath = fileExpectedPath;
            this.addLanguageVariableAsDependencies = addLanguageVariableAsDependencies;

            //todo: uncomment when it merge into the performance branch
            /*try {
                final ContentType hostContentType
                        = APILocator.getContentTypeAPI(APILocator.systemUser()).find("host");

                this.expectedInBundle.add(hostContentType);
            } catch (DotSecurityException | DotDataException e) {
                throw new RuntimeException(e);
            }*/


        }
    }
}
