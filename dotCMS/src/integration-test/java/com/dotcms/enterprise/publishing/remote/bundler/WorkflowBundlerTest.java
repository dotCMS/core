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
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.FileUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.set;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class WorkflowBundlerTest {


    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] workflows() throws Exception {
        prepare();

        final WorkflowScheme workflowScheme = new WorkflowDataGen().nextPersisted();
        final WorkflowStep workflowStep = new WorkflowStepDataGen(workflowScheme.getId()).nextPersisted();
        final WorkflowAction workflowAction = new WorkflowActionDataGen(workflowScheme.getId(), workflowStep.getId())
                .nextPersisted();

        return new WorkflowBundlerTest.TestCase[]{
                new WorkflowBundlerTest.TestCase(workflowScheme, "/bundlers-test/workflow/workflow_with_steps_and_action.workflow.xml")
        };
    }


    /**
     * Method to Test: {@link WorkflowBundler#generate(File, BundlerStatus)}
     * When: Add a {@link WorkflowScheme} in a bundle
     * Should:
     * - The file should be create in: <bundle_root_path>/<workflow_id>.workflow.xml
     */

    @Test
    @UseDataProvider("workflows")
    public void addWorkflowInBundle(final WorkflowBundlerTest.TestCase testCase)
            throws DotBundleException, IOException, DotSecurityException, DotDataException {

        final WorkflowScheme workflowScheme = testCase.workflowScheme;

        final BundlerStatus status = mock(BundlerStatus.class);
        final WorkflowBundler bundler = new WorkflowBundler();
        final File bundleRoot = FileUtil.createTemporaryDirectory("WorkflowBundlerTest.addWorkflowInBundle");

        final FilterDescriptor filterDescriptor = new FileDescriptorDataGen().nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setWorkflows(set(workflowScheme.getId()));
        config.setOperation(PublisherConfig.Operation.PUBLISH);

        new BundleDataGen()
                .pushPublisherConfig(config)
                .addAssets(list(workflowScheme))
                .filter(filterDescriptor)
                .nextPersisted();

        bundler.setConfig(config);
        bundler.generate(bundleRoot, status);

        FileTestUtil.assertBundleFile(bundleRoot, workflowScheme, testCase.expectedFilePath);
    }

    private static class TestCase {
        WorkflowScheme workflowScheme;
        String expectedFilePath;

        public TestCase(final WorkflowScheme workflowScheme, final String expectedFilePath) {
            this.workflowScheme = workflowScheme;
            this.expectedFilePath = expectedFilePath;
        }
    }
}