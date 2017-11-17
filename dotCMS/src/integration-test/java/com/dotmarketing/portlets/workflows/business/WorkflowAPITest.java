package com.dotmarketing.portlets.workflows.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the workflowAPI
 */
public class WorkflowAPITest extends IntegrationTestBase {

    private static User user;
    private static Host defaultHost;
    protected static ContentTypeAPIImpl contentTypeAPI;
    protected static FieldAPI fieldAPI;
    protected static WorkflowAPI workflowAPI;
    protected static RoleAPI roleAPI;
    protected static PermissionAPI permissionAPI;
    protected static ContentletAPI contentletAPI;

    private static String contentTypeName;
    private static String workflowSchemeName1;
    private static String workflowSchemeName2;
    private static String workflowSchemeName3;

    private static ContentType contentType;
    private static Structure contentTypeStructure;

    /* Workflow Scheme 1 */
    private static WorkflowScheme workflowScheme1;

    private static WorkflowStep workflowScheme1Step1;
    private static String workflowScheme1Step1Name;
    private static WorkflowAction workflowScheme1Step1Action1;
    private static String workflowScheme1Step1Action1Name;

    private static WorkflowStep workflowScheme1Step2;
    private static String workflowScheme1Step2Name;
    private static WorkflowAction workflowScheme1Step2Action1;
    private static String workflowScheme1Step2Action1Name;

    /* Workflow Scheme 2 */
    private static WorkflowScheme workflowScheme2;

    private static WorkflowStep workflowScheme2Step1;
    private static String workflowScheme2Step1Name;
    private static WorkflowAction workflowScheme2Step1Action1;
    private static String workflowScheme2Step1Action1Name;

    private static WorkflowStep workflowScheme2Step2;
    private static String workflowScheme2Step2Name;
    private static WorkflowAction workflowScheme2Step2Action1;
    private static String workflowScheme2Step2Action1Name;

    /* Workflow Scheme 3 */
    private static WorkflowScheme workflowScheme3;

    private static WorkflowStep workflowScheme3Step1;

    private static String workflowScheme3Step1Name;

    private static WorkflowAction workflowScheme3Step1Action1;
    private static String workflowScheme3Step1Action1Name;

    private static WorkflowStep workflowScheme3Step2;

    private static String workflowScheme3Step2Name;

    private static WorkflowAction workflowScheme3Step2Action1;
    private static String workflowScheme3Step2Action1Name;

    private static WorkflowAction workflowScheme3Step2Action2;
    private static String workflowScheme3Step2Action2Name;

    private static Role reviewer;
    private static Role contributor;
    private static Role publisher;

    private static final String FIELD_NAME ="Title";
    private static final String FIELD_VAR_NAME ="title";

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        HostAPI hostAPI = APILocator.getHostAPI();

        //Setting the test user
        user = APILocator.getUserAPI().getSystemUser();
        defaultHost = hostAPI.findDefaultHost(user, false);
        contentTypeAPI = (ContentTypeAPIImpl) APILocator.getContentTypeAPI(user);
        fieldAPI = APILocator.getContentTypeFieldAPI();
        workflowAPI = APILocator.getWorkflowAPI();
        roleAPI = APILocator.getRoleAPI();
        permissionAPI = APILocator.getPermissionAPI();
        contentletAPI = APILocator.getContentletAPI();

        publisher = roleAPI.findRoleByName("Publisher / Legal", null);
        reviewer = roleAPI.findRoleByName("Reviewer", publisher);
        contributor = roleAPI.findRoleByName("Contributor", reviewer);

        long time = System.currentTimeMillis();
        contentTypeName = "WorkflowTesting_" + time;

        /**
         * Generate ContentType
         */
        contentType = insertContentType(contentTypeName, BaseContentType.CONTENT);
        contentTypeStructure = new StructureTransformer(ContentType.class.cast(contentType))
                .asStructure();

        /**
         * Generate workflow schemes
         */

