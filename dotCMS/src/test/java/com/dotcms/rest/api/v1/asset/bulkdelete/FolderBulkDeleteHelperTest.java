package com.dotcms.rest.api.v1.asset.bulkdelete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.liferay.portal.model.User;
import org.junit.Test;

/**
 * Unit tests for {@link FolderBulkDeleteHelper#submit(FolderBulkDeleteForm, User)}: the refusals
 * decided before a job exists.
 */
public class FolderBulkDeleteHelperTest {

    /**
     * Given Scenario: A request with no body at all, which reaches the helper as a null form
     * ExpectedResult: Refused as EMPTY_SELECTION on assetPaths, the same as an empty list, and no
     * job is created. Not a 500 from dereferencing the missing form (#37782)
     */
    @Test
    public void test_submit_noBody_emptySelection() {
        final JobQueueManagerAPI jobQueueManagerAPI = mock(JobQueueManagerAPI.class);
        final FolderBulkDeleteHelper helper = new FolderBulkDeleteHelper(jobQueueManagerAPI);

        final FolderBulkDeleteRefusedException refused = assertThrows(
                FolderBulkDeleteRefusedException.class,
                () -> helper.submit(null, new User()));

        assertEquals("EMPTY_SELECTION", refused.errorCode());
        assertEquals("assetPaths", refused.fieldName());
        verifyNoInteractions(jobQueueManagerAPI);
    }
}
