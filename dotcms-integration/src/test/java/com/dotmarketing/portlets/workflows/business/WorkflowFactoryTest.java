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
import com.dotcms.datagen.WorkflowActionClassDataGen;
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

import com.dotmarketing.portlets.workflows.actionlet.*;
import com.dotmarketing.portlets.workflows.model.*;

import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
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

    /**
     * Method to test: {@link WorkflowFactoryImpl#countAllSchemasSteps()}
     * When: create a new Workflow with 4 actions
     * Should: the count must be 4 more than before
     *
     * @throws DotDataException
     */
    @Test
    public void countAllWorkflowActions() throws DotDataException {

        final long firstCount = FactoryLocator.getWorkFlowFactory().countAllSchemasActions();

        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions = Arrays
                .asList(
                        Tuple.of("Editing",
                                Arrays.asList(
                                        // First component of the Tuple is the desired Action-Name.
                                        // The Second Component is The Next-Step we desire to be pointed to by the current action.
                                        // Third is the show-When definition.
                                        Tuple.of("Save as Draft", "Current Step", EnumSet.of(EDITING, UNLOCKED, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of("Send for Review", "Review",  EnumSet.of(EDITING, UNLOCKED, NEW, UNPUBLISHED))
                                )
                        ),
                        Tuple.of("Review",
                                Arrays.asList(
                                        Tuple.of("Save as Draft", "Current Step", EnumSet.of(EDITING, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of("Return for Edits", "Editing", EnumSet.of(LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                )
                        )
                );

        final WorkflowScheme workflow_1= new WorkflowDataGen()
                .stepAndAction(workflowStepsAndActions).nextPersistedWithStepsAndActions();

        final long secondCount = FactoryLocator.getWorkFlowFactory().countAllSchemasActions();
        assertEquals(firstCount + 3, secondCount);

        final WorkflowScheme workflow_2 = new WorkflowDataGen()
                .stepAndAction(workflowStepsAndActions).nextPersistedWithStepsAndActions();

        final long thirdCount = FactoryLocator.getWorkFlowFactory().countAllSchemasActions();
        assertEquals(secondCount + 3, thirdCount);
    }


    /**
     * Method to test: {@link WorkflowFactoryImpl#countAllSchemasSteps()}
     * When: create a new Workflow with 7 Sub actions
     * Should: the count must be 7 more than before
     *
     * @throws DotDataException
     */
    @Test
    public void countAllWorkflowSubActions() throws DotDataException, DotSecurityException {

        final String workflow_step_1 = "countAllWorkflowSubActions_step_1_" + System.currentTimeMillis();
        final String workflow_step_2 = "countAllWorkflowSubActions_step_2_" + System.currentTimeMillis();
        final String workflow_step_3 = "countAllWorkflowSubActions_step_3_" + System.currentTimeMillis();
        final String workflow_step_4 = "countAllWorkflowSubActions_step_4_" + System.currentTimeMillis();

        final String workflow_action_1 = "countAllWorkflowSubActions_action_1_" + System.currentTimeMillis();
        final String workflow_action_2 = "countAllWorkflowSubActions_action_2_" + System.currentTimeMillis();
        final String workflow_action_3 = "countAllWorkflowSubActions_action_3_" + System.currentTimeMillis();
        final String workflow_action_4 = "countAllWorkflowSubActions_action_4_" + System.currentTimeMillis();
        final String workflow_action_5 = "countAllWorkflowSubActions_action_5_" + System.currentTimeMillis();
        final String workflow_action_6 = "countAllWorkflowSubActions_action_6_" + System.currentTimeMillis();
        final String workflow_action_7 = "countAllWorkflowSubActions_action_7_" + System.currentTimeMillis();
        final String workflow_action_8 = "countAllWorkflowSubActions_action_8_" + System.currentTimeMillis();

        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions_1 = Arrays
                .asList(
                        Tuple.of(workflow_step_1,
                                Arrays.asList(
                                        // First component of the Tuple is the desired Action-Name.
                                        // The Second Component is The Next-Step we desire to be pointed to by the current action.
                                        // Third is the show-When definition.
                                        Tuple.of(workflow_action_1, "Current Step", EnumSet.of(EDITING, UNLOCKED, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of(workflow_action_2, workflow_step_2,  EnumSet.of(EDITING, UNLOCKED, NEW, UNPUBLISHED))
                                )
                        ),
                        Tuple.of(workflow_step_2,
                                Arrays.asList(
                                        Tuple.of(workflow_action_3, "Current Step", EnumSet.of(EDITING, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of(workflow_action_4, workflow_step_1, EnumSet.of(LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                )
                        )
                );

        final long firstCount = FactoryLocator.getWorkFlowFactory().countAllSchemasSubActions();

        final WorkflowScheme workflow_1 = new WorkflowDataGen()
                .stepAndAction(workflowStepsAndActions_1).nextPersistedWithStepsAndActions();

        final List<WorkflowAction> actions_1 = APILocator.getWorkflowAPI().findActions(workflow_1, APILocator.systemUser());
        new WorkflowActionClassDataGen(actions_1.get(0).getId()).nextPersisted();
        new WorkflowActionClassDataGen(actions_1.get(0).getId()).nextPersisted();
        new WorkflowActionClassDataGen(actions_1.get(1).getId()).nextPersisted();

        final long secondCount = FactoryLocator.getWorkFlowFactory().countAllSchemasSubActions();
        assertEquals(firstCount + 7, secondCount);

        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions_2 = Arrays
                .asList(
                        Tuple.of(workflow_step_3,
                                Arrays.asList(
                                        // First component of the Tuple is the desired Action-Name.
                                        // The Second Component is The Next-Step we desire to be pointed to by the current action.
                                        // Third is the show-When definition.
                                        Tuple.of(workflow_action_5, "Current Step", EnumSet.of(EDITING, UNLOCKED, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of(workflow_action_6, workflow_step_4,  EnumSet.of(EDITING, UNLOCKED, NEW, UNPUBLISHED))
                                )
                        ),
                        Tuple.of(workflow_step_4,
                                Arrays.asList(
                                        Tuple.of(workflow_action_7, "Current Step", EnumSet.of(EDITING, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of(workflow_action_8, workflow_step_3, EnumSet.of(LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                )
                        )
                );

        final WorkflowScheme workflow_2 = new WorkflowDataGen()
                .stepAndAction(workflowStepsAndActions_2).nextPersistedWithStepsAndActions();

        final List<WorkflowAction> actions_2 = APILocator.getWorkflowAPI().findActions(workflow_2, APILocator.systemUser());
        new WorkflowActionClassDataGen(actions_2.get(2).getId()).nextPersisted();
        new WorkflowActionClassDataGen(actions_2.get(2).getId()).nextPersisted();
        new WorkflowActionClassDataGen(actions_2.get(2).getId()).nextPersisted();

        final long thirdCount = FactoryLocator.getWorkFlowFactory().countAllSchemasSubActions();
        assertEquals(secondCount + 7, thirdCount);
    }

    /**
     * Method to test: {@link WorkflowFactoryImpl#countAllSchemasSubActions()} ()}
     * When: create a new Workflow with 5 SubActions, and later archive it
     * Should: not take account the archived Schema
     *
     * @throws DotDataException
     */
    @Test
    public void notCountSubActionsFromArchivedSchema() throws DotDataException, DotSecurityException, AlreadyExistException {
        final String workflow_step_1 = "countAllWorkflowSubActions_step_1_" + System.currentTimeMillis();
        final String workflow_step_2 = "countAllWorkflowSubActions_step_2_" + System.currentTimeMillis();

        final String workflow_action_1 = "countAllWorkflowSubActions_action_1_" + System.currentTimeMillis();
        final String workflow_action_2 = "countAllWorkflowSubActions_action_2_" + System.currentTimeMillis();
        final String workflow_action_3 = "countAllWorkflowSubActions_action_3_" + System.currentTimeMillis();
        final String workflow_action_4 = "countAllWorkflowSubActions_action_4_" + System.currentTimeMillis();


        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions_1 = Arrays
                .asList(
                        Tuple.of(workflow_step_1,
                                Arrays.asList(
                                        // First component of the Tuple is the desired Action-Name.
                                        // The Second Component is The Next-Step we desire to be pointed to by the current action.
                                        // Third is the show-When definition.
                                        Tuple.of(workflow_action_1, "Current Step", EnumSet.of(EDITING, UNLOCKED, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of(workflow_action_2, workflow_step_2,  EnumSet.of(EDITING, UNLOCKED, NEW, UNPUBLISHED))
                                )
                        ),
                        Tuple.of(workflow_step_2,
                                Arrays.asList(
                                        Tuple.of(workflow_action_3, "Current Step", EnumSet.of(EDITING, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of(workflow_action_4, workflow_step_1, EnumSet.of(LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                )
                        )
                );

        final long firstCount = FactoryLocator.getWorkFlowFactory().countAllSchemasSubActions();

        final WorkflowScheme workflow_1 = new WorkflowDataGen()
                .stepAndAction(workflowStepsAndActions_1).nextPersistedWithStepsAndActions();

        final List<WorkflowAction> actions_1 = APILocator.getWorkflowAPI().findActions(workflow_1, APILocator.systemUser());
        new WorkflowActionClassDataGen(actions_1.get(0).getId()).nextPersisted();
        new WorkflowActionClassDataGen(actions_1.get(0).getId()).nextPersisted();
        new WorkflowActionClassDataGen(actions_1.get(1).getId()).nextPersisted();

        final long secondCount = FactoryLocator.getWorkFlowFactory().countAllSchemasSubActions();
        assertEquals(firstCount + 7, secondCount);

        APILocator.getWorkflowAPI().archive(workflow_1, APILocator.systemUser());

        final long thirdCount = FactoryLocator.getWorkFlowFactory().countAllSchemasSubActions();
        assertEquals(firstCount, thirdCount);
    }

    /**
     * Method to test: {@link WorkflowFactoryImpl#countAllSchemasSteps()}
     * When: create a new Workflow with 5 Action, and later archive it
     * Should: not take account the archived Schema
     *
     * @throws DotDataException
     */
    @Test
    public void notCountActionsFromArchivedSchema() throws DotDataException, DotSecurityException, AlreadyExistException {
        final long firstCount = FactoryLocator.getWorkFlowFactory().countAllSchemasActions();

        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions = Arrays
                .asList(
                        Tuple.of("Editing",
                                Arrays.asList(
                                        // First component of the Tuple is the desired Action-Name.
                                        // The Second Component is The Next-Step we desire to be pointed to by the current action.
                                        // Third is the show-When definition.
                                        Tuple.of("Save as Draft", "Current Step", EnumSet.of(EDITING, UNLOCKED, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of("Send for Review", "Review",  EnumSet.of(EDITING, UNLOCKED, NEW, UNPUBLISHED))
                                )
                        ),
                        Tuple.of("Review",
                                Arrays.asList(
                                        Tuple.of("Save as Draft", "Current Step", EnumSet.of(EDITING, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of("Return for Edits", "Editing", EnumSet.of(LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                )
                        )
                );

        final WorkflowScheme workflow_1= new WorkflowDataGen()
                .stepAndAction(workflowStepsAndActions).nextPersistedWithStepsAndActions();

        final long secondCount = FactoryLocator.getWorkFlowFactory().countAllSchemasActions();
        assertEquals(firstCount + 3, secondCount);

        APILocator.getWorkflowAPI().archive(workflow_1, APILocator.systemUser());

        final long thirdCount = FactoryLocator.getWorkFlowFactory().countAllSchemasActions();
        assertEquals(firstCount, thirdCount);
    }

    /**
     * Method to test: {@link WorkflowFactoryImpl#countAllSchemasSteps()}
     * When: create a new Workflow with 2 Unique Sub actions and then another Workflow with just one more unique SubAction
     * Should: the second time count must be 2 more than first one
     * and the third one should be 1 more than second one
     *
     * @throws DotDataException
     */
    @Test
    public void countAllWorkflowUniqueSubActions() throws DotDataException, DotSecurityException {

        final String workflow_step_1 = "countAllWorkflowUniqueSubActions_step_1_" + System.currentTimeMillis();
        final String workflow_step_2 = "countAllWorkflowUniqueSubActions_step_2_" + System.currentTimeMillis();
        final String workflow_step_3 = "countAllWorkflowUniqueSubActions_step_3_" + System.currentTimeMillis();
        final String workflow_step_4 = "countAllWorkflowUniqueSubActions_step_4_" + System.currentTimeMillis();

        final String workflow_action_1 = "countAllWorkflowUniqueSubActions_action_1_" + System.currentTimeMillis();
        final String workflow_action_2 = "countAllWorkflowUniqueSubActions_action_2_" + System.currentTimeMillis();
        final String workflow_action_3 = "countAllWorkflowUniqueSubActions_action_3_" + System.currentTimeMillis();
        final String workflow_action_4 = "countAllWorkflowUniqueSubActions_action_4_" + System.currentTimeMillis();
        final String workflow_action_5 = "countAllWorkflowUniqueSubActions_action_5_" + System.currentTimeMillis();
        final String workflow_action_6 = "countAllWorkflowUniqueSubActions_action_6_" + System.currentTimeMillis();
        final String workflow_action_7 = "countAllWorkflowUniqueSubActions_action_7_" + System.currentTimeMillis();
        final String workflow_action_8 = "countAllWorkflowUniqueSubActions_action_8_" + System.currentTimeMillis();

        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions_1 = Arrays
                .asList(
                        Tuple.of(workflow_step_1,
                                Arrays.asList(
                                        // First component of the Tuple is the desired Action-Name.
                                        // The Second Component is The Next-Step we desire to be pointed to by the current action.
                                        // Third is the show-When definition.
                                        Tuple.of(workflow_action_1, "Current Step", EnumSet.of(EDITING, UNLOCKED, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of(workflow_action_2, workflow_step_2,  EnumSet.of(EDITING, UNLOCKED, NEW, UNPUBLISHED))
                                )
                        ),
                        Tuple.of(workflow_step_2,
                                Arrays.asList(
                                        Tuple.of(workflow_action_3, "Current Step", EnumSet.of(EDITING, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of(workflow_action_4, workflow_step_1, EnumSet.of(LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                )
                        )
                );

        final long firstCount = FactoryLocator.getWorkFlowFactory().countAllSchemasUniqueSubActions();

        final WorkflowScheme workflow_1 = new WorkflowDataGen()
                .stepAndAction(workflowStepsAndActions_1).nextPersistedWithStepsAndActions();

        final List<WorkflowAction> actions_1 = APILocator.getWorkflowAPI().findActions(workflow_1, APILocator.systemUser());


        new WorkflowActionClassDataGen(actions_1.get(0).getId()).actionClass(Actionlet_1.class).nextPersisted();
        new WorkflowActionClassDataGen(actions_1.get(0).getId()).actionClass(Actionlet_2.class).nextPersisted();
        new WorkflowActionClassDataGen(actions_1.get(1).getId()).actionClass(Actionlet_1.class).nextPersisted();

        final long secondCount = FactoryLocator.getWorkFlowFactory().countAllSchemasUniqueSubActions();
        assertEquals(firstCount + 2, secondCount);

        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions_2 = Arrays
                .asList(
                        Tuple.of(workflow_step_3,
                                Arrays.asList(
                                        // First component of the Tuple is the desired Action-Name.
                                        // The Second Component is The Next-Step we desire to be pointed to by the current action.
                                        // Third is the show-When definition.
                                        Tuple.of(workflow_action_5, "Current Step", EnumSet.of(EDITING, UNLOCKED, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of(workflow_action_6, workflow_step_4,  EnumSet.of(EDITING, UNLOCKED, NEW, UNPUBLISHED))
                                )
                        ),
                        Tuple.of(workflow_step_4,
                                Arrays.asList(
                                        Tuple.of(workflow_action_7, "Current Step", EnumSet.of(EDITING, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of(workflow_action_8, workflow_step_3, EnumSet.of(LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                )
                        )
                );

        final WorkflowScheme workflow_2 = new WorkflowDataGen()
                .stepAndAction(workflowStepsAndActions_2).nextPersistedWithStepsAndActions();

        final List<WorkflowAction> actions_2 = APILocator.getWorkflowAPI().findActions(workflow_2, APILocator.systemUser());
        new WorkflowActionClassDataGen(actions_2.get(2).getId()).actionClass(Actionlet_1.class).nextPersisted();
        new WorkflowActionClassDataGen(actions_2.get(2).getId()).actionClass(Actionlet_2.class).nextPersisted();
        new WorkflowActionClassDataGen(actions_2.get(2).getId()).actionClass(Actionlet_3.class).nextPersisted();

        final long thirdCount = FactoryLocator.getWorkFlowFactory().countAllSchemasUniqueSubActions();
        assertEquals(secondCount + 1, thirdCount);
    }

    /**
     * Method to test: {@link WorkflowFactoryImpl#countAllSchemasSubActions()} ()}
     * When: create a new Workflow with 1 Unique SubActions, and later archive it
     * Should: not take account the archived Schema
     *
     * @throws DotDataException
     */
    @Test
    public void notCountUniqueSubActionsFromArchivedSchema() throws DotDataException, DotSecurityException, AlreadyExistException {
        final String workflow_step_1 = "countAllWorkflowUniqueSubActions_step_1_" + System.currentTimeMillis();
        final String workflow_step_2 = "countAllWorkflowUniqueSubActions_step_2_" + System.currentTimeMillis();

        final String workflow_action_1 = "countAllWorkflowUniqueSubActions_action_1_" + System.currentTimeMillis();
        final String workflow_action_2 = "countAllWorkflowUniqueSubActions_action_2_" + System.currentTimeMillis();
        final String workflow_action_3 = "countAllWorkflowUniqueSubActions_action_3_" + System.currentTimeMillis();
        final String workflow_action_4 = "countAllWorkflowUniqueSubActions_action_4_" + System.currentTimeMillis();

        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions_1 = Arrays
                .asList(
                        Tuple.of(workflow_step_1,
                                Arrays.asList(
                                        // First component of the Tuple is the desired Action-Name.
                                        // The Second Component is The Next-Step we desire to be pointed to by the current action.
                                        // Third is the show-When definition.
                                        Tuple.of(workflow_action_1, "Current Step", EnumSet.of(EDITING, UNLOCKED, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of(workflow_action_2, workflow_step_2,  EnumSet.of(EDITING, UNLOCKED, NEW, UNPUBLISHED))
                                )
                        ),
                        Tuple.of(workflow_step_2,
                                Arrays.asList(
                                        Tuple.of(workflow_action_3, "Current Step", EnumSet.of(EDITING, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of(workflow_action_4, workflow_step_1, EnumSet.of(LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                )
                        )
                );

        final long firstCount = FactoryLocator.getWorkFlowFactory().countAllSchemasUniqueSubActions();

        final WorkflowScheme workflow_1 = new WorkflowDataGen()
                .stepAndAction(workflowStepsAndActions_1).nextPersistedWithStepsAndActions();

        final List<WorkflowAction> actions_1 = APILocator.getWorkflowAPI().findActions(workflow_1, APILocator.systemUser());

        new WorkflowActionClassDataGen(actions_1.get(0).getId()).actionClass(Actionlet_4.class).nextPersisted();

        final long secondCount = FactoryLocator.getWorkFlowFactory().countAllSchemasUniqueSubActions();
        assertEquals(firstCount + 1, secondCount);

        APILocator.getWorkflowAPI().archive(workflow_1, APILocator.systemUser());

        final long thirdCount = FactoryLocator.getWorkFlowFactory().countAllSchemasUniqueSubActions();
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
