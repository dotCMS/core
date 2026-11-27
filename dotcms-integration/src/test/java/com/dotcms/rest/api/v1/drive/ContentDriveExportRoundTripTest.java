package com.dotcms.rest.api.v1.drive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.rest.api.v1.drive.export.DriveExportRequestForm;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.action.EditContentletAction;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.util.ImportUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests proving a Content Drive filter-mode CSV can be imported back (User Story 6;
 * FR-021, FR-022, SC-003, SC-004): the import updates the same contentlets in the same place, and
 * the file equals the Content Search CSV except for the host/folder column.
 */
public class ContentDriveExportRoundTripTest extends IntegrationTestBase {

    private static final String LOCATION = "location";

    private static User systemUser;
    /** Calls the endpoint: a REST request never runs as the system user. */
    private static User adminUser;
    private static Host site;
    private static Folder folder;
    private static ContentType type;

    private static Contentlet inFolder;
    private static Contentlet atSiteRoot;
    private static Contentlet inSystemHost;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        adminUser = new UserDataGen().roles(APILocator.getRoleAPI().loadCMSAdminRole(),
                APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();

        final String unique = String.valueOf(System.currentTimeMillis());
        site = new SiteDataGen().name("drive-roundtrip-" + unique + ".local").nextPersisted();
        folder = new FolderDataGen().site(site).name("rt").nextPersisted();

        // FieldDataGen gives every field a text default value; every field here clears it.
        type = new ContentTypeDataGen().host(site)
                .workflowId(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                .name("RoundTrip" + unique).velocityVarName("driveRoundTrip" + unique)
                .field(new FieldDataGen().defaultValue(null).type(TextField.class).name("title")
                        .velocityVarName("title").next())
                .field(new FieldDataGen().defaultValue(null).type(HostFolderField.class)
                        .name(LOCATION).velocityVarName(LOCATION).next())
                .nextPersisted();

        inFolder = new ContentletDataGen(type).host(site).folder(folder)
                .setProperty("title", "rt-folder").nextPersisted();
        atSiteRoot = new ContentletDataGen(type).host(site)
                .setProperty("title", "rt-root").nextPersisted();
        inSystemHost = new ContentletDataGen(type).host(APILocator.systemHost())
                .setProperty("title", "rt-system").nextPersisted();
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    /** The Drive's filter-mode export of the type: All Site Content, System Host included. */
    private static String driveCsv() throws Exception {
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/drive/export").request())
                .request();
        request.setAttribute(WebKeys.USER, adminUser);
        final DriveRequestForm query = DriveRequestForm.builder()
                .assetPath("//" + site.getHostname() + "/")
                .browseScope(BrowseScope.ALL)
                .includeSystemHost(true)
                .language(List.of(String.valueOf(
                        APILocator.getLanguageAPI().getDefaultLanguage().getId())))
                .contentTypes(List.of(type.variable()))
                .build();

        final Response response = new ContentDriveResource().export(request,
                mock(HttpServletResponse.class), DriveExportRequestForm.builder().query(query).build());
        assertEquals(200, response.getStatus());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((StreamingOutput) response.getEntity()).write(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    /** Content Search's export of the same contentlets. */
    private static String contentSearchCsv(final List<Contentlet> contentlets) throws Exception {
        final StringWriter out = new StringWriter();
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(out));
        // The list in the shape Content Search hands over: two leading entries it skips
        final List<Map<String, String>> list = new ArrayList<>();
        list.add(new HashMap<>());
        list.add(new HashMap<>());
        for (final Contentlet contentlet : contentlets) {
            final Map<String, String> entry = new HashMap<>();
            entry.put("inode", contentlet.getInode());
            list.add(entry);
        }
        new EditContentletAction().downloadToExcel(response, systemUser, list, type.inode());
        return out.toString();
    }

    /** Every data row of a CSV as column name to value, keyed by identifier and language. */
    private static Map<String, Map<String, String>> rowsByKey(final String csv) throws Exception {
        final CsvReader reader = new CsvReader(new StringReader(csv));
        reader.setSafetySwitch(false);
        reader.readHeaders();
        final String[] headers = reader.getHeaders();
        final Map<String, Map<String, String>> rows = new LinkedHashMap<>();
        while (reader.readRecord()) {
            final Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < headers.length; i++) {
                row.put(headers[i], reader.get(i));
            }
            rows.put(row.get("Identifier") + "|" + row.get("languageCode") + "|"
                    + row.get("countryCode"), row);
        }
        reader.close();
        return rows;
    }

    /**
     * Imports a CSV the way the Content Search import does with a multilingual file and no key
     * fields: the identifier column picks the contentlet to update, the language and country
     * columns its language.
     */
    private static Map<String, List<String>> importCsv(final String csv) throws Exception {
        final StringReader reader = new StringReader(csv);
        final CsvReader csvReader = new CsvReader(reader);
        csvReader.setSafetySwitch(false);
        csvReader.readHeaders();
        final String[] headers = csvReader.getHeaders();
        int languageColumn = -1;
        int countryColumn = -1;
        for (int column = 0; column < headers.length; column++) {
            if ("languageCode".equals(headers[column])) {
                languageColumn = column;
            }
            if ("countryCode".equals(headers[column])) {
                countryColumn = column;
            }
        }
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()).request();
        request.setAttribute(WebKeys.USER, systemUser);

        return ImportUtil.importFile(0L, site.getIdentifier(), type.inode(), new String[]{},
                false, true, systemUser, -1, headers, csvReader, languageColumn, countryColumn,
                reader, SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, request);
    }

    /**
     * Rewrites a CSV with the title column changed as given, leaving every other cell as it was.
     * Cells are written back in the format the export uses: quoted only when they hold a comma or
     * a line feed, rows ending with CRLF.
     */
    private static String withTitlesChanged(final String csv, final Map<String, String> newTitles)
            throws Exception {
        final CsvReader reader = new CsvReader(new StringReader(csv));
        reader.setSafetySwitch(false);
        reader.readHeaders();
        final String[] headers = reader.getHeaders();
        final StringBuilder out = new StringBuilder(String.join(",", headers)).append("\r\n");
        while (reader.readRecord()) {
            final List<String> cells = new ArrayList<>();
            for (int i = 0; i < headers.length; i++) {
                String value = reader.get(i);
                if ("title".equals(headers[i])) {
                    value = newTitles.getOrDefault(value, value);
                }
                cells.add(value.contains(",") || value.contains("\n")
                        ? "\"" + value.replace("\"", "\"\"") + "\"" : value);
            }
            out.append(String.join(",", cells)).append("\r\n");
        }
        reader.close();
        return out.toString();
    }

    private static Contentlet working(final Contentlet contentlet) throws Exception {
        return APILocator.getContentletAPI().findContentletByIdentifier(
                contentlet.getIdentifier(), false, contentlet.getLanguageId(), systemUser, false);
    }

    // ---------------------------------------------------------------------------------------------
    // tests
    // ---------------------------------------------------------------------------------------------

    /**
     * Method to test: {@link ContentDriveResource#export} then {@link ImportUtil#importFile}
     * Given Scenario: (a) Contentlets in folder {@code /rt/}, at the site root and in System Host,
     *                 exported in filter mode, one text value changed per row, and imported back
     *                 with the identifier as the key
     * ExpectedResult: No row errors, every value updated, and every contentlet still in its host
     * and folder (SC-003, SC-004)
     */
    @Test
    public void test_exportedCsv_importsBackInPlace() throws Exception {
        final String exported = driveCsv();
        final Map<String, String> newTitles = Map.of("rt-folder", "rt-folder edited",
                "rt-root", "rt-root edited", "rt-system", "rt-system edited");
        final String edited = withTitlesChanged(exported, newTitles);
        assertEquals("fixture: the three rows are exported and edited", 3,
                rowsByKey(edited).values().stream()
                        .filter(row -> newTitles.containsValue(row.get("title"))).count());

        final Map<String, List<String>> results = importCsv(edited);

        assertEquals("row errors: " + results.get("errors"), 0, results.get("errors").size());

        final Map<Contentlet, String> expectedTitles = new LinkedHashMap<>();
        expectedTitles.put(inFolder, "rt-folder edited");
        expectedTitles.put(atSiteRoot, "rt-root edited");
        expectedTitles.put(inSystemHost, "rt-system edited");
        for (final Map.Entry<Contentlet, String> entry : expectedTitles.entrySet()) {
            final Contentlet before = entry.getKey();
            final Contentlet after = working(before);
            assertNotNull(after);
            assertEquals(entry.getValue(), after.getStringProperty("title"));
            assertEquals(entry.getValue() + ": same host", before.getHost(), after.getHost());
            assertEquals(entry.getValue() + ": same folder", before.getFolder(), after.getFolder());
        }
        assertEquals("the folder row stays in /rt/", folder.getInode(), working(inFolder).getFolder());
        assertEquals("the root row stays on the site", site.getIdentifier(),
                working(atSiteRoot).getHost());
        assertEquals("the root row stays at the root", FolderAPI.SYSTEM_FOLDER,
                working(atSiteRoot).getFolder());
        assertEquals("the System Host row stays in System Host", Host.SYSTEM_HOST,
                working(inSystemHost).getHost());
    }

    /**
     * Method to test: {@link ContentDriveResource#export} against
     *                 {@link EditContentletAction#downloadToExcel}
     * Given Scenario: (b) The same contentlets exported by the Drive and by Content Search
     * ExpectedResult: The same header, the same rows, and every column equal except host/folder
     * (FR-021)
     */
    @Test
    public void test_driveCsv_equalsContentSearchExceptHostFolder() throws Exception {
        final Map<String, Map<String, String>> drive = rowsByKey(driveCsv());
        final Map<String, Map<String, String>> contentSearch = rowsByKey(
                contentSearchCsv(List.of(inFolder, atSiteRoot, inSystemHost)));

        assertEquals("the same rows", contentSearch.keySet(), drive.keySet());
        for (final String key : contentSearch.keySet()) {
            final Map<String, String> driveRow = new LinkedHashMap<>(drive.get(key));
            final Map<String, String> contentSearchRow = new LinkedHashMap<>(contentSearch.get(key));
            assertEquals("the same columns in the same order",
                    new ArrayList<>(contentSearchRow.keySet()), new ArrayList<>(driveRow.keySet()));
            driveRow.remove(LOCATION);
            contentSearchRow.remove(LOCATION);
            assertEquals(key, contentSearchRow, driveRow);
        }
    }
}
