package com.dotcms.publishing;

import com.dotcms.LicenseTestUtil;
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
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FieldRelationshipDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FilterDescriptorDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.PushPublishingEndPointDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TemplateLayoutDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.WorkflowActionDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.datagen.WorkflowStepDataGen;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
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
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publisher.util.dependencies.DependencyManager;
import com.dotcms.publishing.manifest.ManifestBuilder;
import com.dotcms.publishing.manifest.ManifestItem;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfoBuilder;
import com.dotcms.publishing.manifest.ManifestReason;
import com.dotcms.test.util.FileTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
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
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import com.sun.net.httpserver.HttpServer;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.set;
import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(DataProviderRunner.class)
public class PublisherAPIImplTest {

    public static final String DEPENDENCY_FROM_TEMPLATE = "Dependency from: ID: %s Title: %s";
    private static String MANIFEST_HEADERS = "INCLUDED/EXCLUDED,object type, Id, inode, title, site, folder, excluded by, reason to be evaluated";
    private static Contentlet languageVariableCreated;

    private static List<String> manifestMetadataLines = list("#Bundle ID:", "#Operation", "#Filter:");
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    public static void removeLanguageVariable(){
        ContentletDataGen.remove(languageVariableCreated);
    }

    public static void createLanguageVariableIfNeeded() throws DotSecurityException, DotDataException {
        final User systemUser = APILocator.systemUser();
        final List<Contentlet> langVariables = getLanguageVariables();

        final ContentType languageVariableContentType =
                APILocator.getContentTypeAPI(systemUser).find(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);

        if (langVariables.isEmpty()) {
            final Language language = new UniqueLanguageDataGen().nextPersisted();

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
                getContentWithSeveralVersions(),
                getUser(),
                getExperiment(),
                getExperimentWithSystemTemplate(),
                getExperimentVariantDifferentLayout(),
                getExperimentContentletInDifferentLang()
        };
    }

    private static TestAsset getExperimentWithSystemTemplate() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        ContentletDataGen.publish(experimentPage);

        final Language language = APILocator.getLanguageAPI()
                .getLanguage(experimentPage.getLanguageId());

        final ContentType pageContentType = experimentPage.getContentType();
        final Experiment experiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        final ExperimentVariant experimentNoDefaultVariant = experiment.trafficProportion().variants()
                .stream()
                .filter(experimentVariant -> !DEFAULT_VARIANT.name().equals(experimentVariant.id()))
                .findFirst()
                .orElseThrow();

        final Variant variant = APILocator.getVariantAPI().get(experimentNoDefaultVariant.id())
                .orElseThrow();

        final Template variantTemplate = APILocator.getTemplateAPI().systemTemplate();
        final Contentlet pageNewVersion = ContentletDataGen.createNewVersion(experimentPage, variant,
                new HashMap<>(Map.of(HTMLPageAssetAPI.TEMPLATE_FIELD,
                        variantTemplate.getIdentifier())));
        ContentletDataGen.publish(pageNewVersion);

