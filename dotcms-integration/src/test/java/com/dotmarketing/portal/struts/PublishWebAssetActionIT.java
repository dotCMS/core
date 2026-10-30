package com.dotmarketing.portal.struts;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.action.PublishLinksAction;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.PortletSessionImpl;
import com.liferay.util.servlet.SessionMessages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for
 * {@link DotPortletAction#_publishWebAsset(ActionRequest, ActionResponse, PortletConfig, ActionForm, User, String)},
 * the publish behind the edit screens' save-and-publish: the asset goes live only for a user
 * holding PUBLISH on it, and a refused user gets the permission message, not a failed request.
 * The users can read the site and everything in it, which is what any back-end user working on
 * the site can do.
 */
public class PublishWebAssetActionIT {

    private static final String SUCCESS_MESSAGE = "message";
    private static final String ERROR_MESSAGE = "error";
    private static final String PERMISSION_ERROR = "Publish-failed-check-you-have-the-required-permission";

    private static User systemUser;
    private static Host site;
    private static final List<User> usersToClean = new ArrayList<>();

    @BeforeAll
    static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
    }

    /** Deletes the test users. */
    @AfterAll
    static void cleanUp() {
        usersToClean.forEach(user -> UserDataGen.remove(user, true));
    }

    /**
     * Method to test: {@link DotPortletAction#_publishWebAsset}
     * Given Scenario: The CMS Owner role holds PUBLISH on the link's folder, which is enough for
     * the legacy pre-check to let any user through, and the user holds READ only
     * ExpectedResult: The link stays unpublished and the screen gets the permission error, not a
     * success and not a failed request
     */
    @Test
    void test_publishWebAsset_userWithoutPublish_isRefusedWithMessage() throws Exception {
        final User user = backendUser();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        grant(folder, APILocator.getRoleAPI().loadCMSOwnerRole(),
                PermissionAPI.PERMISSION_READ | PERMISSION_PUBLISH);
        final Link link = new LinkDataGen(folder).nextPersisted(false);
        assertDoesNotThrow(() -> DotPortletAction._checkUserPermissions(link, user, PERMISSION_PUBLISH),
                "the legacy pre-check should let this user through; the test needs that to reach the publish");
        final ActionRequestImpl request = publishRequest(user, link);

        new PublishLinksAction()._publishWebAsset(request, mock(ActionResponse.class), null, null,
                user, WebKeys.LINK_FORM_EDIT);

        assertFalse(isLive(link));
        assertEquals(PERMISSION_ERROR, SessionMessages.get(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
    }

    /**
     * Method to test: {@link DotPortletAction#_publishWebAsset}
     * Given Scenario: The user's only PUBLISH on the link's folder comes through the front-end
     * "Logged In Site User" role, which the back-office does not take into account
     * ExpectedResult: The link stays unpublished and the screen gets the permission error, not a
     * failed request
     */
    @Test
    void test_publishWebAsset_publishOnlyThroughFrontEndRole_isRefusedWithMessage()
            throws Exception {
        final User user = backendUser(APILocator.getRoleAPI().loadFrontEndUserRole());
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        grant(folder, APILocator.getRoleAPI().loadFrontEndUserRole(),
                PermissionAPI.PERMISSION_READ | PERMISSION_PUBLISH);
        final Link link = new LinkDataGen(folder).nextPersisted(false);
        final ActionRequestImpl request = publishRequest(user, link);

        new PublishLinksAction()._publishWebAsset(request, mock(ActionResponse.class), null, null,
                user, WebKeys.LINK_FORM_EDIT);

        assertFalse(isLive(link));
        assertEquals(PERMISSION_ERROR, SessionMessages.get(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
    }

    /**
     * Method to test: {@link DotPortletAction#_publishWebAsset}
     * Given Scenario: A user holding PUBLISH on the link's folder
     * ExpectedResult: The link goes live and the screen gets the success message
     */
    @Test
    void test_publishWebAsset_userWithPublish_publishes() throws Exception {
        final User user = backendUser();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        grant(folder, APILocator.getRoleAPI().getUserRole(user),
                PermissionAPI.PERMISSION_READ | PERMISSION_PUBLISH);
        final Link link = new LinkDataGen(folder).nextPersisted(false);
        final ActionRequestImpl request = publishRequest(user, link);

        new PublishLinksAction()._publishWebAsset(request, mock(ActionResponse.class), null, null,
                user, WebKeys.LINK_FORM_EDIT);

        assertTrue(isLive(link));
        assertTrue(SessionMessages.contains(request, SUCCESS_MESSAGE));
        assertFalse(SessionMessages.contains(request, ERROR_MESSAGE));
    }

    /**
     * A back-end user, with the extra roles given, who can read the test site and whatever is
     * created in it afterwards, and holds nothing else there. Created before the test's folders
     * and links so that they inherit that READ.
     */
    private static User backendUser(final Role... extraRoles)
            throws DotDataException, DotSecurityException {
        final List<Role> roles = new ArrayList<>(List.of(TestUserUtils.getBackendRole()));
        roles.addAll(List.of(extraRoles));
        final User user = new UserDataGen().roles(roles.toArray(new Role[0])).nextPersisted();
        usersToClean.add(user);
        grant(site, APILocator.getRoleAPI().getUserRole(user), PermissionAPI.PERMISSION_READ);
        return user;
    }

    /** Gives the role the permission bits on the asset and on the folders and links under it. */
    private static void grant(final Permissionable asset, final Role role, final int permission)
            throws DotDataException, DotSecurityException {
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        permissionAPI.save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                asset.getPermissionId(), role.getId(), permission, true), asset, systemUser, false);
        for (final Class<?> childType : List.of(Folder.class, Link.class)) {
            permissionAPI.save(new Permission(childType.getCanonicalName(),
                    asset.getPermissionId(), role.getId(), permission, true), asset, systemUser,
                    false);
        }
    }

    private static boolean isLive(final Link link) throws Exception {
        return APILocator.getVersionableAPI().isLive(link);
    }

    /**
     * A request of the given user carrying the link under {@link WebKeys#LINK_FORM_EDIT}, with a
     * session that names the test site and keeps the messages the action adds.
     */
    private static ActionRequestImpl publishRequest(final User user, final Link link) {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);

        final Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put(WebKeys.CMS_SELECTED_HOST_ID, site.getIdentifier());
        final HttpSession httpSession = mock(HttpSession.class);
        when(httpSession.getAttribute(anyString()))
                .thenAnswer(invocation -> sessionAttributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> sessionAttributes.put(invocation.getArgument(0),
                invocation.getArgument(1))).when(httpSession).setAttribute(anyString(), any());
        when(httpRequest.getSession()).thenReturn(httpSession);
        when(httpRequest.getSession(false)).thenReturn(httpSession);
        final PortletSessionImpl portletSession = mock(PortletSessionImpl.class);
        when(portletSession.getSession()).thenReturn(httpSession);

        final ActionRequestImpl request = mock(ActionRequestImpl.class);
        when(request.getHttpServletRequest()).thenReturn(httpRequest);
        when(request.getPortletSession()).thenReturn(portletSession);
        when(request.getAttribute(WebKeys.LINK_FORM_EDIT)).thenReturn(link);
        return request;
    }
}
