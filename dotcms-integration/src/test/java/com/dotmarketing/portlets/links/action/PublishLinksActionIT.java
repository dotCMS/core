package com.dotmarketing.portlets.links.action;

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
import com.dotcms.repackage.javax.portlet.ActionResponse;
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
import com.dotmarketing.portlets.links.model.Link;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
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
 * Integration tests for {@link PublishLinksAction#processAction}: each posted link is made live
 * only when the user holds PUBLISH on it, and the outcome is reported through the session
 * messages the Links screen shows. The users can read the site and everything in it, which is
 * what any back-end user working on the site can do.
 */
public class PublishLinksActionIT {

    private static final String SUCCESS_MESSAGE = "message";
    private static final String ERROR_MESSAGE = "error";

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
     * Method to test: {@link PublishLinksAction#processAction}
     * Given Scenario: A back-end user who can read the site but holds no PUBLISH on the link posts
     * its inode
     * ExpectedResult: The link stays unpublished and the screen gets an error, not a success
     */
    @Test
    void test_processAction_userWithoutPublish_leavesLinkUnpublished() throws Exception {
        final User user = backendUser();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        final Link link = new LinkDataGen(folder).nextPersisted(false);
        final ActionRequestImpl request = publishRequest(user, link.getInode());

        new PublishLinksAction().processAction(null, null, null, request,
                mock(ActionResponse.class));

        assertFalse(isLive(link));
        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
    }

    /**
     * Method to test: {@link PublishLinksAction#processAction}
     * Given Scenario: A back-end user holding PUBLISH on the link's folder posts its inode
     * ExpectedResult: The link goes live and the screen gets the success message
     */
    @Test
    void test_processAction_userWithPublish_publishesLink() throws Exception {
        final User user = backendUser();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        grant(folder, user, PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_PUBLISH);
        final Link link = new LinkDataGen(folder).nextPersisted(false);
        final ActionRequestImpl request = publishRequest(user, link.getInode());

        new PublishLinksAction().processAction(null, null, null, request,
                mock(ActionResponse.class));

        assertTrue(isLive(link));
        assertTrue(SessionMessages.contains(request, SUCCESS_MESSAGE));
        assertFalse(SessionMessages.contains(request, ERROR_MESSAGE));
    }

    /**
     * Method to test: {@link PublishLinksAction#processAction}
     * Given Scenario: One request with two inodes, where the user may publish only the first link
     * ExpectedResult: The first link goes live, the second stays unpublished, and the screen gets
     * both the success and the error message
     */
    @Test
    void test_processAction_mixedPermissions_publishesOnlyAllowedLinks() throws Exception {
        final User user = backendUser();
        final Folder allowedFolder = new FolderDataGen().site(site).nextPersisted();
        final Folder otherFolder = new FolderDataGen().site(site).nextPersisted();
        grant(allowedFolder, user,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_PUBLISH);
        final Link allowedLink = new LinkDataGen(allowedFolder).nextPersisted(false);
        final Link otherLink = new LinkDataGen(otherFolder).nextPersisted(false);
        final ActionRequestImpl request = publishRequest(user, allowedLink.getInode(),
                otherLink.getInode());

        new PublishLinksAction().processAction(null, null, null, request,
                mock(ActionResponse.class));

        assertTrue(isLive(allowedLink));
        assertFalse(isLive(otherLink));
        assertTrue(SessionMessages.contains(request, SUCCESS_MESSAGE));
        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
    }

    /**
     * A back-end user who can read the test site and whatever is created in it afterwards, and
     * holds nothing else there. Created before the test's folders and links so that they inherit
     * that READ.
     */
    private static User backendUser() throws DotDataException, DotSecurityException {
        final User user = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        usersToClean.add(user);
        grant(site, user, PermissionAPI.PERMISSION_READ);
        return user;
    }

    /**
     * Gives the user's own role the permission bits on the asset and on the folders and links
     * created under it.
     */
    private static void grant(final Permissionable asset, final User user, final int permission)
            throws DotDataException, DotSecurityException {
        final Role role = APILocator.getRoleAPI().getUserRole(user);
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
     * A publish request of the given user carrying the given {@code publishInode} values, with a
     * portlet session that keeps the messages the action adds.
     */
    private static ActionRequestImpl publishRequest(final User user, final String... inodes) {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getAttribute(WebKeys.USER)).thenReturn(user);

        final Map<String, Object> sessionAttributes = new HashMap<>();
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
        when(request.getParameterValues("publishInode")).thenReturn(inodes);
        return request;
    }
}
