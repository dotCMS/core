package com.dotcms.rest.api.v1.asset.bulkduplicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests for {@link FolderBulkDuplicateForm}, the shape only (#37062, contract §1).
 * <p>
 * Mirrors {@code FolderBulkDeleteFormTest}: the form carries {@code assetPaths} and nothing else, and
 * has no self-validation. The empty-selection and over-the-maximum refusals need the configured
 * ceiling and a specific {@code errorCode}, so they are exercised on the endpoint, not here.
 */
public class FolderBulkDuplicateFormTest {

    /**
     * Method to test: {@link FolderBulkDuplicateForm#assetPaths()}
     * Given Scenario: A form built with several paths
     * ExpectedResult: The paths are held in the order given
     */
    @Test
    public void test_assetPaths_holdsGivenOrder() {
        final FolderBulkDuplicateForm form = FolderBulkDuplicateForm.builder()
                .assetPaths(List.of("//default/a/", "//default/b/", "//default/c/"))
                .build();

        assertEquals(List.of("//default/a/", "//default/b/", "//default/c/"), form.assetPaths());
    }

    /**
     * Method to test: {@link FolderBulkDuplicateForm#assetPaths()}
     * Given Scenario: A form built with no paths at all
     * ExpectedResult: It builds: an empty list is a valid shape, even though the endpoint refuses it
     * as a submission.
     */
    @Test
    public void test_assetPaths_emptyList_isAValidShape() {
        final FolderBulkDuplicateForm form = FolderBulkDuplicateForm.builder().build();

        assertTrue(form.assetPaths().isEmpty());
    }

    /**
     * Method to test: JSON deserialisation of {@link FolderBulkDuplicateForm}
     * Given Scenario: The request body the client sends
     * ExpectedResult: It reads into the form with the paths intact, and no destination field exists
     * to be sent, since every duplicate lands beside its original. Read with the REST layer's own
     * mapper, which is what the endpoint uses; a bare mapper cannot build the Guava list the form
     * holds.
     */
    @Test
    public void test_deserialisesFromTheRequestBody() throws Exception {
        final FolderBulkDuplicateForm form = DotObjectMapperProvider.getInstance().getDefaultObjectMapper().readValue(
                "{\"assetPaths\":[\"//demo.dotcms.com/blogs/alpha/\"]}",
                FolderBulkDuplicateForm.class);

        assertEquals(List.of("//demo.dotcms.com/blogs/alpha/"), form.assetPaths());
    }
}
