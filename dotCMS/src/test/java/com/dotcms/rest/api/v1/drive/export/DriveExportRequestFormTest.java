package com.dotcms.rest.api.v1.drive.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.drive.DriveRequestForm;
import com.dotcms.rest.api.v1.drive.export.AbstractDriveExportRequestForm.Mode;
import com.dotcms.rest.exception.BadRequestException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests for {@link DriveExportRequestForm}: which export a request asks for, and the checks
 * made before anything is resolved (contract § 400, data-model "Request").
 * <p>
 * Every query pins {@code language}: its declared default asks the language API for the default
 * language, which reaches the database.
 */
public class DriveExportRequestFormTest {

    private static final String NO_MODE =
            "Select items or filter the listing to one content type to export.";
    private static final String TOO_MANY = "You can export at most 60 items at once.";
    private static final String BAD_CONTENTLET = "Invalid contentlet in the selection.";

    /**
     * The message the client receives. {@code getMessage()} is WebApplicationException's
     * ("HTTP 400 Bad Request"); the text travels in the {@code error-message} header.
     */
    private static String messageOf(final BadRequestException exception) {
        return exception.getResponse().getHeaderString("error-message");
    }

    private static DriveRequestForm query() {
        return DriveRequestForm.builder().assetPath("//demo.dotcms.com/")
                .language(List.of("1")).contentTypes(List.of("Blog")).build();
    }

    private static ExportContentletRefForm ref(final String identifier, final long languageId) {
        return ExportContentletRefForm.builder().identifier(identifier).languageId(languageId).build();
    }

    /**
     * Method to test: {@link DriveExportRequestForm#validate()}
     * Given Scenario: A request with a query and no selection
     * ExpectedResult: Filter mode
     */
    @Test
    public void test_validate_queryOnly_isFilterMode() {
        final DriveExportRequestForm form = DriveExportRequestForm.builder().query(query()).build();

        assertEquals(Mode.FILTER, form.validate());
    }

    /**
     * Method to test: {@link DriveExportRequestForm#validate()}
     * Given Scenario: A request with a selection holding one folder and also a query (FR-003)
     * ExpectedResult: Selection mode: the selection wins over the query
     */
    @Test
    public void test_validate_selectionAndQuery_selectionWins() {
        final DriveExportRequestForm form = DriveExportRequestForm.builder()
                .siteId("site-id")
                .selection(ExportSelectionForm.builder().addFolders("//demo.dotcms.com/blog/").build())
                .query(query())
                .build();

        assertEquals(Mode.SELECTION, form.validate());
    }

    /**
     * Method to test: {@link DriveExportRequestForm#validate()}
     * Given Scenario: A selection holding only a contentlet
     * ExpectedResult: Selection mode
     */
    @Test
    public void test_validate_contentletOnlySelection_isSelectionMode() {
        final DriveExportRequestForm form = DriveExportRequestForm.builder()
                .siteId("site-id")
                .selection(ExportSelectionForm.builder().addContentlets(ref("abc", 1)).build())
                .build();

        assertEquals(Mode.SELECTION, form.validate());
    }

    /**
     * Method to test: {@link DriveExportRequestForm#validate()}
     * Given Scenario: An empty selection and a query
     * ExpectedResult: Filter mode: an empty selection does not count as a selection
     */
    @Test
    public void test_validate_emptySelectionWithQuery_isFilterMode() {
        final DriveExportRequestForm form = DriveExportRequestForm.builder()
                .selection(ExportSelectionForm.builder().build())
                .query(query())
                .build();

        assertEquals(Mode.FILTER, form.validate());
    }

    /**
     * Method to test: {@link DriveExportRequestForm#validate()}
     * Given Scenario: Neither a selection nor a query; and an empty selection with no query
     * ExpectedResult: 400 with the "select or filter" message in both cases
     */
    @Test
    public void test_validate_nothingToExport_isRejected() {
        final BadRequestException empty = assertThrows(BadRequestException.class,
                () -> DriveExportRequestForm.builder().build().validate());
        assertEquals(NO_MODE, messageOf(empty));

        final BadRequestException emptySelection = assertThrows(BadRequestException.class,
                () -> DriveExportRequestForm.builder()
                        .selection(ExportSelectionForm.builder().build()).build().validate());
        assertEquals(NO_MODE, messageOf(emptySelection));
    }

