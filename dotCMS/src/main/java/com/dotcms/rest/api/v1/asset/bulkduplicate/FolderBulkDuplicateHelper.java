package com.dotcms.rest.api.v1.asset.bulkduplicate;

import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Validates a bulk folder duplication submission and enqueues the run (#37062).
 * <p>
 * Only the constants for now; the submission behaviour arrives with the first user story. Mirrors
 * {@code FolderBulkDeleteHelper}, without its overlap check: duplication carries no overlap guard,
 * so the same folders can be submitted twice.
 *
 * @author dotCMS
 */
@ApplicationScoped
public class FolderBulkDuplicateHelper {

    /** The queue this feature's runs are submitted to. */
    public static final String QUEUE_NAME = "folderBulkDuplicate";

    /**
     * The configuration key for the most distinct paths one submission may carry. Advertised to
     * clients as {@code folderBulkDuplicate.maxPaths} on {@code /api/v1/appconfiguration}.
     */
    public static final String MAX_PATHS_KEY = "FOLDER_BULK_DUPLICATE_MAX_PATHS";

    /** The default ceiling, the same as bulk folder delete's and for the same reason. */
    public static final int DEFAULT_MAX_PATHS = 50;

    private final JobQueueManagerAPI jobQueueManagerAPI;

    /**
     * Required by CDI, never called by this code: {@code @ApplicationScoped} needs a no-args
     * constructor for Weld to build a client proxy.
     */
    public FolderBulkDuplicateHelper() {
        this.jobQueueManagerAPI = null;
    }

    /**
     * @param jobQueueManagerAPI the queue runs are submitted to
     */
    @Inject
    public FolderBulkDuplicateHelper(final JobQueueManagerAPI jobQueueManagerAPI) {
        this.jobQueueManagerAPI = jobQueueManagerAPI;
    }

    // RED STUB (#37062 T025): refuses nothing and enqueues nothing; replaced in T028.
    public FolderBulkDuplicateSubmitResponse submit(final FolderBulkDuplicateForm form,
            final User user) throws DotDataException {
        return FolderBulkDuplicateSubmitResponse.builder()
                .jobId("red-stub")
                .statusUrl("")
                .submitted(0)
                .build();
    }

    // RED STUB (#37062 T025): replaced in T028.
    public static List<String> pathsOf(final Map<String, Object> parameters) {
        return List.of();
    }
}
