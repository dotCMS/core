package com.dotmarketing.portlets.workflows.business;

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
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorkflowFactoryTest extends BaseWorkflowIntegrationTest {

    private static CreateSchemeStepActionResult schemeStepActionResult = null;
    private static WorkflowAPI                  workflowAPI            = null;
    private static ContentletAPI contentletAPI          = null;
    private static ContentType type                   = null;
    private static Contentlet contentlet             = null;


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();
        WorkflowFactoryTest.workflowAPI              = APILocator.getWorkflowAPI();
        final ContentTypeAPI contentTypeAPI          = APILocator.getContentTypeAPI(APILocator.systemUser());
        WorkflowFactoryTest.contentletAPI            = APILocator.getContentletAPI();

        // creates the scheme and actions
        WorkflowFactoryTest.schemeStepActionResult   = WorkflowFactoryTest.createSchemeStepActionActionlet
                ("saveContentScheme" + UUIDGenerator.generateUuid(), "step1", "action1", SaveContentActionlet.class);

        // creates the type to trigger the scheme
        WorkflowFactoryTest.createTestType(contentTypeAPI);

        // associated the scheme to the type
        WorkflowFactoryTest.workflowAPI.saveSchemesForStruct(new StructureTransformer(WorkflowFactoryTest.type).asStructure(),
                Arrays.asList(WorkflowFactoryTest.schemeStepActionResult.getScheme()));

    }

    private static void createTestType(final ContentTypeAPI contentTypeAPI) throws DotDataException, DotSecurityException {

        WorkflowFactoryTest.type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .description("DotWorkflowFactoryTest...")
                        .name("DotWorkflowFactoryTest").owner(APILocator.systemUser().toString())
                        .variable("DotWorkflowFactoryTest").build());

        final List<Field> fields = new ArrayList<>(WorkflowFactoryTest.type.fields());

        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(WorkflowFactoryTest.type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(WorkflowFactoryTest.type.id()).dataType(DataTypes.TEXT).indexed(true).build());

        WorkflowFactoryTest.type = contentTypeAPI.save(WorkflowFactoryTest.type, fields);
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup() throws DotDataException, DotSecurityException, AlreadyExistException {

        try {

            if (null != WorkflowFactoryTest.contentlet) {

                WorkflowFactoryTest.contentletAPI.destroy(WorkflowFactoryTest.contentlet, APILocator.systemUser(), false);
            }
        } finally {

            try {

                if (null != WorkflowFactoryTest.schemeStepActionResult) {

                    WorkflowFactoryTest.cleanScheme(WorkflowFactoryTest.schemeStepActionResult.getScheme());
                }
            } finally {

                if (null != WorkflowFactoryTest.type) {

                    ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
                    contentTypeAPI.delete(WorkflowFactoryTest.type);
                }
            }
        }
    } // cleanup

    @Test
    public void force_workflow_scheme_delete_without_license_delete_success_Test() throws Exception {

        final List<WorkflowScheme> workflowSchemesBeforeDelete = workflowAPI.findSchemesForContentType(type);
        final WorkFlowFactory workFlowFactory = FactoryLocator.getWorkFlowFactory();

        Assert.assertNotNull(workflowSchemesBeforeDelete);
        Assert.assertTrue(workflowSchemesBeforeDelete.size() > 0);

        runNoLicense(()-> {

            try {

                workFlowFactory.deleteSchemeForStruct(type.id()); // does not delete anything
                final List<WorkflowScheme> workflowSchemesAfterDelete = workflowAPI.findSchemesForContentType(type);
                Assert.assertNotNull(workflowSchemesAfterDelete);
                Assert.assertTrue(workflowSchemesAfterDelete.size() > 0);
            } catch (Exception e) {
                // quiet
            }

            workFlowFactory.forceDeleteSchemeForContentType(type.id());
        });


        final List<WorkflowScheme> workflowSchemesAfterForceDelete = workflowAPI.findSchemesForContentType(type);

        Assert.assertNotNull(workflowSchemesAfterForceDelete);
        Assert.assertTrue(workflowSchemesAfterForceDelete.size() == 0);
    }
}
