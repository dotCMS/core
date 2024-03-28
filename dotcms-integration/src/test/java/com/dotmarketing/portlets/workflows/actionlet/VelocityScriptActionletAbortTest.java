package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class VelocityScriptActionletAbortTest extends BaseWorkflowIntegrationTest {



    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: abortProcessor and stop
     * Given Scenario: Do nothing, so the abort should be false
     * ExpectedResult: abort should be false
     *
     */
    @Test
    public void Test_Velocity_Script_Actionlet_Abort_Non_Stopped() throws Exception {

        final HttpServletRequest currentRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        try {

            HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
            final Contentlet contentlet = new Contentlet();

            final WorkflowActionClassParameter scriptParameter = new WorkflowActionClassParameter();
            final WorkflowActionClassParameter keyParameter = new WorkflowActionClassParameter();
            scriptParameter.setValue("##nothing");
            keyParameter.setValue(null);
            final Map<String, WorkflowActionClassParameter> params = new HashMap<>();
            params.put("script", scriptParameter);
            params.put("resultKey", keyParameter);
            final VelocityScriptActionlet velocityScriptActionlet = new VelocityScriptActionlet();
            final WorkflowProcessor processor = new WorkflowProcessor(contentlet, APILocator.systemUser());

            Assert.assertFalse(processor.abort());
            velocityScriptActionlet.executeAction(processor, params);
            Assert.assertFalse(processor.abort());
        } finally {

            if (null != currentRequest) {
                HttpServletRequestThreadLocal.INSTANCE.setRequest(currentRequest);
            }
        }
    }

    /**
     * Method to test: abortProcessor and stop
     * Given Scenario: Do nothing, so the abort should be true
     * ExpectedResult: abort should be true
     *
     */
    @Test
    public void Test_Velocity_Script_Actionlet_Abort_Stopped() throws Exception {

        final HttpServletRequest currentRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        try {

            HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
            final Contentlet contentlet = new Contentlet();

            final WorkflowActionClassParameter scriptParameter = new WorkflowActionClassParameter();
            final WorkflowActionClassParameter keyParameter    = new WorkflowActionClassParameter();
            scriptParameter.setValue("$workflow.abortProcessor()");
            keyParameter.setValue(null);
            final Map<String, WorkflowActionClassParameter> params = new HashMap<>();
            params.put("script", scriptParameter);
            params.put("resultKey", keyParameter);
            final VelocityScriptActionlet  velocityScriptActionlet = new VelocityScriptActionlet();
            final WorkflowProcessor processor = new WorkflowProcessor(contentlet, APILocator.systemUser());

            Assert.assertFalse(processor.abort());
            velocityScriptActionlet.executeAction(processor, params);
            Assert.assertTrue(processor.abort());
        } finally {

            if (null != currentRequest) {
                HttpServletRequestThreadLocal.INSTANCE.setRequest(currentRequest);
            }
        }
    }

}
