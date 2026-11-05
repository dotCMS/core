package com.dotmarketing.portlets.contentlet.action;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
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
 * <p>Two ways in, because the import has two ways to succeed. A row that creates content needs
 * WRITE on the Content Type; a row that updates existing content needs add-children on the site,
 * which is what {@code ImportUtil.setSiteAndFolder} enforces per row. A user holding either one
 * can import something today and must keep being able to.</p>
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
        userWithoutAccess = newBackendUser();
        grantWrite(contentType, APILocator.getRoleAPI().getUserRole(importer).getId());
        grantAddChildren(site, APILocator.getRoleAPI().getUserRole(siteOnlyUser).getId());

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
     * Given a back-end user whose only permission is add-children on the site,
     * When an import is checked with no site resolved,
     * Then the user is refused: there is nothing left to authorize them against.
     */
    @Test
    public void canImportInto_siteOnlyUser_withoutSite_isRefused() {
        assertFalse(ContentImportAccess.canImportInto(contentType, null, siteOnlyUser));
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
     * Given a null user, or neither a Content Type nor a site,
     * When an import is checked,
     * Then it is refused rather than throwing.
     */
    @Test
    public void canImportInto_nullArguments_areRefused() {
        assertFalse(ContentImportAccess.canImportInto(contentType, site, null));
        assertFalse(ContentImportAccess.canImportInto(null, null, importer));
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
     * Grants add-children on the given site to the given role.
     *
     * @param host   the {@link Host} to grant on
     * @param roleId the id of the role receiving the grant
     */
    private static void grantAddChildren(final Host host, final String roleId) throws Exception {
        permissionAPI.save(new Permission(host.getPermissionId(), roleId,
                PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, true), host, APILocator.systemUser(),
                false);
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
