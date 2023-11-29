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
import com.dotcms.datagen.WorkflowDataGen;
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
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.util.UUIDGenerator;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static com.dotmarketing.portlets.workflows.model.WorkflowState.*;
import static org.junit.Assert.assertEquals;

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

    /**
     * Method to test: {@link WorkflowFactoryImpl#countAllSchemasSteps()}
     * When: create a new Workflow with 5 steps
     * Should: the count must be 5 more than before
     *
     * @throws DotDataException
     */
    @Test
    public void countAllWorkflowSteps() throws DotDataException {

        final long firstCount = FactoryLocator.getWorkFlowFactory().countAllSchemasSteps();

        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions_1 = getStepsAndActions();

        final WorkflowScheme workflow_1= new WorkflowDataGen().name("Testing")
                .stepAndAction(workflowStepsAndActions_1).nextPersistedWithStepsAndActions();

        final long secondCount = FactoryLocator.getWorkFlowFactory().countAllSchemasSteps();
        assertEquals(firstCount + 5, secondCount);

        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions_2 = getStepsAndActions();

        final WorkflowScheme workflow_2 = new WorkflowDataGen().name("Testing")
                .stepAndAction(workflowStepsAndActions_2).nextPersistedWithStepsAndActions();

        final long thirdCount = FactoryLocator.getWorkFlowFactory().countAllSchemasSteps();
        assertEquals(secondCount + 5, thirdCount);
    }

    /**
     * Method to test: {@link WorkflowFactoryImpl#countAllSchemasSteps()}
     * When: create a new Workflow with 5 steps, and later archive it
     * Should: not take account the archived Schema
     *
     * @throws DotDataException
     */
    @Test
    public void notCountStepsFromArchivedSchema() throws DotDataException, DotSecurityException, AlreadyExistException {

        final long firstCount = FactoryLocator.getWorkFlowFactory().countAllSchemasSteps();

        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions_1 = getStepsAndActions();

        final WorkflowScheme workflow_1= new WorkflowDataGen().name("Testing")
                .stepAndAction(workflowStepsAndActions_1).nextPersistedWithStepsAndActions();

        final long secondCount = FactoryLocator.getWorkFlowFactory().countAllSchemasSteps();
        assertEquals(firstCount + 5, secondCount);

        APILocator.getWorkflowAPI().archive(workflow_1, APILocator.systemUser());

        final long thirdCount = FactoryLocator.getWorkFlowFactory().countAllSchemasSteps();
        assertEquals(firstCount, thirdCount);
    }

    private static List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> getStepsAndActions() {
        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions = Arrays
                .asList(
                        Tuple.of("Editing",
                                Arrays.asList(
                                        Tuple.of("Save as Draft", "Current Step", EnumSet.of(EDITING, UNLOCKED, LOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                )
                        ),
                        Tuple.of("Review",
                                Arrays.asList(
                                        Tuple.of("Save as Draft", "Current Step", EnumSet.of(EDITING, LOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                )
                        ),
                        Tuple.of("Legal Approval",
                                Arrays.asList(
                                        Tuple.of("Save as Draft", "Current Step", EnumSet.of(EDITING, LOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                )
                        ),
                        Tuple.of("Published",
                                Arrays.asList(
                                        Tuple.of("Republish", "Published", EnumSet.of(EDITING, LOCKED, UNLOCKED, PUBLISHED, ARCHIVED))
                                ))
                        ,
                        Tuple.of("Archived",
                                Arrays.asList(
                                        Tuple.of("Full Delete", "Archived", EnumSet.of(EDITING, LISTING, LOCKED, UNLOCKED, ARCHIVED))
                                )

                        )
                );
        return workflowStepsAndActions;
    }
}
