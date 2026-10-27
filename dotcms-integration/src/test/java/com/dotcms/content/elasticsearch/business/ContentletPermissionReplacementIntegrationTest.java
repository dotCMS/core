package com.dotcms.content.elasticsearch.business;

import static com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID;
import static com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.api.v1.workflow.WorkflowResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.form.FireActionForm;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.ImportUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the permission list a caller can pass when saving, publishing, checking in
 * or copying a contentlet: a list that differs from the contentlet's current permissions requires
 * permission to edit permissions (on the contentlet or on its content type), while no list or an
 * unchanged list leaves the permission rows as they are.
 */
public class ContentletPermissionReplacementIntegrationTest {

    private static final String PASSWORD = "pw" + uniqueSuffix();

    private static final List<Contentlet> contentlets = new ArrayList<>();
    private static final List<ContentType> contentTypes = new ArrayList<>();
    private static final List<Host> sites = new ArrayList<>();
    private static final List<User> users = new ArrayList<>();
    private static final List<Role> roles = new ArrayList<>();

    private static User systemUser;
    private static ContentletAPI contentletAPI;
    private static WorkflowAPI workflowAPI;
    private static PermissionAPI permissionAPI;
    private static RoleAPI roleAPI;

    /** Commit listener mode found before this class ran, restored when it finishes. */
    private static boolean asyncCommitListenersBefore;

    /** Role given to every firing user: may use the System Workflow Save and Publish actions. */
    private static Role actorRole;
    /** Role that is not part of the contentlet's permissions until a caller requests it. */
    private static Role extraRole;
    /** Site shared by the cases that do not change site permissions. */
    private static Host site;
    /** Content type shared by the cases that do not change content type permissions. */
    private static ContentType contentType;

    /**
     * Initializes the integration environment, makes commit listeners run synchronously so the
     * permission rows can be read right after each call, and creates the shared site, content type
     * and roles.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        asyncCommitListenersBefore = HibernateUtil.getAsyncCommitListenersFinalization();
        HibernateUtil.setAsyncCommitListenersFinalization(false);

        systemUser = APILocator.systemUser();
        contentletAPI = APILocator.getContentletAPI();
        workflowAPI = APILocator.getWorkflowAPI();
        permissionAPI = APILocator.getPermissionAPI();
        roleAPI = APILocator.getRoleAPI();

        actorRole = newRole();
        extraRole = newRole();

        for (final String actionId : List.of(WORKFLOW_SAVE_ACTION_ID, WORKFLOW_PUBLISH_ACTION_ID)) {
            final WorkflowAction action = workflowAPI.findAction(actionId, systemUser);
            permissionAPI.save(new Permission(action.getPermissionId(), actorRole.getId(),
                    PermissionAPI.PERMISSION_USE), action, systemUser, false);
        }

        site = newSite();
        grantIndividual(site, actorRole,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN);
        contentType = newContentType(site);
    }

    /**
     * Removes every contentlet, content type, site, user and role created by this class, and the
     * permission rows it added on the System Workflow Save and Publish actions, then restores the
     * commit listener mode found before the class ran.
     */
    @AfterClass
    public static void cleanup() {
        try {
            contentlets.forEach(contentlet -> ContentletDataGen.destroy(contentlet, true));
            contentTypes.forEach(ContentTypeDataGen::remove);
            for (final Host createdSite : sites) {
                try {
                    APILocator.getHostAPI().archive(createdSite, systemUser, false);
                    APILocator.getHostAPI().delete(createdSite, systemUser, false);
                } catch (final Exception e) {
                    Logger.warn(ContentletPermissionReplacementIntegrationTest.class,
                            "Unable to remove site " + createdSite.getIdentifier() + ": " + e.getMessage());
                }
            }
            users.forEach(UserDataGen::remove);
            removeActorRoleFromSystemActions();
            roles.forEach(RoleDataGen::remove);
        } finally {
            HibernateUtil.setAsyncCommitListenersFinalization(asyncCommitListenersBefore);
        }
    }

