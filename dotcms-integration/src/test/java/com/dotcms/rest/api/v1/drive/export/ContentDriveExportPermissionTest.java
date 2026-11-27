package com.dotcms.rest.api.v1.drive.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

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
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.drive.BrowseScope;
import com.dotcms.rest.api.v1.drive.ContentDriveResource;
import com.dotcms.rest.api.v1.drive.DriveRequestForm;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for what a Content Drive export reveals to a user with limited permissions,
 * in filter mode (User Story 4; FR-012, FR-015, FR-026a).
 * <p>
 * It sits in the helper's package so case (c) can call the resolve and write steps one at a time
 * and change a permission in between.
 */
public class ContentDriveExportPermissionTest extends IntegrationTestBase {

    /** Every header the contract allows on a 200; anything else could leak a skipped count. */
    private static final Set<String> CONTRACT_HEADERS = Set.of("Content-Type",
            "Content-Disposition", "Content-Length", "X-Export-Contentlets", "X-Export-Files");

    private static boolean asyncReferencesOldValue;
    private static User systemUser;
    private static User limitedUser;
    private static Role limitedRole;
    private static Host site;

    /** Z is readable by another user, not by the limited one; A, B and C are readable by it. */
    private static ContentType zType;
    private static Contentlet z;
    private static List<Contentlet> readableSiblings;

    // Selection part
    /** Readable by the limited user, with readable A1 and A2 and unreadable Z1 in it. */
    private static Folder folderA;
    /** Under {@link #folderA}, not readable; holds S1 and, deeper, S2, both readable themselves. */
    private static Folder secretFolder;
    private static Contentlet a1;
    private static Contentlet a2;
    private static Contentlet z1;
    private static Contentlet s1;
    private static Contentlet s2;
    private static Contentlet inOtherSite;
    private static Folder otherSiteFolder;

    /** Three rows the limited user can read, one of which loses READ between the two steps. */
    private static ContentType revokeType;
    private static List<Contentlet> revokeRows;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

        // Permission references are written asynchronously by default; the grants below must be in
        // place before the assertions. ContentDriveLinksTest pins the same flag for the same reason.
        asyncReferencesOldValue = Config.getBooleanProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", true);
        Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", false);

        final String unique = String.valueOf(System.currentTimeMillis());
        site = new SiteDataGen().name("drive-export-perm-" + unique + ".local").nextPersisted();

