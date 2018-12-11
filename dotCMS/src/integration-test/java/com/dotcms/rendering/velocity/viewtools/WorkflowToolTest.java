package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;

import static com.dotcms.rendering.velocity.viewtools.WorkflowToolFireTestCase.*;
import static com.dotmarketing.business.APILocator.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class WorkflowToolTest {

    private static final String SAVE_WORKFLOW_ID = "b9d89c803d";

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] fireTestCases() {

        // CASE 1 - Saving new Employee contentlet should succeed
        final WorkflowToolFireTestCase case1 = new WorkflowToolFireTestCase();

        HashMap<String, Object> case1Properties = new HashMap<>();
        case1Properties.put("contentType", "Employee");
        case1Properties.put("languageId", 1);
        case1Properties.put("host1", "demo.dotcms.com");
        case1Properties.put("firstName", "Daniel");
        case1Properties.put("lastName", "Silva");
        case1Properties.put("jobTitle", "dev");
        case1Properties.put("email", "daniel.silva@dotcms.com");
        case1Properties.put("mobile", "5555555");
        case1Properties.put("gender", "male");
        case1.setProperties(case1Properties);

        case1.setWorkflowActionId(SAVE_WORKFLOW_ID);
        case1.addAssertion(new Assertion((contentlet) -> UtilMethods.isSet(contentlet.getIdentifier()), "Contentlet identifier is null"));
        case1.addAssertion(new Assertion((contentlet) -> "Daniel".equals(contentlet.get("firstName")), "First name does not match"));
        case1.addAssertion(new Assertion((contentlet) -> "Silva".equals(contentlet.get("lastName")), "Last name does not match"));

        return new WorkflowToolFireTestCase[] {
            case1
        };
    }

    @Test
    @UseDataProvider("fireTestCases")
    public void testFire(final WorkflowToolFireTestCase testCase)
            throws DotSecurityException, DotDataException {

        Contentlet savedContent = null;

        try {
            final User requestingUser = getUserAPI().loadUserById(testCase.getUserId(),
                    systemUser(), false);

            final ViewContext viewContext = mock(ViewContext.class);
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpSession session = mock(HttpSession.class);

            when(viewContext.getRequest()).thenReturn(request);
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER)).thenReturn(requestingUser);

            final WorkflowTool workflowTool = new WorkflowTool();
            workflowTool.init(viewContext);

            final ContentMap contentMap = workflowTool.fire(testCase.getProperties(), testCase.getWorkflowActionId());
            testCase.getAssertions().forEach((assertion) ->
                    Assert.assertTrue(assertion.getMessage(), assertion.getPredicate().test(contentMap.getContentObject()))
            );

            savedContent = contentMap.getContentObject();
        } finally {
            ContentletDataGen.remove(savedContent);
        }
    }

}
