package com.dotcms.rest.api.v1.drive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.drive.export.DriveExportRequestForm;
import com.dotcms.rest.api.v1.drive.export.ExportContentletRefForm;
import com.dotcms.rest.api.v1.drive.export.ExportFileBuilder;
import com.dotcms.rest.api.v1.drive.export.ExportFileNames;
import com.dotcms.rest.api.v1.drive.export.ExportSelectionForm;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.ByteArrayInputStream;
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

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        adminUser = new UserDataGen().roles(APILocator.getRoleAPI().loadCMSAdminRole(),
                APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        limitedUser = new UserDataGen().roles(new RoleDataGen().nextPersisted(),
                APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();

        final String unique = String.valueOf(System.currentTimeMillis());
        site = new SiteDataGen().name("drive-selection-" + unique + ".local").nextPersisted();
        final Host otherSite = new SiteDataGen().name("drive-selection-other-" + unique + ".local")
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
}