        /* Mandatory Workflow */
        workflowSchemeName1 = "WorkflowSchemeTest1" + time;
        workflowScheme1Step1Name = "WorkflowScheme1Step1_" + time;
        workflowScheme1Step1Action1Name = "WorkflowScheme1Step1Action1_" + time;
        workflowScheme1Step2Name = "WorkflowScheme1Step2_" + time;
        workflowScheme1Step2Action1Name = "WorkflowScheme1Step2Action1_" + time;
        workflowScheme1 = addWorkflowScheme(workflowSchemeName1, true);

        /* Generate scheme steps */
        workflowScheme1Step1 = addWorkflowStep(workflowScheme1Step1Name, 1, false, false,
                workflowScheme1.getId());

        workflowScheme1Step2 = addWorkflowStep(workflowScheme1Step2Name, 2, true, false,
                workflowScheme1.getId());

        /* Generate actions */
        workflowScheme1Step2Action1 = addWorkflowAction(workflowScheme1Step2Action1Name, 2,
                workflowScheme1Step2.getId(), true, workflowScheme1Step2.getId(), reviewer);

        workflowScheme1Step1Action1 = addWorkflowAction(workflowScheme1Step1Action1Name, 1,
                workflowScheme1Step2.getId(), true, workflowScheme1Step1.getId(), contributor);



        /* not Mandatory Workflows */
        workflowSchemeName2 = "WorkflowSchemeTest2_" + time;
        workflowScheme2Step1Name = "WorkflowScheme2Step1_" + time;
        workflowScheme2Step1Action1Name = "WorkflowScheme2Step1Action1_" + time;
        workflowScheme2Step2Name = "WorkflowScheme2Step2_" + time;
        workflowScheme2Step2Action1Name = "WorkflowScheme2Step2Action1_" + time;
        workflowScheme2 = addWorkflowScheme(workflowSchemeName2, false);

        /* Generate scheme steps */
        workflowScheme2Step1 = addWorkflowStep(workflowScheme2Step1Name, 1, false, false,
                workflowScheme2.getId());

        workflowScheme2Step2 = addWorkflowStep(workflowScheme2Step2Name, 2, false, false,
                workflowScheme2.getId());

        /* Generate actions */
        workflowScheme2Step2Action1 = addWorkflowAction(workflowScheme2Step2Action1Name, 2,
                workflowScheme2Step2.getId(), true, workflowScheme2Step2.getId(), reviewer);

        workflowScheme2Step1Action1 = addWorkflowAction(workflowScheme2Step1Action1Name, 1,
                workflowScheme2Step2.getId(), true, workflowScheme2Step1.getId(), contributor);



        /* not Mandatory Workflows */
        workflowSchemeName3 = "WorkflowSchemeTest3_" + time;
        workflowScheme3Step1Name = "WorkflowScheme3Step1_" + time;
        workflowScheme3Step1Action1Name = "WorkflowScheme3Step1Action1_" + time;
        workflowScheme3Step2Name = "WorkflowScheme3Step2_" + time;
        workflowScheme3Step2Action1Name = "WorkflowScheme2Step2Action1_" + time;
        workflowScheme3Step2Action2Name = "WorkflowScheme2Step2Action2_" + time;
        workflowScheme3 = addWorkflowScheme(workflowSchemeName3, false);

        /* Generate scheme steps */
        workflowScheme3Step1 = addWorkflowStep(workflowScheme3Step1Name, 1, false, false,
                workflowScheme3.getId());

        workflowScheme3Step2 = addWorkflowStep(workflowScheme3Step2Name, 2, false, false,
                workflowScheme3.getId());

        /* Generate actions */
        workflowScheme3Step2Action1 = addWorkflowAction(workflowScheme3Step2Action1Name, 2,
                workflowScheme3Step2.getId(), true, workflowScheme3Step2.getId(), reviewer);

        workflowScheme3Step2Action2 = addWorkflowAction(workflowScheme3Step2Action2Name, 3,
                workflowScheme3Step2.getId(), true, workflowScheme3Step2.getId(), publisher);

