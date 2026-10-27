package com.dotcms.rest.api.v1.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.form.FireActionForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the READ requirement on the workflow history/comments endpoints of
 * {@link WorkflowResource}: a contentlet's timeline can be read, and a comment added to it, only by
 * a user who can read that contentlet.
 */
public class WorkflowResourceCommentsReadAccessTest {

    private static final String ANY_LANGUAGE = "-1";

    private static WorkflowAPI workflowAPI;
    private static PermissionAPI permissionAPI;
    private static Host site;
    private static Language otherLanguage;
    private static Contentlet content;
    private static Contentlet otherLanguageContent;
    private static Contentlet anonymousReadableContent;
    private static User userWithoutAccess;
    private static User reader;
    private static final List<User> createdUsers = new ArrayList<>();

    /**
     * Creates a site with three contentlets, each with a workflow task: one in the default
     * language, one only in another language, and one the anonymous role can read. Also creates a
     * back-end user with no permission on them and a back-end user granted READ on the first two.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        permissionAPI = APILocator.getPermissionAPI();

        site = new SiteDataGen().nextPersisted();
        final ContentType contentType = TestDataUtils.getRichTextLikeContentType();
        final long defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        content = newContent(contentType, defaultLanguage);
        otherLanguage = new LanguageDataGen().nextPersisted();
        otherLanguageContent = newContent(contentType, otherLanguage.getId());
        // The anonymous role only grants READ on live content, so this one is published.
        anonymousReadableContent = ContentletDataGen.publish(newContent(contentType, defaultLanguage));

        userWithoutAccess = newBackendUser();
        reader = newBackendUser();
        grantRead(content, APILocator.getRoleAPI().getUserRole(reader).getId());
        grantRead(otherLanguageContent, APILocator.getRoleAPI().getUserRole(reader).getId());
        grantRead(anonymousReadableContent, APILocator.getRoleAPI().loadCMSAnonymousRole().getId());

        assertFalse("The fixture needs a user who cannot read the content",
                permissionAPI.doesUserHavePermission(content, PermissionAPI.PERMISSION_READ,
                        userWithoutAccess, false));
        assertFalse("The fixture needs a user who cannot read the content",
                permissionAPI.doesUserHavePermission(otherLanguageContent,
                        PermissionAPI.PERMISSION_READ, userWithoutAccess, false));
        assertTrue("The fixture needs a user who can read the content",
                permissionAPI.doesUserHavePermission(content, PermissionAPI.PERMISSION_READ,
                        reader, false));
        assertFalse("Without front-end roles the user cannot read the anonymous-readable content",
                permissionAPI.doesUserHavePermission(anonymousReadableContent,
                        PermissionAPI.PERMISSION_READ, userWithoutAccess, false));
        assertTrue("With front-end roles the user can read the anonymous-readable content",
                permissionAPI.doesUserHavePermission(anonymousReadableContent,
                        PermissionAPI.PERMISSION_READ, userWithoutAccess, true));
        assertNotNull("Persisting a contentlet is expected to create its workflow task",
                workflowAPI.findTaskByContentlet(content));
        assertNotNull("Persisting a contentlet is expected to create its workflow task",
                workflowAPI.findTaskByContentlet(otherLanguageContent));
    }

    /**
     * Removes the users, the site and the language created for these tests.
     */
    @AfterClass
    public static void cleanup() {
        createdUsers.forEach(user -> UserDataGen.remove(user, true));
        if (site != null) {
            try {
                APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
            } catch (final Exception e) {
                Logger.warn(WorkflowResourceCommentsReadAccessTest.class,
                        "Could not remove test site: " + e.getMessage());
            }
        }
        LanguageDataGen.remove(otherLanguage);
    }

    /**
     * Given a back-end user with no permission on the contentlet,
     * When the user requests its workflow history and comments without a language,
     * Then the request answers not found, the same as for an unknown identifier.
     */
    @Test
    public void historyComments_withoutRead_returnsNotFound() {
        final WorkflowResource resource = resourceFor(userWithoutAccess);

        assertThrows(DoesNotExistException.class, () -> resource.getWorkflowTasksHistoryComments(
                requestFor(userWithoutAccess), new EmptyHttpResponse(), content.getIdentifier(),
                ANY_LANGUAGE));
    }

