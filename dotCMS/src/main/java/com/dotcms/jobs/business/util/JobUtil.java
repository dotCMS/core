package com.dotcms.jobs.business.util;

import com.dotcms.jobs.business.job.Job;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class for job-related operations.
 */
public class JobUtil {

    private JobUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Retrieves the temporary file associated with the given job.
     *
     * @param job The job containing the temporary file information in its parameters.
     * @return An Optional containing the DotTempFile if found, or an empty Optional if not found or
     * if an error occurs.
     */
    public static Optional<DotTempFile> retrieveTempFile(final Job job) {

        if (job == null) {
            Logger.error(JobUtil.class, "Job cannot be null");
            return Optional.empty();
        }

        Map<String, Object> params = job.parameters();
        if (params == null) {
            Logger.error(JobUtil.class, "Job parameters cannot be null");
            return Optional.empty();
        }

        // Extract parameters
        String tempFileId = (String) params.get("tempFileId");
        if (tempFileId == null) {
            Logger.error(JobUtil.class, "Parameter 'tempFileId' is required");
            return Optional.empty();
        }

        final Object requestFingerPrintRaw = params.get("requestFingerPrint");
        if (!(requestFingerPrintRaw instanceof String)) {
            Logger.error(JobUtil.class,
                    "Parameter 'requestFingerPrint' is required and must be a string.");
            return Optional.empty();
        }
        final String requestFingerPrint = (String) requestFingerPrintRaw;

        // Retrieve the temporary file
        final TempFileAPI tempFileAPI = APILocator.getTempFileAPI();
        final Optional<DotTempFile> tempFile = tempFileAPI.getTempFile(
                List.of(requestFingerPrint), tempFileId
        );
        if (tempFile.isEmpty()) {
            Logger.error(JobUtil.class, "Temporary file not found: " + tempFileId);
        }

        return tempFile;
    }

}
