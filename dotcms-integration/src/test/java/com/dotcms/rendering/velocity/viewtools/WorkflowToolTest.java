package com.dotcms.rendering.velocity.viewtools;

import static com.dotcms.rendering.velocity.viewtools.WorkflowToolFireTestCase.Assertion;
import static com.dotmarketing.business.APILocator.getUserAPI;
import static com.dotmarketing.business.APILocator.systemUser;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.UniqueLanguageDataGen;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class WorkflowToolTest {

    private static final String SAVE_WORKFLOW_ID = "b9d89c803d";
    private static final String DELETE_WORKFLOW_ID = "777f1c6bc8";

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] fireTestCases() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        long english = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        long spaninsh = new UniqueLanguageDataGen().nextPersisted().getId();

        ContentType contentType = TestDataUtils.getEmployeeLikeContentType();
        Contentlet employeeContent = TestDataUtils.getEmployeeContent(true,1,contentType.id());

        // CASE 1 - Saving new Employee contentlet should succeed
        final WorkflowToolFireTestCase case1 = new WorkflowToolFireTestCase();

        HashMap<String, Object> case1Properties = new HashMap<>();
        case1Properties.put("contentType", contentType.name());
        case1Properties.put("languageId", english);
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

        final String KNOWN_EMPLOYEE_ID = employeeContent.getIdentifier();
        final long SPANISH_LANG_ID = spaninsh;

        // CASE 2 - Updating an existing Employee contentlet should succeed
        final WorkflowToolFireTestCase case2 = new WorkflowToolFireTestCase();

        HashMap<String, Object> case2Properties = new HashMap<>();
        case2Properties.put("identifier", KNOWN_EMPLOYEE_ID);
        case2Properties.put("contentType", contentType.name());
        case2Properties.put("languageId", SPANISH_LANG_ID);
        case2Properties.put("host1", "demo.dotcms.com");
        case2Properties.put("firstName", "Updated Name");
        case2Properties.put("lastName", "Updated Last Name");
        case2Properties.put("jobTitle", "support");
        case2Properties.put("email", "updated.mail@dotcms.com");
        case2Properties.put("mobile", "5555555");
        case2Properties.put("gender", "female");
        case2.setProperties(case2Properties);

        case2.setWorkflowActionId(SAVE_WORKFLOW_ID);
        case2.addAssertion(new Assertion((contentlet) -> UtilMethods.isSet(contentlet.getIdentifier()), "Contentlet identifier is null"));
        case2.addAssertion(new Assertion((contentlet) -> contentlet.getIdentifier().equals(KNOWN_EMPLOYEE_ID), "Unexpected Contentlet identifier"));
        case2.addAssertion(new Assertion((contentlet) -> "Updated Name".equals(contentlet.get("firstName")), "First name does not match"));
        case2.addAssertion(new Assertion((contentlet) -> "Updated Last Name".equals(contentlet.get("lastName")), "Last name does not match"));
        case2.addAssertion(new Assertion((contentlet) -> contentlet.getLanguageId()==SPANISH_LANG_ID, "Last name does not match"));

        return new WorkflowToolFireTestCase[] {
                case1, case2 
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
            if(savedContent!=null && UtilMethods.isSet(savedContent.getIdentifier())) {
                ContentletDataGen.remove(savedContent);
            }
        }
    }

}