    /**
     * Given a back-end user with no permission on the contentlet,
     * When the user adds a comment to it without a language,
     * Then the request answers not found and no comment is saved on the task.
     */
    @Test
    public void saveComment_withoutRead_returnsNotFoundAndSavesNothing() throws Exception {
        final WorkflowResource resource = resourceFor(userWithoutAccess);
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);
        final int commentsBefore = workflowAPI.findWorkFlowComments(task).size();

        assertThrows(DoesNotExistException.class, () -> resource.saveComment(
                requestFor(userWithoutAccess), new EmptyHttpResponse(), content.getIdentifier(),
                ANY_LANGUAGE, new WorkflowCommentForm("comment")));

        assertEquals(commentsBefore, workflowAPI.findWorkFlowComments(task).size());
    }

    /**
     * Given a back-end user with no permission on the contentlet,
     * When the user requests its workflow history and comments for a given language,
     * Then the request answers not found, the same as for an unknown identifier.
     */
    @Test
    public void historyComments_withLanguage_withoutRead_returnsNotFound() {
        final WorkflowResource resource = resourceFor(userWithoutAccess);

        assertThrows(DoesNotExistException.class, () -> resource.getWorkflowTasksHistoryComments(
                requestFor(userWithoutAccess), new EmptyHttpResponse(), content.getIdentifier(),
                String.valueOf(content.getLanguageId())));
    }

    /**
     * Given a back-end user with no permission on the contentlet,
     * When the user adds a comment to it for a given language,
     * Then the request answers not found and no comment is saved on the task.
     */
    @Test
    public void saveComment_withLanguage_withoutRead_returnsNotFoundAndSavesNothing()
            throws Exception {
        final WorkflowResource resource = resourceFor(userWithoutAccess);
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);
        final int commentsBefore = workflowAPI.findWorkFlowComments(task).size();

        assertThrows(DoesNotExistException.class, () -> resource.saveComment(
                requestFor(userWithoutAccess), new EmptyHttpResponse(), content.getIdentifier(),
                String.valueOf(content.getLanguageId()), new WorkflowCommentForm("comment")));

        assertEquals(commentsBefore, workflowAPI.findWorkFlowComments(task).size());
    }

    /**
     * Given a contentlet the anonymous role can read, and a back-end user with no permission of
     * their own on it,
     * When the user requests its history or adds a comment in LIVE mode,
     * Then both requests answer not found: front-end and anonymous roles do not count on these
     * endpoints, whatever the page mode.
     */
    @Test
    public void endpoints_liveMode_ignoreAnonymousRead() throws Exception {
        final WorkflowResource resource = resourceFor(userWithoutAccess);
        final WorkflowTask task = workflowAPI.findTaskByContentlet(anonymousReadableContent);
        final int commentsBefore = workflowAPI.findWorkFlowComments(task).size();

        assertThrows(DoesNotExistException.class, () -> resource.getWorkflowTasksHistoryComments(
                liveRequestFor(userWithoutAccess), new EmptyHttpResponse(),
                anonymousReadableContent.getIdentifier(), ANY_LANGUAGE));
        assertThrows(DoesNotExistException.class, () -> resource.saveComment(
                liveRequestFor(userWithoutAccess), new EmptyHttpResponse(),
                anonymousReadableContent.getIdentifier(), ANY_LANGUAGE,
                new WorkflowCommentForm("comment")));

        assertEquals(commentsBefore, workflowAPI.findWorkFlowComments(task).size());
    }

    /**
     * Given a back-end user with no permission on a contentlet that exists only in a non-default
     * language,
     * When the user requests its workflow history and comments without a language,
     * Then the request answers not found.
     */
    @Test
    public void historyComments_otherLanguageOnly_withoutRead_returnsNotFound() {
        final WorkflowResource resource = resourceFor(userWithoutAccess);

        assertThrows(DoesNotExistException.class, () -> resource.getWorkflowTasksHistoryComments(
                requestFor(userWithoutAccess), new EmptyHttpResponse(),
                otherLanguageContent.getIdentifier(), ANY_LANGUAGE));
    }

    /**
     * Given a back-end user with no permission on a contentlet that exists only in a non-default
     * language,
     * When the user adds a comment to it without a language,
     * Then the request answers not found and no comment is saved on the task.
     */
    @Test
    public void saveComment_otherLanguageOnly_withoutRead_returnsNotFoundAndSavesNothing()
            throws Exception {
        final WorkflowResource resource = resourceFor(userWithoutAccess);
        final WorkflowTask task = workflowAPI.findTaskByContentlet(otherLanguageContent);
        final int commentsBefore = workflowAPI.findWorkFlowComments(task).size();

        assertThrows(DoesNotExistException.class, () -> resource.saveComment(
                requestFor(userWithoutAccess), new EmptyHttpResponse(),
                otherLanguageContent.getIdentifier(), ANY_LANGUAGE,
                new WorkflowCommentForm("comment")));

        assertEquals(commentsBefore, workflowAPI.findWorkFlowComments(task).size());
    }

    /**
     * Given a back-end user with no permission on a contentlet that exists only in a non-default
     * language,
     * When the user fires the Publish action on it by identifier without a language,
     * Then the request answers not found and the contentlet is not published.
     */
    @Test
    public void fireAction_byIdentifier_otherLanguageOnly_withoutRead_returnsNotFound()
            throws Exception {
        final WorkflowResource resource = resourceFor(userWithoutAccess);

        assertThrows(DoesNotExistException.class, () -> resource.fireActionSinglePart(
                requestFor(userWithoutAccess), new EmptyHttpResponse(),
                SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID, null,
                otherLanguageContent.getIdentifier(), null, ANY_LANGUAGE,
                new FireActionForm(new FireActionForm.Builder())));

        assertFalse(APILocator.getVersionableAPI().hasLiveVersion(otherLanguageContent));
    }

    /**
     * Given a non-admin back-end user granted READ on the contentlet,
     * When the user requests its workflow history and comments, with and without a language,
     * Then the timeline is returned.
     */
    @Test
    public void historyComments_withRead_returnsTimeline() throws Exception {
        final WorkflowResource resource = resourceFor(reader);
        resource.saveComment(requestFor(reader), new EmptyHttpResponse(), content.getIdentifier(),
                ANY_LANGUAGE, new WorkflowCommentForm("timeline entry"));

        final ResponseEntityWorkflowHistoryCommentsView anyLanguage =
                resource.getWorkflowTasksHistoryComments(requestFor(reader),
                        new EmptyHttpResponse(), content.getIdentifier(), ANY_LANGUAGE);
        final ResponseEntityWorkflowHistoryCommentsView withLanguage =
                resource.getWorkflowTasksHistoryComments(requestFor(reader),
                        new EmptyHttpResponse(), content.getIdentifier(),
                        String.valueOf(content.getLanguageId()));

        assertFalse(anyLanguage.getEntity().isEmpty());
        assertFalse(withLanguage.getEntity().isEmpty());
    }

    /**
     * Given a non-admin back-end user granted READ on the contentlet,
     * When the user adds a comment to it,
     * Then the comment is saved on the contentlet's task.
     */
    @Test
    public void saveComment_withRead_savesComment() throws Exception {
        final WorkflowTask task = workflowAPI.findTaskByContentlet(content);
        final int commentsBefore = workflowAPI.findWorkFlowComments(task).size();

        resourceFor(reader).saveComment(requestFor(reader), new EmptyHttpResponse(),
                content.getIdentifier(), ANY_LANGUAGE, new WorkflowCommentForm("comment"));

        assertEquals(commentsBefore + 1, workflowAPI.findWorkFlowComments(task).size());
    }

    /**
     * Given a non-admin back-end user granted READ on a contentlet that exists only in a
     * non-default language,
     * When the user requests its workflow history and comments without a language,
     * Then that contentlet's timeline is returned.
     */
    @Test
    public void historyComments_otherLanguageOnly_withRead_returnsTimeline() throws Exception {
        final WorkflowResource resource = resourceFor(reader);
        resource.saveComment(requestFor(reader), new EmptyHttpResponse(),
                otherLanguageContent.getIdentifier(), ANY_LANGUAGE,
                new WorkflowCommentForm("other language entry"));

        final ResponseEntityWorkflowHistoryCommentsView view = resource
                .getWorkflowTasksHistoryComments(requestFor(reader), new EmptyHttpResponse(),
                        otherLanguageContent.getIdentifier(), ANY_LANGUAGE);

        assertTrue(view.getEntity().stream()
                .anyMatch(item -> "other language entry".equals(item.getCommentDescription())));
    }

    /**
     * Given a lookup that resolves the contentlet regardless of the user, and a back-end user with
     * no permission on it,
     * When the user requests its history or adds a comment,
     * Then both requests answer not found: each endpoint requires READ on the contentlet itself.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void endpoints_requireReadOnResolvedContentlet() throws Exception {
        final WorkflowHelper resolvingHelper = mock(WorkflowHelper.class);
        when(resolvingHelper.getContentletByIdentifier(anyString(), any(PageMode.class),
                any(User.class), any(Supplier.class))).thenReturn(Optional.of(content));
        final WorkflowResource resource = resourceFor(userWithoutAccess, resolvingHelper);

        assertThrows(DoesNotExistException.class, () -> resource.getWorkflowTasksHistoryComments(
                requestFor(userWithoutAccess), new EmptyHttpResponse(), content.getIdentifier(),
                ANY_LANGUAGE));
        assertThrows(DoesNotExistException.class, () -> resource.saveComment(
                requestFor(userWithoutAccess), new EmptyHttpResponse(), content.getIdentifier(),
                ANY_LANGUAGE, new WorkflowCommentForm("comment")));
    }

    /**
     * Builds the resource with the real workflow helper, acting as the given user.
     */
    private static WorkflowResource resourceFor(final User user) {
        return resourceFor(user, new WorkflowHelper(workflowAPI, APILocator.getRoleAPI(),
                APILocator.getContentletAPI(), permissionAPI, WorkflowImportExportUtil.getInstance()));
    }

    /**
     * Builds the resource with the given workflow helper, acting as the given user.
     */
    private static WorkflowResource resourceFor(final User user, final WorkflowHelper helper) {
        final InitDataObject initDataObject = mock(InitDataObject.class);
        when(initDataObject.getUser()).thenReturn(user);
        final WebResource webResource = mock(WebResource.class);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initDataObject);

        return new WorkflowResource(helper, ContentHelper.getInstance(), workflowAPI,
                APILocator.getContentletAPI(), ResponseUtil.INSTANCE, permissionAPI,
                WorkflowImportExportUtil.getInstance(), new MultiPartUtils(), webResource,
                SystemActionApiFireCommandFactory.getInstance());
    }

    /**
     * A request carrying the given back-end user, so the page mode resolves as it does for a
     * logged-in back-end caller.
     */
    private static HttpServletRequest requestFor(final User user) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        when(request.getRequestURI()).thenReturn("/api/v1/workflow");
        return request;
    }

    /**
     * A request carrying the given back-end user that asks for LIVE page mode.
     */
    private static HttpServletRequest liveRequestFor(final User user) {
        final HttpServletRequest request = requestFor(user);
        when(request.getParameter(com.dotmarketing.util.WebKeys.PAGE_MODE_PARAMETER))
                .thenReturn(PageMode.LIVE.name());
        return request;
    }

    private static Contentlet newContent(final ContentType contentType, final long languageId) {
        return new ContentletDataGen(contentType.id())
                .languageId(languageId)
                .host(site)
                .setProperty("title", "content-" + System.currentTimeMillis())
                .setProperty("body", "body")
                .nextPersisted();
    }

    private static void grantRead(final Contentlet contentlet, final String roleId) throws Exception {
        permissionAPI.save(new Permission(contentlet.getPermissionId(), roleId,
                PermissionAPI.PERMISSION_READ, true), contentlet, APILocator.systemUser(), false);
    }

    private static User newBackendUser() throws Exception {
        final User user = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        createdUsers.add(user);
        return user;
    }
}