    /**
     * Removes the USE rows {@link #prepare()} added for {@link #actorRole} on the System Workflow
     * Save and Publish actions. Saving a row with no bits deletes it.
     */
    private static void removeActorRoleFromSystemActions() {
        if (null == actorRole) {
            return;
        }
        for (final String actionId : List.of(WORKFLOW_SAVE_ACTION_ID, WORKFLOW_PUBLISH_ACTION_ID)) {
            try {
                final WorkflowAction action = workflowAPI.findAction(actionId, systemUser);
                permissionAPI.save(new Permission(action.getPermissionId(), actorRole.getId(), 0, true),
                        action, systemUser, false);
            } catch (final Exception e) {
                Logger.warn(ContentletPermissionReplacementIntegrationTest.class,
                        "Unable to remove the permissions on action " + actionId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Method to test: {@link WorkflowAPI#fireContentWorkflow(Contentlet, ContentletDependencies)}
     * Given Scenario: A user with READ and WRITE (no permission to edit permissions) fires Save
     * without a permission list
     * ExpectedResult: The content is saved and the permission rows are unchanged
     */
    @Test
    public void fireSaveWithoutPermissionList_withoutEditPermissions_keepsRows() throws Exception {
        final User editor = newBackEndUser();
        final Contentlet contentlet = newContentlet(contentType, site);
        grantIndividual(contentlet, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);
        final Map<String, Integer> rowsBefore = individualRows(contentlet);
        final String workingInodeBefore = workingInode(contentlet);

        final Contentlet saved = fire(contentlet, WORKFLOW_SAVE_ACTION_ID, editor, null);

        assertNotNull(saved);
        assertNotEquals("A new version is expected", workingInodeBefore, workingInode(contentlet));
        assertEquals(rowsBefore, individualRows(contentlet));
    }

    /**
     * Method to test: {@link WorkflowAPI#fireContentWorkflow(Contentlet, ContentletDependencies)}
     * Given Scenario: A user with READ and WRITE (no permission to edit permissions) fires Save, which
     * runs {@code SaveContentAsDraftActionlet}, with a permission list that adds a role
     * ExpectedResult: The actionlet fails with a {@link WorkflowActionFailureException} caused by a
     * {@link DotSecurityException}; the rows, versions and workflow step are unchanged
     */
    @Test
    public void fireSaveWithChangedList_withoutEditPermissions_isRefusedAndRolledBack() throws Exception {
        final User editor = newBackEndUser();
        final Contentlet contentlet = newContentlet(contentType, site);
        grantIndividual(contentlet, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);

        assertChangedListIsRefused(contentlet, WORKFLOW_SAVE_ACTION_ID, editor);
    }

    /**
     * Method to test: {@link WorkflowAPI#fireContentWorkflow(Contentlet, ContentletDependencies)}
     * Given Scenario: A user with READ, WRITE and PUBLISH (no permission to edit permissions) fires
     * Publish, which runs {@code SaveContentActionlet}, with a permission list that adds a role
     * ExpectedResult: The actionlet fails with a {@link WorkflowActionFailureException} caused by a
     * {@link DotSecurityException}; the rows, versions and workflow step are unchanged
     */
    @Test
    public void firePublishWithChangedList_withoutEditPermissions_isRefusedAndRolledBack() throws Exception {
        final User editor = newBackEndUser();
        final Contentlet contentlet = newContentlet(contentType, site);
        grantIndividual(contentlet, userRole(editor), PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH);

        assertChangedListIsRefused(contentlet, WORKFLOW_PUBLISH_ACTION_ID, editor);
    }

    /**
     * Method to test: {@link WorkflowAPI#fireContentWorkflow(Contentlet, ContentletDependencies)}
     * Given Scenario: A user with READ and WRITE (no permission to edit permissions) fires Save with a
     * permission list that leaves out a role the contentlet has a row for
     * ExpectedResult: The actionlet fails with a {@link WorkflowActionFailureException} caused by a
     * {@link DotSecurityException}; the rows, versions and workflow step are unchanged
     */
    @Test
    public void fireSaveWithListMissingARole_withoutEditPermissions_isRefusedAndRolledBack() throws Exception {
        final User editor = newBackEndUser();
        final Contentlet contentlet = newContentlet(contentType, site);
        grantIndividual(contentlet, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);
        grantIndividual(contentlet, extraRole, PermissionAPI.PERMISSION_READ);
        final Map<String, Integer> requested = new HashMap<>(individualRows(contentlet));
        requested.remove(extraRole.getId());

        assertListIsRefused(contentlet, WORKFLOW_SAVE_ACTION_ID, editor, requested);
    }

    /**
     * Method to test: {@link WorkflowAPI#fireContentWorkflow(Contentlet, ContentletDependencies)}
     * Given Scenario: A user with READ, WRITE and PUBLISH (no permission to edit permissions) fires
     * Publish with a permission list that drops WRITE from the user's own row
     * ExpectedResult: The actionlet fails with a {@link WorkflowActionFailureException} caused by a
     * {@link DotSecurityException}; the rows, versions and workflow step are unchanged
     */
    @Test
    public void firePublishWithListLoweringOwnRow_withoutEditPermissions_isRefusedAndRolledBack()
            throws Exception {
        final User editor = newBackEndUser();
        final Role editorRole = userRole(editor);
        final Contentlet contentlet = newContentlet(contentType, site);
        grantIndividual(contentlet, editorRole, PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH);
        final Map<String, Integer> requested = new HashMap<>(individualRows(contentlet));
        requested.put(editorRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_PUBLISH);

        assertListIsRefused(contentlet, WORKFLOW_PUBLISH_ACTION_ID, editor, requested);
    }

    /**
     * Method to test: {@link WorkflowAPI#fireContentWorkflow(Contentlet, ContentletDependencies)}
     * Given Scenario: A contentlet without its own rows inherits READ and WRITE from its site; a user
     * with those inherited permissions fires Save with a permission list that adds a role
     * ExpectedResult: The call is refused and the contentlet still has no rows of its own
     */
    @Test
    public void fireSaveWithChangedList_onInheritingContentlet_withoutEditPermissions_createsNoRows()
            throws Exception {
        final User editor = newBackEndUser();
        final Host inheritingSite = newSite();
        grantIndividual(inheritingSite, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN);
        grantInheritableForContent(inheritingSite, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);
        final Contentlet contentlet = newContentlet(contentType, inheritingSite);
        assertTrue(individualRows(contentlet).isEmpty());

        assertChangedListIsRefused(contentlet, WORKFLOW_SAVE_ACTION_ID, editor);
        assertTrue(individualRows(contentlet).isEmpty());
    }

    /**
     * Method to test: {@link WorkflowAPI#fireContentWorkflow(Contentlet, ContentletDependencies)}
     * Given Scenario: A user whose permission to edit permissions comes only from the contentlet's
     * own rows fires Save with a permission list that adds a role
     * ExpectedResult: The rows are replaced by the requested list
     */
    @Test
    public void ownRowsWithEditPermissions_replaceRows() throws Exception {
        final User owner = newBackEndUser();
        final Contentlet contentlet = newContentlet(contentType, site);
        final int ownerBits = PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE
                | PermissionAPI.PERMISSION_EDIT_PERMISSIONS;
        grantIndividual(contentlet, userRole(owner), ownerBits);
        final Map<String, Integer> requested = Map.of(
                userRole(owner).getId(), ownerBits,
                extraRole.getId(), PermissionAPI.PERMISSION_READ);

        fire(contentlet, WORKFLOW_SAVE_ACTION_ID, owner, toPermissions(requested));

        assertRowsContain(requested, individualRows(contentlet));
    }

    /**
     * Method to test: {@link WorkflowAPI#fireContentWorkflow(Contentlet, ContentletDependencies)}
     * Given Scenario: A contentlet without its own rows; the user's permission to edit permissions is
     * inherited from the site; the user fires Save with a permission list that adds a role
     * ExpectedResult: The contentlet gets rows equal to the requested list
     */
    @Test
    public void editPermissionsInheritedFromSite_setsRows() throws Exception {
        final User manager = newBackEndUser();
        final Host managedSite = newSite();
        final int managerBits = PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE
                | PermissionAPI.PERMISSION_EDIT_PERMISSIONS;
        grantIndividual(managedSite, userRole(manager),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN);
        grantInheritableForContent(managedSite, userRole(manager), managerBits);
        final Contentlet contentlet = newContentlet(contentType, managedSite);
        assertTrue(individualRows(contentlet).isEmpty());
        final Map<String, Integer> requested = Map.of(
                userRole(manager).getId(), managerBits,
                extraRole.getId(), PermissionAPI.PERMISSION_READ);

        fire(contentlet, WORKFLOW_SAVE_ACTION_ID, manager, toPermissions(requested));

        assertRowsContain(requested, individualRows(contentlet));
    }

    /**
     * Method to test: {@link WorkflowAPI#fireContentWorkflow(Contentlet, ContentletDependencies)}
     * Given Scenario: A user with READ and WRITE on the contentlet whose permission to edit
     * permissions is granted only on the content type fires Save with a permission list that adds
     * a role
     * ExpectedResult: The rows are replaced by the requested list
     */
    @Test
    public void editPermissionsOnContentTypeOnly_replacesRows() throws Exception {
        final User typeManager = newBackEndUser();
        final ContentType managedType = newContentType(site);
        grantIndividual(managedType, userRole(typeManager),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);
        final Contentlet contentlet = newContentlet(managedType, site);
        final int contentBits = PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE;
        grantIndividual(contentlet, userRole(typeManager), contentBits);
        final Map<String, Integer> requested = Map.of(
                userRole(typeManager).getId(), contentBits,
                extraRole.getId(), PermissionAPI.PERMISSION_READ);

        fire(contentlet, WORKFLOW_SAVE_ACTION_ID, typeManager, toPermissions(requested));

        assertRowsContain(requested, individualRows(contentlet));
    }

    /**
     * Method to test: {@link WorkflowAPI#fireContentWorkflow(Contentlet, ContentletDependencies)}
     * Given Scenario: An administrator fires Save with a permission list that adds a role and the
     * front-end role
     * ExpectedResult: The rows are replaced by the requested list
     */
    @Test
    public void adminUser_replacesRows() throws Exception {
        final User editor = newBackEndUser();
        final Contentlet contentlet = newContentlet(contentType, site);
        grantIndividual(contentlet, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);
        final Map<String, Integer> requested = Map.of(
                userRole(editor).getId(), PermissionAPI.PERMISSION_READ,
                extraRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE,
                roleAPI.loadFrontEndUserRole().getId(), PermissionAPI.PERMISSION_READ);

        fire(contentlet, WORKFLOW_SAVE_ACTION_ID, TestUserUtils.getAdminUser(), toPermissions(requested));

        assertRowsContain(requested, individualRows(contentlet));
    }

    /**
     * Method to test: {@link WorkflowAPI#fireContentWorkflow(Contentlet, ContentletDependencies)}
     * Given Scenario: The system user fires Save with a permission list that adds a role
     * ExpectedResult: The rows are replaced by the requested list
     */
    @Test
    public void systemUser_replacesRows() throws Exception {
        final User editor = newBackEndUser();
        final Contentlet contentlet = newContentlet(contentType, site);
        grantIndividual(contentlet, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);
        final Map<String, Integer> requested = Map.of(
                userRole(editor).getId(), PermissionAPI.PERMISSION_READ,
                extraRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);

        fire(contentlet, WORKFLOW_SAVE_ACTION_ID, systemUser, toPermissions(requested));

        assertRowsContain(requested, individualRows(contentlet));
    }

    /**
     * Method to test: {@link ContentletAPI#copyContentlet(Contentlet, Host, User, boolean)}
     * Given Scenario: A user with READ and WRITE (no permission to edit permissions) copies a
     * contentlet that has its own rows
     * ExpectedResult: The copy has the same rows as the source
     */
    @Test
    public void copyByUserWithoutEditPermissions_carriesSourcePermissions() throws Exception {
        final User editor = newBackEndUser();
        // Creating the copy needs WRITE on the content type
        final ContentType copyType = newContentType(site);
        grantIndividual(copyType, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);
        final Contentlet source = newContentlet(copyType, site);
        grantIndividual(source, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);
        grantIndividual(source, extraRole, PermissionAPI.PERMISSION_READ);
        final Map<String, Integer> sourceRows = individualRows(source);

        final Contentlet copy = contentletAPI.copyContentlet(source, site, editor, false);
        contentlets.add(copy);

        assertNotEquals(source.getIdentifier(), copy.getIdentifier());
        assertEquals(sourceRows, individualRows(copy));
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, ContentletRelationships, List, List, User, boolean)}
     * Given Scenario: A user with READ and WRITE (no permission to edit permissions) checks in a new
     * version passing the contentlet's current permissions
     * ExpectedResult: The check-in succeeds and the rows are unchanged
     */
    @Test
    public void checkinWithCurrentPermissions_withoutEditPermissions_keepsRows() throws Exception {
        final User editor = newBackEndUser();
        final Contentlet contentlet = newContentlet(contentType, site);
        grantIndividual(contentlet, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);
        grantIndividual(contentlet, extraRole, PermissionAPI.PERMISSION_READ);
        final Map<String, Integer> rowsBefore = individualRows(contentlet);

        final Contentlet checkedIn = checkinWithCurrentPermissions(contentlet, editor);

        assertNotNull(checkedIn);
        assertEquals(rowsBefore, individualRows(contentlet));
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, ContentletRelationships, List, List, User, boolean)}
     * Given Scenario: A contentlet without its own rows inherits READ and WRITE from its site; a user
     * with those inherited permissions checks in a new version passing the inherited permissions
     * ExpectedResult: The check-in succeeds and the contentlet still has no rows of its own
     */
    @Test
    public void checkinWithInheritedPermissions_withoutEditPermissions_createsNoRows() throws Exception {
        final User editor = newBackEndUser();
        final Host inheritingSite = newSite();
        grantIndividual(inheritingSite, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN);
        grantInheritableForContent(inheritingSite, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);
        final Contentlet contentlet = newContentlet(contentType, inheritingSite);
        assertTrue(individualRows(contentlet).isEmpty());

        final Contentlet checkedIn = checkinWithCurrentPermissions(contentlet, editor);

        assertNotNull(checkedIn);
        assertTrue(individualRows(contentlet).isEmpty());
    }

    /**
     * Method to test: {@link WorkflowResource#fireActionSinglePart(HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, String, String, String, String, String, FireActionForm)}
     * Given Scenario: A back-end user with READ and WRITE (no permission to edit permissions) fires
     * Save through the REST resource with individual permissions that add a role
     * ExpectedResult: The call fails because of a {@link DotSecurityException}, the response status
     * is 403 and the rows are unchanged
     */
    @Test
    public void fireActionResource_withChangedList_withoutEditPermissions_answersForbidden() throws Exception {
        final User editor = newBackEndUser();
        final Contentlet contentlet = newContentlet(contentType, site);
        grantIndividual(contentlet, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);
        final Map<String, Integer> rowsBefore = individualRows(contentlet);

        final Exception refusal = fireThroughResourceExpectingRefusal(contentlet, editor);

        assertTrue("Expected a DotSecurityException in the cause chain but got: " + refusal,
                ExceptionUtil.causedBy(refusal, DotSecurityException.class));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(),
                ResponseUtil.mapExceptionResponse(refusal).getStatus());
        assertEquals(rowsBefore, individualRows(contentlet));
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, ContentletRelationships, List, List, User, boolean)}
     * Given Scenario: A user with READ and WRITE (no permission to edit permissions) checks in a new
     * version, outside any transaction, with a permission list that adds a role
     * ExpectedResult: The call throws a {@link DotSecurityException} and the rows are unchanged
     */
    @Test
    public void checkinWithChangedList_withoutEditPermissions_isRefused() throws Exception {
        final User editor = newBackEndUser();
        final Contentlet contentlet = newContentlet(contentType, site);
        grantIndividual(contentlet, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);

        assertDirectChangedListIsRefused(contentlet, checkoutFor(contentlet, editor),
                (editable, permissions) -> contentletAPI.checkin(editable,
                        (ContentletRelationships) null, null, permissions, editor, false));
    }

    /**
     * Method to test: {@link ContentletAPI#saveDraft(Contentlet, ContentletRelationships, List, List, User, boolean)}
     * Given Scenario: A user with READ and WRITE (no permission to edit permissions) saves a draft,
     * outside any transaction, with a permission list that adds a role
     * ExpectedResult: The call throws a {@link DotSecurityException} and the rows are unchanged
     */
    @Test
    public void saveDraftWithChangedList_withoutEditPermissions_isRefused() throws Exception {
        final User editor = newBackEndUser();
        final Contentlet contentlet = newContentlet(contentType, site);
        grantIndividual(contentlet, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);

        assertDirectChangedListIsRefused(contentlet, workingCopyFor(contentlet, editor),
                (editable, permissions) -> contentletAPI.saveDraft(editable,
                        (ContentletRelationships) null, null, permissions, editor, false));
    }

    /**
     * Method to test: {@link ContentletAPI#checkinWithoutVersioning(Contentlet, Map, List, List, User, boolean)}
     * Given Scenario: A user with READ and WRITE (no permission to edit permissions) checks in the
     * working version without a new version, outside any transaction, with a permission list that
     * adds a role
     * ExpectedResult: The call throws a {@link DotSecurityException} and the rows are unchanged
     */
    @Test
    public void checkinWithoutVersioningWithChangedList_withoutEditPermissions_isRefused() throws Exception {
        final User editor = newBackEndUser();
        final Contentlet contentlet = newContentlet(contentType, site);
        grantIndividual(contentlet, userRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);

        // The ContentletRelationships overload does not apply a permission list; this one does
        assertDirectChangedListIsRefused(contentlet, workingCopyFor(contentlet, editor),
                (editable, permissions) -> contentletAPI.checkinWithoutVersioning(editable,
                        new HashMap<>(), null, permissions, editor, false));
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, ContentletRelationships, List, List, User, boolean)}
     * Given Scenario: A user with EDIT_PERMISSIONS on the contentlet checks in a new version, outside
     * any transaction, with a permission list that adds a role
     * ExpectedResult: Right after the call the rows equal the requested list
     */
    @Test
    public void checkinWithChangedList_withEditPermissions_replacesRows() throws Exception {
        final User owner = newBackEndUser();
        final Contentlet contentlet = newContentlet(contentType, site);
        final int ownerBits = PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE
                | PermissionAPI.PERMISSION_EDIT_PERMISSIONS;
        grantIndividual(contentlet, userRole(owner), ownerBits);
        final Map<String, Integer> requested = Map.of(
                userRole(owner).getId(), ownerBits,
                extraRole.getId(), PermissionAPI.PERMISSION_READ);

        final Contentlet checkedIn = contentletAPI.checkin(checkoutFor(contentlet, owner),
                (ContentletRelationships) null, null, toPermissions(requested), owner, false);

        assertNotNull(checkedIn);
        assertEquals(requested, individualRows(contentlet));
    }

    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean,
     * User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Given Scenario: A user with READ and WRITE on a content type that has its own permission rows,
     * and no permission to edit permissions anywhere, imports a new row
     * ExpectedResult: The import reports no errors and the new contentlet has no rows of its own
     */
    @Test
    public void importByUserWithoutEditPermissions_newRowKeepsInheriting() throws Exception {
        final User importer = newBackEndUser();
        final ContentType importType = newContentType(site);
        grantIndividual(importType, userRole(importer),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);
        grantIndividual(importType, extraRole, PermissionAPI.PERMISSION_READ);

        final Map<String, List<String>> results = importTitle(importType, importer,
                "title-test-" + uniqueSuffix(), new String[]{});

        assertNoImportErrors(results);
        final List<Contentlet> imported = contentletsOf(importType);
        assertEquals(1, imported.size());
        assertTrue(individualRows(imported.get(0)).isEmpty());
    }

    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean,
     * User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Given Scenario: A user with READ and WRITE (no permission to edit permissions) updates, through
     * a key field, an existing contentlet that has its own rows
     * ExpectedResult: The import reports no errors and the contentlet's rows are unchanged
     */
    @Test
    public void importUpdateByUserWithoutEditPermissions_keepsRowPermissions() throws Exception {
        final User importer = newBackEndUser();
        final ContentType importType = newContentType(site);
        grantIndividual(importType, userRole(importer),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);
        grantIndividual(importType, extraRole, PermissionAPI.PERMISSION_READ);
        final String title = "title-test-" + uniqueSuffix();
        final Contentlet existing = newContentlet(importType, site, title);
        grantIndividual(existing, userRole(importer),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);
        final Map<String, Integer> rowsBefore = individualRows(existing);
        final Field titleField = importType.fieldMap().get("title");

        final Map<String, List<String>> results = importTitle(importType, importer, title,
                new String[]{titleField.id()});

        assertNoImportErrors(results);
        final List<Contentlet> afterImport = contentletsOf(importType);
        assertEquals("The import must update the existing contentlet, not add one",
                1, afterImport.size());
        assertEquals(existing.getIdentifier(), afterImport.get(0).getIdentifier());
        assertNotEquals("The import must write a new version of the existing contentlet",
                existing.getInode(), afterImport.get(0).getInode());
        assertEquals(rowsBefore, individualRows(existing));
    }

    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean,
     * User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Given Scenario: A user with EDIT_PERMISSIONS on a content type that has its own permission rows
     * imports a new row
     * ExpectedResult: The import reports no errors and the new contentlet's rows include the
     * content type's permissions
     */
    @Test
    public void importByUserWithEditPermissionsOnContentType_appliesTypePermissions() throws Exception {
        final User importer = newBackEndUser();
        final ContentType importType = newContentType(site);
        grantIndividual(importType, userRole(importer), PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);
        grantIndividual(importType, extraRole, PermissionAPI.PERMISSION_READ);
        final Map<String, Integer> typeRows = individualRows(importType);

        final Map<String, List<String>> results = importTitle(importType, importer,
                "title-test-" + uniqueSuffix(), new String[]{});

        assertNoImportErrors(results);
        final List<Contentlet> imported = contentletsOf(importType);
        assertEquals(1, imported.size());
        assertRowsContain(typeRows, individualRows(imported.get(0)));
    }

