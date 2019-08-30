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
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
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
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This class verifies the correct behavior of the {@link FourEyeApproverActionlet}, which requires
 * a specific number of users to approve a content BEFORE it can be published. The workflow action
 * that triggers this action should have the following actionlets in it:
 * <ol>
 * <li>Unlock Content.</li>
 * <li>'4 Eye' Approval.</li>
 * <li>Publish Content.</li>
 * </ol>
 * This ensures that a contentlet will be correctly published after the required approvers have
 * accepted the new content. This is how the Multiple Approver action works too.
 *
 * @author Jose Castro
 * @version 4.3.0
 * @since Jan 10, 2018
 */
public class FourEyeApproverActionletTest extends BaseWorkflowIntegrationTest {

    private static WorkflowAPI workflowAPI;
    private static ContentletAPI contentletAPI;
    private static LanguageAPI languageAPI;
    private static ContentTypeAPI contentTypeAPI;

    private static User systemUser;
    private static CreateSchemeStepActionResult schemeStepActionResult = null;
    private static ContentType type = null;
    private static User publisher1;
    private static User publisher2;
    private static User contributor1;
    private static Host site;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting up the web app environment
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();

        workflowAPI = APILocator.getWorkflowAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        contentletAPI = APILocator.getContentletAPI();
        languageAPI = APILocator.getLanguageAPI();

        site = new SiteDataGen().nextPersisted();
        // Get the test role and two users from such a role
        final Role publisherRole = TestUserUtils.getOrCreatePublisherRole(site);
        publisher1 = TestUserUtils.getChrisPublisherUser(site);
        publisher2 = TestUserUtils
                .getUser(publisherRole, "daniel@dotcms.com", "Daniel", "dotCMS", "daniel");

        contributor1 = TestUserUtils.getJoeContributorUser(site);

        // Create the scheme and actions. This method allows you to add just one sub-action
        // in the beginning
        final long sysTime = System.currentTimeMillis();
        schemeStepActionResult = createSchemeStepActionActionlet
                ("itFourEyeApprovalScheme_" + sysTime, "step1", "action1",
                        CheckinContentActionlet.class);
        // Set the role ID of the people who can use the action
        addWhoCanUseToAction(schemeStepActionResult.getAction(),
                Collections.singletonList(publisherRole.getId()));
        // Add the remaining two sub-actions for this test
        addActionletToAction(schemeStepActionResult.getAction().getId(),
                FourEyeApproverActionlet.class, 1);
        addActionletToAction(schemeStepActionResult.getAction().getId(),
                PublishContentActionlet.class, 2);
        // Add the required parameters to the '4-eyes' sub-action
        final List<WorkflowActionClass> actionletClasses = getActionletsFromAction(
                schemeStepActionResult.getAction());
        WorkflowActionClass workflowActionClass = actionletClasses.get(1);
        addParameterValuesToActionlet(workflowActionClass,
                Arrays.asList("chris@dotcms.com,daniel@dotcms.com", "2",
                        "'4 Eye' Approval Required", "Please review this content."));

        // Create the content type to trigger the scheme
        createTestContentType();