        // A back-end user (Drive users are) with READ on the site, so its path resolves, and on
        // no content except what is granted below one row at a time.
        limitedRole = new RoleDataGen().nextPersisted();
        limitedUser = new UserDataGen().roles(limitedRole,
                APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        permissionAPI.save(new Permission(site.getPermissionId(), limitedRole.getId(),
                PermissionAPI.PERMISSION_READ), site, systemUser, false);

        zType = titledType("PermZ", unique);
        z = titled(zType, "Z");
        readableSiblings = new ArrayList<>();
        for (final String title : List.of("A", "B", "C")) {
            final Contentlet sibling = titled(zType, title);
            grantRead(sibling);
            readableSiblings.add(sibling);
        }

        // Z is readable, just not by the limited user.
        final Role otherRole = new RoleDataGen().nextPersisted();
        final User otherReader = new UserDataGen().roles(otherRole,
                APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        permissionAPI.save(new Permission(z.getPermissionId(), otherRole.getId(),
                PermissionAPI.PERMISSION_READ), z, systemUser, false);
        assertTrue("Fixture error: another user must read Z",
                permissionAPI.doesUserHavePermission(z, PermissionAPI.PERMISSION_READ, otherReader,
                        false));
        assertFalse("Fixture error: the limited user must not read Z",
                permissionAPI.doesUserHavePermission(z, PermissionAPI.PERMISSION_READ, limitedUser,
                        false));
        for (final Contentlet sibling : readableSiblings) {
            assertTrue("Fixture error: the limited user must read " + sibling.getTitle(),
                    permissionAPI.doesUserHavePermission(sibling, PermissionAPI.PERMISSION_READ,
                            limitedUser, false));
        }

        // /a/ is readable through an individual grant on the folder itself, which its subfolders
        // do not inherit, so /a/secret/ and everything below it stays unreadable.
        final ContentType selectionType = titledType("PermSelection", unique);
        folderA = new FolderDataGen().site(site).name("a").nextPersisted();
        secretFolder = new FolderDataGen().parent(folderA).name("secret").nextPersisted();
        final Folder deepFolder = new FolderDataGen().parent(secretFolder).name("deep")
                .nextPersisted();
        permissionAPI.save(new Permission(folderA.getPermissionId(), limitedRole.getId(),
                PermissionAPI.PERMISSION_READ), folderA, systemUser, false);
        a1 = titledIn(selectionType, folderA, "A1");
        a2 = titledIn(selectionType, folderA, "A2");
        z1 = titledIn(selectionType, folderA, "Z1");
        s1 = titledIn(selectionType, secretFolder, "S1");
        s2 = titledIn(selectionType, deepFolder, "S2");
        for (final Contentlet readable : List.of(a1, a2, s1, s2)) {
            grantRead(readable);
        }
        final Host otherSite = new SiteDataGen().name("drive-export-perm-other-" + unique + ".local")
                .nextPersisted();
        otherSiteFolder = new FolderDataGen().site(otherSite).name("elsewhere").nextPersisted();
        inOtherSite = new ContentletDataGen(selectionType).host(otherSite)
                .setProperty("title", "other site").nextPersisted();
        grantRead(inOtherSite);

        assertTrue("Fixture error: the limited user must read /a/", permissionAPI
                .doesUserHavePermission(folderA, PermissionAPI.PERMISSION_READ, limitedUser, false));
        assertFalse("Fixture error: the limited user must not read /a/secret/", permissionAPI
                .doesUserHavePermission(secretFolder, PermissionAPI.PERMISSION_READ, limitedUser,
                        false));
        assertFalse("Fixture error: the limited user must not read Z1", permissionAPI
                .doesUserHavePermission(z1, PermissionAPI.PERMISSION_READ, limitedUser, false));
        for (final Contentlet readable : List.of(a1, a2, s1, s2)) {
            assertTrue("Fixture error: the limited user must read " + readable.getTitle(),
                    permissionAPI.doesUserHavePermission(readable, PermissionAPI.PERMISSION_READ,
                            limitedUser, false));
        }

        revokeType = titledType("PermRevoke", unique);
        revokeRows = new ArrayList<>();
        for (final String title : List.of("R1", "R2", "R3")) {
            final Contentlet row = titled(revokeType, title);
            grantRead(row);
            revokeRows.add(row);
        }
    }

    @AfterClass
    public static void restore() {
        Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", asyncReferencesOldValue);
    }

    private static ContentType titledType(final String name, final String unique) {
        return new ContentTypeDataGen().host(site)
                .name(name + unique).velocityVarName("driveExport" + name + unique)
                .field(new FieldDataGen().defaultValue(null).type(TextField.class).name("title")
                        .velocityVarName("title").next())
                .nextPersisted();
    }

    private static Contentlet titled(final ContentType type, final String title) {
        return new ContentletDataGen(type).host(site)
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty("title", title).nextPersisted();
    }

    private static Contentlet titledIn(final ContentType type, final Folder folder,
            final String title) {
        return new ContentletDataGen(type).host(site).folder(folder)
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty("title", title).nextPersisted();
    }

    private static void grantRead(final Contentlet contentlet) throws Exception {
        APILocator.getPermissionAPI().save(new Permission(contentlet.getPermissionId(),
                limitedRole.getId(), PermissionAPI.PERMISSION_READ), contentlet, systemUser, false);
    }

    /** The whole site, without System Host, for one type, as the Drive sends it. */
    private static DriveRequestForm wholeSite(final ContentType type) {
        return DriveRequestForm.builder()
                .assetPath("//" + site.getHostname() + "/")
                .browseScope(BrowseScope.ALL)
                .includeSystemHost(false)
                .language(List.of(String.valueOf(
                        APILocator.getLanguageAPI().getDefaultLanguage().getId())))
                .contentTypes(List.of(type.variable()))
                .showFolders(false)
                .live(false)
                .archived(false)
                .build();
    }

    private static HttpServletRequest requestAs(final User user) {
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/drive/export").request())
                .request();
        request.setAttribute(WebKeys.USER, user);
        return request;
    }

    private static Response export(final DriveRequestForm query, final User user) {
        return new ContentDriveResource().export(requestAs(user), mock(HttpServletResponse.class),
                DriveExportRequestForm.builder().query(query).build());
    }

    private static String body(final Response response) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((StreamingOutput) response.getEntity()).write(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    /** The first cell of every data row: the identifiers, in file order. */
    private static List<String> identifiers(final String csv) {
        final String[] lines = csv.split("\r\n");
        final List<String> identifiers = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                identifiers.add(lines[i].split(",", -1)[0]);
            }
        }
        return identifiers;
    }