    /**
     * Fires the action with a permission list that adds {@link #extraRole} to the contentlet's
     * current rows and asserts the call is refused and nothing about the contentlet changed.
     */
    private static void assertChangedListIsRefused(final Contentlet contentlet, final String actionId,
            final User user) throws DotDataException, DotSecurityException {

        final Map<String, Integer> requested = new HashMap<>(individualRows(contentlet));
        requested.merge(extraRole.getId(), PermissionAPI.PERMISSION_READ, (current, added) -> current | added);

        assertListIsRefused(contentlet, actionId, user, requested);
    }

    /**
     * Fires the action with the given permission list and asserts the save actionlet fails with a
     * {@link WorkflowActionFailureException} caused by a {@link DotSecurityException}, and nothing
     * about the contentlet changed.
     */
    private static void assertListIsRefused(final Contentlet contentlet, final String actionId,
            final User user, final Map<String, Integer> requested)
            throws DotDataException, DotSecurityException {

        final Map<String, Integer> rowsBefore = individualRows(contentlet);
        final ContentletState stateBefore = ContentletState.of(contentlet);

        try {
            fire(contentlet, actionId, user, toPermissions(requested));
            fail("A changed permission list requires permission to edit permissions");
        } catch (final Exception e) {
            final WorkflowActionFailureException failure = findCause(e, WorkflowActionFailureException.class);
            assertNotNull("Expected the save actionlet to fail but got: " + e, failure);
            assertTrue("Expected a DotSecurityException behind the actionlet failure but got: " + failure,
                    isCausedBySecurityException(failure.getCause()));
        }

        assertEquals(rowsBefore, individualRows(contentlet));
        assertEquals(stateBefore, ContentletState.of(contentlet));
    }

