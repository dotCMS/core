package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class VelocityScriptActionletTest extends BaseWorkflowIntegrationTest {

    private static CreateSchemeStepActionResult schemeStepActionResult = null;
    private static WorkflowAPI workflowAPI = null;
    private static ContentletAPI contentletAPI = null;
    private static ContentTypeAPI contentTypeAPI = null;
    private static ContentType customContentType = null;
    private static final int LIMIT = 20;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        contentletAPI = APILocator.getContentletAPI();

        // creates the scheme and actions
        schemeStepActionResult = createSchemeStepActionActionlet
                ("VelocityScriptActionlet" + UUIDGenerator.generateUuid(), "step1", "action1",
                        VelocityScriptActionlet.class);

        // creates the type to trigger the scheme
        customContentType = createTestType(contentTypeAPI);
        saveActionletScriptCode(schemeStepActionResult);

        // associated the scheme to the type
        workflowAPI.saveSchemesForStruct(new StructureTransformer(customContentType).asStructure(),
                Collections.singletonList(schemeStepActionResult.getScheme()));

        setDebugMode(false);
    }

    private static void saveActionletScriptCode(final CreateSchemeStepActionResult schemeStepActionResult) throws DotDataException {

        final String code =  "#set($title = $content.getTitle()) \n" +
                    "#set($pow = $math.pow(2, 3)) \n" +
                    "#set($title = \"$title $pow\") \n" +
                    "$content.getMap().put(\"title\",\"$title\") \n" +
                    "$dotJSON.put(\"title\",$title) \n";
        final WorkflowActionClass workflowActionClass = schemeStepActionResult.getActionClass();
        final List<WorkflowActionClassParameter> params = new ArrayList<>();
        final User user = APILocator.systemUser();
        final WorkflowActionClassParameter parameter = new WorkflowActionClassParameter();
        parameter.setActionClassId(workflowActionClass.getId());
        parameter.setKey("script");
        parameter.setValue(code);
        params.add(parameter);
        final WorkflowActionClassParameter parameterResult = new WorkflowActionClassParameter();
        parameterResult.setActionClassId(workflowActionClass.getId());
        parameterResult.setKey("resultKey");
        parameterResult.setValue("result");
        params.add(parameterResult);
        workflowAPI.saveWorkflowActionClassParameters(params, user);
    }

    private static ContentType createTestType(final ContentTypeAPI contentTypeAPI)
            throws DotDataException, DotSecurityException {

        final ContentType type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .description("Dot..")
                        .name("DotVelocityActionletTest" + System.currentTimeMillis())
                        .owner(APILocator.systemUser().toString())
                        .variable("DotVelocityActionletTest" + System.currentTimeMillis()).build());

        final List<Field> fields = new ArrayList<>(type.fields());

        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

        return contentTypeAPI.save(type, fields);
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup()
            throws DotDataException, DotSecurityException, AlreadyExistException {

        if (null != customContentType) {

            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
            contentTypeAPI.delete(customContentType);
        }

        if (null != schemeStepActionResult) {

            cleanScheme(schemeStepActionResult.getScheme());
        }

        cleanupDebug(VelocityScriptActionletTest.class);
    } // cleanup

    @Test
    public void Test_Velocity_Script_Actionlet_Expect_Success() throws Exception {

        final Contentlet contentlet = new Contentlet();

        contentlet.setContentType(customContentType);
        contentlet.setProperty("title", "Test");
        contentlet.setProperty("txt", "Test Txt");

        final Contentlet checkinContentlet = contentletAPI.checkin(contentlet, APILocator.systemUser(), false);
        final Contentlet resultContentlet = workflowAPI.fireContentWorkflow(checkinContentlet, new ContentletDependencies.Builder()
                .modUser(APILocator.systemUser())
                .workflowActionId(schemeStepActionResult.getAction().getId()).build());

        final String expectedTitle = "Test 8";
        Assert.assertNotNull(resultContentlet);
        Assert.assertNotNull(resultContentlet.getTitle());
        Assert.assertNotNull(resultContentlet.getMap().get("result"));
        Assert.assertEquals(expectedTitle, resultContentlet.getTitle());
        final Map<String, Object> result  = (Map)resultContentlet.get("result");
        Assert.assertFalse(result.isEmpty());
        Assert.assertTrue(result.containsKey("dotJSON"));
        Assert.assertEquals(expectedTitle, DotJSON.class.cast(result.get("dotJSON")).get("title"));
    }

}
