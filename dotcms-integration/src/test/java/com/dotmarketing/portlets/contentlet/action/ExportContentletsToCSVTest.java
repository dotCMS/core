package com.dotmarketing.portlets.contentlet.action;

import static com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.LineDividerField;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.ImportUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Integration tests for the content CSV export
 * ({@link EditContentletAction#downloadToExcel(HttpServletResponse, User, List, String)}),
 * covering how File fields are exported (issue #33944) and that the exported CSV can be
 * imported back with {@link ImportUtil}.
 */
public class ExportContentletsToCSVTest {

    private static final String TITLE_VAR = "title";
    private static final String FILE_VAR = "file";
    private static final String IMAGE_VAR = "image";
    private static final String BINARY_VAR = "binary";

    private static User user;
    private static Host site;
    private static Language language;
    private static Folder folder;

    private static final List<ContentType> createdContentTypes = new ArrayList<>();
    private static final List<Contentlet> createdFileAssets = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        user = APILocator.systemUser();
        site = APILocator.getHostAPI().findDefaultHost(user, false);
        language = APILocator.getLanguageAPI().getDefaultLanguage();
        folder = new FolderDataGen().site(site).nextPersisted();
    }

    @AfterClass
    public static void cleanUp() {
        for (final ContentType contentType : createdContentTypes) {
            try {
                APILocator.getContentTypeAPI(user).delete(contentType);
            } catch (final Exception e) {
                Logger.error(ExportContentletsToCSVTest.class,
                        "Unable to delete content type " + contentType.variable(), e);
            }
        }
        for (final Contentlet fileAsset : createdFileAssets) {
            try {
                APILocator.getContentletAPI().destroy(fileAsset, user, false);
            } catch (final Exception e) {
                Logger.debug(ExportContentletsToCSVTest.class,
                        "File asset already removed: " + fileAsset.getIdentifier());
            }
        }
        try {
            FolderDataGen.remove(folder);
        } catch (final Exception e) {
            Logger.error(ExportContentletsToCSVTest.class, "Unable to delete test folder", e);
        }
    }

    /**
     * Method to test: {@link EditContentletAction#downloadToExcel(HttpServletResponse, User, List, String)}
     * Given Scenario: A content type has a Text field and a File field, and two contentlets
     * each reference a different file asset
     * Expected Result: The CSV header contains a column named after the File field variable
     * and each row holds the dotCMS path of the file its contentlet references
     */
    @Test
    public void export_fileField_writesHeaderAndFilePath() throws Exception {
        final ContentType type = createTypeWithFileField(false);
        final Contentlet first = createFileAsset("first");
        final Contentlet second = createFileAsset("second");
        final Contentlet contentA = createContentlet(type, "contentA", first.getIdentifier());
        final Contentlet contentB = createContentlet(type, "contentB", second.getIdentifier());

        final Csv csv = export(type, contentA, contentB);

        assertTrue("File field column is missing from the header: " + csv.headers,
                csv.headers.contains(FILE_VAR));
        assertEquals(pathOf(first), csv.rowByTitle("contentA").get(FILE_VAR));
        assertEquals(pathOf(second), csv.rowByTitle("contentB").get(FILE_VAR));
    }

    /**
     * Method to test: {@link EditContentletAction#downloadToExcel(HttpServletResponse, User, List, String)}
     * Given Scenario: A content type has a non-required File field. One contentlet has no file,
     * one references a file asset that was deleted afterwards, and one references a valid file
     * Expected Result: The empty and dangling values are exported as empty cells, and the row
     * after them is still exported with its file path
     */
    @Test
    public void export_fileFieldEmptyOrDangling_writesEmptyCellAndKeepsExporting() throws Exception {
        final ContentType type = createTypeWithFileField(false);
        final Contentlet valid = createFileAsset("valid");
        final Contentlet doomed = createFileAsset("doomed");
        final Contentlet noFile = createContentlet(type, "noFile", null);
        final Contentlet dangling = createContentlet(type, "dangling", doomed.getIdentifier());
        final Contentlet withFile = createContentlet(type, "withFile", valid.getIdentifier());
        APILocator.getContentletAPI().destroy(doomed, user, false);

        final Csv csv = export(type, noFile, dangling, withFile);

        assertTrue("File field column is missing from the header: " + csv.headers,
                csv.headers.contains(FILE_VAR));
        assertEquals("", csv.rowByTitle("noFile").get(FILE_VAR));
        assertEquals("", csv.rowByTitle("dangling").get(FILE_VAR));
        assertEquals("The export stopped after the dangling file", pathOf(valid),
                csv.rowByTitle("withFile").get(FILE_VAR));
    }

    /**
     * Method to test: {@link EditContentletAction#downloadToExcel(HttpServletResponse, User, List, String)}
     * and {@link ImportUtil#importFile}
     * Given Scenario: Contentlets of a type with a File field are exported, and the CSV is
     * imported into a second content type whose File field is required
     * Expected Result: The import reports no errors (in particular, not "required and was not
     * found in the header") and the imported contentlets reference the same files
     */
    @Test
    public void exportThenImport_requiredFileField_roundTrips() throws Exception {
        final ContentType source = createTypeWithFileField(false);
        final ContentType target = createTypeWithFileField(true);
        final Contentlet first = createFileAsset("roundtripA");
        final Contentlet second = createFileAsset("roundtripB");
        final Contentlet contentA = createContentlet(source, "roundA", first.getIdentifier());
        final Contentlet contentB = createContentlet(source, "roundB", second.getIdentifier());

        final Csv exported = export(source, contentA, contentB);
        // The Identifier column points at the source contentlets, so it is left out to create new ones
        final String importCsv = exported.withoutColumn("Identifier");

        final CsvReader reader = new CsvReader(new StringReader(importCsv));
        reader.setSafetySwitch(false);
        reader.readHeaders();
        final Map<String, List<String>> results = ImportUtil.importFile(0L, site.getIdentifier(),
                target.inode(), new String[]{}, false, false, user, language.getId(),
                reader.getHeaders(), reader, -1, -1, new StringReader(importCsv),
                WORKFLOW_SAVE_ACTION_ID, getHttpRequest());

        assertEquals("Import errors: " + results.get("errors"), 0, results.get("errors").size());
        final Map<String, String> fileByTitle = new HashMap<>();
        for (final Contentlet imported : APILocator.getContentletAPI()
                .findByStructure(target.inode(), user, false, 0, 0)) {
            fileByTitle.put(imported.getStringProperty(TITLE_VAR),
                    imported.getStringProperty(FILE_VAR));
        }
        assertEquals(first.getIdentifier(), fileByTitle.get("roundA"));
        assertEquals(second.getIdentifier(), fileByTitle.get("roundB"));
    }

    /**
     * Method to test: {@link EditContentletAction#downloadToExcel(HttpServletResponse, User, List, String)}
     * Given Scenario: A content type has Text, Image and Binary fields plus layout fields (Row,
     * Column, Line Divider and Tab Divider)
     * Expected Result: Text and Image values are exported as before, the Binary column is
     * present, and none of the layout fields appear in the header
     */
    @Test
    public void export_otherFieldTypes_unchanged() throws Exception {
        final List<Field> fields = new ArrayList<>(Arrays.asList(
                FieldBuilder.builder(RowField.class).name("row").variable("row").build(),
                FieldBuilder.builder(ColumnField.class).name("column").variable("column").build(),
                textField(),
                FieldBuilder.builder(ImageField.class).name(IMAGE_VAR).variable(IMAGE_VAR).build(),
                FieldBuilder.builder(BinaryField.class).name(BINARY_VAR).variable(BINARY_VAR).build(),
                FieldBuilder.builder(LineDividerField.class).name("line").variable("line").build(),
                FieldBuilder.builder(TabDividerField.class).name("tab").variable("tab").build()));
        final ContentType type = persistType(fields);
        final Contentlet image = createFileAsset("image");
        final Contentlet content = new ContentletDataGen(type.id())
                .languageId(language.getId()).host(site)
                .setProperty(TITLE_VAR, "regression")
                .setProperty(IMAGE_VAR, image.getIdentifier())
                .nextPersisted();

        final Csv csv = export(type, content);

        assertEquals("regression", csv.rowByTitle("regression").get(TITLE_VAR));
        assertEquals(pathOf(image), csv.rowByTitle("regression").get(IMAGE_VAR));
        assertTrue("Binary column is missing: " + csv.headers, csv.headers.contains(BINARY_VAR));
        for (final String layout : Arrays.asList("row", "column", "line", "tab")) {
            assertFalse("Layout field '" + layout + "' must not be exported: " + csv.headers,
                    csv.headers.contains(layout));
        }
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    private static Field textField() {
        return FieldBuilder.builder(TextField.class).name(TITLE_VAR).variable(TITLE_VAR)
                .required(true).indexed(true).listed(true).searchable(true).build();
    }

    private static ContentType createTypeWithFileField(final boolean fileRequired) {
        return persistType(Arrays.asList(
                textField(),
                FieldBuilder.builder(FileField.class).name(FILE_VAR).variable(FILE_VAR)
                        .required(fileRequired).build()));
    }

    private static ContentType persistType(final List<Field> fields) {
        final ContentType type = new ContentTypeDataGen()
                .name("ExportCsv" + System.nanoTime())
                .host(site)
                .fields(fields)
                .nextPersisted();
        createdContentTypes.add(type);
        return type;
    }

    private static Contentlet createFileAsset(final String name) throws Exception {
        final Contentlet fileAsset = FileAssetDataGen.createFileAsset(folder,
                name + System.nanoTime(), ".txt");
        // The import only resolves a path to a live (published) file
        APILocator.getContentletAPI().publish(fileAsset, user, false);
        createdFileAssets.add(fileAsset);
        return fileAsset;
    }

    private static Contentlet createContentlet(final ContentType type, final String title,
            final String fileIdentifier) {
        final ContentletDataGen gen = new ContentletDataGen(type.id())
                .languageId(language.getId()).host(site).setProperty(TITLE_VAR, title);
        if (fileIdentifier != null) {
            gen.setProperty(FILE_VAR, fileIdentifier);
        }
        return gen.nextPersisted();
    }

    private static String pathOf(final Contentlet fileAsset) throws Exception {
        return APILocator.getIdentifierAPI().find(fileAsset.getIdentifier()).getPath();
    }

    /** Runs the export for the given contentlets and parses the resulting CSV. */
    private static Csv export(final ContentType type, final Contentlet... contentlets)
            throws Exception {
        // downloadToExcel ignores the first two entries (search metadata) of the list
        final List<Map<String, String>> toExport = new ArrayList<>();
        toExport.add(new HashMap<>());
        toExport.add(new HashMap<>());
        for (final Contentlet contentlet : contentlets) {
            final HashMap<String, String> entry = new HashMap<>();
            entry.put("inode", contentlet.getInode());
            toExport.add(entry);
        }

        final StringWriter out = new StringWriter();
        final PrintWriter writer = new PrintWriter(out);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(response.getWriter()).thenReturn(writer);

        new EditContentletAction().downloadToExcel(response, user, toExport, type.id());
        writer.flush();

        return Csv.parse(out.toString());
    }

    private static HttpServletRequest getHttpRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request())
                        .request()).request());
        request.setHeader("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString("admin@dotcms.com:admin".getBytes()));
        request.setHeader("Origin", "localhost");
        request.setAttribute(WebKeys.USER, user);
        request.setAttribute(WebKeys.USER_ID, user.getUserId());
        return request;
    }

    /** A parsed CSV: the header names and one map per row. */
    private static final class Csv {

        final List<String> headers;
        final List<Map<String, String>> rows;

        private Csv(final List<String> headers, final List<Map<String, String>> rows) {
            this.headers = headers;
            this.rows = rows;
        }

        static Csv parse(final String text) throws Exception {
            final CsvReader reader = new CsvReader(new StringReader(text));
            reader.setSafetySwitch(false);
            reader.readHeaders();
            final List<String> headers = Arrays.asList(reader.getHeaders());
            final List<Map<String, String>> rows = new ArrayList<>();
            while (reader.readRecord()) {
                final Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    row.put(headers.get(i), reader.get(i));
                }
                rows.add(row);
            }
            return new Csv(headers, rows);
        }

        Map<String, String> rowByTitle(final String title) {
            return rows.stream().filter(r -> title.equals(r.get(TITLE_VAR))).findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "No exported row with title '" + title + "': " + rows));
        }

        /** Serializes this CSV again without the given column, quoting every value. */
        String withoutColumn(final String column) {
            final List<String> kept = new ArrayList<>(headers);
            kept.remove(column);
            final StringBuilder sb = new StringBuilder(String.join(",", kept)).append("\r\n");
            for (final Map<String, String> row : rows) {
                final List<String> cells = new ArrayList<>();
                for (final String header : kept) {
                    final String value = row.get(header);
                    assertNotNull(value);
                    cells.add("\"" + value.replace("\"", "\"\"") + "\"");
                }
                sb.append(String.join(",", cells)).append("\r\n");
            }
            return sb.toString();
        }
    }
}
