package com.dotcms.rest.api.v1.drive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.DotAssetDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.drive.export.ContentDriveExportHelper;
import com.dotcms.rest.api.v1.drive.export.DriveExportRequestForm;
import com.dotcms.rest.api.v1.drive.export.ExportContentletRefForm;
import com.dotcms.rest.api.v1.drive.export.ExportFileBuilder;
import com.dotcms.rest.api.v1.drive.export.ExportFileNames;
import com.dotcms.rest.api.v1.drive.export.ExportSelectionForm;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import io.vavr.control.Try;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@code POST /api/v1/drive/export} in selection mode, contentlet part
 * (User Story 2; FR-002, FR-002a, FR-003, FR-004a, FR-008, FR-016 – FR-019): the selected
 * contentlets, each in its selected language, as a zip with one CSV per content type.
 */
public class ContentDriveExportSelectionModeTest extends IntegrationTestBase {

    private static final String TOO_MANY = "You can export at most 60 items at once.";
    private static final String FOLDER_NOT_FOUND = "Folder not found.";

    /** Calls the endpoint: a REST request never runs as the system user. */
    private static User adminUser;
    /** A back-end user who can read nothing on the site. */
    private static User limitedUser;
    private static Host site;
    private static Language defaultLanguage;
    private static Language secondLanguage;

    private static ContentType blog;
    private static ContentType news;
    private static ContentType twinOne;
    private static ContentType twinTwo;
    private static ContentType unsafeName;

    private static Contentlet blogOne;
    private static Contentlet blogOneSecondLanguage;
    private static Contentlet blogTwo;
    private static Contentlet newsOne;
    private static Contentlet archivedBlog;
    private static Contentlet editedBlog;
    private static Contentlet newsInSystemHost;
    private static Contentlet inOtherSite;
    private static Contentlet formEntry;
    private static Contentlet twinOneEntry;
    private static Contentlet twinTwoEntry;
    private static Contentlet unsafeNameEntry;

    // Folder part (User Story 3)
    private static final String INVALID_PATH = "Invalid folder path.";
    private static Host otherSite;
    /** {@code /a/} holds X; {@code /a/b/c/} holds Y. */
    private static Folder folderA;
    private static Folder folderAB;
    private static Contentlet x;
    private static Contentlet y;
    /** Holds one contentlet in two languages and an archived one. */
    private static Folder languagesFolder;
    private static Contentlet bilingual;
    private static Contentlet bilingualSecondLanguage;
    private static Contentlet archivedInFolder;
    /** Holds a file asset, a dotAsset and a page. */
    private static Folder assetsFolder;
    private static Contentlet fileAsset;
    private static Contentlet dotAsset;
    private static Contentlet page;
    /** Holds a Form entry and a Blog entry. */
    private static Folder formFolder;
    private static Contentlet formInFolder;
    private static Contentlet blogInFormFolder;
    private static Folder otherSiteFolder;