    /**
     * Checks out the contentlet and fires the given action with the given user and, when not
     * {@code null}, the given permission list.
     */
    private static Contentlet fire(final Contentlet contentlet, final String actionId, final User user,
            final List<Permission> permissions) throws DotDataException, DotSecurityException {

        final Contentlet checkout = contentletAPI.checkout(contentlet.getInode(), user, false);
        checkout.setIndexPolicy(IndexPolicy.FORCE);
        final ContentletDependencies.Builder dependencies = new ContentletDependencies.Builder()
                .workflowActionId(actionId)
                .modUser(user);
        if (null != permissions) {
            dependencies.permissions(permissions);
        }
        return workflowAPI.fireContentWorkflow(checkout, dependencies.build());
    }

    /**
     * Checks in a new version of the contentlet passing the permissions it currently has.
     */
    private static Contentlet checkinWithCurrentPermissions(final Contentlet contentlet, final User user)
            throws DotDataException, DotSecurityException {

        final List<Permission> currentPermissions = permissionAPI.getPermissions(contentlet);
        final Contentlet checkout = contentletAPI.checkout(contentlet.getInode(), user, false);
        checkout.setIndexPolicy(IndexPolicy.FORCE);
        return contentletAPI.checkin(checkout, (ContentletRelationships) null, null,
                currentPermissions, user, false);
    }

