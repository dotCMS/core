package com.dotmarketing.portlets.workflows.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.business.event.ContentletCheckinEvent;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.TestWorkflowUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.WorkflowActionClassDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.mock.request.MockInternalRequest;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageDeletedEvent;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.actionlet.ArchiveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CheckinContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.DeleteContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.MoveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.ResetTaskActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentAsDraftActionlet;
import com.dotmarketing.portlets.workflows.actionlet.UnarchiveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.UnpublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.portlets.workflows.model.WorkflowTimelineItem;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest.createContentTypeAndAssignPermissions;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.ARCHIVED;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.EDITING;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.LISTING;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.LOCKED;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.NEW;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.PUBLISHED;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.UNLOCKED;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.UNPUBLISHED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    protected static WorkflowCache workflowCache;

    private static String contentTypeName;
    private static String contentTypeName2;
    private static String contentTypeName3;
    private static String workflowSchemeName1;
    private static String workflowSchemeName2;
    private static String workflowSchemeName3;
    private static String workflowSchemeName4;
    private static String workflowSchemeName5;

    private static ContentType contentType;
    private static Structure contentTypeStructure;

    private static ContentType contentType2;

    private static ContentType contentType3;
    private static Structure contentTypeStructure3;

    private static ContentType contentType4;

    private static ContentType contentType5;

    /* Workflow Scheme 1 */
    private static WorkflowScheme workflowScheme1;

    private static WorkflowStep workflowScheme1Step1;
    private static String workflowScheme1Step1Name;
    private static WorkflowAction workflowScheme1Step1Action1;
    private static String workflowScheme1Step1ActionIntranetName;

    private static WorkflowStep workflowScheme1Step2;
    private static String workflowScheme1Step2Name;
    private static WorkflowAction workflowScheme1Step2Action1;
    private static String workflowScheme1Step2Action1Name;

    /* Workflow Scheme 2 */
    private static WorkflowScheme workflowScheme2;

    private static WorkflowStep workflowScheme2Step1;
    private static String workflowScheme2Step1Name;
    private static WorkflowAction workflowScheme2Step1Action1;
    private static String workflowScheme2Step1ActionReviewerName;

    private static WorkflowStep workflowScheme2Step2;
    private static String workflowScheme2Step2Name;
    private static WorkflowAction workflowScheme2Step2Action1;
    private static String workflowScheme2Step2Action1Name;

    /* Workflow Scheme 3 */
    private static WorkflowScheme workflowScheme3;

    private static WorkflowStep workflowScheme3Step1;

    private static String workflowScheme3Step1Name;

    private static WorkflowAction workflowScheme3Step1Action1;
    private static String workflowScheme3Step1ActionPublisherName;

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

    /* Workflow Scheme 5 */
    private static WorkflowScheme workflowScheme5;

    private static WorkflowStep workflowScheme5Step1;
    private static String workflowScheme5Step1Name;
    private static WorkflowAction workflowScheme5Step1Action1;
    private static String workflowScheme5Step1ActionPublishName;
    private static String workflowScheme5Step1Action1SubAction1Name;
    private static WorkflowActionClass workflowScheme5Step1Action1SubAction1;

    /*Workflow Scheme 6*/
    private static WorkflowScheme workflowScheme6;

    /*Workflow Scheme 7*/
    private static WorkflowScheme workflowScheme7;

    /* Roles */
    private static Role reviewer;
    private static Role contributor;
    private static Role publisher;
    private static Role intranet;
    private static Role anyWhoView;
    private static Role anyWhoEdit;
    private static Role anyWhoPublish;
    private static Role anyWhoEditPermissions;

    private static final String FIELD_NAME = "Title";
    private static final String FIELD_VAR_NAME = "title";

    private static final String DOCUMENT_MANAGEMENT_WORKFLOW_NAME = "Document Management";
    private static final String EDITING_STEP_NAME = "Editing";
    private static final String REVIEW_STEP_NAME = "Review";
    private static final String LEGAL_APPROVAL_STEP_NAME = "Legal Approval";
    private static final String PUBLISHED_STEP_NAME = "Published";
    private static final String ARCHIVED_STEP_NAME = "Archived";

    private static final String SAVE_AS_DRAFT_ACTION_NAME = "Save as Draft";
    private static final String SEND_FOR_REVIEW_ACTION_NAME = "Send for Review";
    private static final String RETURN_FOR_EDITS_ACTION_NAME = "Return for Edits";
    private static final String SEND_TO_LEGAL_ACTION_NAME = "Send to Legal";
    private static final String PUBLISH_ACTION_NAME = "Publish";
    private static final String REPUBLISH_ACTION_NAME = "Republish";
    private static final String UNPUBLISH_ACTION_NAME = "Unpublish";
    private static final String ARCHIVE_ACTION_NAME = "Archive";
    private static final String DELETE_ACTION_NAME = "Full Delete";
    private static final String RESET_WORKFLOW_ACTION_NAME = "Reset Workflow";

    private static final String SAVE_AS_DRAFT_SUBACTION = "Save Draft content";
    private static final String PUBLISH_SUBACTION = "Publish content";
    private static final String UNLOCK_SUBACTION = "Unlock content";
    private static final String SAVE_CONTENT_SUBACTION = "Save content";
    private static final String ARCHIVE_SUBACTION = "Archive content";
    private static final String UNARCHIVE_SUBACTION = "Unarchive content";
    private static final String UNPUBLISH_SUBACTION = "Unpublish content";
    private static final String DELETE_SUBACTION = "Unpublish content";
    private static final String RESET_WORKFLOW_SUBACTION = "Reset Workflow";

    private static final String DATE_FORMAT = "MMddyyyy_HHmmss";
    private static final String CONTENTLET_ON_WRONG_STEP_MESSAGE = "Contentlet is on the wrong Workflow Step";
    private static final String WRONG_ACTION_AVAILABLE_MESSAGE = "Wrong action available";
    private static final String INCORRECT_NUMBER_OF_ACTIONS_MESSAGE = "Incorrect number of actions available";

    private static final String ACTIONS_LIST_SHOULD_BE_EMPY = "Actions list should be empty";
    private static final String STEPS_LIST_SHOULD_BE_EMPTY = "Steps list should be empty";
    private static final String SCHEME_SHOULDNT_EXIST = "Scheme shouldn't exist";
    private static final String TASK_STATUS_SHOULD_NOT_BE_NULL = "Workflow Task status shouldn't be null";
    private static final String TASK_STATUS_SHOULD_BE_NULL = "Workflow Task status should be null";
    private static final String INCORRECT_TASK_STATUS = "The task status is incorrect";
    private static final String CONTENTLET_IS_NOT_ON_STEP = "The contentlet is not on a step";

    private static final int editPermission =
            PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_EDIT;
    private static final int publishPermission = editPermission + PermissionAPI.PERMISSION_PUBLISH;

    private static User joeContributor;
    private static User janeReviewer;
    private static User chrisPublisher;
    private static User billIntranet;

    private static final String WORKFLOW_SCHEME_CACHE_SHOULD_BE_NULL = "Workflow Scheme Cache should be null";
    private static final String WORKFLOW_SCHEME_CACHE_SHOULD_NOT_BE_NULL = "Workflow Scheme Cache should not be null";
    private static final String WORKFLOW_SCHEME_CACHE_WITH_WRONG_SIZE = "Workflow Scheme cache List size is incorrect";
    private static final String WORKFLOW_SCHEME_LIST_WITH_WRONG_SIZE = "Workflow Scheme List size is incorrect";
    private static final String WORKFLOW_STEPS_CACHE_SHOULD_BE_NULL = "Workflow Scheme Cache should be null";
    private static final String WORKFLOW_STEPS_CACHE_SHOULD_NOT_BE_NULL = "Workflow Scheme Cache should not be null";
    private static final String WORKFLOW_STEPS_CACHE_WITH_WRONG_SIZE = "Workflow Steps cache List size is incorrect";
    private static final String WORKFLOW_STEPS_LIST_WITH_WRONG_SIZE = "Workflow Steps List size is incorrect";

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
        workflowCache = CacheLocator.getWorkFlowCache();

        publisher = TestUserUtils.getOrCreatePublisherRole();
        reviewer =  TestUserUtils.getOrCreateReviewerRole();
        contributor = TestUserUtils.getOrCreateContributorRole();
        intranet = TestUserUtils.getOrCreateIntranetRole();

        final Map<String,Role> workflowRoles = TestUserUtils.getOrCreateWorkflowRoles();

        anyWhoView = workflowRoles.get(RoleAPI.WORKFLOW_ANY_WHO_CAN_VIEW_ROLE_KEY);
        anyWhoEdit = workflowRoles.get(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_ROLE_KEY);
        anyWhoPublish = workflowRoles.get(RoleAPI.WORKFLOW_ANY_WHO_CAN_PUBLISH_ROLE_KEY);
        anyWhoEditPermissions = workflowRoles.get(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_PERMISSIONS_ROLE_KEY);

        joeContributor = TestUserUtils.getJoeContributorUser();
        janeReviewer = TestUserUtils.getJaneReviewerUser();
        chrisPublisher = TestUserUtils.getChrisPublisherUser();
        billIntranet =  TestUserUtils.getBillIntranetUser();

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

        /*  Workflow 1 */
        workflowSchemeName1 = "WorkflowSchemeTest1" + time;
        workflowScheme1Step1Name = "WorkflowScheme1Step1_" + time;
        workflowScheme1Step1ActionIntranetName = "WorkflowScheme1Step1ActionIntranet_" + time;
        workflowScheme1Step2Name = "WorkflowScheme1Step2_" + time;
        workflowScheme1Step2Action1Name = "WorkflowScheme1Step2ActionReviewer_" + time;
        workflowScheme1 = addWorkflowScheme(workflowSchemeName1);

        /* Generate scheme steps */
        workflowScheme1Step1 = addWorkflowStep(workflowScheme1Step1Name, 1, false, false,
                workflowScheme1.getId());

        workflowScheme1Step2 = addWorkflowStep(workflowScheme1Step2Name, 2, true, false,
                workflowScheme1.getId());

        /* Generate actions */
        workflowScheme1Step2Action1 = addWorkflowAction(workflowScheme1Step2Action1Name, 2,
                workflowScheme1Step2.getId(), workflowScheme1Step2.getId(), reviewer,
                workflowScheme1.getId());

        workflowScheme1Step1Action1 = addWorkflowAction(workflowScheme1Step1ActionIntranetName, 1,
                workflowScheme1Step2.getId(), workflowScheme1Step1.getId(), intranet,
                workflowScheme1.getId());



        /* not Mandatory Workflows */
        workflowSchemeName2 = "WorkflowSchemeTest2_" + time;
        workflowScheme2Step1Name = "WorkflowScheme2Step1_" + time;
        workflowScheme2Step1ActionReviewerName = "WorkflowScheme2Step1ActionReviewer_" + time;
        workflowScheme2Step2Name = "WorkflowScheme2Step2_" + time;
        workflowScheme2Step2Action1Name = "WorkflowScheme2Step2ActionReviewer_" + time;
        workflowScheme2 = addWorkflowScheme(workflowSchemeName2);

        /* Generate scheme steps */
        workflowScheme2Step1 = addWorkflowStep(workflowScheme2Step1Name, 1, false, false,
                workflowScheme2.getId());

        workflowScheme2Step2 = addWorkflowStep(workflowScheme2Step2Name, 2, false, false,
                workflowScheme2.getId());

        /* Generate actions */
        workflowScheme2Step2Action1 = addWorkflowAction(workflowScheme2Step2Action1Name, 2,
                workflowScheme2Step2.getId(), workflowScheme2Step2.getId(), contributor,
                workflowScheme2.getId());

        workflowScheme2Step1Action1 = addWorkflowAction(workflowScheme2Step1ActionReviewerName, 1,
                workflowScheme2Step2.getId(), workflowScheme2Step1.getId(), reviewer,
                workflowScheme2.getId());



        /* not Mandatory Workflows */
        workflowSchemeName3 = "WorkflowSchemeTest3_" + time;
        workflowScheme3Step1Name = "WorkflowScheme3Step1_" + time;
        workflowScheme3Step1ActionPublisherName = "WorkflowScheme3Step1ActionPublisher_" + time;
        workflowScheme3Step2Name = "WorkflowScheme3Step2_" + time;
        workflowScheme3Step2Action1Name = "WorkflowScheme2Step2ActionReviewer_" + time;
        workflowScheme3Step2Action2Name = "WorkflowScheme2Step2ActionPublisher_" + time;
        workflowScheme3 = addWorkflowScheme(workflowSchemeName3);

        /* Generate scheme steps */
        workflowScheme3Step1 = addWorkflowStep(workflowScheme3Step1Name, 1, false, false,
                workflowScheme3.getId());

        workflowScheme3Step2 = addWorkflowStep(workflowScheme3Step2Name, 2, false, false,
                workflowScheme3.getId());

        /* Generate actions */
        workflowScheme3Step2Action1 = addWorkflowAction(workflowScheme3Step2Action1Name, 2,
                workflowScheme3Step2.getId(), workflowScheme3Step2.getId(), reviewer,
                workflowScheme3.getId());

        workflowScheme3Step2Action2 = addWorkflowAction(workflowScheme3Step2Action2Name, 3,
                workflowScheme3Step2.getId(), workflowScheme3Step2.getId(), contributor,
                workflowScheme3.getId());

        workflowScheme3Step1Action1 = addWorkflowAction(workflowScheme3Step1ActionPublisherName, 1,
                workflowScheme3Step2.getId(), workflowScheme3Step1.getId(), publisher,
                workflowScheme3.getId());

        //fourth Workflow Scheme
        contentTypeName2 = "WorkflowTesting2_" + time;
        /* Mandatory Workflow */
        workflowSchemeName4 = "WorkflowSchemeTest4" + time;
        workflowScheme4Step1Name = "WorkflowScheme4Step1_" + time;
        workflowScheme4Step1ActionViewName = "WorkflowScheme4Step1ActionView_" + time;
        workflowScheme4Step1ActionEditName = "WorkflowScheme4Step1ActionEdit_" + time;
        workflowScheme4Step1ActionPublishName = "WorkflowScheme4Step1ActionPublish_" + time;
        workflowScheme4Step1ActionEditPermissionsName =
                "WorkflowScheme4Step1ActionEditPermissions_" + time;
        workflowScheme4Step1ActionContributorName = "WorkflowScheme4Step1ActionContributor_" + time;

        workflowScheme4Step2Name = "WorkflowScheme4Step2_" + time;
        workflowScheme4Step2ActionViewName = "WorkflowScheme4Step2ActionView_" + time;
        workflowScheme4Step2ActionEditName = "WorkflowScheme4Step2ActionEdit_" + time;
        workflowScheme4Step2ActionPublishName = "WorkflowScheme4Step2ActionPublish_" + time;
        workflowScheme4Step2ActionEditPermissionsName =
                "WorkflowScheme4Step2ActionEditPermissions_" + time;
        workflowScheme4Step2ActionReviewerName = "WorkflowScheme4Step2ActionReviewer_" + time;

        workflowScheme4Step3Name = "WorkflowScheme4Step3_" + time;
        workflowScheme4Step3ActionViewName = "WorkflowScheme4Step3ActionView_" + time;
        workflowScheme4Step3ActionEditName = "WorkflowScheme4Step3ActionEdit_" + time;
        workflowScheme4Step3ActionPublishName = "WorkflowScheme4Step3ActionPublish_" + time;
        workflowScheme4Step3ActionEditPermissionsName =
                "WorkflowScheme4Step3ActionEditPermissions_" + time;
        workflowScheme4Step3ActionPublisherName = "WorkflowScheme4Step3ActionPublisher_" + time;

        /**
         * Generate ContentType 2
         */
        contentType2 = insertContentType(contentTypeName2, BaseContentType.CONTENT);

        /**
         * Generate workflow schemes
         */
        workflowScheme4 = addWorkflowScheme(workflowSchemeName4);

        /* Generate scheme steps */
        workflowScheme4Step1 = addWorkflowStep(workflowScheme4Step1Name, 1, false, false,
                workflowScheme4.getId());

        workflowScheme4Step2 = addWorkflowStep(workflowScheme4Step2Name, 2, true, false,
                workflowScheme4.getId());

        workflowScheme4Step3 = addWorkflowStep(workflowScheme4Step3Name, 3, true, false,
                workflowScheme4.getId());

        /* Generate actions */
        //-- Step 3
        workflowScheme4Step3ActionView = addWorkflowAction(workflowScheme4Step3ActionViewName, 1,
                workflowScheme4Step3.getId(), workflowScheme4Step3.getId(), anyWhoView,
                workflowScheme4.getId());
        workflowScheme4Step3ActionEdit = addWorkflowAction(workflowScheme4Step3ActionEditName, 2,
                workflowScheme4Step3.getId(), workflowScheme4Step3.getId(), anyWhoEdit,
                workflowScheme4.getId());
        workflowScheme4Step3ActionPublish = addWorkflowAction(workflowScheme4Step3ActionPublishName,
                3,
                workflowScheme4Step3.getId(), workflowScheme4Step3.getId(), anyWhoPublish,
                workflowScheme4.getId());
        workflowScheme4Step3ActionEditPermissions = addWorkflowAction(
                workflowScheme4Step3ActionEditPermissionsName, 4,
                workflowScheme4Step3.getId(), workflowScheme4Step3.getId(),
                anyWhoEditPermissions, workflowScheme4.getId());

        workflowScheme4Step3ActionPublisher = addWorkflowAction(
                workflowScheme4Step3ActionPublisherName, 5,
                workflowScheme4Step3.getId(), workflowScheme4Step3.getId(), publisher,
                workflowScheme4.getId());

       //-- Step 2
        workflowScheme4Step2ActionView = addWorkflowAction(workflowScheme4Step2ActionViewName, 1,
                workflowScheme4Step3.getId(), workflowScheme4Step2.getId(), anyWhoView,
                workflowScheme4.getId());
        workflowScheme4Step2ActionEdit = addWorkflowAction(workflowScheme4Step2ActionEditName, 2,
                workflowScheme4Step3.getId(), workflowScheme4Step2.getId(), anyWhoEdit,
                workflowScheme4.getId());
        workflowScheme4Step2ActionPublish = addWorkflowAction(workflowScheme4Step2ActionPublishName,
                3,
                workflowScheme4Step3.getId(), workflowScheme4Step2.getId(), anyWhoPublish,
                workflowScheme4.getId());
        workflowScheme4Step2ActionEditPermissions = addWorkflowAction(
                workflowScheme4Step2ActionEditPermissionsName, 4,
                workflowScheme4Step3.getId(), workflowScheme4Step2.getId(),
                anyWhoEditPermissions, workflowScheme4.getId());

        workflowScheme4Step2ActionReviewer = addWorkflowAction(
                workflowScheme4Step2ActionReviewerName, 5,
                workflowScheme4Step3.getId(), workflowScheme4Step2.getId(), reviewer,
                workflowScheme4.getId());

       //-- Step 1
        workflowScheme4Step1ActionView = addWorkflowAction(workflowScheme4Step1ActionViewName, 1,
                workflowScheme4Step2.getId(), workflowScheme4Step1.getId(), anyWhoView,
                workflowScheme4.getId());
        workflowScheme4Step1ActionEdit = addWorkflowAction(workflowScheme4Step1ActionEditName, 2,
                workflowScheme4Step2.getId(), workflowScheme4Step1.getId(), anyWhoEdit,
                workflowScheme4.getId());
        workflowScheme4Step1ActionPublish = addWorkflowAction(workflowScheme4Step1ActionPublishName,
                3,
                workflowScheme4Step2.getId(), workflowScheme4Step1.getId(), anyWhoPublish,
                workflowScheme4.getId());
        workflowScheme4Step1ActionEditPermissions = addWorkflowAction(
                workflowScheme4Step1ActionEditPermissionsName, 4,
                workflowScheme4Step2.getId(), workflowScheme4Step1.getId(),
                anyWhoEditPermissions, workflowScheme4.getId());
        workflowScheme4Step1ActionContributor = addWorkflowAction(
                workflowScheme4Step1ActionContributorName, 5,
                workflowScheme4Step2.getId(), workflowScheme4Step1.getId(), contributor,
                workflowScheme4.getId());

        /**
         * Generate ContentType 3
         */
        contentTypeName3 = "WorkflowTesting3_" + time;
        contentType3 = insertContentType(contentTypeName3, BaseContentType.CONTENT);
        contentTypeStructure3 = new StructureTransformer(ContentType.class.cast(contentType3))
                .asStructure();


        /* Workflow */
        workflowSchemeName5 = "WorkflowSchemeTest5" + time;
        workflowScheme5Step1Name = "WorkflowScheme5Step1_" + time;
        workflowScheme5Step1ActionPublishName = "WorkflowScheme5Step1ActionPublish_" + time;
        workflowScheme5Step1Action1SubAction1Name = "Publish content";

        workflowScheme5 = addWorkflowScheme(workflowSchemeName5);

        /* Generate scheme steps */
        workflowScheme5Step1 = addWorkflowStep(workflowScheme5Step1Name, 1, false, false,
                workflowScheme5.getId());

        workflowScheme5Step1Action1 = addWorkflowAction(workflowScheme5Step1ActionPublishName, 1,
                workflowScheme5Step1.getId(), workflowScheme5Step1.getId(), anyWhoView,
                workflowScheme5.getId());

        workflowScheme5Step1Action1SubAction1 = addSubActionClass(
                workflowScheme5Step1Action1SubAction1Name,
                workflowScheme5Step1Action1.getId(),
                com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet.class, 1);


    }

    /**
     * Method to test: APILocator.getPermissionAPI().doesUserHavePermission(user, permissionType, content)
     * Given Scenario: Creates a limited user with BE role and a content type.
     * Then saves a permission with full grants, the permission should be allowed from the content type.
     * ExpectedResult: The permission should be allowed to the contentlet by using owner role.
     */
    @Test()
    public void send_permission_with_limited_user_Test()
            throws DotDataException, DotSecurityException, AlreadyExistException {

        // 1 create a limited user
        final long time = System.currentTimeMillis();
        final Role backendRole = APILocator.getRoleAPI().loadBackEndUserRole();
        final Role cmsOwnerRole = APILocator.getRoleAPI().loadCMSOwnerRole();
        final User wflimitedUser = new UserDataGen().active(true).emailAddress("wflimiteduser" + time + "@dotcms.com").roles(backendRole).nextPersisted();
        // create a content type with cms owner full permissions
        final ContentType testContentType = new ContentTypeDataGen().velocityVarName("testcontenttype" + time)
                .host(APILocator.systemHost()).name("testcontenttype" + time).nextPersisted();
        final int permissionType = PermissionAPI.PERMISSION_USE | PermissionAPI.PERMISSION_EDIT |
                PermissionAPI.PERMISSION_PUBLISH | PermissionAPI.PERMISSION_EDIT_PERMISSIONS;
        final Permission permission = new Permission(testContentType.getPermissionId(), cmsOwnerRole.getId(), permissionType);
        APILocator.getPermissionAPI().save(permission, testContentType, APILocator.systemUser(), false);
        // create a contentlet of the type given permissions to the someone else
        final Contentlet contentlet = new ContentletDataGen(testContentType).user(wflimitedUser).next();
        final List<Permission> permissions = new ArrayList<>();
        permissions.add(new Permission(null, cmsOwnerRole.getId(), PermissionAPI.Type.USE.getType()));
        APILocator.getContentletAPI().checkin(contentlet, permissions, wflimitedUser, false);
        // check if the contentlet has the right permissions
        final List<Permission> contentletPermissions = APILocator.getPermissionAPI().getPermissions(contentlet);
        Assert.assertNotNull(contentletPermissions);
        Assert.assertTrue(contentletPermissions.stream().anyMatch(p -> p.getRoleId().equals(cmsOwnerRole.getId())));
    }
    @Test()
    public void delete_action_and_dependencies_Test()
            throws DotDataException, DotSecurityException, AlreadyExistException {

        // 1 create the step with one step and one action
        // 2 create a content type
        // 3 associated this action to scheme and content type
        // 4 check if mappings exists
        // 5 deletes the action
        // check mappings are gone and cache clean

        final long time               = System.currentTimeMillis();
        final String myWorkflowName   = "workflow"+time;
        final String myStep1Name      = "Step1"+time;
        final String myActionName     = "Action1"+time;
        final WorkflowScheme myWorkflowScheme       = addWorkflowScheme(myWorkflowName);
        final WorkflowStep   myWorkflowSchemeStep1  = addWorkflowStep(myStep1Name, 1, false, false, myWorkflowScheme.getId());
        final WorkflowAction myWorkflowSchemeAction = addWorkflowAction(myActionName, 1, myWorkflowSchemeStep1.getId(), myWorkflowSchemeStep1.getId(), reviewer, myWorkflowScheme.getId());
        final String      myContentTypeName         = "CTWorkflowTesting_" + time;
        final ContentType myContentType             = insertContentType(myContentTypeName, BaseContentType.CONTENT);

        workflowAPI.saveSchemeIdsForContentType                     (myContentType, CollectionsUtils.set(myWorkflowScheme.getId()));
        workflowAPI.mapSystemActionToWorkflowActionForContentType   (SystemAction.NEW, myWorkflowSchemeAction, myContentType);
        workflowAPI.mapSystemActionToWorkflowActionForWorkflowScheme(SystemAction.NEW, myWorkflowSchemeAction, myWorkflowScheme);

        final List<SystemActionWorkflowActionMapping> contentTypeMappings = workflowAPI.findSystemActionsByContentType(myContentType, user);
        final List<SystemActionWorkflowActionMapping> schemeMappings      = workflowAPI.findSystemActionsByScheme     (myWorkflowScheme, user);

        assertNotNull("contentTypeMappings can not be null", contentTypeMappings);
        assertNotNull("schemeMappings can not be null",      schemeMappings);

        assertEquals("contentTypeMappings must have 1 record", 1, contentTypeMappings.size());
        assertEquals("schemeMappings must have 1 record",      1, schemeMappings.size());

        workflowAPI.deleteAction(myWorkflowSchemeAction, user);

        final List<SystemActionWorkflowActionMapping> contentTypeMappings2 = workflowAPI.findSystemActionsByContentType(myContentType, user);
        final List<SystemActionWorkflowActionMapping> schemeMappings2      = workflowAPI.findSystemActionsByScheme     (myWorkflowScheme, user);

        assertFalse("contentTypeMappings2 and contentTypeMappings must be diff", contentTypeMappings == contentTypeMappings2);
        assertFalse("schemeMappings2 and schemeMappings must be diff", schemeMappings == schemeMappings2);
        assertFalse("contentTypeMappings2 must be null", UtilMethods.isSet(contentTypeMappings2));
        assertFalse("schemeMappings2 must be null", UtilMethods.isSet(schemeMappings2));

    }

    @Test(expected = IllegalArgumentException.class)
    public void mapSystemActionToWorkflowActionForContentType_Null_SystemAction_Test() throws DotDataException {

        workflowAPI.mapSystemActionToWorkflowActionForContentType(null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapSystemActionToWorkflowActionForContentType_Null_WFAction_Test() throws DotDataException {

        workflowAPI.mapSystemActionToWorkflowActionForContentType(WorkflowAPI.SystemAction.NEW, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapSystemActionToWorkflowActionForContentType_Null_ContentType_Test() throws DotDataException, DotSecurityException {

        workflowAPI.mapSystemActionToWorkflowActionForContentType(WorkflowAPI.SystemAction.NEW, workflowAPI.findAction(
                SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, APILocator.systemUser()), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapSystemActionToWorkflowActionForContentType_InvalidHost_ContentType_Test() throws DotDataException, DotSecurityException {

        workflowAPI.mapSystemActionToWorkflowActionForContentType(WorkflowAPI.SystemAction.NEW, workflowAPI.findAction(
                SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, APILocator.systemUser()), APILocator.systemHost().getContentType());
    }

    @Test()
    public void onLanguageDeletedEvent_Test() throws DotDataException, DotSecurityException {

        Language frenchLanguage = null;
        Contentlet contentlet;

        try {
            frenchLanguage = new LanguageDataGen()
                    .country("French")
                    .countryCode("FR")
                    .languageCode("fr")
                    .languageName("French").nextPersisted();

            final ContentType contentGenericType = contentTypeAPI.find("webPageContent");

            final ContentletDataGen contentletDataGen = new ContentletDataGen(contentGenericType.id());
            contentlet = contentletDataGen.setProperty("title", "TestContent")
                    .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT_UNICODE_CHARS).languageId(frenchLanguage.getId()).nextPersisted();
            final WorkflowStep workflowStep           = workflowAPI.findStep(SystemWorkflowConstants.WORKFLOW_NEW_STEP_ID);
            //UnassignedWorkflowContentletCheckinListener.assigned is called by default creating a task. So we better reset here before we get an duplicate entry violation.
            workflowAPI.deleteWorkflowTaskByContentletIdAnyLanguage(contentlet, user);
            final WorkflowTask workflowTask = workflowAPI.createWorkflowTask(contentlet, user, workflowStep, "test", "test");
            workflowAPI.saveWorkflowTask(workflowTask);

            final Optional<WorkflowStep> currentStepOpt = workflowAPI.findCurrentStep(contentlet);
            assertTrue(currentStepOpt.isPresent());
            assertEquals(SystemWorkflowConstants.WORKFLOW_NEW_STEP_ID, currentStepOpt.get().getId());
            APILocator.getLocalSystemEventsAPI().notify(new LanguageDeletedEvent(frenchLanguage));
            frenchLanguage = null;

            final List<Map<String, Object>>  results = new DotConnect().setSQL("select * from workflow_task where id = ?")
                    .addParam(workflowTask.getId()).loadObjectResults();
            assertFalse(UtilMethods.isSet(results));
            final List<WorkflowHistory> histories = workflowAPI.findWorkflowHistory(workflowTask);
            assertFalse(UtilMethods.isSet(histories));
        } finally {

            if (null != frenchLanguage) {
                LanguageDataGen.remove(frenchLanguage);
            }
        }
    }

    @Test()
    public void findActionMappedBySystemActionContentlet_Test() throws DotDataException, DotSecurityException {

        final ContentType contentGenericType = new ContentTypeDataGen().workflowId(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                .baseContentType(BaseContentType.CONTENT)
                .field(new FieldDataGen().name("title").velocityVarName("title").next())
                .field(new FieldDataGen().name("body").velocityVarName("body").next()).nextPersisted();
        final String unicodeText = "Numéro de téléphone";

        final List<WorkflowScheme> schemes = workflowAPI.findSchemesForContentType(contentGenericType);
        Logger.info(this, "Schemes for content type: " + contentGenericType.variable() + ", schemes" + schemes);
        assertTrue(schemes.stream().anyMatch(scheme -> scheme.getId().equals(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)));

        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentGenericType.id());
        Contentlet contentlet = contentletDataGen.setProperty("title", "TestContent")
                .setProperty("body", unicodeText ).languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId()).nextPersisted();
        contentletAPI.lock(contentlet, user, false);

        Optional<WorkflowStep> workflowStep = workflowAPI.findCurrentStep(contentlet);
        if (workflowStep.isPresent()) {
            workflowAPI.deleteWorkflowTaskByContentletIdAnyLanguage(contentlet, user);
        }

        assertFalse(workflowAPI.findCurrentStep(contentlet).isPresent());

        Optional<WorkflowAction> workflowActionOpt = workflowAPI.findActionMappedBySystemActionContentlet
                (contentlet, WorkflowAPI.SystemAction.NEW, user);

        assertTrue(workflowActionOpt.isPresent());
        assertEquals(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, workflowActionOpt.get().getId());

        workflowActionOpt = workflowAPI.findActionMappedBySystemActionContentlet
                (contentlet, WorkflowAPI.SystemAction.EDIT, user);

        assertTrue(workflowActionOpt.isPresent());
        assertEquals(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, workflowActionOpt.get().getId());
    }

    @Test()
    public void mapSystemActionToWorkflowActionForContentType_Test() throws DotDataException, DotSecurityException {

        final WorkflowAction saveAction =
                workflowAPI.findAction(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, APILocator.systemUser());

        workflowAPI.saveSchemeIdsForContentType(contentType, CollectionsUtils.set(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID));

        final SystemActionWorkflowActionMapping mapping =
                workflowAPI.mapSystemActionToWorkflowActionForContentType
                    (WorkflowAPI.SystemAction.NEW, saveAction, contentType);

        assertNotNull(mapping);
        assertEquals(WorkflowAPI.SystemAction.NEW, mapping.getSystemAction());
        assertEquals(saveAction,  mapping.getWorkflowAction());
        assertEquals(contentType, mapping.getOwner());

        final Optional<SystemActionWorkflowActionMapping> savedMapping =
                workflowAPI.findSystemActionByIdentifier(mapping.getIdentifier(), APILocator.systemUser());

        assertNotNull(savedMapping);
        assertTrue(savedMapping.isPresent());
        assertEquals(WorkflowAPI.SystemAction.NEW, savedMapping.get().getSystemAction());
        assertEquals(saveAction,  savedMapping.get().getWorkflowAction());
        assertEquals(contentType.id(), ContentType.class.cast(savedMapping.get().getOwner()).id());

        final SystemActionWorkflowActionMapping mappingEdit =
                workflowAPI.mapSystemActionToWorkflowActionForContentType
                        (WorkflowAPI.SystemAction.EDIT, saveAction, contentType);

        assertNotNull(mappingEdit);
        assertEquals(WorkflowAPI.SystemAction.EDIT, mappingEdit.getSystemAction());
        assertEquals(saveAction,  mappingEdit.getWorkflowAction());
        assertEquals(contentType, mappingEdit.getOwner());

        /////

        final Optional<SystemActionWorkflowActionMapping> systemActionByContentTypeOpt = workflowAPI.findSystemActionByContentType
                (WorkflowAPI.SystemAction.NEW, contentType, APILocator.systemUser());

        assertNotNull(systemActionByContentTypeOpt);
        assertTrue(systemActionByContentTypeOpt.isPresent());
        assertEquals(WorkflowAPI.SystemAction.NEW, systemActionByContentTypeOpt.get().getSystemAction());
        assertEquals(saveAction, systemActionByContentTypeOpt.get().getWorkflowAction());
        assertEquals(contentType, systemActionByContentTypeOpt.get().getOwner());

        final Optional<SystemActionWorkflowActionMapping> systemActionByContentTypeEditOpt = workflowAPI.findSystemActionByContentType
                (WorkflowAPI.SystemAction.EDIT, contentType, APILocator.systemUser());

        assertNotNull(systemActionByContentTypeEditOpt);
        assertTrue(systemActionByContentTypeEditOpt.isPresent());
        assertEquals(WorkflowAPI.SystemAction.EDIT, systemActionByContentTypeEditOpt.get().getSystemAction());
        assertEquals(saveAction, systemActionByContentTypeEditOpt.get().getWorkflowAction());
        assertEquals(contentType, systemActionByContentTypeEditOpt.get().getOwner());

        final List<SystemActionWorkflowActionMapping> mappings = workflowAPI.findSystemActionsByContentType(contentType, APILocator.systemUser());

        assertTrue(UtilMethods.isSet(mappings));
        assertTrue(mappings.size() >= 2);

        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(contentType);

        final Optional<WorkflowAction> newAction = workflowAPI.findActionMappedBySystemActionContentlet
                (contentlet, WorkflowAPI.SystemAction.NEW, APILocator.systemUser());

        assertTrue(newAction.isPresent());
        assertEquals(saveAction, newAction.get());

        /////
        final List<SystemActionWorkflowActionMapping> systemActionsByWorkflowActionList =
                workflowAPI.findSystemActionsByWorkflowAction(saveAction, APILocator.systemUser());

        assertTrue(UtilMethods.isSet(systemActionsByWorkflowActionList));
        assertTrue(systemActionsByWorkflowActionList.stream().anyMatch(aMapping -> aMapping.getSystemAction() == WorkflowAPI.SystemAction.NEW));

        for (final SystemActionWorkflowActionMapping systemActionWorkflowActionMapping : mappings) {
            workflowAPI.deleteSystemAction(systemActionWorkflowActionMapping);
        }

        final Optional<SystemActionWorkflowActionMapping> systemActionByContentTypeDeletedOpt = workflowAPI.findSystemActionByContentType
                (WorkflowAPI.SystemAction.NEW, contentType, APILocator.systemUser());

        assertNotNull(systemActionByContentTypeDeletedOpt);
        assertFalse(systemActionByContentTypeDeletedOpt.isPresent());
    }



    /////
    @Test(expected = IllegalArgumentException.class)
    public void mapSystemActionToWorkflowActionForWorkflowScheme_Null_SystemAction_Test() throws DotDataException {

        workflowAPI.mapSystemActionToWorkflowActionForWorkflowScheme(null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapSystemActionToWorkflowActionForWorkflowScheme_Null_WFAction_Test() throws DotDataException {

        workflowAPI.mapSystemActionToWorkflowActionForWorkflowScheme(WorkflowAPI.SystemAction.NEW, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapSystemActionToWorkflowActionForWorkflowScheme_Null_Workflow_Scheme_Test() throws DotDataException, DotSecurityException {

        workflowAPI.mapSystemActionToWorkflowActionForWorkflowScheme(WorkflowAPI.SystemAction.NEW, workflowAPI.findAction(
                SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, APILocator.systemUser()), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapSystemActionToWorkflowActionForWorkflowScheme_InvalidScheme_Test() throws DotDataException, DotSecurityException {

        final Optional<WorkflowScheme> foundSchemeOpt = workflowAPI.findSchemes(false).stream().filter(workflowScheme ->
                !SystemWorkflowConstants.SYSTEM_WORKFLOW_ID.equals(workflowScheme.getId())).findFirst();

        if (foundSchemeOpt.isPresent()) {

            workflowAPI.mapSystemActionToWorkflowActionForWorkflowScheme(WorkflowAPI.SystemAction.NEW, workflowAPI.findAction(
                    SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, APILocator.systemUser()), foundSchemeOpt.get());
        }
    }

    @Test()
    public void mapSystemActionToWorkflowActionForWorkflowScheme_Test() throws DotDataException, DotSecurityException {

        final WorkflowScheme myWorkflow = TestWorkflowUtils
                .getDocumentWorkflow("Workflow" + System.currentTimeMillis());
        final ContentType blogContentType = TestDataUtils.getBlogLikeContentType();

        workflowAPI.saveSchemeIdsForContentType(blogContentType,
                CollectionsUtils.set(myWorkflow.getId()));

        final WorkflowAction firstAction =
            workflowAPI.findActions(workflowAPI.findFirstStep(myWorkflow.getId()).get(), user).stream().findFirst().get();
        final SystemActionWorkflowActionMapping mapping =
                workflowAPI.mapSystemActionToWorkflowActionForWorkflowScheme
                        (WorkflowAPI.SystemAction.NEW, firstAction, myWorkflow);

        assertNotNull(mapping);
        assertEquals(WorkflowAPI.SystemAction.NEW, mapping.getSystemAction());
        assertEquals(firstAction,  mapping.getWorkflowAction());
        assertEquals(myWorkflow, mapping.getOwner());

        final List<SystemActionWorkflowActionMapping> systemActionsByScheme = workflowAPI.findSystemActionsByScheme
                (myWorkflow, APILocator.systemUser());

        assertNotNull(systemActionsByScheme);
        assertTrue(UtilMethods.isSet(systemActionsByScheme));
        assertTrue(systemActionsByScheme.stream().anyMatch(systemMapping ->
                systemMapping.getSystemAction() == WorkflowAPI.SystemAction.NEW && systemMapping.getWorkflowAction().equals(firstAction)));

        final Contentlet contentlet = TestDataUtils
                .getBlogContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());
        final Optional<WorkflowAction> newAction = workflowAPI.findActionMappedBySystemActionContentlet
                (contentlet, WorkflowAPI.SystemAction.NEW, APILocator.systemUser());

        assertTrue(newAction.isPresent());
        assertEquals(firstAction, newAction.get());

        for (final SystemActionWorkflowActionMapping systemActionWorkflowActionMapping: systemActionsByScheme) {

            workflowAPI.deleteSystemAction(systemActionWorkflowActionMapping);
        }

        final List<SystemActionWorkflowActionMapping> systemActionsBySchemeDeleted = workflowAPI.findSystemActionsByScheme
                (myWorkflow, APILocator.systemUser());

        assertFalse(UtilMethods.isSet(systemActionsBySchemeDeleted));
    }
    /////

    @Test(expected = DoesNotExistException.class)
    public void findFirstStepForActionNonExistingTest () throws DotDataException {

        final WorkflowAction workflowAction       = new WorkflowAction();
        workflowAction.setId("non-existing");
        workflowAction.setSchemeId("non-existing");
        final Optional<WorkflowStep> workflowStep = workflowAPI.findFirstStepForAction(workflowAction);
        Assert.assertFalse(workflowStep.isPresent());
    }

    @Test()
    public void findFirstStepForActionExistingTest () throws DotDataException, DotSecurityException {

        final WorkflowAction workflowAction       = workflowAPI.findAction(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, APILocator.systemUser());
        final Optional<WorkflowStep> workflowStep = workflowAPI.findFirstStepForAction(workflowAction);
        Assert.assertTrue(workflowStep.isPresent());
        Assert.assertEquals(SystemWorkflowConstants.WORKFLOW_NEW_STEP_ID, workflowStep.get().getId());
    }

    @Test(expected = DoesNotExistException.class)
    public void findFirstStepNonExistingTest () throws DotDataException {

        final Optional<WorkflowStep> workflowStep = workflowAPI.findFirstStep("xxxx");
        Assert.assertFalse(workflowStep.isPresent());
    }

    @Test()
    public void findFirstStepExistingTest () throws DotDataException {

        final Optional<WorkflowStep> workflowStep = workflowAPI.findFirstStep(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID);
        Assert.assertTrue(workflowStep.isPresent());
        Assert.assertEquals(SystemWorkflowConstants.WORKFLOW_NEW_STEP_ID, workflowStep.get().getId());
    }

    @Test()
    public void hasSaveActionlet_True_Test () throws DotDataException, DotSecurityException {

        final WorkflowAction saveAction = workflowAPI.findAction
                (SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, APILocator.systemUser());

        assertNotNull(saveAction);
        assertTrue(workflowAPI.hasSaveActionlet(saveAction));
    }

    @Test()
    public void hasSaveActionlet_False_Test () throws DotDataException, DotSecurityException {

        final WorkflowAction deleteAction = workflowAPI.findAction
                (SystemWorkflowConstants.WORKFLOW_DELETE_ACTION_ID, APILocator.systemUser());

        assertNotNull(deleteAction);
        assertFalse(workflowAPI.hasSaveActionlet(deleteAction));
    }

    @Test()
    public void hasPublishActionlet_True_Test () throws DotDataException, DotSecurityException {

        final WorkflowAction publishAction = workflowAPI.findAction
                (SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID, APILocator.systemUser());

        assertNotNull(publishAction);
        assertTrue(workflowAPI.hasPublishActionlet(publishAction));
    }

    @Test()
    public void hasPublishActionlet_False_Test () throws DotDataException, DotSecurityException {

        final WorkflowAction deleteAction = workflowAPI.findAction
                (SystemWorkflowConstants.WORKFLOW_DELETE_ACTION_ID, APILocator.systemUser());

        assertNotNull(deleteAction);
        assertFalse(workflowAPI.hasPublishActionlet(deleteAction));
    }

    @Test()
    public void hasArchiveActionlet_True_Test () throws DotDataException, DotSecurityException {

        final WorkflowAction publishAction = workflowAPI.findAction
                (SystemWorkflowConstants.WORKFLOW_ARCHIVE_ACTION_ID, APILocator.systemUser());

        assertNotNull(publishAction);
        assertTrue(workflowAPI.hasArchiveActionlet(publishAction));
    }

    @Test()
    public void hasArchiveActionlet_False_Test () throws DotDataException, DotSecurityException {

        final WorkflowAction deleteAction = workflowAPI.findAction
                (SystemWorkflowConstants.WORKFLOW_DELETE_ACTION_ID, APILocator.systemUser());

        assertNotNull(deleteAction);
        assertFalse(workflowAPI.hasArchiveActionlet(deleteAction));
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

        /* Associate the schemas to the content type */
        workflowAPI.saveSchemesForStruct(contentTypeStructure, worflowSchemes);

        List<WorkflowScheme> contentTypeSchemes = workflowAPI
                .findSchemesForStruct(contentTypeStructure);
        assertTrue(contentTypeSchemes != null && contentTypeSchemes.size() == 2);
        assertTrue(containsScheme(workflowScheme1, contentTypeSchemes));
        assertTrue(containsScheme(workflowScheme2, contentTypeSchemes));

        //Associate another Workflow Scheme
        worflowSchemes.add(workflowScheme3);
        workflowAPI.saveSchemesForStruct(contentTypeStructure, worflowSchemes);

        /* validate that the schemes area associated */
        contentTypeSchemes = workflowAPI.findSchemesForStruct(contentTypeStructure);
        assertTrue(contentTypeSchemes != null && contentTypeSchemes.size() == 3);
        assertTrue(containsScheme(workflowScheme1, contentTypeSchemes));
        assertTrue(containsScheme(workflowScheme2, contentTypeSchemes));
        assertTrue(containsScheme(workflowScheme3, contentTypeSchemes));

        //Remove one associated Workflow Scheme
        worflowSchemes.remove(1);
        workflowAPI.saveSchemesForStruct(contentTypeStructure, worflowSchemes);

        /* validate that the schemes area associated */
        contentTypeSchemes = workflowAPI.findSchemesForStruct(contentTypeStructure);
        assertTrue(contentTypeSchemes != null && contentTypeSchemes.size() == 2);
        assertTrue(containsScheme(workflowScheme1, contentTypeSchemes));
        assertTrue(containsScheme(workflowScheme3, contentTypeSchemes));
    }

    /**
     * This method test the deep copy workflow scheme method
     */
    @Test
    public void copy_system_workflow_success()
            throws DotDataException, DotSecurityException, AlreadyExistException, ExecutionException, InterruptedException {

        WorkflowScheme schemeCopied = null;
        try {

            final WorkflowScheme scheme =
                    workflowAPI.findSystemWorkflowScheme();

            schemeCopied = workflowAPI.deepCopyWorkflowScheme(scheme, user, Optional.empty());

            assertNotNull(schemeCopied);
            assertNotEquals(schemeCopied.getId(), scheme.getId());
            assertNotEquals(schemeCopied.getName(), scheme.getName());
            assertEquals(schemeCopied.getDescription(), scheme.getDescription());

            final List<WorkflowStep> steps =
                    workflowAPI.findSteps(scheme);

            final List<WorkflowStep> stepsCopied =
                    workflowAPI.findSteps(schemeCopied);

            assertNotNull(steps);
            assertNotNull(stepsCopied);
            assertEquals(steps.size(), stepsCopied.size());

            assertEqualsSteps(steps, stepsCopied, scheme, schemeCopied);

            final List<WorkflowAction> actions =
                    workflowAPI.findActions(scheme, user);

            final List<WorkflowAction> actionsCopied =
                    workflowAPI.findActions(schemeCopied, user);

            assertNotNull(actions);
            assertNotNull(actionsCopied);
            assertEquals(actions.size(), actionsCopied.size());

            assertEqualsActions(actions, actionsCopied, scheme, schemeCopied);
        } finally {

            // remove the copied scheme
            if (null != schemeCopied) {
                workflowAPI.archive(schemeCopied, user);
                workflowAPI.deleteScheme(schemeCopied, user).get();
            }
        }
    }

    private void assertEqualsActions(final List<WorkflowAction> actions,
            final List<WorkflowAction> actionsCopied,
            final WorkflowScheme scheme,
            final WorkflowScheme schemeCopied) {

        for (final WorkflowAction action : actions) {

            final Optional<WorkflowAction> copiedAction =
                    actionsCopied.stream()
                            .filter(theAction -> theAction.getName().equals(action.getName()))
                            .findFirst();
            if (copiedAction.isPresent()) {

                assertNotEquals(copiedAction.get().getId(), action.getId());
                assertNotEquals(copiedAction.get().getSchemeId(), action.getSchemeId());

                assertEquals(action.getSchemeId(), scheme.getId());
                assertEquals(copiedAction.get().getSchemeId(), schemeCopied.getId());

                assertEquals(copiedAction.get().getName(), action.getName());

                if (WorkflowAction.CURRENT_STEP.equals(action.getNextStep())) {
                    assertEquals(copiedAction.get().getNextStep(), action.getNextStep());
                } else {
                    assertNotEquals(copiedAction.get().getNextStep(), WorkflowAction.CURRENT_STEP);
                    assertNotEquals(copiedAction.get().getNextStep(), action.getNextStep());
                }
            } else {
                fail("The step: " + action.getName()
                        + " does not exists and must exists as part of the copy");
            }
        }
    }

    private void assertEqualsSteps(final List<WorkflowStep> steps,
            final List<WorkflowStep> stepsCopied,
            final WorkflowScheme scheme,
            final WorkflowScheme schemeCopied) {

        for (final WorkflowStep step : steps) {

            final Optional<WorkflowStep> copiedStep =
                    stepsCopied.stream().filter(theStep -> theStep.getName().equals(step.getName()))
                            .findFirst();

            if (copiedStep.isPresent()) {

                assertNotEquals(copiedStep.get().getId(), step.getId());
                assertNotEquals(copiedStep.get().getSchemeId(), step.getSchemeId());

                assertEquals(step.getSchemeId(), scheme.getId());
                assertEquals(copiedStep.get().getSchemeId(), schemeCopied.getId());
            } else {
                fail("The step: " + step.getName()
                        + " does not exists and must exists as part of the copy");
            }
        }
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
            c1.setIndexPolicy(IndexPolicy.FORCE);
            c1 = contentletAPI.checkin(c1, user, false);

            c2.setLanguageId(1);
            c2.setStringProperty(FIELD_VAR_NAME, "WorkflowContentTest2_" + time);
            c2.setContentTypeId(contentType.id());
            c2.setIndexPolicy(IndexPolicy.FORCE);
            c2 = contentletAPI.checkin(c2, user, false);


            Contentlet c = APILocator.getContentletAPI().checkout(c2.getInode(), user, false);

            //set step action for content2
            c.setActionId(workflowScheme2Step1Action1.getId());
            c.setStringProperty("wfActionComments", "Test" + time);

            c2 = APILocator.getContentletAPI().checkin(c, user, false);

            //get step for content with a selection action
            List<WorkflowStep>  steps = workflowAPI.findStepsByContentlet(c2);
            assertTrue(steps.size() >= 1);
            // workflowScheme2Step2.getId(), true, workflowScheme2Step1.getId(),
            assertEquals(workflowScheme2Step2.getName(), steps.get(0).getName());
        } finally {

            contentletAPI.destroy(c1, user, false);
            contentletAPI.destroy(c2, user, false);
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

        actions = workflowAPI.findActions(workflowScheme1Step2, reviewer, null);
        assertNotNull(actions);
        assertTrue(actions.size() == 1);

        actions = workflowAPI.findActions(workflowScheme3Step2, contributor, null);
        assertNotNull(actions);
        assertTrue(actions.size() == 1);

    }

    @Test
    public void findActionsRestrictedByPermission() throws DotDataException, DotSecurityException {
        ContentType contentTypeForPublisher = null;
        ContentType contentTypeForContributor = null;
        try {
            final String ctVisibleByPublisher = "CTVisibleByPublisher" + System.currentTimeMillis();
            contentTypeForPublisher = createContentTypeAndAssignPermissions(ctVisibleByPublisher,
                    BaseContentType.CONTENT, editPermission, publisher.getId());

            //Actions visible by contributor
            final List<WorkflowAction> actionsVisibleByContributor1 = workflowAPI.findActions(workflowScheme3Step2, contributor, contentTypeForPublisher);
            assertNotNull(actionsVisibleByContributor1);
            assertTrue(actionsVisibleByContributor1.size() == 0);

            final String ctVisibleByContributor = "CTVisibleByContributor" + System.currentTimeMillis();
            contentTypeForContributor = createContentTypeAndAssignPermissions(ctVisibleByContributor,
                    BaseContentType.CONTENT, editPermission, contributor.getId());

            final List<WorkflowAction> actionsVisibleByContributor2 = workflowAPI.findActions(workflowScheme3Step2, contributor, contentTypeForContributor);
            assertNotNull(actionsVisibleByContributor2);
            assertTrue(actionsVisibleByContributor2.size() == 1);

        } finally {
            if(contentTypeForPublisher != null){
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentTypeForPublisher);
            }

            if(contentTypeForContributor != null){
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentTypeForContributor);
            }
        }
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
            c1.setIndexPolicy(IndexPolicy.FORCE);
            c1 = contentletAPI.checkin(c1, user, false);

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

        } finally {
            contentletAPI.destroy(c1, user, false);
        }
    }


    /**
     * Test the find findAvailableActions methods
     */
    @Test
    public void findAvailableActions() throws Exception {

        /*
        Need to do the test checking with different user the actions displayed. We need to specify
        the permission for Intranet, Reviewer, Contributor and Publisher to see if the action
        returned are the right ones
         */

        Contentlet testContentlet = new Contentlet();
        try {
            List<WorkflowScheme> workflowSchemes = new ArrayList<>();
            workflowSchemes.add(workflowScheme1);
            workflowSchemes.add(workflowScheme2);
            workflowSchemes.add(workflowScheme3);
            workflowSchemes.add(workflowScheme4);

            /* Associate the schemas to the content type */
            workflowAPI.saveSchemesForStruct(contentTypeStructure, workflowSchemes);

            long time = System.currentTimeMillis();

            //Create a test contentlet
            testContentlet.setLanguageId(1);
            testContentlet.setStringProperty(FIELD_VAR_NAME, "WorkflowContentTest_" + time);
            testContentlet.setContentTypeId(contentType.id());
            testContentlet.setHost(defaultHost.getIdentifier());
            testContentlet.setIndexPolicy(IndexPolicy.FORCE);
            testContentlet = contentletAPI.checkin(testContentlet, user, false);
            APILocator.getWorkflowAPI().deleteWorkflowTaskByContentletIdAnyLanguage(testContentlet, user);

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

                    for(final WorkflowAction a:foundActions){
                       System.out.println(a);
                    }

            assertNotNull(foundActions);
            assertFalse(foundActions.isEmpty());
            assertTrue(foundActions.size() > 1);

            foundActions = APILocator.getWorkflowAPI()
                    .findAvailableActions(testContentlet, janeReviewer);

            assertNotNull(foundActions);
            assertFalse(foundActions.isEmpty());
            assertEquals(foundActions.size(), 4);

            foundActions = APILocator.getWorkflowAPI()
                    .findAvailableActions(testContentlet, chrisPublisher);

            assertNotNull(foundActions);
            assertFalse(foundActions.isEmpty());
            assertEquals(foundActions.size(), 6);

            foundActions = APILocator.getWorkflowAPI()
                    .findAvailableActions(testContentlet, joeContributor);

            assertNotNull(foundActions);
            assertFalse(foundActions.isEmpty());
            assertEquals(foundActions.size(), 5);
            ////
            final Contentlet testContentleti = testContentlet;
            runNoLicense(()-> {

               final List<WorkflowStep> steps = getSteps(testContentleti);

                Assert.assertFalse(steps.isEmpty());
                Assert.assertEquals(WorkflowAPI.SYSTEM_WORKFLOW_ID, steps.get(0).getSchemeId());
            });
        } finally {
            contentletAPI.destroy(testContentlet, user, false);
        }

    }

    @WrapInTransaction
    private List<WorkflowStep>  getSteps (final Contentlet testContentleti) throws DotDataException {

        final WorkFlowFactory workFlowFactory = FactoryLocator.getWorkFlowFactory();
        final List<WorkflowScheme> schemes = Arrays.asList(workFlowFactory.findSystemWorkflow()) ;
        CacheLocator.getWorkFlowCache().clearCache();
        List<WorkflowStep> steps = FactoryLocator.getWorkFlowFactory().findStepsByContentlet(testContentleti, schemes);
        return steps;
    }

    /**
     * Test the find findAvailableActions methods
     */
    @Test
    public void findAvailableActions_working_version_get_empty_actions() throws DotDataException, DotSecurityException {

        /*
        Need to do the test checking with different user the actions displayed. We need to specify
        the permission for Intranet, Reviewer, Contributor and Publisher to see if the action
        returned are the right ones
         */

        Contentlet testContentlet1 = new Contentlet();
        Contentlet testContentlet1Checkout = null;
        Contentlet testContentlet2 = new Contentlet();
        Contentlet testContentlet2Checkout = null;
        Contentlet testContentletTop = new Contentlet();
        try {
            List<WorkflowScheme> workflowSchemes = new ArrayList<>();
            workflowSchemes.add(workflowScheme1);
            workflowSchemes.add(workflowScheme2);
            workflowSchemes.add(workflowScheme3);
            workflowSchemes.add(workflowScheme4);

            /* Associate the schemas to the content type */
            workflowAPI.saveSchemesForStruct(contentTypeStructure, workflowSchemes);

            long time = System.currentTimeMillis();

            //Create a test contentlet
            testContentlet1.setLanguageId(1);
            testContentlet1.setStringProperty(FIELD_VAR_NAME, "WorkflowContentTest_" + time);
            testContentlet1.setContentTypeId(contentType.id());
            testContentlet1.setHost(defaultHost.getIdentifier());
            testContentlet1.setIndexPolicy(IndexPolicy.FORCE);
            testContentlet1 = contentletAPI.checkin(testContentlet1, user, false);
            APILocator.getWorkflowAPI().deleteWorkflowTaskByContentletIdAnyLanguage(testContentlet1, user);

            final Role role = APILocator.getRoleAPI().getUserRole(billIntranet);
            //Adding permissions to the just created contentlet
            List<Permission> permissions = new ArrayList<>();
            Permission p1 = new Permission(
                    testContentlet1.getPermissionId(),
                    role.getId(),
                    (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT),
                    true);

            permissions.add(p1);

            APILocator.getPermissionAPI().save(permissions, testContentlet1, user, false);

            // making more versions
            testContentlet1Checkout = contentletAPI.checkout(testContentlet1.getInode(), user, false);
            testContentlet1Checkout.setStringProperty(FIELD_VAR_NAME, "WorkflowContentTest_" + System.currentTimeMillis());
            testContentlet1Checkout.setIndexPolicy(IndexPolicy.FORCE);
            testContentlet2 = contentletAPI.checkin(testContentlet1Checkout, user, false);
            APILocator.getWorkflowAPI().deleteWorkflowTaskByContentletIdAnyLanguage(testContentlet2, user);

            // top version
            testContentlet2Checkout = contentletAPI.checkout(testContentlet2.getInode(), user, false);
            testContentlet2Checkout.setStringProperty(FIELD_VAR_NAME, "WorkflowContentTest_" + System.currentTimeMillis());
            testContentlet2Checkout.setIndexPolicy(IndexPolicy.FORCE);
            testContentlet1Checkout.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
            testContentletTop = contentletAPI.checkin(testContentlet2Checkout, user, false);
            APILocator.getWorkflowAPI().deleteWorkflowTaskByContentletIdAnyLanguage(testContentlet2Checkout, user);

            // expected behavior
            List<WorkflowAction> foundActions = APILocator.getWorkflowAPI().findAvailableActions(testContentletTop, billIntranet);
            assertNotNull(foundActions);
            assertFalse(foundActions.isEmpty());
            assertTrue(foundActions.size() > 1);

            // no top version
            foundActions = APILocator.getWorkflowAPI()
                    .findAvailableActions(testContentlet2, billIntranet);
            assertNotNull(foundActions);
            assertTrue(foundActions.isEmpty());
        } finally {
            try {
                final Contentlet contentletToDelete = contentletAPI.findContentletByIdentifierAnyLanguage(testContentletTop.getIdentifier());

                contentletAPI.destroy(contentletToDelete, user, false);
            } catch (Exception e) {}
        }
    }
    /**
     * Test the find findActionRespectingPermissions methods
     */
    @Test
    public void findActionRespectingPermissions() throws DotDataException, DotSecurityException {

        //Users
        final User billIntranet =  TestUserUtils.getBillIntranetUser(); //APILocator.getUserAPI().loadUserById("dotcms.org.2806");
        final User chrisPublisher = TestUserUtils.getChrisPublisherUser(); //APILocator.getUserAPI().loadUserById("dotcms.org.2795");

        Contentlet testContentlet = new Contentlet();
        try {

            //Set Workflow on contentType3
            List<WorkflowScheme> worflowSchemes = new ArrayList<>();
            worflowSchemes.add(workflowScheme5);

            /* Associate the schemas to the content type */
            workflowAPI.saveSchemesForStruct(contentTypeStructure3, worflowSchemes);

            long time = System.currentTimeMillis();
            testContentlet.setLanguageId(1);
            testContentlet.setStringProperty(FIELD_VAR_NAME, "Workflow5ContentTest_" + time);
            testContentlet.setContentTypeId(contentType3.id());
            testContentlet.setHost(defaultHost.getIdentifier());
            testContentlet.setIndexPolicy(IndexPolicy.FORCE);
            testContentlet = contentletAPI.checkin(testContentlet,
                    APILocator.getPermissionAPI().getPermissions(testContentlet, false, true), user,
                    false);

            //Adding permissions to limited user on the contentType3
            List<Permission> permissions = new ArrayList<>();
            Permission p1 = new Permission(
                    testContentlet.getPermissionId(),
                    APILocator.getRoleAPI().getUserRole(chrisPublisher).getId(),
                    (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                            | PermissionAPI.PERMISSION_WRITE
                            | PermissionAPI.PERMISSION_PUBLISH
                            | PermissionAPI.PERMISSION_EDIT_PERMISSIONS),
                    true);
            permissions.add(p1);

            APILocator.getPermissionAPI().save(permissions, testContentlet, user, false);

            //Validate the saved permissions
            List<Permission> foundContentletPermissions = APILocator.getPermissionAPI()
                    .getPermissions(testContentlet);
            assertNotNull(foundContentletPermissions);
            assertFalse(foundContentletPermissions.isEmpty());

            WorkflowAction action = APILocator.getWorkflowAPI()
                    .findActionRespectingPermissions(workflowScheme5Step1Action1.getId(),
                            testContentlet, chrisPublisher);
            assertNotNull(action);
            assertEquals(action.getName(), workflowScheme5Step1Action1.getName());

            //This should throw a DotSecurityException
            try {
                action = APILocator.getWorkflowAPI()
                        .findActionRespectingPermissions(workflowScheme5Step1Action1.getId(),
                                testContentlet, billIntranet);
            } catch (Exception e) {
                assertTrue(e instanceof DotSecurityException);
            }

            action = APILocator.getWorkflowAPI()
                    .findActionRespectingPermissions(workflowScheme5Step1Action1.getId(),
                            workflowScheme5Step1.getId(), testContentlet, chrisPublisher);
            assertNotNull(action);
            assertEquals(action.getName(), workflowScheme5Step1Action1.getName());

            //This should throw a DotSecurityException
            try {
                action = APILocator.getWorkflowAPI()
                        .findActionRespectingPermissions(workflowScheme5Step1Action1.getId(),
                                workflowScheme5Step1.getId(), testContentlet, billIntranet);
            } catch (Exception e) {
                assertTrue(e instanceof DotSecurityException);
            }

        } finally {
            contentletAPI.destroy(testContentlet, user, false);
        }

    }

    /**
     * Test the find findAction methods
     */
    @Test
    public void findAction() throws DotDataException, DotSecurityException {

        //Users
        final User billIntranet =  TestUserUtils.getBillIntranetUser(); //APILocator.getUserAPI().loadUserById("dotcms.org.2806");
        final User chrisPublisher = TestUserUtils.getChrisPublisherUser(); //APILocator.getUserAPI().loadUserById("dotcms.org.2795");

        Contentlet testContentlet = new Contentlet();
        try {

            //Set Workflow on contentType3
            List<WorkflowScheme> worflowSchemes = new ArrayList<>();
            worflowSchemes.add(workflowScheme5);

            /* Associate the schemas to the content type */
            workflowAPI.saveSchemesForStruct(contentTypeStructure3, worflowSchemes);

            long time = System.currentTimeMillis();
            testContentlet.setLanguageId(1);
            testContentlet.setStringProperty(FIELD_VAR_NAME, "Workflow5ContentTest_" + time);
            testContentlet.setContentTypeId(contentType3.id());
            testContentlet.setHost(defaultHost.getIdentifier());
            testContentlet.setIndexPolicy(IndexPolicy.FORCE);
            testContentlet = contentletAPI.checkin(testContentlet,
                    APILocator.getPermissionAPI().getPermissions(testContentlet, false, true), user,
                    false);

            //Adding permissions to limited user on the contentType3
            List<Permission> permissions = new ArrayList<>();
            Permission p1 = new Permission(
                    testContentlet.getPermissionId(),
                    APILocator.getRoleAPI().getUserRole(chrisPublisher).getId(),
                    (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                            | PermissionAPI.PERMISSION_WRITE
                            | PermissionAPI.PERMISSION_PUBLISH
                            | PermissionAPI.PERMISSION_EDIT_PERMISSIONS),
                    true);
            permissions.add(p1);

            APILocator.getPermissionAPI().save(permissions, testContentlet, user, false);

            //Validate the saved permissions
            List<Permission> foundContentletPermissions = APILocator.getPermissionAPI()
                    .getPermissions(testContentlet);
            assertNotNull(foundContentletPermissions);
            assertFalse(foundContentletPermissions.isEmpty());

            WorkflowAction action = APILocator.getWorkflowAPI()
                    .findAction(workflowScheme5Step1Action1.getId(),
                            workflowScheme5Step1.getId(), chrisPublisher);
            assertNotNull(action);
            assertEquals(action.getName(), workflowScheme5Step1Action1.getName());

            action = APILocator.getWorkflowAPI().findAction(workflowScheme5Step1Action1.getId(),
                    workflowScheme5Step1.getId(), chrisPublisher);
            assertNotNull(action);
            assertEquals(action.getName(), workflowScheme5Step1Action1.getName());

        } finally {
            contentletAPI.destroy(testContentlet, user, false);
        }

    }

    /**
     * This Test validate that a workflow step could not be deleted if depends of another step or
     * has a contentlet related
     */
    @Test
    public void issue5197()
            throws DotDataException, IOException, DotSecurityException, AlreadyExistException, ExecutionException, InterruptedException {
        WorkflowScheme ws = null;
        WorkflowStep step1 = null;
        WorkflowStep step2 = null;
        WorkflowAction action1 = null;
        WorkflowAction action2 = null;
        ContentType st = null;
        Contentlet contentlet1 = null;

        try {

            final User adminUser = APILocator.getUserAPI()
                    .loadByUserByEmail("admin@dotcms.com", user, false);
            Role role = roleAPI.getUserRole(adminUser);


            final User anonymousUser = APILocator.getUserAPI().getAnonymousUser();
            final Role anonymousRole = roleAPI.getUserRole(anonymousUser);

            /*
             * Create workflow scheme
		     */
            String schemeName = "issue5197-" + UtilMethods.dateToHTMLDate(new Date(), DATE_FORMAT);
            addWorkflowScheme(schemeName);

            ws = workflowAPI.findSchemeByName(schemeName);
            assertTrue(UtilMethods.isSet(ws));

            /*
             * Create scheme step1
		     */
            addWorkflowStep("Edit", 1, false, false, ws.getId());

            List<WorkflowStep> steps = workflowAPI.findSteps(ws);
            assertTrue(steps.size() == 1);
            step1 = steps.get(0);

            /*
             * Create scheme step2
		     */
            addWorkflowStep("Publish", 2, true, false, ws.getId());
            steps = workflowAPI.findSteps(ws);
            assertTrue(steps.size() == 2);
            step2 = steps.get(1);

            /*
		     * Add action to scheme step1
		     */
            addWorkflowAction("Edit", 1,
                    step2.getId(), step1.getId(), anonymousRole,
                    ws.getId());

            final List<WorkflowAction> actions1 = workflowAPI.findActions(step1, user);
            assertTrue(actions1.size() == 1);
            action1 = actions1.get(0);

            /*
		     * Add action to scheme step2
		     */
            addWorkflowAction("Publish", 1,
                    step2.getId(), step2.getId(), anonymousRole,
                    ws.getId());

            final List<WorkflowAction> actions2 = workflowAPI.findActions(step2, user);
            assertTrue(actions2.size() == 1);
            action2 = actions2.get(0);

            /*
		     * Create structure and add workflow scheme
		     */
            st = insertContentType("Issue5197Structure", BaseContentType.CONTENT);
            final Structure contentTypeSt = new StructureTransformer(ContentType.class.cast(st))
                    .asStructure();
            Permission p = new Permission();
            p.setInode(st.getPermissionId());
            p.setRoleId(roleAPI.loadCMSAnonymousRole().getId());
            p.setPermission(PermissionAPI.PERMISSION_READ);
            permissionAPI.save(p, st, user, true);

            p = new Permission();
            p.setInode(st.getPermissionId());
            p.setRoleId(roleAPI.loadCMSAnonymousRole().getId());
            p.setPermission(PermissionAPI.PERMISSION_EDIT);
            permissionAPI.save(p, st, user, true);

            p = new Permission();
            p.setInode(st.getPermissionId());
            p.setRoleId(roleAPI.loadCMSAnonymousRole().getId());
            p.setPermission(PermissionAPI.PERMISSION_PUBLISH);
            permissionAPI.save(p, st, user, true);

            List<WorkflowScheme> schemes = new ArrayList<>();
            schemes.add(ws);
            workflowAPI.saveSchemesForStruct(contentTypeSt, schemes);

            /*
		     * Create test content and set it up in scheme step
		     */
            contentlet1 = createContent("test5197-1", st);
            contentlet1 = contentletAPI.checkin(contentlet1, user, false);
            if (permissionAPI
                    .doesUserHavePermission(contentlet1, PermissionAPI.PERMISSION_PUBLISH, user)) {
                APILocator.getVersionableAPI().setLive(contentlet1);
            }

            /*
		     * Test that delete is not possible for step2
		     * while has associated step or content
		     */
            contentlet1.setStringProperty("wfActionId", action1.getId());
            contentlet1.setStringProperty("wfActionComments", "step1");
            contentlet1.setStringProperty("wfActionAssign", role.getId());
            workflowAPI.fireWorkflowNoCheckin(contentlet1, user);

            contentlet1.setStringProperty("wfActionId", action2.getId());
            contentlet1.setStringProperty("wfActionComments", "step2");
            contentlet1.setStringProperty("wfActionAssign", role.getId());
            workflowAPI.fireWorkflowNoCheckin(contentlet1, user);

            WorkflowStep currentStep = workflowAPI.findStepByContentlet(contentlet1);
            assertNotNull(currentStep);
            assertTrue(currentStep.getId().equals(step2.getId()));

		    /*
		     * Validate that step2 could not be deleted
		     */
            try {
                workflowAPI.deleteStep(step2, user).get();
            } catch (Exception e) {
			/*
			 * Should enter here with this exception
			 * </br> <b> Step : 'Publish' is being referenced by </b> </br></br> Step : 'Edit' ->  Action : 'Edit' </br></br>
			 */
            }
            assertTrue(UtilMethods.isSet(workflowAPI.findStep(step2.getId())));
		    /*
		     * Validate correct deletion of step1
		     */
            workflowAPI.deleteStep(step1, user).get();

		    /*
		     * Validate that the step 1 was deleted from the scheme
		     */
            steps = workflowAPI.findSteps(ws);
            assertTrue(steps.size() == 1);
            assertTrue(steps.get(0).getId().equals(step2.getId()));

		    /*
		     * Validate that step2 could not be deleted
		     */
            try {
                workflowAPI.deleteStep(step2, user).get();
            } catch (Exception e) {
			/*
			 * Should enter here with this exception
			 * </br> <b> Step : 'Publish' is being referenced by: X Contentlet(s) </br></br>
			 */
            }
            currentStep = workflowAPI.findStepByContentlet(contentlet1);
            assertNotNull(currentStep);
            assertTrue(currentStep.getId().equals(step2.getId()));

            /*
		     * Validate that step2 is not deleted
		     */
            steps = workflowAPI.findSteps(ws);
            assertTrue(steps.size() == 1);
            assertTrue(steps.get(0).getId().equals(step2.getId()));

        } finally {
            /*
		     * Clean test
		     */
            contentTypeAPI.delete(st);
            ws.setArchived(true);
            workflowAPI.saveScheme(ws, user);
            workflowAPI.deleteStep(step2, user).get();
            workflowAPI.deleteScheme(ws, user).get();
        }
    }

    /**
     * This test validate a contentlet workflow flow through different steps with different users
     * simulating the Document Management workflow
     */
    @Test
    public void validatingDocumentManagementWorkflow()
            throws DotDataException, IOException, DotSecurityException, AlreadyExistException, ExecutionException, InterruptedException {

        try {

            contentType4 = insertContentType(
                    "ValidatingDMWf" + UtilMethods.dateToHTMLDate(new Date(), DATE_FORMAT),
                    BaseContentType.CONTENT);

            Permission p = new Permission(contentType4.getPermissionId(), contributor.getId(),
                    editPermission, true);
            permissionAPI.save(p, contentType4, user, true);

            p = new Permission(Contentlet.class.getCanonicalName(), contentType4.getPermissionId(),
                    contributor.getId(), editPermission, true);
            permissionAPI.save(p, contentType4, user, true);

            p = new Permission(contentType4.getPermissionId(), publisher.getId(), publishPermission,
                    true);
            permissionAPI.save(p, contentType4, user, true);

            p = new Permission(Contentlet.class.getCanonicalName(), contentType4.getPermissionId(),
                    publisher.getId(), publishPermission, true);
            permissionAPI.save(p, contentType4, user, true);

            //get the Document Management workflow scheme
            workflowScheme6 = createDocumentManagentReplica(
                    DOCUMENT_MANAGEMENT_WORKFLOW_NAME + UtilMethods
                            .dateToHTMLDate(new Date(), DATE_FORMAT));

            final Set<String> schemes = new HashSet<>();
            schemes.add(workflowScheme6.getId());
            workflowAPI.saveSchemeIdsForContentType(contentType4, schemes);

            /*
             * Create test content and set it up in scheme step
		     */
            Contentlet contentlet1 = createContent("testDocumentManagement-1", contentType4);

            List<WorkflowAction> actions = workflowAPI
                    .findAvailableActions(contentlet1, joeContributor);
            if (actions.isEmpty() || actions.size() != 1) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!SAVE_AS_DRAFT_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            final WorkflowAction saveAsDraft = actions.get(0);

            //As Contributor - Save as Draft
            final ContentletRelationships contentletRelationships = APILocator.getContentletAPI()
                    .getAllRelationships(contentlet1);
            //Save as a draft
            contentlet1 = fireWorkflowAction(contentlet1, contentletRelationships, saveAsDraft,
                    StringPool.BLANK, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet1).getName()));

            //As Contributor - Send for Review
            actions = workflowAPI.findAvailableActions(contentlet1, joeContributor);
            if (actions.isEmpty() || actions.size() != 1) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }

            if (!SEND_FOR_REVIEW_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            final WorkflowAction sendForReview = actions.get(0);
            //Send For Review
            contentlet1 = fireWorkflowAction(contentlet1, contentletRelationships, sendForReview,
                    StringPool.BLANK, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, REVIEW_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet1).getName()));

            //As Reviewer - return for editing
            actions = workflowAPI.findAvailableActions(contentlet1, janeReviewer);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!RETURN_FOR_EDITS_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            if (!SEND_TO_LEGAL_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            final WorkflowAction returnForEdits = actions.get(0);
            //Return for Edit
            contentlet1 = fireWorkflowAction(contentlet1, contentletRelationships, returnForEdits,
                    StringPool.BLANK, contributor.getId(), janeReviewer);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet1).getName()));
            // As contributor. lock for editing
            contentletAPI.lock(contentlet1, joeContributor, false);

            //As Contributor - save as Draft
            actions = workflowAPI.findAvailableActions(contentlet1, joeContributor);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!SAVE_AS_DRAFT_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            if (!SEND_FOR_REVIEW_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            final String title2 = contentlet1.getStringProperty(FIELD_VAR_NAME) + "-2";
            contentlet1.setStringProperty(FIELD_VAR_NAME, title2);

            //Save as draft
            contentlet1 = fireWorkflowAction(contentlet1, contentletRelationships, saveAsDraft,
                    StringPool.BLANK, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet1).getName()));

            //As Contributor - Send for Review
            actions = workflowAPI.findAvailableActions(contentlet1, joeContributor);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!SAVE_AS_DRAFT_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            if (!SEND_FOR_REVIEW_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            //Send For Review
            contentlet1.setIndexPolicy(IndexPolicy.FORCE);
            contentlet1 = fireWorkflowAction(contentlet1, contentletRelationships, sendForReview,
                    StringPool.BLANK, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, REVIEW_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet1).getName()));

            //As reviewer - send to legal
            actions = workflowAPI.findAvailableActions(contentlet1, janeReviewer);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!RETURN_FOR_EDITS_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            if (!SEND_TO_LEGAL_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            final WorkflowAction sendToLegal = actions.get(1);

            //Send to Legal
            contentlet1 = fireWorkflowAction(contentlet1, contentletRelationships, sendToLegal,
                    StringPool.BLANK, StringPool.BLANK, janeReviewer);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, LEGAL_APPROVAL_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet1).getName()));

            //As Publisher - publish
            actions = workflowAPI.findAvailableActions(contentlet1, chrisPublisher);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!RETURN_FOR_EDITS_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            if (!PUBLISH_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            final WorkflowAction publish = actions.get(1);

            //Publish
            contentlet1 = fireWorkflowAction(contentlet1, contentletRelationships, publish,
                    StringPool.BLANK, StringPool.BLANK, chrisPublisher);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, PUBLISHED_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet1).getName()));
            assertTrue(title2.equals(contentlet1.getStringProperty(FIELD_VAR_NAME)));
            assertTrue(contentlet1.isLive());

        } finally {
            /*
             * Clean test
		     */
            //delete content type
            contentTypeAPI.delete(contentType4);

            //Deleting workflow 6
            workflowAPI.archive(workflowScheme6, user);
            workflowAPI.deleteScheme(workflowScheme6, user).get();
        }
    }

    /**
     * Test the deleteScheme method
     */
    @Test
    public void deleteScheme()
            throws DotDataException, IOException, DotSecurityException, AlreadyExistException {

        try {

            final String comment1 = "please review";
            final String comment2 = "please fix this text";
            final String comment3 = "please review again";
            final String comment4 = "Ready to publish";

            final int editPermission =
                    PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_EDIT;
            final int publishPermission = editPermission + PermissionAPI.PERMISSION_PUBLISH;

            contentType5 = insertContentType(
                    "DeleteWf" + UtilMethods.dateToHTMLDate(new Date(), DATE_FORMAT),
                    BaseContentType.CONTENT);

            Permission p = new Permission(contentType5.getPermissionId(), contributor.getId(),
                    editPermission, true);
            permissionAPI.save(p, contentType5, user, true);

            p = new Permission(Contentlet.class.getCanonicalName(), contentType5.getPermissionId(),
                    contributor.getId(), editPermission, true);
            permissionAPI.save(p, contentType5, user, true);

            p = new Permission(contentType5.getPermissionId(), publisher.getId(), publishPermission,
                    true);
            permissionAPI.save(p, contentType5, user, true);

            p = new Permission(Contentlet.class.getCanonicalName(), contentType5.getPermissionId(),
                    publisher.getId(), publishPermission, true);
            permissionAPI.save(p, contentType5, user, true);

            //get the Document Management workflow scheme
            workflowScheme7 = createDocumentManagentReplica(
                    DOCUMENT_MANAGEMENT_WORKFLOW_NAME + UtilMethods
                            .dateToHTMLDate(new Date(), DATE_FORMAT));

            final Set<String> schemes = new HashSet<>();
            schemes.add(workflowScheme7.getId());
            workflowAPI.saveSchemeIdsForContentType(contentType5, schemes);

        /*
         * Create test contents and set it up in scheme steps
		 */
            //Contentlet1 on published step
            Contentlet contentlet1 = createContent("testDeleteWf-1", contentType5);

            List<WorkflowAction> actions = workflowAPI
                    .findAvailableActions(contentlet1, joeContributor);
            if (actions.isEmpty() || actions.size() != 1) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!SAVE_AS_DRAFT_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            final WorkflowAction saveAsDraft = actions.get(0);

            //As Contributor - Save as Draft
            final ContentletRelationships contentletRelationships = APILocator.getContentletAPI()
                    .getAllRelationships(contentlet1);
            //save as Draft
            contentlet1 = fireWorkflowAction(contentlet1, contentletRelationships, saveAsDraft,
                    StringPool.BLANK, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet1).getName()));

            //As Contributor - Send for Review
            actions = workflowAPI.findAvailableActions(contentlet1, joeContributor);
            if (actions.isEmpty() || actions.size() != 1) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }

            if (!SEND_FOR_REVIEW_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            final WorkflowAction sendForReview = actions.get(0);
            //Send for Review
            contentlet1 = fireWorkflowAction(contentlet1, contentletRelationships, sendForReview,
                    comment1, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, REVIEW_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet1).getName()));

            //As Reviewer - return for editing
            actions = workflowAPI.findAvailableActions(contentlet1, janeReviewer);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!RETURN_FOR_EDITS_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            if (!SEND_TO_LEGAL_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            final WorkflowAction returnForEdits = actions.get(0);

            //Return for edit
            contentlet1 = fireWorkflowAction(contentlet1, contentletRelationships, returnForEdits,
                    comment2, contributor.getId(), janeReviewer);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet1).getName()));
            // As contributor. lock for editing
            contentletAPI.lock(contentlet1, joeContributor, false);

            //As Contributor - save as Draft
            actions = workflowAPI.findAvailableActions(contentlet1, joeContributor);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!SAVE_AS_DRAFT_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            if (!SEND_FOR_REVIEW_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            final String title2 = contentlet1.getStringProperty(FIELD_VAR_NAME) + "-2";
            contentlet1.setStringProperty(FIELD_VAR_NAME, title2);

            //Save as a draft
            contentlet1 = fireWorkflowAction(contentlet1, contentletRelationships, saveAsDraft,
                    StringPool.BLANK, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet1).getName()));

            //As Contributor - Send for Review
            actions = workflowAPI.findAvailableActions(contentlet1, joeContributor);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!SAVE_AS_DRAFT_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            if (!SEND_FOR_REVIEW_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            //Send For Review
            contentlet1 = fireWorkflowAction(contentlet1, contentletRelationships, sendForReview,
                    comment3, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, REVIEW_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet1).getName()));

            //As reviewer - send to legal
            actions = workflowAPI.findAvailableActions(contentlet1, janeReviewer);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!RETURN_FOR_EDITS_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            if (!SEND_TO_LEGAL_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            final WorkflowAction sendToLegal = actions.get(1);
            //Send to Legal
            contentlet1 = fireWorkflowAction(contentlet1, contentletRelationships, sendToLegal,
                    comment4, StringPool.BLANK, janeReviewer);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, LEGAL_APPROVAL_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet1).getName()));

            //As Publisher - publish
            actions = workflowAPI.findAvailableActions(contentlet1, chrisPublisher);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!RETURN_FOR_EDITS_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            if (!PUBLISH_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            final WorkflowAction publish = actions.get(1);

            //publish
            contentlet1 = fireWorkflowAction(contentlet1, contentletRelationships, publish,
                    StringPool.BLANK, StringPool.BLANK, chrisPublisher);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, PUBLISHED_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet1).getName()));
            assertTrue(title2.equals(contentlet1.getStringProperty(FIELD_VAR_NAME)));
            assertTrue(contentlet1.isLive());

            //Content2 on Legal Approval
            Contentlet contentlet2 = createContent("testDeleteWf-2", contentType5);

            actions = workflowAPI
                    .findAvailableActions(contentlet2, joeContributor);
            if (actions.isEmpty() || actions.size() != 1) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!SAVE_AS_DRAFT_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            //As Contributor - Save as Draft
            final ContentletRelationships contentletRelationships2 = APILocator.getContentletAPI()
                    .getAllRelationships(contentlet2);
            //Save as a draft
            contentlet2 = fireWorkflowAction(contentlet2, contentletRelationships2, saveAsDraft,
                    StringPool.BLANK, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet2).getName()));

            //As Contributor - Send for Review
            actions = workflowAPI.findAvailableActions(contentlet2, joeContributor);
            if (actions.isEmpty() || actions.size() != 1) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }

            if (!SEND_FOR_REVIEW_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            //Send For Review
            contentlet2 = fireWorkflowAction(contentlet2, contentletRelationships2, sendForReview,
                    comment1, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, REVIEW_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet2).getName()));

            //As Reviewer - return for editing
            actions = workflowAPI.findAvailableActions(contentlet2, janeReviewer);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!RETURN_FOR_EDITS_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            if (!SEND_TO_LEGAL_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            //return For Edits
            contentlet2 = fireWorkflowAction(contentlet2, contentletRelationships2, returnForEdits,
                    comment2, contributor.getId(), janeReviewer);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet2).getName()));
            // As contributor. lock for editing
            contentletAPI.lock(contentlet2, joeContributor, false);

            //As Contributor - save as Draft
            actions = workflowAPI.findAvailableActions(contentlet2, joeContributor);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!SAVE_AS_DRAFT_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            if (!SEND_FOR_REVIEW_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            //save as draft
            contentlet2 = fireWorkflowAction(contentlet2, contentletRelationships2, saveAsDraft,
                    comment3, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet2).getName()));

            //As Contributor - Send for Review
            actions = workflowAPI.findAvailableActions(contentlet2, joeContributor);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!SAVE_AS_DRAFT_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            if (!SEND_FOR_REVIEW_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            //send for review
            contentlet2 = fireWorkflowAction(contentlet2, contentletRelationships2, sendForReview,
                    comment4, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, REVIEW_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet2).getName()));

            //As reviewer - send to legal
            actions = workflowAPI.findAvailableActions(contentlet2, janeReviewer);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!RETURN_FOR_EDITS_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            if (!SEND_TO_LEGAL_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            //send to legal
            contentlet2 = fireWorkflowAction(contentlet2, contentletRelationships2, sendToLegal,
                    comment4, StringPool.BLANK, janeReviewer);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, LEGAL_APPROVAL_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet2).getName()));

            //As Publisher - publish
            actions = workflowAPI.findAvailableActions(contentlet2, chrisPublisher);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!RETURN_FOR_EDITS_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }
            if (!PUBLISH_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            //Content 3 in Review Step
            Contentlet contentlet3 = createContent("testDeleteWf-3", contentType5);

            actions = workflowAPI
                    .findAvailableActions(contentlet3, joeContributor);
            if (actions.isEmpty() || actions.size() != 1) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!SAVE_AS_DRAFT_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            //As Contributor - Save as Draft
            final ContentletRelationships contentletRelationships3 = APILocator.getContentletAPI()
                    .getAllRelationships(contentlet3);
            //Save as a draft
            contentlet3 = fireWorkflowAction(contentlet3, contentletRelationships3, saveAsDraft,
                    StringPool.BLANK, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet3).getName()));

            //As Contributor - Send for Review
            actions = workflowAPI.findAvailableActions(contentlet3, joeContributor);
            if (actions.isEmpty() || actions.size() != 1) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }

            if (!SEND_FOR_REVIEW_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            //Send For Review
            contentlet3 = fireWorkflowAction(contentlet3, contentletRelationships3, sendForReview,
                    comment1, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, REVIEW_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet3).getName()));

            //Content4 on Editing
            Contentlet contentlet4 = createContent("testDeleteWf-4", contentType5);

            actions = workflowAPI
                    .findAvailableActions(contentlet4, joeContributor);
            if (actions.isEmpty() || actions.size() != 1) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!SAVE_AS_DRAFT_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            //As Contributor - Save as Draft
            final ContentletRelationships contentletRelationships4 = APILocator.getContentletAPI()
                    .getAllRelationships(contentlet4);
            contentlet4 = fireWorkflowAction(contentlet4, contentletRelationships4, saveAsDraft,
                    StringPool.BLANK, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet4).getName()));

            //As Contributor - Send for Review
            actions = workflowAPI.findAvailableActions(contentlet4, joeContributor);
            if (actions.isEmpty() || actions.size() != 1) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }

            if (!SEND_FOR_REVIEW_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            //Send for review
            contentlet4 = fireWorkflowAction(contentlet4, contentletRelationships4, sendForReview,
                    comment1, StringPool.BLANK, joeContributor);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, REVIEW_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet4).getName()));

            //As Reviewer - return for editing
            actions = workflowAPI.findAvailableActions(contentlet4, janeReviewer);
            if (actions.isEmpty() || actions.size() != 2) {
                assertTrue(INCORRECT_NUMBER_OF_ACTIONS_MESSAGE, false);
            }
            if (!RETURN_FOR_EDITS_ACTION_NAME.equals(actions.get(0).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            if (!SEND_TO_LEGAL_ACTION_NAME.equals(actions.get(1).getName())) {
                assertTrue(WRONG_ACTION_AVAILABLE_MESSAGE, false);
            }

            //return fo Edits
            contentlet4 = fireWorkflowAction(contentlet4, contentletRelationships4, returnForEdits,
                    comment2, contributor.getId(), janeReviewer);

            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(workflowAPI.findStepByContentlet(contentlet4).getName()));

            //Validate workflow tasks status
            WorkflowTask task1 = workflowAPI.findTaskByContentlet(contentlet1);
            assertNotNull(task1);

            List<WorkflowComment> comments1 = workflowAPI.findWorkFlowComments(task1);
            assertNotNull(comments1);
            assertTrue(comments1.size() == 4);

            List<WorkflowHistory> histories1 = workflowAPI.findWorkflowHistory(task1);
            assertNotNull(histories1);
            assertEquals(7, histories1.size());

            WorkflowTask task2 = workflowAPI.findTaskByContentlet(contentlet2);
            assertNotNull(task2);

            List<WorkflowComment> comments2 = workflowAPI.findWorkFlowComments(task2);
            assertNotNull(comments2);
            assertTrue(comments2.size() == 5);

            List<WorkflowHistory> histories2 = workflowAPI.findWorkflowHistory(task2);
            assertNotNull(histories2);
            assertEquals(6, histories2.size());

            WorkflowTask task3 = workflowAPI.findTaskByContentlet(contentlet3);
            assertNotNull(task3);
            List<WorkflowComment> comments3 = workflowAPI.findWorkFlowComments(task3);
            assertNotNull(comments3);
            assertTrue(comments3.size() == 1);

            List<WorkflowHistory> histories3 = workflowAPI.findWorkflowHistory(task3);
            assertNotNull(histories3);
            assertEquals(2, histories3.size());

            WorkflowTask task4 = workflowAPI.findTaskByContentlet(contentlet4);
            assertNotNull(task4);
            List<WorkflowComment> comments4 = workflowAPI.findWorkFlowComments(task4);
            assertNotNull(comments4);
            assertTrue(comments4.size() == 2);

            List<WorkflowHistory> histories4 = workflowAPI.findWorkflowHistory(task4);
            assertNotNull(histories4);
            assertEquals(3, histories4.size());

            //Test the delete
            //Deleting workflow 7
            workflowAPI.archive(workflowScheme7, user);

            try {
                Future<WorkflowScheme> result = workflowAPI.deleteScheme(workflowScheme7, user);
                result.get();
            } catch (InterruptedException | ExecutionException e) {
                assertTrue(e.getMessage(), false);
            }

            //validate actions deleted
            assertTrue(ACTIONS_LIST_SHOULD_BE_EMPY,
                    workflowAPI.findActions(workflowScheme7, user).isEmpty());

            //validate steps deleted
            assertTrue(STEPS_LIST_SHOULD_BE_EMPTY,
                    workflowAPI.findSteps(workflowScheme7).isEmpty());

            try {
                //validate scheme deleted
                workflowScheme7 = workflowAPI.findScheme(workflowScheme7.getId());
                assertTrue(SCHEME_SHOULDNT_EXIST, false);
            } catch (DoesNotExistException e) {
                assertTrue(true);
            }

            //validate workflow comments deleted
            assertTrue(workflowAPI.findWorkFlowComments(task1).size() == 0);
            assertTrue(workflowAPI.findWorkFlowComments(task2).size() == 0);
            assertTrue(workflowAPI.findWorkFlowComments(task3).size() == 0);
            assertTrue(workflowAPI.findWorkFlowComments(task4).size() == 0);

            //validate workflow history deleted
            assertTrue(workflowAPI.findWorkflowHistory(task1).size() == 0);
            assertTrue(workflowAPI.findWorkflowHistory(task2).size() == 0);
            assertTrue(workflowAPI.findWorkflowHistory(task3).size() == 0);
            assertTrue(workflowAPI.findWorkflowHistory(task4).size() == 0);

            //validate workflow tasks deleted
            task1 = workflowAPI.findTaskByContentlet(contentlet1);
            assertNull(task1);

            task2 = workflowAPI.findTaskByContentlet(contentlet2);
            assertNull(task2);

            task3 = workflowAPI.findTaskByContentlet(contentlet3);
            assertNull(task3);

            task4 = workflowAPI.findTaskByContentlet(contentlet4);
            assertNull(task4);

        } finally {
            //clean test
            //delete content type
            contentTypeAPI.delete(contentType5);
        }
    }

    /**
     * This test validate that a content type could be created without any workflow scheme
     * associated if there is a license applied.
     */
    @Test
    public void validatingNoObligatorieContentTypeWorkflow()
            throws DotDataException, IOException, DotSecurityException, AlreadyExistException {

        ContentType contentType6 = null;
        try {
            contentType6 = insertContentType(
                    "NoObligatoryWf" + UtilMethods.dateToHTMLDate(new Date(), DATE_FORMAT),
                    BaseContentType.CONTENT);
            final int editPermission =
                    PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_EDIT;

            Permission p = new Permission(contentType6.getPermissionId(), contributor.getId(),
                    editPermission, true);
            permissionAPI.save(p, contentType6, user, true);

            p = new Permission(Contentlet.class.getCanonicalName(), contentType6.getPermissionId(),
                    contributor.getId(), editPermission, true);
            permissionAPI.save(p, contentType6, user, true);

            final List<WorkflowScheme> results = workflowAPI
                    .findSchemesForContentType(contentType6);
            assertNotNull(results);
            assertTrue(results.isEmpty());
        } finally {
            //clean test
            //delete content type
            contentTypeAPI.delete(contentType6);
        }
    }

    /**
     * Validate that when a Content type is modified associating and/or removing workflows schemes
     * then the existing workflow tasks associated to the scheme keep the current status and the
     * removed ones are set on null
     */
    @Test
    public void saveScheme_keepExistingContentWorkflowTaskStatus_IfWorkflowSchemeRemainsAssociated()
            throws DotDataException, DotSecurityException, AlreadyExistException, ExecutionException, InterruptedException {
        WorkflowScheme workflowSchemeA = null;
        WorkflowScheme workflowSchemeB = null;
        ContentType keepWfTaskStatusContentType = null;
        Contentlet keepWfTaskStatusContentlet = null;
        try {

            //Create testing content type
            keepWfTaskStatusContentType = generateContentTypeAndAssignPermissions(
                    "KeepWfTaskStatus",
                    BaseContentType.CONTENT, editPermission, contributor.getId());

            // Create testing workflows
            workflowSchemeA = createDocumentManagentReplica(
                    DOCUMENT_MANAGEMENT_WORKFLOW_NAME + "_1_" + UtilMethods
                            .dateToHTMLDate(new Date(), DATE_FORMAT));

            workflowSchemeB = createDocumentManagentReplica(
                    DOCUMENT_MANAGEMENT_WORKFLOW_NAME + "_2_" + UtilMethods
                            .dateToHTMLDate(new Date(), DATE_FORMAT));

            final Set<String> schemeIds = new HashSet<>();
            schemeIds.add(workflowSchemeA.getId());

            workflowAPI.saveSchemeIdsForContentType(keepWfTaskStatusContentType, schemeIds);

            //Add Workflow Task
            //Contentlet1 on published step
            keepWfTaskStatusContentlet = createContent("testKeepWfTaskStatus",
                    keepWfTaskStatusContentType);

            List<WorkflowAction> actions = workflowAPI
                    .findAvailableActions(keepWfTaskStatusContentlet, joeContributor);
            final WorkflowAction saveAsDraft = actions.get(0);

            //As Contributor - Save as Draft
            final ContentletRelationships contentletRelationships = APILocator.getContentletAPI()
                    .getAllRelationships(keepWfTaskStatusContentlet);
            //save as Draft
            keepWfTaskStatusContentlet = fireWorkflowAction(keepWfTaskStatusContentlet,
                    contentletRelationships, saveAsDraft,
                    StringPool.BLANK, StringPool.BLANK, joeContributor);

            //validate workflow tasks deleted
            WorkflowStep editingStep = workflowAPI.findSteps(workflowSchemeA).get(0);
            WorkflowStep step = workflowAPI.findStepByContentlet(keepWfTaskStatusContentlet);
            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(step.getName()) && editingStep.getId().equals(step.getId()));

            WorkflowTask task1 = workflowAPI.findTaskByContentlet(keepWfTaskStatusContentlet);
            assertNotNull(task1.getId());
            assertNotNull(TASK_STATUS_SHOULD_NOT_BE_NULL, task1.getStatus());
            assertTrue(INCORRECT_TASK_STATUS, editingStep.getId().equals(task1.getStatus()));

            //Add a new Scheme to content type
            schemeIds.add(workflowSchemeB.getId());
            workflowAPI.saveSchemeIdsForContentType(keepWfTaskStatusContentType, schemeIds);

            //Validate that the contentlet Workflow task keeps the original value
            step = workflowAPI.findStepByContentlet(keepWfTaskStatusContentlet);
            assertTrue(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(step.getName()) && editingStep.getId().equals(step.getId()));

            task1 = workflowAPI.findTaskByContentlet(keepWfTaskStatusContentlet);
            assertNotNull(task1.getId());
            assertNotNull(TASK_STATUS_SHOULD_NOT_BE_NULL, task1.getStatus());
            assertTrue(INCORRECT_TASK_STATUS, editingStep.getId().equals(task1.getStatus()));

            //remove an existing Scheme with workflow task associated to the content type
            schemeIds.remove(workflowSchemeA.getId());
            workflowAPI.saveSchemeIdsForContentType(keepWfTaskStatusContentType, schemeIds);

            //Validate that the contentlet Workflow task lost the original value
            step = workflowAPI.findStepByContentlet(keepWfTaskStatusContentlet);
            assertNotNull(CONTENTLET_IS_NOT_ON_STEP, step);
            assertFalse(CONTENTLET_ON_WRONG_STEP_MESSAGE, EDITING_STEP_NAME
                    .equals(step.getName()) && editingStep.getId().equals(step.getId()));

            task1 = workflowAPI.findTaskByContentlet(keepWfTaskStatusContentlet);
            assertNotNull(task1.getId());
            assertNull(TASK_STATUS_SHOULD_BE_NULL, task1.getStatus());

        } finally {
            //clean test
            //delete content type
            contentTypeAPI.delete(keepWfTaskStatusContentType);

            workflowAPI.archive(workflowSchemeA, user);
            workflowAPI.deleteScheme(workflowSchemeA, user).get();

            workflowAPI.archive(workflowSchemeB, user);
            workflowAPI.deleteScheme(workflowSchemeB, user).get();
        }
    }

    /**
     * Validate that when a Content type is modified associating and/or removing workflows schemes
     * then the values are updated correctly in cache to avoid extra calls to the DB
     */
    @Test
    public void findSchemesForContenttype_validateIfSchemesResultsAreOnCache()
            throws DotDataException, DotSecurityException, AlreadyExistException, ExecutionException, InterruptedException {
        WorkflowScheme workflowSchemeC = null;
        WorkflowScheme workflowSchemeD = null;
        ContentType contentType = null;
        try {

            contentType = generateContentTypeAndAssignPermissions("KeepWfTaskStatus",
                    BaseContentType.CONTENT, editPermission, contributor.getId());

            // Create testing workflows
            workflowSchemeC = createDocumentManagentReplica(
                    DOCUMENT_MANAGEMENT_WORKFLOW_NAME + "_3_" + UtilMethods
                            .dateToHTMLDate(new Date(), DATE_FORMAT));

            workflowSchemeD = createDocumentManagentReplica(
                    DOCUMENT_MANAGEMENT_WORKFLOW_NAME + "_4_" + UtilMethods
                            .dateToHTMLDate(new Date(), DATE_FORMAT));

            List<WorkflowScheme> schemesInCache = new ArrayList<>();

            //0. Test with no schemes associated
            schemesInCache = workflowCache.getSchemesByStruct(contentType.id());
            assertNull(WORKFLOW_SCHEME_CACHE_SHOULD_BE_NULL, schemesInCache);

            //search for schemes
            List<WorkflowScheme> schemes = workflowAPI.findSchemesForContentType(contentType);
            assertTrue(WORKFLOW_SCHEME_LIST_WITH_WRONG_SIZE, schemes.size() == 0);

            //validate cache values
            schemesInCache = workflowCache.getSchemesByStruct(contentType.id());
            assertNotNull(WORKFLOW_SCHEME_CACHE_SHOULD_NOT_BE_NULL, schemesInCache);
            assertTrue(WORKFLOW_SCHEME_CACHE_WITH_WRONG_SIZE, schemesInCache.size() == 0);

            //1. Test Adding one scheme
            final Set<String> schemeIds = new HashSet<>();
            schemeIds.add(workflowSchemeC.getId());
            workflowAPI.saveSchemeIdsForContentType(contentType, schemeIds);

            //validate cache values
            schemesInCache = workflowCache.getSchemesByStruct(contentType.id());
            assertNull(WORKFLOW_SCHEME_CACHE_SHOULD_BE_NULL, schemesInCache);

            //search for schemes
            schemes = workflowAPI.findSchemesForContentType(contentType);
            assertTrue(WORKFLOW_SCHEME_LIST_WITH_WRONG_SIZE, schemes.size() == 1);

            //validate cache values
            schemesInCache = workflowCache.getSchemesByStruct(contentType.id());
            assertNotNull(WORKFLOW_SCHEME_CACHE_SHOULD_NOT_BE_NULL, schemesInCache);
            assertTrue(WORKFLOW_SCHEME_CACHE_WITH_WRONG_SIZE, schemesInCache.size() == 1);

            //2. Test adding a second scheme
            schemeIds.add(workflowSchemeD.getId());
            workflowAPI.saveSchemeIdsForContentType(contentType, schemeIds);

            //validate cache values
            schemesInCache = workflowCache.getSchemesByStruct(contentType.id());
            assertNull(WORKFLOW_SCHEME_CACHE_SHOULD_BE_NULL, schemesInCache);

            //search for schemes
            schemes = workflowAPI.findSchemesForContentType(contentType);
            assertTrue(WORKFLOW_SCHEME_LIST_WITH_WRONG_SIZE, schemes.size() == 2);

            //validate cache values
            schemesInCache = workflowCache.getSchemesByStruct(contentType.id());
            assertNotNull(WORKFLOW_SCHEME_CACHE_SHOULD_NOT_BE_NULL, schemesInCache);
            assertTrue(WORKFLOW_SCHEME_CACHE_WITH_WRONG_SIZE, schemesInCache.size() == 2);

            //3. Test removing one scheme
            schemeIds.remove(workflowSchemeC.getId());
            workflowAPI.saveSchemeIdsForContentType(contentType, schemeIds);

            //validate cache values
            schemesInCache = workflowCache.getSchemesByStruct(contentType.id());
            assertNull(WORKFLOW_SCHEME_CACHE_SHOULD_BE_NULL, schemesInCache);

            //search for schemes
            schemes = workflowAPI.findSchemesForContentType(contentType);
            assertTrue(WORKFLOW_SCHEME_LIST_WITH_WRONG_SIZE, schemes.size() == 1);

            //validate cache values
            schemesInCache = workflowCache.getSchemesByStruct(contentType.id());
            assertNotNull(WORKFLOW_SCHEME_CACHE_SHOULD_NOT_BE_NULL, schemesInCache);
            assertTrue(WORKFLOW_SCHEME_CACHE_WITH_WRONG_SIZE, schemesInCache.size() == 1);

            //4. test removing all schemes
            schemeIds.remove(workflowSchemeD.getId());
            workflowAPI.saveSchemeIdsForContentType(contentType, schemeIds);

            //validate cache values
            schemesInCache = workflowCache.getSchemesByStruct(contentType.id());
            assertNull(WORKFLOW_SCHEME_CACHE_SHOULD_BE_NULL, schemesInCache);

            //search for schemes
            schemes = workflowAPI.findSchemesForContentType(contentType);
            assertTrue(WORKFLOW_SCHEME_LIST_WITH_WRONG_SIZE, schemes.size() == 0);

            //validate cache values
            schemesInCache = workflowCache.getSchemesByStruct(contentType.id());
            assertNotNull(WORKFLOW_SCHEME_CACHE_SHOULD_NOT_BE_NULL, schemesInCache);
            assertTrue(WORKFLOW_SCHEME_CACHE_WITH_WRONG_SIZE, schemesInCache.size() == 0);

        } finally {
            //clean test
            //delete content type
            contentTypeAPI.delete(contentType);

            workflowAPI.archive(workflowSchemeC, user);
            workflowAPI.deleteScheme(workflowSchemeC, user).get();

            workflowAPI.archive(workflowSchemeD, user);
            workflowAPI.deleteScheme(workflowSchemeD, user).get();
        }
    }

    /**
     * Validate that the findStepsByContentlet method is saving in cache the workflow steps to avoid
     * extra calls to the DB
     */
    @Test
    public void findStepsByContentlet_validateIfStepsResultsAreOnCache()
            throws DotDataException, DotSecurityException, AlreadyExistException, ExecutionException, InterruptedException {
        WorkflowScheme workflowScheme = null;
        ContentType contentType = null;
        try {

            contentType = generateContentTypeAndAssignPermissions("KeepWfTaskStatus",
                    BaseContentType.CONTENT, editPermission, contributor.getId());

            // Create testing workflows
            workflowScheme = createDocumentManagentReplica(
                    DOCUMENT_MANAGEMENT_WORKFLOW_NAME + "_5_" + UtilMethods
                            .dateToHTMLDate(new Date(), DATE_FORMAT));

            final Set<String> schemeIds = new HashSet<>();
            schemeIds.add(workflowScheme.getId());
            workflowAPI.saveSchemeIdsForContentType(contentType, schemeIds);

            //Add Workflow Task
            //Contentlet1 on published step
            Contentlet contentlet = createContent("testCacheFindStepsByContentlet", contentType);

            List<WorkflowAction> actions = workflowAPI
                    .findAvailableActions(contentlet, joeContributor);
            final WorkflowAction saveAsDraft = actions.get(0);

            //As Contributor - Save as Draft
            final ContentletRelationships contentletRelationships = APILocator.getContentletAPI()
                    .getAllRelationships(contentlet);
            //save as Draft
            contentlet = fireWorkflowAction(contentlet, contentletRelationships, saveAsDraft,
                    StringPool.BLANK, StringPool.BLANK, joeContributor);

            //Validating cache
            List<WorkflowStep> stepsInCacheList = workflowCache.getSteps(contentlet);
            assertNull(WORKFLOW_STEPS_CACHE_SHOULD_BE_NULL, stepsInCacheList);

            //Search for steps
            List<WorkflowStep> steps = workflowAPI.findStepsByContentlet(contentlet);
            assertTrue(WORKFLOW_STEPS_LIST_WITH_WRONG_SIZE, steps.size() == 1);

            //validate steps in cache
            stepsInCacheList = workflowCache.getSteps(contentlet);
            assertNotNull(WORKFLOW_STEPS_CACHE_SHOULD_NOT_BE_NULL, stepsInCacheList);
            assertTrue(WORKFLOW_STEPS_CACHE_WITH_WRONG_SIZE, stepsInCacheList.size() == 1);

        } finally {
            //clean test
            //delete content type
            contentTypeAPI.delete(contentType);

            workflowScheme.setArchived(true);
            workflowAPI.saveScheme(workflowScheme, user);
            workflowAPI.deleteScheme(workflowScheme, user).get();
        }
    }

    /**
     * Test the archive workflow method
     */
    @Test
    public void archive_success_whenWorkflowIsArchived()
            throws DotDataException, DotSecurityException, AlreadyExistException, ExecutionException, InterruptedException {
        WorkflowScheme workflowScheme = null;
        try {

            workflowScheme = createDocumentManagentReplica(
                    DOCUMENT_MANAGEMENT_WORKFLOW_NAME + "_6_" + UtilMethods
                            .dateToHTMLDate(new Date(), DATE_FORMAT));

            assertFalse(workflowScheme.isArchived());

            //archive workflow
            workflowAPI.archive(workflowScheme, user);

            assertTrue(workflowScheme.isArchived());

        } finally {
            workflowAPI.deleteScheme(workflowScheme, user).get();
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
                .defaultType(false)
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
    protected static WorkflowScheme addWorkflowScheme(final String schemeName)
            throws DotDataException, DotSecurityException {
        WorkflowScheme scheme = null;
        try {
            scheme = new WorkflowScheme();
            scheme.setName(schemeName);
            scheme.setDescription("testing workflows " + schemeName);
            scheme.setCreationDate(new Date());
            workflowAPI.saveScheme(scheme, APILocator.systemUser());
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
            workflowAPI.saveStep(step, user);
        } catch (AlreadyExistException e) {
            //scheme already exist
        }
        return step;
    }

    protected static WorkflowAction addWorkflowAction(final String name, final int order,
                                                      final String nextStep, final String stepId, final Role whoCanUse,
                                                      final String schemeId)
            throws DotDataException {
        return addWorkflowAction(name, order, nextStep, stepId, whoCanUse, schemeId, new HashMap<>());
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
            final String nextStep, final String stepId, final Role whoCanUse,
            final String schemeId, final Map<String, Object> metadata)
            throws DotDataException {

        WorkflowAction action = null;
        try {
            action = new WorkflowAction();
            action.setName(name);
            action.setSchemeId(schemeId);
            action.setOwner(whoCanUse.getId());
            action.setOrder(order);
            action.setNextStep(nextStep);
            action.setRequiresCheckout(false);
            action.setStepId(stepId);
            action.setNextAssign(whoCanUse.getId());
            action.setCommentable(true);
            action.setAssignable(false);
            action.setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED, WorkflowState.NEW,
                    WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.ARCHIVED, WorkflowState.EDITING);
            action.setMetadata(metadata);
            workflowAPI.saveAction(action,
                    Arrays.asList(new Permission[]{
                            new Permission(action.getId(),
                                    whoCanUse.getId(),
                                    PermissionAPI.PERMISSION_USE)}),
                    APILocator.systemUser());

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
            workflowAPI.saveActionClass(actionClass, APILocator.systemUser());
        } catch (AlreadyExistException e) {
            //scheme already exist
        }
        return actionClass;
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup()
            throws DotDataException, DotSecurityException, InterruptedException, ExecutionException, AlreadyExistException {

        if (null != contentType) {
            contentTypeAPI.delete(contentType);
        }

        if (null != contentType2) {
            contentTypeAPI.delete(contentType2);
        }

        if (null != contentType3) {
            contentTypeAPI.delete(contentType3);
        }

        //Deleting workflow 1
        if (null != workflowScheme1) {
            workflowAPI.archive(workflowScheme1, user);
            workflowAPI.deleteScheme(workflowScheme1, user).get();
        }

        //Deleting workflow 2
        if (null != workflowScheme2) {
            workflowAPI.archive(workflowScheme2, user);
            workflowAPI.deleteScheme(workflowScheme2, user).get();
        }

        //Deleting workflow 3
        if (null != workflowScheme3) {
            workflowAPI.archive(workflowScheme3, user);
            workflowAPI.deleteScheme(workflowScheme3, user).get();
        }

        //Deleting workflow 4
        if (null != workflowScheme4) {
            workflowAPI.archive(workflowScheme4, user);
            workflowAPI.deleteScheme(workflowScheme4, user).get();
        }

        //Deleting workflow 5
        if (null != workflowScheme5) {
            workflowAPI.archive(workflowScheme5, user);
            workflowAPI.deleteScheme(workflowScheme5, user).get();
        }
    }

    /**
     * This method generate a replica of the current Document Management Workflow
     *
     * @param newWorkflowScheme The new for the workflow replica
     * @return The new workflow
     */
    public static WorkflowScheme createDocumentManagentReplica(final String newWorkflowScheme)
            throws DotDataException, DotSecurityException {

        final WorkflowScheme scheme = addWorkflowScheme(newWorkflowScheme);
        final Role adminRole = roleAPI.loadCMSAdminRole();
        final Role anonymus = roleAPI.loadCMSAnonymousRole();
        //Create Steps

        //Editing Step
        final WorkflowStep editingStep = addWorkflowStep(EDITING_STEP_NAME, 0, Boolean.FALSE,
                Boolean.FALSE, scheme.getId());

        //Review Step
        final WorkflowStep reviewStep = addWorkflowStep(REVIEW_STEP_NAME, 1, Boolean.FALSE,
                Boolean.FALSE,
                scheme.getId());

        //Legal Approval Step
        final WorkflowStep legalApprovalStep = addWorkflowStep(LEGAL_APPROVAL_STEP_NAME, 2,
                Boolean.FALSE,
                Boolean.FALSE, scheme.getId());

        //Published Step
        final WorkflowStep publishedStep = addWorkflowStep(PUBLISHED_STEP_NAME, 3, Boolean.TRUE,
                Boolean.FALSE, scheme.getId());

        //Archived Step
        final WorkflowStep archivedStep = addWorkflowStep(ARCHIVED_STEP_NAME, 4, Boolean.TRUE,
                Boolean.FALSE, scheme.getId());

        //Create Actions

        //Save as Draft Action
        WorkflowAction saveAsDraftAction = null;
        try {
            saveAsDraftAction = new WorkflowAction();
            saveAsDraftAction.setName(SAVE_AS_DRAFT_ACTION_NAME);
            saveAsDraftAction.setSchemeId(scheme.getId());
            saveAsDraftAction.setOwner(adminRole.getId());
            saveAsDraftAction.setOrder(0);
            saveAsDraftAction.setNextStep(WorkflowAction.CURRENT_STEP);
            saveAsDraftAction.setNextAssign(anonymus.getId());
            saveAsDraftAction.setCommentable(true);
            saveAsDraftAction.setAssignable(false);
            saveAsDraftAction.setShowOn(WorkflowState.LOCKED, WorkflowState.NEW,
                    WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);

            workflowAPI.saveAction(saveAsDraftAction,
                    Arrays.asList(new Permission[]{
                            new Permission(saveAsDraftAction.getId(),
                                    anyWhoEdit.getId(),
                                    PermissionAPI.PERMISSION_USE)}),
                    APILocator.systemUser());

            workflowAPI.saveAction(saveAsDraftAction.getId(), editingStep.getId(),
                    APILocator.systemUser(), 0);
            workflowAPI.saveAction(saveAsDraftAction.getId(), reviewStep.getId(),
                    APILocator.systemUser(), 0);
            workflowAPI.saveAction(saveAsDraftAction.getId(), legalApprovalStep.getId(),
                    APILocator.systemUser(), 0);
        } catch (AlreadyExistException e) {
            //scheme already exist
        }

        //Send for review Action
        WorkflowAction sendForReviewAction = null;
        try {
            sendForReviewAction = new WorkflowAction();
            sendForReviewAction.setName(SEND_FOR_REVIEW_ACTION_NAME);
            sendForReviewAction.setSchemeId(scheme.getId());
            sendForReviewAction.setOwner(adminRole.getId());
            sendForReviewAction.setOrder(1);
            sendForReviewAction.setNextStep(reviewStep.getId());
            sendForReviewAction.setNextAssign(reviewer.getId());
            sendForReviewAction.setCommentable(true);
            sendForReviewAction.setAssignable(false);
            sendForReviewAction.setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED,
                    WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);

            workflowAPI.saveAction(sendForReviewAction,
                    Arrays.asList(new Permission[]{
                            new Permission(sendForReviewAction.getId(),
                                    anyWhoEdit.getId(),
                                    PermissionAPI.PERMISSION_USE),
                            new Permission(sendForReviewAction.getId(),
                                    contributor.getId(),
                                    PermissionAPI.PERMISSION_USE)}),
                    APILocator.systemUser());

            workflowAPI.saveAction(sendForReviewAction.getId(), editingStep.getId(),
                    APILocator.systemUser(), 1);
        } catch (AlreadyExistException e) {
            //scheme already exist
        }

        //Send to Legal Action
        WorkflowAction sendToLegalAction = null;
        try {
            sendToLegalAction = new WorkflowAction();
            sendToLegalAction.setName(SEND_TO_LEGAL_ACTION_NAME);
            sendToLegalAction.setSchemeId(scheme.getId());
            sendToLegalAction.setOwner(adminRole.getId());
            sendToLegalAction.setOrder(1);
            sendToLegalAction.setNextStep(legalApprovalStep.getId());
            sendToLegalAction.setNextAssign(publisher.getId());
            sendToLegalAction.setCommentable(true);
            sendToLegalAction.setAssignable(false);
            sendToLegalAction.setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED,
                    WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);

            workflowAPI.saveAction(sendToLegalAction,
                    Arrays.asList(new Permission[]{
                            new Permission(sendToLegalAction.getId(),
                                    reviewer.getId(),
                                    PermissionAPI.PERMISSION_USE)}),
                    APILocator.systemUser());

            workflowAPI.saveAction(sendToLegalAction.getId(), editingStep.getId(),
                    APILocator.systemUser(), 1);
            workflowAPI.saveAction(sendToLegalAction.getId(), reviewStep.getId(),
                    APILocator.systemUser(), 2);
        } catch (AlreadyExistException e) {
            //scheme already exist
        }

        //Publish Action
        WorkflowAction publishAction = null;
        try {
            publishAction = new WorkflowAction();
            publishAction.setName(PUBLISH_ACTION_NAME);
            publishAction.setSchemeId(scheme.getId());
            publishAction.setOwner(adminRole.getId());
            publishAction.setOrder(1);
            publishAction.setNextStep(publishedStep.getId());
            publishAction.setNextAssign(anonymus.getId());
            publishAction.setCommentable(false);
            publishAction.setAssignable(false);
            publishAction.setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED,
                    WorkflowState.UNPUBLISHED, WorkflowState.EDITING);

            workflowAPI.saveAction(publishAction,
                    Arrays.asList(new Permission[]{
                            new Permission(publishAction.getId(),
                                    anyWhoPublish.getId(),
                                    PermissionAPI.PERMISSION_USE)}),
                    APILocator.systemUser());

            workflowAPI
                    .saveAction(publishAction.getId(), editingStep.getId(), APILocator.systemUser(),
                            3);
            workflowAPI
                    .saveAction(publishAction.getId(), reviewStep.getId(), APILocator.systemUser(),
                            3);
            workflowAPI.saveAction(publishAction.getId(), legalApprovalStep.getId(),
                    APILocator.systemUser(), 3);
        } catch (AlreadyExistException e) {
            //scheme already exist
        }

        //Return For Edit Action
        WorkflowAction returnForEditAction = null;
        try {
            returnForEditAction = new WorkflowAction();
            returnForEditAction.setName(RETURN_FOR_EDITS_ACTION_NAME);
            returnForEditAction.setSchemeId(scheme.getId());
            returnForEditAction.setOwner(adminRole.getId());
            returnForEditAction.setOrder(1);
            returnForEditAction.setNextStep(editingStep.getId());
            returnForEditAction.setNextAssign(contributor.getId());
            returnForEditAction.setCommentable(true);
            returnForEditAction.setAssignable(true);
            returnForEditAction.setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED,
                    WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);

            workflowAPI.saveAction(returnForEditAction,
                    Arrays.asList(new Permission[]{
                            new Permission(returnForEditAction.getId(),
                                    reviewer.getId(),
                                    PermissionAPI.PERMISSION_USE)}),
                    APILocator.systemUser());

            workflowAPI.saveAction(returnForEditAction.getId(), reviewStep.getId(),
                    APILocator.systemUser(), 1);
            workflowAPI.saveAction(returnForEditAction.getId(), legalApprovalStep.getId(),
                    APILocator.systemUser(), 1);
        } catch (AlreadyExistException e) {
            //scheme already exist
        }

        //Republish Action
        WorkflowAction republishAction = null;
        try {
            republishAction = new WorkflowAction();
            republishAction.setName(REPUBLISH_ACTION_NAME);
            republishAction.setSchemeId(scheme.getId());
            republishAction.setOwner(adminRole.getId());
            republishAction.setOrder(0);
            republishAction.setNextStep(publishedStep.getId());
            republishAction.setNextAssign(anonymus.getId());
            republishAction.setCommentable(false);
            republishAction.setAssignable(false);
            republishAction.setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED,
                    WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);

            workflowAPI.saveAction(republishAction,
                    Arrays.asList(new Permission[]{
                            new Permission(republishAction.getId(),
                                    anonymus.getId(),
                                    PermissionAPI.PERMISSION_USE)}),
                    APILocator.systemUser());

            workflowAPI.saveAction(republishAction.getId(), publishedStep.getId(),
                    APILocator.systemUser(), 0);
            workflowAPI.saveAction(republishAction.getId(), archivedStep.getId(),
                    APILocator.systemUser(), 2);
        } catch (AlreadyExistException e) {
            //scheme already exist
        }

        //Unpublish Action
        WorkflowAction unpublishAction = null;
        try {
            unpublishAction = new WorkflowAction();
            unpublishAction.setName(UNPUBLISH_ACTION_NAME);
            unpublishAction.setSchemeId(scheme.getId());
            unpublishAction.setOwner(adminRole.getId());
            unpublishAction.setOrder(1);
            unpublishAction.setNextStep(reviewStep.getId());
            unpublishAction.setNextAssign(anonymus.getId());
            unpublishAction.setCommentable(false);
            unpublishAction.setAssignable(false);
            unpublishAction.setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED,
                    WorkflowState.PUBLISHED, WorkflowState.EDITING);

            workflowAPI.saveAction(unpublishAction,
                    Arrays.asList(new Permission[]{
                            new Permission(unpublishAction.getId(),
                                    anyWhoPublish.getId(),
                                    PermissionAPI.PERMISSION_USE)}),
                    APILocator.systemUser());

            workflowAPI.saveAction(unpublishAction.getId(), publishedStep.getId(),
                    APILocator.systemUser(), 1);
        } catch (AlreadyExistException e) {
            //scheme already exist
        }

        //Archive action
        WorkflowAction archiveAction = null;
        try {
            archiveAction = new WorkflowAction();
            archiveAction.setName(ARCHIVE_ACTION_NAME);
            archiveAction.setSchemeId(scheme.getId());
            archiveAction.setOwner(adminRole.getId());
            archiveAction.setOrder(1);
            archiveAction.setNextStep(archivedStep.getId());
            archiveAction.setNextAssign(anonymus.getId());
            archiveAction.setCommentable(false);
            archiveAction.setAssignable(false);
            archiveAction.setShowOn(WorkflowState.UNLOCKED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);

            workflowAPI.saveAction(archiveAction,
                    Arrays.asList(new Permission[]{
                            new Permission(archiveAction.getId(),
                                    publisher.getId(),
                                    PermissionAPI.PERMISSION_USE)}),
                    APILocator.systemUser());

            workflowAPI.saveAction(archiveAction.getId(), publishedStep.getId(),
                    APILocator.systemUser(), 2);
        } catch (AlreadyExistException e) {
            //scheme already exist
        }

        //Delete action
        WorkflowAction deleteAction = null;
        try {
            deleteAction = new WorkflowAction();
            deleteAction.setName(DELETE_ACTION_NAME);
            deleteAction.setSchemeId(scheme.getId());
            deleteAction.setOwner(adminRole.getId());
            deleteAction.setOrder(1);
            deleteAction.setNextStep(archivedStep.getId());
            deleteAction.setNextAssign(anonymus.getId());
            deleteAction.setCommentable(false);
            deleteAction.setAssignable(false);
            deleteAction.setShowOn(WorkflowState.UNLOCKED, WorkflowState.ARCHIVED, WorkflowState.EDITING);

            workflowAPI.saveAction(deleteAction,
                    Arrays.asList(new Permission[]{
                            new Permission(archiveAction.getId(),
                                    publisher.getId(),
                                    PermissionAPI.PERMISSION_USE)}),
                    APILocator.systemUser());

            workflowAPI
                    .saveAction(deleteAction.getId(), archivedStep.getId(), APILocator.systemUser(),
                            1);
        } catch (AlreadyExistException e) {
            //scheme already exist
        }

        //reset workflow action
        WorkflowAction resetWorkflowAction = null;
        try {
            resetWorkflowAction = new WorkflowAction();
            resetWorkflowAction.setName(RESET_WORKFLOW_ACTION_NAME);
            resetWorkflowAction.setSchemeId(scheme.getId());
            resetWorkflowAction.setOwner(adminRole.getId());
            resetWorkflowAction.setOrder(1);
            resetWorkflowAction.setNextStep(archivedStep.getId());
            resetWorkflowAction.setNextAssign(anonymus.getId());
            resetWorkflowAction.setCommentable(false);
            resetWorkflowAction.setAssignable(false);
            resetWorkflowAction.setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED,
                    WorkflowState.ARCHIVED, WorkflowState.EDITING);

            workflowAPI.saveAction(resetWorkflowAction,
                    Arrays.asList(new Permission[]{
                            new Permission(archiveAction.getId(),
                                    publisher.getId(),
                                    PermissionAPI.PERMISSION_USE)}),
                    APILocator.systemUser());

            workflowAPI.saveAction(resetWorkflowAction.getId(), archivedStep.getId(),
                    APILocator.systemUser(), 0);
        } catch (AlreadyExistException e) {
            //scheme already exist
        }

        //Add subactions to the actions

        //Save as Draft subactionss
        addSubActionClass(SAVE_AS_DRAFT_SUBACTION, saveAsDraftAction.getId(),
                SaveContentAsDraftActionlet.class, 0);

        //Send For Review subactions
        addSubActionClass(SAVE_AS_DRAFT_SUBACTION, sendForReviewAction.getId(),
                SaveContentAsDraftActionlet.class, 0);
        addSubActionClass(UNLOCK_SUBACTION, sendForReviewAction.getId(),
                CheckinContentActionlet.class, 1);

        //Send to Legal subactions
        addSubActionClass(SAVE_AS_DRAFT_SUBACTION, sendToLegalAction.getId(),
                SaveContentAsDraftActionlet.class, 0);
        addSubActionClass(UNLOCK_SUBACTION, sendToLegalAction.getId(),
                CheckinContentActionlet.class, 1);

        //Publish subactions
        addSubActionClass(SAVE_CONTENT_SUBACTION, publishAction.getId(), SaveContentActionlet.class,
                0);
        addSubActionClass(PUBLISH_SUBACTION, publishAction.getId(), PublishContentActionlet.class,
                1);
        addSubActionClass(UNLOCK_SUBACTION, publishAction.getId(), CheckinContentActionlet.class,
                2);

        //Return for Edit subactions
        addSubActionClass(SAVE_AS_DRAFT_SUBACTION, returnForEditAction.getId(),
                SaveContentAsDraftActionlet.class, 0);
        addSubActionClass(UNLOCK_SUBACTION, returnForEditAction.getId(),
                CheckinContentActionlet.class, 1);

        //Republish subactions
        addSubActionClass(SAVE_AS_DRAFT_SUBACTION, republishAction.getId(),
                SaveContentAsDraftActionlet.class, 0);
        addSubActionClass(UNARCHIVE_SUBACTION, republishAction.getId(),
                UnarchiveContentActionlet.class, 1);
        addSubActionClass(PUBLISH_SUBACTION, republishAction.getId(), PublishContentActionlet.class,
                2);
        addSubActionClass(UNLOCK_SUBACTION, republishAction.getId(), CheckinContentActionlet.class,
                3);

        //Unpublish subactions
        addSubActionClass(SAVE_AS_DRAFT_SUBACTION, unpublishAction.getId(),
                SaveContentAsDraftActionlet.class, 0);
        addSubActionClass(UNPUBLISH_SUBACTION, unpublishAction.getId(),
                UnpublishContentActionlet.class, 1);
        addSubActionClass(UNLOCK_SUBACTION, unpublishAction.getId(), CheckinContentActionlet.class,
                2);

        //Archive subactions
        addSubActionClass(UNLOCK_SUBACTION, archiveAction.getId(), CheckinContentActionlet.class,
                0);
        addSubActionClass(UNPUBLISH_SUBACTION, archiveAction.getId(),
                UnpublishContentActionlet.class, 1);
        addSubActionClass(ARCHIVE_SUBACTION, archiveAction.getId(), ArchiveContentActionlet.class,
                2);

        //Reset workflow subactions
        addSubActionClass(UNLOCK_SUBACTION, resetWorkflowAction.getId(),
                CheckinContentActionlet.class, 0);
        addSubActionClass(UNARCHIVE_SUBACTION, resetWorkflowAction.getId(),
                UnpublishContentActionlet.class, 1);
        addSubActionClass(RESET_WORKFLOW_SUBACTION, resetWorkflowAction.getId(),
                ResetTaskActionlet.class, 2);

        //Delete subactions
        addSubActionClass(UNLOCK_SUBACTION, deleteAction.getId(), CheckinContentActionlet.class, 0);
        addSubActionClass(ARCHIVE_SUBACTION, deleteAction.getId(), ArchiveContentActionlet.class,
                1);
        addSubActionClass(RESET_WORKFLOW_SUBACTION, deleteAction.getId(), ResetTaskActionlet.class,
                2);
        addSubActionClass(DELETE_SUBACTION, deleteAction.getId(), DeleteContentActionlet.class, 3);

        return scheme;
    }

    /**
     * Execute a workflow action for a contentlet
     *
     * @param contentlet The contentlet to modified by the action
     * @param contentletRelationships The contentlet relationships
     * @param action The Workflow action to be execute
     * @param comment The workflow comment
     * @param workflowAssignKey The workflow assigned role key
     * @param user The user executing the actions
     * @return The Contentlet modifierd by the workflow action
     */
    public static Contentlet fireWorkflowAction(Contentlet contentlet,
            ContentletRelationships contentletRelationships, WorkflowAction action, String comment,
            String workflowAssignKey, User user) throws DotDataException, DotSecurityException {

        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet = APILocator.getWorkflowAPI().fireContentWorkflow(contentlet,
                new ContentletDependencies.Builder().respectAnonymousPermissions(Boolean.FALSE)
                        .modUser(user)
                        .relationships(contentletRelationships)
                        .workflowActionId(action.getId()) //Return for Editing
                        .workflowActionComments(comment)
                        .workflowAssignKey(workflowAssignKey)
                        .workflowPublishDate("2020-08-11")
                        .workflowPublishTime("10-35")
                        .workflowExpireDate("2020-08-12")
                        .workflowExpireTime("18-35")
                        .workflowFilterKey("Yaml File")
                        .workflowWhereToSend("environment")
                        .workflowIWantTo("publish")
                        .categories(Collections.emptyList())
                        .generateSystemEvent(Boolean.FALSE).build());

        return contentlet;
    }

    /**
     * Generate a generic value for a new contentlet
     *
     * @param title The title field of the content
     * @param contentType The content type of the new content
     * @return a new contentlet
     */
    public static Contentlet createContent(final String title, final ContentType contentType) {
        Contentlet content = new Contentlet();
        content.setContentTypeId(contentType.id());
        content.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        content.setStringProperty(FIELD_VAR_NAME,
                title + UtilMethods.dateToHTMLDate(new Date(), DATE_FORMAT));
        return content;
    }

    /**
     * Generate a content type and set a default permission permissions
     *
     * @param contentTypeName Name of the content type to create
     * @param baseContentType The type of content type to create
     * @param permission The type of permission
     * @param roleId The role that will have permission over the content type
     * @return the new content type
     */
    private ContentType generateContentTypeAndAssignPermissions(final String contentTypeName,
            final BaseContentType baseContentType, final int permission, final String roleId)
            throws DotDataException, DotSecurityException {
        ContentType contentType = insertContentType(
                contentTypeName + UtilMethods.dateToHTMLDate(new Date(), DATE_FORMAT),
                baseContentType);

        Permission p = new Permission(contentType.getPermissionId(), roleId,
                permission, true);
        permissionAPI.save(p, contentType, user, true);

        p = new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                roleId, permission, true);
        permissionAPI.save(p, contentType, user, true);

        return contentType;
    }

    @Test
    public void testPushIndexUpdate() throws Exception{
        final WorkflowScheme scheme = workflowAPI.findSystemWorkflowScheme();
        final WorkflowAPIImpl impl = WorkflowAPIImpl.class.cast(workflowAPI);
        final int rows = impl.pushIndexUpdate(scheme, user);
    }

    @Test
    public void find_Task_By_Contentlet_Then_Find_Version_Info() throws DotDataException, DotSecurityException {

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
            c1.setStringProperty(FIELD_VAR_NAME, "WorkflowContentTest3__" + time);
            c1.setContentTypeId(contentType.id());
            c1.setIndexPolicy(IndexPolicy.FORCE);
            c1 = contentletAPI.checkin(c1, user, false);

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

            String inode = null;
            try {
                inode = APILocator.getVersionableAPI().getVersionInfo(task.getWebasset())
                        .getWorkingInode();
            } finally {
                assertNotNull(inode);
            }

        } finally {
            try {

                contentletAPI.destroy(c1, user, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Test
    public void Create_Contentlet_Assign_Task_With_Null_Status_Then_Checkin_Then_Verify_Auto_Assign_is_Called_And_New_Task_Is_Set_Contentlet()
            throws DotDataException, DotSecurityException {

        final WorkflowScheme scheme = TestWorkflowUtils.getSystemWorkflow();
        final ContentType contentType = new ContentTypeDataGen().workflowId(scheme.getId())
                .name("myLameContentType").nextPersisted();
        final WorkflowStep emptyWorkflowStep = new WorkflowStep(); // This empty object will generate a null step id which is precisely the scenario we want to replicate.
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).nextPersisted();
        final WorkflowTask task = workflowAPI
                .createWorkflowTask(contentlet, user, emptyWorkflowStep,
                        "test Task with null status",
                        "test");
        assertNull(task.getStatus());
        final Contentlet out = contentletAPI.checkout(contentlet.getInode(), user, false);
        // Set up some flags to make sure the `ContentletCheckinEvent` is issued.
        out.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        out.setBoolProperty(Contentlet.WORKFLOW_IN_PROGRESS, false);
        out.setBoolProperty(Contentlet.AUTO_ASSIGN_WORKFLOW, true);
        //The Workflow task should be assigned by UnassignedWorkflowContentletCheckinListener
        APILocator.getLocalSystemEventsAPI().subscribe(ContentletCheckinEvent.class,
                (EventSubscriber<ContentletCheckinEvent>) event -> {
                    final Contentlet expectedContentlet = event.getContentlet();
                    if (null != expectedContentlet) {
                        // Ensure this is the same contentlet we sent.
                        if (out.getIdentifier().equals(expectedContentlet.getIdentifier())) {
                            try {
                                final WorkflowTask workflowTask = APILocator.getWorkflowAPI()
                                        .findTaskByContentlet(contentlet);
                                assertNotNull(workflowTask);
                                assertNotNull(workflowTask
                                        .getStatus()); // null Status should have been taken care by now.
                            } catch (DotDataException e) {
                                Logger.error(WorkflowAPITest.class, e);
                                fail(String
                                        .format("Something happened getting the workflow task for contentlet [%s] ",
                                                contentlet)
                                );
                            }
                        }
                    }
                }
        );
        contentletAPI.checkin(out, user, false);
    }

    /**
     * Tests the {@link WorkflowAPI#deleteWorkflowHistoryOldVersions(Date)} method
     */
    @Test
    public void deleteWorkflowHistoryOldVersions() throws DotDataException {

        final User systemUser = APILocator.systemUser();
        final Language language = APILocator.getLanguageAPI().getDefaultLanguage();
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

        final WorkflowScheme systemWorkflowScheme = workflowAPI.findSystemWorkflowScheme();
        final ContentType contentType = new ContentTypeDataGen()
                .workflowId(systemWorkflowScheme.getId())
                .nextPersisted();

        //Create a dummy contentlet
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .nextPersisted();

        assertNotNull(contentlet);

        //Saving a dummy workflow task
        final WorkflowTask workflowTask = workflowAPI.findTaskByContentlet(contentlet);
        assertNotNull(workflowTask);

        //Get the count of the workflow history records before the inserts
        final int initial = workflowHistoryCount(workflowTask.getId());
        assertEquals(0, initial);

        // Saving workflow history records with a 2019 creation date
        LocalDate date1 = LocalDate.of(2019, Month.DECEMBER, 15);
        final Date pastDate1 = Date.from(date1.atStartOfDay(ZoneId.systemDefault()).toInstant());

        for (int i = 0; i < 5; i++) {
            final WorkflowHistory workflowHistory = new WorkflowHistory();
            workflowHistory.setChangeDescription("workflow history description");
            workflowHistory.setCreationDate(pastDate1);
            workflowHistory.setMadeBy(systemUser.getUserId());
            workflowHistory.setWorkflowtaskId(workflowTask.getId());
            workflowAPI.saveWorkflowHistory(workflowHistory);
        }

        // Saving workflow history records with a 2018 creation date
        LocalDate date2 = LocalDate.of(2018, Month.DECEMBER, 15);
        final Date pastDate2 = Date.from(date2.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // Saving workflow history records
        for (int i = 0; i < 5; i++) {
            final WorkflowHistory workflowHistory = new WorkflowHistory();
            workflowHistory.setChangeDescription("workflow history description");
            workflowHistory.setCreationDate(pastDate2);
            workflowHistory.setMadeBy(systemUser.getUserId());
            workflowHistory.setWorkflowtaskId(workflowTask.getId());
            workflowAPI.saveWorkflowHistory(workflowHistory);
        }

        //Get the count of the workflow history records before deleting
        final int before = workflowHistoryCount(workflowTask.getId());
        assertEquals(10, before);

        // Now we will try to delete and validate the deletes with different dates
        LocalDate date3 = LocalDate.of(2018, Month.DECEMBER, 16);
        final Date pastDate3 = Date.from(date3.atStartOfDay(ZoneId.systemDefault()).toInstant());
        int deleted = workflowAPI.deleteWorkflowHistoryOldVersions(pastDate3);
        assertTrue(deleted > 0);
        final int left = workflowHistoryCount(workflowTask.getId());
        assertEquals(5, left);

        LocalDate date4 = LocalDate.of(2019, Month.DECEMBER, 16);
        final Date pastDate4 = Date.from(date4.atStartOfDay(ZoneId.systemDefault()).toInstant());
        deleted = workflowAPI.deleteWorkflowHistoryOldVersions(pastDate4);
        assertTrue(deleted > 0);

        final int finalCount = workflowHistoryCount(workflowTask.getId());
        assertEquals(0, finalCount);
    }

    private int workflowHistoryCount(final String workflowTaskId) throws DotDataException {

        DotConnect dotConnect = new DotConnect();
        //Get the count of the workflow history records before deleting.
        String countSQL = "select count(*) as count from workflow_history where workflowtask_id = ?";
        dotConnect.setSQL(countSQL);
        dotConnect.addParam(workflowTaskId);
        List<Map<String, String>> result = dotConnect.loadResults();

        return Integer.parseInt(result.get(0).get("count"));
    }

    /**
     * Copy Properties: The idea is to check if the transient variables on the contentlet such as workflow attributes are being copied after save
     * Given Scenario: Create a content, fires a save and checks if the new checkout still having the workflow attributes
     * ExpectedResult: The workflow attributes still there after the save
     *
     */
    @Test
    public void fireWorkflowAction_checkPropertiesAreCopied_successfully()
            throws DotDataException, DotSecurityException, AlreadyExistException, ExecutionException, InterruptedException {
        WorkflowScheme workflowScheme = null;
        ContentType contentType = null;
        try {

            contentType = generateContentTypeAndAssignPermissions("KeepWfTaskStatus",
                    BaseContentType.CONTENT, editPermission, contributor.getId());

            // Create testing workflows
            workflowScheme = createDocumentManagentReplica(
                    DOCUMENT_MANAGEMENT_WORKFLOW_NAME + "_5_" + UtilMethods
                            .dateToHTMLDate(new Date(), DATE_FORMAT));

            final Set<String> schemeIds = new HashSet<>();
            schemeIds.add(workflowScheme.getId());
            workflowAPI.saveSchemeIdsForContentType(contentType, schemeIds);

            //Add Workflow Task
            //Contentlet1 on published step
            Contentlet contentlet = createContent("testCacheFindStepsByContentlet", contentType);

            List<WorkflowAction> actions = workflowAPI
                    .findAvailableActions(contentlet, joeContributor);
            final WorkflowAction saveAsDraft = actions.get(0);

            //As Contributor - Save as Draft
            final ContentletRelationships contentletRelationships = APILocator.getContentletAPI()
                    .getAllRelationships(contentlet);
            //save as Draft
            contentlet = fireWorkflowAction(contentlet, contentletRelationships, saveAsDraft,
                    StringPool.BLANK, StringPool.BLANK, joeContributor);

            Assert.assertTrue(UtilMethods.isSet(contentlet.getStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE)));
            Assert.assertTrue(UtilMethods.isSet(contentlet.getStringProperty(Contentlet.WORKFLOW_PUBLISH_TIME)));
            Assert.assertTrue(UtilMethods.isSet(contentlet.getStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE)));
            Assert.assertTrue(UtilMethods.isSet(contentlet.getStringProperty(Contentlet.WORKFLOW_EXPIRE_TIME)));
            Assert.assertTrue(UtilMethods.isSet(contentlet.getStringProperty(Contentlet.WHERE_TO_SEND)));
            Assert.assertTrue(UtilMethods.isSet(contentlet.getStringProperty(Contentlet.FILTER_KEY)));
            Assert.assertTrue(UtilMethods.isSet(contentlet.getStringProperty(Contentlet.I_WANT_TO)));
            Assert.assertTrue(UtilMethods.isNotSet(contentlet.getStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY)));
            Assert.assertTrue(UtilMethods.isNotSet(contentlet.getStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY)));

        } finally {
            //clean test
            //delete content type
            contentTypeAPI.delete(contentType);

            workflowScheme.setArchived(true);
            workflowAPI.saveScheme(workflowScheme, user);
            workflowAPI.deleteScheme(workflowScheme, user).get();
        }
    }

    /**
     * Method to test: {@link WorkflowAPI#findActions(List, User, Permissionable)}
     * Given Scenario: Creates a wokflow with two move action: 1 move without path and 1 move with path
     * ExpectedResult: Returns the move actions with the right move settings
     *
     */
    @Test
    public void test_create_workflow_with_two_move_actionlets_test_findActions() throws DotDataException, DotSecurityException {

        final long time = System.currentTimeMillis();
        final String workflowSchemeName = "WorkflowSchemeTestMove_" + time;
        final String workflowScheme3Step1Name = "WorkflowSchemeMoveStep1_" + time;
        final String workflowScheme3Step1ActionMoveToName = "Move_to_" + time;
        final String workflowScheme3Step2ActionMoveToFolderName = "Move_to_folder" + time;
        final WorkflowScheme workflowScheme = addWorkflowScheme(workflowSchemeName);

        /* Generate scheme steps */
        final WorkflowStep workflowSchemeStep = addWorkflowStep(workflowScheme3Step1Name, 1, false, false,
                workflowScheme.getId());

        /* Generate actions */
        final WorkflowAction workflowSchemeMoveToAction = addWorkflowAction(workflowScheme3Step1ActionMoveToName, 1,
                workflowSchemeStep.getId(), workflowSchemeStep.getId(), reviewer,
                workflowScheme.getId());

        final WorkflowAction workflowSchemeMoveToFolderAction = addWorkflowAction(workflowScheme3Step2ActionMoveToFolderName, 2,
                workflowSchemeStep.getId(), workflowSchemeStep.getId(), contributor,
                workflowScheme.getId());

        final WorkflowActionClass moveToActionClass = addSubActionClass("Move", workflowSchemeMoveToAction.getId(), MoveContentActionlet.class, 0);
        final WorkflowActionClass moveToFolderActionClass = addSubActionClass("Move", workflowSchemeMoveToFolderAction.getId(), MoveContentActionlet.class, 0);

        final List<WorkflowActionClassParameter> params = new ArrayList<>();
        final WorkflowActionClassParameter pathParam = new WorkflowActionClassParameter();
        pathParam.setActionClassId(moveToFolderActionClass.getId());
        pathParam.setKey(MoveContentActionlet.PATH_KEY);
        pathParam.setValue("//default/application");
        params.add(pathParam);
        workflowAPI.saveWorkflowActionClassParameters(params, user);

        List<WorkflowStep> steps = workflowAPI.findSteps(workflowScheme);
        assertNotNull(steps);
        assertEquals(1, steps.size());

        //check available actions for admin user
        List<WorkflowAction> actions = workflowAPI.findActions(steps, user);
        assertNotNull(actions);
        assertEquals(2, actions.size());

        actions = workflowAPI.findActions(steps, user, null);
        assertNotNull(actions);
        assertTrue(actions.size() == 2);

        final Optional<WorkflowAction> moveToAction = actions.stream().filter(action -> action.getName().equals(workflowScheme3Step1ActionMoveToName)).findFirst();
        final Optional<WorkflowAction> moveToFolderAction = actions.stream().filter(action -> action.getName().equals(workflowScheme3Step2ActionMoveToFolderName)).findFirst();

        assertTrue(moveToAction.isPresent());
        assertTrue(moveToFolderAction.isPresent());

        assertTrue(moveToAction.get().hasMoveActionletActionlet());
        assertFalse(moveToAction.get().hasMoveActionletHasPathActionlet());

        assertTrue(moveToFolderAction.get().hasMoveActionletActionlet());
        assertTrue(moveToFolderAction.get().hasMoveActionletHasPathActionlet());
    }

    /**
     * Method to test: {@link WorkflowFactoryImpl#saveWorkflowTask(WorkflowTask)}
     * when: Save a new {@link WorkflowTask} to a new {@link Contentlet}, later Update the {@link WorkflowStep}
     * should: first create a new {@link WorkflowTask} and later update it with the new {@link WorkflowStep}
     *
     * @throws DotDataException
     */
    @Test()
    public void saveWorkflowTask() throws DotDataException {
        final String title = "title";
        final String description = "description";

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType.id()).nextPersisted();
        final User user = APILocator.systemUser();

        final WorkflowTask task = new WorkflowTask();
        final Date now = new Date();

        final WorkflowTask taskByContentlet = FactoryLocator.getWorkFlowFactory()
                .findTaskByContentlet(contentlet);

        assertNotNull(taskByContentlet);
        assertNotNull(taskByContentlet.getId());
        assertEquals("Auto assign to the step: New", taskByContentlet.getTitle());
        assertEquals(String.format("The content titled \"%s\" has been moved automatically to the step New", contentlet.getTitle()),
                taskByContentlet.getDescription());

        final WorkflowStep newWorkflowStep = workflowAPI.findStep(SystemWorkflowConstants.WORKFLOW_NEW_STEP_ID);
        assertEquals(taskByContentlet.getStatus(), newWorkflowStep.getId());

        final List<WorkflowStep> steps = workflowAPI
                .findSteps(workflowAPI.findSystemWorkflowScheme());

        final Optional<WorkflowStep> notNewWorkflowStep = steps.stream()
                .filter(step -> step.getId() != taskByContentlet.getStatus())
                .findAny();
        task.setStatus(notNewWorkflowStep.get().getId());
        workflowAPI.saveWorkflowTask(task);

        assertNotNull(taskByContentlet);
        assertNotNull(taskByContentlet.getId());
        assertEquals(taskByContentlet.getStatus(), notNewWorkflowStep.get().getId());

    }


    /**
     * This test is meant to update relationship between two contentlets     *
     * It creates 2 content types Movie and Region and creates a one-to-many relationship between Movie and Region, then create 2 child contentlets
     * of Region (Africa and Asia) and then finally create 1 child contentlet of Movie and update the relationship.
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void createContentletsWithRelationshipOneToManyCardinality_updateRelationship_shouldCreateRelationshipSuccessfully() throws DotSecurityException, DotDataException{

        ContentType movieContentType  = null;
        ContentType regionContentType = null;
        try{

            //Create content types
            movieContentType  = insertContentType("Movie", BaseContentType.CONTENT);
            regionContentType = insertContentType("Region", BaseContentType.CONTENT);
            APILocator.getWorkflowAPI().saveSchemeIdsForContentType(movieContentType,  Set.of(new String[]{SystemWorkflowConstants.SYSTEM_WORKFLOW_ID}));
            APILocator.getWorkflowAPI().saveSchemeIdsForContentType(regionContentType, Set.of(new String[]{SystemWorkflowConstants.SYSTEM_WORKFLOW_ID}));

            //Create Relationship Field
            final Field relField = createRelationshipField("regions", movieContentType.id(),
                    regionContentType.variable(), String.valueOf(
                            WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()));

            Contentlet africa = createContent("africa", regionContentType);
            Contentlet asia   = createContent("asia", regionContentType);

            //Publish
            final WorkflowAction publishAction = APILocator.getWorkflowAPI().findAction(
                    SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID, APILocator.systemUser());

            africa.setIndexPolicy(IndexPolicy.WAIT_FOR);
            africa = fireWorkflowAction(africa, null,
                    publishAction,
                    StringPool.BLANK, StringPool.BLANK, user);

            asia.setIndexPolicy(IndexPolicy.WAIT_FOR);
            asia = fireWorkflowAction(asia, null,
                    publishAction,
                    StringPool.BLANK, StringPool.BLANK, user);

            assertTrue(africa.isLive());
            assertTrue(asia.isLive());

            Contentlet movie   = createContent("movie", movieContentType);
            movie.setProperty("Regions", Arrays.asList(africa, asia));

            final List<Contentlet> records = new ArrayList<>(Arrays.asList(asia, africa));
            final List<Relationship> relationships = APILocator.getRelationshipAPI().byContentType(movieContentType);
            final Relationship relationship = APILocator.getRelationshipAPI().byInode(relationships.stream().findFirst().get().getInode());

            final List<ContentletRelationships.ContentletRelationshipRecords> relationshipsRecords = new ArrayList<>();
            final ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords =
                    new ContentletRelationships(null).new ContentletRelationshipRecords(relationship, true);

            contentletRelationshipRecords.setRecords(records);
            relationshipsRecords.add(contentletRelationshipRecords);

            final ContentletRelationships contentletRelationships = new ContentletRelationships(movie, relationshipsRecords);
            movie.setIndexPolicy(IndexPolicy.WAIT_FOR);
            movie = fireWorkflowAction(movie, contentletRelationships,
                    publishAction,
                    StringPool.BLANK, StringPool.BLANK, user);

            final List<Contentlet> movieContentRelated = APILocator.getContentletAPI().getRelatedContent(movie, relationship, user, false);

            Assert.assertNotNull(movieContentRelated);
            Assert.assertTrue(movieContentRelated.size() == 2);
        } finally {
            if(movieContentType != null){
                contentTypeAPI.delete(movieContentType);
            }
            if(regionContentType != null){
                contentTypeAPI.delete(regionContentType);
            }
        }
    }

    private Field createRelationshipField(final String relationshipName, final String parentTypeId,
                                          final String childTypeVar, final String cardinality)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(RelationshipField.class).name(relationshipName)
                .contentTypeId(parentTypeId).values(cardinality).relationType(childTypeVar).build();

        return fieldAPI.save(field, user);
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link WorkflowAPI#findAvailableActions(Contentlet, User)}
     *     </li>
     *     <li><b>Given Scenario: </b>Create a test Workflow Scheme with two Steps. One of them
     *     has a separator between the actions. Then, create a test Contentlet assigned to such a
     *     Workflow and check the available actions that can be executed on the Contentlet.</li>
     *     <li><b>Expected Result: </b>The available actions must include the separator.</li>
     * </ul>
     */
    @Test
    public void addSeparatorToWorkflowStep() throws DotDataException, DotSecurityException {
        long time = System.currentTimeMillis();
        final String workflowSchemeName = "WorkflowSchemeWithSeparator" + time;
        final String step1Name = "WithSeparatorStep1" + time;
        final String step2Name = "WithoutSeparatorStep2_" + time;
        final String workflowAction1NameStep1 = "action1Step1_" + time;
        final String workflowAction3NameStep1 = "action3Step1_" + time;
        final String workflowAction1NameStep2 = "action1Step2_" + time;
        final String typeName = "WorkflowActionSeparatorTest_" + time;

        // Add a test Workflow
        final WorkflowScheme workflowScheme = addWorkflowScheme(workflowSchemeName);

        // Add two Steps to it
        final WorkflowStep workflowStep1 = addWorkflowStep(step1Name, 1, false, false,
                workflowScheme.getId());
        final WorkflowStep workflowStep2 = addWorkflowStep(step2Name, 2, true, false,
                workflowScheme.getId());

        // Add actions to the Steps: Two actions and one separator in Step 1, and one action in
        // Step 2
        final WorkflowAction workflowAction1Step1 = addWorkflowAction(workflowAction1NameStep1, 1,
                workflowStep1.getId(), workflowStep1.getId(), intranet, workflowScheme.getId());
        addWorkflowAction(WorkflowAction.SEPARATOR, 2, workflowStep1.getId(),
                workflowStep1.getId(), intranet, workflowScheme.getId(), Map.of("subtype",
                        WorkflowAction.SEPARATOR));
        addWorkflowAction(workflowAction3NameStep1, 3, workflowStep1.getId(),
                workflowStep1.getId(), intranet, workflowScheme.getId());

        addWorkflowAction(workflowAction1NameStep2, 1, workflowStep2.getId(),
                workflowStep2.getId(), intranet, workflowScheme.getId());

        // Assign the Workflow Scheme to a test Content Type
        final ContentType testContentType = insertContentType(typeName, BaseContentType.CONTENT);
        final Structure testContentTypeStruct = new StructureTransformer(testContentType).asStructure();
        workflowAPI.saveSchemesForStruct(testContentTypeStruct, List.of(workflowScheme));

        // Create a test contentlet
        Contentlet testContentlet1 = new Contentlet();
        testContentlet1.setLanguageId(1);
        testContentlet1.setStringProperty(FIELD_VAR_NAME, "My Test Contentlet " + time);
        testContentlet1.setContentTypeId(testContentType.id());
        testContentlet1.setHost(defaultHost.getIdentifier());
        testContentlet1.setIndexPolicy(IndexPolicy.FORCE);
        testContentlet1 = contentletAPI.checkin(testContentlet1, user, false);
        testContentlet1 = fireWorkflowAction(testContentlet1, null, workflowAction1Step1,
                StringPool.BLANK, StringPool.BLANK, user);
        workflowAPI.deleteWorkflowTaskByContentletIdAnyLanguage(testContentlet1, user);

        // Adding permissions to the just created contentlet
        final Role role = roleAPI.getUserRole(billIntranet);
        final Permission p1 = new Permission(testContentlet1.getPermissionId(), role.getId(),
                (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT), true);
        final List<Permission> permissions = List.of(p1);
        permissionAPI.save(permissions, testContentlet1, user, false);

        // Checkout the created Contentlet
        final Contentlet testContentlet1Checkout =
                contentletAPI.checkout(testContentlet1.getInode(), user, false);
        final List<WorkflowAction> foundActions =
                workflowAPI.findAvailableActions(testContentlet1Checkout, billIntranet);

        assertNotNull(foundActions);
        assertEquals("The second Workflow Action must be of subtype SEPARATOR",
                WorkflowAction.SEPARATOR, foundActions.get(1).getMetadata().get("subtype"));
    }

    /**
     * Method to test: {@link WorkflowAPI#countWorkflowSchemes(User)}
     * Given Scenario: Creates 4 workflow schemes, archive one
     * ExpectedResult: Returns 4, which is the 3 created + system workflow scheme. The archived one
     * is not counted.
     */
    @Test
    public void testCountWorkflowSchemes()
            throws DotDataException, DotSecurityException, AlreadyExistException {

        final int intialSchemaCount = workflowAPI.findSchemes(false).size();

        addWorkflowScheme("countTest1" + System.currentTimeMillis());
        addWorkflowScheme("countTest2" + System.currentTimeMillis());
        addWorkflowScheme("countTest3" + System.currentTimeMillis());
        final WorkflowScheme toArchive = addWorkflowScheme("countTest4"
                + System.currentTimeMillis());

        // let's archive one
        workflowAPI.archive(toArchive, APILocator.systemUser());

        // 3 new schemes. Archived one is not counted
        assertEquals(intialSchemaCount + 3,workflowAPI.countWorkflowSchemes(APILocator.systemUser()));
    }

    /**
     * Method to test: {@link WorkflowAPI#countWorkflowSchemesIncludeArchived(User)}
     * Given Scenario: Creates 4 workflow schemes, archive one
     * ExpectedResult: Returns 4, which is the 3 created + system workflow scheme. The archived one
     * is not counted.
     */
    @Test
    public void testCountWorkflowSchemesIncludeArchived()
            throws DotDataException, DotSecurityException, AlreadyExistException {

        final int intialSchemaCount = workflowAPI.findSchemes(true).size();

        addWorkflowScheme("countTest1" + System.currentTimeMillis());
        addWorkflowScheme("countTest2" + System.currentTimeMillis());
        addWorkflowScheme("countTest3" + System.currentTimeMillis());
        final WorkflowScheme toArchive = addWorkflowScheme("countTest4"
                + System.currentTimeMillis());

        // let's archive one
        workflowAPI.archive(toArchive, APILocator.systemUser());

        // 3 new schemes + 1 archived. Archived one is counted
        assertEquals(intialSchemaCount + 4, workflowAPI.countWorkflowSchemesIncludeArchived(APILocator.systemUser()));
    }

     /*
     * Method to test: {@link WorkflowFactoryImpl#countAllSchemasSteps()}
     * When: create a new Workflow with 5 steps
     * Should: the count must be 5 more than before
     *
     * @throws DotDataException
     */
    @Test
    public void countAllWorkflowSteps() throws DotDataException, DotSecurityException {

        final long firstCount = APILocator.getWorkflowAPI().countAllSchemasSteps(APILocator.systemUser());

        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions_1 = getStepsAndActions();

        final WorkflowScheme workflow_1= new WorkflowDataGen().name("Testing")
                .stepAndAction(workflowStepsAndActions_1).nextPersistedWithStepsAndActions();

        final long secondCount = APILocator.getWorkflowAPI().countAllSchemasSteps(APILocator.systemUser());
        assertEquals(firstCount + 5, secondCount);

        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions_2 = getStepsAndActions();

        final WorkflowScheme workflow_2 = new WorkflowDataGen().name("Testing")
                .stepAndAction(workflowStepsAndActions_2).nextPersistedWithStepsAndActions();

        final long thirdCount = APILocator.getWorkflowAPI().countAllSchemasSteps(APILocator.systemUser());
        assertEquals(secondCount + 5, thirdCount);
    }


    /**
     * Method to test: {@link WorkflowFactoryImpl#countAllSchemasSteps()}
     * When: Try to count Steos with limited USer
     * Should: throw a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test(expected = DotSecurityException.class)
    public void countStepsWithLimitedUser() throws DotDataException, DotSecurityException {
        final User user = new UserDataGen().nextPersisted();
        APILocator.getWorkflowAPI().countAllSchemasSteps(user);
    }

    /**
     * Method to test: {@link WorkflowAPIImpl#countAllSchemasActions(User)}
     * When: create a new Workflow with 4 actions
     * Should: the count must be 4 more than before
     *
     * @throws DotDataException
     */
    @Test
    public void countAllWorkflowActions() throws DotDataException, DotSecurityException {

        final long firstCount = APILocator.getWorkflowAPI().countAllSchemasActions(APILocator.systemUser());

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

        final long secondCount = APILocator.getWorkflowAPI().countAllSchemasActions(APILocator.systemUser());
        assertEquals(firstCount + 3, secondCount);

        final WorkflowScheme workflow_2 = new WorkflowDataGen()
                .stepAndAction(workflowStepsAndActions).nextPersistedWithStepsAndActions();

        final long thirdCount = APILocator.getWorkflowAPI().countAllSchemasActions(APILocator.systemUser());
        assertEquals(secondCount + 3, thirdCount);
    }

    /**
     * Method to test: {@link WorkflowAPIImpl#countAllSchemasActions(User)}
     * When: Try to count Steos with limited USer
     * Should: throw a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test(expected = DotSecurityException.class)
    public void countActionsWithLimitedUser() throws DotDataException, DotSecurityException {
        final User user = new UserDataGen().nextPersisted();
        APILocator.getWorkflowAPI().countAllSchemasActions(user);
    }

    /**
     * Method to test: {@link WorkflowAPIImpl#countAllSchemasSubActions(User)}
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

        final long firstCount = APILocator.getWorkflowAPI().countAllSchemasSubActions(APILocator.systemUser());

        final WorkflowScheme workflow_1 = new WorkflowDataGen()
                .stepAndAction(workflowStepsAndActions_1).nextPersistedWithStepsAndActions();

        final List<WorkflowAction> actions_1 = APILocator.getWorkflowAPI().findActions(workflow_1, APILocator.systemUser());
        new WorkflowActionClassDataGen(actions_1.get(0).getId()).nextPersisted();
        new WorkflowActionClassDataGen(actions_1.get(0).getId()).nextPersisted();
        new WorkflowActionClassDataGen(actions_1.get(1).getId()).nextPersisted();

        final long secondCount = APILocator.getWorkflowAPI().countAllSchemasSubActions(APILocator.systemUser());
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

        final long thirdCount = APILocator.getWorkflowAPI().countAllSchemasSubActions(APILocator.systemUser());
        assertEquals(secondCount + 7, thirdCount);
    }

    /**
     * Method to test: {@link WorkflowAPIImpl#countAllSchemasUniqueSubActions(User)} ()}
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

        final long firstCount = APILocator.getWorkflowAPI().countAllSchemasUniqueSubActions(APILocator.systemUser());

        final WorkflowScheme workflow_1 = new WorkflowDataGen()
                .stepAndAction(workflowStepsAndActions_1).nextPersistedWithStepsAndActions();

        final List<WorkflowAction> actions_1 = APILocator.getWorkflowAPI().findActions(workflow_1, APILocator.systemUser());


        new WorkflowActionClassDataGen(actions_1.get(0).getId()).actionClass(Actionlet_5.class).nextPersisted();
        new WorkflowActionClassDataGen(actions_1.get(0).getId()).actionClass(Actionlet_5.class).nextPersisted();
        new WorkflowActionClassDataGen(actions_1.get(1).getId()).actionClass(Actionlet_5.class).nextPersisted();

        final long secondCount = APILocator.getWorkflowAPI().countAllSchemasUniqueSubActions(APILocator.systemUser());
        assertEquals(firstCount + 1, secondCount);
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

    /**
     * Method to test: {@link WorkflowAPIImpl#getCommentsAndChangeHistory(WorkflowTask)}
     * Given Scenario: The list of comments should be the newest first.
     * ExpectedResult: The list of comments in the correct order.
     *
     */
    @Test
    public void test_comment() throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();
        final Language language = APILocator.getLanguageAPI().getDefaultLanguage();
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        Contentlet contentlet = null;
        contentlet = TestDataUtils.getPageContent(true, language.getId());

        //save workflow task
        final WorkflowStep workflowStep = workflowAPI.findStep(
                SystemWorkflowConstants.WORKFLOW_NEW_STEP_ID);
        workflowAPI.deleteWorkflowTaskByContentletIdAnyLanguage(contentlet, systemUser);
        final WorkflowTask workflowTask = workflowAPI
                .createWorkflowTask(contentlet, systemUser, workflowStep, "test", "test");
        workflowAPI.saveWorkflowTask(workflowTask);

        //save workflow comment
        WorkflowComment comment = new WorkflowComment();
        comment.setComment("comment test 1");
        comment.setCreationDate(new Date());
        comment.setPostedBy(systemUser.getUserId());
        comment.setWorkflowtaskId(workflowTask.getId());
        workflowAPI.saveComment(comment);

        //set date to yesterday
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);

        //save workflow comment
        comment = new WorkflowComment();
        comment.setComment("comment test 2 yesterday date");
        comment.setCreationDate(calendar.getTime());
        comment.setPostedBy(systemUser.getUserId());
        comment.setWorkflowtaskId(workflowTask.getId());
        workflowAPI.saveComment(comment);

        List<WorkflowTimelineItem> comments1 = workflowAPI.getCommentsAndChangeHistory(workflowTask);
        assertNotNull(comments1);
        assertEquals(2, comments1.size());
        //validate the date order of the comments
        assertTrue(comments1.get(0).createdDate().after(comments1.get(1).createdDate()) );
        //the comment 1 should be the first one
        assertEquals("comment test 1", comments1.get(0).commentDescription());
    }

    /**
     * Method to test: N/A
     * <p>
     * Given Scenario: Workflows with the same and different names are created
     * <p>
     * ExpectedResult: Variable names are correctly created and incremented for workflows with
     * duplicate names and properly created for workflows with different names
     *
     * @throws DotDataException      if a data access error occurs.
     * @throws DotSecurityException  if a security error occurs.
     * @throws AlreadyExistException if an existing entity conflicts with the operation.
     */
    @Test
    public void test_variableName_creation()
            throws DotDataException, DotSecurityException, AlreadyExistException {

        final long currentTimeMilis = System.currentTimeMillis();
        final String schemeName = "TestScheme" + currentTimeMilis;

        // Creating test workflows, some with the same name
        final var workflow1 = new WorkflowDataGen().name(schemeName).nextPersisted();
        final var workflow2 = new WorkflowDataGen().name(schemeName).nextPersisted();
        final var workflow3 = new WorkflowDataGen().name(schemeName).nextPersisted();
        final var workflow4 = new WorkflowDataGen().name(schemeName).nextPersisted();
        final var workflow5 = new WorkflowDataGen().name("AnotherTestScheme" + currentTimeMilis).
                nextPersisted();

        try {
            // Validate we created properly the variable name
            var expectedVariableNames = new String[]{
                    "TestScheme" + currentTimeMilis,
                    "TestScheme" + currentTimeMilis + "1",
                    "TestScheme" + currentTimeMilis + "2",
                    "TestScheme" + currentTimeMilis + "3",
                    "AnotherTestScheme" + currentTimeMilis
            };

            for (String variableName : expectedVariableNames) {
                final DotConnect dotConnect = new DotConnect();
                dotConnect.setSQL("SELECT count(*) FROM workflow_scheme WHERE variable_name = ?");
                dotConnect.addParam(variableName);

                assertEquals(1, dotConnect.getInt("count"));
            }
        } finally {
            cleanUpWorkflows(workflow1, workflow2, workflow3, workflow4, workflow5);
        }
    }

    /**
     * Method to test: {@link WorkflowAPI#findScheme(String)}
     * <p>
     * Given Scenario: Workflow schemes are searched by their ID and variable name
     * <p>
     * ExpectedResult: Workflow schemes are found by their ID and variable name, and an appropriate
     * error is thrown for non-existent schemes
     *
     * @throws DotDataException      if a data access error occurs.
     * @throws DotSecurityException  if a security error occurs.
     * @throws AlreadyExistException if an existing entity conflicts with the operation.
     */
    @Test
    public void test_findScheme()
            throws DotDataException, DotSecurityException, AlreadyExistException {

        final long currentTimeMilis = System.currentTimeMillis();

        // Creating test workflows, some with the same name
        final var workflow1 = new WorkflowDataGen().name("testScheme" + currentTimeMilis).
                nextPersisted();
        final var workflow2 = new WorkflowDataGen().name("anotherTestScheme" + currentTimeMilis).
                nextPersisted();

        try {
            // Searching the system workflow by id
            var systemWorkflow = workflowAPI.findScheme(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID);
            assertNotNull(systemWorkflow);
            assertEquals(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID, systemWorkflow.getId());
            assertEquals(SystemWorkflowConstants.SYSTEM_WORKFLOW_VARIABLE_NAME,
                    systemWorkflow.getVariableName());

            // Searching the system workflow by variable name
            systemWorkflow = workflowAPI.findScheme(
                    SystemWorkflowConstants.SYSTEM_WORKFLOW_VARIABLE_NAME);
            assertNotNull(systemWorkflow);
            assertEquals(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID, systemWorkflow.getId());
            assertEquals(SystemWorkflowConstants.SYSTEM_WORKFLOW_VARIABLE_NAME,
                    systemWorkflow.getVariableName());

            // Searching for the just created test workflows
            var result = workflowAPI.findScheme("testscheme" + currentTimeMilis);
            assertNotNull(result);
            assertEquals(workflow1.getId(), result.getId());

            result = workflowAPI.findScheme("tesTscheME" + currentTimeMilis);
            assertNotNull(result);
            assertEquals(workflow1.getId(), result.getId());

            result = workflowAPI.findScheme(workflow2.getId());
            assertNotNull(result);
            assertEquals(workflow2.getId(), result.getId());

            // Searching a non-existing workflow scheme and validate the proper error handling
            try {
                workflowAPI.findScheme("does-not-exist");
                fail("Expected DoesNotExistException not thrown");
            } catch (Exception e) {
                assertTrue(e instanceof DoesNotExistException);
            }
        } finally {
            cleanUpWorkflows(workflow1, workflow2);
        }
    }

    /**
     * Cleans up the test workflows created during the test cases.
     *
     * @param workflowSchemes the workflows to be removed.
     */
    private void cleanUpWorkflows(final WorkflowScheme... workflowSchemes)
            throws DotDataException, DotSecurityException, AlreadyExistException {
        for (WorkflowScheme testScheme : workflowSchemes) {
            APILocator.getWorkflowAPI().archive(testScheme, APILocator.systemUser());
            APILocator.getWorkflowAPI().deleteScheme(testScheme, APILocator.systemUser());
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link WorkflowAPI#findActionlet(String)}</li>
     *     <li><b>Given Scenario: </b>Find the list of expected Actionlets in a Workflow Action of
     *     the first Step in the System Workflow.</li>
     *     <li><b>Expected Result: </b>The API must return the expected two Actionlets: "Save Draft
     *     content", and "Unlock content".</li>
     * </ul>
     */
    @Test
    public void findActionletsInWorkflowActionByClassNameAsString() throws DotDataException,
            DotSecurityException {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final Map<String, String> expectedActionletData = Map.of(
                SAVE_AS_DRAFT_SUBACTION, SaveContentAsDraftActionlet.class.getName(),
                UNLOCK_SUBACTION, CheckinContentActionlet.class.getName());

        // Get the list of Actionlets from the 'Save' Step in the 'System Workflow'
        final WorkflowScheme systemWorkflow = workflowAPI.findScheme(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID);
        final List<WorkflowStep> workflowSteps = workflowAPI.findSteps(systemWorkflow);
        assertFalse("There must be at least one Step for the System Workflow", workflowSteps.isEmpty());
        final WorkflowStep firstWorkflowStep = workflowSteps.get(0);
        final List<WorkflowAction> workflowActions = workflowAPI.findActions(firstWorkflowStep, user);
        assertFalse("There must be at least one Action for the System Workflow", workflowActions.isEmpty());
        final List<WorkflowActionClass> actionClasses =
                workflowAPI.findActionClasses(workflowActions.get(0));
        assertEquals(String.format("Workflow Step '%s' must have 2 Workflow Actionlets", firstWorkflowStep.getName()),
                2, actionClasses.size());
        for (final WorkflowActionClass actionClass : actionClasses) {
            final WorkFlowActionlet workFlowActionlet = workflowAPI.findActionlet(actionClass.getClazz());

            // ╔══════════════╗
            // ║  Assertions  ║
            // ╚══════════════╝
            assertNotNull(String.format("Actionlet '%s' cannot be null", actionClass.getClazz()), workFlowActionlet);
            assertNotNull(String.format("Actionlet '%s' is not the expected one", workFlowActionlet.getName()),
                    expectedActionletData.get(workFlowActionlet.getName()));
            assertEquals(String.format("Actionlet class '%s' is not the expected one", actionClass.getClazz()),
                    expectedActionletData.get(workFlowActionlet.getName()), actionClass.getClazz());
        }
    }


    /**
     * Method to test: This test tries the {@link WorkflowAPIImpl#findWorkflowTaskFilesAsContent(WorkflowTask, User)}
     * Given Scenario: Attaching a file to a workflow task when it is a dotAsset
     * ExpectedResult: The addition of the dotAsset should be made without throwing exceptions and it also should add a new "fileLink" property.
     */
    @Test
    public void testAttachDotAssetToWorkflowTask()
            throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();
        final Language language = APILocator.getLanguageAPI().getDefaultLanguage();
        //final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

        Contentlet contentlet = null;

        try {
            contentlet = TestDataUtils.getPageContent(true, language.getId());

            //save workflow task
            final WorkflowStep workflowStep = workflowAPI.findStep(
                    SystemWorkflowConstants.WORKFLOW_NEW_STEP_ID);
            workflowAPI.deleteWorkflowTaskByContentletIdAnyLanguage(contentlet, systemUser);
            final WorkflowTask workflowTask = workflowAPI
                    .createWorkflowTask(contentlet, systemUser, workflowStep, "test", "test");
            workflowAPI.saveWorkflowTask(workflowTask);



            //save workflow task dot asset file
            final Contentlet fileAsset = TestDataUtils.getDotAssetLikeContentlet();
            workflowAPI.attachFileToTask(workflowTask, fileAsset.getInode());

            HttpServletRequestThreadLocal.INSTANCE.setRequest(new MockInternalRequest().request());

            List<IFileAsset> files = workflowAPI.findWorkflowTaskFilesAsContent(workflowTask, systemUser);

            assertNotNull(((FileAsset) files.get(0)).getMap().get("fileLink"));

        } finally {
            if (contentlet != null && contentlet.getInode() != null) {
                ContentletDataGen.destroy(contentlet);
            }

        }

    }

}