    // Limits and outcomes (Phase 10)
    private static Folder fiveRowsFolder;
    private static Folder sixRowsFolder;
    /** Holds 61 contentlets: more rows than selection entries allow, as one entry. */
    private static Folder largeFolder;
    /** Readable as a folder by {@link #folderReader}, whose content they cannot read. */
    private static Folder lockedContentFolder;
    private static User folderReader;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        adminUser = new UserDataGen().roles(APILocator.getRoleAPI().loadCMSAdminRole(),
                APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        limitedUser = new UserDataGen().roles(new RoleDataGen().nextPersisted(),
                APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();

        final String unique = String.valueOf(System.currentTimeMillis());
        site = new SiteDataGen().name("drive-selection-" + unique + ".local").nextPersisted();
        otherSite = new SiteDataGen().name("drive-selection-other-" + unique + ".local")
                .nextPersisted();
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        secondLanguage = new LanguageDataGen().nextPersisted();

        blog = titledType("Blog" + unique, "driveSelBlog" + unique);
        news = titledType("News" + unique, "driveSelNews" + unique);
        twinOne = titledType("Twin " + unique, "driveSelTwinOne" + unique);
        twinTwo = titledType("Twin " + unique, "driveSelTwinTwo" + unique);
        unsafeName = titledType("A/B: C", "driveSelUnsafe" + unique);

        blogOne = entry(blog, site, "blog one");
        final Contentlet secondLanguageDraft = ContentletDataGen.checkout(blogOne);
        secondLanguageDraft.setLanguageId(secondLanguage.getId());
        blogOneSecondLanguage = ContentletDataGen.checkin(secondLanguageDraft);
        blogTwo = entry(blog, site, "blog two");
        newsOne = entry(news, site, "news one");

        archivedBlog = ContentletDataGen.publish(entry(blog, site, "archived blog"));
        ContentletDataGen.archive(archivedBlog);

        editedBlog = entry(blog, site, "before edit");
        newsInSystemHost = entry(news, APILocator.systemHost(), "shared news");
        inOtherSite = entry(blog, otherSite, "other site blog");

        final ContentType formType = new ContentTypeDataGen()
                .baseContentType(BaseContentType.FORM).host(site)
                .name("DriveSelForm" + unique).velocityVarName("driveSelForm" + unique)
                .nextPersisted();
        formEntry = new ContentletDataGen(formType).host(site).nextPersistedWithSampleTextValues();

        twinOneEntry = entry(twinOne, site, "twin one");
        twinTwoEntry = entry(twinTwo, site, "twin two");
        unsafeNameEntry = entry(unsafeName, site, "unsafe");

        folderA = new FolderDataGen().site(site).name("a").nextPersisted();
        folderAB = new FolderDataGen().parent(folderA).name("b").nextPersisted();
        final Folder folderABC = new FolderDataGen().parent(folderAB).name("c").nextPersisted();
        x = entryIn(blog, folderA, "x");
        y = entryIn(news, folderABC, "y");

        languagesFolder = new FolderDataGen().site(site).name("languages").nextPersisted();
        bilingual = entryIn(blog, languagesFolder, "bilingual");
        final Contentlet bilingualDraft = ContentletDataGen.checkout(bilingual);
        bilingualDraft.setLanguageId(secondLanguage.getId());
        bilingualSecondLanguage = ContentletDataGen.checkin(bilingualDraft);
        archivedInFolder = ContentletDataGen.publish(entryIn(blog, languagesFolder, "archived"));
        ContentletDataGen.archive(archivedInFolder);

        // "assets" is a reserved folder name
        assetsFolder = new FolderDataGen().site(site).name("media").nextPersisted();
        final File text = File.createTempFile("drive-export-file", ".txt");
        Files.writeString(text.toPath(), "file asset body");
        fileAsset = new FileAssetDataGen(assetsFolder, text).nextPersisted();
        final File image = File.createTempFile("drive-export-dotasset", ".png");
        Files.write(image.toPath(), new byte[]{1, 2, 3});
        dotAsset = new DotAssetDataGen(site, assetsFolder, image).nextPersisted();
        page = new HTMLPageDataGen(assetsFolder, new TemplateDataGen().nextPersisted())
                .nextPersisted();

        formFolder = new FolderDataGen().site(site).name("forms").nextPersisted();
        formInFolder = new ContentletDataGen(formType).host(site).folder(formFolder)
                .nextPersistedWithSampleTextValues();
        blogInFormFolder = entryIn(blog, formFolder, "beside the form");

        otherSiteFolder = new FolderDataGen().site(otherSite).name("elsewhere").nextPersisted();

        fiveRowsFolder = folderWith("five", 5);
        sixRowsFolder = folderWith("six", 6);
        largeFolder = folderWith("large", 61);

        // READ on the site and on the folder itself, which its content does not inherit
        lockedContentFolder = folderWith("locked", 2);
        final Role folderReaderRole = new RoleDataGen().nextPersisted();
        folderReader = new UserDataGen().roles(folderReaderRole,
                APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        for (final Permissionable permissionable : List.of(site, lockedContentFolder)) {
            APILocator.getPermissionAPI().save(new Permission(permissionable.getPermissionId(),
                    folderReaderRole.getId(), PermissionAPI.PERMISSION_READ), permissionable,
                    APILocator.systemUser(), false);
        }
        assertTrue("Fixture error: the folder reader must read the folder",
                APILocator.getPermissionAPI().doesUserHavePermission(lockedContentFolder,
                        PermissionAPI.PERMISSION_READ, folderReader, false));
        assertTrue("Fixture error: the folder reader must read none of its content",
                APILocator.getContentletAPI().findContentletsByFolder(lockedContentFolder,
                        APILocator.systemUser(), false).stream()
                        .noneMatch(contentlet -> Try.of(() -> APILocator.getPermissionAPI()
                                .doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ,
                                        folderReader, false)).get()));
    }

    /** A folder at the site root holding {@code rows} Blog contentlets. */
    private static Folder folderWith(final String name, final int rows) {
        final Folder folder = new FolderDataGen().site(site).name(name).nextPersisted();
        for (int i = 0; i < rows; i++) {
            entryIn(blog, folder, name + " " + i);
        }
        return folder;
    }

    private static Contentlet entryIn(final ContentType type, final Folder folder,
            final String title) {
        return new ContentletDataGen(type).host(site).folder(folder)
                .languageId(defaultLanguage.getId()).setProperty("title", title).nextPersisted();
    }

    private static ContentType titledType(final String name, final String variable) {
        return new ContentTypeDataGen().host(site).name(name).velocityVarName(variable)
                .field(new FieldDataGen().defaultValue(null).type(TextField.class).name("title")
                        .velocityVarName("title").next())
                .nextPersisted();
    }

    private static Contentlet entry(final ContentType type, final Host host, final String title) {
        return new ContentletDataGen(type).host(host).languageId(defaultLanguage.getId())
                .setProperty("title", title).nextPersisted();
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    private static ExportContentletRefForm ref(final Contentlet contentlet) {
        return ExportContentletRefForm.builder().identifier(contentlet.getIdentifier())
                .languageId(contentlet.getLanguageId()).build();
    }

    private static DriveExportRequestForm.Builder selection(final String siteId,
            final List<ExportContentletRefForm> contentlets) {
        return DriveExportRequestForm.builder().siteId(siteId)
                .selection(ExportSelectionForm.builder().contentlets(contentlets).build());
    }

    private static DriveExportRequestForm selectionOf(final Contentlet... contentlets) {
        return selection(site.getIdentifier(), Stream.of(contentlets)
                .map(ContentDriveExportSelectionModeTest::ref).collect(Collectors.toList()))
                .build();
    }

    private static String pathOf(final Folder folder) {
        return "//" + site.getHostname() + folder.getPath();
    }

    /** A selection of folder paths and contentlets in the browsed site. */
    private static DriveExportRequestForm folderSelection(final List<String> folders,
            final Contentlet... contentlets) {
        return DriveExportRequestForm.builder().siteId(site.getIdentifier())
                .selection(ExportSelectionForm.builder().folders(folders)
                        .contentlets(Stream.of(contentlets)
                                .map(ContentDriveExportSelectionModeTest::ref)
                                .collect(Collectors.toList()))
                        .build())
                .build();
    }

    private static HttpServletRequest requestAs(final User user) {
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/drive/export").request())
                .request();
        request.setAttribute(WebKeys.USER, user);
        return request;
    }

    private static Response export(final DriveExportRequestForm form, final User user) {
        return new ContentDriveResource().export(requestAs(user), mock(HttpServletResponse.class),
                form);
    }

    private static Response export(final DriveExportRequestForm form) {
        return export(form, adminUser);
    }

    private static String header(final Response response, final String name) {
        final Object value = response.getMetadata().getFirst(name);
        return value == null ? null : value.toString();
    }

    private static String errorMessage(final Response response) {
        assertTrue("the error body is a ResponseEntityView",
                response.getEntity() instanceof ResponseEntityView);
        final List<ErrorEntity> errors = ((ResponseEntityView<?>) response.getEntity()).getErrors();
        return errors.get(0).getMessage();
    }

    /** The zip's entries, name to CSV text, in entry order. */
    private static Map<String, String> zipEntries(final Response response) throws Exception {
        assertEquals(200, response.getStatus());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((StreamingOutput) response.getEntity()).write(out);
        final Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    /** The data rows of a CSV, split on commas (the fixtures hold no comma in any value). */
    private static List<List<String>> dataRows(final String csv) {
        final String[] lines = csv.split("\r\n");
        final List<List<String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                rows.add(List.of(lines[i].split(",", -1)));
            }
        }
        return rows;
    }

    /** {@code identifier|languageCode|countryCode} of every row of every entry. */
    private static Set<String> rowKeys(final Map<String, String> entries) {
        final Set<String> keys = new HashSet<>();
        entries.values().forEach(csv -> dataRows(csv)
                .forEach(row -> keys.add(row.get(0) + "|" + row.get(1) + "|" + row.get(2))));
        return keys;
    }

    private static String keyOf(final Contentlet contentlet) {
        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());
        return contentlet.getIdentifier() + "|" + language.getLanguageCode() + "|"
                + language.getCountryCode();
    }

