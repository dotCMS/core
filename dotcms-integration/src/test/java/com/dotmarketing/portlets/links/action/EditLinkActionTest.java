package com.dotmarketing.portlets.links.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.struts.LinkForm;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.PortletConfigImpl;
import com.liferay.portlet.PortletSessionImpl;
import com.liferay.util.servlet.SessionMessages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link EditLinkAction#processAction} with {@code cmd=edit}, the Site
 * Browser's Edit on a menu link: a link locked by another user is not opened, the user is sent
 * back to where they came from, and the lock stays with its holder; a link locked by the caller
 * opens in the editor.
 */
public class EditLinkActionTest {

    /** What the Site Browser's referer arrives as: its own query parameters are cut off. */
    private static final String REFERER = "/c/portal/layout?p_l_id=test-layout";
    /** The referer plus the parameters that reopen the Site Browser portlet maximized. */
    private static final String SITE_BROWSER_RETURN =
            REFERER + "&p_p_state=maximized&p_p_id=site-browser&p_p_action=1";

    private static User systemUser;
    private static Host site;
    private static final List<User> usersToClean = new ArrayList<>();

    @BeforeAll
    static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
    }

    /**
     * Deletes the test site, with every folder and link created in it, and the test users. A
     * failure here is logged and does not hide the test results.
     */
    @AfterAll
    static void cleanUp() {
        if (site != null) {
            try {
                APILocator.getHostAPI().archive(site, systemUser, false);
                APILocator.getHostAPI().delete(site, systemUser, false);
            } catch (final Exception e) {
                Logger.warn(EditLinkActionTest.class, "Could not delete the test site: "
                        + e.getMessage());
            }
        }
        for (final User user : usersToClean) {
            try {
                APILocator.getUserAPI().delete(user, TestUserUtils.getAdminUser(), systemUser,
                        false);
            } catch (final Exception e) {
                Logger.warn(EditLinkActionTest.class, "Could not delete a test user", e);
            }
        }
    }

    /**
     * Method to test: {@link EditLinkAction#processAction}
     * Given Scenario: A link is locked by another user; the caller can read and write it and
     * opens it with {@code cmd=edit} from the Site Browser
     * ExpectedResult: The caller is sent back to the Site Browser with the "locked" message, and the
     * link is still locked by the other user
     */
    @Test
    void test_processAction_editLinkLockedByOther_sendsBackWithMessage() throws Exception {
        final User lockHolder = backendUser();
        final User caller = backendUser();
        final Folder folder = folderWritableBy(caller);
        final Link link = lockedLink(folder, lockHolder);

        final EditRun run = edit(caller, link, REFERER);

        assertEquals(List.of(SITE_BROWSER_RETURN), run.redirects);
        assertTrue(SessionMessages.contains(run.session, "custommessage"),
                "the \"locked\" message is kept in the session for the referer page");
        assertEquals(Optional.of(lockHolder.getUserId()), lockedBy(link));
    }

    /**
     * Method to test: {@link EditLinkAction#processAction}
     * Given Scenario: A link is locked by another user; the caller can only read it and opens it
     * with {@code cmd=edit} from the Site Browser
     * ExpectedResult: The caller is sent back to the Site Browser, and the link is still locked by the
     * other user
     */
    @Test
    void test_processAction_editLinkLockedByOtherReadOnly_sendsBack() throws Exception {
        final User lockHolder = backendUser();
        final User caller = backendUser();
        final Link link = lockedLink(new FolderDataGen().site(site).nextPersisted(), lockHolder);

        final EditRun run = edit(caller, link, REFERER);

        assertEquals(List.of(SITE_BROWSER_RETURN), run.redirects);
        assertEquals(Optional.of(lockHolder.getUserId()), lockedBy(link));
    }

    /**
     * Method to test: {@link EditLinkAction#processAction}
     * Given Scenario: A link is locked by another user; the caller opens it with
     * {@code cmd=edit} and no referer
     * ExpectedResult: The caller is not sent to another action, and the link is still locked by
     * the other user
     */
    @Test
    void test_processAction_editLinkLockedByOtherNoReferer_doesNotRedirect() throws Exception {
        final User lockHolder = backendUser();
        final User caller = backendUser();
        final Folder folder = folderWritableBy(caller);
        final Link link = lockedLink(folder, lockHolder);

        final EditRun run = edit(caller, link, null);

        assertTrue(run.redirects.isEmpty(), "no redirect: " + run.redirects);
        assertEquals(Optional.of(lockHolder.getUserId()), lockedBy(link));
    }

    /**
     * Method to test: {@link EditLinkAction#processAction}
     * Given Scenario: The caller holds the lock on a link and opens it with {@code cmd=edit}
     * ExpectedResult: The editor opens (no redirect), and the caller still holds the lock
     */
    @Test
    void test_processAction_editLinkLockedByCaller_opensEditor() throws Exception {
        final User caller = backendUser();
        final Folder folder = folderWritableBy(caller);
        final Link link = lockedLink(folder, caller);

        final EditRun run = edit(caller, link, REFERER);

        assertTrue(run.redirects.isEmpty(), "no redirect: " + run.redirects);
        assertNotNull(run.forward, "the link editor is shown");
        assertEquals(Optional.of(caller.getUserId()), lockedBy(link));
    }

    /**
     * A back-end user who can read the test site and whatever is in it, and holds nothing else
     * there.
     */
    private static User backendUser() throws DotDataException, DotSecurityException {
        final User user = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        usersToClean.add(user);
        grant(site, APILocator.getRoleAPI().getUserRole(user), PermissionAPI.PERMISSION_READ);
        return user;
    }

    /** A new folder of the test site where the user can read, write and add links. */
    private static Folder folderWritableBy(final User user)
            throws DotDataException, DotSecurityException {
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        grant(folder, APILocator.getRoleAPI().getUserRole(user), PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN);
        return folder;
    }

    /** Gives the role the permission bits on the asset and on the assets of each type under it. */
    private static void grant(final Permissionable asset, final Role role, final int permission)
            throws DotDataException, DotSecurityException {
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        permissionAPI.save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                asset.getPermissionId(), role.getId(), permission, true), asset, systemUser, false);
        for (final Class<?> childType : List.of(Folder.class, Link.class, Contentlet.class)) {
            permissionAPI.save(new Permission(childType.getCanonicalName(),
                    asset.getPermissionId(), role.getId(), permission, true), asset, systemUser,
                    false);
        }
    }

    /** A new link in the folder, locked by the given user. */
    private static Link lockedLink(final Folder folder, final User lockOwner) throws Exception {
        final Link link = new LinkDataGen(folder).nextPersisted(false);
        APILocator.getVersionableAPI().setLocked(link, true, lockOwner);
        return link;
    }

    private static Optional<String> lockedBy(final Link link) throws DotDataException {
        return APILocator.getVersionableAPI().getLockedBy(link);
    }

    /** What the action did with one request: the redirects it sent, its forward, the session. */
    private record EditRun(List<String> redirects, String forward, HttpSession session) {
    }

    /**
     * Opens the link with {@code cmd=edit} as the given user, from a Site Browser portlet page.
     *
     * @param referer the page the user came from, or {@code null} for none
     */
    private static EditRun edit(final User user, final Link link, final String referer)
            throws Exception {
        final Map<String, String> parameters = new HashMap<>();
        parameters.put("cmd", "edit");
        parameters.put("inode", link.getInode());
        if (referer != null) {
            parameters.put("referer", referer);
        }
        final Map<String, Object> sessionAttributes = new HashMap<>();
        final Map<String, Object> requestAttributes = new HashMap<>();
        final ActionRequestImpl request = actionRequest(user, parameters, sessionAttributes,
                requestAttributes);

        final List<String> redirects = new ArrayList<>();
        final ActionResponse response = mock(ActionResponse.class);
        doAnswer(invocation -> redirects.add(invocation.getArgument(0)))
                .when(response).sendRedirect(anyString());

        try {
            new EditLinkAction().processAction(null, new LinkForm(), null, request, response);
        } finally {
            // a request ends by closing its connection; without this, a transaction the action
            // leaves open would carry over into the next test's setup
            DbConnectionFactory.closeSilently();
        }

        return new EditRun(redirects,
                (String) requestAttributes.get(WebKeys.PORTLET_STRUTS_FORWARD),
                request.getHttpServletRequest().getSession());
    }

    /**
     * A portlet request of the given user carrying the parameters, whose request and session
     * keep what the action stores in them. The page is the Site Browser portlet of a layout, so
     * the action can build portlet URLs.
     */
    private static ActionRequestImpl actionRequest(final User user,
            final Map<String, String> parameters, final Map<String, Object> sessionAttributes,
            final Map<String, Object> requestAttributes) {
        final PortletConfigImpl portletConfig = mock(PortletConfigImpl.class);
        when(portletConfig.getPortletId()).thenReturn("site-browser");
        final Layout layout = mock(Layout.class);
        when(layout.getId()).thenReturn("test-layout");
        requestAttributes.put(WebKeys.JAVAX_PORTLET_CONFIG, portletConfig);
        requestAttributes.put(WebKeys.LAYOUT, layout);
        requestAttributes.put(WebKeys.USER, user);

        final HttpSession httpSession = mock(HttpSession.class);
        when(httpSession.getAttribute(anyString()))
                .thenAnswer(invocation -> sessionAttributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> sessionAttributes.put(invocation.getArgument(0),
                invocation.getArgument(1))).when(httpSession).setAttribute(anyString(), any());

        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getAttribute(anyString()))
                .thenAnswer(invocation -> requestAttributes.get(invocation.<String>getArgument(0)));
        when(httpRequest.getSession()).thenReturn(httpSession);
        when(httpRequest.getSession(false)).thenReturn(httpSession);
        when(httpRequest.getParameter(anyString()))
                .thenAnswer(invocation -> parameters.get(invocation.<String>getArgument(0)));
        final PortletSessionImpl portletSession = mock(PortletSessionImpl.class);
        when(portletSession.getSession()).thenReturn(httpSession);

        final ActionRequestImpl request = mock(ActionRequestImpl.class);
        when(request.getHttpServletRequest()).thenReturn(httpRequest);
        when(request.getPortletSession()).thenReturn(portletSession);
        when(request.getParameter(anyString()))
                .thenAnswer(invocation -> parameters.get(invocation.<String>getArgument(0)));
        when(request.getAttribute(anyString()))
                .thenAnswer(invocation -> requestAttributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> requestAttributes.put(invocation.getArgument(0),
                invocation.getArgument(1))).when(request).setAttribute(anyString(), any());
        return request;
    }
}
