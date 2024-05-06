package com.dotmarketing.portlets.workflows.actionlet;

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
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Test for the actionlet {@link MoveContentActionlet}
 * @author jsanca
 */
public class MoveContentActionletTest extends BaseWorkflowIntegrationTest {

    private static CreateSchemeStepActionResult schemeStepActionResult = null;
    private static WorkflowAPI workflowAPI = null;
    private static ContentletAPI contentletAPI = null;
    private static ContentTypeAPI contentTypeAPI = null;
    private static ContentType customContentType = null;
    private static String destinyFolder = "";

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        contentletAPI = APILocator.getContentletAPI();

        // creates the scheme and actions
        schemeStepActionResult = createSchemeStepActionActionlet
                ("MoveActionlet" + UUIDGenerator.generateUuid(), "step1", "action1",
                        MoveContentActionlet.class);

        // creates the type to trigger the scheme
        customContentType = createTestType(contentTypeAPI);

        // associated the scheme to the type
        workflowAPI.saveSchemesForStruct(new StructureTransformer(customContentType).asStructure(),
                Collections.singletonList(schemeStepActionResult.getScheme()));

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final List<WorkflowActionClassParameter> params = new ArrayList<>();
        final WorkflowActionClassParameter pathParam = new WorkflowActionClassParameter();
        pathParam.setActionClassId(schemeStepActionResult.getActionClass().getId());
        pathParam.setKey(MoveContentActionlet.PATH_KEY);
        destinyFolder =  "/destinyFolder/";
        pathParam.setValue("//" + defaultHost.getHostname() + destinyFolder);
        params.add(pathParam);
        workflowAPI.saveWorkflowActionClassParameters(params, APILocator.systemUser());

        setDebugMode(false);
    }

    private static ContentType createTestType(final ContentTypeAPI contentTypeAPI)
            throws DotDataException, DotSecurityException {

        final ContentType type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .description("Dot..")
                        .name("DotCopyActionletTest" + System.currentTimeMillis())
                        .owner(APILocator.systemUser().toString())
                        .variable("DotCopyActionletTest" + System.currentTimeMillis()).build());

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
        cleanupDebug(MoveContentActionletTest.class);
    } // cleanup

    private Contentlet saveCustomTestContentType() throws DotDataException, DotSecurityException {

        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final Contentlet contentlet = new Contentlet();
        final User user = APILocator.systemUser();
        contentlet.setContentTypeId(customContentType.id());
        contentlet.setOwner(APILocator.systemUser().toString());
        contentlet.setModDate(new Date());
        contentlet.setLanguageId(languageId);
        contentlet.setStringProperty("title", "Test Save");
        contentlet.setStringProperty("txt", "Test Save Text");
        contentlet.setHost(Host.SYSTEM_HOST);
        contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);

        // first save
        final Contentlet contentlet1 = contentletAPI.checkin(contentlet, user, false);

        return contentlet1;
    }

    /**
     *
     * @param original
     * @return
     */
    private void submitAction(final Contentlet original, final User user) throws Exception {

        // triggering the copy action
        original.setStringProperty(Contentlet.WORKFLOW_ACTION_KEY,
                schemeStepActionResult.getAction().getId());

        final WorkflowProcessor processor =
                workflowAPI.fireWorkflowPreCheckin(original, user);

        workflowAPI.fireWorkflowPostCheckin(processor);
    }

    /**
     * Method to test: {@link MoveContentActionlet#executeAction(WorkflowProcessor, Map)}
     * Given Scenario: Creates a workflow with a move actionlet and executes it
     * ExpectedResult: The Contentlet should be move to the new place
     *
     */
    @Test
    public void Test_Copy_Content_Expect_Success() throws Exception {

        final User systemUser = APILocator.systemUser();
        final Contentlet contentlet = this.saveCustomTestContentType();
        final Identifier identifier = APILocator.getIdentifierAPI().findFromInode(contentlet.getInode());
        final String originalPath   = identifier.getPath();

        this.submitAction(contentlet, systemUser);

        final Identifier identifier2 = APILocator.getIdentifierAPI().findFromInode(contentlet.getInode());
        final String     newPath     = identifier2.getPath();
        final String     parentPath  = identifier2.getParentPath();

        assertNotEquals(originalPath, newPath);
        assertEquals(parentPath, destinyFolder );
    }
}