    private static String csvName(final ContentType type) {
        return ExportFileNames.csvName(type.name(), type.variable(), new Date());
    }

    private static Set<Path> exportDirectoryFiles() throws Exception {
        final Path directory = ExportFileBuilder.defaultDirectory();
        if (!Files.exists(directory)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.collect(Collectors.toSet());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // tests
    // ---------------------------------------------------------------------------------------------

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (a) Two Blog contentlets in the default language (one also has an unselected
     *                 second-language version) and one News contentlet are selected
     * ExpectedResult: A zip named after the site with one CSV per type: Blog with the 2 selected
     * rows only, News with 1; 3 rows in 2 files
     */
    @Test
    public void test_selectedContentlets_exportOneCsvPerType() throws Exception {
        final Response response = export(selectionOf(blogOne, blogTwo, newsOne));

        assertEquals(200, response.getStatus());
        assertEquals("application/zip", header(response, "Content-Type"));
        assertEquals(ExportFileNames.contentDisposition(
                        ExportFileNames.zipName(site.getHostname(), new Date())),
                header(response, "Content-Disposition"));
        assertEquals("3", header(response, "X-Export-Contentlets"));
        assertEquals("2", header(response, "X-Export-Files"));

        final Map<String, String> entries = zipEntries(response);
        assertEquals(Set.of(csvName(blog), csvName(news)), entries.keySet());
        final Set<String> blogRows = rowKeys(Map.of("blog", entries.get(csvName(blog))));
        assertEquals(Set.of(keyOf(blogOne), keyOf(blogTwo)), blogRows);
        assertFalse("the unselected language is not exported",
                blogRows.contains(keyOf(blogOneSecondLanguage)));
        assertEquals(Set.of(keyOf(newsOne)), rowKeys(Map.of("news", entries.get(csvName(news)))));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (b) Both language versions of one contentlet are selected
     * ExpectedResult: Both rows, each with its own language and country code
     */
    @Test
    public void test_bothLanguagesSelected_giveBothRows() throws Exception {
        final Map<String, String> entries =
                zipEntries(export(selectionOf(blogOne, blogOneSecondLanguage)));

        assertEquals(Set.of(keyOf(blogOne), keyOf(blogOneSecondLanguage)), rowKeys(entries));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (c) An archived contentlet is selected; (h) a System Host contentlet is
     *                 selected
     * ExpectedResult: Both are exported
     */
    @Test
    public void test_archivedAndSystemHostSelections_areExported() throws Exception {
        final Map<String, String> entries =
                zipEntries(export(selectionOf(archivedBlog, newsInSystemHost)));

        assertEquals(Set.of(keyOf(archivedBlog), keyOf(newsInSystemHost)), rowKeys(entries));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (d) The selection of (a), sent with a query naming another content type, a
     *                 text, a workflow, a status and another language
     * ExpectedResult: The same rows as without the query: the selection wins (FR-003, FR-008)
     */
    @Test
    public void test_query_doesNotChangeASelection() throws Exception {
        final Set<String> plain = rowKeys(zipEntries(export(selectionOf(blogOne, blogTwo, newsOne))));

        final DriveRequestForm query = DriveRequestForm.builder()
                .assetPath("//" + site.getHostname() + "/")
                .contentTypes(List.of(twinOne.variable()))
                .language(List.of(String.valueOf(secondLanguage.getId())))
                .status(List.of("ARCHIVED"))
                .workflow(List.of(WorkflowFilterForm.builder()
                        .scheme(UUID.randomUUID().toString()).build()))
                .filters(QueryFilters.builder().text("nothing matches this").build())
                .build();
        final DriveExportRequestForm withQuery = DriveExportRequestForm.builder()
                .from(selectionOf(blogOne, blogTwo, newsOne)).query(query).build();

        assertEquals(plain, rowKeys(zipEntries(export(withQuery))));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (e) A contentlet is edited after it was selected, so it has a new working
     *                 inode
     * ExpectedResult: It is exported once, with the edited values
     */
    @Test
    public void test_editedAfterSelection_exportsTheEditedValuesOnce() throws Exception {
        final DriveExportRequestForm form = selectionOf(editedBlog);

        final Contentlet checkout = ContentletDataGen.checkout(editedBlog);
        checkout.setProperty("title", "after edit");
        ContentletDataGen.checkin(checkout);

        final List<List<String>> rows = dataRows(zipEntries(export(form)).get(csvName(blog)));
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).contains("after edit"));
        assertFalse(rows.get(0).contains("before edit"));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (f) The same entry is sent twice
     * ExpectedResult: It is exported once
     */
    @Test
    public void test_repeatedEntry_isExportedOnce() throws Exception {
        final Response response = export(selectionOf(blogTwo, blogTwo));

        assertEquals("1", header(response, "X-Export-Contentlets"));
        assertEquals(1, dataRows(zipEntries(response).get(csvName(blog))).size());
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (g) A selection with a missing identifier, a contentlet of another site and a
     *                 Form entry besides a valid contentlet
     * ExpectedResult: They are dropped silently: the response equals the one for the valid
     * contentlet alone (FR-002a)
     */
    @Test
    public void test_unexportableEntries_areDroppedSilently() throws Exception {
        final Response alone = export(selectionOf(blogOne));
        final List<ExportContentletRefForm> mixed = new ArrayList<>(List.of(ref(blogOne),
                ExportContentletRefForm.builder().identifier(UUID.randomUUID().toString())
                        .languageId(defaultLanguage.getId()).build(),
                ref(inOtherSite), ref(formEntry)));
        final Response withDropped = export(selection(site.getIdentifier(), mixed).build());

        assertEquals(alone.getStatus(), withDropped.getStatus());
        assertEquals(header(alone, "X-Export-Contentlets"),
                header(withDropped, "X-Export-Contentlets"));
        assertEquals(header(alone, "X-Export-Files"), header(withDropped, "X-Export-Files"));
        assertEquals(header(alone, "Content-Disposition"),
                header(withDropped, "Content-Disposition"));
        assertEquals(zipEntries(alone), zipEntries(withDropped));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (i) Two content types with the same name; (j) a content type named
     *                 {@code A/B: C}
     * ExpectedResult: Two CSVs, the second ending {@code -2}; the unsafe name becomes
     * {@code A_B_ C} (FR-019)
     */
    @Test
    public void test_entryNames_areDistinctAndSafe() throws Exception {
        final Map<String, String> entries =
                zipEntries(export(selectionOf(twinOneEntry, twinTwoEntry, unsafeNameEntry)));

        // "_contents_<M_d_yyyy>.csv", today
        final String suffix = ExportFileNames.csvName("x", "x", new Date()).substring(1);
        assertTrue(entries.containsKey(twinOne.name() + suffix));
        assertTrue(entries.containsKey(twinOne.name() + suffix.replace(".csv", "-2.csv")));
        assertTrue(entries.containsKey("A_B_ C" + suffix));
        assertEquals(3, entries.size());
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (k) 61 entries
     * ExpectedResult: 400 with the 60-item message, and no file left (FR-004a)
     */
    @Test
    public void test_moreThanSixtyEntries_isRefused() throws Exception {
        final Set<Path> before = exportDirectoryFiles();
        final List<ExportContentletRefForm> refs = new ArrayList<>();
        for (int i = 0; i < 61; i++) {
            refs.add(ref(blogOne));
        }

        final Response response = export(selection(site.getIdentifier(), refs).build());

        assertEquals(400, response.getStatus());
        assertEquals(TOO_MANY, errorMessage(response));
        final Set<Path> added = new HashSet<>(exportDirectoryFiles());
        added.removeAll(before);
        assertEquals("no temp file is left", Set.of(), added);
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (l) A {@code siteId} that does not exist, and one the user cannot read
     * ExpectedResult: 404 "Folder not found." both times, with the same message (FR-014)
     */
    @Test
    public void test_missingOrUnreadableSite_isNotFound() {
        final Response missing = export(selection(UUID.randomUUID().toString(),
                List.of(ref(blogOne))).build());
        final Response unreadable = export(selectionOf(blogOne), limitedUser);

        assertEquals(404, missing.getStatus());
        assertEquals(FOLDER_NOT_FOUND, errorMessage(missing));
        assertEquals(404, unreadable.getStatus());
        assertEquals(FOLDER_NOT_FOUND, errorMessage(unreadable));
    }
    // ---------------------------------------------------------------------------------------------
    // folders (User Story 3)
    // ---------------------------------------------------------------------------------------------

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (a) {@code /a/} holds X and {@code /a/b/c/} holds Y; {@code /a/},
     *                 {@code /a/b/} and X are selected
     * ExpectedResult: X and Y once each, at any depth, and no row for a folder (FR-006, FR-017)
     */
    @Test
    public void test_nestedFolderSelection_exportsEachContentletOnce() throws Exception {
        final Response response = export(
                folderSelection(List.of(pathOf(folderA), pathOf(folderAB)), x));

        assertEquals("2", header(response, "X-Export-Contentlets"));
        final Map<String, String> entries = zipEntries(response);
        assertEquals(Set.of(keyOf(x), keyOf(y)), rowKeys(entries));
        final List<String> identifiers = new ArrayList<>();
        entries.values().forEach(csv -> dataRows(csv).forEach(row -> identifiers.add(row.get(0))));
        assertEquals("each contentlet once", 2, identifiers.size());
        assertFalse("no row for a folder", identifiers.contains(folderA.getIdentifier()));
        assertFalse("no row for a folder", identifiers.contains(folderAB.getIdentifier()));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (b) A folder holding a contentlet in two languages and an archived contentlet
     * ExpectedResult: All three rows: every language and archived content (FR-006)
     */
    @Test
    public void test_folder_exportsEveryLanguageAndArchivedContent() throws Exception {
        final Map<String, String> entries =
                zipEntries(export(folderSelection(List.of(pathOf(languagesFolder)))));

        assertEquals(Set.of(keyOf(bilingual), keyOf(bilingualSecondLanguage),
                keyOf(archivedInFolder)), rowKeys(entries));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (c) A folder holding a file asset, a dotAsset and a page
     * ExpectedResult: Each in its own type's CSV; the file asset's binary field is a download URL
     */
    @Test
    public void test_folderAssets_exportInTheirOwnTypeCsv() throws Exception {
        final Map<String, String> entries =
                zipEntries(export(folderSelection(List.of(pathOf(assetsFolder)))));

        for (final Contentlet asset : List.of(fileAsset, dotAsset, page)) {
            final String csv = entries.get(csvName(asset.getContentType()));
            assertNotNull("a CSV for " + asset.getContentType().variable(), csv);
            assertEquals(Set.of(keyOf(asset)), rowKeys(Map.of("csv", csv)));
        }
        final String fileCsv = entries.get(csvName(fileAsset.getContentType()));
        final String[] lines = fileCsv.split("\r\n");
        final List<String> header = List.of(lines[0].split(","));
        final List<String> row = List.of(lines[1].split(",", -1));
        final String binary = row.get(header.indexOf("fileAsset"));
        assertTrue("binary as a download URL: " + binary, binary.startsWith("http"));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (d) A folder and a contentlet outside it are selected
     * ExpectedResult: Both end up in the same zip
     */
    @Test
    public void test_folderAndOutsideContentlet_shareOneZip() throws Exception {
        final Map<String, String> entries =
                zipEntries(export(folderSelection(List.of(pathOf(folderA)), newsOne)));

        assertTrue(rowKeys(entries).containsAll(Set.of(keyOf(x), keyOf(y), keyOf(newsOne))));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (e) A folder holding a Form entry and a Blog entry
     * ExpectedResult: The Blog entry only: Form content is never exported (FR-007). Sites live in
     *                 System Host, never in a site folder, so the Host exclusion is covered by the
     *                 filter-mode and BrowserAPI tests
     */
    @Test
    public void test_folderForm_isNotExported() throws Exception {
        final Map<String, String> entries =
                zipEntries(export(folderSelection(List.of(pathOf(formFolder)))));

        assertEquals(Set.of(keyOf(blogInFormFolder)), rowKeys(entries));
        assertFalse(rowKeys(entries).contains(keyOf(formInFolder)));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (f) A path that does not parse, {@code //}, and the site root; (h) no file
     * ExpectedResult: 400 "Invalid folder path." each time, and no file left
     */
    @Test
    public void test_invalidFolderPaths_areRejected() throws Exception {
        final Set<Path> before = exportDirectoryFiles();
        for (final String path : List.of("not a path", "//", "//" + site.getHostname() + "/")) {
            final Response response = export(folderSelection(List.of(path)));

            assertEquals(path, 400, response.getStatus());
            assertEquals(path, INVALID_PATH, errorMessage(response));
        }
        final Set<Path> added = new HashSet<>(exportDirectoryFiles());
        added.removeAll(before);
        assertEquals("no temp file is left", Set.of(), added);
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: (g) A path that does not exist, a path naming a file, and a folder of another
     *                 site; (h) no file
     * ExpectedResult: 404 "Folder not found." each time, with identical bodies, and no file left
     * (FR-014)
     */
    @Test
    public void test_unresolvableFolders_areNotFound() throws Exception {
        final Set<Path> before = exportDirectoryFiles();
        final String filePath = "//" + site.getHostname()
                + APILocator.getIdentifierAPI().find(fileAsset.getIdentifier()).getPath();
        final Set<String> bodies = new HashSet<>();
        for (final String path : List.of(pathOf(folderA) + "no-such-folder/", filePath,
                "//" + otherSite.getHostname() + otherSiteFolder.getPath())) {
            final Response response = export(folderSelection(List.of(path)));

            assertEquals(path, 404, response.getStatus());
            assertEquals(path, FOLDER_NOT_FOUND, errorMessage(response));
            bodies.add(DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                    .writeValueAsString(response.getEntity()));
        }
        assertEquals("identical bodies", 1, bodies.size());
        final Set<Path> added = new HashSet<>(exportDirectoryFiles());
        added.removeAll(before);
        assertEquals("no temp file is left", Set.of(), added);
    }
    // ---------------------------------------------------------------------------------------------
    // limits and outcomes in selection mode (Phase 10)
    // ---------------------------------------------------------------------------------------------

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: {@code CONTENT_DRIVE_EXPORT_MAX_ROWS} set to 5; a folder with 6 readable
     *                 rows, then one with 5
     * ExpectedResult: 400 with the limit message, then 200 with 5 rows (FR-010)
     */
    @Test
    public void test_folderRows_countTowardTheRowLimit() throws Exception {
        final int previous = Config.getIntProperty(ContentDriveExportHelper.MAX_ROWS_KEY,
                ContentDriveExportHelper.MAX_ROWS_DEFAULT);
        Config.setProperty(ContentDriveExportHelper.MAX_ROWS_KEY, 5);
        try {
            final Set<Path> before = exportDirectoryFiles();
            final Response over = export(folderSelection(List.of(pathOf(sixRowsFolder))));
            assertEquals(400, over.getStatus());
            assertEquals(ContentDriveExportHelper.overLimitMessage(5), errorMessage(over));
            final Set<Path> added = new HashSet<>(exportDirectoryFiles());
            added.removeAll(before);
            assertEquals("no temp file is left", Set.of(), added);

            final Response atLimit = export(folderSelection(List.of(pathOf(fiveRowsFolder))));
            assertEquals("5", header(atLimit, "X-Export-Contentlets"));
        } finally {
            Config.setProperty(ContentDriveExportHelper.MAX_ROWS_KEY, previous);
        }
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: One folder holding 61 contentlets
     * ExpectedResult: 200 with 61 rows: the folder is one selected item, not 61 (FR-004a)
     */
    @Test
    public void test_largeFolder_countsAsOneSelectedItem() throws Exception {
        final Response response = export(folderSelection(List.of(pathOf(largeFolder))));

        assertEquals(200, response.getStatus());
        assertEquals("61", header(response, "X-Export-Contentlets"));
    }

    /**
     * Method to test: {@link ContentDriveResource#export}
     * Given Scenario: A user who can read a folder but none of its content selects it
     * ExpectedResult: 204, and no file left (FR-027, FR-032)
     */
    @Test
    public void test_readableFolderWithNothingReadable_isNoContent() throws Exception {
        final Set<Path> before = exportDirectoryFiles();

        final Response response =
                export(folderSelection(List.of(pathOf(lockedContentFolder))), folderReader);

        assertEquals(204, response.getStatus());
        final Set<Path> added = new HashSet<>(exportDirectoryFiles());
        added.removeAll(before);
        assertEquals("no temp file is left", Set.of(), added);
    }
}
