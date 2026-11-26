package com.dotcms.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link ReadableRowFilter}: the batch permission check with the per-item
 * re-check of what it rejects (research R4, FR-012).
 * <p>
 * The batch query reads inherited permissions only through {@code permission_reference} rows,
 * which are written lazily, and ignores CMS Owner grants. These cases prove such rows survive the
 * filter, so an export never holds fewer rows than the listing shows, while unreadable rows are
 * still dropped.
 */
public class ReadableRowFilterTest extends IntegrationTestBase {

    private static User systemUser;
    private static PermissionAPI permissionAPI;
    private static boolean asyncReferencesOldValue;

    private static User limitedUser;
    private static ContentType contentType;

    /** Readable through an individual READ grant to the limited user's role. */
    private static Contentlet individuallyReadable;
    /** Readable only by inheriting READ from its folder, with its permission references deleted. */
    private static Contentlet inheritedOnly;
    /** Readable only because the limited user owns it and CMS Owner has READ on it. */
    private static Contentlet ownedByUser;
    /** Not readable by the limited user at all. */
    private static Contentlet unreadable;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        permissionAPI = APILocator.getPermissionAPI();

        // Permission reference updates are async by default; the grants below must be in place
        // before the assertions. ContentDriveLinksTest pins the same flag for the same reason.
        asyncReferencesOldValue = Config.getBooleanProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", true);
        Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", false);

        final String unique = String.valueOf(System.currentTimeMillis());
        final Host site = new SiteDataGen().name("readable-rows-" + unique + ".local").nextPersisted();
        final Folder openFolder = new FolderDataGen().site(site).name("open-" + unique).nextPersisted();
        contentType = new ContentTypeDataGen().host(site)
                .field(new FieldDataGen().type(TextField.class).name("title").velocityVarName("title").next())
                .nextPersisted();

        // A back-end user, as every Drive user is: dotCMS denies a front-end user READ on any
        // contentlet that is not live, and these fixtures are working versions.
        final Role role = new RoleDataGen().nextPersisted();
        limitedUser = new UserDataGen()
                .roles(role, APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final String userRoleId = APILocator.getRoleAPI().getUserRole(limitedUser).getId();

        individuallyReadable = contentlet(site, null, "individual");
        permissionAPI.save(new Permission(individuallyReadable.getPermissionId(), userRoleId,
                PermissionAPI.PERMISSION_READ), individuallyReadable, systemUser, false);
        assertTrue("Fixture error: the individually granted contentlet must be readable per item",
                permissionAPI.doesUserHavePermission(individuallyReadable,
                        PermissionAPI.PERMISSION_READ, limitedUser, false));

        // An inheritable READ on the folder, keyed on the Contentlet class: dotCMS keys inheritable
        // grants on the child's class name. A new folder inherits from its site, so it first gets its
        // own permission set; otherwise the grant below is not what its contentlets inherit.
        permissionAPI.permissionIndividually(permissionAPI.findParentPermissionable(openFolder),
                openFolder, systemUser);
        permissionAPI.save(new Permission(Contentlet.class.getCanonicalName(),
                openFolder.getPermissionId(), userRoleId, PermissionAPI.PERMISSION_READ, true),
                openFolder, systemUser, false);
        inheritedOnly = contentlet(site, openFolder, "inherited");
        assertTrue("Fixture error: the inherited contentlet must be readable per item",
                permissionAPI.doesUserHavePermission(inheritedOnly, PermissionAPI.PERMISSION_READ,
                        limitedUser, false));
        new DotConnect().setSQL("DELETE FROM permission_reference WHERE asset_id = ?")
                .addParam(inheritedOnly.getPermissionId()).loadResult();

        final Contentlet ownedDraft = new ContentletDataGen(contentType).host(site)
                .setProperty("title", "owned").next();
        ownedDraft.setOwner(limitedUser.getUserId());
        ownedByUser = ContentletDataGen.checkin(ownedDraft);
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        permissionAPI.save(new Permission(ownedByUser.getPermissionId(), cmsOwner.getId(),
                PermissionAPI.PERMISSION_READ), ownedByUser, systemUser, false);
        assertEquals("Fixture error: the limited user must own this contentlet",
                limitedUser.getUserId(), ownedByUser.getOwner());
        assertTrue("Fixture error: the owner must be able to read it per item",
                permissionAPI.doesUserHavePermission(ownedByUser, PermissionAPI.PERMISSION_READ,
                        limitedUser, false));

        unreadable = contentlet(site, null, "unreadable");
        assertFalse("Fixture error: the unreadable contentlet must not be readable",
                permissionAPI.doesUserHavePermission(unreadable, PermissionAPI.PERMISSION_READ,
                        limitedUser, false));
    }

