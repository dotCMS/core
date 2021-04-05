package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.test.util.FileTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.FileUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static com.dotcms.util.CollectionsUtils.*;
import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class ContentTypeBundlerTest {

    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    @DataProvider
    public static Object[] contentTypes() throws Exception {
        prepare();

        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Folder folder = new FolderDataGen().nextPersisted();
        final ContentType contentTypeWithFolder = new ContentTypeDataGen()
                .folder(folder)
                .nextPersisted();

        final WorkflowScheme workflowScheme = new WorkflowDataGen().nextPersisted();

        final ContentType contentTypeWithWorkflow = new ContentTypeDataGen()
                .host(host)
                .workflowId(workflowScheme.getId())
                .nextPersisted();

        final Field field = new FieldDataGen().next();

        final ContentType contentTypeWithField = new ContentTypeDataGen()
                .host(host)
                .fields(list(field))
                .nextPersisted();

        final ContentType contentTypeWithAllDependencies = new ContentTypeDataGen()
                .host(host)
                .fields(list(field))
                .workflowId(workflowScheme.getId())
                .nextPersisted();

        return new TestCase[]{
                new TestCase(contentType),
                new TestCase(contentTypeWithFolder),
                new TestCase(contentTypeWithWorkflow),
                new TestCase(contentTypeWithField, "/bundlers-test/content_types/content_types_with_fields.contentType.json"),
                new TestCase(contentTypeWithAllDependencies, "/bundlers-test/content_types/content_types_with_fields.contentType.json")
        };
    }

    /**
     * Method to Test: {@link ContentTypeBundler#generate(File, BundlerStatus)}
     * When: Add a {@link ContentType} in a bundle
     * Should:
     * - The file should be create in:
     * For Live Version: <bundle_root_path>/live/<content_type_host_name>/<content_type_inode>.contentType.json
     * For Working: <bundle_root_path>/working/<content_type_host_name>/<content_type_inode>.contentType.json
     *
     * If the ContentType has live and working version then to files will be created
     */
    @Test
    @UseDataProvider("contentTypes")
    public void addContentTypeInBundle(final TestCase testCase)
            throws DotBundleException, IOException, DotSecurityException, DotDataException {

        final ContentType contentType =  testCase.contentType;

        final BundlerStatus status = mock(BundlerStatus.class);
        final ContentTypeBundler bundler = new ContentTypeBundler();
        final File bundleRoot = FileUtil.createTemporaryDirectory("ContentTypeBundlerTest_addContentTypeInBundle_");

        final FilterDescriptor filterDescriptor = new FilterDescriptorDataGen().nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setStructures(set(contentType.id()));
        config.setOperation(PublisherConfig.Operation.PUBLISH);

        new BundleDataGen()
                .pushPublisherConfig(config)
                .addAssets(list(contentType))
                .filter(filterDescriptor)
                .nextPersisted();

        bundler.setConfig(config);
        bundler.generate(bundleRoot, status);

        FileTestUtil.assertBundleFile(bundleRoot, contentType, testCase.expectedFilePath);
    }

    private static class TestCase{
        ContentType contentType;
        String expectedFilePath;

        public TestCase(final ContentType contentType, final String expectedFilePath) {
            this.contentType = contentType;
            this.expectedFilePath = expectedFilePath;
        }

        public TestCase(final ContentType contentType) {
            this(contentType, "/bundlers-test/content_types/content_types_without_fields.contentType.json");
        }
    }
}
