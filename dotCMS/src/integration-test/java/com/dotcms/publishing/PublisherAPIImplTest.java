package com.dotcms.publishing;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.util.CollectionsUtils.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
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
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.PushPublishingEndPointDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TemplateLayoutDataGen;
import com.dotcms.datagen.WorkflowActionDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.datagen.WorkflowStepDataGen;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleFactoryImpl;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.dependencies.DependencyManager;
import com.dotcms.publishing.manifest.ManifestItem;
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
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
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
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import com.sun.net.httpserver.HttpServer;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DataProviderRunner.class)
public class PublisherAPIImplTest {
    private static String MANIFEST_HEADERS = "INCLUDED/EXCLUDED,object type, Id, title, site, folder, excluded by, included by";
    private static Contentlet languageVariableCreated;

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

        return  new TestAsset[]{
                getContentTypeWithHost(),
                getTemplateWithDependencies(),
                getContainerWithDependencies(),
                getFolderWithDependencies(),
                getHostWithDependencies(),
                getLinkWithDependencies(),
                getWorkflowWithDependencies(),
                getLanguageWithDependencies(),
                getRuleWithDependencies(),
                getContentWithSeveralVersions()
        };
    }

    private static TestAsset getContentWithSeveralVersions() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final Field textField = new FieldDataGen().type(TextField.class).next();
        final ContentType contentType = new ContentTypeDataGen()
                .field(textField)
                .host(host)
                .nextPersisted();

        final Contentlet liveVersion = new ContentletDataGen(contentType)
                .setProperty(textField.variable(), "Live versions")
                .host(host)
                .nextPersisted();

        ContentletDataGen.publish(liveVersion);

        final Contentlet workingVersion = ContentletDataGen.checkout(liveVersion);
        workingVersion.setStringProperty(textField.variable(), "Working versions");
        ContentletDataGen.checkin(workingVersion);

        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

        return new TestAsset(workingVersion,
                map(
                    workingVersion, list(host, contentType, defaultLanguage)
                ),
                set(liveVersion),
                "/bundlers-test/contentlet/contentlet/contentlet.content.xml");
    }

    private static TestAsset getRuleWithDependencies() {
        final Host host = new SiteDataGen().nextPersisted();

        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        final Rule ruleWithPage = new RuleDataGen().page(htmlPageAsset).host(host).nextPersisted();

        return new TestAsset(ruleWithPage,
                map(ruleWithPage, list(host)),
                "/bundlers-test/rule/rule.rule.xml", false);
    }

    private static TestAsset getLanguageWithDependencies() {
        final Language language = new LanguageDataGen().nextPersisted();

        return new TestAsset(language, map(), "/bundlers-test/language/language.language.xml");
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
                    map(
                        host, list(template, container, contentlet, containerContentType, contentType, folder, rule),
                        contentlet,list(contentType, language),
                        container,list(containerContentType),
                        folder, list(folderContentType)
                    ),
                    "/bundlers-test/host/host.host.xml");
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

        return new TestAsset(link, map(link, list(host, folder)), "/bundlers-test/link/link.link.xml");
    }

    private static TestAsset getWorkflowWithDependencies() {

        final WorkflowScheme workflowScheme = new WorkflowDataGen().nextPersisted();
        final WorkflowStep workflowStep = new WorkflowStepDataGen(workflowScheme.getId()).nextPersisted();
        final WorkflowAction workflowAction = new WorkflowActionDataGen(workflowScheme.getId(), workflowStep.getId())
                .nextPersisted();


        return new TestAsset(workflowScheme, map(), "/bundlers-test/workflow/workflow_with_steps_and_action.workflow.xml");
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

        final Structure folderStructure = CacheLocator.getContentTypeCache()
                .getStructureByInode(parentFolder.getDefaultFileType());

        final ContentType folderContentType = new StructureTransformer(folderStructure).from();

        final ContentType fileAssetContentType = contentlet.getContentType();

        return new TestAsset(folderWithDependencies,
                map(
                    folderWithDependencies,
                    list(host, parentFolder, contentlet, folderContentType, link, subFolder, contentType),
                    contentlet, list(language, fileAssetContentType),
                    contentlet_2, list(language, fileAssetContentType),
                    subFolder, list(contentlet_2)
                ),
                "/bundlers-test/folder/folder.folder.xml");
    }

    private static TestAsset getTemplateWithDependencies() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Container container_1 = new ContainerDataGen()
                .site(host)
                .withContentType(contentType, "")
                .nextPersisted();

        final Container container_2 = new ContainerDataGen()
                .site(host)
                .clearContentTypes()
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

        return new TestAsset(templateWithTemplateLayout,
                map(
                    templateWithTemplateLayout, list(host, container_1, container_2, contentType),
                    container_1, list(contentType),
                    contentType, list(systemWorkflowScheme)
                ),
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
                map(
                        contentType, list(host, workflowScheme, relationship, category, systemWorkflowScheme),
                        relationship, list(contentTypeChild)
                ),
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
                map(
                        containerWithContentType, list(host, contentType),
                        contentType, list(systemWorkflowScheme)
                ),
                "/bundlers-test/container/container.containers.container.xml");
    }

    /**
     * Method to Test: {@link PublisherAPIImpl#publish(PublisherConfig)}
     * When: Add different assets into a bundle, and generate it
     * Should: Create all the files and the Manifest File correctly
     */
    @Test
    @UseDataProvider("publishers")
    public void generateBundle(final TestAsset testAsset) throws DotPublishingException, DotSecurityException, IOException, DotDataException {
        final Class<? extends Publisher> publisher = GenerateBundlePublisher.class;

        createLanguageVariableIfNeeded();

        List<Contentlet> languageVariables = getLanguageVariables();
        Set<?> languagesVariableDependencies = getLanguagesVariableDependencies(
                languageVariables,
                testAsset.addLanguageVariableDependencies, true, true);

        final FilterDescriptor filterDescriptor = new FilterDescriptorDataGen().nextPersisted();

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);

        final PublisherAPIImpl publisherAPI = new PublisherAPIImpl();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setPublishers(list(publisher));
        config.setOperation(PublisherConfig.Operation.PUBLISH);
        config.setLuceneQueries(list());
        config.setId("PublisherAPIImplTest_" + System.currentTimeMillis());

        new BundleDataGen()
                .pushPublisherConfig(config)
                .addAssets(list(testAsset.asset))
                .filter(filterDescriptor)
                .nextPersisted();

        final PublishStatus publish = publisherAPI.publish(config);
        File bundleRoot = publish.getOutputFiles().get(0);

        final File extractHere = new File(bundleRoot.getParent() + File.separator + config.getName());
        extractTarArchive(bundleRoot, extractHere);

        final List<Contentlet> languageVariablesAddInBundle = testAsset.addLanguageVariableDependencies ?
                getLanguageVariables() : Collections.EMPTY_LIST;

        assertBundle(testAsset,
                getJustOneList(testAsset.otherVersions, testAsset.getDependencies(),
                        languageVariablesAddInBundle, languagesVariableDependencies),
                extractHere);

        if (!Rule.class.isInstance(testAsset.asset)) {
            final ManifestItemsMapTest manifestLines = testAsset.manifestLines();
            manifestLines.addExcludes(map("Excluded System Folder/Host",
                    list(APILocator.getHostAPI().findSystemHost(), APILocator.getFolderAPI().findSystemFolder())));

            addLanguageVariableManifestItem(
                    manifestLines,
                    testAsset.addLanguageVariableDependencies,
                    languageVariablesAddInBundle
            );

            final String manifestFilePath = extractHere.getAbsolutePath() + File.separator + "manifest.csv";
            final File manifestFile = new File(manifestFilePath);

            assertManifestFile(manifestFile, manifestLines);
        }
    }

    public static void addLanguageVariableManifestItem(
            final ManifestItemsMapTest manifestLines,
            final boolean addLanguageVariableDependencies,
            final List<Contentlet> languageVariablesAddInBundle)
            throws DotDataException, DotSecurityException {

        languageVariablesAddInBundle.stream().forEach(
                contentlet -> manifestLines.add(contentlet, "Added Automatically by dotCMS")
        );

        final ContentType languageVariablesContentType = getLanguageVariablesContentType();

        for (Contentlet languageVariable : languageVariablesAddInBundle) {
            final Collection<Object> dependenciesFrom = getLanguageVariable(languageVariable,
                    addLanguageVariableDependencies, true, true);

            dependenciesFrom.stream().forEach(
                    dependency -> manifestLines.add((ManifestItem) dependency,
                            "Dependency from: " + languageVariable.getIdentifier())
            );

            manifestLines.add(languageVariablesContentType,
                    "Dependency from: " + languageVariable.getIdentifier());
        }

        if (!languageVariablesAddInBundle.isEmpty()) {

            final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI()
                    .findSystemWorkflowScheme();
            manifestLines.add(systemWorkflowScheme,
                    "Dependency from: " + languageVariablesContentType.id());

            final Host systemHost = APILocator.getHostAPI().findSystemHost();
            manifestLines.addExclude(systemHost, "Excluded System Folder/Host");
        }
    }

    private static Collection<Object> getJustOneList(Collection<?>... collections){
        return Arrays.stream(collections)
                .flatMap(collection -> collection.stream())
                .collect(Collectors.toSet());
    }

    public static void assertManifestFile(final File manifestFile,
            final ManifestItemsMapTest manifestItems) throws IOException {

        assertTrue(manifestFile.exists());

        manifestItems.startCheck();

        try(BufferedReader csvReader = new BufferedReader(new FileReader(manifestFile))) {
            String line;
            int nLines = 0;

            final StringBuffer buffer = new StringBuffer();

            while ((line = csvReader.readLine()) != null) {
                System.out.println("line = " + line);
                buffer.append(line + "\n");

                if (nLines == 0) {
                    assertEquals("Wrong headers", MANIFEST_HEADERS, line);
                } else {
                    final boolean contains = manifestItems.contains(line);
                    assertTrue(manifestItems + " not contain " + line, contains);
                }

                nLines++;
            }

            assertEquals("manifestItems " + manifestItems + " Manifest content " + buffer.toString(),
                    manifestItems.size(), nLines - 1 );
        }
    }

    /**
     * Method to Test: {@link PublisherAPIImpl#publish(PublisherConfig)}
     * When: Add different assets into a bundle, and send it
     * Should: Create all the files
     */
    @Test
    @UseDataProvider("publishers")
    public void sendPushPublishBundle(final TestAsset testAsset)
            throws DotPublishingException, DotSecurityException, IOException, DotDataException, DotPublisherException {
        final Class<? extends Publisher> publisher = PushPublisher.class;

        final Environment environment = new EnvironmentDataGen().nextPersisted();

        final PushPublishingEndPoint publishingEndPoint = new PushPublishingEndPointDataGen()
                .environment(environment)
                .nextPersisted();

        final FilterDescriptor filterDescriptor = new FilterDescriptorDataGen().nextPersisted();

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setPublishers(list(publisher));
        config.setOperation(PublisherConfig.Operation.PUBLISH);
        config.setLuceneQueries(list());
        config.setId("sendPushPublishBundle_" + System.currentTimeMillis());

        final Bundle bundle = new BundleDataGen()
                .pushPublisherConfig(config)
                .addAssets(list(testAsset.asset))
                .filter(filterDescriptor)
                .nextPersisted();

        final BundleFactoryImpl bundleFactory = new BundleFactoryImpl();
        bundleFactory.saveBundleEnvironment(bundle, environment);

        final Collection<Object> dependencies = new HashSet<>();
        dependencies.addAll(testAsset.getDependencies());
        dependencies.add(testAsset.asset);
        dependencies.addAll(testAsset.otherVersions);

        createLanguageVariableIfNeeded();

        addLanguageVariableDependencies(dependencies,
                    testAsset.addLanguageVariableDependencies);

        final PublisherAPIImpl publisherAPI = new PublisherAPIImpl();

        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundle.getId());

        final PublishAuditHistory publishAuditHistory = new PublishAuditHistory();
        publishAuditStatus.setStatusPojo(publishAuditHistory);

        PublishAuditAPI.getInstance().insertPublishAuditStatus(publishAuditStatus);

        final File tempFile = com.dotmarketing.util.FileUtil
                .createTemporaryFile("sendPushPublishBundle_");

        HttpServer httpServer = createHttpServer(tempFile);

        try {
            httpServer.start();
            final PublishStatus publish = publisherAPI.publish(config);
            File bundleRoot = publish.getOutputFiles().get(0);

            assertTrue(tempFile.exists());
            assertTrue(tempFile.length() > 0);

            final File extractHere = new File(bundleRoot.getParent() + File.separator + config.getName());
            extractTarArchive(tempFile, extractHere);
            assertBundle(testAsset, dependencies, extractHere);
        } finally {
            httpServer.stop(0);
        }

        assertPushAsset(bundle, environment, publishingEndPoint, dependencies);
    }

    private void assertPushAsset(final Bundle bundle,
            final Environment environment,
            PushPublishingEndPoint publishingEndPoint,
            final Collection<Object> dependencies) throws DotDataException {

        final List<PushedAsset> pushedAssets = APILocator.getPushedAssetsAPI()
                .getPushedAssetsByBundleIdAndEnvironmentId(bundle.getId(), environment.getId());

        for (Object asset : dependencies) {
            final String assetId = DependencyManager.getBundleKey(asset);

            final List<PushedAsset> pushedAssetsByAsset = pushedAssets.stream()
                    .filter(pushedAsset -> pushedAsset.getAssetId().equals(assetId))
                    .collect(Collectors.toList());

            assertEquals(1, pushedAssetsByAsset.size());

            for (PushedAsset pushedAsset : pushedAssetsByAsset) {
                assertEquals(assetId, pushedAsset.getAssetId());
                assertEquals(bundle.getId(), pushedAsset.getBundleId());
                assertEquals(environment.getId(), pushedAsset.getEnvironmentId());
                assertEquals(publishingEndPoint.getId(), pushedAsset.getEndpointIds());
                assertEquals(PushPublisher.class, publishingEndPoint.getPublisher());
            }
        }

    }

    private HttpServer createHttpServer(File tempFile) throws IOException {
        final HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);

        httpServer.createContext("/api/bundlePublisher/publish", exchange -> {
            final InputStream responseBody = exchange.getRequestBody();
            FileUtils.copyInputStreamToFile(responseBody, tempFile);
            exchange.sendResponseHeaders( HttpURLConnection.HTTP_OK, 0);
            exchange.close();
        });

        return httpServer;
    }

    private void assertBundle(TestAsset testAsset, Collection<Object> dependencies, File bundleRoot)
            throws IOException {
        final Collection<File> filesExpected = new HashSet<>();
        filesExpected.addAll(
                FileTestUtil.assertBundleFile(bundleRoot, testAsset.asset,
                        testAsset.fileExpectedPath)
        );

        for (Object assetToAssert : dependencies) {
            final Collection<File> files = FileTestUtil
                    .assertBundleFile(bundleRoot, assetToAssert);
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

        //All the dependencies plus, the asset and the bundle xml and manifest
        int numberFilesExpected = filesExpected.size() + 2;
        final int numberFiles = files.size();

        final List<String> filesExpectedPath = filesExpected.stream()
                .map(file -> file.getAbsolutePath()).collect(Collectors.toList());
        final List<String> filePaths = files.stream().map(file -> file.getAbsolutePath())
                .collect(Collectors.toList());

        List<String> differences = getDifferences(numberFilesExpected, numberFiles,
                filesExpectedPath, filePaths);

        assertEquals(String.format(
                "Expected %d but get %d in %s\nExpected %s\nExisting %s\ndifference %s\n",
                numberFilesExpected, numberFiles, bundleRoot, filesExpectedPath, filePaths,
                differences),
                numberFilesExpected, numberFiles);
    }

    @Nullable
    private List<String> getDifferences(long numberFilesExpected, int numberFiles, List<String> filesExpectedPath, List<String> filePaths) {
        List<String> differences = null ;

        if (numberFilesExpected > numberFiles){
            differences = new ArrayList<>(filesExpectedPath);
            differences.removeAll(filePaths);
        } else if (numberFilesExpected < numberFiles) {
            differences = new ArrayList<>(filePaths);
            differences.removeAll(filesExpectedPath);
        }
        return differences;
    }


    public static List<Contentlet> getLanguageVariables() throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();
        final String langVarsQuery = "+contentType:" + LanguageVariableAPI.LANGUAGEVARIABLE;
        return APILocator.getContentletAPI().search(langVarsQuery, 0, -1,
                StringPool.BLANK, systemUser, false);
    }

    private static void addLanguageVariableDependencies(final Collection<Object> dependecies, boolean addLanguageVariableDependencies)
            throws DotDataException, DotSecurityException {

        List<Object> languageVariablesDependencies = getLanguagesVariableDependencies(
                addLanguageVariableDependencies, true, true).stream()
                    .filter(dependency -> !isHostFolderSystem(dependency))
                    .collect(Collectors.toList());

        if (!languageVariablesDependencies.isEmpty()){
            dependecies.addAll(languageVariablesDependencies);
        }
    }

    private static boolean isHostFolderSystem(Object dependency) {

        try {
            final Host  systemHost = APILocator.getHostAPI().findSystemHost();
            final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

            if (Contentlet.class.isInstance(dependency)){
                return ((Contentlet) dependency).getIdentifier().equals(systemHost.getIdentifier());
            } else  if (Folder.class.isInstance(dependency)){
                return ((Folder) dependency).getIdentifier().equals(systemFolder.getIdentifier());
            } else {
                return false;
            }
        } catch (DotDataException e) {
            return false;
        }
    }

    public static Set<Object> getLanguagesVariableDependencies()
            throws DotDataException, DotSecurityException {
        return getLanguagesVariableDependencies(
                true, true, true);
    }

    public static Set<Object> getLanguagesVariableDependencies(
            boolean addLanguageVariableDependencies,
            boolean addRulesDependencies,
            boolean addLiveAndWorking)
            throws DotDataException, DotSecurityException {

        final List<Contentlet> languageVariables = getLanguageVariables();

        final Set<Object> languagesVariableDependencies = getLanguagesVariableDependencies(
                languageVariables, addLanguageVariableDependencies,
                addRulesDependencies, addLiveAndWorking);

        if (addLanguageVariableDependencies) {
            languagesVariableDependencies.addAll(languageVariables);
        }

        return languagesVariableDependencies;
    }

    public static Set<Object> getLanguagesVariableDependencies(
            final List<Contentlet> languageVariables,
            boolean addLanguageVariableDependencies,
            boolean addRulesDependencies,
            boolean addLiveAndWorking)
            throws DotDataException, DotSecurityException {

        Set<Object> dependencies = new HashSet<>();

        for (final Contentlet langVariable : languageVariables) {
            dependencies.addAll(
                getLanguageVariable(langVariable, addLanguageVariableDependencies, addRulesDependencies,
                        addLiveAndWorking)
            );
        }

        Logger.info(PublisherAPIImplTest.class,"languageVariables " + languageVariables);
        if (!languageVariables.isEmpty() && addLanguageVariableDependencies) {
            dependencies.addAll(getLanguageVariablesContentTypeDependencies());

            final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();
            dependencies.add(systemFolder);

            final Host systemHost = APILocator.getHostAPI().findSystemHost();
            dependencies.add(systemHost);
        }

        return dependencies.stream()
                .filter(dependency -> !isHostFolderSystem(dependency))
                .collect(Collectors.toSet());
    }

    private static Collection<?> getLanguageVariablesContentTypeDependencies()
            throws DotDataException, DotSecurityException {

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI()
                .findSystemWorkflowScheme();

        return list(getLanguageVariablesContentType(), systemWorkflowScheme);
    }

    public static ContentType getLanguageVariablesContentType()
            throws DotSecurityException, DotDataException {

        final User systemUser = APILocator.systemUser();

        return APILocator.getContentTypeAPI(systemUser).find(LanguageVariableAPI.LANGUAGEVARIABLE);
    }

    private static Collection<Object> getLanguageVariable(final Contentlet langVariable,
            final boolean addLanguageVariableDependencies, final boolean addRulesDependencies,
            boolean addLiveAndWorking)
            throws DotDataException, DotSecurityException {

        final Collection<Object> dependencies = new HashSet<>();

        final User systemUser = APILocator.systemUser();
        final Host host = APILocator.getHostAPI().find(langVariable.getHost(), systemUser, false);

        if (addLiveAndWorking) {
            addContentletVersion(dependencies, host);
        }
        dependencies.add(host);

        if (addRulesDependencies) {
            List<Rule> ruleList = APILocator.getRulesAPI().getAllRulesByParent(host, systemUser, false);
            dependencies.addAll(ruleList);
        }

        if (addLanguageVariableDependencies) {
            final Language language = APILocator.getLanguageAPI().getLanguage(langVariable.getLanguageId());
            dependencies.add(language);

            if (addLiveAndWorking) {
                addContentletVersion(dependencies, langVariable);
            }
        }

        return dependencies;
    }

    private static void addContentletVersion(final Collection<Object> dependencies, final Contentlet contentlet)
            throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();

        final ContentletVersionInfo contentletVersionInfo
                = APILocator.getVersionableAPI().getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId()).get();

        final Contentlet workingContentlet =
                APILocator.getContentletAPI().find(contentletVersionInfo.getWorkingInode(), systemUser, false);

        if (!workingContentlet.getInode().equals(contentlet.getInode())) {
            dependencies.add(workingContentlet);
        }

        if (contentletVersionInfo.getLiveInode() != null &&
                !contentletVersionInfo.getWorkingInode().equals(contentletVersionInfo.getLiveInode()) &&
                !contentletVersionInfo.getLiveInode().equals(contentlet.getInode())){
            final Contentlet liveContentlet =
                    APILocator.getContentletAPI().find(contentletVersionInfo.getLiveInode(), systemUser, false);
            dependencies.add(liveContentlet);
        }
    }

    public static void extractTarArchive(File file, File folder) throws IOException {
        folder.mkdirs();
        try (FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                GzipCompressorInputStream gzip = new GzipCompressorInputStream(bis);
                TarArchiveInputStream tar = new TarArchiveInputStream(gzip)) {

            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tar.getNextEntry()) != null) {

                final String path = folder.getAbsolutePath() + File.separator + entry.getName();
                final File entryFile = new File(path);

                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                    continue;
                }

                entryFile.getParentFile().mkdirs();

                byte[] buf = new byte[1024];

                try (OutputStream outputStream = Files.newOutputStream(entryFile.toPath())) {

                    int bytesRead;
                    while ((bytesRead = tar.read(buf, 0, 1024)) > -1) {
                        outputStream.write(buf, 0, bytesRead);
                    }
                }
            }
        }
    }

    private static class TestAsset {
        Object asset;
        Map<ManifestItem, Collection<ManifestItem>> dependencies;
        String fileExpectedPath;
        boolean addLanguageVariableDependencies = true;
        Set<Object> otherVersions;

        public TestAsset(
                final Object asset,
                final Map<ManifestItem, Collection<ManifestItem>> dependencies,
                final Set<Object> otherVersions,
                final String fileExpectedPath) {

            this(asset, dependencies, otherVersions, fileExpectedPath, true);
        }

        public TestAsset(
                final Object asset,
                final Map<ManifestItem, Collection<ManifestItem>> dependencies,
                final String fileExpectedPath) {

            this(asset, dependencies, null, fileExpectedPath, true);
        }

        public TestAsset(
                final Object asset,
                final Map<ManifestItem, Collection<ManifestItem>> dependencies,
                final String fileExpectedPath,
                final boolean addLanguageVariableDependencies) {

            this(asset, dependencies, null, fileExpectedPath, addLanguageVariableDependencies);
        }

        public TestAsset(
                final Object asset,
                final Map<ManifestItem, Collection<ManifestItem>> dependencies,
                final Set<Object> otherVersions,
                final String fileExpectedPath,
                final boolean addLanguageVariableDependencies) {

            this.asset = asset;
            this.dependencies = dependencies;
            this.fileExpectedPath = fileExpectedPath;
            this.addLanguageVariableDependencies = addLanguageVariableDependencies;
            this.otherVersions = otherVersions != null ? otherVersions : Collections.EMPTY_SET;
        }

        public ManifestItemsMapTest manifestLines() {
            final ManifestItemsMapTest manifestItemsMap = new ManifestItemsMapTest();
            final ManifestItem assetManifestItem = (ManifestItem) asset;
            manifestItemsMap.add(assetManifestItem, "Added directly by User");

            manifestItemsMap.addDependencies(dependencies);

            return manifestItemsMap;
        }

        public Collection<Object> getDependencies() {
            return dependencies.values().stream()
                    .flatMap(dependencies -> dependencies.stream())
                    .collect(Collectors.toList());
        }
    }
}