    @AfterClass
    public static void restore() {
        Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", asyncReferencesOldValue);
    }

    private static Contentlet contentlet(final Host site, final Folder folder, final String title) {
        final ContentletDataGen dataGen = new ContentletDataGen(contentType).host(site)
                .setProperty("title", title);
        if (folder != null) {
            dataGen.folder(folder);
        }
        return dataGen.nextPersisted();
    }

    private static ExportRow row(final Contentlet contentlet) {
        return new ExportRow(contentlet.getInode(), contentlet.getIdentifier(),
                contentlet.getLanguageId(), contentlet.getContentTypeId());
    }

    private static List<String> identifiers(final List<ExportRow> rows) {
        return rows.stream().map(ExportRow::identifier).collect(Collectors.toList());
    }

    /**
     * Method to test: {@link ReadableRowFilter#filter(List, User)}
     * Given Scenario: A limited user and one row readable through an individual permission
     * ExpectedResult: The row is kept
     */
    @Test
    public void test_filter_keepsIndividuallyReadableRow() throws Exception {
        final List<ExportRow> kept = new ReadableRowFilter()
                .filter(List.of(row(individuallyReadable)), limitedUser);

        assertEquals(List.of(individuallyReadable.getIdentifier()), identifiers(kept));
    }

    /**
     * Method to test: {@link ReadableRowFilter#filter(List, User)}
     * Given Scenario: A row readable only through inheritance from its folder, whose
     *                 {@code permission_reference} rows were deleted
     * ExpectedResult: The row is kept, by the per-item re-check
     */
    @Test
    public void test_filter_keepsInheritedRowWithoutPermissionReferences() throws Exception {
        final List<ExportRow> kept = new ReadableRowFilter()
                .filter(List.of(row(inheritedOnly)), limitedUser);

        assertEquals(List.of(inheritedOnly.getIdentifier()), identifiers(kept));
    }

    /**
     * Method to test: {@link ReadableRowFilter#filter(List, User)}
     * Given Scenario: A row the limited user can read only as its CMS Owner
     * ExpectedResult: The row is kept
     */
    @Test
    public void test_filter_keepsRowReadableAsCmsOwner() throws Exception {
        final List<ExportRow> kept = new ReadableRowFilter()
                .filter(List.of(row(ownedByUser)), limitedUser);

        assertEquals(List.of(ownedByUser.getIdentifier()), identifiers(kept));
    }

    /**
     * Method to test: {@link ReadableRowFilter#filter(List, User)}
     * Given Scenario: A row the limited user cannot read
     * ExpectedResult: The row is dropped
     */
    @Test
    public void test_filter_dropsUnreadableRow() throws Exception {
        final List<ExportRow> kept = new ReadableRowFilter()
                .filter(List.of(row(unreadable)), limitedUser);

        assertTrue(kept.isEmpty());
    }

    /**
     * Method to test: {@link ReadableRowFilter#filter(List, User)}
     * Given Scenario: A CMS Administrator and every row, including the one the limited user cannot
     *                 read
     * ExpectedResult: Every row is kept, and no batch permission query is made
     */
    @Test
    public void test_filter_adminKeepsEveryRowWithoutAPermissionQuery() throws Exception {
        final User admin = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadCMSAdminRole()).nextPersisted();
        final PermissionAPI spiedPermissions = spy(APILocator.getPermissionAPI());
        final ReadableRowFilter filter =
                new ReadableRowFilter(spiedPermissions, APILocator.getContentletAPI());

        final List<ExportRow> rows = List.of(row(individuallyReadable), row(inheritedOnly),
                row(ownedByUser), row(unreadable));
        final List<ExportRow> kept = filter.filter(rows, admin);

        assertEquals(identifiers(rows), identifiers(kept));
        verify(spiedPermissions, never()).filterCollection(anyCollection(), anyInt(),
                any(User.class), anyBoolean());
    }

    /**
     * Method to test: {@link ReadableRowFilter#filter(List, User)}
     * Given Scenario: Readable and unreadable rows mixed, in an order that is not sorted
     * ExpectedResult: The readable rows are returned in their input order
     */
    @Test
    public void test_filter_preservesInputOrder() throws Exception {
        final List<ExportRow> kept = new ReadableRowFilter().filter(List.of(row(ownedByUser),
                row(unreadable), row(inheritedOnly), row(individuallyReadable)), limitedUser);

        assertEquals(List.of(ownedByUser.getIdentifier(), inheritedOnly.getIdentifier(),
                individuallyReadable.getIdentifier()), identifiers(kept));
    }
}
