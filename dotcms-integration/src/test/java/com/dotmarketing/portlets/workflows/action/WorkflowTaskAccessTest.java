package com.dotmarketing.portlets.workflows.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link WorkflowTaskAccess}: a workflow task may be written to only by a
 * user who can read the contentlet that task belongs to.
 *
 * <p>The same question answers the read side: the task screen shows the contentlet, so it may
 * only be rendered for a user who could have read that contentlet on their own.</p>
 *
 * <p>Commenting and attaching ask for READ, so that they match what
 * {@code POST /api/v1/workflow/{contentletId}/comments} already requires, and so that a user who
 * can only read a contentlet keeps the ability to comment on its task, which is what the legacy
 * screen offers them today. Removing an attachment asks for EDIT instead — it destroys another
 * user's work and there is no REST endpoint that sets a lower bar for it.</p>
 *
 * <p><b>What these tests do not cover.</b> They exercise the decision, not the wiring: nothing
 * here drives {@link EditWorkflowTaskAction#processAction}, so deleting the guard from that method
 * would leave every test below passing. {@link EditWorkflowTaskActionTest} is the other half — it
 * drives the action itself and covers the guard's placement, above the {@code add_comment},
 * {@code add_file} and {@code remove_file} branches and below {@code view}, and the command set in
 * {@code isTaskWriteCommand}. Anyone moving that guard will be caught there, not here.</p>
 */
public class WorkflowTaskAccessTest {

    private static WorkflowAPI workflowAPI;
    private static PermissionAPI permissionAPI;
    private static Host site;
    private static Contentlet content;
    private static Contentlet publishedContent;
    private static User reader;
    private static User editor;
    private static User userWithoutAccess;
    private static User adminUser;
    private static final List<User> createdUsers = new ArrayList<>();

    /**
     * Creates a site with two contentlets, each with its own workflow task, a back-end user granted
     * READ on them, and a back-end user with no permission at all. The published contentlet is
     * readable by the anonymous role, which is how a front-end-only grant is exercised.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        permissionAPI = APILocator.getPermissionAPI();

        adminUser = TestUserUtils.getAdminUser();
        site = new SiteDataGen().nextPersisted();
        final ContentType contentType = TestDataUtils.getRichTextLikeContentType();
        final long defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        content = newContent(contentType, defaultLanguage);
        publishedContent = ContentletDataGen.publish(newContent(contentType, defaultLanguage));

        reader = newBackendUser();
        editor = newBackendUser();
        userWithoutAccess = newBackendUser();
        grantRead(content, APILocator.getRoleAPI().getUserRole(reader).getId());
        permissionAPI.save(new Permission(content.getPermissionId(),
                        APILocator.getRoleAPI().getUserRole(editor).getId(),
                        PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT, true),
                content, APILocator.systemUser(), false);
        grantRead(publishedContent, APILocator.getRoleAPI().loadCMSAnonymousRole().getId());

        assertNotNull("Persisting a contentlet is expected to create its workflow task",
                workflowAPI.findTaskByContentlet(content));
        assertTrue("The fixture needs a user who can read the content",
                permissionAPI.doesUserHavePermission(content, PermissionAPI.PERMISSION_READ,
                        reader, false));
        assertFalse("The fixture needs a user who cannot read the content",
                permissionAPI.doesUserHavePermission(content, PermissionAPI.PERMISSION_READ,
                        userWithoutAccess, false));
        assertFalse("The fixture needs a reader who cannot edit the content",
                permissionAPI.doesUserHavePermission(content, PermissionAPI.PERMISSION_EDIT,
                        reader, false));
        assertTrue("The fixture needs a user who can edit the content",
                permissionAPI.doesUserHavePermission(content, PermissionAPI.PERMISSION_EDIT,
                        editor, false));
    }

    /**
     * Removes the users and the site created for these tests.
     */
    @AfterClass
    public static void cleanup() {
        createdUsers.forEach(user -> UserDataGen.remove(user, true));
        if (site != null) {
            try {
                APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
            } catch (final Exception e) {
                Logger.warn(WorkflowTaskAccessTest.class,
                        "Could not remove test site: " + e.getMessage());
            }
        }
    }

    /**
     * Given a back-end user with no permission on the contentlet,
     * When the task of that contentlet is checked,
     * Then the user is refused.
     */
    @Test
    public void canAccess_userWithoutRead_isRefused() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);

        assertFalse(WorkflowTaskAccess.canAccess(task, userWithoutAccess));
    }

    /**
     * Given a back-end user granted READ on the contentlet but not EDIT,
     * When the task of that contentlet is checked,
     * Then the user is allowed: reading the content is the whole bar for writing on its task.
     */
    @Test
    public void canAccess_userWithReadOnly_isAllowed() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);

        assertTrue(WorkflowTaskAccess.canAccess(task, reader));
    }

    /**
     * Given a CMS administrator,
     * When any task is checked,
     * Then the administrator is allowed.
     */
    @Test
    public void canAccess_administrator_isAllowed() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);

        assertTrue(WorkflowTaskAccess.canAccess(task, adminUser));
    }

    /**
     * Given a contentlet only the anonymous role can read, and a back-end user with no permission
     * of their own on it,
     * When the task of that contentlet is checked,
     * Then the user is refused: front-end roles do not grant a back-end write.
     */
    @Test
    public void canAccess_anonymousReadableContent_isRefused() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(publishedContent);

        assertFalse(WorkflowTaskAccess.canAccess(task, userWithoutAccess));
    }

    /**
     * Given a back-end user granted READ on the contentlet but not EDIT,
     * When the task is checked for the commands that destroy something,
     * Then the user is refused: reading is enough to comment, not to remove another user's
     * attachment.
     */
    @Test
    public void canEdit_userWithReadOnly_isRefused() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);

        assertTrue("The reader must still pass the READ bar", WorkflowTaskAccess.canAccess(task,
                reader));
        assertFalse(WorkflowTaskAccess.canEdit(task, reader));
    }

    /**
     * Given a back-end user granted EDIT on the contentlet,
     * When the task is checked for the commands that destroy something,
     * Then the user is allowed.
     */
    @Test
    public void canEdit_userWithEdit_isAllowed() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);

        assertTrue(WorkflowTaskAccess.canEdit(task, editor));
    }

    /**
     * Given a back-end user with no permission on the contentlet,
     * When the task is checked for the commands that destroy something,
     * Then the user is refused without the EDIT lookup ever being reached.
     */
    @Test
    public void canEdit_userWithoutRead_isRefused() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);

        assertFalse(WorkflowTaskAccess.canEdit(task, userWithoutAccess));
    }

    /**
     * Given a null task or a null user,
     * When access is checked,
     * Then it is refused rather than throwing: the caller cannot be left to guess.
     */
    @Test
    public void canAccess_nullArguments_areRefused() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);

        assertFalse(WorkflowTaskAccess.canAccess(null, reader));
        assertFalse(WorkflowTaskAccess.canAccess(task, null));
    }

    /**
     * Given a task whose contentlet cannot be resolved, such as one left behind by deleted content,
     * When access is checked,
     * Then it is refused: a lookup that cannot be answered is not an authorization.
     */
    @Test
    public void canAccess_taskWithUnknownContentlet_isRefused() {
        final WorkflowTask orphanTask = new WorkflowTask();
        orphanTask.setId("made-up-task-id");
        orphanTask.setWebasset("made-up-identifier");
        orphanTask.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());

        assertFalse(WorkflowTaskAccess.canAccess(orphanTask, reader));
    }

    /**
     * Given a back-end user granted READ on the contentlet,
     * When the contentlet behind the task is asked for on that user's behalf,
     * Then it comes back, so the screen that renders it has something to render.
     */
    @Test
    public void contentletFor_userWithRead_returnsTheContentlet() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);

        final Optional<Contentlet> resolved = WorkflowTaskAccess.contentletFor(task, reader);

        assertTrue("A reader must get the task's contentlet", resolved.isPresent());
        assertEquals(content.getIdentifier(), resolved.get().getIdentifier());
    }

    /**
     * Given a back-end user with no permission on the contentlet,
     * When the contentlet behind the task is asked for on that user's behalf,
     * Then nothing comes back: the lookup is itself the refusal, and the screen is left with
     * nothing to show.
     */
    @Test
    public void contentletFor_userWithoutRead_isEmpty() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);

        assertFalse(WorkflowTaskAccess.contentletFor(task, userWithoutAccess).isPresent());
    }

    /**
     * Given a contentlet only the anonymous role can read, and a back-end user with no permission
     * of their own on it,
     * When the contentlet behind its task is asked for on that user's behalf,
     * Then nothing comes back: a front-end grant does not open a back-end screen.
     */
    @Test
    public void contentletFor_anonymousReadableContent_isEmpty() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(publishedContent);

        assertFalse(WorkflowTaskAccess.contentletFor(task, userWithoutAccess).isPresent());
    }

    /**
     * Creates a persisted contentlet of the given type and language on the test site.
     *
     * @param contentType the {@link ContentType} to create the contentlet from
     * @param languageId  the language to create it in
     * @return the persisted {@link Contentlet}
     */
    private static Contentlet newContent(final ContentType contentType, final long languageId) {
        return new ContentletDataGen(contentType.id())
                .languageId(languageId)
                .host(site)
                .setProperty("title", "content-" + System.currentTimeMillis())
                .setProperty("body", "body")
                .nextPersisted();
    }

    /**
     * Grants READ on the given contentlet to the given role.
     *
     * @param contentlet the {@link Contentlet} to grant on
     * @param roleId     the id of the role receiving the grant
     */
    private static void grantRead(final Contentlet contentlet, final String roleId)
            throws Exception {
        permissionAPI.save(new Permission(contentlet.getPermissionId(), roleId,
                PermissionAPI.PERMISSION_READ, true), contentlet, APILocator.systemUser(), false);
    }

    /**
     * Creates a persisted back-end {@link User} with no permissions of its own.
     *
     * @return the persisted {@link User}
     */
    private static User newBackendUser() throws Exception {
        final User user = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        createdUsers.add(user);
        return user;
    }
}