    /**
     * Fires Save through {@link WorkflowResource} authenticated as the given user, with individual
     * permissions that grant READ to {@link #extraRole}, and returns the exception the call fails
     * with. Fails the test when the call succeeds.
     */
    private static Exception fireThroughResourceExpectingRefusal(final Contentlet contentlet,
            final User user) {
        final FireActionForm form = new FireActionForm.Builder()
                .individualPermissions(Map.of(PermissionAPI.Type.READ, List.of(extraRole.getId())))
                .build();
        final int status;
        try {
            status = new WorkflowResource().fireActionSinglePart(requestAs(user), new EmptyHttpResponse(),
                    WORKFLOW_SAVE_ACTION_ID, contentlet.getInode(), null, "FORCE", "-1", form)
                    .getStatus();
        } catch (final Exception e) {
            return e;
        }
        throw new AssertionError("A changed permission list requires permission to edit permissions,"
                + " but the call answered " + status);
    }

    /**
     * Calls a save method with a permission list that adds {@link #extraRole} to the contentlet's
     * current rows and asserts the call throws a {@link DotSecurityException} itself, not wrapped,
     * and the rows are unchanged.
     */
    private static void assertDirectChangedListIsRefused(final Contentlet contentlet,
            final Contentlet editable, final SaveWithPermissions call) throws DotDataException {

        final Map<String, Integer> rowsBefore = individualRows(contentlet);
        final Map<String, Integer> requested = new HashMap<>(rowsBefore);
        requested.merge(extraRole.getId(), PermissionAPI.PERMISSION_READ, (current, added) -> current | added);

        try {
            call.save(editable, toPermissions(requested));
            fail("A changed permission list requires permission to edit permissions");
        } catch (final DotSecurityException expected) {
            // the refusal reaches the caller as thrown by the contentlet API
        }

        assertEquals(rowsBefore, individualRows(contentlet));
    }

