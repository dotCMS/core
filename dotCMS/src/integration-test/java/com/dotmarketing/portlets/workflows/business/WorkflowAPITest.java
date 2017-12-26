package com.dotmarketing.portlets.workflows.business;

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
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
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

import static org.junit.Assert.*;

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
    private static String contentTypeName2;
    private static String workflowSchemeName1;
    private static String workflowSchemeName2;
    private static String workflowSchemeName3;
    private static String workflowSchemeName4;

    private static ContentType contentType;
    private static Structure contentTypeStructure;

    private static ContentType contentType2;
    private static Structure contentTypeStructure2;

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

    /* Workflow Scheme 4 */
    private static WorkflowScheme workflowScheme4;

    private static WorkflowStep workflowScheme4Step1;
    private static String workflowScheme4Step1Name;
    private static WorkflowAction workflowScheme4Step1ActionView;
    private static String workflowScheme4Step1ActionViewName;
    private static WorkflowAction workflowScheme4Step1ActionEdit;
    private static String workflowScheme4Step1ActionEditName;
    private static WorkflowAction workflowScheme4Step1ActionPublish;
    private static String workflowScheme4Step1ActionPublishName;
    private static WorkflowAction workflowScheme4Step1ActionEditPermissions;
    private static String workflowScheme4Step1ActionEditPermissionsName;
    private static WorkflowAction workflowScheme4Step1ActionContributor;
    private static String workflowScheme4Step1ActionContributorName;

    private static WorkflowStep workflowScheme4Step2;
    private static String workflowScheme4Step2Name;
    private static WorkflowAction workflowScheme4Step2ActionView;
    private static String workflowScheme4Step2ActionViewName;
    private static WorkflowAction workflowScheme4Step2ActionEdit;
    private static String workflowScheme4Step2ActionEditName;
    private static WorkflowAction workflowScheme4Step2ActionPublish;
    private static String workflowScheme4Step2ActionPublishName;
    private static WorkflowAction workflowScheme4Step2ActionEditPermissions;
    private static String workflowScheme4Step2ActionEditPermissionsName;
    private static WorkflowAction workflowScheme4Step2ActionReviewer;
    private static String workflowScheme4Step2ActionReviewerName;

    private static WorkflowStep workflowScheme4Step3;
    private static String workflowScheme4Step3Name;
    private static WorkflowAction workflowScheme4Step3ActionView;
    private static String workflowScheme4Step3ActionViewName;
    private static WorkflowAction workflowScheme4Step3ActionEdit;
    private static String workflowScheme4Step3ActionEditName;
    private static WorkflowAction workflowScheme4Step3ActionPublish;
    private static String workflowScheme4Step3ActionPublishName;
    private static WorkflowAction workflowScheme4Step3ActionEditPermissions;
    private static String workflowScheme4Step3ActionEditPermissionsName;
    private static WorkflowAction workflowScheme4Step3ActionPublisher;
    private static String workflowScheme4Step3ActionPublisherName;

    /* Roles */
    private static Role reviewer;
    private static Role contributor;
    private static Role publisher;
    private static Role intranet;
    private static Role anyWhoView;
    private static Role anyWhoEdit;
    private static Role anyWhoPublish;
    private static Role anyWhoEditPermissions;

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
        intranet = roleAPI.findRoleByName("Intranet", null);
        anyWhoView = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_VIEW_ROLE_KEY);
        anyWhoEdit = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_ROLE_KEY);
        anyWhoPublish = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_PUBLISH_ROLE_KEY);
        anyWhoEditPermissions = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_PERMISSIONS_ROLE_KEY);

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
                workflowScheme1Step2.getId(), true, workflowScheme1Step2.getId(), reviewer, workflowScheme1.getId());

        workflowScheme1Step1Action1 = addWorkflowAction(workflowScheme1Step1Action1Name, 1,
                workflowScheme1Step2.getId(), true, workflowScheme1Step1.getId(), contributor, workflowScheme1.getId());



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
                workflowScheme2Step2.getId(), true, workflowScheme2Step2.getId(), reviewer,
                workflowScheme2.getId());

        workflowScheme2Step1Action1 = addWorkflowAction(workflowScheme2Step1Action1Name, 1,
                workflowScheme2Step2.getId(), true, workflowScheme2Step1.getId(), contributor,
                workflowScheme2.getId());



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
                workflowScheme3Step2.getId(), true, workflowScheme3Step2.getId(), reviewer,
                workflowScheme3.getId());

        workflowScheme3Step2Action2 = addWorkflowAction(workflowScheme3Step2Action2Name, 3,
                workflowScheme3Step2.getId(), true, workflowScheme3Step2.getId(), publisher,
                workflowScheme3.getId());

        workflowScheme3Step1Action1 = addWorkflowAction(workflowScheme3Step1Action1Name, 1,
                workflowScheme3Step2.getId(), true, workflowScheme3Step1.getId(), contributor,
                workflowScheme3.getId());

        //fourth Workflow Scheme
        contentTypeName2 = "WorkflowTesting2_" + time;
        /* Mandatory Workflow */
        workflowSchemeName4 = "WorkflowSchemeTest4" + time;
        workflowScheme4Step1Name = "WorkflowScheme4Step1_" + time;
        workflowScheme4Step1ActionViewName = "WorkflowScheme4Step1Action1_" + time;
        workflowScheme4Step1ActionEditName = "WorkflowScheme4Step1Action2_" + time;
        workflowScheme4Step1ActionPublishName = "WorkflowScheme4Step1Action3_" + time;
        workflowScheme4Step1ActionEditPermissionsName = "WorkflowScheme4Step1Action4_" + time;
        workflowScheme4Step1ActionContributorName = "WorkflowScheme4Step1Action5_" + time;

        workflowScheme4Step2Name = "WorkflowScheme4Step2_" + time;
        workflowScheme4Step2ActionViewName = "WorkflowScheme4Step2Action1_" + time;
        workflowScheme4Step2ActionEditName = "WorkflowScheme4Step2Action2_" + time;
        workflowScheme4Step2ActionPublishName = "WorkflowScheme4Step2Action3_" + time;
        workflowScheme4Step2ActionEditPermissionsName = "WorkflowScheme4Step2Action4_" + time;
        workflowScheme4Step2ActionReviewerName = "WorkflowScheme4Step2Action5_" + time;

        workflowScheme4Step3Name = "WorkflowScheme4Step3_" + time;
        workflowScheme4Step3ActionViewName = "WorkflowScheme4Step3Action1_" + time;
        workflowScheme4Step3ActionEditName = "WorkflowScheme4Step3Action2_" + time;
        workflowScheme4Step3ActionPublishName = "WorkflowScheme4Step3Action3_" + time;
        workflowScheme4Step3ActionEditPermissionsName = "WorkflowScheme4Step3Action4_" + time;
        workflowScheme4Step3ActionPublisherName = "WorkflowScheme4Step3Action5_" + time;

        /**
         * Generate ContentType
         */
        contentType2 = insertContentType(contentTypeName2, BaseContentType.CONTENT);
        contentTypeStructure2 = new StructureTransformer(ContentType.class.cast(contentType2))
                .asStructure();

        /**
         * Generate workflow schemes
         */
        workflowScheme4 = addWorkflowScheme(workflowSchemeName4, true);

        /* Generate scheme steps */
        workflowScheme4Step1 = addWorkflowStep(workflowScheme4Step1Name, 1, false, false,
                workflowScheme4.getId());

        workflowScheme4Step2 = addWorkflowStep(workflowScheme4Step2Name, 2, true, false,
                workflowScheme4.getId());

        workflowScheme4Step3 = addWorkflowStep(workflowScheme4Step3Name, 3, true, false,
                workflowScheme4.getId());

        /* Generate actions */
        workflowScheme4Step3ActionView = addWorkflowAction(workflowScheme4Step3ActionViewName, 1,
                workflowScheme4Step3.getId(), false, workflowScheme4Step3.getId(), anyWhoView,
                workflowScheme4.getId());
        workflowScheme4Step3ActionEdit = addWorkflowAction(workflowScheme4Step3ActionEditName, 2,
                workflowScheme4Step3.getId(), false, workflowScheme4Step3.getId(), anyWhoEdit,
                workflowScheme4.getId());
        workflowScheme4Step3ActionPublish = addWorkflowAction(workflowScheme4Step3ActionPublishName, 3,
                workflowScheme4Step3.getId(), false, workflowScheme4Step3.getId(), anyWhoPublish,
                workflowScheme4.getId());
        workflowScheme4Step3ActionEditPermissions = addWorkflowAction(workflowScheme4Step2ActionEditPermissionsName, 4,
                workflowScheme4Step3.getId(), false, workflowScheme4Step3.getId(),
                anyWhoEditPermissions, workflowScheme4.getId());
        workflowScheme4Step3ActionPublisher = addWorkflowAction(workflowScheme4Step2ActionReviewerName, 5,
                workflowScheme4Step3.getId(), false, workflowScheme4Step3.getId(), publisher,
                workflowScheme4.getId());

        workflowScheme4Step2ActionView = addWorkflowAction(workflowScheme4Step2ActionViewName, 1,
                workflowScheme4Step3.getId(), false, workflowScheme4Step2.getId(), anyWhoView,
                workflowScheme4.getId());
        workflowScheme4Step2ActionEdit = addWorkflowAction(workflowScheme4Step2ActionEditName, 2,
                workflowScheme4Step3.getId(), false, workflowScheme4Step2.getId(), anyWhoEdit,
                workflowScheme4.getId());
        workflowScheme4Step2ActionPublish = addWorkflowAction(workflowScheme4Step2ActionPublishName, 3,
                workflowScheme4Step3.getId(), false, workflowScheme4Step2.getId(), anyWhoPublish,
                workflowScheme4.getId());
        workflowScheme4Step2ActionEditPermissions = addWorkflowAction(workflowScheme4Step2ActionEditPermissionsName, 4,
                workflowScheme4Step3.getId(), false, workflowScheme4Step2.getId(),
                anyWhoEditPermissions, workflowScheme4.getId());
        workflowScheme4Step2ActionReviewer = addWorkflowAction(workflowScheme4Step2ActionReviewerName, 5,
                workflowScheme4Step3.getId(), false, workflowScheme4Step2.getId(), reviewer,
                workflowScheme4.getId());

        workflowScheme4Step1ActionView = addWorkflowAction(workflowScheme4Step1ActionViewName, 1,
                workflowScheme4Step2.getId(), false, workflowScheme4Step1.getId(), anyWhoView,
                workflowScheme4.getId());
        workflowScheme4Step1ActionEdit = addWorkflowAction(workflowScheme4Step1ActionEditName, 2,
                workflowScheme4Step2.getId(), false, workflowScheme4Step1.getId(), anyWhoEdit,
                workflowScheme4.getId());
        workflowScheme4Step1ActionPublish = addWorkflowAction(workflowScheme4Step1ActionPublishName, 3,
                workflowScheme4Step2.getId(), false, workflowScheme4Step1.getId(), anyWhoPublish,
                workflowScheme4.getId());
        workflowScheme4Step1ActionEditPermissions = addWorkflowAction(workflowScheme4Step1ActionEditPermissionsName, 4,
                workflowScheme4Step2.getId(), false, workflowScheme4Step1.getId(),
                anyWhoEditPermissions, workflowScheme4.getId());
        workflowScheme4Step1ActionContributor = addWorkflowAction(workflowScheme4Step1ActionContributorName, 5,
                workflowScheme4Step2.getId(), false, workflowScheme4Step1.getId(), contributor,
                workflowScheme4.getId());


    }

    /**
     * This method test the saveSchemesForStruct method
     */
    @Test
    public void saveSchemesForStruct() throws DotDataException, DotSecurityException {

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
        workflowScheme3 = workflowAPI.findSchemeByName(workflowSchemeName3);
        assertTrue(null != workflowScheme3 && UtilMethods.isSet(workflowScheme3.getId()));

        worflowSchemes.add(workflowScheme3);

        /* Associate the schemas to the content type */
        workflowAPI.saveSchemesForStruct(contentTypeStructure, worflowSchemes);

        List<WorkflowScheme> contentTypeSchemes = workflowAPI
                .findSchemesForStruct(contentTypeStructure);
        assertTrue(contentTypeSchemes != null && contentTypeSchemes.size() == 3);

        /* Validate that the default scheme is not associated to the content tyepe*/
        WorkflowScheme defaultScheme = workflowAPI.findDefaultScheme();
        assertFalse(containsScheme(defaultScheme, contentTypeSchemes));
        assertTrue(containsScheme(workflowScheme1, contentTypeSchemes));
        assertTrue(containsScheme(workflowScheme2, contentTypeSchemes));
        assertTrue(containsScheme(workflowScheme3, contentTypeSchemes));

        /* Associate the default schema to the content type */
        worflowSchemes.add(defaultScheme);
        workflowAPI.saveSchemesForStruct(contentTypeStructure, worflowSchemes);

        /* validate that the schemes area associated */
        contentTypeSchemes = workflowAPI.findSchemesForStruct(contentTypeStructure);
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
        Contentlet c1 = new Contentlet();
        Contentlet c2 = new Contentlet();
        try {
            List<WorkflowScheme> worflowSchemes = new ArrayList<>();
            worflowSchemes.add(workflowScheme1);
            worflowSchemes.add(workflowScheme2);
            worflowSchemes.add(workflowScheme3);

            /* Associate the schemas to the content type */
            workflowAPI.saveSchemesForStruct(contentTypeStructure, worflowSchemes);

            long time = System.currentTimeMillis();

            //create contentlets
            c1.setLanguageId(1);
            c1.setStringProperty(FIELD_VAR_NAME, "WorkflowContentTest1_" + time);
            c1.setContentTypeId(contentType.id());
            c1 = contentletAPI.checkin(c1, user, false);

            c2.setLanguageId(1);
            c2.setStringProperty(FIELD_VAR_NAME, "WorkflowContentTest2_" + time);
            c2.setContentTypeId(contentType.id());
            c2 = contentletAPI.checkin(c2, user, false);

            contentletAPI.isInodeIndexed(c1.getInode());
            contentletAPI.isInodeIndexed(c2.getInode());

            Contentlet c = APILocator.getContentletAPI().checkout(c2.getInode(), user, false);

            //set step action for content2
            c.setStringProperty("wfActionId", workflowScheme2Step1Action1.getId());
            c.setStringProperty("wfActionComments", "Test" + time);

            c2 = APILocator.getContentletAPI().checkin(c, user, false);

            //check steps available for content without step
            List<WorkflowStep> steps = workflowAPI.findStepsByContentlet(c1);
            assertTrue(steps.size() == 3);

            //get step for content with a selection action
            steps = workflowAPI.findStepsByContentlet(c2);
            assertTrue(steps.size() == 1);
            assertTrue(workflowScheme2Step2.getName().equals(steps.get(0).getName()));
        }finally {
            contentletAPI.archive(c1,user,false);
            contentletAPI.delete(c1,user,false);
            contentletAPI.archive(c2,user,false);
            contentletAPI.delete(c2,user,false);
        }

    }

    /**
     * This method test the findActions methods
     */
    @Test
    public void findActions() throws DotDataException, DotSecurityException {

        List<WorkflowStep> steps = workflowAPI.findSteps(workflowScheme3);
        assertNotNull(steps);
        assertEquals(2, steps.size());

        //check available actions for admin user
        List<WorkflowAction> actions = workflowAPI.findActions(steps, user);
        assertNotNull(actions);
        assertEquals(3, actions.size());

        //get a contributor users
        User contributorUser = roleAPI.findUsersForRole(contributor).get(0);
        assertTrue(null != contributorUser && UtilMethods.isSet(contributorUser.getUserId()));

        //Check valid action for restricted user
        actions = workflowAPI.findActions(steps, contributorUser);
        assertTrue(null != actions && actions.size() == 1);

        //Get a reviewer  user
        User reviewerUser = roleAPI.findUsersForRole(reviewer).get(0);
        assertTrue(null != contributorUser && UtilMethods.isSet(contributorUser.getUserId()));

        //check valid action for
        actions = workflowAPI.findActions(steps, reviewerUser);
        assertTrue(null != actions && actions.size() == 2);
    }

    /**
     * This method test the findTaskByContentlet method
     */
    @Test
    public void findTaskByContentlet() throws DotDataException, DotSecurityException {

        Contentlet c1 = new Contentlet();
        try {
            List<WorkflowScheme> worflowSchemes = new ArrayList<>();
            worflowSchemes.add(workflowScheme1);
            worflowSchemes.add(workflowScheme2);
            worflowSchemes.add(workflowScheme3);

            /* Associate the schemas to the content type */
            workflowAPI.saveSchemesForStruct(contentTypeStructure, worflowSchemes);

            long time = System.currentTimeMillis();

            //create contentlets
            c1.setLanguageId(1);
            c1.setStringProperty(FIELD_VAR_NAME, "WorkflowContentTest3_" + time);
            c1.setContentTypeId(contentType.id());
            c1 = contentletAPI.checkin(c1, user, false);

            contentletAPI.isInodeIndexed(c1.getInode());

            Contentlet c = contentletAPI.checkout(c1.getInode(), user, false);

            //set step action for content2
            c.setStringProperty("wfActionId", workflowScheme3Step1Action1.getId());
            c.setStringProperty("wfActionComments", "Test" + time);

            c1 = contentletAPI.checkin(c, user, false);

            //check steps available for content without step
            WorkflowTask task = workflowAPI.findTaskByContentlet(c1);
            assertNotNull(task);
            //task should be on the second step of the scheme 3
            assertTrue(workflowScheme3Step2.getId().equals(task.getStatus()));

        }finally {
            contentletAPI.archive(c1,user,false);
            contentletAPI.delete(c1,user,false);
        }
    }


    /**
     * Test the find findAvailableActions methods
     */
    @Test
    public void findAvailableActions() throws DotDataException, DotSecurityException {

        //Users
        final User joeContributor = APILocator.getUserAPI().loadUserById("dotcms.org.2789");
        final User janeReviewer = APILocator.getUserAPI().loadUserById("dotcms.org.2787");
        final User chrisPublisher = APILocator.getUserAPI().loadUserById("dotcms.org.2795");
        final User billIntranet = APILocator.getUserAPI().loadUserById("dotcms.org.2806");

        /*
        Need to do the test checking with different user the actions displayed. We need to specify
        the permission for Intranet, Reviewer, Contributor and Publisher to see if the action
        returned are the right ones
         */

        Contentlet testContentlet = new Contentlet();
        try {
            List<WorkflowScheme> worflowSchemes = new ArrayList<>();
            worflowSchemes.add(workflowScheme1);
            worflowSchemes.add(workflowScheme2);
            worflowSchemes.add(workflowScheme3);
            worflowSchemes.add(workflowScheme4);

            /* Associate the schemas to the content type */
            workflowAPI.saveSchemesForStruct(contentTypeStructure, worflowSchemes);

            long time = System.currentTimeMillis();

            //Create a test contentlet
            testContentlet.setLanguageId(1);
            testContentlet.setStringProperty(FIELD_VAR_NAME, "WorkflowContentTest_" + time);
            testContentlet.setContentTypeId(contentType.id());
            testContentlet = contentletAPI.checkin(testContentlet, user, false);

            contentletAPI.isInodeIndexed(testContentlet.getInode());

            //Adding permissions to the just created contentlet
            List<Permission> permissions = new ArrayList<>();
            Permission p1 = new Permission(
                    testContentlet.getPermissionId(),
                    APILocator.getRoleAPI().getUserRole(billIntranet).getId(),
                    (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT),
                    true);
            Permission p2 = new Permission(
                    testContentlet.getPermissionId(),
                    APILocator.getRoleAPI().getUserRole(janeReviewer).getId(),
                    (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                            | PermissionAPI.PERMISSION_WRITE),
                    true);
            Permission p3 = new Permission(
                    testContentlet.getPermissionId(),
                    APILocator.getRoleAPI().getUserRole(chrisPublisher).getId(),
                    (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                            | PermissionAPI.PERMISSION_WRITE
                            | PermissionAPI.PERMISSION_PUBLISH),
                    true);
            Permission p4 = new Permission(
                    testContentlet.getPermissionId(),
                    APILocator.getRoleAPI().getUserRole(joeContributor).getId(),
                    (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                            | PermissionAPI.PERMISSION_WRITE
                            | PermissionAPI.PERMISSION_PUBLISH
                            | PermissionAPI.PERMISSION_EDIT_PERMISSIONS),
                    true);
            permissions.add(p1);
            permissions.add(p2);
            permissions.add(p3);
            permissions.add(p4);

            APILocator.getPermissionAPI().save(permissions, testContentlet, user, false);

            //Validate the saved permissions
            List<Permission> foundPermissions = APILocator.getPermissionAPI()
                    .getPermissions(testContentlet);
            assertNotNull(foundPermissions);
            assertFalse(foundPermissions.isEmpty());

            /*
            Verify we are using/searching correctly with the special roles:
                - Any who can View
                - Any who can Edit
                - Any who can Publish
                - Any who can Edit Permission
             */
            List<WorkflowAction> foundActions = APILocator.getWorkflowAPI()
                    .findAvailableActions(testContentlet, billIntranet);
            assertNotNull(foundActions);
            assertFalse(foundActions.isEmpty());
            assertEquals(foundActions.size(), 3);

            foundActions = APILocator.getWorkflowAPI()
                    .findAvailableActions(testContentlet, janeReviewer);
            assertNotNull(foundActions);
            assertFalse(foundActions.isEmpty());
            assertEquals(foundActions.size(), 3);

            foundActions = APILocator.getWorkflowAPI()
                    .findAvailableActions(testContentlet, chrisPublisher);
            assertNotNull(foundActions);
            assertFalse(foundActions.isEmpty());
            assertEquals(foundActions.size(), 4);

            foundActions = APILocator.getWorkflowAPI()
                    .findAvailableActions(testContentlet, joeContributor);
            assertNotNull(foundActions);
            assertFalse(foundActions.isEmpty());
            assertEquals(foundActions.size(), 5);

        } finally {
            contentletAPI.archive(testContentlet, user, false);
            contentletAPI.delete(testContentlet, user, false);
        }

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
            final boolean requiresCheckout, final String stepId, final Role whoCanUse,
            final String schemeId)
            throws DotDataException, DotSecurityException {

        WorkflowAction action = null;
        try {
            action = new WorkflowAction();
            action.setName(name);
            action.setSchemeId(schemeId);
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

            workflowAPI.saveAction(action.getId(), stepId, APILocator.systemUser());
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
