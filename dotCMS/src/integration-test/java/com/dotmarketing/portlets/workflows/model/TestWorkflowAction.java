package com.dotmarketing.portlets.workflows.model;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.web.VelocityWebUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.StringWriter;

/**
 * Test {@link WorkflowAction}
 * @author jsanca
 */
public class TestWorkflowAction extends IntegrationTestBase {

    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        user = APILocator.getUserAPI().getSystemUser();
    }

    /**
     * Test the getActionIdByName
     * - Creates a content type content
     * - Create a new content based on that content type
     * - Get the action Id by Name for Save, successfully
     * - Get the action Id by Name for Archive, successfully even if it is not in the firs step.
     * - Does checkin to persist the contentlet by firing saveg
     * - Ask again for the Archive, successfully
     * - Finally with the same contentlet, Get the action Id by Name for a non existing action, returns null as expected.
     */
    @Test
    public void test_condition_null_evaluateActionCondition () throws SystemException, DotDataException, DotSecurityException, PortalException {

        final HttpServletResponse response       = new MockHttpResponse();
        final HttpServletRequest  request        = new MockHeaderRequest((
                new MockSessionRequest(new MockAttributeRequest(new MockHttpRequest("localhost","/").request()).request())).request());

        final WorkflowAction      workflowAction = new WorkflowAction();
        workflowAction.setName("test");
        workflowAction.setCondition(null); // this is what we are testing
        final Context ctx = VelocityWebUtil.getVelocityContext(request, response);
        final StringWriter out = new StringWriter();
        VelocityUtil.getEngine().evaluate(ctx, out, "WorkflowVelocity:" + workflowAction.getName(), workflowAction.getCondition());
    }
}