    /**
     * Method to test: {@link DriveExportRequestForm#validate()}
     * Given Scenario: 60 entries (folders and contentlets together), then 61
     * ExpectedResult: 60 is accepted; 61 gives 400 with the 60-item message (FR-004a)
     */
    @Test
    public void test_validate_sixtyItemsLimit() {
        assertEquals(60, AbstractDriveExportRequestForm.MAX_SELECTION_ITEMS);

        assertEquals(Mode.SELECTION, selectionOf(30, 30).validate());

        final BadRequestException tooMany = assertThrows(BadRequestException.class,
                () -> selectionOf(30, 31).validate());
        assertEquals(TOO_MANY, messageOf(tooMany));
    }

    private static DriveExportRequestForm selectionOf(final int folders, final int contentlets) {
        final List<String> folderPaths = new ArrayList<>();
        for (int i = 0; i < folders; i++) {
            folderPaths.add("//demo.dotcms.com/f" + i + "/");
        }
        final List<ExportContentletRefForm> refs = new ArrayList<>();
        for (int i = 0; i < contentlets; i++) {
            refs.add(ref("id-" + i, 1));
        }
        return DriveExportRequestForm.builder().siteId("site-id")
                .selection(ExportSelectionForm.builder().folders(folderPaths).contentlets(refs).build())
                .build();
    }

    /**
     * Method to test: {@link DriveExportRequestForm#validate()}
     * Given Scenario: A contentlet entry with a blank identifier, with no identifier, and with a
     *                 language of 0 or below
     * ExpectedResult: 400 with the same "invalid contentlet" message each time
     */
    @Test
    public void test_validate_invalidContentletRef_isRejected() {
        for (final ExportContentletRefForm bad : List.of(ref(" ", 1),
                ExportContentletRefForm.builder().languageId(1).build(), ref("abc", 0),
                ref("abc", -3))) {
            final DriveExportRequestForm form = DriveExportRequestForm.builder().siteId("site-id")
                    .selection(ExportSelectionForm.builder().addContentlets(bad).build()).build();

            final BadRequestException error =
                    assertThrows(BadRequestException.class, form::validate);
            assertEquals(BAD_CONTENTLET, messageOf(error));
        }
    }

    /**
     * Method to test: JSON deserialisation of {@link DriveExportRequestForm}
     * Given Scenario: The selection-mode body the UI sends, with unknown properties at every level
     * ExpectedResult: It reads into the form; unknown properties are ignored, and a contentlet entry
     * without a language reads as 0 (so validation rejects it)
     */
    @Test
    public void test_deserialisesFromTheRequestBody_ignoringUnknownProperties() throws Exception {
        final DriveExportRequestForm form = DotObjectMapperProvider.getInstance()
                .getDefaultObjectMapper().readValue("{"
                        + "\"siteId\":\"site-id\",\"unknown\":true,"
                        + "\"selection\":{\"folders\":[\"//demo.dotcms.com/blog/\"],"
                        + "\"contentlets\":[{\"identifier\":\"abc\",\"languageId\":2,\"extra\":1},"
                        + "{\"identifier\":\"def\"}],\"other\":[]},"
                        + "\"query\":{\"assetPath\":\"//demo.dotcms.com/\",\"language\":[\"1\"],"
                        + "\"contentTypes\":[\"Blog\"],\"mystery\":\"x\"}}",
                        DriveExportRequestForm.class);

        assertEquals("site-id", form.siteId());
        assertEquals(List.of("//demo.dotcms.com/blog/"), form.selection().folders());
        assertEquals("abc", form.selection().contentlets().get(0).identifier());
        assertEquals(2, form.selection().contentlets().get(0).languageId());
        assertEquals(0, form.selection().contentlets().get(1).languageId());
        assertEquals(List.of("Blog"), form.query().contentTypes());
    }

    /**
     * Method to test: JSON deserialisation of {@link DriveExportRequestForm}
     * Given Scenario: The filter-mode body, which carries no selection and no site
     * ExpectedResult: Selection and site read as null
     */
    @Test
    public void test_deserialisesFilterModeBody() throws Exception {
        final DriveExportRequestForm form = DotObjectMapperProvider.getInstance()
                .getDefaultObjectMapper().readValue("{\"query\":{\"assetPath\":\"//demo.dotcms.com/\","
                        + "\"language\":[\"1\"],\"contentTypes\":[\"Blog\"]}}",
                        DriveExportRequestForm.class);

        assertNull(form.selection());
        assertNull(form.siteId());
        assertEquals(Mode.FILTER, form.validate());
    }
}
