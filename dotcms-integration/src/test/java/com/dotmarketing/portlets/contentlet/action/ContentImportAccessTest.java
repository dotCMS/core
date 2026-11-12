package com.dotmarketing.portlets.contentlet.action;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link ContentImportAccess}: a CSV import may be started only by a user who
 * is allowed to create content of the chosen Content Type.
 *
 * <p>Three ways in, because the import has three ways to succeed. A row that creates content needs
 * WRITE on the Content Type. A row that updates existing content is authorized against the
 * contentlet, which inherits from its folder and then its site, and
 * {@code ImportUtil.setSiteAndFolder} separately requires add-children, asking the folder before
 * the site. So add-children on the site is one way and add-children on a folder of it is another.
 * A user holding any of the three can import something today and must keep being able to.</p>
 *
 * <p>Deliberately not keyed to a portlet id: custom content portlets exist, so that would lock
 * out legitimate users.</p>
 *
 * <p><b>What these tests do not cover.</b> They exercise the decision, not the wiring: nothing
 * here drives {@link ImportContentletsAction#processAction}, so deleting the two
 * {@code _checkImportAccess} call sites would leave every test below passing.
 * {@link ImportContentletsActionTest} is the other half — it drives the action on the
 * {@code publish} command and covers the gate being asked at all, the Content Type coming from
 * the session form rather than the request form, and the refusal message being encoded. What
 * neither class reaches is the ordering fix on {@code preview}, that the upload is copied only at
 * the two {@code _generatePreview} call sites and below every rejection path: that one needs a
 * real multipart request and is left to manual QA.</p>
 */
public class ContentImportAccessTest {

    private static PermissionAPI permissionAPI;
    private static Host site;
    private static ContentType contentType;
    private static User importer;
    private static User siteOnlyUser;
    private static User folderOnlyUser;
    private static User userWithoutAccess;
    private static User adminUser;
    private static final List<User> createdUsers = new ArrayList<>();

    /**
     * Creates a Content Type and a site, a back-end user granted WRITE on the type only, a
     * back-end user granted add-children on the site only, and a back-end user with no permission
     * at all.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        permissionAPI = APILocator.getPermissionAPI();

        adminUser = TestUserUtils.getAdminUser();
        site = new SiteDataGen().nextPersisted();
        contentType = new ContentTypeDataGen()
                .baseContentType(BaseContentType.CONTENT)
                .host(site)
                .nextPersisted();

        importer = newBackendUser();
        siteOnlyUser = newBackendUser();
        folderOnlyUser = newBackendUser();
        userWithoutAccess = newBackendUser();
        grantWrite(contentType, APILocator.getRoleAPI().getUserRole(importer).getId());
        // READ as well as add-children: that is what a site-level grant looks like in the
        // permissions UI, and without READ the user cannot see the site to import into it.
        permissionAPI.save(new Permission(site.getPermissionId(),
                        APILocator.getRoleAPI().getUserRole(siteOnlyUser).getId(),
                        PermissionAPI.PERMISSION_READ
                                | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, true),
                site, APILocator.systemUser(), false);

        // A section delegated the ordinary way: the user can see the site and add content inside
        // one folder of it, and holds nothing on the site itself or on the Content Type.
        final Folder delegatedFolder = new FolderDataGen().site(site).nextPersisted();
        permissionAPI.save(new Permission(site.getPermissionId(),
                        APILocator.getRoleAPI().getUserRole(folderOnlyUser).getId(),
                        PermissionAPI.PERMISSION_READ, true),
                site, APILocator.systemUser(), false);
        grantAddChildren(delegatedFolder,
                APILocator.getRoleAPI().getUserRole(folderOnlyUser).getId());

        assertTrue("The fixture needs a user who can create content of this type",
                permissionAPI.doesUserHavePermission(contentType, PermissionAPI.PERMISSION_WRITE,
                        importer, false));
        assertFalse("The fixture needs a user who cannot create content of this type",
                permissionAPI.doesUserHavePermission(contentType, PermissionAPI.PERMISSION_WRITE,
                        userWithoutAccess, false));
        assertFalse("The site-only user must not have WRITE on the type, or the test proves "
                        + "nothing",
                permissionAPI.doesUserHavePermission(contentType, PermissionAPI.PERMISSION_WRITE,
                        siteOnlyUser, false));
        assertTrue("The fixture needs a user who can add content to the site",
                permissionAPI.doesUserHavePermission(site,
                        PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, siteOnlyUser, false));
        assertFalse("The folder-only user must hold nothing on the type",
                permissionAPI.doesUserHavePermission(contentType, PermissionAPI.PERMISSION_WRITE,
                        folderOnlyUser, false));
        assertFalse("The folder-only user must hold nothing on the site itself, or the test "
                        + "proves nothing",
                permissionAPI.doesUserHavePermission(site,
                        PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, folderOnlyUser, false));
        assertTrue("The folder-only user needs add-children on the delegated folder",
                permissionAPI.doesUserHavePermission(delegatedFolder,
                        PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, folderOnlyUser, false));
    }

    /**
     * Removes the Content Type, the users and the site created for these tests.
     */
    @AfterClass
    public static void cleanup() {
        createdUsers.forEach(user -> UserDataGen.remove(user, true));
        if (contentType != null) {
            try {
                ContentTypeDataGen.remove(contentType);
            } catch (final Exception e) {
                Logger.warn(ContentImportAccessTest.class,
                        "Could not remove test Content Type: " + e.getMessage());
            }
        }
        if (site != null) {
            try {
                APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
            } catch (final Exception e) {
                Logger.warn(ContentImportAccessTest.class,
                        "Could not remove test site: " + e.getMessage());
            }
        }
    }

    /**
     * Given a back-end user with no permission on either the Content Type or the site,
     * When an import is checked,
     * Then the user is refused.
     */
    @Test
    public void canImportInto_userWithNeitherPermission_isRefused() {
        assertFalse(ContentImportAccess.canImportInto(contentType, site, userWithoutAccess));
    }

    /**
     * Given a back-end user granted WRITE on the Content Type,
     * When an import into that type is checked,
     * Then the user is allowed.
     */
    @Test
    public void canImportInto_userWithWriteOnType_isAllowed() {
        assertTrue(ContentImportAccess.canImportInto(contentType, site, importer));
    }

    /**
     * Given a back-end user with add-children on the site but nothing on the Content Type,
     * When an import is checked,
     * Then the user is allowed.
     *
     * <p>This is the case an update-only CSV import relies on: a row that updates existing
     * content is authorized against the contentlet, which inherits from its folder and site, not
     * from the type. Refusing this user would take away an import that works today.</p>
     */
    @Test
    public void canImportInto_userWithAddChildrenOnSiteOnly_isAllowed() {
        assertTrue(ContentImportAccess.canImportInto(contentType, site, siteOnlyUser));
    }

    /**
     * Given a back-end user whose only permission is add-children on one folder of the site,
     * When an import is checked,
     * Then the user is allowed.
     *
     * <p>This is the delegation pattern the gate used to miss. A row that updates existing content
     * is authorized against the contentlet, which inherits from its folder, and
     * {@code ImportUtil.setSiteAndFolder} asks the folder before it asks the site. A user holding
     * only a folder imports updates today and must keep being able to.</p>
     */
    @Test
    public void canImportInto_userWithAddChildrenOnFolderOnly_isAllowed() {
        assertTrue(ContentImportAccess.canImportInto(contentType, site, folderOnlyUser));
    }

    /**
     * Given a back-end user whose only permission is add-children on the site,
     * When an import is checked with no site selected, as happens on "All Sites",
     * Then the user is still allowed: the question becomes whether they may add content to any
     * site they can reach, and they may.
     */
    @Test
    public void canImportInto_siteOnlyUser_withoutSelectedSite_isAllowed() {
        assertTrue(ContentImportAccess.canImportInto(contentType, null, siteOnlyUser));
    }

    /**
     * Given a back-end user with no permission anywhere,
     * When an import is checked with no site selected,
     * Then the user is refused: there is nothing to authorize them against.
     */
    @Test
    public void canImportInto_userWithNeitherPermission_withoutSelectedSite_isRefused() {
        assertFalse(ContentImportAccess.canImportInto(contentType, null, userWithoutAccess));
    }

    /**
     * Given a CMS administrator,
     * When an import into any type is checked,
     * Then the administrator is allowed.
     */
    @Test
    public void canImportInto_administrator_isAllowed() {
        assertTrue(ContentImportAccess.canImportInto(contentType, site, adminUser));
    }

    /**
     * Given a null user, or a user with nothing and neither a Content Type nor a site,
     * When an import is checked,
     * Then it is refused rather than throwing.
     */
    @Test
    public void canImportInto_nullArguments_areRefused() {
        assertFalse(ContentImportAccess.canImportInto(contentType, site, null));
        assertFalse(ContentImportAccess.canImportInto(null, null, userWithoutAccess));
    }

    /**
     * Grants WRITE on the given Content Type to the given role.
     *
     * @param type   the {@link ContentType} to grant on
     * @param roleId the id of the role receiving the grant
     */
    private static void grantWrite(final ContentType type, final String roleId) throws Exception {
        permissionAPI.save(new Permission(type.getPermissionId(), roleId,
                PermissionAPI.PERMISSION_WRITE, true), type, APILocator.systemUser(), false);
    }

    /**
     * Grants add-children on the given site or folder to the given role.
     *
     * @param target the {@link Permissionable} to grant on
     * @param roleId the id of the role receiving the grant
     */
    private static void grantAddChildren(final Permissionable target, final String roleId)
            throws Exception {
        permissionAPI.save(new Permission(target.getPermissionId(), roleId,
                PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, true), target,
                APILocator.systemUser(), false);
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
