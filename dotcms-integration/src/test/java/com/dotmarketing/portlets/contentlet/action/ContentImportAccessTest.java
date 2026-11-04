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
 * <p>The bar is WRITE on the Content Type because that is what
 * {@code ContentletAPI.checkin} already requires of a brand-new contentlet. Pre-checking the same
 * bar cannot refuse anyone who would have been able to finish the import anyway, and it does not
 * depend on which portlet the user reached the screen from — custom content portlets exist, so a
 * portlet-id check would lock out legitimate users.</p>
 */
public class ContentImportAccessTest {

    private static PermissionAPI permissionAPI;
    private static Host site;
    private static ContentType contentType;
    private static User importer;
    private static User userWithoutAccess;
    private static User adminUser;
    private static final List<User> createdUsers = new ArrayList<>();

    /**
     * Creates a Content Type, a back-end user granted WRITE on it, and a back-end user with no
     * permission at all.
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
        userWithoutAccess = newBackendUser();
        grantWrite(contentType, APILocator.getRoleAPI().getUserRole(importer).getId());

        assertTrue("The fixture needs a user who can create content of this type",
                permissionAPI.doesUserHavePermission(contentType, PermissionAPI.PERMISSION_WRITE,
                        importer, false));
        assertFalse("The fixture needs a user who cannot create content of this type",
                permissionAPI.doesUserHavePermission(contentType, PermissionAPI.PERMISSION_WRITE,
                        userWithoutAccess, false));
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
     * Given a back-end user with no permission on the Content Type,
     * When an import into that type is checked,
     * Then the user is refused.
     */
    @Test
    public void canImportInto_userWithoutWrite_isRefused() {
        assertFalse(ContentImportAccess.canImportInto(contentType, userWithoutAccess));
    }

    /**
     * Given a back-end user granted WRITE on the Content Type,
     * When an import into that type is checked,
     * Then the user is allowed.
     */
    @Test
    public void canImportInto_userWithWrite_isAllowed() {
        assertTrue(ContentImportAccess.canImportInto(contentType, importer));
    }

    /**
     * Given a CMS administrator,
     * When an import into any type is checked,
     * Then the administrator is allowed.
     */
    @Test
    public void canImportInto_administrator_isAllowed() {
        assertTrue(ContentImportAccess.canImportInto(contentType, adminUser));
    }

    /**
     * Given a null Content Type or a null user,
     * When an import is checked,
     * Then it is refused rather than throwing.
     */
    @Test
    public void canImportInto_nullArguments_areRefused() {
        assertFalse(ContentImportAccess.canImportInto(null, importer));
        assertFalse(ContentImportAccess.canImportInto(contentType, null));
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
