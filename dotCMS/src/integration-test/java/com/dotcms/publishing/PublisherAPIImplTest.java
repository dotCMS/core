package com.dotcms.publishing;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.*;
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
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.liferay.util.FileUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.dotcms.util.CollectionsUtils.*;
import static org.jgroups.util.Util.assertEquals;

@RunWith(DataProviderRunner.class)
public class PublisherAPIImplTest {

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


    @DataProvider
    public static Object[] publishers() throws Exception {
        prepare();

        final Set<TestAsset> assets = set(
                getContentTypeWithHost(),
                getTemplateWithDependencies(),
                getContainerWithDependencies(),
                getFolderWithDependencies()
        );
        final List<Class<? extends Publisher>> publishers = list(
                GenerateBundlePublisher.class,
                PushPublisherMock.class
        );

        final List<TestCase> cases = new ArrayList<>();

        //final Set<Set<TestAsset>> sets = new HashSet();
        //sets.add(set(assets));
        final Set<Set<TestAsset>> sets = Sets.powerSet(assets);

        for (final Class<? extends Publisher> publisher : publishers) {
            for (Set<TestAsset> set : sets) {
                cases.add(new TestCase(publisher, set));
            }
        }
        return cases.toArray();
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
                list(host, parentFolder, folderContentType, systemWorkflowScheme,
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
                list(host, container_1, container_2, contentType, systemWorkflowScheme),
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
                list(host, workflowScheme, systemWorkflowScheme, contentTypeChild, relationship, category),
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
                list(host, contentType, systemWorkflowScheme),
                "/bundlers-test/container/container.containers.container.xml");
    }

    /**
     * Method to Test: {@link PublisherAPIImpl#publish(PublisherConfig)}
     * When: Add any assets
     * Should:
     * - create files correctly
     */
    @Test
    @UseDataProvider("publishers")
    public void publish(final TestCase testCase) throws DotPublishingException, DotSecurityException, IOException, DotDataException {
        final Class<? extends Publisher> publisher = testCase.publisher;
        final Set<TestAsset> testAssets = testCase.assets;
        final Set<Object> assets = new HashSet<>();
        final Set<Object> dependencies = new HashSet<>();

        for (final TestAsset testAsset : testAssets) {
            assets.add(testAsset.asset);
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
                .addAssets(list(assets))
                .filter(filterDescriptor)
                .nextPersisted();

        publisherAPI.publish(config);

        final File bundleRoot = BundlerUtil.getBundleRoot(config);

        for (final TestAsset testAsset : testAssets) {
            FileTestUtil.assertBundleFile(bundleRoot, testAsset.asset, testAsset.fileExpectedPath);
        }

        int nDependencies = 0;
        for (Object assetToAssert : dependencies) {
            final int dependenciesProcessed = FileTestUtil.assertBundleFile(bundleRoot, assetToAssert);
            nDependencies += dependenciesProcessed;
        }

        final String messagesPath = bundleRoot.getAbsolutePath() + File.separator + "messages";
        final String systemHostPath = bundleRoot.getAbsolutePath()
                + "/working/System Host/855a2d72-f2f3-4169-8b04-ac5157c4380c.contentType.json";

        final List<File> fileList = FileUtil.listFilesRecursively(bundleRoot);
        final long numberFiles =fileList.stream()
                .filter(file -> file.isFile())
                .filter(file -> !file.getParentFile().getAbsolutePath().equals(messagesPath))
                .filter(file -> !file.getAbsolutePath().equals(systemHostPath))
                .count();

        //All the dependencies plus, the asset and the bundle xml
        long numberFilesExpected = nDependencies + assets.size() + 1;

        assertEquals(String.format("Expected %d but get %d in %s",numberFilesExpected, numberFiles, bundleRoot),
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

    private static class TestAsset {
        Object asset;
        List<Object> expectedInBundle;
        String fileExpectedPath;

        public TestAsset(Object asset, List<Object> expectedInBundle, String fileExpectedPath) {
            this.asset = asset;
            this.expectedInBundle = expectedInBundle;
            this.fileExpectedPath = fileExpectedPath;

            //todo: uncomment ehrn merge into the performance branch
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
