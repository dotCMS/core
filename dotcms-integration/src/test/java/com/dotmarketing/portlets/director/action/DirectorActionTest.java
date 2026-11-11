package com.dotmarketing.portlets.director.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
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
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.PortletSessionImpl;
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
 * Integration tests for {@link DirectorAction#processAction} on the {@code editLink},
 * {@code editFile}, {@code editTemplate} and {@code editHTMLPage} commands: a {@code subcmd}
 * parameter does not change who holds the edit lock on the asset, and {@code editLink} releases
 * a lock held by the caller who last saved the link. The caller is a back-end user with READ on
 * the test site and everything in it; in the {@code editLink} release test it can also save links
 * in a folder of its own.
 */
public class DirectorActionTest {

    private static final String DIRECTOR_ACTION = "/ext/director/direct";

    private static User systemUser;
    private static Host site;
    private static User owner;
    private static final List<User> usersToClean = new ArrayList<>();

    @BeforeAll
    static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
        owner = backendUser();
    }

    /**
     * Deletes the test site, with every folder, link, template and page created in it, and the
     * test users. A failure here is logged and does not hide the test results.
     */
    @AfterAll
    static void cleanUp() {
        if (site != null) {
            try {
                APILocator.getHostAPI().archive(site, systemUser, false);
                APILocator.getHostAPI().delete(site, systemUser, false);
            } catch (final Exception e) {
                Logger.warn(DirectorActionTest.class, "Could not delete the test site: "
                        + e.getMessage());
            }
        }
        for (final User user : usersToClean) {
            try {
                APILocator.getUserAPI().delete(user, TestUserUtils.getAdminUser(), systemUser,
                        false);
            } catch (final Exception e) {
                Logger.warn(DirectorActionTest.class, "Could not delete a test user", e);
            }
        }
    }

    /**
     * Method to test: {@link DirectorAction#processAction}
     * Given Scenario: A link is locked by another user; a back-end user who can only read it
     * calls {@code cmd=editLink} with {@code subcmd=unlockLink}
     * ExpectedResult: The link is still locked by the other user
     */
    @Test
    void test_processAction_editLinkWithSubcommand_keepsLockOfOwner() throws Exception {
        final User caller = backendUser();
        final Link link = lockedLink(owner);

        runDirector(caller, Map.of("cmd", "editLink", "subcmd", "unlockLink",
                "link", link.getInode()));

        assertEquals(Optional.of(owner.getUserId()), lockedBy(link));
    }

    /**
     * Method to test: {@link DirectorAction#processAction}
     * Given Scenario: A link is locked by another user; a back-end user who can only read it
     * sends its inode to {@code cmd=editFile} with {@code subcmd=unlockFile}
     * ExpectedResult: The link is still locked by the other user
     */
    @Test
    void test_processAction_editFileWithSubcommand_keepsLockOfOwner() throws Exception {
        final User caller = backendUser();
        final Link link = lockedLink(owner);

        runDirector(caller, Map.of("cmd", "editFile", "subcmd", "unlockFile",
                "file", link.getInode()));

        assertEquals(Optional.of(owner.getUserId()), lockedBy(link));
    }

    /**
     * Method to test: {@link DirectorAction#processAction}
     * Given Scenario: A template is locked by another user; a back-end user who can only read it
     * calls {@code cmd=editTemplate} with {@code subcmd=unlockTemplate}
     * ExpectedResult: The template is still locked by the other user
     */
    @Test
    void test_processAction_editTemplateWithSubcommand_keepsLockOfOwner() throws Exception {
        final User caller = backendUser();
        final Template template = new TemplateDataGen().site(site).nextPersisted();
        APILocator.getVersionableAPI().setLocked(template, true, owner);

        runDirector(caller, Map.of("cmd", "editTemplate", "subcmd", "unlockTemplate",
                "template", template.getIdentifier()));

        assertEquals(Optional.of(owner.getUserId()), lockedBy(template));
    }

    /**
     * Method to test: {@link DirectorAction#processAction}
     * Given Scenario: A page is locked by another user; a back-end user who can only read it
     * calls {@code cmd=editHTMLPage} with {@code subcmd=unlockHTMLPage}
     * ExpectedResult: The page is still locked by the other user
     */
    @Test
    void test_processAction_editHTMLPageWithSubcommand_keepsLockOfOwner() throws Exception {
        final User caller = backendUser();
        final Template template = new TemplateDataGen().site(site).nextPersisted();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
        APILocator.getVersionableAPI().setLocked(page, true, owner);

        runDirector(caller, Map.of("cmd", "editHTMLPage", "subcmd", "unlockHTMLPage",
                "htmlPage", page.getInode()));

        assertEquals(Optional.of(owner.getUserId()), lockedBy(page));
    }

    /**
     * Method to test: {@link DirectorAction#processAction}
     * Given Scenario: A back-end user last saved a link and holds its lock, and opens it through
     * {@code cmd=editLink} with no {@code subcmd}, as the Links editor does when the link is locked
     * ExpectedResult: The lock is released
     */
    @Test
    void test_processAction_editLinkLockedByCaller_releasesLock() throws Exception {
        final User caller = backendUser();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        grant(folder, APILocator.getRoleAPI().getUserRole(caller),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE
                        | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN);
        final Link link = new LinkDataGen(folder).next();
        APILocator.getMenuLinkAPI().save(link, folder, caller, false);
        APILocator.getVersionableAPI().setLocked(link, true, caller);
        assertEquals(caller.getUserId(), link.getModUser());

        runDirector(caller, Map.of("cmd", "editLink", "link", link.getInode()));

        assertTrue(lockedBy(link).isEmpty());
    }

    /**
     * A back-end user who can read the test site and whatever is created in it afterwards, and
     * holds nothing else there. Created before the test's assets so that they inherit that READ.
     */
    private static User backendUser() throws DotDataException, DotSecurityException {
        final User user = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        usersToClean.add(user);
        grant(site, APILocator.getRoleAPI().getUserRole(user), PermissionAPI.PERMISSION_READ);
        return user;
    }

    /** Gives the role the permission bits on the asset and on the assets of each type under it. */
    private static void grant(final Permissionable asset, final Role role, final int permission)
            throws DotDataException, DotSecurityException {
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        permissionAPI.save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                asset.getPermissionId(), role.getId(), permission, true), asset, systemUser, false);
        for (final Class<?> childType : List.of(Folder.class, Link.class, Template.class,
                Contentlet.class, IHTMLPage.class)) {
            permissionAPI.save(new Permission(childType.getCanonicalName(),
                    asset.getPermissionId(), role.getId(), permission, true), asset, systemUser,
                    false);
        }
    }

    /** A new link in a new folder of the test site, locked by the given user. */
    private static Link lockedLink(final User lockOwner) throws Exception {
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        final Link link = new LinkDataGen(folder).nextPersisted(false);
        APILocator.getVersionableAPI().setLocked(link, true, lockOwner);
        return link;
    }

    private static Optional<String> lockedBy(final Versionable asset) throws DotDataException {
        return APILocator.getVersionableAPI().getLockedBy(asset);
    }

    /** Sends the parameters to the director action as the given user. */
    private static void runDirector(final User user, final Map<String, String> parameters)
            throws Exception {
        new DirectorAction().processAction(null, null, null, directorRequest(user, parameters),
                mock(ActionResponse.class));
    }

    /**
     * A request of the given user to {@value #DIRECTOR_ACTION} carrying the given parameters,
     * with a session that keeps what the action stores in it.
     */
    private static ActionRequestImpl directorRequest(final User user,
            final Map<String, String> parameters) {
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
        when(request.getParameter(anyString()))
                .thenAnswer(invocation -> parameters.get(invocation.<String>getArgument(0)));
        return request;
    }
}