        workflowScheme3Step1Action1 = addWorkflowAction(workflowScheme3Step1Action1Name, 1,
                workflowScheme3Step2.getId(), true, workflowScheme3Step1.getId(), contributor);

    }

    /**
     * This method test the saveSchemeForStruct method
     */
    @Test
    public void saveSchemeForStruct() throws DotDataException, DotSecurityException {

        List<WorkflowScheme> worflowSchemes = new ArrayList<>();

        /* Validate that the workflow scheme was created*/
        workflowScheme1 = workflowAPI.findSchemeByName(workflowSchemeName1);
        assertTrue(null != workflowScheme1 && UtilMethods.isSet(workflowScheme1.getId()));

        worflowSchemes.add(workflowScheme1);

        /* Validate that the workflow scheme was created*/
        workflowScheme2 = workflowAPI.findSchemeByName(workflowSchemeName2);
        assertTrue(null != workflowScheme2 && UtilMethods.isSet(workflowScheme2.getId()));

        worflowSchemes.add(workflowScheme2);

        /* Validate that the workflow scheme was created */
        workflowScheme3 = workflowAPI.findSchemeByName(workflowSchemeName2);
        assertTrue(null != workflowScheme3 && UtilMethods.isSet(workflowScheme3.getId()));

        worflowSchemes.add(workflowScheme3);

        /* Associate the schemas to the content type */
        workflowAPI.saveSchemeForStruct(contentTypeStructure, worflowSchemes);

        List<WorkflowScheme> contentTypeSchemes = workflowAPI
                .findSchemeForStruct(contentTypeStructure);
        assertTrue(contentTypeSchemes != null && contentTypeSchemes.size() == 3);

        /* Validate that the default scheme is not associated to the content tyepe*/
        WorkflowScheme defaultScheme = workflowAPI.findDefaultScheme();
        assertFalse(containsScheme(defaultScheme, contentTypeSchemes));
        assertTrue(containsScheme(workflowScheme1, contentTypeSchemes));
        assertTrue(containsScheme(workflowScheme2, contentTypeSchemes));
        assertTrue(containsScheme(workflowScheme3, contentTypeSchemes));

        /* Associate the default schema to the content type */
        worflowSchemes.add(defaultScheme);
        workflowAPI.saveSchemeForStruct(contentTypeStructure, worflowSchemes);

        /* validate that the schemes area associated */
        contentTypeSchemes = workflowAPI.findSchemeForStruct(contentTypeStructure);
        assertTrue(contentTypeSchemes != null && contentTypeSchemes.size() == 4);
        assertTrue(containsScheme(defaultScheme, contentTypeSchemes));
        assertTrue(containsScheme(workflowScheme1, contentTypeSchemes));
        assertTrue(containsScheme(workflowScheme2, contentTypeSchemes));
        assertTrue(containsScheme(workflowScheme3, contentTypeSchemes));
    }

    /**
     * This method test the findStepsByContentlet method
     */
    @Test
    public void findStepsByContentlet() throws DotDataException, DotSecurityException {
        List<WorkflowScheme> worflowSchemes = new ArrayList<>();
        worflowSchemes.add(workflowScheme1);
        worflowSchemes.add(workflowScheme2);
        worflowSchemes.add(workflowScheme3);

        /* Associate the schemas to the content type */
        workflowAPI.saveSchemeForStruct(contentTypeStructure, worflowSchemes);

        long time = System.currentTimeMillis();
        Contentlet c1=new Contentlet();
        c1.setLanguageId(1);
        c1.setStringProperty(FIELD_VAR_NAME, "WorkflowContentTest1_"+time);
        c1.setContentTypeId(contentType.id());
        c1 = contentletAPI.checkin(c1, user, false);

        Contentlet c2=new Contentlet();
        c2.setLanguageId(1);
        c2.setStringProperty(FIELD_VAR_NAME, "WorkflowContentTest1_2"+time);
        c2.setContentTypeId(contentType.id());
        c2 = contentletAPI.checkin(c2, user, false);

        contentletAPI.isInodeIndexed(c1.getInode());
        contentletAPI.isInodeIndexed(c2.getInode());

        Contentlet c = APILocator.getContentletAPI().checkout(c2.getInode(), user, false);

        c.setStringProperty("wfActionId", workflowScheme2Step1Action1.getId());
        c.setStringProperty("wfActionComments", "Test"+time);

        c2 = APILocator.getContentletAPI().checkin(c, user, false);


        List<WorkflowStep> steps = workflowAPI.findStepsByContentlet(c1);
        assertTrue(steps.size() == 3);

        steps = workflowAPI.findStepsByContentlet(c2);
        assertTrue(steps.size() == 1);
        assertTrue(workflowScheme2Step2.getName().equals(steps.get(0).getName()));

    }

    /**
     * This method test the findActions methods
     */
    @Test
    public void findActions() throws DotDataException, DotSecurityException {

        List<WorkflowStep> steps = workflowAPI.findSteps(workflowScheme3);
        List<WorkflowAction> actions = workflowAPI.findActions(steps, user);
        assertTrue(null != actions && actions.size() == 3);

        User contributorUser = roleAPI.findUsersForRole(contributor).get(0);
        assertTrue(null != contributorUser && UtilMethods.isSet(contributorUser.getUserId()));

        actions = workflowAPI.findActions(steps, contributorUser);
        assertTrue(null != actions && actions.size() == 1);

        User reviewerUser = roleAPI.findUsersForRole(reviewer).get(0);
        assertTrue(null != contributorUser && UtilMethods.isSet(contributorUser.getUserId()));

        actions = workflowAPI.findActions(steps, reviewerUser);
        assertTrue(null != actions && actions.size() == 2);
    }

    /**
     * Validate if the scheme is present in the list of schemes
     *
     * @param scheme WorkflowScheme to check
     * @param schemes List of WorkflowSchemes to compare
     * @return true if exist, false if not
     */
    protected static boolean containsScheme(WorkflowScheme scheme, List<WorkflowScheme> schemes) {
        boolean containsScheme = false;
        for (WorkflowScheme compareScheme : schemes) {
            if (compareScheme.getId().equals(scheme.getId())) {
                containsScheme = true;
                break;
            }
        }
        return containsScheme;
    }

    /**
     * Generate the test content type
     */
    protected static ContentType insertContentType(String contentTypeName, BaseContentType baseType)
            throws DotDataException, DotSecurityException {

        ContentTypeBuilder builder = ContentTypeBuilder.builder(baseType.immutableClass())
                .description("description")
                .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                .name(contentTypeName).owner("owner")
                .variable("velocityVarName" + contentTypeName);

        ContentType type = builder.build();
        type = contentTypeAPI.save(type);

        Field savedField = ImmutableTextField.builder()
                .name(FIELD_NAME)
                .variable(FIELD_VAR_NAME)
                .required(true)
                .listed(true)
                .indexed(true)
                .sortOrder(1)
                .contentTypeId(type.id())
                .fixed(true)
                .searchable(true)
                .values("")
                .build();

        fieldAPI.save(savedField, user);

        return type;
    }

    /**
     * Create a WorkflowScheme
     *
     * @param schemeName Name of the new Scheme
     * @return the new Scheme
     */
    protected static WorkflowScheme addWorkflowScheme(final String schemeName,
            final boolean isMandatory)
            throws DotDataException, DotSecurityException {
        WorkflowScheme scheme = null;
        try {
            scheme = new WorkflowScheme();
            scheme.setName(schemeName);
            scheme.setDescription("testing workflows " + schemeName);
            scheme.setCreationDate(new Date());
            scheme.setMandatory(isMandatory);
            workflowAPI.saveScheme(scheme);
        } catch (AlreadyExistException e) {
            //scheme already exist
        }
        return scheme;

    }

    /**
     * Create a new Workflow Step
     *
     * @param name Name of the step
     * @param order step order
     * @param resolved Is resolved
     * @param enableEscalation Allows Escalations
     * @param schemeId Scheme Id
     * @return The created step
     */
    protected static WorkflowStep addWorkflowStep(final String name, final int order,
            final boolean resolved,
            final boolean enableEscalation, final String schemeId)
            throws DotDataException, DotSecurityException {
        WorkflowStep step = null;
        try {
            step = new WorkflowStep();
            step.setCreationDate(new Date());
            step.setEnableEscalation(enableEscalation);
            step.setMyOrder(order);
            step.setName(name);
            step.setResolved(resolved);
            step.setSchemeId(schemeId);
            workflowAPI.saveStep(step);
        } catch (AlreadyExistException e) {
            //scheme already exist
        }
        return step;
    }

    /**
     * Add a workflowAction
     *
     * @param name Name of the action
     * @param order Order
     * @param nextStep next step
     * @param requiresCheckout is checkout required
     * @param stepId Current step id
     * @param whoCanUse Role permissions
     * @return A workflowAction
     */
    protected static WorkflowAction addWorkflowAction(final String name, final int order,
            final String nextStep,
            final boolean requiresCheckout, final String stepId, final Role whoCanUse)
            throws DotDataException, DotSecurityException {

        WorkflowAction action = null;
        try {
            action = new WorkflowAction();
            action.setName(name);
            action.setOwner(whoCanUse.getId());
            action.setOrder(order);
            action.setNextStep(nextStep);
            action.setRequiresCheckout(requiresCheckout);
            action.setStepId(stepId);
            action.setNextAssign(whoCanUse.getId());
            action.setCommentable(true);
            action.setAssignable(false);
            workflowAPI.saveAction(action,
                    Arrays.asList(new Permission[]{
                            new Permission(action.getId(),
                                    whoCanUse.getId(),
                                    PermissionAPI.PERMISSION_USE)}));
        } catch (AlreadyExistException e) {
            //scheme already exist
        }
        return action;
    }

    /**
     * Add a subaction
     *
     * @param name name of the subaction
     * @param actionId Action id where the subaction should be associated
     * @param actionClassToUse The subaction classs to use
     * @param order Order of executions
     */
    protected static WorkflowActionClass addSubActionClass(final String name, final String actionId,
            final Class actionClassToUse, final int order)
            throws DotDataException, DotSecurityException {
        WorkflowActionClass actionClass = null;
        try {
            actionClass = new WorkflowActionClass();
            actionClass.setActionId(actionId);
            actionClass.setClazz(actionClassToUse.getCanonicalName());
            actionClass.setName(name);
            actionClass.setOrder(order);
            workflowAPI.saveActionClass(actionClass);
        } catch (AlreadyExistException e) {
            //scheme already exist
        }
        return actionClass;
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup() throws DotDataException, DotSecurityException {

        contentTypeAPI.delete(contentType);
        try {
            workflowAPI.deleteAction(workflowScheme1Step1Action1);
            workflowAPI.deleteAction(workflowScheme1Step2Action1);

            workflowAPI.deleteStep(workflowScheme1Step1);
            workflowAPI.deleteStep(workflowScheme1Step2);

            workflowScheme1.setArchived(true);
            workflowAPI.saveScheme(workflowScheme1);
            workflowAPI.deleteScheme(workflowScheme1);


            workflowAPI.deleteAction(workflowScheme2Step1Action1);
            workflowAPI.deleteAction(workflowScheme2Step2Action1);
            workflowAPI.deleteStep(workflowScheme2Step1);
            workflowAPI.deleteStep(workflowScheme2Step2);

            workflowScheme2.setArchived(true);
            workflowAPI.saveScheme(workflowScheme2);
            workflowAPI.deleteScheme(workflowScheme2);

            workflowAPI.deleteAction(workflowScheme3Step1Action1);
            workflowAPI.deleteAction(workflowScheme3Step2Action1);
            workflowAPI.deleteAction(workflowScheme3Step2Action2);

            workflowAPI.deleteStep(workflowScheme3Step1);
            workflowAPI.deleteStep(workflowScheme3Step2);

            workflowScheme3.setArchived(true);
            workflowAPI.saveScheme(workflowScheme3);
            workflowAPI.deleteScheme(workflowScheme3);

        }catch (AlreadyExistException e){

        }


    }

}