        // Associate the scheme to the content type
        workflowAPI.saveSchemesForStruct(new StructureTransformer(type).asStructure(),
                Collections.singletonList(schemeStepActionResult.getScheme()));
    }

    /**
     * Creates the test Content Type
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private static void createTestContentType()
            throws DotDataException, DotSecurityException {
        type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .description("Content Type for testing the 4-Eye Approval actionlet.")
                        .name("FourEyeActionletTest").owner(APILocator.systemUser().toString())
                        .variable("FourEyeActionletTest").build());
        final List<Field> fields = new ArrayList<>(type.fields());
        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        type = contentTypeAPI.save(type, fields);
    }

    @Test
    public void contentApprovalWithTwoApprovers()
            throws DotSecurityException, DotDataException {

        Contentlet contentletToCleanUp = null;

        try {
            // Create a contentlet first and save it
            final long languageId = languageAPI.getDefaultLanguage().getId();
            final Contentlet cont = new Contentlet();
            cont.setContentTypeId(type.id());
            cont.setOwner(APILocator.systemUser().toString());
            cont.setModDate(new Date());
            cont.setLanguageId(languageId);
            cont.setStringProperty("title", "4-Eye Approval Test Title");
            cont.setStringProperty("txt", "4-Eye Approval Test Text");
            cont.setHost(site.getIdentifier());
            cont.setIndexPolicy(IndexPolicy.WAIT_FOR);
            cont.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            Contentlet contentlet1 = contentletAPI.checkin(cont, systemUser, false);
            Assert.assertFalse("The contentlet cannot be live, it has just been created.",
                    contentlet1.isLive());
            //Assign permissions
            final int contentPermissions = (PermissionAPI.PERMISSION_READ
                    | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_PUBLISH);

            APILocator.getPermissionAPI().save(
                    new Permission(contentlet1.getPermissionId(),
                            TestUserUtils.getOrCreatePublisherRole().getId(),
                            contentPermissions),
                    contentlet1, APILocator.systemUser(), false);

            // Set the appropriate workflow action to the contentlet
            contentlet1.setActionId(
                    schemeStepActionResult.getAction().getId());
            contentlet1.setStringProperty("title", "Test Save");
            contentlet1.setStringProperty("txt", "Test Save Text");

            // Triggering the four eyes action
            WorkflowProcessor processor =
                    workflowAPI.fireWorkflowPreCheckin(contentlet1, publisher1);
            workflowAPI.fireWorkflowPostCheckin(processor);
            Contentlet processedContentlet = processor.getContentlet();

            // The contentlet MUST NOT be live yet, it needs one more approval
            contentletAPI.isInodeIndexed(processedContentlet.getInode(), 6);
            ContentletVersionInfo contentletVersionInfo = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(processedContentlet.getIdentifier(), languageId);
            Assert.assertNotNull(contentletVersionInfo);
            Assert.assertNull("The contentlet cannot be live, it needs 1 more approver.",
                    contentletVersionInfo.getLiveInode());
            Assert.assertNotNull("The contentlet should be working, it needs 1 more approver.",
                    contentletVersionInfo.getWorkingInode());

            // Triggering the four eyes action
            Contentlet contentlet2 = contentletAPI
                    .find(contentletVersionInfo.getWorkingInode(), systemUser, false);
            contentlet2.setActionId(
                    schemeStepActionResult.getAction().getId());
            processor = workflowAPI.fireWorkflowPreCheckin(contentlet2, publisher2);
            workflowAPI.fireWorkflowPostCheckin(processor);
            processedContentlet = processor.getContentlet();

            // The contentlet MUST be live now as it has been approved by another user
            contentletAPI.isInodeIndexed(processedContentlet.getInode(), true, 6);
            contentletVersionInfo = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(processedContentlet.getIdentifier(), languageId);
            Assert.assertNotNull(contentletVersionInfo);
            Assert.assertNotNull("The contentlet MUST be live, it has all the approvers",
                    contentletVersionInfo.getLiveInode());
            Assert.assertNotNull("The contentlet should be working also.",
                    contentletVersionInfo.getWorkingInode());

            Contentlet contentlet3 = contentletAPI
                    .findContentletByIdentifier(processedContentlet.getIdentifier(),
                            true, languageId, systemUser, false);
            Assert.assertNotNull(contentlet3);
            Assert.assertTrue("The contentlet MUST be live, it has all the approvers.",
                    contentlet3.isLive());

            //For clean up
            contentletToCleanUp = contentlet3;
        } finally {
            // Cleanup
            if (null != contentletToCleanUp) {

                contentletAPI.destroy(contentletToCleanUp, systemUser, false);
            }
        }

    }

    @Test
    public void contentApprovalWithOneApprover()
            throws DotSecurityException, DotDataException, InterruptedException {
        // Create a contentlet first and save it
        final long languageId = languageAPI.getDefaultLanguage().getId();
        final Contentlet cont = new Contentlet();
        cont.setContentTypeId(type.id());
        cont.setOwner(APILocator.systemUser().toString());
        cont.setModDate(new Date());
        cont.setLanguageId(languageId);
        cont.setStringProperty("title", "4-Eye Approval Test Title");
        cont.setStringProperty("txt", "4-Eye Approval Test Text");
        cont.setHost(site.getIdentifier());
        final Contentlet contentlet1 = contentletAPI.checkin(cont, systemUser, false);
        Assert.assertFalse("The contentlet cannot be live, it has just been created.",
                contentlet1.isLive());
        //Assign permissions
        final int contentPermissions = (PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_PUBLISH);

        APILocator.getPermissionAPI().save(
                new Permission(contentlet1.getPermissionId(),
                        TestUserUtils.getOrCreatePublisherRole().getId(),
                        contentPermissions),
                contentlet1, APILocator.systemUser(), false);

        // Set the appropriate workflow action to the contentlet
        contentlet1.setActionId(
                schemeStepActionResult.getAction().getId());
        contentlet1.setStringProperty("title", "Test Save");
        contentlet1.setStringProperty("txt", "Test Save Text");

        // Triggering the save content action
        WorkflowProcessor processor =
                workflowAPI.fireWorkflowPreCheckin(contentlet1, publisher1);
        workflowAPI.fireWorkflowPostCheckin(processor);

        // The contentlet MUST NOT be live yet, it needs one more approval
        final Contentlet contentlet2 = contentletAPI
                .findContentletByIdentifier(contentlet1.getIdentifier(),
                        false, languageId, systemUser, false);
        Assert.assertFalse("The contentlet cannot be live, it needs 1 more approver.",
                contentlet2.isLive());

        // Cleanup
        contentletAPI.destroy(contentlet2, systemUser, false);
    }

    @Test
    public void unauthorizedUserTriggeringWorkflowAction()
            throws DotSecurityException, DotDataException {
        // Create a contentlet first and save it
        final long languageId = languageAPI.getDefaultLanguage().getId();
        final Contentlet cont = new Contentlet();
        cont.setContentTypeId(type.id());
        cont.setOwner(APILocator.systemUser().toString());
        cont.setModDate(new Date());
        cont.setLanguageId(languageId);
        cont.setStringProperty("title", "4-Eye Approval Test Title");
        cont.setStringProperty("txt", "4-Eye Approval Test Text");
        cont.setHost(site.getIdentifier());
        final Contentlet contentlet1 = contentletAPI.checkin(cont, systemUser, false);
        Assert.assertFalse("The contentlet cannot be live, it has just been created.",
                contentlet1.isLive());

        contentlet1.setActionId(
                schemeStepActionResult.getAction().getId());
        contentlet1.setStringProperty("title", "Test Save");
        contentlet1.setStringProperty("txt", "Test Save Text");

        // Expect the correct 'user cannot read' exception
        boolean isErrorExpected = false;
        final String expectedErrorMsg = String
                .format("User %s [ID: %s][email:%s] cannot read action action1",
                        contributor1.getFirstName() + " " + contributor1.getLastName(),
                        contributor1.getUserId(), contributor1.getEmailAddress());
        try {
            // Triggering the save content action with a non-authorized user
            workflowAPI.fireWorkflowPreCheckin(contentlet1, contributor1);
        } catch (Exception e) {
            // Get the expected error message that validates if user can use the workflow action
            final String errorMsg = e.getCause().getCause().getMessage();
            isErrorExpected = expectedErrorMsg.equalsIgnoreCase(errorMsg);
        }

        // Cleanup
        contentletAPI.destroy(contentlet1, systemUser, false);

        Assert.assertTrue(
                "The root cause of the exception IS NOT the expected error. Please check this test.",
                isErrorExpected);
    }

    /**
     * Removes the test contentlet, workflow, and content type.
     */
    @AfterClass
    public static void cleanup()
            throws DotDataException, DotSecurityException, AlreadyExistException {
        if (null != type) {
            contentTypeAPI.delete(type);
        }
        if (null != schemeStepActionResult) {
            cleanScheme(schemeStepActionResult.getScheme());
        }
    }

}
