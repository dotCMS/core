package com.dotmarketing.portlets.browser.ajax;

import com.dotcms.datagen.TestUserUtils;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.rest.api.v1.browsertree.BrowserTreeHelper;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.ThreadUtilsTest;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.ajax.DwrUtil;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.ThreadUtils;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.servlet.SessionMessages;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.MockedStatic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This test class validates the correct behavior of the methods exposed in the {@link BrowserAjax} class.
 *
 * @author Jose Castro
 * @since Apr 18th, 2023
 */
public class BrowserAjaxTest {

    /** Synthetic MIME type the legacy browser dialogs send for Pages. See {@code PageViewStrategy}. */
    private static final String DOTPAGE_MIME_TYPE = "application/dotpage";

    private static Host testSite = null;
    private static Folder parentFolderOne = null;
    private static Folder parentFolderTwo = null;
    private static Folder childFolderOne = null;
    private static Folder childFolderTwo = null;

    private static HttpSession session = null;

    // Folder tree used by the read-permission tests:
    //
    // |_ readSite
    //      |_ readable                (limitedUser: READ; children inherit READ + WRITE)
    //          |_ readable-child
    //              |_ readable-grandchild
    //          |_ restricted-child    (only fullReadUser)
    //              |_ restricted-grandchild
    //          |_ write-only-child    (limitedUser: WRITE without READ; fullReadUser: READ)
    //      |_ restricted-root         (only fullReadUser)
    private static Host readSite = null;
    private static Folder readableFolder = null;
    private static Folder readableChild = null;
    private static Folder readableGrandchild = null;
    private static Folder restrictedChild = null;
    private static Folder restrictedGrandchild = null;
    private static Folder writeOnlyChild = null;
    private static Folder restrictedRoot = null;
    private static User limitedUser = null;
    private static User fullReadUser = null;

    /**
     * Set up the DWR context and data for this test. The Site's folder tree looks like this:
     *
     * |_test-site
     *      |_ parent-one
     *          |_child-one
     *              |_child-two
     *      |_parent-two
     *
     * The read-permission tests use a second Site, described next to the {@code readSite} field.
     *
     * @throws Exception An error occurred during the setup.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        final long time = System.currentTimeMillis();
        testSite = new SiteDataGen().name("browserajaxtest." + time).nextPersisted();

        parentFolderOne = new FolderDataGen().site(testSite).name("parent-one").nextPersisted();
        parentFolderTwo = new FolderDataGen().site(testSite).name("parent-two").nextPersisted();

        childFolderOne = new FolderDataGen().site(testSite).parent(parentFolderOne).name("child-one").nextPersisted();
        childFolderTwo = new FolderDataGen().site(testSite).parent(childFolderOne).name("child-two").nextPersisted();

        prepareReadPermissionTree(time);
        setUpDwrContext(TestUserUtils.getAdminUser());
    }

    /**
     * Creates the Site, Folders and Users used by the read-permission tests. The limited user can read the Site and
     * the {@code readable} folder branch only; the full-read user can read every folder.
     *
     * @param time Suffix that keeps the Site and User names unique.
     */
    private static void prepareReadPermissionTree(final long time) throws DotDataException, DotSecurityException {
        final Role backEndRole = APILocator.getRoleAPI().loadBackEndUserRole();
        limitedUser = new UserDataGen().firstName("limited" + time).roles(backEndRole).nextPersisted();
        fullReadUser = new UserDataGen().firstName("fullread" + time).roles(backEndRole).nextPersisted();
        final Role limitedRole = APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId());
        final Role fullReadRole = APILocator.getRoleAPI().loadRoleByKey(fullReadUser.getUserId());

        readSite = new SiteDataGen().name("browserajaxread." + time).nextPersisted();
        readableFolder = new FolderDataGen().site(readSite).name("readable").nextPersisted();
        readableChild = new FolderDataGen().site(readSite).parent(readableFolder).name("readable-child").nextPersisted();
        readableGrandchild = new FolderDataGen().site(readSite).parent(readableChild).name("readable-grandchild")
                .nextPersisted();
        restrictedChild = new FolderDataGen().site(readSite).parent(readableFolder).name("restricted-child")
                .nextPersisted();
        restrictedGrandchild = new FolderDataGen().site(readSite).parent(restrictedChild)
                .name("restricted-grandchild").nextPersisted();
        writeOnlyChild = new FolderDataGen().site(readSite).parent(readableFolder).name("write-only-child")
                .nextPersisted();
        restrictedRoot = new FolderDataGen().site(readSite).name("restricted-root").nextPersisted();

        final String folderType = Folder.class.getCanonicalName();
        final int read = PermissionAPI.PERMISSION_READ;
        final int readWrite = PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE;
        for (final Role role : List.of(limitedRole, fullReadRole)) {
            grant(readSite, role, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, read);
            grant(readableFolder, role, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, read);
            grant(readableFolder, role, folderType, readWrite);
        }
        for (final Folder folder : List.of(restrictedChild, restrictedRoot)) {
            grant(folder, fullReadRole, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, read);
            grant(folder, fullReadRole, folderType, read);
        }
        grant(writeOnlyChild, limitedRole, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_WRITE);
        grant(writeOnlyChild, fullReadRole, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, read);

