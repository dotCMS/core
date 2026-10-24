package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Covers the destination check that {@link ESContentletAPIImpl} applies when a new contentlet is
 * checked in: besides WRITE on the content type, the user needs "Can Add Children" on the folder
 * or site the contentlet is placed in. Existing contentlets, content placed on the System Host,
 * and the kill switch {@link FeatureFlagName#FEATURE_FLAG_CHECKIN_DESTINATION_PERMISSION_CHECK}
 * are exercised as well.
 */
public class ContentletCheckinDestinationPermissionTest {

    private static final int ADD_CHILDREN_ON_SITE = PermissionAPI.PERMISSION_READ
            | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;
    private static final int TYPE_WRITE = PermissionAPI.PERMISSION_READ
            | PermissionAPI.PERMISSION_WRITE;

    private static ContentletAPI contentletAPI;
    private static PermissionAPI permissionAPI;
    private static Host grantedSite;
    private static Host writeOnlySite;
    private static Host otherSite;
    private static Folder grantedFolder;
    private static Folder otherFolder;
    private static ContentType contentType;
    private static Field titleField;
    private static Role limitedRole;
    private static User limitedUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        contentletAPI = APILocator.getContentletAPI();
        permissionAPI = APILocator.getPermissionAPI();

        grantedSite = new SiteDataGen().nextPersisted();
        writeOnlySite = new SiteDataGen().nextPersisted();
        otherSite = new SiteDataGen().nextPersisted();
        grantedFolder = new FolderDataGen().site(grantedSite).nextPersisted();
        otherFolder = new FolderDataGen().site(otherSite).nextPersisted();

        titleField = new FieldDataGen().type(TextField.class).name("title")
                .velocityVarName("title").next();
        final Field hostField = new FieldDataGen().type(HostFolderField.class)
                .name("Site or Folder").velocityVarName("hostfolder").next();
        contentType = new ContentTypeDataGen().host(APILocator.systemHost())
                .field(titleField).field(hostField).nextPersisted();

        limitedRole = new RoleDataGen().nextPersisted();
        limitedUser = new UserDataGen()
                .roles(limitedRole, APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();

        // WRITE on the type everywhere; Add Children on the first site and its folder, WRITE
        // alone on the second site, READ alone on the third
        grant(contentType, limitedRole, TYPE_WRITE);
        grant(grantedSite, limitedRole, ADD_CHILDREN_ON_SITE);
        grant(grantedFolder, limitedRole, ADD_CHILDREN_ON_SITE);
        grant(writeOnlySite, limitedRole, TYPE_WRITE);
        grant(otherSite, limitedRole, PermissionAPI.PERMISSION_READ);
        grant(otherFolder, limitedRole, PermissionAPI.PERMISSION_READ);
    }

    @AfterClass
    public static void cleanUp() {
        Config.setProperty(FeatureFlagName.FEATURE_FLAG_CHECKIN_DESTINATION_PERMISSION_CHECK, true);
        if (limitedUser != null) {
            UserDataGen.remove(limitedUser, true);
        }
        if (limitedRole != null) {
            RoleDataGen.remove(limitedRole, true);
        }
        if (contentType != null) {
            ContentTypeDataGen.remove(contentType, true);
        }
        if (grantedFolder != null) {
            FolderDataGen.remove(grantedFolder);
        }
        if (otherFolder != null) {
            FolderDataGen.remove(otherFolder);
        }
        removeSite(grantedSite);
        removeSite(writeOnlySite);
        removeSite(otherSite);
    }

    /**
     * Archives and deletes a test site, ignoring failures so cleanup never fails the suite.
     */
    private static void removeSite(final Host site) {
        if (site == null) {
            return;
        }
        Try.run(() -> {
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
        });
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}
     * Given Scenario: The user has WRITE on the type and Add Children on the site the new
     *                 contentlet is placed in.
     * ExpectedResult: The contentlet is created on that site.
     */
    @Test
    public void checkinOnSiteWithAddChildren_shouldSucceed() throws Exception {
        final Contentlet saved = checkinAs(limitedUser, false, grantedSite, null);

        assertTrue(UtilMethods.isSet(saved.getIdentifier()));
        assertEquals(grantedSite.getIdentifier(), saved.getHost());
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}
     * Given Scenario: The user has WRITE on the type and WRITE, but not Add Children, on the site
     *                 the new contentlet is placed in.
     * ExpectedResult: The contentlet is created; WRITE on the destination is enough.
     */
    @Test
    public void checkinOnSiteWithWriteOnly_shouldSucceed() throws Exception {
        final Contentlet saved = checkinAs(limitedUser, false, writeOnlySite, null);

        assertEquals(writeOnlySite.getIdentifier(), saved.getHost());
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}
     * Given Scenario: The user has WRITE on the type but neither Add Children nor WRITE on the
     *                 site the new contentlet is placed in.
     * ExpectedResult: A {@link DotSecurityException}, and nothing is created on that site.
     */
    @Test
    public void checkinOnSiteWithoutAddChildren_shouldBeDenied() {
        assertDenied(() -> checkinAs(limitedUser, false, otherSite, null));
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}
     * Given Scenario: The new contentlet is placed in a folder on which the user has no Add
     *                 Children, on a site where the user has none either.
     * ExpectedResult: A {@link DotSecurityException}.
     */
    @Test
    public void checkinInFolderWithoutAddChildren_shouldBeDenied() {
        assertDenied(() -> checkinAs(limitedUser, false, otherSite, otherFolder));
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}
     * Given Scenario: The new contentlet is placed in a folder on which the user has Add Children.
     * ExpectedResult: The contentlet is created in that folder.
     */
    @Test
    public void checkinInFolderWithAddChildren_shouldSucceed() throws Exception {
        final Contentlet saved = checkinAs(limitedUser, false, grantedSite, grantedFolder);

        assertEquals(grantedFolder.getInode(), saved.getFolder());
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}
     * Given Scenario: The new contentlet is not placed on any site (System Host) and the user only
     *                 has WRITE on the type.
     * ExpectedResult: The contentlet is created; the destination check does not apply.
     */
    @Test
    public void checkinOnSystemHostWithTypeWriteOnly_shouldSucceed() throws Exception {
        final Contentlet saved = checkinAs(limitedUser, false, APILocator.systemHost(), null);

        assertEquals(Host.SYSTEM_HOST, saved.getHost());
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}
     * Given Scenario: The type lives on the System Host and the new contentlet is placed on the
     *                 default site by a user who only has WRITE on the type.
     * ExpectedResult: The contentlet is created; the default site is the type's default
     *                 placement, so no Add Children is needed there.
     */
    @Test
    public void checkinOnDefaultSiteWithTypeWriteOnly_shouldSucceed() throws Exception {
        final Host defaultSite = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(),
                false);

        final Contentlet saved = checkinAs(limitedUser, false, defaultSite, null);

        assertEquals(defaultSite.getIdentifier(), saved.getHost());
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}
     * Given Scenario: A type that lives on the site without Add Children; the user only has WRITE
     *                 on that type and places content on that same site, then inside one of its
     *                 folders.
     * ExpectedResult: The site itself is the type's default placement and succeeds; the folder is
     *                 a chosen destination and is denied.
     */
    @Test
    public void checkinOnTheTypesOwnSite_shouldNotRequireAddChildren() throws Exception {
        final ContentType siteType = new ContentTypeDataGen().host(otherSite)
                .field(new FieldDataGen().type(TextField.class).name("title")
                        .velocityVarName("title").next())
                .nextPersisted();
        try {
            grant(siteType, limitedRole, TYPE_WRITE);

            final Contentlet saved = contentletAPI.checkin(new ContentletDataGen(siteType)
                    .host(otherSite).setProperty("title", "on own site").next(), limitedUser,
                    false);
            assertEquals(otherSite.getIdentifier(), saved.getHost());

            assertDenied(() -> contentletAPI.checkin(new ContentletDataGen(siteType)
                    .host(otherSite).folder(otherFolder).setProperty("title", "in folder").next(),
                    limitedUser, false));
        } finally {
            ContentTypeDataGen.remove(siteType, true);
        }
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}
     * Given Scenario: An existing contentlet on the site without Add Children is edited by a user
     *                 who has WRITE on that contentlet.
     * ExpectedResult: The edit succeeds; the destination check only applies to new contentlets.
     */
    @Test
    public void editExistingContentletOnSite_shouldNotRequireAddChildren() throws Exception {
        final Contentlet existing = checkinAs(APILocator.systemUser(), false, otherSite, null);
        grant(existing, limitedRole, TYPE_WRITE);

        contentletAPI.unlock(existing, APILocator.systemUser(), false);
        final Contentlet checkout = contentletAPI.checkout(existing.getInode(), limitedUser, false);
        checkout.setStringProperty(titleField.variable(), "edited");
        final Contentlet edited = contentletAPI.checkin(checkout, limitedUser, false);

        assertEquals("edited", edited.getStringProperty(titleField.variable()));
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}
     * Given Scenario: {@link FeatureFlagName#FEATURE_FLAG_CHECKIN_DESTINATION_PERMISSION_CHECK} is
     *                 off and the user has WRITE on the type but no Add Children on the site.
     * ExpectedResult: The contentlet is created, as before the destination check existed.
     */
    @Test
    public void checkinWithFlagOff_shouldOnlyRequireTypeWrite() throws Exception {
        Config.setProperty(FeatureFlagName.FEATURE_FLAG_CHECKIN_DESTINATION_PERMISSION_CHECK, false);
        try {
            final Contentlet saved = checkinAs(limitedUser, false, otherSite, null);
            assertEquals(otherSite.getIdentifier(), saved.getHost());
        } finally {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_CHECKIN_DESTINATION_PERMISSION_CHECK, true);
        }
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}
     * Given Scenario: Anonymous writes are allowed, front-end roles are respected, and the
     *                 anonymous role has WRITE on the type and Add Children on one site only.
     * ExpectedResult: The anonymous user can create content on that site but not on the other.
     */
    @Test
    public void anonymousCheckin_shouldFollowTheAnonymousRoleGrants() throws Exception {
        final String previous = Config.getStringProperty(
                AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "READ");
        final Role anonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
        final User anonymous = APILocator.getUserAPI().getAnonymousUser();
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS,
                AnonymousAccess.WRITE.name());
        try {
            grant(contentType, anonymousRole, TYPE_WRITE);
            grant(grantedSite, anonymousRole, ADD_CHILDREN_ON_SITE);

            final Contentlet saved = checkinAs(anonymous, true, grantedSite, null);
            assertEquals(grantedSite.getIdentifier(), saved.getHost());

            assertDenied(() -> checkinAs(anonymous, true, otherSite, null));
        } finally {
            Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, previous);
        }
    }

    /**
     * Runs a checkin that must be refused. When the content type's default workflow action takes
     * over the save, the refusal reaches the caller wrapped in a {@link DotRuntimeException}, so
     * both the bare {@link DotSecurityException} and the wrapped form count as denied.
     */
    private static void assertDenied(final CheckedRunnable checkin) {
        try {
            checkin.run();
            fail("The checkin should have been denied");
        } catch (final Exception e) {
            assertTrue("Expected a permission refusal but got " + e,
                    e instanceof DotSecurityException
                            || ExceptionUtil.causedBy(e, DotSecurityException.class));
            assertTrue(e.getMessage().contains("Add Children"));
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }

    /**
     * Checks in a brand-new contentlet of the test type on the given site, optionally inside the
     * given folder, as the given user. Created content is removed with the content type.
     */
    private static Contentlet checkinAs(final User user, final boolean respectFrontendRoles,
            final Host site, final Folder folder) throws DotDataException, DotSecurityException {

        final ContentletDataGen dataGen = new ContentletDataGen(contentType)
                .host(site)
                .setProperty(titleField.variable(), "title-" + System.nanoTime());
        if (folder != null) {
            dataGen.folder(folder);
        }
        final Contentlet contentlet = dataGen.next();

        return contentletAPI.checkin(contentlet, user, respectFrontendRoles);
    }

    /**
     * Gives the role the given permission bits on the asset as an individual permission.
     */
    private static void grant(final Permissionable asset, final Role role, final int bits)
            throws DotDataException, DotSecurityException {
        final Permission permission = new Permission(asset.getPermissionId(), role.getId(), bits,
                true);
        permissionAPI.save(permission, asset, APILocator.systemUser(), false);
    }
}
