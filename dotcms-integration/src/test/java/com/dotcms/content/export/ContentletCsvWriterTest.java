package com.dotcms.content.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HiddenField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.LineDividerField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FieldRelationshipDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.action.EditContentletAction;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.WebKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link ContentletCsvWriter}, the CSV writer moved out of the Content Search
 * export ({@code EditContentletAction.downloadToExcel}).
 * <p>
 * The first test is a <b>characterization</b> of today's Content Search CSV (T004). It runs
 * against the legacy code, so it must pass before the writer exists, and must keep passing after
 * {@code downloadToExcel} delegates to the writer: that is what proves Content Search output does
 * not change (FR-023). The identifiers, inodes and paths are generated, so the expected text is
 * checked cell by cell rather than held in a constant; the byte-for-byte comparison with the new
 * writer is made against the legacy output captured in the same run.
 * <p>
 * The other tests cover what the writer adds (T005): the {@link HostFolderMode#LOCATION} column,
 * skipping rows the user cannot read or that no longer exist, an empty cell for a field that fails,
 * a whole-file failure propagating, and the header-only cases.
 */
public class ContentletCsvWriterTest extends IntegrationTestBase {

    private static final String TITLE_VALUE = "He said \"hi\", then\r\nleft";
    private static final String TITLE_EXPECTED = "He said \"hi\", then\nleft";
    private static final String STORY_BLOCK_VALUE =
            "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":"
                    + "[{\"type\":\"text\",\"text\":\"Valid block\"}]}]}";
    private static final String CONSTANT_VALUE = "CONSTANT-VALUE";

    private static User systemUser;
    private static Host site;
    private static Host systemHost;
    private static Folder folder;
    private static Language defaultLanguage;
    private static Language secondLanguage;
    private static Category childOne;
    private static Category childTwo;
    private static Contentlet imageAsset;
    private static Contentlet relatedOne;
    private static Contentlet relatedTwo;
    private static ContentType contentType;
    private static Date dateValue;
    private static Date dateTimeValue;
    private static Date timeValue;

    /** The main contentlet, in a folder, in the default language. */
    private static Contentlet inFolder;
    /** The same identifier as {@link #inFolder}, in {@link #secondLanguage}. */
    private static Contentlet inFolderSecondLanguage;
    /** A contentlet at the site root (the site's system folder). */
    private static Contentlet atSiteRoot;
    /** A contentlet in System Host. */
    private static Contentlet inSystemHost;

    private static HttpServletRequest request;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.systemUser();
        systemHost = APILocator.getHostAPI().findSystemHost();
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        secondLanguage = new LanguageDataGen().nextPersisted();

        final String unique = String.valueOf(System.currentTimeMillis());
        site = new SiteDataGen().name("csv-writer-" + unique + ".local").nextPersisted();
        folder = new FolderDataGen().site(site).name("csv-" + unique).nextPersisted();

        final Category parent = new CategoryDataGen().setCategoryName("csvParent" + unique)
                .setKey("csvParent" + unique).setCategoryVelocityVarName("csvParent" + unique)
                .nextPersisted();
        childOne = new CategoryDataGen().setCategoryName("csvChildOne" + unique)
                .setKey("csvKeyOne" + unique).setCategoryVelocityVarName("csvChildOne" + unique)
                .parent(parent).nextPersisted();
        childTwo = new CategoryDataGen().setCategoryName("csvChildTwo" + unique)
                .setKey("csvKeyTwo" + unique).setCategoryVelocityVarName("csvChildTwo" + unique)
                .parent(parent).nextPersisted();

        final File image = File.createTempFile("csv-image", ".png");
        Files.write(image.toPath(), new byte[]{1, 2, 3});
        imageAsset = new FileAssetDataGen(folder, image).nextPersisted();

        final ContentType relatedType = new ContentTypeDataGen().host(site)
                .field(new FieldDataGen().defaultValue(null).type(TextField.class).name("label")
                        .velocityVarName("label").next())
                .nextPersisted();
        relatedOne = new ContentletDataGen(relatedType).host(site).setProperty("label", "r1")
                .nextPersisted();
        relatedTwo = new ContentletDataGen(relatedType).host(site).setProperty("label", "r2")
                .nextPersisted();

        // FieldDataGen gives every field a text default value; DateField rejects it, and a default
        // would also fill cells the test expects empty, so every field here clears it.
        final List<Field> fields = new ArrayList<>();
        fields.add(new FieldDataGen().defaultValue(null).type(TextField.class).name("title").velocityVarName("title").next());
        fields.add(new FieldDataGen().defaultValue(null).type(TextAreaField.class).name("notes").velocityVarName("notes").next());
        fields.add(new FieldDataGen().defaultValue(null).type(WysiwygField.class).name("body").velocityVarName("body").next());
        fields.add(new FieldDataGen().defaultValue(null).type(SelectField.class).name("color").velocityVarName("color")
                .values("Red|red\r\nBlue|blue").next());
        fields.add(new FieldDataGen().defaultValue(null).type(MultiSelectField.class).name("letters").velocityVarName("letters")
                .values("A|a\r\nB|b").next());
        fields.add(new FieldDataGen().defaultValue(null).type(CheckboxField.class).name("options").velocityVarName("options")
                .values("X|x\r\nY|y").next());
        fields.add(new FieldDataGen().defaultValue(null).type(RadioField.class).name("choice").velocityVarName("choice")
                .values("One|1\r\nTwo|2").next());
        fields.add(new FieldDataGen().defaultValue(null).type(DateField.class).name("pubDate").velocityVarName("pubDate").next());
        fields.add(new FieldDataGen().defaultValue(null).type(DateTimeField.class).name("eventAt").velocityVarName("eventAt").next());
        fields.add(new FieldDataGen().defaultValue(null).type(TimeField.class).name("startsAt").velocityVarName("startsAt").next());
        fields.add(new FieldDataGen().defaultValue(null).type(CategoryField.class).name("cats").velocityVarName("cats")
                .values(parent.getInode()).next());
        fields.add(new FieldDataGen().defaultValue(null).type(TagField.class).name("tags").velocityVarName("tags").next());
        fields.add(new FieldDataGen().defaultValue(null).type(KeyValueField.class).name("meta").velocityVarName("meta").next());
        fields.add(new FieldDataGen().defaultValue(null).type(ImageField.class).name("picture").velocityVarName("picture").next());
        fields.add(new FieldDataGen().defaultValue(null).type(BinaryField.class).name("attachment").velocityVarName("attachment").next());
        fields.add(new FieldDataGen().defaultValue(null).type(HostFolderField.class).name("location").velocityVarName("location").next());
        fields.add(new FieldDataGen().defaultValue(null).type(ConstantField.class).name("fixedText").velocityVarName("fixedText")
                .values(CONSTANT_VALUE).next());
        fields.add(new FieldDataGen().defaultValue(null).type(HiddenField.class).name("secret").velocityVarName("secret").next());
        fields.add(new FieldDataGen().defaultValue(null).type(StoryBlockField.class).name("story").velocityVarName("story").next());
        fields.add(new FieldDataGen().defaultValue(null).type(LineDividerField.class).name("divider").velocityVarName("divider").next());

        final ContentType created = new ContentTypeDataGen().host(site)
                .velocityVarName("csvWriter" + unique).name("CSV Writer " + unique)
                .fields(fields).nextPersisted();
        new FieldRelationshipDataGen().parent(created).child(relatedType).nextPersisted();
        contentType = APILocator.getContentTypeAPI(systemUser).find(created.variable());

        dateValue = new GregorianCalendar(2026, Calendar.MARCH, 5).getTime();
        dateTimeValue = new GregorianCalendar(2026, Calendar.MARCH, 5, 14, 30).getTime();
        timeValue = new GregorianCalendar(1970, Calendar.JANUARY, 1, 9, 15).getTime();

        inFolder = fullContentlet().host(site).folder(folder).languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet secondLanguageDraft = ContentletDataGen.checkout(inFolder);
        secondLanguageDraft.setLanguageId(secondLanguage.getId());
        inFolderSecondLanguage = ContentletDataGen.checkin(secondLanguageDraft);

        atSiteRoot = new ContentletDataGen(contentType).host(site)
                .languageId(defaultLanguage.getId()).setProperty("title", "root item")
                .nextPersisted();

        inSystemHost = new ContentletDataGen(contentType).host(systemHost)
                .languageId(defaultLanguage.getId()).setProperty("title", "system host item")
                .nextPersisted();

        request = mock(HttpServletRequest.class);
        when(request.isSecure()).thenReturn(false);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
    }

    private static ContentletDataGen fullContentlet() throws IOException {
        final File binary = File.createTempFile("csv-binary", ".txt");
        Files.writeString(binary.toPath(), "binary content");

        final Map<String, Object> keyValue = new LinkedHashMap<>();
        keyValue.put("k1", "v1");
        keyValue.put("k2", "v2");

        return new ContentletDataGen(contentType)
                .setProperty("title", TITLE_VALUE)
                .setProperty("notes", "Plain notes")
                .setProperty("body", "<p>Body</p>")
                .setProperty("color", "red")
                .setProperty("letters", "a,b")
                .setProperty("options", "x,y")
                .setProperty("choice", "1")
                .setProperty("pubDate", dateValue)
                .setProperty("eventAt", dateTimeValue)
                .setProperty("startsAt", timeValue)
                .setProperty("tags", "alpha,beta")
                .setProperty("meta", keyValue)
                .setProperty("picture", imageAsset.getIdentifier())
                .setProperty("attachment", binary)
                .setProperty("secret", "hidden-value")
                .setProperty("story", STORY_BLOCK_VALUE)
                .setProperty(relationshipVariable(), List.of(relatedOne, relatedTwo))
                .addCategory(childOne)
                .addCategory(childTwo);
    }

    private static String relationshipVariable() {
        return contentType.fields(RelationshipField.class).get(0).variable();
    }

    @AfterClass
    public static void cleanup() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    // ---------------------------------------------------------------------------------------------
    // T004 — characterization of today's Content Search CSV
    // ---------------------------------------------------------------------------------------------

    /**
     * Method to test: {@link EditContentletAction#downloadToExcel(HttpServletResponse, User, List, String)}
     * Given Scenario: The legacy Content Search export of a content type with one field of every
     *                 exported kind, a layout field, and contentlets in a folder (two languages), at
     *                 the site root and in System Host
     * ExpectedResult: The CSV Content Search writes today: the header, the quoting and line endings,
     * and every per-field format. It must pass before and after the writer move (FR-023).
     */
    @Test
    public void test_legacyDownloadToExcel_characterization() throws Exception {
        final String csv = legacyCsv(List.of(inFolder, inFolderSecondLanguage, atSiteRoot, inSystemHost));

        assertTrue("rows end with CRLF", csv.endsWith("\r\n"));
        final List<List<String>> rows = parseCsv(csv);
        assertEquals("header plus four rows", 5, rows.size());

        final List<String> header = rows.get(0);
        assertEquals(expectedHeader(), header);
        assertFalse("layout fields are not exported", header.contains("divider"));

        final Map<String, String> row = asMap(header, rows.get(1));
        assertEquals(inFolder.getIdentifier(), row.get("Identifier"));
        assertEquals(defaultLanguage.getLanguageCode(), row.get("languageCode"));
        assertEquals(defaultLanguage.getCountryCode(), row.get("countryCode"));

        assertEquals("CR stripped, quotes doubled inside a quoted cell", TITLE_EXPECTED, row.get("title"));
        assertTrue("a value with a comma is quoted, with its quotes doubled",
                csv.contains(",\"He said \"\"hi\"\", then\nleft\","));
        assertEquals("Plain notes", row.get("notes"));
        assertEquals("<p>Body</p>", row.get("body"));
        assertEquals("red", row.get("color"));
        assertEquals("a,b", row.get("letters"));
        assertEquals("x,y", row.get("options"));
        assertEquals("1", row.get("choice"));

        assertEquals(new SimpleDateFormat(WebKeys.DateFormats.EXP_IMP_DATE).format(dateValue),
                row.get("pubDate"));
        assertEquals(new SimpleDateFormat(WebKeys.DateFormats.EXP_IMP_DATETIME).format(dateTimeValue),
                row.get("eventAt"));
        assertEquals(new SimpleDateFormat(WebKeys.DateFormats.EXP_IMP_TIME).format(timeValue),
                row.get("startsAt"));

        assertEquals("categories as keys", Set.of(childOne.getKey(), childTwo.getKey()),
                splitToSet(row.get("cats")));
        assertEquals("tags as names", Set.of("alpha", "beta"), splitToSet(row.get("tags")));
        assertEquals("key-value as JSON", Map.of("k1", "v1", "k2", "v2"),
                new ObjectMapper().readValue(row.get("meta"), Map.class));

        final Identifier imageId = APILocator.getIdentifierAPI().find(imageAsset.getIdentifier());
        assertEquals("image as path", imageId.getPath(), row.get("picture"));
        assertTrue("binary as a download link: " + row.get("attachment"),
                row.get("attachment").startsWith("http://" + site.getHostname() + "/"));

        assertEquals("host/folder as the folder inode", folder.getInode(), row.get("location"));
        assertEquals(CONSTANT_VALUE, row.get("fixedText"));
        assertEquals("hidden-value", row.get("secret"));
        assertTrue(row.get("story").contains("Valid block"));
        assertEquals("related identifiers, comma-joined",
                Set.of(relatedOne.getIdentifier(), relatedTwo.getIdentifier()),
                splitToSet(row.get(relationshipVariable())));

        final Map<String, String> secondLanguageRow = asMap(header, rows.get(2));
        assertEquals(inFolder.getIdentifier(), secondLanguageRow.get("Identifier"));
        assertEquals(secondLanguage.getLanguageCode(), secondLanguageRow.get("languageCode"));
        assertEquals(secondLanguage.getCountryCode(), secondLanguageRow.get("countryCode"));

        assertEquals("host/folder as the host id at a site root", site.getIdentifier(),
                asMap(header, rows.get(3)).get("location"));
        assertEquals("host/folder as the System Host id", systemHost.getIdentifier(),
                asMap(header, rows.get(4)).get("location"));
    }

    /**
     * Method to test: {@link EditContentletAction#downloadToExcel(HttpServletResponse, User, List, String)}
     * Given Scenario: The legacy export of a page
     * ExpectedResult: The page's URL column is written as the page path
     */
    @Test
    public void test_legacyDownloadToExcel_pageUrlAsPath() throws Exception {
        final HTMLPageAsset page = new HTMLPageDataGen(folder, new TemplateDataGen().nextPersisted())
                .nextPersisted();
        final ContentType pageType = page.getContentType();

        final StringWriter out = new StringWriter();
        new EditContentletAction().downloadToExcel(responseWriting(out), systemUser,
                legacyList(List.of(page)), pageType.inode());

        final List<List<String>> rows = parseCsv(out.toString());
        final Identifier pageId = APILocator.getIdentifierAPI().find(page.getIdentifier());
        assertEquals(pageId.getPath(), asMap(rows.get(0), rows.get(1)).get("url"));
    }

    // ---------------------------------------------------------------------------------------------
    // T005 — the new writer
    // ---------------------------------------------------------------------------------------------

    /**
     * Method to test: {@link ContentletCsvWriter#write}
     * Given Scenario: The same contentlets as the characterization, written in
     *                 {@link HostFolderMode#IDENTIFIER}
     * ExpectedResult: Byte-for-byte the CSV the legacy export writes (FR-020, FR-023); four rows
     * reported
     */
    @Test
    public void test_write_identifierMode_matchesLegacyByteForByte() throws Exception {
        final List<Contentlet> contentlets =
                List.of(inFolder, inFolderSecondLanguage, atSiteRoot, inSystemHost);
        final String legacy = legacyCsv(contentlets);

        final StringWriter out = new StringWriter();
        final int written = new ContentletCsvWriter().write(contentType, inodes(contentlets),
                systemUser, request, HostFolderMode.IDENTIFIER, out);

        assertEquals(4, written);
        assertEquals(legacy, out.toString());
    }

    /**
     * Method to test: {@link ContentletCsvWriter#write}
     * Given Scenario: The same contentlets written in {@link HostFolderMode#LOCATION}
     * ExpectedResult: The host/folder column is {@code //host/path/} in a folder, the bare host name
     * at a site root and {@code SYSTEM_HOST} in System Host (FR-022); every other column equals the
     * {@link HostFolderMode#IDENTIFIER} output
     */
    @Test
    public void test_write_locationMode_writesReadableLocations() throws Exception {
        final List<Contentlet> contentlets = List.of(inFolder, atSiteRoot, inSystemHost);

        final StringWriter identifierOut = new StringWriter();
        new ContentletCsvWriter().write(contentType, inodes(contentlets), systemUser, request,
                HostFolderMode.IDENTIFIER, identifierOut);
        final StringWriter locationOut = new StringWriter();
        new ContentletCsvWriter().write(contentType, inodes(contentlets), systemUser, request,
                HostFolderMode.LOCATION, locationOut);

        final List<List<String>> identifierRows = parseCsv(identifierOut.toString());
        final List<List<String>> locationRows = parseCsv(locationOut.toString());
        final List<String> header = locationRows.get(0);
        assertEquals(identifierRows.get(0), header);

        assertEquals("//" + site.getHostname() + folder.getPath(),
                asMap(header, locationRows.get(1)).get("location"));
        assertEquals(site.getHostname(), asMap(header, locationRows.get(2)).get("location"));
        assertEquals("SYSTEM_HOST", asMap(header, locationRows.get(3)).get("location"));

        for (int i = 1; i < locationRows.size(); i++) {
            final Map<String, String> location = asMap(header, locationRows.get(i));
            final Map<String, String> identifier = asMap(header, identifierRows.get(i));
            location.remove("location");
            identifier.remove("location");
            assertEquals("row " + i + " differs only in the host/folder column", identifier, location);
        }
    }

    /**
     * Method to test: {@link ContentletCsvWriter#write}
     * Given Scenario: A limited user who can read {@link #inFolder} through an individual
     *                 permission but not {@link #atSiteRoot}
     * ExpectedResult: The unreadable row is skipped and not counted; the count equals the data rows
     * written (FR-026a)
     */
    @Test
    public void test_write_skipsRowsTheUserCannotRead() throws Exception {
        // A back-end user, as every Drive user is: dotCMS denies a front-end user READ on any
        // contentlet that is not live, and these fixtures are working versions.
        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen()
                .roles(role, APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        permissionAPI.save(new Permission(inFolder.getPermissionId(), role.getId(),
                PermissionAPI.PERMISSION_READ), inFolder, systemUser, false);

        final StringWriter out = new StringWriter();
        final int written = new ContentletCsvWriter().write(contentType,
                inodes(List.of(inFolder, atSiteRoot)), limitedUser, request,
                HostFolderMode.LOCATION, out);

        assertEquals(1, written);
        final List<List<String>> rows = parseCsv(out.toString());
        assertEquals(2, rows.size());
        assertEquals(inFolder.getIdentifier(), rows.get(1).get(0));
    }

    /**
     * Method to test: {@link ContentletCsvWriter#write}
     * Given Scenario: An inode that does not exist, between two that do
     * ExpectedResult: It is skipped and not counted
     */
    @Test
    public void test_write_skipsInodesThatNoLongerExist() throws Exception {
        final StringWriter out = new StringWriter();
        final int written = new ContentletCsvWriter().write(contentType,
                List.of(inFolder.getInode(), "00000000-0000-0000-0000-000000000000",
                        atSiteRoot.getInode()),
                systemUser, request, HostFolderMode.LOCATION, out);

        assertEquals(2, written);
        assertEquals(3, parseCsv(out.toString()).size());
    }

    /**
     * Method to test: {@link ContentletCsvWriter#write}
     * Given Scenario: Reading one field's value throws
     * ExpectedResult: That cell is empty and the next fields are still written, as legacy does
     */
    @Test
    public void test_write_fieldFailureWritesAnEmptyCell() throws Exception {
        final ContentletAPI failing = spy(APILocator.getContentletAPI());
        doThrow(new RuntimeException("cannot read notes")).when(failing)
                .getFieldValue(any(Contentlet.class), argThat((Field field) -> field != null
                        && "notes".equals(field.variable())));
        final ContentletCsvWriter writer = new ContentletCsvWriter(failing,
                APILocator.getCategoryAPI(), APILocator.getTagAPI(), APILocator.getIdentifierAPI(),
                APILocator.getHostAPI(), APILocator.getFolderAPI());

        final StringWriter out = new StringWriter();
        final int written = writer.write(contentType, List.of(inFolder.getInode()), systemUser,
                request, HostFolderMode.LOCATION, out);

        assertEquals(1, written);
        final List<List<String>> rows = parseCsv(out.toString());
        final Map<String, String> row = asMap(rows.get(0), rows.get(1));
        assertEquals("", row.get("notes"));
        assertEquals("<p>Body</p>", row.get("body"));
    }

    /**
     * Method to test: {@link ContentletCsvWriter#write}
     * Given Scenario: The output fails while writing
     * ExpectedResult: The failure propagates, so the caller never sends a partial file (FR-020a)
     */
    @Test
    public void test_write_outputFailurePropagates() {
        final Writer broken = new Writer() {
            @Override
            public void write(final char[] buffer, final int offset, final int length)
                    throws IOException {
                throw new IOException("disk full");
            }

            @Override
            public void flush() throws IOException {
                throw new IOException("disk full");
            }

            @Override
            public void close() {
            }
        };

        assertThrows(IOException.class, () -> new ContentletCsvWriter().write(contentType,
                List.of(inFolder.getInode()), systemUser, request, HostFolderMode.LOCATION, broken));
    }

    /**
     * Method to test: {@link ContentletCsvWriter#write}
     * Given Scenario: No inodes
     * ExpectedResult: Only the header row is written, and zero rows are reported
     */
    @Test
    public void test_write_noInodesWritesOnlyTheHeader() throws Exception {
        final StringWriter out = new StringWriter();
        final int written = new ContentletCsvWriter().write(contentType, List.of(), systemUser,
                request, HostFolderMode.LOCATION, out);

        assertEquals(0, written);
        final List<List<String>> rows = parseCsv(out.toString());
        assertEquals(1, rows.size());
        assertEquals(expectedHeader(), rows.get(0));
    }

    /**
     * Method to test: {@link ContentletCsvWriter#write}
     * Given Scenario: A content type whose only field is a layout field
     * ExpectedResult: The header is {@code Identifier,languageCode,countryCode}
     */
    @Test
    public void test_write_typeWithNoExportableFields() throws Exception {
        final ContentType layoutOnly = new ContentTypeDataGen().host(site)
                .field(new FieldDataGen().defaultValue(null).type(LineDividerField.class).name("line")
                        .velocityVarName("line").next())
                .nextPersisted();

        final StringWriter out = new StringWriter();
        new ContentletCsvWriter().write(layoutOnly, List.of(), systemUser, request,
                HostFolderMode.LOCATION, out);

        assertEquals("Identifier,languageCode,countryCode\r\n", out.toString());
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    private static List<String> expectedHeader() {
        final List<String> header = new ArrayList<>(
                List.of("Identifier", "languageCode", "countryCode"));
        // Layout fields are the @Unexportable ones; the content type API adds rows and columns on save
        contentType.fields().stream()
                .filter(field -> !(field instanceof LineDividerField || field instanceof RowField
                        || field instanceof ColumnField || field instanceof TabDividerField))
                .forEach(field -> header.add(field.variable()));
        return header;
    }

    private static String legacyCsv(final List<Contentlet> contentlets) throws Exception {
        final StringWriter out = new StringWriter();
        new EditContentletAction().downloadToExcel(responseWriting(out), systemUser,
                legacyList(contentlets), contentType.inode());
        return out.toString();
    }

    private static HttpServletResponse responseWriting(final StringWriter out) throws IOException {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(out));
        return response;
    }

    /**
     * The list in the shape Content Search hands to {@code downloadToExcel}: two leading entries it
     * skips, then one map per contentlet holding its inode.
     */
    private static List<Map<String, String>> legacyList(final List<? extends Contentlet> contentlets) {
        final List<Map<String, String>> list = new ArrayList<>();
        list.add(new HashMap<>());
        list.add(new HashMap<>());
        for (final Contentlet contentlet : contentlets) {
            final Map<String, String> entry = new HashMap<>();
            entry.put("inode", contentlet.getInode());
            list.add(entry);
        }
        return list;
    }

    private static List<String> inodes(final List<Contentlet> contentlets) {
        return contentlets.stream().map(Contentlet::getInode).collect(Collectors.toList());
    }

    private static Map<String, String> asMap(final List<String> header, final List<String> row) {
        assertEquals("row has one cell per column", header.size(), row.size());
        final Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < header.size(); i++) {
            map.put(header.get(i), row.get(i));
        }
        return map;
    }

    private static Set<String> splitToSet(final String value) {
        return new HashSet<>(Arrays.asList(value.split(",")));
    }

    /**
     * Parses the legacy CSV format: rows end with CRLF, a cell is quoted when it holds a comma or a
     * newline, and a quote inside a quoted cell is doubled.
     */
    static List<List<String>> parseCsv(final String csv) {
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
}