        // The tests are only meaningful if the fixture grants exactly what the tree above describes
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        for (final Folder folder : List.of(readableFolder, readableChild, readableGrandchild)) {
            assertTrue("Limited user must read " + folder.getPath(),
                    permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, limitedUser));
        }
        // Same check through the role-based lookup the Site Browser tree uses
        final List<Integer> readableChildPermissions = permissionAPI.getPermissionIdsFromRoles(readableChild,
                DwrUtil.getUserRoles(limitedUser), limitedUser);
        assertTrue("Limited user must read and edit " + readableChild.getPath(),
                readableChildPermissions.containsAll(List.of(PermissionAPI.PERMISSION_READ,
                        PermissionAPI.PERMISSION_WRITE)));
        for (final Folder folder : List.of(restrictedChild, restrictedGrandchild, writeOnlyChild, restrictedRoot)) {
            assertFalse("Limited user must not read " + folder.getPath(),
                    permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, limitedUser));
            assertTrue("Full-read user must read " + folder.getPath(),
                    permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, fullReadUser));
        }
    }

    /**
     * Saves one permission row on the given asset for the given Role.
     *
     * @param permissionable The Site or Folder receiving the permission.
     * @param role           The Role being granted.
     * @param type           {@link PermissionAPI#INDIVIDUAL_PERMISSION_TYPE} for the asset itself, or the class name
     *                       of the children that inherit it.
     * @param permission     The permission bits.
     */
    private static void grant(final Permissionable permissionable, final Role role, final String type,
                              final int permission) throws DotDataException, DotSecurityException {
        APILocator.getPermissionAPI().save(
                new Permission(type, permissionable.getPermissionId(), role.getId(), permission, true),
                permissionable, APILocator.systemUser(), false);
    }

    /**
     * Removes the Site, Folders and Users created for the read-permission tests.
     */
    @AfterClass
    public static void cleanUpReadPermissionTree() {
        if (null != restrictedRoot) {
            FolderDataGen.remove(restrictedRoot);
        }
        if (null != readableFolder) {
            FolderDataGen.remove(readableFolder);
        }
        if (null != limitedUser) {
            UserDataGen.remove(limitedUser, true);
        }
        if (null != fullReadUser) {
            UserDataGen.remove(fullReadUser, true);
        }
        if (null != readSite) {
            try {
                APILocator.getHostAPI().archive(readSite, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(readSite, APILocator.systemUser(), false);
            } catch (final Exception e) {
                // Best effort: a leftover Site does not affect other tests
            }
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getTree(String)}</li>
     *     <li><b>Given Scenario:</b> Display the folder tree for a specific Site in the Site Browser.</li>
     *     <li><b>Expected Result:</b> If a Site is set to be displayed in the Site Browser, the returned data tree must
     *     reflect only the direct child Folders under such a Site in the UI.</li>
     * </ul>
     * This is how the folder tree must look like:
     * <pre>
     *      --> test-site [open]
     *          |_ parent-one
     *          |_ parent-two
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testSelectingSiteRoot() throws Exception {
        // Set test values in session
        when(session.getAttribute(BrowserTreeHelper.ACTIVE_FOLDER_ID)).thenReturn(null);
        when(session.getAttribute(BrowserTreeHelper.OPEN_FOLDER_IDS)).thenReturn(new HashSet<>());

        final BrowserAjax browserAjax = new BrowserAjax();
        final List<Map> folderTree = browserAjax.getTree(testSite.getIdentifier());

        // Check the main Site folder tree structure
        // Site MUST be "open"
        assertNotNull("Folder tree must never be null", folderTree);
        assertEquals("There must be only one Site folder tree", 1, folderTree.size());
        final Map<String, Object> siteFolderTree = folderTree.get(0);
        assertEquals("The returned Site ID [" + siteFolderTree.get("identifier") + "] doesn't match the ID of the " +
                             "test Site [" + testSite.getIdentifier() + "]", testSite.getIdentifier(),
                siteFolderTree.get("identifier"));
        assertTrue("The Site node must be open", (boolean) siteFolderTree.get("open"));

        // Only parent-one MUST be open
        // parent-two MUST NOT be neither "open" nor "selected"
        final List<Map<String, Object>> parentFolders = (List<Map<String, Object>>) siteFolderTree.get("childrenFolders");
        assertEquals("There must be two parent Folders for the Test Site", 2, parentFolders.size());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getTree(String)}</li>
     *     <li><b>Given Scenario:</b> The grandchild of a folder has been selected via the HTTP Session so that the Site
     *     Browser displays it in the UI.</li>
     *     <li><b>Expected Result:</b> If a sub-folder is set to be displayed in the Site Browser, the returned data
     *     tree must reflect both its selected status and open up all the respective parent folders so that it can be
     *     seen correctly in the UI.</li>
     * </ul>
     * This is how the folder tree must look like:
     * <pre>
     *      --> test-site [open]
     *          |_ parent-one [open]
     *              |_child-one [open]
     *                  |_child-two [open, selected]
     *          |_ parent-two
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testExpectedFolderTreeInfo() throws Exception {
        // Set test values in session
        when(session.getAttribute(BrowserTreeHelper.ACTIVE_FOLDER_ID)).thenReturn(childFolderTwo.getIdentifier());
        final Set<String> openFolderIds = CollectionsUtils.set(parentFolderOne.getIdentifier(),
                childFolderOne.getIdentifier(), childFolderTwo.getIdentifier());
        when(session.getAttribute(BrowserTreeHelper.OPEN_FOLDER_IDS)).thenReturn(openFolderIds);

        final BrowserAjax browserAjax = new BrowserAjax();
        final List<Map> folderTree = browserAjax.getTree(testSite.getIdentifier());

        // Only parent-one MUST be open
        // parent-two MUST NOT be neither "open" nor "selected"
        final Map<String, Object> siteFolderTree = folderTree.get(0);
        final List<Map<String, Object>> parentFolders = (List<Map<String, Object>>) siteFolderTree.get("childrenFolders");
        assertEquals("There must be two parent Folders for the Test Site", 2, parentFolders.size());
        assertTrue("Folder '" + parentFolderOne.getPath() + "' must be open", (boolean) parentFolders.get(0).get("open"));
        assertFalse("Folder '" + parentFolderOne.getPath() + "' must NOT be selected", (boolean) parentFolders.get(0).get("selected"));

        assertFalse("Folder '" + parentFolderTwo.getPath() + "' must NOT be open", (boolean) parentFolders.get(1).get("open"));
        assertFalse("Folder '" + parentFolderTwo.getPath() + "' must NOT be selected", (boolean) parentFolders.get(1).get("selected"));

        // Check the parent-one folder tree structure
        final List<Map<String, Object>> parentFolderOneData = (List<Map<String, Object>>) parentFolders.get(0).get("childrenFolders");
        assertEquals("There must be only one child folder under '" + parentFolderOne.getPath() + "'", 1,
                parentFolderOneData.size());

        // Check the child-one folder tree structure
        // child-one MUST be "open" but NOT "selected"
        final Map<String, Object> childFolderOneData = parentFolderOneData.get(0);
        assertEquals("The returned Child One ID [" + childFolderOneData.get("identifier") + "] doesn't match the ID of the " +
                             "test Child One [" + childFolderOne.getIdentifier() + "]", childFolderOne.getIdentifier(),
                childFolderOneData.get("identifier"));
        assertTrue("Folder '" + childFolderOne.getPath() + "' must be open", (boolean) childFolderOneData.get("open"));
        assertFalse("Folder '" + childFolderOne.getPath() + "' must NOT be selected", (boolean) childFolderOneData.get("selected"));

        // Check the child-one folder tree structure
        final List<Map<String, Object>> parentFolderTwoData = (List<Map<String, Object>>) childFolderOneData.get("childrenFolders");
        assertEquals("There must be only one child folder under '" + childFolderOne.getPath() + "'", 1,
                parentFolderOneData.size());

        // Check the child-two folder tree structure
        // child-two MUST be "open" AND "selected"
        final Map<String, Object> childFolderTwoData = parentFolderTwoData.get(0);
        assertEquals("The returned Child Two ID [" + childFolderTwoData.get("identifier") + "] doesn't match the ID of the " +
                             "test Child Two [" + childFolderTwo.getIdentifier() + "]", childFolderTwo.getIdentifier(),
                childFolderTwoData.get("identifier"));
        assertTrue("Folder '" + childFolderTwo.getPath() + "' must be open", (boolean) childFolderTwoData.get("open"));
        assertTrue("Folder '" + childFolderTwo.getPath() + "' must be selected", (boolean) childFolderTwoData.get("selected"));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getTree(String)}</li>
     *     <li><b>Given Scenario:</b> The grandchild of a folder has been selected via the HTTP Session so that the Site
     *     Browser displays it in the UI.</li>
     *     <li><b>Expected Result:</b> If a sub-folder is set to be displayed in the Site Browser, the returned data
     *     tree must reflect both its selected status and open up all the respective parent folders so that it can be
     *     seen correctly in the UI.</li>
     * </ul>
     * This is how the folder tree must look like:
     * <pre>
     *      --> test-site [open]
     *          |_ parent-one
     *          |_ parent-two
     * </pre>
     * @throws Exception
     */
    @Test
    public void testFolderTreeInfoWithInvalidFolderId() throws Exception {
        // Set test values in session
        when(session.getAttribute(BrowserTreeHelper.ACTIVE_FOLDER_ID)).thenReturn("123");
        when(session.getAttribute(BrowserTreeHelper.OPEN_FOLDER_IDS)).thenReturn(new HashSet<>());

        final BrowserAjax browserAjax = new BrowserAjax();
        final List<Map> folderTree = browserAjax.getTree(testSite.getIdentifier());

        // Only parent-one MUST be open
        // parent-two MUST NOT be neither "open" nor "selected"
        final Map<String, Object> siteFolderTree = folderTree.get(0);
        final List<Map<String, Object>> parentFolders = (List<Map<String, Object>>) siteFolderTree.get("childrenFolders");
        assertEquals("There must be two parent Folders for the Test Site", 2, parentFolders.size());
        assertFalse("Folder '" + parentFolderOne.getPath() + "' must NOT be open", (boolean) parentFolders.get(0).get("open"));
        assertFalse("Folder '" + parentFolderOne.getPath() + "' must NOT be selected", (boolean) parentFolders.get(0).get("selected"));

        assertFalse("Folder '" + parentFolderTwo.getPath() + "' must NOT be open", (boolean) parentFolders.get(1).get("open"));
        assertFalse("Folder '" + parentFolderTwo.getPath() + "' must NOT be selected", (boolean) parentFolders.get(1).get("selected"));
    }

    /**
     * Sets up the DWR context using Mockito so that the {@link BrowserAjax} class can be tested correctly.
     */
    private static void setUpDwrContext(User user) throws DotDataException, DotSecurityException {
        session = mock(HttpSession.class);
        when(session.getAttribute(SessionMessages.KEY)).thenReturn(new LinkedHashMap());

        final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getSession()).thenReturn(session);

        when(httpServletRequest.getAttribute(WebKeys.USER)).thenReturn(user);

        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(httpServletRequest);

        final WebContextFactory.WebContextBuilder webContextBuilderMock =
                mock(WebContextFactory.WebContextBuilder.class);
        when(webContextBuilderMock.get()).thenReturn(webContext);

        final com.dotcms.repackage.org.directwebremoting.Container containerMock =
                mock(com.dotcms.repackage.org.directwebremoting.Container.class);
        when(containerMock.getBean(WebContextFactory.WebContextBuilder.class)).thenReturn(webContextBuilderMock);

        WebContextFactory.attach(containerMock);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#openFolderContent(String, String, boolean, long)}</li>
     *     <li><b>Given Scenario:</b> Opening a Folder to view their items</li>
     *     <li><b>Expected Result:</b> The items should be sorted by modDate descending by default</li>
     * </ul>
     * @throws Exception
     */
    @Test
    public void test_openFolderContent_defaultBehavior() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();

        final Folder mainFolder = new FolderDataGen().site(host).nextPersisted();

        final Link link = new LinkDataGen().parent(mainFolder).nextPersisted(false);
        ThreadUtils.sleep(1000);
        final Template templateDataGen = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(mainFolder, templateDataGen).nextPersisted();
        ThreadUtils.sleep(1000);
        final Folder subFolder = new FolderDataGen().parent(mainFolder).nextPersisted();


        final BrowserAjax browserAjax = new BrowserAjax();
        final List<Map<String, Object>> folderContent = browserAjax.openFolderContent(mainFolder.getIdentifier(),
                "",false,APILocator.getLanguageAPI().getDefaultLanguage().getId());
        //The order should be subfolder, page, link.
        assertTrue(folderContent.get(0).get("identifier").equals(subFolder.getIdentifier()));
        assertTrue(folderContent.get(1).get("identifier").equals(page.getIdentifier()));
        assertTrue(folderContent.get(2).get("identifier").equals(link.getIdentifier()));
    }

    //crete the test for the method public List<Map<String, Object>> getFolderSubfolders(final String parentFolderId)
    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getFolderSubfolders(String)}</li>
     *     <li><b>Given Scenario: The folder is sorted by Title not Name,
     *     subfolders in content search are not sorted alphabetically if the folder Name is changed</b> </li>
     *     <li><b>Expected Result:The subfolder list should be sorted alphabetically </b> </li>
     * </ul>
     * @throws Exception
     */
    @Test
    public void test_getFolderSubfolders() throws Exception {
        setUpDwrContext(TestUserUtils.getAdminUser());
        final User sysUser = APILocator.getUserAPI().getSystemUser();
        final Host host = new SiteDataGen().nextPersisted();
        final Folder mainFolder = new FolderDataGen().name("mainFolder"+System.currentTimeMillis()).site(host).nextPersisted();
        final Folder subFolderC = new FolderDataGen().name("c"+System.currentTimeMillis()).parent(mainFolder).nextPersisted();
        final Folder subFolderB = new FolderDataGen().name("b"+System.currentTimeMillis()).parent(mainFolder).nextPersisted();
        final Folder subFolderA = new FolderDataGen().name("a"+System.currentTimeMillis()).parent(mainFolder).nextPersisted();
        final BrowserAjax browserAjax = new BrowserAjax();
        //method to test
        List<Map<String, Object>> folderContent = browserAjax.getFolderSubfolders(mainFolder.getIdentifier());
        //assert all the subfolders are alphabetical sorted by name
        assertEquals(folderContent.get(0).get("name"),subFolderA.getName());
        //rename the subfolderA to z...
        APILocator.getFolderAPI().renameFolder(subFolderA, "z"+System.currentTimeMillis(), sysUser , false);
        //method to test
        folderContent = browserAjax.getFolderSubfolders(mainFolder.getIdentifier());
        //now to subFolderA should be the last one
        assertNotEquals(folderContent.get(0).get("name"), subFolderA.getName());
        assertEquals(folderContent.get(folderContent.size()-1).get("name"),subFolderA.getName());
    }


    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getHosts()}</li>
     *     <li><b>Given Scenario: Limited users should be able to create content types on System Host if given the proper permissions</b> </li>
     *     <li><b>Expected Result:The System host should appear in the list of host </b> </li>
     * </ul>
     * @throws Exception
     */
    @Test
    public void test_getHosts_ShoudlRetrieveSystemHosts() throws DotDataException, DotSecurityException, SystemException, PortalException {
        final BrowserAjax browserAjax = new BrowserAjax();
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

        //create the user
        final User user =TestUserUtils.getChrisPublisherUser();
        //assign permissions to system host
        final Permission permissions = new Permission(Host.class.getCanonicalName(),
                APILocator.systemHost().getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(user.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT, true);
        permissionAPI.save(permissions, APILocator.systemHost(), APILocator.getUserAPI().getSystemUser(), false);
        //set up the dwr context
        setUpDwrContext(user);
        //get the logged-in user
        WebContext ctx = WebContextFactory.get();
        UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
        User loggedInUser = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
        Host nsite = new SiteDataGen().nextPersisted();
        //method to test
        List<Map<String, Object>> hosts = browserAjax.getHosts();
        assertNotNull(hosts);
        assertTrue(hosts.size() > 0);
        //should contain the system host as a limited user
        assertTrue(hosts.stream().anyMatch(host -> host.get("identifier").equals(APILocator.systemHost().getIdentifier())) && loggedInUser.getFirstName().equals(user.getFirstName()) );
        setUpDwrContext(TestUserUtils.getAdminUser());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getFolderContentWithDotAssets(String, int, int, String,
     *     List, List, boolean, boolean, boolean, String, boolean, boolean, boolean)}</li>
     *     <li><b>Given Scenario:</b> The legacy "Select a file" / "Select link" dialog browses a folder that
     *     contains a Page, passing the synthetic {@code application/dotpage} MIME type. This is the exact call the
     *     redirect target picker makes from a Page's Properties.</li>
     *     <li><b>Expected Result:</b> The Page is listed and {@code total} is greater than zero -- instead of the
     *     {@code {total: 0, list: []}} the dialog reports today.</li>
     * </ul>
     * AC-001 and AC-003 of <a href="https://github.com/dotCMS/core/issues/36916">#36916</a>.
     */
    @Test
    public void test_getFolderContentWithDotAssets_dotPageMimeType_returnsPages() throws Exception {
        setUpDwrContext(TestUserUtils.getAdminUser());

        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();

        final BrowserAjax browserAjax = new BrowserAjax();
        final Map<String, Object> results = browserAjax.getFolderContentWithDotAssets(folder.getIdentifier(), 0,
                100, "", List.of(DOTPAGE_MIME_TYPE), List.of(), false, true, false, "moddate", true, true, false);

        assertNotNull("The dialog must always get a result map back", results);
        final List<Map<String, Object>> contentList = (List<Map<String, Object>>) results.get("list");
        assertNotNull("The dialog result must carry a list", contentList);
        assertTrue("The dialog must report at least one item for a folder that contains a Page, but reported: "
                + results.get("total"), ((Integer) results.get("total")) > 0);
        assertTrue("The Page must be listed for an '" + DOTPAGE_MIME_TYPE + "' browse",
                contentList.stream().anyMatch(item -> page.getIdentifier().equals(item.get("identifier"))));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#openFolderTree(String)}</li>
     *     <li><b>Given Scenario:</b> A user who can read a folder expands it in the Site Browser. Of its sub-folders,
     *     the user can read one, has no permission on another, and can only edit the third.</li>
     *     <li><b>Expected Result:</b> Only the sub-folder the user can read is returned.</li>
     * </ul>
     */
    @Test
    public void openFolderTree_returnsOnlyReadableSubfolders() throws Exception {
        setUpReadPermissionContext(limitedUser, Set.of());

        final List<Map> children = new BrowserAjax().openFolderTree(readableFolder.getIdentifier());

        assertEquals("openFolderTree", List.of(readableChild.getName()), names(children));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getTree(String)}</li>
     *     <li><b>Given Scenario:</b> A user who can read the Site, and only one of its two root folders, loads the
     *     Site's folder tree.</li>
     *     <li><b>Expected Result:</b> Only the root folder the user can read is returned.</li>
     * </ul>
     */
    @Test
    public void getTree_returnsOnlyReadableRootFolders() throws Exception {
        setUpReadPermissionContext(limitedUser, Set.of());

        final List<Map> siteTree = new BrowserAjax().getTree(readSite.getIdentifier());

        assertEquals("getTree sites", 1, siteTree.size());
        assertTrue("The Site must still list READ, which the Site Browser needs to show it",
                ((List<Integer>) siteTree.get(0).get("permissions")).contains(PermissionAPI.PERMISSION_READ));
        assertEquals("getTree root folders", List.of(readableFolder.getName()),
                names((List<Map>) siteTree.get(0).get("childrenFolders")));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getTree(String)}</li>
     *     <li><b>Given Scenario:</b> A user who can read only some Sites loads the tree of all Sites while the Site
     *     cache is already loaded.</li>
     *     <li><b>Expected Result:</b> Only the Sites the user can read are returned.</li>
     * </ul>
     */
    @Test
    public void getTree_allSitesReturnsOnlyReadableSites() throws Exception {
        // Back-office rule, as the tree applies it: front-end role grants (e.g. CMS Anonymous) do not count
        assertFalse("Limited user must not read " + testSite.getHostname(), APILocator.getPermissionAPI()
                .doesUserHavePermission(testSite, PermissionAPI.PERMISSION_READ, limitedUser, false));
        APILocator.getHostAPI().findAllFromCache(APILocator.systemUser(), false);
        setUpReadPermissionContext(limitedUser, Set.of());

        final List<Map> siteTree = new BrowserAjax().getTree("allHosts");

        final List<Object> siteIds = siteTree.stream().map(site -> site.get("identifier"))
                .collect(Collectors.toList());
        assertTrue("getTree(allHosts) must return the readable Site", siteIds.contains(readSite.getIdentifier()));
        assertFalse("getTree(allHosts) must not return a Site the user cannot read",
                siteIds.contains(testSite.getIdentifier()));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#openHostTree(String)}</li>
     *     <li><b>Given Scenario:</b> A user who can read the Site, and only one of its two root folders, expands the
     *     Site in the Site Browser.</li>
     *     <li><b>Expected Result:</b> Only the root folder the user can read is returned.</li>
     * </ul>
     */
    @Test
    public void openHostTree_returnsOnlyReadableRootFolders() throws Exception {
        setUpReadPermissionContext(limitedUser, Set.of());

        final List<Map> rootFolders = new BrowserAjax().openHostTree(readSite.getIdentifier());

        assertEquals("openHostTree", List.of(readableFolder.getName()), names(rootFolders));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getTree(String)}</li>
     *     <li><b>Given Scenario:</b> The session marks as open a folder the user can read, a sub-folder the user cannot
     *     read, and that sub-folder's own child.</li>
     *     <li><b>Expected Result:</b> The readable folder is expanded with its readable sub-folder only; the folders
     *     the user cannot read are not returned.</li>
     * </ul>
     */
    @Test
    public void getTree_expandsOnlyReadableOpenFolders() throws Exception {
        setUpReadPermissionContext(limitedUser, Set.of(readableFolder.getIdentifier(),
                restrictedChild.getIdentifier(), restrictedGrandchild.getIdentifier()));

        final List<Map> siteTree = new BrowserAjax().getTree(readSite.getIdentifier());

        final List<Map> rootFolders = (List<Map>) siteTree.get(0).get("childrenFolders");
        assertEquals("getTree root folders", List.of(readableFolder.getName()), names(rootFolders));
        final Map readable = folderNamed(rootFolders, readableFolder.getName());
        assertTrue("open folder must be expanded", (boolean) readable.get("open"));
        assertEquals("getTree open folder children", List.of(readableChild.getName()),
                names((List<Map>) readable.get("childrenFolders")));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getTreeMinInfoByHostId(String)}</li>
     *     <li><b>Given Scenario:</b> A user loads the compact folder tree of a Site where some nested folders are not
     *     readable by the user.</li>
     *     <li><b>Expected Result:</b> Every level of the tree holds only the folders the user can read.</li>
     * </ul>
     */
    @Test
    public void getTreeMinInfoByHostId_returnsOnlyReadableNestedFolders() throws Exception {
        setUpReadPermissionContext(limitedUser, Set.of());

        final List<Map<String, Object>> tree = new BrowserAjax().getTreeMinInfoByHostId(readSite.getIdentifier());

        final List<Map> rootFolders = (List<Map>) tree.get(0).get("children");
        assertEquals("min-info root folders", List.of(readableFolder.getName()), names(rootFolders));
        final List<Map> children = (List<Map>) folderNamed(rootFolders, readableFolder.getName()).get("children");
        assertEquals("min-info second level", List.of(readableChild.getName()), names(children));
        assertEquals("min-info third level", List.of(readableGrandchild.getName()),
                names((List<Map>) folderNamed(children, readableChild.getName()).get("children")));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getTreeMinInfo()}</li>
     *     <li><b>Given Scenario:</b> A user who can read only some Sites and folders loads the compact tree of all
     *     Sites.</li>
     *     <li><b>Expected Result:</b> Only the Sites and folders the user can read are returned, at every level.</li>
     * </ul>
     */
    @Test
    public void getTreeMinInfo_allSitesReturnsOnlyReadableFolders() throws Exception {
        setUpReadPermissionContext(limitedUser, Set.of());

        final List<Map<String, Object>> tree = new BrowserAjax().getTreeMinInfo();

        final List<Object> siteIds = tree.stream().map(site -> site.get("id")).collect(Collectors.toList());
        assertFalse("min-info all Sites must not return a Site the user cannot read",
                siteIds.contains(testSite.getIdentifier()));
        final Map<String, Object> site = tree.stream().filter(entry -> readSite.getIdentifier().equals(entry.get("id")))
                .findFirst().orElseThrow(() -> new AssertionError("readable Site not returned"));
        final List<Map> rootFolders = (List<Map>) site.get("children");
        assertEquals("min-info root folders", List.of(readableFolder.getName()), names(rootFolders));
        final List<Map> children = (List<Map>) folderNamed(rootFolders, readableFolder.getName()).get("children");
        assertEquals("min-info second level", List.of(readableChild.getName()), names(children));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#openFolderTree(String)}</li>
     *     <li><b>Given Scenario:</b> A user who can read and edit a sub-folder expands its parent.</li>
     *     <li><b>Expected Result:</b> The returned sub-folder still carries every permission the user holds on it,
     *     which the Site Browser uses to enable its actions.</li>
     * </ul>
     */
    @Test
    public void openFolderTree_keepsPermissionIdsOfReturnedFolders() throws Exception {
        setUpReadPermissionContext(limitedUser, Set.of());

        final List<Map> children = new BrowserAjax().openFolderTree(readableFolder.getIdentifier());

        final List<Integer> permissions =
                (List<Integer>) folderNamed(children, readableChild.getName()).get("permissions");
        assertTrue("READ must be listed", permissions.contains(PermissionAPI.PERMISSION_READ));
        assertTrue("WRITE must be listed", permissions.contains(PermissionAPI.PERMISSION_WRITE));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#openFolderTree(String)}</li>
     *     <li><b>Given Scenario:</b> A user who can read every sub-folder expands their parent, and loading the
     *     user's permissions on one of the sub-folders fails.</li>
     *     <li><b>Expected Result:</b> That sub-folder is left out; the others are still returned.</li>
     * </ul>
     */
    @Test
    public void openFolderTree_leavesOutFolderWhosePermissionsCannotBeLoaded() throws Exception {
        setUpReadPermissionContext(fullReadUser, Set.of());
        final PermissionAPI permissionAPI = spy(APILocator.getPermissionAPI());
        doThrow(new DotDataException("Permissions not available")).when(permissionAPI)
                .getPermissionIdsFromRoles(argThat(isPermissionableOf(readableChild.getIdentifier())),
                        any(Role[].class), any(User.class));

        final List<Map> children;
        try (MockedStatic<APILocator> apis = mockStatic(APILocator.class, CALLS_REAL_METHODS)) {
            apis.when(APILocator::getPermissionAPI).thenReturn(permissionAPI);
            children = new BrowserAjax().openFolderTree(readableFolder.getIdentifier());
        }

        verify(permissionAPI).getPermissionIdsFromRoles(argThat(isPermissionableOf(readableChild.getIdentifier())),
                any(Role[].class), any(User.class));
        assertEquals("openFolderTree", List.of(restrictedChild.getName(), writeOnlyChild.getName()), names(children));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getTree(String)}</li>
     *     <li><b>Given Scenario:</b> A user who can read a Site loads its tree, and loading the user's permissions on
     *     the Site fails.</li>
     *     <li><b>Expected Result:</b> The Site is left out.</li>
     * </ul>
     */
    @Test
    public void getTree_leavesOutSiteWhosePermissionsCannotBeLoaded() throws Exception {
        setUpReadPermissionContext(fullReadUser, Set.of());
        final PermissionAPI permissionAPI = spy(APILocator.getPermissionAPI());
        doThrow(new DotDataException("Permissions not available")).when(permissionAPI)
                .getPermissionIdsFromRoles(argThat(isPermissionableOf(readSite.getIdentifier())),
                        any(Role[].class), any(User.class));

        final List<Map> siteTree;
        try (MockedStatic<APILocator> apis = mockStatic(APILocator.class, CALLS_REAL_METHODS)) {
            apis.when(APILocator::getPermissionAPI).thenReturn(permissionAPI);
            siteTree = new BrowserAjax().getTree(readSite.getIdentifier());
        }

        verify(permissionAPI).getPermissionIdsFromRoles(argThat(isPermissionableOf(readSite.getIdentifier())),
                any(Role[].class), any(User.class));
        assertTrue("getTree must leave out the Site", siteTree.isEmpty());
    }

    /**
     * Matches the Site or folder with the given identifier.
     *
     * @param identifier The identifier of the Site or folder.
     *
     * @return The argument matcher.
     */
    private static ArgumentMatcher<Permissionable> isPermissionableOf(final String identifier) {
        return permissionable -> permissionable != null && (identifier.equals(permissionable.getPermissionId())
                || (permissionable instanceof Folder && identifier.equals(((Folder) permissionable).getIdentifier()))
                || (permissionable instanceof Host && identifier.equals(((Host) permissionable).getIdentifier())));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#openFolderTree(String)}</li>
     *     <li><b>Given Scenario:</b> A user without the CMS Administrator role who can read every folder of the Site
     *     expands a folder.</li>
     *     <li><b>Expected Result:</b> Every sub-folder is returned.</li>
     * </ul>
     */
    @Test
    public void openFolderTree_returnsAllSubfoldersForUserWithReadOnAll() throws Exception {
        setUpReadPermissionContext(fullReadUser, Set.of());

        final List<Map> children = new BrowserAjax().openFolderTree(readableFolder.getIdentifier());

        assertEquals("openFolderTree",
                List.of(readableChild.getName(), restrictedChild.getName(), writeOnlyChild.getName()),
                names(children));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getTree(String)}</li>
     *     <li><b>Given Scenario:</b> A user without the CMS Administrator role who can read every folder of the Site
     *     loads the Site's folder tree.</li>
     *     <li><b>Expected Result:</b> Every root folder is returned.</li>
     * </ul>
     */
    @Test
    public void getTree_returnsAllRootFoldersForUserWithReadOnAll() throws Exception {
        setUpReadPermissionContext(fullReadUser, Set.of());

        final List<Map> siteTree = new BrowserAjax().getTree(readSite.getIdentifier());

        assertEquals("getTree root folders", List.of(readableFolder.getName(), restrictedRoot.getName()),
                names((List<Map>) siteTree.get(0).get("childrenFolders")));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getTreeMinInfoByHostId(String)}</li>
     *     <li><b>Given Scenario:</b> A user without the CMS Administrator role who can read every folder of the Site
     *     loads the compact folder tree.</li>
     *     <li><b>Expected Result:</b> Every folder is returned, at every level.</li>
     * </ul>
     */
    @Test
    public void getTreeMinInfoByHostId_returnsAllFoldersForUserWithReadOnAll() throws Exception {
        setUpReadPermissionContext(fullReadUser, Set.of());

        final List<Map<String, Object>> tree = new BrowserAjax().getTreeMinInfoByHostId(readSite.getIdentifier());

        final List<Map> rootFolders = (List<Map>) tree.get(0).get("children");
        assertEquals("min-info root folders", List.of(readableFolder.getName(), restrictedRoot.getName()),
                names(rootFolders));
        final List<Map> children = (List<Map>) folderNamed(rootFolders, readableFolder.getName()).get("children");
        assertEquals("min-info second level",
                List.of(readableChild.getName(), restrictedChild.getName(), writeOnlyChild.getName()),
                names(children));
        assertEquals("min-info third level", List.of(restrictedGrandchild.getName()),
                names((List<Map>) folderNamed(children, restrictedChild.getName()).get("children")));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> visibility of {@code BrowserAjax#refreshIndex(Folder, Host, Folder)}</li>
     *     <li><b>Given Scenario:</b> The class is exposed through DWR, which publishes every public method.</li>
     *     <li><b>Expected Result:</b> The index refresh helper is internal to the class.</li>
     * </ul>
     */
    @Test
    public void refreshIndex_isNotPublic() throws Exception {
        final int modifiers = BrowserAjax.class
                .getDeclaredMethod("refreshIndex", Folder.class, Host.class, Folder.class).getModifiers();

        assertFalse("refreshIndex must not be public", Modifier.isPublic(modifiers));
    }

    /**
     * Restores the admin DWR context after every test, so a test that switches users does not affect the next one.
     */
    @After
    public void restoreAdminContext() throws Exception {
        setUpDwrContext(TestUserUtils.getAdminUser());
    }

    /**
     * Sets the DWR context to the given user and stubs the Site Browser session attributes: no active folder, and
     * the given folders marked as open.
     *
     * @param user          The user the DWR calls run as.
     * @param openFolderIds The identifiers of the folders the session marks as open.
     */
    private static void setUpReadPermissionContext(final User user, final Set<String> openFolderIds)
            throws DotDataException, DotSecurityException {
        setUpDwrContext(user);
        when(session.getAttribute(BrowserTreeHelper.ACTIVE_FOLDER_ID)).thenReturn(null);
        when(session.getAttribute(BrowserTreeHelper.OPEN_FOLDER_IDS)).thenReturn(new HashSet<>(openFolderIds));
    }

    /**
     * Returns the {@code name} of each folder map, sorted, so assertions don't depend on the order of the lookup.
     *
     * @param folders The folder maps returned by a tree method.
     *
     * @return The sorted folder names.
     */
    private static List<String> names(final List<? extends Map> folders) {
        return folders.stream().map(folder -> (String) folder.get("name")).sorted().collect(Collectors.toList());
    }

    /**
     * Returns the folder map with the given name, failing the test if it is missing.
     *
     * @param folders The folder maps returned by a tree method.
     * @param name    The folder name to look for.
     *
     * @return The matching folder map.
     */
    private static Map folderNamed(final List<? extends Map> folders, final String name) {
        return folders.stream().filter(folder -> name.equals(folder.get("name"))).findFirst()
                .orElseThrow(() -> new AssertionError("Folder '" + name + "' not returned"));
    }
}
