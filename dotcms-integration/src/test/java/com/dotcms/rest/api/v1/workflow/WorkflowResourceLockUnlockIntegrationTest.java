package com.dotcms.rest.api.v1.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.form.FireActionForm;
import com.dotcms.workflow.form.FireMultipleActionForm;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Covers firing {@link SystemAction#LOCK} and {@link SystemAction#UNLOCK} through the default
 * system-action endpoints, both for a single contentlet and for a collection.
 *
 * <p>Locking is <b>per user</b>, which makes these two system actions behave unlike the rest:
 * {@code canLock} refuses to release a lock held by somebody else unless the caller holds the CMS
 * Administrator role. The bulk endpoint therefore reports those items as failures instead of
 * refusing the whole batch, and that contract is what most of these tests pin down.</p>
 *
 * @see SystemActionApiFireCommandFactory
 */
public class WorkflowResourceLockUnlockIntegrationTest {

    private static WorkflowResource workflowResource;
    private static ContentletAPI contentletAPI;
    private static PermissionAPI permissionAPI;
    private static User systemUser;
    private static Language defaultLanguage;
    private static ContentType contentType;

    private static final String ADMIN_EMAIL = "admin@dotcms.com";
    private static final String ADMIN_PASSWORD = "admin";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        // The real WebResource on purpose: these tests hinge on *which* user is calling, so the
        // caller has to be resolved from the request's Basic auth rather than stubbed.
        workflowResource = new WorkflowResource();
        contentletAPI = APILocator.getContentletAPI();
        permissionAPI = APILocator.getPermissionAPI();
        systemUser = APILocator.systemUser();
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        contentType = new ContentTypeDataGen().nextPersisted();
    }

    /**
     * Method to test: {@link WorkflowResource#fireActionDefaultSinglePart}
     * <p>
     * Given scenario: An unlocked contentlet and an administrator firing {@code LOCK} over it.
     * <p>
     * Expected result: 200, and the contentlet is locked by the calling user.
     */
    @Test
    public void test_fireLock_singleContentlet_locksItForTheCaller() throws Exception {
        final Contentlet contentlet = newContentlet();

        final Response response = fireDefaultAction(adminRequest(), SystemAction.LOCK, contentlet);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(adminUser().getUserId(), lockedBy(contentlet));
    }

    /**
     * Method to test: {@link WorkflowResource#fireActionDefaultSinglePart}
     * <p>
     * Given scenario: A contentlet the caller has locked, then firing {@code UNLOCK} over it.
     * <p>
     * Expected result: 200, and the lock is released.
     */
    @Test
    public void test_fireUnlock_singleContentlet_releasesTheLock() throws Exception {
        final Contentlet contentlet = newContentlet();
        contentletAPI.lock(contentlet, adminUser(), false);

        final Response response = fireDefaultAction(adminRequest(), SystemAction.UNLOCK, contentlet);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertNull(lockedBy(contentlet));
    }

    /**
     * Method to test: {@link WorkflowResource#fireActionDefaultSinglePart}
     * <p>
     * Given scenario: A brand new (never persisted) contentlet firing {@code LOCK}.
     * <p>
     * Expected result: A non-OK response — there is nothing to lock yet, and letting this through
     * would lock a contentlet the caller never created.
     */
    @Test
    public void test_fireLock_newContentlet_isRejected() throws Exception {
        final FireActionForm form = new FireActionForm(
                new FireActionForm.Builder().contentlet(Map.of("stInode", contentType.inode())));

        final Response response = workflowResource.fireActionDefaultSinglePart(adminRequest(),
                new EmptyHttpResponse(), null, null, "WAIT_FOR",
                String.valueOf(defaultLanguage.getId()), "DEFAULT", SystemAction.LOCK, form);

        assertNotEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    /**
     * Method to test: {@link WorkflowResource#fireMultipleActionDefault}
     * <p>
     * Given scenario: Three unlocked contentlets, one {@code LOCK} call over all of them.
     * <p>
     * Expected result: Every contentlet is locked and the streamed summary reports three
     * successes and no failures — one request, not three.
     */
    @Test
    public void test_fireLock_multipleContentlets_locksAllOfThem() throws Exception {
        final List<Contentlet> contentlets = List.of(newContentlet(), newContentlet(),
                newContentlet());

        final JsonNode summary = fireMultiple(adminRequest(), SystemAction.LOCK, contentlets);

        assertEquals(3, summary.get("successCount").asInt());
        assertEquals(0, summary.get("failCount").asInt());
        for (final Contentlet contentlet : contentlets) {
            assertEquals(adminUser().getUserId(), lockedBy(contentlet));
        }
    }

    /**
     * Method to test: {@link WorkflowResource#fireMultipleActionDefault}
     * <p>
     * Given scenario: Two locked contentlets, one {@code UNLOCK} call over both.
     * <p>
     * Expected result: Both locks are released and the summary reports two successes.
     */
    @Test
    public void test_fireUnlock_multipleContentlets_releasesAllOfThem() throws Exception {
        final List<Contentlet> contentlets = List.of(newContentlet(), newContentlet());
        for (final Contentlet contentlet : contentlets) {
            contentletAPI.lock(contentlet, adminUser(), false);
        }

        final JsonNode summary = fireMultiple(adminRequest(), SystemAction.UNLOCK, contentlets);

        assertEquals(2, summary.get("successCount").asInt());
        assertEquals(0, summary.get("failCount").asInt());
        for (final Contentlet contentlet : contentlets) {
            assertNull(lockedBy(contentlet));
        }
    }

    /**
     * Method to test: {@link WorkflowResource#fireMultipleActionDefault}
     * <p>
     * Given scenario: Two contentlets locked by an administrator. A limited user who <b>does</b>
     * hold EDIT permission on them — so the permission gate passes — fires {@code UNLOCK}.
     * <p>
     * Expected result: The batch is not refused; both items come back as failures and the locks
     * survive. This is the documented rule: a lock held by another user is reported per item, not
     * silently skipped and not fatal to the whole request.
     */
    @Test
    public void test_fireUnlock_lockHeldByAnotherUser_reportsPerItemFailure() throws Exception {
        final String password = "TestPass" + System.currentTimeMillis() + "!";
        final User limitedUser = newLimitedUser(password);
        final List<Contentlet> contentlets = List.of(newContentlet(), newContentlet());

        for (final Contentlet contentlet : contentlets) {
            grantEdit(contentlet, limitedUser);
            contentletAPI.lock(contentlet, adminUser(), false);
        }

        final JsonNode entity = fireMultipleEntity(
                requestForUser(limitedUser.getEmailAddress(), password), SystemAction.UNLOCK,
                contentlets);
        final JsonNode summary = entity.get("summary");

        assertEquals(0, summary.get("successCount").asInt());
        assertEquals(2, summary.get("failCount").asInt());
        for (final Contentlet contentlet : contentlets) {
            assertEquals("The lock must survive a denied unlock", adminUser().getUserId(),
                    lockedBy(contentlet));
        }
        // Guards against the failure coming from the wrong place. Without the content-type half of
        // `grantEdit` these items are rejected by `populateContentlet` on permissions and never
        // reach `canLock`, so the counts above would be satisfied without the lock rule ever being
        // exercised — green, and proving nothing.
        assertTrue("Expected a lock failure, got: " + failMessages(entity),
                failMessages(entity).toLowerCase().contains("lock"));
    }

    /**
     * Method to test: {@link WorkflowResource#fireMultipleActionDefault}
     * <p>
     * Given scenario: A mixed batch — one contentlet locked by the calling user, one locked by
     * somebody else — fired as {@code UNLOCK} by that limited user.
     * <p>
     * Expected result: The caller's own lock is released while the other one is reported as a
     * failure. A partial result, not an all-or-nothing one.
     */
    @Test
    public void test_fireUnlock_mixedOwnership_unlocksOwnLockAndFailsTheOther() throws Exception {
        final String password = "TestPass" + System.currentTimeMillis() + "!";
        final User limitedUser = newLimitedUser(password);
        final Contentlet ownLock = newContentlet();
        final Contentlet foreignLock = newContentlet();

        grantEdit(ownLock, limitedUser);
        grantEdit(foreignLock, limitedUser);
        contentletAPI.lock(ownLock, limitedUser, false);
        contentletAPI.lock(foreignLock, adminUser(), false);

        final JsonNode summary = fireMultiple(
                requestForUser(limitedUser.getEmailAddress(), password), SystemAction.UNLOCK,
                List.of(ownLock, foreignLock));

        assertEquals(1, summary.get("successCount").asInt());
        assertEquals(1, summary.get("failCount").asInt());
        assertNull(lockedBy(ownLock));
        assertEquals(adminUser().getUserId(), lockedBy(foreignLock));
    }

    /**
     * Method to test: {@link WorkflowResource#fireMultipleActionDefault}
     * <p>
     * Given scenario: A limited user granted nothing at all on the target fires {@code LOCK}.
     * <p>
     * Expected result: The item is reported as a failure and stays unlocked — permission is
     * enforced server-side, regardless of what the UI offered.
     * <p>
     * No permissions are stripped to set this up: a freshly created user holds only the backend and
     * frontend roles, so it starts with no access to the content type. An earlier attempt overrode
     * the contentlet's permissions in favour of the system role, which fails outright because that
     * role is locked for editing.
     */
    @Test
    public void test_fireLock_withoutAnyPermission_reportsFailure() throws Exception {
        final String password = "TestPass" + System.currentTimeMillis() + "!";
        final User limitedUser = newLimitedUser(password);
        final Contentlet contentlet = newContentlet();

        final JsonNode summary = fireMultiple(
                requestForUser(limitedUser.getEmailAddress(), password), SystemAction.LOCK,
                List.of(contentlet));

        assertEquals(0, summary.get("successCount").asInt());
        assertEquals(1, summary.get("failCount").asInt());
        assertNull(lockedBy(contentlet));
    }

    /**
     * Method to test: {@link WorkflowResource#fireActionDefaultSinglePart}
     * <p>
     * Given scenario: A locked contentlet, unlocked and then locked again by the same caller.
     * <p>
     * Expected result: Both calls succeed — LOCK on content the caller already holds is a no-op
     * rather than an error, so re-firing over a stale selection cannot fail the batch.
     */
    @Test
    public void test_fireLock_alreadyLockedByCaller_succeeds() throws Exception {
        final Contentlet contentlet = newContentlet();
        contentletAPI.lock(contentlet, adminUser(), false);

        final Response response = fireDefaultAction(adminRequest(), SystemAction.LOCK, contentlet);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(adminUser().getUserId(), lockedBy(contentlet));
    }

    // ------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------

    private static Contentlet newContentlet() {
        return new ContentletDataGen(contentType.id()).languageId(defaultLanguage.getId())
                .nextPersisted();
    }

    private static User adminUser() throws Exception {
        return APILocator.getUserAPI().loadByUserByEmail(ADMIN_EMAIL, systemUser, false);
    }

    private static User newLimitedUser(final String password) throws Exception {
        final Role role = TestUserUtils.getBackendRole();

        return new UserDataGen().password(password)
                .roles(role, TestUserUtils.getFrontendRole()).nextPersisted();
    }

    /**
     * Grants the user READ+EDIT on the contentlet <b>and on its content type</b>.
     *
     * The content type half is not optional. {@code fireTransactionalAction} loads the target
     * through {@code populateContentlet}, which rejects a caller without content-type permission
     * before any lock logic runs — so a contentlet-only grant never reaches {@code canLock}, and a
     * test written to prove the lock-ownership rule would pass on the wrong exception.
     */
    private static void grantEdit(final Contentlet contentlet, final User user) throws Exception {
        final int readEdit = PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT;
        final String roleId = APILocator.getRoleAPI().getUserRole(user).getId();

        final List<Permission> typePermissions = new ArrayList<>();
        typePermissions.add(new Permission(contentType.getPermissionId(), roleId, readEdit, true));
        permissionAPI.save(typePermissions, contentType, systemUser, false);

        final List<Permission> permissions = new ArrayList<>();
        permissions.add(new Permission(contentlet.getPermissionId(), roleId, readEdit, true));
        permissionAPI.save(permissions, contentlet, systemUser, false);
    }

    /**
     * The user id currently holding the lock, or {@code null} when the contentlet is unlocked.
     */
    private static String lockedBy(final Contentlet contentlet) throws Exception {
        return APILocator.getVersionableAPI()
                .getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId())
                .map(versionInfo -> versionInfo.getLockedBy())
                .orElse(null);
    }

    private static Response fireDefaultAction(final HttpServletRequest request,
            final SystemAction systemAction, final Contentlet contentlet) {

        return workflowResource.fireActionDefaultSinglePart(request, new EmptyHttpResponse(),
                contentlet.getInode(), null, "WAIT_FOR",
                String.valueOf(contentlet.getLanguageId()), "DEFAULT", systemAction, null);
    }

    /**
     * Fires the multi-contentlet variant and returns the streamed {@code summary} node, which is
     * where the per-item success/failure counts live.
     */
    private static JsonNode fireMultiple(final HttpServletRequest request,
            final SystemAction systemAction, final List<Contentlet> contentlets) throws Exception {

        return fireMultipleEntity(request, systemAction, contentlets).get("summary");
    }

    /**
     * As {@link #fireMultiple}, but returns the whole entity so a test can inspect the per-item
     * {@code results} — needed to tell *why* an item failed, not merely that it did.
     */
    private static JsonNode fireMultipleEntity(final HttpServletRequest request,
            final SystemAction systemAction, final List<Contentlet> contentlets) throws Exception {

        final List<Map<String, Object>> contentletForms = new ArrayList<>();
        for (final Contentlet contentlet : contentlets) {
            contentletForms.add(Map.of("inode", contentlet.getInode()));
        }

        final FireMultipleActionForm form = new FireMultipleActionForm.Builder()
                .contentlets(contentletForms).build();

        final Response response = workflowResource.fireMultipleActionDefault(request,
                new EmptyHttpResponse(), systemAction, form);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        StreamingOutput.class.cast(response.getEntity()).write(output);

        final JsonNode entity = new ObjectMapper().readTree(output.toByteArray()).get("entity");
        assertTrue("The response must carry a summary", entity.has("summary"));

        return entity;
    }

    /** Every {@code errorMessage} in the streamed results, flattened for assertion. */
    private static String failMessages(final JsonNode entity) {
        final StringBuilder messages = new StringBuilder();

        for (final JsonNode result : entity.get("results")) {
            result.fields().forEachRemaining(field -> {
                final JsonNode errorMessage = field.getValue().get("errorMessage");
                if (null != errorMessage) {
                    messages.append(errorMessage.asText()).append(' ');
                }
            });
        }

        return messages.toString();
    }

    private static HttpServletRequest adminRequest() {
        return requestForUser(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private static HttpServletRequest requestForUser(final String email, final String password) {
        final MockHeaderRequest request = new MockHeaderRequest(new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request());

        request.setHeader("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString((email + ":" + password).getBytes()));

        return request;
    }
}
