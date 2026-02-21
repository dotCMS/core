package com.dotcms.rest.api.v1.categories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.liferay.portal.model.User;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests for the categories import flow: {@link CategoryHelper#addOrUpdateCategory(User, String, BufferedReader, Boolean)}
 * and {@link CategoryImporter#from(BufferedReader)}.
 */
public class CategoryHelperTest {

    private static final String CSV_HEADER = "\"name\",\"key\",\"variable\",\"sort\"\r\n";

    /**
     * Builds a BufferedReader over a CSV that has the given rows appended after a standard header.
     * Each row is: name,key,variable,sort
     */
    private BufferedReader csvReader(final String... rows) {
        final StringBuilder sb = new StringBuilder(CSV_HEADER);
        for (final String row : rows) {
            sb.append(row).append("\r\n");
        }
        return new BufferedReader(new StringReader(sb.toString()));
    }

    // -----------------------------------------------------------------------
    // Merge mode
    // -----------------------------------------------------------------------

    /**
     * When merging a CSV with two new categories (keys not found in DB), both should be saved and
     * the returned success count must equal 2.
     */
    @Test
    public void testMerge_newCategories_returnsSuccessCountEqualToRowCount()
            throws Exception {

        final CategoryAPI categoryAPI = mock(CategoryAPI.class);
        final User user = mock(User.class);

        // Both keys are not found → null means "new category"
        when(categoryAPI.findByKey(anyString(), eq(user), anyBoolean())).thenReturn(null);
        // suggestVelocityVarName returns the input unchanged
        when(categoryAPI.suggestVelocityVarName(anyString())).thenAnswer(
                inv -> inv.getArgument(0));

        final CategoryHelper helper = new CategoryHelper(categoryAPI);
        final BufferedReader reader = csvReader(
                "\"Boys\",\"boys\",\"boys\",0",
                "\"Girls\",\"girls\",\"girls\",0");

        final long count = helper.addOrUpdateCategory(user, null, reader, true);

        assertEquals(2, count);
        verify(categoryAPI, times(2))
                .save(isNull(), any(Category.class), eq(user), eq(false));
    }

    /**
     * When merging a CSV where a category key already exists in the DB, the existing category
     * should be updated (not re-created) and the save should still succeed.
     */
    @Test
    public void testMerge_existingCategory_updatesAndCountsSuccess()
            throws Exception {

        final CategoryAPI categoryAPI = mock(CategoryAPI.class);
        final User user = mock(User.class);

        final Category existing = new Category();
        existing.setKey("boys");
        existing.setCategoryName("Boys Old Name");

        when(categoryAPI.findByKey(eq("boys"), eq(user), anyBoolean())).thenReturn(existing);
        when(categoryAPI.suggestVelocityVarName(anyString())).thenAnswer(
                inv -> inv.getArgument(0));

        final CategoryHelper helper = new CategoryHelper(categoryAPI);
        final BufferedReader reader = csvReader("\"Boys New\",\"boys\",\"boys\",1");

        final long count = helper.addOrUpdateCategory(user, null, reader, true);

        assertEquals(1, count);
        verify(categoryAPI, times(1))
                .save(isNull(), eq(existing), eq(user), eq(false));
    }

    /**
     * When a DotSecurityException is thrown during save, the failing category should not be counted
     * as a success, but the method must not propagate the exception and must continue processing
     * remaining rows.
     */
    @Test
    public void testMerge_securityExceptionOnSave_countedAsFailure()
            throws Exception {

        final CategoryAPI categoryAPI = mock(CategoryAPI.class);
        final User user = mock(User.class);

        when(categoryAPI.findByKey(anyString(), eq(user), anyBoolean())).thenReturn(null);
        when(categoryAPI.suggestVelocityVarName(anyString())).thenAnswer(
                inv -> inv.getArgument(0));

        // First save throws, second save succeeds
        doThrow(new DotSecurityException("Not allowed"))
                .doNothing()
                .when(categoryAPI).save(isNull(), any(Category.class), eq(user), eq(false));

        final CategoryHelper helper = new CategoryHelper(categoryAPI);
        final BufferedReader reader = csvReader(
                "\"Boys\",\"boys\",\"boys\",0",
                "\"Girls\",\"girls\",\"girls\",0");

        final long count = helper.addOrUpdateCategory(user, null, reader, true);

        // Only the second row succeeded
        assertEquals(1, count);
    }

    /**
     * When all rows fail with a DotSecurityException during save, the success count must be 0.
     */
    @Test
    public void testMerge_allSecurityExceptions_returnsZero()
            throws Exception {

        final CategoryAPI categoryAPI = mock(CategoryAPI.class);
        final User user = mock(User.class);

        when(categoryAPI.findByKey(anyString(), eq(user), anyBoolean())).thenReturn(null);
        when(categoryAPI.suggestVelocityVarName(anyString())).thenAnswer(
                inv -> inv.getArgument(0));

        doThrow(new DotSecurityException("Not allowed"))
                .when(categoryAPI).save(isNull(), any(Category.class), eq(user), eq(false));

        final CategoryHelper helper = new CategoryHelper(categoryAPI);
        final BufferedReader reader = csvReader("\"Boys\",\"boys\",\"boys\",0");

        final long count = helper.addOrUpdateCategory(user, null, reader, true);

        assertEquals(0, count);
    }

    // -----------------------------------------------------------------------
    // Replace mode
    // -----------------------------------------------------------------------

    /**
     * In replace mode the categories are saved without looking up an existing key. A successful
     * save must be reflected in the success count.
     */
    @Test
    public void testReplace_newCategories_returnsSuccessCount()
            throws Exception {

        final CategoryAPI categoryAPI = mock(CategoryAPI.class);
        final User user = mock(User.class);

        when(categoryAPI.suggestVelocityVarName(anyString())).thenAnswer(
                inv -> inv.getArgument(0));

        final CategoryHelper helper = new CategoryHelper(categoryAPI);
        final BufferedReader reader = csvReader(
                "\"Mens\",\"mens\",\"mens\",0",
                "\"Womens\",\"womens\",\"womens\",0");

        final long count = helper.addOrUpdateCategory(user, null, reader, false);

        assertEquals(2, count);
        // In replace mode findByKey should NOT be called
        verify(categoryAPI, never()).findByKey(anyString(), any(User.class), anyBoolean());
        verify(categoryAPI, times(2))
                .save(isNull(), any(Category.class), eq(user), eq(false));
    }

    /**
     * An empty CSV (only headers, no data rows) should result in a success count of 0 and no save
     * calls, regardless of merge mode.
     */
    @Test
    public void testEmptyCSV_returnsZero() throws Exception {

        final CategoryAPI categoryAPI = mock(CategoryAPI.class);
        final User user = mock(User.class);

        final CategoryHelper helper = new CategoryHelper(categoryAPI);
        // Only headers, no data rows
        final BufferedReader reader = new BufferedReader(
                new StringReader(CSV_HEADER));

        final long count = helper.addOrUpdateCategory(user, null, reader, true);

        assertEquals(0, count);
        verify(categoryAPI, never()).save(any(), any(), any(), anyBoolean());
    }

    // -----------------------------------------------------------------------
    // CategoryImporter - header validation and name trimming (Issue #3 and #4)
    // -----------------------------------------------------------------------

    /**
     * A CSV with correct headers (name, key, variable, sort) must parse without error.
     */
    @Test
    public void testImporter_validHeaders_parsesRows() throws IOException {
        final BufferedReader reader = csvReader("\"Boys\",\"boys\",\"boys\",0");
        final List<CategoryDTO> result = CategoryImporter.from(reader);
        assertEquals(1, result.size());
        assertEquals("Boys", result.get(0).getCategoryName());
    }

    /**
     * A CSV with wrong column headers must throw IOException so the caller can return 400.
     */
    @Test
    public void testImporter_invalidHeaders_throwsIOException() {
        final BufferedReader reader = new BufferedReader(new StringReader(
                "\"invalid1\",\"invalid2\",\"invalid3\",\"invalid4\"\r\n"
                        + "\"Boys\",\"boys\",\"boys\",0\r\n"));
        try {
            CategoryImporter.from(reader);
            fail("Expected IOException for invalid CSV headers");
        } catch (IOException e) {
            // expected
        }
    }

    /**
     * A CSV with fewer than 4 columns in the header must throw IOException.
     */
    @Test
    public void testImporter_tooFewHeaders_throwsIOException() {
        final BufferedReader reader = new BufferedReader(
                new StringReader("\"name\",\"key\"\r\n\"Boys\",\"boys\"\r\n"));
        try {
            CategoryImporter.from(reader);
            fail("Expected IOException for too-few CSV headers");
        } catch (IOException e) {
            // expected
        }
    }

    /**
     * Category names with leading/trailing spaces must be trimmed when parsed.
     */
    @Test
    public void testImporter_leadingSpacesInName_trimmed() throws IOException {
        final BufferedReader reader = csvReader("\"  Laptops  \",\"laptops\",\"laptops\",0");
        final List<CategoryDTO> result = CategoryImporter.from(reader);
        assertEquals(1, result.size());
        assertEquals("Laptops", result.get(0).getCategoryName());
    }

    // -----------------------------------------------------------------------
    // CategoryHelper - DotDataException handling (Issue #1)
    // -----------------------------------------------------------------------

    /**
     * When a DotDataException is thrown during save, the failing row should not be counted as a
     * success. The method must continue processing remaining rows (no abort).
     */
    @Test
    public void testMerge_dataExceptionOnSave_countedAsFailure()
            throws Exception {

        final CategoryAPI categoryAPI = mock(CategoryAPI.class);
        final User user = mock(User.class);

        when(categoryAPI.findByKey(anyString(), eq(user), anyBoolean())).thenReturn(null);
        when(categoryAPI.suggestVelocityVarName(anyString())).thenAnswer(
                inv -> inv.getArgument(0));

        // First save throws DotDataException, second succeeds
        doThrow(new DotDataException("DB error"))
                .doNothing()
                .when(categoryAPI).save(isNull(), any(Category.class), eq(user), eq(false));

        final CategoryHelper helper = new CategoryHelper(categoryAPI);
        final BufferedReader reader = csvReader(
                "\"Boys\",\"boys\",\"boys\",0",
                "\"Girls\",\"girls\",\"girls\",0");

        final long count = helper.addOrUpdateCategory(user, null, reader, true);

        // Only the second row succeeded
        assertEquals(1, count);
    }
}
