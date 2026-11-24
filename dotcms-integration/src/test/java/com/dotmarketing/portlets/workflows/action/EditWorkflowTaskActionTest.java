package com.dotmarketing.portlets.workflows.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.portlets.workflows.struts.WorkflowTaskForm;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link EditWorkflowTaskAction#processAction}: the guard that refuses a
 * write to a workflow task is exercised through the action itself, not through the class that
 * makes the decision.
 *
 * <p>This is the half {@link WorkflowTaskAccessTest} cannot reach. That class proves
 * {@link WorkflowTaskAccess#canAccess} answers correctly; these tests prove the action asks it,
 * asks it for every write command, and refuses without touching the task. Deleting the guard
 * leaves {@code WorkflowTaskAccessTest} green and these red.</p>
 *
 * <p>{@code view} is exercised here too. It is asked for the way the admin's task dialog asks
 * for it — the task named by {@code taskId}, with no {@code inode} — because a guard that read
 * the task from {@code inode} would refuse every real caller while still looking correct.</p>
 *
 * <p><b>What these tests do not cover.</b> The screen itself. These tests end at the forward, so
 * they say which page the action chose, never what that page then reads or renders.</p>
 */
public class EditWorkflowTaskActionTest {

    private static final String ERROR_FORWARD = Constants.COMMON_ERROR;
    private static final String DONE_FORWARD = "portlet.ext.workflows.edit_workflow_task";
    private static final String VIEW_FORWARD = "portlet.ext.workflows.view_workflow_task";

    private static WorkflowAPI workflowAPI;
    private static PermissionAPI permissionAPI;
    private static Host site;
    private static Contentlet content;
    private static User reader;
    private static User userWithoutAccess;
    private static final List<User> createdUsers = new ArrayList<>();

    /**
     * Creates a site with one contentlet and its workflow task, a back-end user granted READ on
     * that contentlet, and a back-end user with no permission on it at all.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        permissionAPI = APILocator.getPermissionAPI();

        site = new SiteDataGen().nextPersisted();
        final ContentType contentType = TestDataUtils.getRichTextLikeContentType();
        content = new ContentletDataGen(contentType.id())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .host(site)
                .setProperty("title", "task-action-" + System.currentTimeMillis())
                .setProperty("body", "body")
                .nextPersisted();

        reader = newBackendUser();
        userWithoutAccess = newBackendUser();
        permissionAPI.save(new Permission(content.getPermissionId(),
                        APILocator.getRoleAPI().getUserRole(reader).getId(),
                        PermissionAPI.PERMISSION_READ, true),
                content, APILocator.systemUser(), false);
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
                Logger.warn(EditWorkflowTaskActionTest.class,
                        "Could not remove test site: " + e.getMessage());
            }
        }
    }

    /**
     * Method to test: {@link EditWorkflowTaskAction#processAction}
     * Given Scenario: A back-end user with no permission on the task's contentlet posts
     *   {@code cmd=add_comment} with a comment
     * Expected Result: No comment is stored and the request is forwarded to the error page
     */
    @Test
    public void processAction_addComment_userWithoutRead_isRefused() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);
        final int commentsBefore = comments(task).size();
        final ActionRequestImpl request = taskRequest(userWithoutAccess,
                com.dotmarketing.util.Constants.ADD_COMMENT, task.getId(), "let me in");

        new EditWorkflowTaskAction().processAction(null, null, null, request,
                mock(ActionResponseImpl.class));

        assertEquals("A refused write must land on the error page", ERROR_FORWARD,
                request.getAttribute(WebKeys.PORTLET_STRUTS_FORWARD));
        assertEquals("A refused comment must not be stored", commentsBefore,
                comments(task).size());
    }

    /**
     * Method to test: {@link EditWorkflowTaskAction#processAction}
     * Given Scenario: The same user posts {@code cmd=add_file}, the command that attaches a file
     *   to the task
     * Expected Result: The request is forwarded to the error page, so the attach never runs
     */
    @Test
    public void processAction_addFile_userWithoutRead_isRefused() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);
        final ActionRequestImpl request = taskRequest(userWithoutAccess,
                com.dotmarketing.util.Constants.ADD_FILE, task.getId(), null);

        new EditWorkflowTaskAction().processAction(null, null, null, request,
                mock(ActionResponseImpl.class));

        assertEquals(ERROR_FORWARD, request.getAttribute(WebKeys.PORTLET_STRUTS_FORWARD));
    }

    /**
     * Method to test: {@link EditWorkflowTaskAction#processAction}
     * Given Scenario: The same user posts {@code cmd=remove_file}, the command that detaches a
     *   file from the task
     * Expected Result: The request is forwarded to the error page, so the detach never runs
     */
    @Test
    public void processAction_removeFile_userWithoutRead_isRefused() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);
        final ActionRequestImpl request = taskRequest(userWithoutAccess,
                com.dotmarketing.util.Constants.REMOVE_FILE, task.getId(), null);

        new EditWorkflowTaskAction().processAction(null, null, null, request,
                mock(ActionResponseImpl.class));

        assertEquals(ERROR_FORWARD, request.getAttribute(WebKeys.PORTLET_STRUTS_FORWARD));
    }

    /**
     * Method to test: {@link EditWorkflowTaskAction#processAction}
     * Given Scenario: A back-end user who holds READ on the contentlet but not EDIT posts
     *   {@code cmd=remove_file}
     * Expected Result: The request is forwarded to the error page. Reading is enough to comment
     *   on a task, not to delete an attachment someone else put on it
     */
    @Test
    public void processAction_removeFile_userWithReadOnly_isRefused() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);
        final ActionRequestImpl request = taskRequest(reader,
                com.dotmarketing.util.Constants.REMOVE_FILE, task.getId(), null);

        new EditWorkflowTaskAction().processAction(null, null, null, request,
                mock(ActionResponseImpl.class));

        assertEquals(ERROR_FORWARD, request.getAttribute(WebKeys.PORTLET_STRUTS_FORWARD));
    }

    /**
     * Method to test: {@link EditWorkflowTaskAction#processAction}
     * Given Scenario: A back-end user who holds READ on the contentlet, and nothing more, posts
     *   {@code cmd=add_comment}
     * Expected Result: The comment is stored and the screen comes back normally — the guard asks
     *   for READ, so commenting on a task you can read keeps working
     */
    @Test
    public void processAction_addComment_userWithReadOnly_isAllowed() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);
        final int commentsBefore = comments(task).size();
        final String comment = "allowed-" + System.currentTimeMillis();
        final ActionRequestImpl request = taskRequest(reader,
                com.dotmarketing.util.Constants.ADD_COMMENT, task.getId(), comment);

        new EditWorkflowTaskAction().processAction(null, null, null, request,
                mock(ActionResponseImpl.class));

        assertEquals("An allowed write must not land on the error page", DONE_FORWARD,
                request.getAttribute(WebKeys.PORTLET_STRUTS_FORWARD));
        assertEquals(commentsBefore + 1, comments(task).size());
        assertTrue("The stored comment should be the one that was posted",
                comments(task).stream().anyMatch(stored -> comment.equals(stored.getComment())));
    }

    /**
     * Method to test: {@link EditWorkflowTaskAction#processAction}
     * Given Scenario: A back-end user with no permission on the task's contentlet asks for
     *   {@code cmd=view} on it, naming the task with {@code taskId} the way the admin's task
     *   dialog does
     * Expected Result: The request is forwarded to the error page, so the screen never renders
     */
    @Test
    public void processAction_view_userWithoutRead_isRefused() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);
        final ActionRequestImpl request = viewRequest(userWithoutAccess, task.getId());

        new EditWorkflowTaskAction().processAction(null, new WorkflowTaskForm(), null, request,
                mock(ActionResponseImpl.class));

        assertEquals("A refused view must land on the error page", ERROR_FORWARD,
                request.getAttribute(WebKeys.PORTLET_STRUTS_FORWARD));
    }

    /**
     * Method to test: {@link EditWorkflowTaskAction#processAction}
     * Given Scenario: A back-end user who holds READ on the task's contentlet asks for
     *   {@code cmd=view} the same way, with {@code taskId} and no {@code inode}
     * Expected Result: The task screen is forwarded to. This is the request the admin's dialog
     *   actually sends, so a guard that looked the task up under {@code inode} would refuse every
     *   legitimate caller and fail here
     */
    @Test
    public void processAction_view_userWithRead_rendersTheScreen() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);
        final ActionRequestImpl request = viewRequest(reader, task.getId());

        new EditWorkflowTaskAction().processAction(null, new WorkflowTaskForm(), null, request,
                mock(ActionResponseImpl.class));

        assertEquals("A caller who can read the content must get the screen", VIEW_FORWARD,
                request.getAttribute(WebKeys.PORTLET_STRUTS_FORWARD));
    }

    /**
     * Reads the comments currently stored on the given task.
     *
     * @param task the {@link WorkflowTask} to read
     * @return its {@link WorkflowComment}s, never {@code null}
     */
    private static List<WorkflowComment> comments(final WorkflowTask task) throws Exception {
        final List<WorkflowComment> stored = workflowAPI.findWorkFlowComments(task);
        return null != stored ? stored : new ArrayList<>();
    }

    /**
     * Builds the portlet request the action reads: the command, the task's id under the
     * {@code inode} parameter the action looks for, the posted comment, the calling user, and an
     * attribute map so that what the action stores on the request can be read back.
     *
     * <p>No referer and no redirect are given, which keeps {@code _sendToReferral} from trying to
     * redirect a mock.</p>
     *
     * @param user    the {@link User} making the request
     * @param cmd     the command being posted
     * @param taskId  the id of the {@link WorkflowTask} being written to
     * @param comment the comment being posted, or {@code null} for the other commands
     * @return the request to hand to {@code processAction}
     */
    private static ActionRequestImpl taskRequest(final User user, final String cmd,
            final String taskId, final String comment) {
        final Map<String, Object> sessionAttributes = new HashMap<>();
        final HttpSession httpSession = mock(HttpSession.class);
        when(httpSession.getAttribute(anyString()))
                .thenAnswer(call -> sessionAttributes.get(call.<String>getArgument(0)));
        doAnswer(call -> sessionAttributes.put(call.getArgument(0), call.getArgument(1)))
                .when(httpSession).setAttribute(anyString(), any());

        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getAttribute(WebKeys.USER)).thenReturn(user);
        when(httpRequest.getSession()).thenReturn(httpSession);
        when(httpRequest.getSession(false)).thenReturn(httpSession);

        final Map<String, Object> requestAttributes = new HashMap<>();
        final ActionRequestImpl request = mock(ActionRequestImpl.class);
        when(request.getHttpServletRequest()).thenReturn(httpRequest);
        when(request.getParameter(Constants.CMD)).thenReturn(cmd);
        when(request.getParameter("inode")).thenReturn(taskId);
        when(request.getParameter("comment")).thenReturn(comment);
        when(request.getAttribute(anyString()))
                .thenAnswer(call -> requestAttributes.get(call.<String>getArgument(0)));
        doAnswer(call -> requestAttributes.put(call.getArgument(0), call.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        return request;
    }

    /**
     * Builds the request the admin's task dialog sends to open a task: the {@code view} command
     * and the task named by {@code taskId}, with no {@code inode} parameter at all.
     *
     * @param user   the {@link User} opening the screen
     * @param taskId the id of the {@link WorkflowTask} being opened
     * @return the request to hand to {@code processAction}
     */
    private static ActionRequestImpl viewRequest(final User user, final String taskId) {
        final ActionRequestImpl request = taskRequest(user, Constants.VIEW, null, null);
        when(request.getParameter("taskId")).thenReturn(taskId);
        return request;
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
