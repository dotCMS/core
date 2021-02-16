package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.datagen.*;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.test.util.FileTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.FileUtil;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.set;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class TemplateBundlerTest {

    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    @DataProvider
    public static Object[] templates() throws Exception {
        prepare();

        final Host host = new SiteDataGen().nextPersisted();
        final Template advancedTemplate = new TemplateDataGen().host(host).nextPersisted();

        final Container container_1 = new ContainerDataGen().site(host).nextPersisted();
        final Container container_2 = new ContainerDataGen().site(host).nextPersisted();
        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(container_1)
                .withContainer(container_2)
                .next();

        final Template templateWithTemplateLayout = new TemplateDataGen()
                .host(host)
                .drawedBody(templateLayout)
                .nextPersisted();

        final Template advancedPublishTemplate = new TemplateDataGen().host(host).nextPersisted();
        TemplateDataGen.publish(advancedPublishTemplate);

        Template templateWithDifferentVersions = new TemplateDataGen().host(host).nextPersisted();
        APILocator.getTemplateAPI().publishTemplate(templateWithDifferentVersions, APILocator.systemUser(), false);
        templateWithDifferentVersions.setInode("");
        TemplateDataGen.save(templateWithDifferentVersions);

        return new TestCase[]{
                new TestCase(advancedTemplate),
                new TestCase(templateWithTemplateLayout),
                new TestCase(advancedPublishTemplate),
                new TestCase(templateWithDifferentVersions)
        };
    }


    /**
     * Method to Test: {@link TemplateBundler#generate(File, BundlerStatus)}
     * When: Add a {@link Template} in a bundle
     * Should:
     * - The file should be create in:
     * For Live Version: <bundle_root_path>/live/<container_host_name>/<template_id>.template.xml
     * For Working: <bundle_root_path>/working/<container_host_name>/<template_id>.template.xml
     *
     * If the Template has live and working version then to files will be created
     */
    @Test
    @UseDataProvider("templates")
    public void addTemplateInBundle(final TestCase testCase)
            throws DotBundleException, IOException, DotSecurityException, DotDataException {

        final Template template =  testCase.template;

        final BundlerStatus status = mock(BundlerStatus.class);
        final TemplateBundler bundler = new TemplateBundler();
        final File bundleRoot = FileUtil.createTemporaryDirectory("TemplateBundlerTest_addTemplateInBundle_");

        final FilterDescriptor filterDescriptor = new FileDescriptorDataGen().nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setTemplates(set(template.getIdentifier()));
        config.setOperation(PublisherConfig.Operation.PUBLISH);

        new BundleDataGen()
                .pushPublisherConfig(config)
                .addAssets(list(template))
                .filter(filterDescriptor)
                .nextPersisted();

        bundler.setConfig(config);
        bundler.generate(bundleRoot, status);

        final User systemUser = APILocator.systemUser();

        final Template workingTemplate = APILocator.getTemplateAPI().findWorkingTemplate(
                template.getIdentifier(), systemUser, false);

        FileTestUtil.assertBundleFile(bundleRoot, workingTemplate, testCase.expectedFilePath);

        final Template liveTemplate = APILocator.getTemplateAPI().findLiveTemplate(template.getIdentifier(), systemUser, false);

        if (liveTemplate != null && !liveTemplate.getInode().equals(workingTemplate.getInode())) {
            FileTestUtil.assertBundleFile(bundleRoot, liveTemplate, testCase.expectedFilePath);
        }
    }

    private static class TestCase{
        Template template;
        String expectedFilePath;

        public TestCase(final Template template, final String expectedFilePath) {
            this.template = template;
            this.expectedFilePath = expectedFilePath;
        }

        public TestCase(final Template template) {
            this(template, "/bundlers-test/template/template.template.xml");
        }
    }
}
