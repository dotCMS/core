package com.dotmarketing.portlets.workflows.model;

import com.dotcms.IntegrationTestBase;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.web.VelocityWebUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;
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
     * Test the evaluate
     * - Creates a response, request
     * - Create a proxy workflow action with name but null condition
     * - Tries to evaluate the velocity that was getting NPE
     */
    @Test
    public void test_condition_null_evaluate () throws SystemException, DotDataException, DotSecurityException, PortalException {

        final HttpServletResponse response       = new MockHttpResponse();
        final HttpServletRequest  request        = new MockHeaderRequest((
                new MockSessionRequest(new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost","/").request()).request())).request());

        final WorkflowAction      workflowAction = new WorkflowAction();
        workflowAction.setName("test");
        workflowAction.setCondition(null); // this is what we are testing
        final Context ctx = VelocityWebUtil.getVelocityContext(request, response);
        final StringWriter out = new StringWriter();
        VelocityUtil.getEngine().evaluate(ctx, out, "WorkflowVelocity:" + workflowAction.getName(), workflowAction.getCondition());
    }
}
