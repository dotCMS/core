package com.dotmarketing.portlets.workflows.actionlet;

import static com.dotcms.datagen.TestUserUtils.*;
import static com.dotmarketing.util.WebKeys.DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowHistoryState;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * This class verifies the correct behavior of the {@link ResetApproversActionlet}, which resets 
 * the approval history for workflow tasks. The test validates that after resetting approvals:
 * 1. Content requires re-approval from the original specified approvers
 * 2. Workflow history is properly marked with RESET state
 */
public class ResetApproversActionletTest extends BaseWorkflowIntegrationTest {

    private static WorkflowAPI workflowAPI;
    private static ContentletAPI contentletAPI;
    private static LanguageAPI languageAPI;
    private static ContentTypeAPI contentTypeAPI;

    private static User systemUser;
    private static CreateSchemeStepActionResult approveStepActionResult = null;
    private static CreateSchemeStepActionResult resetStepActionResult = null;
    private static WorkflowStep draftStep = null;
    private static WorkflowStep publishedStep = null;
    private static ContentType type = null;
    private static User approverUser;
    private static User otherUser;
    private static Role adminRole;
    private static Host site;


    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();


        workflowAPI = APILocator.getWorkflowAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        contentletAPI = APILocator.getContentletAPI();
        languageAPI = APILocator.getLanguageAPI();

        final long sysTime = System.currentTimeMillis();

        site = new SiteDataGen().nextPersisted();
        adminRole = TestUserUtils.getOrCreateAdminRole();
        approverUser = TestUserUtils.getAdminUser();
        otherUser = new UserDataGen().roles(APILocator.getRoleAPI().loadCMSAdminRole(), getFrontendRole(), getBackendRole()).emailAddress("otherUser" + sysTime + "@dotcms.com").nextPersisted();



        approveStepActionResult = createSchemeStepActionActionlet(
                "itResetApprovalScheme_" + sysTime, "draft", "approve_action",
                FourEyeApproverActionlet.class);
        
        draftStep = approveStepActionResult.getStep();
        
        // Add PublishContent actionlet as next action to be executed after approval
        addActionletToAction(approveStepActionResult.getAction().getId(),
                PublishContentActionlet.class, 1);
        
        // Configure FourEyeApprover
        final List<WorkflowActionClass> approveActionletClasses = getActionletsFromAction(
                approveStepActionResult.getAction());
        WorkflowActionClass fourEyeActionClass = approveActionletClasses.get(0);
        addParameterValuesToActionlet(fourEyeActionClass,
                Arrays.asList(approverUser.getEmailAddress(), "1",
                        "Approval Required", "Please review this content."));

        List<String> actionPermissions = new ArrayList<>(List.of(adminRole.getId()));
        addWhoCanUseToAction(approveStepActionResult.getAction(), actionPermissions);


        publishedStep = createNewWorkflowStep("published", approveStepActionResult.getScheme().getId());
        
        // Create reset action in the published step that goes back to draft
        resetStepActionResult = createActionActionlet(
                approveStepActionResult.getScheme().getId(), 
                publishedStep.getId(),
                "reset_action", 
                ResetApproversActionlet.class,
                draftStep.getId()); // next step is back to draft
        

        addWhoCanUseToAction(resetStepActionResult.getAction(), actionPermissions);

        WorkflowAction approvalAction = approveStepActionResult.getAction();
        approvalAction.setNextStep(publishedStep.getId());
        workflowAPI.saveAction(approvalAction, null, systemUser);

        createTestContentType();

