package com.dotcms.rest.api.v1.categories;

import static org.junit.Assert.assertEquals;
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

import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.liferay.portal.model.User;
import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.Test;

/**
 * Unit tests for {@link CategoryHelper#addOrUpdateCategory(User, String, BufferedReader, Boolean)}.
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
}