        return new TestAsset(experiment,
                new HashMap<>(Map.of(
                        experiment, list(variant, experimentPage, pageNewVersion),
                        experimentPage, list(host, template, pageContentType, language)
                )),
                "/bundlers-test/experiment/experiment.json",
                true, true, experimentPage);
    }

    private static TestAsset getExperimentVariantDifferentLayout()
            throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        ContentletDataGen.publish(experimentPage);

        final Language language = APILocator.getLanguageAPI()
                .getLanguage(experimentPage.getLanguageId());

        final ContentType pageContentType = experimentPage.getContentType();
        final Experiment experiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        final ExperimentVariant experimentNoDefaultVariant = experiment.trafficProportion().variants()
                .stream()
                .filter(experimentVariant -> !DEFAULT_VARIANT.name().equals(experimentVariant.id()))
                .findFirst()
                .orElseThrow();

        final Variant variant = APILocator.getVariantAPI().get(experimentNoDefaultVariant.id())
                .orElseThrow();

        final Template variantTemplate = new TemplateDataGen().host(host).nextPersisted();
        final Contentlet pageNewVersion = ContentletDataGen.createNewVersion(experimentPage, variant,
                new HashMap<>(Map.of(HTMLPageAssetAPI.TEMPLATE_FIELD,
                        variantTemplate.getIdentifier())));
        ContentletDataGen.publish(pageNewVersion);

        return new TestAsset(experiment,
                new HashMap<>(Map.of(
                        experiment, list(variant, experimentPage, pageNewVersion),
                        variant, list(variantTemplate),
                        experimentPage, list(host, template, variantTemplate, pageContentType, language, variantTemplate)
                )),
                "/bundlers-test/experiment/experiment.json", true);
    }

    private static TestAsset getExperimentContentletInDifferentLang()
            throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        ContentletDataGen.publish(experimentPage);

        final Language language = APILocator.getLanguageAPI()
                .getLanguage(experimentPage.getLanguageId());

        final ContentType pageContentType = experimentPage.getContentType();
        final Experiment experiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        final ExperimentVariant experimentNoDefaultVariant = experiment.trafficProportion().variants()
                .stream()
                .filter(experimentVariant -> !DEFAULT_VARIANT.name().equals(experimentVariant.id()))
                .findFirst()
                .orElseThrow();

        final Variant variant = APILocator.getVariantAPI().get(experimentNoDefaultVariant.id())
                .orElseThrow();

        final ContentType contentType = new ContentTypeDataGen().host(host).nextPersisted();
        Language languageToContentlet = new LanguageDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType)
                .host(host)
                .languageId(languageToContentlet.getId())
                .variant(variant)
                .nextPersisted();

        final MultiTree multiTree = new MultiTreeDataGen()
                .setContentlet(contentlet)
                .setPage(experimentPage)
                .setVariant(variant)
                .setContainer(APILocator.getContainerAPI().systemContainer())
                .nextPersisted();

        return new TestAsset(experiment,
                new HashMap<>(Map.of(
                        experiment, list(variant, experimentPage, contentlet),
                        contentlet, list(languageToContentlet, contentType),
                        experimentPage, list(host, template, pageContentType, language),
                        contentType, list(host)
                )),
                "/bundlers-test/experiment/experiment.json", true);
    }

    private static TestAsset getExperiment() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        ContentletDataGen.publish(experimentPage);
        final Language language = APILocator.getLanguageAPI()
                .getLanguage(experimentPage.getLanguageId());

        final ContentType pageContentType = experimentPage.getContentType();
        final Experiment experiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        final ExperimentVariant experimentNoDefaultVariant = experiment.trafficProportion().variants()
                .stream()
                .filter(experimentVariant -> !DEFAULT_VARIANT.name().equals(experimentVariant.id()))
                .findFirst()
                .orElseThrow();

        final Variant variant = APILocator.getVariantAPI().get(experimentNoDefaultVariant.id())
                .orElseThrow();

        return new TestAsset(experiment,
                    new HashMap<>(Map.of(
                            experiment, list(variant, experimentPage),
                            experimentPage, list(host, template, pageContentType, language)
                    )),
                    "/bundlers-test/experiment/experiment.json", true);
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

        final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI()
                .findSystemWorkflowScheme();

        return new TestAsset(workingVersion,
                new HashMap<>(Map.of(
                        workingVersion, list(host, contentType, defaultLanguage),
                        contentType, list(systemWorkflowScheme)
                )),
                set(liveVersion),
                "/bundlers-test/contentlet/contentlet/contentlet.content.xml");
    }

    private static TestAsset getRuleWithDependencies() {
        final Host host = new SiteDataGen().nextPersisted();

        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        final Rule ruleWithPage = new RuleDataGen().page(htmlPageAsset).host(host).nextPersisted();

        return new TestAsset(ruleWithPage,
                new HashMap<>(Map.of(ruleWithPage, list(host))),
                "/bundlers-test/rule/rule.rule.xml", false);
    }

    private static TestAsset getUser() {
        final User user = new UserDataGen().nextPersisted();
        return new TestAsset(user,
                new HashMap<>(),
                "/bundlers-test/user/user.user.xml", false);
    }

    private static TestAsset getLanguageWithDependencies() {
        final Language language = new LanguageDataGen().nextPersisted();

        return new TestAsset(language, new HashMap<>(), "/bundlers-test/language/language.language.xml");
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
                    new HashMap<>(Map.of(
                            host, list(template, container, contentlet, containerContentType, contentType, folder, rule),
                            contentlet,list(contentType, language),
                            container,list(containerContentType),
                            folder, list(folderContentType)
                    )),
                    "/bundlers-test/host/host.host.xml");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TestAsset getLinkWithDependencies() {

        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final Link link = new LinkDataGen(folder)
                .hostId(host.getIdentifier())
                .nextPersisted();

        return new TestAsset(link, new HashMap<>(Map.of(link, list(host, folder))), "/bundlers-test/link/link.link.xml");
    }

    private static TestAsset getWorkflowWithDependencies() {

        final WorkflowScheme workflowScheme = new WorkflowDataGen().nextPersisted();
        final WorkflowStep workflowStep = new WorkflowStepDataGen(workflowScheme.getId()).nextPersisted();
        final WorkflowAction workflowAction = new WorkflowActionDataGen(workflowScheme.getId(), workflowStep.getId())
                .nextPersisted();


        return new TestAsset(workflowScheme, new HashMap<>(), "/bundlers-test/workflow/workflow_with_steps_and_action.workflow.xml");
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
                new HashMap<>(Map.of(
                        folderWithDependencies,
                        list(host, parentFolder, contentlet, folderContentType, link, subFolder, contentType),
                        contentlet, list(language, fileAssetContentType),
                        contentlet_2, list(language, fileAssetContentType),
                        subFolder, list(contentlet_2),
                        contentType, list(APILocator.getWorkflowAPI().findSystemWorkflowScheme()),
                        folderContentType, list(APILocator.getWorkflowAPI().findSystemWorkflowScheme())
                )),
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
                new HashMap<>(Map.of(
                        templateWithTemplateLayout, list(host, container_1, container_2, contentType),
                        container_1, list(contentType),
                        contentType, list(systemWorkflowScheme)
                )),
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
                new HashMap<>(Map.of(
                        contentType, list(host, workflowScheme, relationship, category, systemWorkflowScheme),
                        relationship, list(contentTypeChild)
                )),
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
                new HashMap<>(Map.of(
                        containerWithContentType, list(host, contentType),
                        contentType, list(systemWorkflowScheme)
                )),
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

        Set<?> languagesVariableDependencies = !User.class.isInstance(testAsset.asset) && !Category.class.isInstance(testAsset.asset) ?
                getLanguagesVariableDependencies(
                        languageVariables,
                        testAsset.addLanguageVariableDependencies, true, true)
                : Collections.EMPTY_SET;

        final FilterDescriptor filterDescriptor = new FilterDescriptorDataGen().nextPersisted();

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
                .operation(PublisherConfig.Operation.PUBLISH)
                .setSavePublishQueueElements(true)
                .nextPersisted();

        final PublishStatus publish = publisherAPI.publish(config);

        File bundleRoot = publish.getOutputFiles().get(0);

        final File extractHere = new File(bundleRoot.getParent() + File.separator + config.getName());
        extractTarArchive(bundleRoot, extractHere);

        final List<Contentlet> languageVariablesAddInBundle = testAsset.addLanguageVariableDependencies ?
                getLanguageVariables() : Collections.EMPTY_LIST;

        final Collection<Object> dependencies = getJustOneList(testAsset.otherVersions,
                testAsset.getDependencies(),
                languageVariablesAddInBundle, languagesVariableDependencies);

        final ManifestItemsMapTest manifestLines = testAsset.manifestLines();

        if (Category.class.isInstance(testAsset.asset)) {
            List<Category> topLevelCategories = APILocator.getCategoryAPI()
                    .findTopLevelCategories(APILocator.systemUser(), true);

            final List<ManifestItem> manifestItems = topLevelCategories.stream()
                    .map(category -> (ManifestItem) category).collect(Collectors.toList());
            manifestLines.addDependencies(new HashMap<>(Map.of((Category) testAsset.asset, manifestItems)));

            for (Category topLevel : topLevelCategories) {
                dependencies.add(topLevel);

                final List<Category> children = APILocator.getCategoryAPI()
                        .findChildren(APILocator.systemUser(), topLevel.getInode(), true,
                                null);
                for (Category child : children) {
                    dependencies.add(child);
                }

                final List<ManifestItem> childManifestItems = children.stream()
                        .map(category -> (ManifestItem) category).collect(Collectors.toList());
                manifestLines.addDependencies(new HashMap<>(Map.of(topLevel, childManifestItems)));
            }

            dependencies.addAll(APILocator.getCategoryAPI().findAll(APILocator.systemUser(), true));
        }

        assertBundle(testAsset, dependencies, extractHere);

        if (!Rule.class.isInstance(testAsset.asset) && !User.class.isInstance(testAsset.asset)) {
            if (!Category.class.isInstance(testAsset.asset)) {
                manifestLines.addExcludes(new HashMap<>(Map.of("Excluded System Folder/Host/Container/Template",
                        list(APILocator.getHostAPI().findSystemHost(),
                                APILocator.getFolderAPI().findSystemFolder()))));

                addLanguageVariableManifestItem(
                        manifestLines,
                        testAsset.addLanguageVariableDependencies,
                        languageVariablesAddInBundle
                );
            }

            final String manifestFilePath = extractHere.getAbsolutePath() + File.separator +
                    ManifestBuilder.MANIFEST_NAME;
            final File manifestFile = new File(manifestFilePath);

            if (testAsset.addExcludeForSystemTemplate()) {
                final ManifestItem dependsFrom = (ManifestItem) testAsset.systemTemplateDependsFrom;
                final String evaluateTemplateReason = dependsFrom != null ?
                        String.format(DEPENDENCY_FROM_TEMPLATE,
                                dependsFrom.getManifestInfo().id(),
                                dependsFrom.getManifestInfo().title()) : "";

                manifestLines.addExclude(
                        APILocator.getTemplateAPI().systemTemplate(),
                        evaluateTemplateReason,"Excluded System Folder/Host/Container/Template");
            }

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
                            String.format(DEPENDENCY_FROM_TEMPLATE, languageVariable.getIdentifier(), languageVariable.getTitle())
                    ));

            final long languageId = languageVariable.getLanguageId();
            final Language language = APILocator.getLanguageAPI().getLanguage(languageId);

            manifestLines.add(languageVariablesContentType,
                    list(
                            String.format(DEPENDENCY_FROM_TEMPLATE, languageVariable.getIdentifier(), languageVariable.getTitle()),
                            String.format(DEPENDENCY_FROM_TEMPLATE, language.getId(), language.getLanguage())
                    )
            );
        }

        if (!languageVariablesAddInBundle.isEmpty()) {

            final WorkflowScheme systemWorkflowScheme = APILocator.getWorkflowAPI()
                    .findSystemWorkflowScheme();
            manifestLines.add(systemWorkflowScheme,
                    String.format(DEPENDENCY_FROM_TEMPLATE, languageVariablesContentType.id(), languageVariablesContentType.name()));

            final Host systemHost = APILocator.getHostAPI().findSystemHost();
            manifestLines.addExclude(systemHost,
                    String.format(DEPENDENCY_FROM_TEMPLATE, languageVariablesContentType.id(), languageVariablesContentType.name()),
                    "Excluded System Folder/Host/Container/Template");

            final Contentlet languageVariable = languageVariablesAddInBundle.get(0);
            final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();
            manifestLines.addExclude(systemFolder,
                    String.format(DEPENDENCY_FROM_TEMPLATE, languageVariable.getIdentifier(), languageVariable.getTitle()),
                    "Excluded System Folder/Host/Container/Template");

        }
    }

    private static Collection<Object> getJustOneList(Collection<?>... collections){
        return Arrays.stream(collections)
                .flatMap(collection -> collection.stream())
                .collect(Collectors.toSet());
    }

    public static void assertManifestFile(final File manifestFile,
            final ManifestItemsMapTest manifestItems) throws IOException {
        assertManifestFile(manifestFile, manifestItems, manifestMetadataLines);
    }

    public static void assertManifestFile(final File manifestFile,
            final ManifestItemsMapTest manifestItems, final List<String> manifestMetadataLines) throws IOException {

        assertTrue(manifestFile.exists());

        manifestItems.startCheck();

        try(BufferedReader csvReader = new BufferedReader(new FileReader(manifestFile))) {
            String line;
            int nLines = 0;

            final StringBuffer buffer = new StringBuffer();

            while ((line = csvReader.readLine()) != null) {

                buffer.append(line + "\n");

                if (nLines < manifestMetadataLines.size()) {
                    assertTrue("Wrong Metadata " + nLines + " " + line, line.startsWith(manifestMetadataLines.get(nLines)));
                } else if (nLines == manifestMetadataLines.size()) {
                    assertEquals("Wrong headers", MANIFEST_HEADERS, line);
                } else {
                    final boolean contains = manifestItems.contains(line);
                    assertTrue(manifestItems + " not contain " + line, contains);
                }

                nLines++;
            }

            assertEquals("manifestItems\n" + manifestItems + "\nManifest content\n" + buffer,
                    manifestItems.size(), nLines - (manifestMetadataLines.size() + 1) );
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

        if (!User.class.isInstance(testAsset.asset)) {
            addLanguageVariableDependencies(dependencies,
                    testAsset.addLanguageVariableDependencies);
        }

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

        if (!User.class.isInstance(testAsset.asset)) {
            assertPushAsset(bundle, environment, publishingEndPoint, dependencies);
        }
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

        if (testAsset != null) {
            filesExpected.addAll(
                    FileTestUtil.assertBundleFile(bundleRoot, testAsset.asset,
                            testAsset.fileExpectedPath)
            );
        }

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

        //All the assets plus the manifest file
        int numberFilesExpected = filesExpected.size() + 1;
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
        final String langVarsQuery = "+contentType:" + LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME;
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

        return APILocator.getContentTypeAPI(systemUser).find(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);
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
        private boolean excludeForSystemTemplate;
        private ManifestItem systemTemplateDependsFrom;

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
                final String fileExpectedPath,
                final boolean addLanguageVariableDependencies,
                final boolean excludeForSystemTemplate,
                final ManifestItem systemTemplateDependsFrom) {

            this(asset, dependencies, null, fileExpectedPath, addLanguageVariableDependencies, excludeForSystemTemplate, systemTemplateDependsFrom);
        }

        public TestAsset(
                final Object asset,
                final Map<ManifestItem, Collection<ManifestItem>> dependencies,
                final Set<Object> otherVersions,
                final String fileExpectedPath,
                final boolean addLanguageVariableDependencies) {
            this(asset, dependencies, otherVersions, fileExpectedPath, addLanguageVariableDependencies, false, null);
        }

        public TestAsset(
                final Object asset,
                final Map<ManifestItem, Collection<ManifestItem>> dependencies,
                final Set<Object> otherVersions,
                final String fileExpectedPath,
                final boolean addLanguageVariableDependencies,
                final boolean excludeForSystemTemplate,
                final ManifestItem systemTemplateDependsFrom) {

            this.asset = asset;
            this.dependencies = dependencies;
            this.fileExpectedPath = fileExpectedPath;
            this.addLanguageVariableDependencies = addLanguageVariableDependencies;
            this.otherVersions = otherVersions != null ? otherVersions : Collections.EMPTY_SET;
            this.excludeForSystemTemplate = excludeForSystemTemplate;
            this.systemTemplateDependsFrom = systemTemplateDependsFrom;
        }

        public ManifestItemsMapTest manifestLines() {
            final ManifestItemsMapTest manifestItemsMap = new ManifestItemsMapTest();
            final ManifestItem assetManifestItem = "CAT".equals(asset) ? getCategoryManifestItem()
                    : (ManifestItem) asset;
            manifestItemsMap.add(assetManifestItem, "Added directly by User");

            manifestItemsMap.addDependencies(dependencies);

            return manifestItemsMap;
        }

        public Collection<Object> getDependencies() {
            return dependencies.values().stream()
                    .flatMap(dependencies -> dependencies.stream())
                    .collect(Collectors.toList());
        }

        public boolean addExcludeForSystemTemplate() {
            return excludeForSystemTemplate;
        }

        public ManifestItem getSystemTemplateDependsFrom() {
            return systemTemplateDependsFrom;
        }
    }

    @NotNull
    private static ManifestItem getCategoryManifestItem() {
        return (ManifestItem) () -> new ManifestInfoBuilder()
                .objectType(PusheableAsset.CATEGORY.getType())
                .title("Syncing All Categorie")
                .build();
    }

    /**
     * Method to Test: {@link PublisherAPIImpl#publish(PublisherConfig)}
     * When: Add a {@link Category} into a Bundle
     * Should: Add all the {@link Category} into the Bundle
     */
    @Test
    public void AddAllCategoryIntoBundle() throws Exception {
        prepare();
        final Class<? extends Publisher> publisher = GenerateBundlePublisher.class;

        final FilterDescriptor filterDescriptor = new FilterDescriptorDataGen().nextPersisted();

        final PublisherAPIImpl publisherAPI = new PublisherAPIImpl();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setPublishers(list(publisher));
        config.setOperation(PublisherConfig.Operation.PUBLISH);
        config.setLuceneQueries(list());
        config.setId("PublisherAPIImplTest_" + System.currentTimeMillis());

        new BundleDataGen()
                .pushPublisherConfig(config)
                .addAsset("CAT", PusheableAsset.CATEGORY)
                .filter(filterDescriptor)
                .operation(PublisherConfig.Operation.PUBLISH)
                .setSavePublishQueueElements(true)
                .nextPersisted();

        final PublishStatus publish = publisherAPI.publish(config);

        File bundleRoot = publish.getOutputFiles().get(0);

        final File extractHere = new File(bundleRoot.getParent() + File.separator + config.getName());
        extractTarArchive(bundleRoot, extractHere);

        List topLevelCategories = APILocator.getCategoryAPI()
                .findAll(APILocator.systemUser(), true);

        assertBundle(null, topLevelCategories, extractHere);

        final String manifestFilePath = extractHere.getAbsolutePath() + File.separator +
                ManifestBuilder.MANIFEST_NAME;
        final File manifestFile = new File(manifestFilePath);

        final ManifestItemsMapTest manifestItemsMapTest = new ManifestItemsMapTest();
        final ManifestItem manifestItem = () -> new ManifestInfoBuilder()
                .objectType(PusheableAsset.CATEGORY.getType())
                .title("Syncing All Categories")
                .build();

        manifestItemsMapTest.add(manifestItem, ManifestReason.INCLUDE_BY_USER.getMessage());

        assertManifestFile(manifestFile, manifestItemsMapTest);
    }

    /**
     * Tests All filter related methods
     *  First we mock real assets-path.
     *  then we create a brand-new descriptor file then we..
     *  Try different scenarios like removing the file then trying to locate it via finders using the key
     *  Then we update the descriptor using the upsert method and verify the changes take place
     *  Finally we test the descriptor can be removed and does not show up on the finders result
     */
    @Test
    @Ignore("Fix me publisherAPI.init() uses memoized version of filter path, changing ASSET_REAL_PATH will not be picked up after it is set")
    public void testFilterDescriptors() throws IOException {

        final String realAssetsRootPath = Config.getStringProperty("ASSET_REAL_PATH", null);

        try {
            final Path base = Files.createTempDirectory("tmp_real_assets");
            final String canonicalPath = base.toFile().getCanonicalPath();
            Config.setProperty("ASSET_REAL_PATH", canonicalPath);

            final Path path = Paths.get(canonicalPath, "server", "publishing-filters");

            final File dir = path.toFile();
            if (dir.exists()) {
                Assert.assertTrue(dir.delete());
            }

            final PublisherAPIImpl publisherAPI = new PublisherAPIImpl();
            publisherAPI.init();

            Assert.assertTrue(path.toFile().exists());

            final String filterKey = "foo";

            final String title = "any";

            Assert.assertFalse(publisherAPI.existsFilterDescriptor(filterKey));

            final FilterDescriptor filterDescriptor = new FilterDescriptorDataGen().key(filterKey)
                    .title(title).nextPersisted();

            publisherAPI.upsertFilterDescriptor(filterDescriptor);

            Assert.assertTrue(publisherAPI.existsFilterDescriptor(filterKey));

            final FilterDescriptor descriptorByKey = publisherAPI.getFilterDescriptorByKey(
                    filterKey);

            Assert.assertEquals(filterDescriptor, descriptorByKey);

            Assert.assertTrue(publisherAPI.deleteFilterDescriptor(filterKey));

            Assert.assertFalse(publisherAPI.existsFilterDescriptor(filterKey));

            publisherAPI.upsertFilterDescriptor(filterDescriptor);

            Assert.assertTrue(publisherAPI.existsFilterDescriptor(filterKey));

            final FilterDescriptor modified = new FilterDescriptorDataGen().key(filterKey)
                    .title("modified").sort("1").forcePush(true).next();

            publisherAPI.upsertFilterDescriptor(modified);

            final FilterDescriptor afterUpsert = publisherAPI.getFilterDescriptorByKey(filterKey);

            Assert.assertNotNull(afterUpsert);

            Assert.assertEquals(afterUpsert.getTitle(), "modified");
            Assert.assertEquals(afterUpsert.getSort(), "1");

            Assert.assertTrue(publisherAPI.deleteFilterDescriptor(filterKey));
        }finally {
            Config.setProperty("ASSET_REAL_PATH", realAssetsRootPath);
        }

    }
}