        workflowAPI.saveSchemesForStruct(new StructureTransformer(type).asStructure(),
                List.of(approveStepActionResult.getScheme()));
    }

    /**
     * Creates the test Content Type
     */
    private static void createTestContentType()
            throws DotDataException, DotSecurityException {
        type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .description("Content Type for testing the Reset Approvers actionlet.")
                        .name("ResetApproversActionletTest").owner(APILocator.systemUser().toString())
                        .variable("ResetApproversActionletTest").build());
        final List<Field> fields = new ArrayList<>(type.fields());
        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        type = contentTypeAPI.save(type, fields);
    }

    /**
     * Method to test: {@link ResetApproversActionlet#executeAction(WorkflowProcessor, Map)}
     * Given Scenario:
     * 1. Content is created and approved by the specified user - content becomes published
     * 2. Content is modified and approved by another user - content remains published (has prior approval)
     * Expected Result:
     * 3. Approvals are reset using ResetApproversActionlet
     * 4. When another user tries to approve again - content should NOT be published and remain in draft, this means it stops the next actions
     * 5. Only when the originally specified approver approves should the content be published
     */
    @Test
    public void testResetApprovalsWorkflow()
            throws DotSecurityException, DotDataException {

        Contentlet contentletToCleanUp = null;

        final boolean defaultContentToDefaultLanguage = Config.getBooleanProperty(
                DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, false);
        try {
            Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, true);
            final long languageId = languageAPI.getDefaultLanguage().getId();
            

            final Contentlet cont = new Contentlet();
            cont.setContentTypeId(type.id());
            cont.setOwner(APILocator.systemUser().toString());
            cont.setModDate(new Date());
            cont.setLanguageId(languageId);
            cont.setStringProperty("title", "Reset Approvals Test Title");
            cont.setStringProperty("txt", "Reset Approvals Test Text");
            cont.setHost(site.getIdentifier());
            cont.setIndexPolicy(IndexPolicy.WAIT_FOR);
            cont.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            Contentlet contentlet = contentletAPI.checkin(cont, systemUser, true);
            

            final int contentPermissions = (PermissionAPI.PERMISSION_READ
                    | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_PUBLISH);
            APILocator.getPermissionAPI().save(
                    new Permission(contentlet.getPermissionId(),
                            adminRole.getId(),
                            contentPermissions),
                    contentlet, APILocator.systemUser(), true);

            // Initial approval by specified user (should publish)
            contentlet.setActionId(approveStepActionResult.getAction().getId());
            contentlet.setStringProperty("title", "Initial Approval");
            WorkflowProcessor processor = workflowAPI.fireWorkflowPreCheckin(contentlet, approverUser);
            workflowAPI.fireWorkflowPostCheckin(processor);
            contentlet = processor.getContentlet();

            // Verify content is published after initial approval
            contentletAPI.isInodeIndexed(contentlet.getInode(), true, 6);
            Optional<ContentletVersionInfo> versionInfo = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(contentlet.getIdentifier(), languageId);
            
            Assert.assertTrue("ContentletVersionInfo should exist", versionInfo.isPresent());
            Assert.assertNotNull("Content should be published after initial approval by specified user",
                    versionInfo.get().getLiveInode());

            // Reset approvals
            contentlet = contentletAPI.find(versionInfo.get().getWorkingInode(), systemUser, false);
            contentlet.setActionId(resetStepActionResult.getAction().getId());
            processor = workflowAPI.fireWorkflowPreCheckin(contentlet, systemUser);
            workflowAPI.fireWorkflowPostCheckin(processor);
            contentlet = processor.getContentlet();

            // Verify approval history now contains RESET state
            List<WorkflowHistory> history = processor.getHistory();
            boolean hasResetState = history.stream().anyMatch(h -> {
                Map<String, Object> changeMap = h.getChangeMap();
                return WorkflowHistoryState.RESET.name().equals(changeMap.get("state"));
            });
            Assert.assertTrue("Workflow history should contain RESET state after reset approvals", hasResetState);

            // Verify content is back in draft step after reset
            Optional<WorkflowStep> currentStep = workflowAPI.findCurrentStep(contentlet);
            Assert.assertEquals("Content should be in draft step after reset", 
                    draftStep.getId(), currentStep.get().getId());

            // Try to approve with a different user (should NOT publish, stay in draft)
            contentlet.setActionId(approveStepActionResult.getAction().getId());
            contentlet.setStringProperty("title", "Post-Reset Approval Non-Specified User");
            processor = workflowAPI.fireWorkflowPreCheckin(contentlet, otherUser);
            workflowAPI.fireWorkflowPostCheckin(processor);
            contentlet = processor.getContentlet();

            
            // Content should still be in draft step (FourEyeApprover should have stopped the workflow)
            Optional<WorkflowStep> stepAfterNonSpecApproval = workflowAPI.findCurrentStep(contentlet);
            Assert.assertEquals("Content should remain in draft step when approved by non-specified user", 
                    draftStep.getId(), stepAfterNonSpecApproval.get().getId());


            // Now approve with the specified user (should finally publish)
            contentlet.setActionId(approveStepActionResult.getAction().getId());
            contentlet.setStringProperty("title", "Final Approval by Specified User");
            processor = workflowAPI.fireWorkflowPreCheckin(contentlet, approverUser);
            workflowAPI.fireWorkflowPostCheckin(processor);
            contentlet = processor.getContentlet();

            contentletAPI.isInodeIndexed(contentlet.getInode(), true, 6);

            Assert.assertNotNull("Content should be published when approved by specified user after reset",
                    versionInfo.get().getLiveInode());

            Contentlet finalContentlet = contentletAPI.findContentletByIdentifier(
                    contentlet.getIdentifier(), true, languageId, systemUser, false);
            Optional<WorkflowStep> finalStep = workflowAPI.findCurrentStep(finalContentlet);
            Assert.assertNotNull("Final published contentlet should exist", finalContentlet);
            Assert.assertTrue("Final contentlet should be live after approval by specified user",
                    finalContentlet.isLive());
            Assert.assertEquals(finalStep.get().getId(), publishedStep.getId());

            contentletToCleanUp = finalContentlet;
            
        } finally {
            // Cleanup
            if (null != contentletToCleanUp) {
                contentletAPI.destroy(contentletToCleanUp, systemUser, false);
            }
            Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE,
                    defaultContentToDefaultLanguage);
        }
    }

    /**
     * Removes the test workflow, and content type.
     */
    @AfterClass
    public static void cleanup()
            throws DotDataException, DotSecurityException, AlreadyExistException {
        if (null != type) {
            contentTypeAPI.delete(type);
        }
        if (null != approveStepActionResult) {
            cleanScheme(approveStepActionResult.getScheme());
        }
    }
}