    /**
     * Checks out the contentlet for the given user, ready to be checked in as a new version.
     */
    private static Contentlet checkoutFor(final Contentlet contentlet, final User user)
            throws DotDataException, DotSecurityException {
        final Contentlet checkout = contentletAPI.checkout(contentlet.getInode(), user, false);
        checkout.setIndexPolicy(IndexPolicy.FORCE);
        return checkout;
    }

    /**
     * Returns an editable copy of the contentlet's working version that keeps its inode, so it can be
     * saved as a draft or checked in without a new version.
     */
    private static Contentlet workingCopyFor(final Contentlet contentlet, final User user)
            throws DotDataException, DotSecurityException {
        final Contentlet copy = checkoutFor(contentlet, user);
        copy.setInode(contentlet.getInode());
        return copy;
    }

    /**
     * Imports one CSV row with the given title into the content type on {@link #site}, as the given
     * user, without a workflow action, and returns the import results.
     */
    private static Map<String, List<String>> importTitle(final ContentType type, final User user,
            final String title, final String[] keyFields) throws IOException, DotDataException {
        final Reader reader = new StringReader("title\r\n" + title + "\r\n");
        final CsvReader csvReader = new CsvReader(reader);
        csvReader.setSafetySwitch(false);
        final String[] csvHeaders = csvReader.getHeaders();
        return ImportUtil.importFile(0L, site.getInode(), type.inode(), keyFields, false, false, user,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(), csvHeaders, csvReader, -1, -1,
                reader, null, importRequestAs(user));
    }