    private static Set<String> identifiersOf(final List<Contentlet> contentlets) {
        return contentlets.stream().map(Contentlet::getIdentifier).collect(Collectors.toSet());
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (a) A limited user exports a type in which they can read A, B and C but not
     *                 Z, which another user can read
     * ExpectedResult: The CSV holds A, B and C, and not Z (FR-012)
     */
    @Test
    public void test_unreadableRow_isNotExported() throws Exception {
        final Response response = export(wholeSite(zType), limitedUser);

        assertEquals(200, response.getStatus());
        final List<String> exported = identifiers(body(response));
        assertEquals(identifiersOf(readableSiblings), Set.copyOf(exported));
        assertFalse(exported.contains(z.getIdentifier()));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (b) The same export
     * ExpectedResult: Only the contract's headers are sent, and {@code X-Export-Contentlets} equals
     * the data rows, so nothing tells how many rows were skipped (FR-015)
     */
    @Test
    public void test_response_revealsNothingAboutSkippedRows() throws Exception {
        final Response response = export(wholeSite(zType), limitedUser);

        assertEquals(200, response.getStatus());
        for (final String header : response.getMetadata().keySet()) {
            assertTrue("unexpected header " + header, CONTRACT_HEADERS.contains(header));
        }
        final int rows = identifiers(body(response)).size();
        assertEquals(String.valueOf(rows),
                String.valueOf(response.getMetadata().getFirst("X-Export-Contentlets")));
        assertEquals(readableSiblings.size(), rows);
    }

    /**
     * Method to test: {@link ContentDriveExportHelper#resolveFilterMode} then
     *                 {@link ContentDriveExportHelper#write}
     * Given Scenario: (c) Three readable rows are resolved; READ on one of them is revoked before
     *                 they are written
     * ExpectedResult: That row is skipped, the file holds the other two, and the reported count
     * equals the rows written (US4 #8, FR-026a)
     */
    @Test
    public void test_rowUnreadableBeforeWriting_isSkippedAndNotCounted() throws Exception {
        final ContentDriveExportHelper helper = new ContentDriveExportHelper();
        final ContentDriveExportHelper.FilterResolution resolution =
                helper.resolveFilterMode(wholeSite(revokeType), limitedUser);
        assertEquals("fixture: all three rows resolve", 3, resolution.candidates().rows().size());

        final Contentlet revoked = revokeRows.get(1);
        APILocator.getPermissionAPI().removePermissions(revoked);
        assertFalse("fixture: READ is gone", APILocator.getPermissionAPI()
                .doesUserHavePermission(revoked, PermissionAPI.PERMISSION_READ, limitedUser,
                        false));

        final ExportFile file = helper.write(resolution, limitedUser, requestAs(limitedUser));
        try {
            final List<String> written = identifiers(Files.readString(file.path(),
                    StandardCharsets.UTF_8));
            assertEquals(2, file.contentlets());
            assertEquals(file.contentlets(), written.size());
            assertFalse(written.contains(revoked.getIdentifier()));
        } finally {
            Files.deleteIfExists(file.path());
        }
    }
    // ---------------------------------------------------------------------------------------------
    // selection mode
    // ---------------------------------------------------------------------------------------------

    private static ExportContentletRefForm ref(final String identifier) {
        return ExportContentletRefForm.builder().identifier(identifier)
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId()).build();
    }

    private static DriveExportRequestForm selection(final List<String> folders,
            final List<ExportContentletRefForm> contentlets) {
        return DriveExportRequestForm.builder().siteId(site.getIdentifier())
                .selection(ExportSelectionForm.builder().folders(folders).contentlets(contentlets)
                        .build())
                .build();
    }

    private static Response exportSelection(final DriveExportRequestForm form) {
        return new ContentDriveResource().export(requestAs(limitedUser),
                mock(HttpServletResponse.class), form);
    }

    private static String pathOf(final Host host, final Folder folder) {
        return "//" + host.getHostname() + folder.getPath();
    }

    /** The identifiers in every CSV of a zip response. */
    private static Set<String> zipIdentifiers(final Response response) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((StreamingOutput) response.getEntity()).write(out);
        final Set<String> identifiers = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                identifiers.addAll(identifiers(new String(zip.readAllBytes(),
                        StandardCharsets.UTF_8)));
            }
        }
        return identifiers;
    }

    private static String entityJson(final Response response) throws Exception {
        return DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                .writeValueAsString(response.getEntity());
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (a) The limited user exports /a/, which holds A1, A2 and the unreadable Z1,
     *                 and the unreadable /a/secret/ (with S1, and S2 deeper, both readable
     *                 themselves); (e) the same response's headers
     * ExpectedResult: A1 and A2 only: nothing under the unreadable folder at any depth, and not Z1
     * (FR-013); only the contract's headers, with the count equal to the rows (FR-015)
     */
    @Test
    public void test_selectedFolder_leavesOutUnreadableFoldersAndRows() throws Exception {
        final Response response = exportSelection(
                selection(List.of(pathOf(site, folderA)), List.of()));

        assertEquals(200, response.getStatus());
        for (final String header : response.getMetadata().keySet()) {
            assertTrue("unexpected header " + header, CONTRACT_HEADERS.contains(header));
        }
        assertEquals("2", String.valueOf(response.getMetadata().getFirst("X-Export-Contentlets")));
        assertEquals(Set.of(a1.getIdentifier(), a2.getIdentifier()), zipIdentifiers(response));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (b) S1, readable, sits in the unreadable /a/secret/ and is selected directly
     * ExpectedResult: S1 is exported: a contentlet's own READ decides, not its folder's (FR-009)
     */
    @Test
    public void test_readableContentletInUnreadableFolder_isExportedWhenSelected()
            throws Exception {
        final Response response = exportSelection(
                selection(List.of(), List.of(ref(s1.getIdentifier()))));

        assertEquals(200, response.getStatus());
        assertEquals(Set.of(s1.getIdentifier()), zipIdentifiers(response));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (c) An unreadable folder, a missing folder and another site's folder
     * ExpectedResult: 404 each time, with byte-identical bodies (FR-014)
     */
    @Test
    public void test_unreadableMissingAndOtherSiteFolders_lookTheSame() throws Exception {
        final Set<String> bodies = new HashSet<>();
        for (final String path : List.of(pathOf(site, secretFolder),
                pathOf(site, folderA) + "missing-" + System.currentTimeMillis() + "/",
                "//" + otherSiteFolder.getHost().getHostname() + otherSiteFolder.getPath())) {
            final Response response = exportSelection(selection(List.of(path), List.of()));
            assertEquals(path, 404, response.getStatus());
            bodies.add(entityJson(response));
        }
        assertEquals("byte-identical bodies", 1, bodies.size());
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (d) A1 with a missing contentlet, the unreadable Z1 and another site's
     *                 contentlet; then those three alone, together and one at a time
     * ExpectedResult: The same response as A1 alone; with nothing else left, 204 every time
     * (FR-002a, FR-015)
     */
    @Test
    public void test_droppedContentlets_leaveNoTrace() throws Exception {
        final List<ExportContentletRefForm> dropped = List.of(
                ref(UUID.randomUUID().toString()), ref(z1.getIdentifier()),
                ref(inOtherSite.getIdentifier()));

        final Response alone = exportSelection(
                selection(List.of(), List.of(ref(a1.getIdentifier()))));
        final List<ExportContentletRefForm> mixed = new ArrayList<>(List.of(ref(a1.getIdentifier())));
        mixed.addAll(dropped);
        final Response withDropped = exportSelection(selection(List.of(), mixed));

        assertEquals(alone.getStatus(), withDropped.getStatus());
        for (final String header : CONTRACT_HEADERS) {
            assertEquals(header, String.valueOf(alone.getMetadata().getFirst(header)),
                    String.valueOf(withDropped.getMetadata().getFirst(header)));
        }
        assertEquals(zipIdentifiers(alone), zipIdentifiers(withDropped));

        assertEquals(204, exportSelection(selection(List.of(), dropped)).getStatus());
        for (final ExportContentletRefForm one : dropped) {
            assertEquals(one.identifier(), 204,
                    exportSelection(selection(List.of(), List.of(one))).getStatus());
        }
    }
}
