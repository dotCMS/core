package com.dotcms.rest.api.v1.asset.bulkduplicate;

import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

/**
 * Validates a bulk folder duplication submission and enqueues the run (#37062).
 * <p>
 * Only what makes a submission unusable is refused here: an empty selection, or more distinct
 * folders than the configured maximum. Whether each folder can actually be duplicated is the run's
 * business, reported per folder, so one bad path never refuses the rest.
 * <p>
 * Mirrors {@code FolderBulkDeleteHelper} without its overlap check and site locks: duplication has
 * no overlap guard, so submitting the same folders twice is two separate runs and two duplicates.
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

    /**
     * The default ceiling, the same as bulk folder delete's and for the same reason: each accepted
     * folder is one sequential transaction of unbounded size.
     */
    public static final int DEFAULT_MAX_PATHS = 50;

    /** The job parameter holding the de-duplicated selection, in submission order. */
    static final String ASSET_PATHS_PARAMETER = "assetPaths";

    /** The job parameter holding the submitting user's id. */
    static final String USER_ID_PARAMETER = "userId";

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

    /**
     * Validates the submission and enqueues the run. Nothing has been duplicated when this returns.
     *
     * @param form the selected folders
     * @param user the submitting author; the run checks rights as them and notifies them
     * @return the run's handle
     * @throws FolderBulkDuplicateRefusedException the submission is empty, or over the maximum
     * @throws DotDataException                    the job could not be created
     */
    public FolderBulkDuplicateSubmitResponse submit(final FolderBulkDuplicateForm form,
            final User user) throws DotDataException {

        if (form.assetPaths().isEmpty()) {
            throw new FolderBulkDuplicateRefusedException("EMPTY_SELECTION", ASSET_PATHS_PARAMETER,
                    Response.Status.BAD_REQUEST, "no folder paths were submitted");
        }

        // Collapsed before the run so the outcome reports each folder once. Keyed so that
        // "//host/a", "//host/a/" and "//host/A/" are one folder, since folder resolution ignores
        // case, while the first spelling is kept as sent (trailing slash added): it is what the
        // result key carries back to the author. LinkedHashMap keeps submission order.
        final Map<String, String> firstSpellingByKey = new LinkedHashMap<>();
        for (final String path : form.assetPaths()) {
            firstSpellingByKey.putIfAbsent(comparisonKey(path), normalize(path));
        }
        final List<String> distinctPaths = List.copyOf(firstSpellingByKey.values());

        final int maxPaths = Config.getIntProperty(MAX_PATHS_KEY, DEFAULT_MAX_PATHS);
        if (distinctPaths.size() > maxPaths) {
            throw new FolderBulkDuplicateRefusedException("OVER_MAX_PATHS",
                    ASSET_PATHS_PARAMETER, Response.Status.BAD_REQUEST,
                    String.format("selection exceeds the maximum of %d paths", maxPaths));
        }

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put(USER_ID_PARAMETER, user.getUserId());
        parameters.put(ASSET_PATHS_PARAMETER, new ArrayList<>(distinctPaths));
        final String jobId = jobQueueManagerAPI.createJob(QUEUE_NAME, parameters);

        Logger.info(this, String.format(
                "Bulk folder duplicate job [%s] created by user [%s] for %d path(s)",
                jobId, user.getUserId(), distinctPaths.size()));

        return FolderBulkDuplicateSubmitResponse.builder()
                .jobId(jobId)
                .statusUrl("/api/v1/jobs/" + jobId + "/status")
                .submitted(distinctPaths.size())
                .build();
    }

    /**
     * Reads the selection back from a run's parameters.
     *
     * @param parameters the job's parameters
     * @return the stored paths, in submission order; empty when there are none
     */
    public static List<String> pathsOf(final Map<String, Object> parameters) {
        final Object raw = parameters.get(ASSET_PATHS_PARAMETER);
        if (!(raw instanceof Collection)) {
            return List.of();
        }
        final List<String> paths = new ArrayList<>();
        for (final Object entry : (Collection<?>) raw) {
            paths.add(String.valueOf(entry));
        }
        return paths;
    }

    /**
     * A path with its trailing slash, the form a folder path takes.
     *
     * @param path a site-qualified folder path
     * @return the path ending in {@code /}
     */
    static String normalize(final String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    /** The form two spellings of one folder share: trailing slash added, lowercased. */
    private static String comparisonKey(final String path) {
        return normalize(path).toLowerCase();
    }
}