    /**
     * Asserts the import results carry no errors.
     */
    private static void assertNoImportErrors(final Map<String, List<String>> results) {
        final List<String> errors = results.get(ImportUtil.KEY_ERRORS);
        assertTrue("Unexpected import errors: " + errors, null == errors || errors.isEmpty());
    }

    /**
     * Returns the working contentlets of the given content type and registers them for cleanup.
     */
    private static List<Contentlet> contentletsOf(final ContentType type)
            throws DotDataException, DotSecurityException {
        final List<Contentlet> found = contentletAPI.findByStructure(type.inode(), systemUser, false, 0, 0);
        contentlets.addAll(found);
        return found;
    }

    /**
     * Builds a request for an import run by the given user.
     */
    private static HttpServletRequest importRequestAs(final User user) {
        final HttpServletRequest request = requestAs(user);
        request.setAttribute(WebKeys.USER, user);
        request.setAttribute(WebKeys.USER_ID, user.getUserId());
        return request;
    }

    /**
     * Builds a request carrying Basic authentication for the given user.
     */
    private static HttpServletRequest requestAs(final User user) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                        .request());
        final String credentials = user.getEmailAddress() + ":" + PASSWORD;
        request.setHeader("Authorization", "Basic "
                + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        return request;
    }

    /**
     * Returns the contentlet's own permission rows read from the database, as role id to the OR of
     * its permission bits.
     */
    private static Map<String, Integer> individualRows(final Permissionable permissionable)
            throws DotDataException {

        final Map<String, Integer> rows = new HashMap<>();
        for (final Permission permission : permissionAPI.getPermissions(permissionable, true, true, true)) {
            rows.merge(permission.getRoleId(), permission.getPermission(), (current, added) -> current | added);
        }
        return rows;
    }

    /**
     * Asserts every requested role is present in the rows with exactly the requested bits.
     */
    private static void assertRowsContain(final Map<String, Integer> requested,
            final Map<String, Integer> rows) {
        requested.forEach((roleId, bits) -> assertEquals("Permission bits for role " + roleId,
                bits, rows.get(roleId)));
    }

    /**
     * Converts role id to bits into a permission list as callers pass it.
     */
    private static List<Permission> toPermissions(final Map<String, Integer> roleBits) {
        final List<Permission> permissions = new ArrayList<>();
        roleBits.forEach((roleId, bits) -> permissions.add(new Permission(null, roleId, bits)));
        return permissions;
    }

    /**
     * Returns whether the throwable or any of its causes is a {@link DotSecurityException}.
     */
    private static boolean isCausedBySecurityException(final Throwable throwable) {
        return null != findCause(throwable, DotSecurityException.class);
    }

    /**
     * Returns the first throwable of the given type in the cause chain, starting with the throwable
     * itself, or {@code null} when there is none.
     */
    private static <T extends Throwable> T findCause(final Throwable throwable, final Class<T> type) {
        Throwable current = throwable;
        while (null != current) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return null;
    }

    /**
     * Saves an individual permission row for the role on the given permissionable.
     */
    private static void grantIndividual(final Permissionable permissionable, final Role role,
            final int bits) throws DotDataException, DotSecurityException {
        permissionAPI.save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                permissionable.getPermissionId(), role.getId(), bits, true), permissionable,
                systemUser, false);
    }

    /**
     * Saves on the site a permission row inherited by the contentlets that live on it.
     */
    private static void grantInheritableForContent(final Host targetSite, final Role role, final int bits)
            throws DotDataException, DotSecurityException {
        permissionAPI.save(new Permission(Contentlet.class.getCanonicalName(),
                targetSite.getPermissionId(), role.getId(), bits, true), targetSite, systemUser, false);
    }

    /**
     * Returns the personal role of the given user.
     */
    private static Role userRole(final User user) throws DotDataException {
        return roleAPI.loadRoleByKey(user.getUserId());
    }

    /**
     * Creates a back-end user that may use the System Workflow Save and Publish actions.
     */
    private static User newBackEndUser() throws DotDataException {
        return newUser(roleAPI.loadBackEndUserRole(), actorRole);
    }

    /**
     * Creates a user with the given roles and the shared test password.
     */
    private static User newUser(final Role... userRoles) {
        final User user = new UserDataGen()
                .emailAddress("user" + uniqueSuffix() + "@dotcms.com")
                .password(PASSWORD)
                .roles(userRoles)
                .nextPersisted();
        users.add(user);
        return user;
    }

    /**
     * Creates a role with a unique name and key.
     */
    private static Role newRole() {
        final String suffix = uniqueSuffix();
        final Role role = new RoleDataGen().name("role" + suffix).key("role" + suffix).nextPersisted();
        roles.add(role);
        return role;
    }

    /**
     * Creates a site.
     */
    private static Host newSite() {
        final Host newSite = new SiteDataGen().nextPersisted();
        sites.add(newSite);
        return newSite;
    }

    /**
     * Creates a content type with one text field on the given site, readable by {@link #actorRole}.
     */
    private static ContentType newContentType(final Host targetSite)
            throws DotDataException, DotSecurityException {
        final String suffix = uniqueSuffix();
        final ContentType type = new ContentTypeDataGen()
                .name("type" + suffix)
                .velocityVarName("type" + suffix)
                .host(targetSite)
                .field(new FieldDataGen().name("title").velocityVarName("title").next())
                .nextPersisted();
        contentTypes.add(type);
        grantIndividual(type, actorRole, PermissionAPI.PERMISSION_READ);
        return type;
    }

    /**
     * Creates a contentlet of the given type on the given site, without permission rows of its own.
     */
    private static Contentlet newContentlet(final ContentType type, final Host targetSite) {
        return newContentlet(type, targetSite, "title-test");
    }

    /**
     * Creates a contentlet of the given type and title on the given site, without permission rows of
     * its own.
     */
    private static Contentlet newContentlet(final ContentType type, final Host targetSite,
            final String title) {
        final Contentlet contentlet = new ContentletDataGen(type.id())
                .host(targetSite)
                .setProperty("title", title)
                .nextPersisted();
        contentlets.add(contentlet);
        return contentlet;
    }

    /**
     * Returns the inode of the contentlet's current working version.
     */
    private static String workingInode(final Contentlet contentlet)
            throws DotDataException, DotSecurityException {
        return contentletAPI.findContentletByIdentifier(contentlet.getIdentifier(), false,
                contentlet.getLanguageId(), systemUser, false).getInode();
    }

    /**
     * Returns a short random suffix for unique names.
     */
    private static String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * A contentlet API save call that receives the contentlet to save and a permission list.
     */
    @FunctionalInterface
    private interface SaveWithPermissions {

        /**
         * Saves the contentlet with the given permission list.
         */
        Contentlet save(Contentlet editable, List<Permission> permissions)
                throws DotDataException, DotSecurityException;
    }

    /**
     * Working version, number of versions and workflow step of a contentlet at a point in time.
     */
    private static final class ContentletState {

        private final String workingInode;
        private final int versionCount;
        private final String workflowStepId;

        private ContentletState(final String workingInode, final int versionCount,
                final String workflowStepId) {
            this.workingInode = workingInode;
            this.versionCount = versionCount;
            this.workflowStepId = workflowStepId;
        }

        /**
         * Reads the current state of the given contentlet.
         */
        static ContentletState of(final Contentlet contentlet) throws DotDataException, DotSecurityException {
            final Contentlet working = contentletAPI.findContentletByIdentifier(contentlet.getIdentifier(),
                    false, contentlet.getLanguageId(), systemUser, false);
            final int versionCount = contentletAPI.findAllVersions(
                    APILocator.getIdentifierAPI().find(contentlet.getIdentifier()), systemUser, false).size();
            final WorkflowTask task = workflowAPI.findTaskByContentlet(working);
            return new ContentletState(working.getInode(), versionCount,
                    null == task ? null : task.getStatus());
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ContentletState)) {
                return false;
            }
            final ContentletState that = (ContentletState) other;
            return versionCount == that.versionCount
                    && Objects.equals(workingInode, that.workingInode)
                    && Objects.equals(workflowStepId, that.workflowStepId);
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return Objects.hash(workingInode, versionCount, workflowStepId);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "ContentletState{workingInode=" + workingInode + ", versionCount=" + versionCount
                    + ", workflowStepId=" + workflowStepId + "}";
        }
    }
}
