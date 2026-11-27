package com.dotcms.rest.api.v1.drive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.IntegrationTestBase;
import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;
import com.dotcms.content.export.ContentletCsvWriter;
import com.dotcms.content.export.HostFolderMode;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.drive.export.ContentDriveExportHelper;
import com.dotcms.rest.api.v1.drive.export.DriveExportRequestForm;
import com.dotcms.rest.api.v1.drive.export.ExportFileBuilder;
import com.dotcms.rest.api.v1.drive.export.ExportFileNames;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.ConfigUtils;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@code POST /api/v1/drive/export} in filter mode
 * ({@link ContentDriveResource#export}): nothing selected, the listing filtered to one content
 * type, one CSV with everything the listing shows (User Story 1; FR-001, FR-004, FR-005, FR-016,
 * FR-026, FR-034).
 * <p>
 * Every row check compares the CSV with the listing for the same request, paged to the end, as
 * identifier and language pairs.
 */
public class ContentDriveExportFilterModeTest extends IntegrationTestBase {

    private static final String ONE_TYPE = "Choose exactly one content type you can read to export.";
    private static final String FORMS_AND_SITES = "Forms and sites can't be exported.";
    private static final String EXPORT_FAILED =
            "The export failed. Try again, and contact your administrator if it keeps failing.";
    private static final String FOLDER_NOT_FOUND = "Folder not found.";
    private static final int MAX_PAGES = 200;

    /**
     * Created in {@link #prepare()}: building it reaches the index API, which only exists once the
     * test environment is initialized.
     */
    private static ContentDriveHelper helper;
    private static User systemUser;
    /**
     * Calls the endpoint. A REST request never runs as the system user (InteractiveUserPolicy
     * refuses it), so the export runs as a CMS Administrator, who also reads everything.
     */
    private static User adminUser;
    private static User limitedUser;
    /** A back-end user who can read the site and the Blog type but none of the Blog rows. */
    private static User siteReader;

    private static Host site;
    private static Folder folder;
    private static Folder subFolder;
    private static Language defaultLanguage;
    private static Language secondLanguage;
    private static WorkflowScheme scheme;

    private static ContentType blog;
    private static ContentType otherType;
    private static ContentType formType;
    /** A type the limited user cannot read: its only permission is READ for an unrelated role. */
    private static ContentType unreadableType;

    private static Contentlet inFolder;
    private static Contentlet inSubFolder;
    private static Contentlet atSiteRoot;
    private static Contentlet inSystemHost;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        helper = ContentDriveHelper.newInstance();
        systemUser = APILocator.systemUser();

        final String unique = String.valueOf(System.currentTimeMillis());
        site = new SiteDataGen().name("drive-export-" + unique + ".local").nextPersisted();
        folder = new FolderDataGen().site(site).name("blog-" + unique).nextPersisted();
        subFolder = new FolderDataGen().parent(folder).name("sub-" + unique).nextPersisted();
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        secondLanguage = new LanguageDataGen().nextPersisted();
        scheme = new WorkflowDataGen().name("DriveExportScheme_" + unique)
                .nextPersistedWithDefaultStepsAndActions();

        // FieldDataGen gives every field a text default value; every field here clears it.
        blog = new ContentTypeDataGen().host(site).workflowId(scheme.getId())
                .name("Blog" + unique).velocityVarName("driveExportBlog" + unique)
                .field(new FieldDataGen().defaultValue(null).type(TextField.class).name("title")
                        .velocityVarName("title").next())
                .field(new FieldDataGen().defaultValue(null).type(HostFolderField.class)
                        .name("location").velocityVarName("location").next())
                .nextPersisted();
        otherType = new ContentTypeDataGen().host(site)
                .name("Other" + unique).velocityVarName("driveExportOther" + unique)
                .field(new FieldDataGen().defaultValue(null).type(TextField.class).name("title")
                        .velocityVarName("title").next())
                .nextPersisted();
        // Content type READ is checked with front-end roles, so a new type inherits a READ any
        // user passes; an individual permission for another role replaces what it inherits.
        unreadableType = new ContentTypeDataGen().host(site)
                .name("Unreadable" + unique).velocityVarName("driveExportUnreadable" + unique)
                .field(new FieldDataGen().defaultValue(null).type(TextField.class).name("title")
                        .velocityVarName("title").next())
                .nextPersisted();
        APILocator.getPermissionAPI().save(new Permission(unreadableType.getPermissionId(),
                        new RoleDataGen().nextPersisted().getId(), PermissionAPI.PERMISSION_READ),
                unreadableType, systemUser, false);
        formType = new ContentTypeDataGen().baseContentType(BaseContentType.FORM).host(site)
                .name("DriveExportForm" + unique).velocityVarName("driveExportForm" + unique)
                .nextPersisted();

        inFolder = ContentletDataGen.publish(blogEntry(folder, "summer needle"));
        final Contentlet secondLanguageDraft = ContentletDataGen.checkout(inFolder);
        secondLanguageDraft.setLanguageId(secondLanguage.getId());
        ContentletDataGen.checkin(secondLanguageDraft);
        blogEntry(folder, "winter draft");
        final Contentlet archived = ContentletDataGen.publish(blogEntry(folder, "archived needle"));
        ContentletDataGen.archive(archived);
        inSubFolder = blogEntry(subFolder, "nested needle");
        atSiteRoot = blogEntry(null, "root entry");
        inSystemHost = new ContentletDataGen(blog).host(APILocator.systemHost())
                .languageId(defaultLanguage.getId()).setProperty("title", "shared needle")
                .nextPersisted();
        new ContentletDataGen(otherType).host(site).folder(folder).setProperty("title", "other")
                .nextPersisted();

        adminUser = new UserDataGen().roles(APILocator.getRoleAPI().loadCMSAdminRole(),
                APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        // A back-end user (Drive users are) who can read none of this site's content types.
        limitedUser = new UserDataGen().roles(new RoleDataGen().nextPersisted(),
                APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final Role siteReaderRole = new RoleDataGen().nextPersisted();
        siteReader = new UserDataGen().roles(siteReaderRole,
                APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        APILocator.getPermissionAPI().save(new Permission(site.getPermissionId(),
                siteReaderRole.getId(), PermissionAPI.PERMISSION_READ), site, systemUser, false);
        assertNotNull("Fixture error: the site reader must read the site",
                APILocator.getHostAPI().find(site.getIdentifier(), siteReader, false));
        assertNotNull("Fixture error: the site reader must read the Blog type",
                APILocator.getContentTypeAPI(siteReader).find(blog.variable()));
        assertFalse("Fixture error: the site reader must not read the Blog rows",
                APILocator.getPermissionAPI().doesUserHavePermission(inFolder,
                        PermissionAPI.PERMISSION_READ, siteReader, false));

        assertThrows("Fixture error: the limited user must not be able to read this type",
                DotSecurityException.class,
                () -> APILocator.getContentTypeAPI(limitedUser).find(unreadableType.variable()));
    }

    private static Contentlet blogEntry(final Folder target, final String title) {
        final ContentletDataGen dataGen = new ContentletDataGen(blog).host(site)
                .languageId(defaultLanguage.getId()).setProperty("title", title);
        if (target != null) {
            dataGen.folder(target);
        }
        return dataGen.nextPersisted();
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    private static String sitePath() {
        return "//" + site.getHostname() + "/";
    }

    private static String folderPath() {
        return "//" + site.getHostname() + folder.getPath();
    }

    /** A listing request for Blog in both languages, the way the Drive sends it. */
    private static DriveRequestForm.Builder query(final String assetPath) {
        return DriveRequestForm.builder()
                .assetPath(assetPath)
                .language(List.of(String.valueOf(defaultLanguage.getId()),
                        String.valueOf(secondLanguage.getId())))
                .contentTypes(List.of(blog.variable()))
                .showFolders(false)
                .live(false)
                .archived(false)
                .maxResults(5);
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

    private static Response export(final ContentDriveResource resource,
            final DriveRequestForm query, final User user) {
        return resource.export(requestAs(user), mock(HttpServletResponse.class),
                DriveExportRequestForm.builder().query(query).build());
    }

    /** The files that appeared in the export directory since {@code before}. */
    private static Set<Path> newFilesSince(final Set<Path> before) throws Exception {
        final Set<Path> added = new HashSet<>(exportDirectoryFiles());
        added.removeAll(before);
        return added;
    }

    /** The response entity as the client receives it. */
    private static String entityJson(final Response response) throws Exception {
        return DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                .writeValueAsString(response.getEntity());
    }

    /**
     * Writes the header row and then fails, as a disk or database failure halfway through a file
     * would.
     */
    private static final class FailingWriter extends ContentletCsvWriter {

        static final String SECRET = "internal detail that must never reach the client";

        @Override
        public int write(final ContentType contentType, final List<String> inodes,
                final User user, final HttpServletRequest request,
                final HostFolderMode hostFolderMode, final Writer out)
                throws DotDataException, IOException {
            out.write("Identifier,languageCode,countryCode\r\n");
            throw new DotDataException(SECRET);
        }
    }

    private static Response export(final DriveRequestForm query) {
        return export(query, adminUser);
    }

    private static String header(final Response response, final String name) {
        final Object value = response.getMetadata().getFirst(name);
        return value == null ? null : value.toString();
    }

    private static byte[] body(final Response response) throws Exception {
        assertTrue("the body is streamed", response.getEntity() instanceof StreamingOutput);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((StreamingOutput) response.getEntity()).write(out);
        return out.toByteArray();
    }

    private static String errorMessage(final Response response) {
        assertTrue("the error body is a ResponseEntityView",
                response.getEntity() instanceof ResponseEntityView);
        final List<ErrorEntity> errors = ((ResponseEntityView<?>) response.getEntity()).getErrors();
        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        return errors.get(0).getMessage();
    }

    /** {@code identifier|languageCode|countryCode} of every data row of a CSV. */
    private static Set<String> csvKeys(final String csv) {
        final List<List<String>> rows = parseCsv(csv);
        return rows.subList(1, rows.size()).stream()
                .map(row -> row.get(0) + "|" + row.get(1) + "|" + row.get(2))
                .collect(Collectors.toSet());
    }

    /** The same keys for every row the listing shows for a request, every page. */
    private static Set<String> listingKeys(final DriveRequestForm request) throws Exception {
        final Map<Long, Language> languages = new HashMap<>();
        final Set<String> keys = new HashSet<>();
        int cursor = 0;
        for (int page = 1; page <= MAX_PAGES; page++) {
            final PaginatedContents result = helper.driveSearch(
                    DriveRequestForm.builder().from(request).showFolders(false).live(false)
                            .offset(0).contentCursor(cursor).build(), systemUser);
            for (final Map<String, Object> item : result.list) {
                final Contentlet contentlet = APILocator.getContentletAPI()
                        .find((String) item.get("inode"), systemUser, false);
                final Language language = languages.computeIfAbsent(contentlet.getLanguageId(),
                        id -> APILocator.getLanguageAPI().getLanguage(id));
                keys.add(contentlet.getIdentifier() + "|" + language.getLanguageCode() + "|"
                        + language.getCountryCode());
            }
            if (!result.hasMoreContent) {
                return keys;
            }
            cursor = result.nextContentCursor;
        }
        throw new AssertionError("The listing did not end after " + MAX_PAGES + " pages");
    }

    private static String keyOf(final Contentlet contentlet) {
        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());
        return contentlet.getIdentifier() + "|" + language.getLanguageCode() + "|"
                + language.getCountryCode();
    }

    /**
     * Exports a request and asserts the CSV holds exactly the listing's rows, and that the listing
     * is not empty, so the comparison cannot pass vacuously.
     *
     * @return the CSV's keys
     */
    private static Set<String> assertSameRowsAsListing(final String label,
            final DriveRequestForm request) throws Exception {
        final Set<String> listing = listingKeys(request);
        assertFalse(label + ": the listing must show rows, or the comparison proves nothing",
                listing.isEmpty());
        final Response response = export(request);
        assertEquals(label, 200, response.getStatus());
        final Set<String> exported = csvKeys(new String(body(response), StandardCharsets.UTF_8));
        assertEquals(label, listing, exported);
        return exported;
    }

    private static Set<Path> exportDirectoryFiles() throws Exception {
        final Path directory = Path.of(ConfigUtils.getAssetTempPath(), ExportFileBuilder.DIRECTORY_NAME);
        if (!Files.exists(directory)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.collect(Collectors.toSet());
        }
    }

    /** Parses the Content Search CSV format: CRLF rows, quoted cells with doubled quotes. */
    private static List<List<String>> parseCsv(final String csv) {
        final List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        final StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            final char c = csv.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cell.append(c);
                }
            } else if (c == '"' && cell.length() == 0) {
                quoted = true;
            } else if (c == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
                i++;
            } else {
                cell.append(c);
            }
        }
        return rows;
    }

    // ---------------------------------------------------------------------------------------------
    // views
    // ---------------------------------------------------------------------------------------------

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: Each Drive view: All Site Content with and without System Host, the site
     *                 root, a folder, and System Host
     * ExpectedResult: Exactly the rows the listing shows in that view, every page
     */
    @Test
    public void test_eachView_exportsTheListingRows() throws Exception {
        final Set<String> all = assertSameRowsAsListing("All Site Content with System Host",
                query(sitePath()).browseScope(BrowseScope.ALL).includeSystemHost(true).build());
        assertTrue("any depth", all.contains(keyOf(inSubFolder)));
        assertTrue("System Host included", all.contains(keyOf(inSystemHost)));

        final Set<String> withoutSystemHost = assertSameRowsAsListing(
                "All Site Content without System Host",
                query(sitePath()).browseScope(BrowseScope.ALL).includeSystemHost(false).build());
        assertFalse("System Host excluded", withoutSystemHost.contains(keyOf(inSystemHost)));

        final Set<String> root = assertSameRowsAsListing("site root",
                query(sitePath()).browseScope(BrowseScope.ROOT).build());
        assertTrue(root.contains(keyOf(atSiteRoot)));
        assertFalse("only what sits at the root", root.contains(keyOf(inFolder)));

        final Set<String> inBlogFolder = assertSameRowsAsListing("folder",
                query(folderPath()).build());
        assertTrue(inBlogFolder.contains(keyOf(inFolder)));
        assertFalse("direct content only", inBlogFolder.contains(keyOf(inSubFolder)));

        final Set<String> systemHost = assertSameRowsAsListing("System Host",
                query(sitePath()).browseScope(BrowseScope.SYSTEM_HOST).build());
        assertEquals(Set.of(keyOf(inSystemHost)), systemHost);
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: Each listing filter: language, workflow, status, text
     * ExpectedResult: The same rows as the listing for each
     */
    @Test
    public void test_eachFilter_exportsTheListingRows() throws Exception {
        final DriveRequestForm.Builder all = query(sitePath()).browseScope(BrowseScope.ALL);

        assertSameRowsAsListing("language",
                all.build().withLanguage(List.of(String.valueOf(secondLanguage.getId()))));
        assertSameRowsAsListing("workflow", query(sitePath()).browseScope(BrowseScope.ALL)
                .workflow(List.of(WorkflowFilterForm.builder().scheme(scheme.getId()).build()))
                .build());
        assertSameRowsAsListing("status", query(sitePath()).browseScope(BrowseScope.ALL)
                .status(List.of("UNPUBLISHED")).build());
        final Set<String> text = assertSameRowsAsListing("text",
                query(sitePath()).browseScope(BrowseScope.ALL)
                        .filters(QueryFilters.builder().text("needle").build()).build());
        assertFalse("the text filter narrows", text.contains(keyOf(atSiteRoot)));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: A filter-mode export of a folder
     * ExpectedResult: 200 with the CSV media type, the Content Search file name, the length of the
     * body, the row count and one file; the body starts with the header row and writes the
     * host/folder column as a readable location
     */
    @Test
    public void test_response_headersAndBody() throws Exception {
        final Response response = export(query(folderPath()).build());

        assertEquals(200, response.getStatus());
        final MediaType mediaType = MediaType.valueOf(header(response, "Content-Type"));
        assertEquals("text", mediaType.getType());
        assertEquals("csv", mediaType.getSubtype());
        assertEquals("UTF-8", mediaType.getParameters().get("charset").toUpperCase());

        final String fileName = ExportFileNames.csvName(blog.name(), blog.variable(), new Date());
        assertEquals(ExportFileNames.contentDisposition(fileName),
                header(response, "Content-Disposition"));

        final byte[] body = body(response);
        assertEquals(String.valueOf(body.length), header(response, "Content-Length"));

        final List<List<String>> rows = parseCsv(new String(body, StandardCharsets.UTF_8));
        assertEquals(String.valueOf(rows.size() - 1), header(response, "X-Export-Contentlets"));
        assertEquals("1", header(response, "X-Export-Files"));

        final List<String> headerRow = rows.get(0);
        assertEquals(List.of("Identifier", "languageCode", "countryCode"), headerRow.subList(0, 3));
        assertTrue(headerRow.contains("title"));
        final int location = headerRow.indexOf("location");
        for (final List<String> row : rows.subList(1, rows.size())) {
            assertEquals("host/folder as a readable location",
                    "//" + site.getHostname() + folder.getPath(), row.get(location));
        }
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: The same request with the paging, sorting and display fields the export
     *                 ignores: cursor, offset, page size, sort, showFolders, showLinks and live
     * ExpectedResult: The same rows as without them
     */
    @Test
    public void test_ignoredFields_doNotChangeTheRows() throws Exception {
        final DriveRequestForm plain = query(sitePath()).browseScope(BrowseScope.ALL).build();
        final Set<String> expected = csvKeys(new String(body(export(plain)), StandardCharsets.UTF_8));
        assertTrue("fixture: the plain export has several rows", expected.size() > 3);

        final DriveRequestForm noisy = DriveRequestForm.builder().from(plain)
                .contentCursor(3).offset(2).maxResults(1).sortBy("title:desc")
                .showFolders(true).showLinks(true).live(true).build();
        final Response response = export(noisy);

        assertEquals(200, response.getStatus());
        assertEquals(expected, csvKeys(new String(body(response), StandardCharsets.UTF_8)));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: Zero content types, two, a variable that does not exist, and a type the
     *                 limited user cannot read
     * ExpectedResult: 400 with the same message every time, and no temp file left behind (FR-004,
     * FR-010)
     */
    @Test
    public void test_notExactlyOneReadableType_isRejectedWithOneMessage() throws Exception {
        final Set<Path> before = exportDirectoryFiles();
        final Map<String, Response> responses = new LinkedHashMap<>();
        responses.put("zero types", export(query(sitePath()).contentTypes(List.of()).build()));
        responses.put("two types", export(query(sitePath())
                .contentTypes(List.of(blog.variable(), otherType.variable())).build()));
        responses.put("unknown type", export(query(sitePath())
                .contentTypes(List.of("noSuchType" + System.currentTimeMillis())).build()));
        responses.put("unreadable type", export(query(sitePath())
                .contentTypes(List.of(unreadableType.variable())).build(), limitedUser));

        responses.forEach((label, response) -> {
            assertEquals(label, 400, response.getStatus());
            assertEquals(label, ONE_TYPE, errorMessage(response));
        });
        assertEquals("no temp file is left", before, exportDirectoryFiles());
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: The listing filtered to a Form type
     * ExpectedResult: 400 "Forms and sites can't be exported." (FR-007)
     */
    @Test
    public void test_formType_isRejected() {
        final Response response = export(query(sitePath())
                .contentTypes(List.of(formType.variable())).build());

        assertEquals(400, response.getStatus());
        assertEquals(FORMS_AND_SITES, errorMessage(response));
    }
    // ---------------------------------------------------------------------------------------------
    // outcomes: nothing to export, failure, not found (User Story 7)
    // ---------------------------------------------------------------------------------------------

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (a) A user who can read the site and the type, exporting the whole site
     *                 (without System Host), none of whose Blog rows they can read
     * ExpectedResult: 204 with no body, and no file left in the export directory (FR-027, FR-032)
     */
    @Test
    public void test_nothingReadable_isNoContent() throws Exception {
        final Set<Path> before = exportDirectoryFiles();

        // The site, not a folder: the site reader's READ is on the site only, which its folders do
        // not inherit, so a folder path would be a 404 rather than an empty export.
        final Response response = export(query(sitePath()).browseScope(BrowseScope.ALL)
                .includeSystemHost(false).build(), siteReader);

        assertEquals(204, response.getStatus());
        assertNull(response.getEntity());
        assertEquals("no temp file is left", Set.of(), newFilesSince(before));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (b) The CSV writer fails halfway through the file
     * ExpectedResult: 500 with the generic message; neither the cause nor a stack trace reaches the
     * client, and no file is left in the export directory (FR-029, FR-031, FR-032)
     */
    @Test
    public void test_writerFailure_isAGenericServerError() throws Exception {
        final Set<Path> before = exportDirectoryFiles();
        final ContentDriveResource failing =
                new ContentDriveResource(new ContentDriveExportHelper(new FailingWriter()));

        final Response response = export(failing, query(folderPath()).build(), adminUser);

        assertEquals(500, response.getStatus());
        assertEquals(EXPORT_FAILED, errorMessage(response));
        final String body = entityJson(response);
        assertFalse("the cause is not returned", body.contains(FailingWriter.SECRET));
        assertFalse("no stack trace is returned", body.contains("Exception"));
        assertFalse("no stack trace is returned", body.contains("\tat "));
        assertEquals("no temp file is left", Set.of(), newFilesSince(before));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (c) A site the user cannot read, a site that does not exist, and a folder that
     *                 does not exist in a real site
     * ExpectedResult: 404 "Folder not found." each time, with identical bodies, so the response never
     * tells which applied (FR-014)
     */
    @Test
    public void test_missingOrUnreadableLocation_isNotFound() throws Exception {
        final Map<String, Response> responses = new LinkedHashMap<>();
        responses.put("unreadable site", export(query(sitePath()).build(), limitedUser));
        responses.put("missing site", export(query("//no-such-site-" + System.currentTimeMillis()
                + ".local/").build(), adminUser));
        responses.put("missing folder", export(query(sitePath() + "no-such-folder-"
                + System.currentTimeMillis() + "/").build(), adminUser));

        final Set<String> bodies = new HashSet<>();
        for (final Map.Entry<String, Response> entry : responses.entrySet()) {
            assertEquals(entry.getKey(), 404, entry.getValue().getStatus());
            assertEquals(entry.getKey(), FOLDER_NOT_FOUND, errorMessage(entry.getValue()));
            bodies.add(entityJson(entry.getValue()));
        }
        assertEquals("identical bodies", 1, bodies.size());
    }
}
