package com.dotcms.jobs.business.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

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

    /**
     * Utility method to create or retrieve an HttpServletRequest when needed from job processors.
     * Uses thread-local request if available, otherwise creates a mock request with the specified
     * user and site information.
     *
     * @param user     The user performing the import
     * @param siteName The name of the site for the import
     * @return An HttpServletRequest instance configured for the import operation
     */
    public static HttpServletRequest generateMockRequest(final User user, final String siteName) {

        if (null != HttpServletRequestThreadLocal.INSTANCE.getRequest()) {
            return HttpServletRequestThreadLocal.INSTANCE.getRequest();
        }

        final HttpServletRequest requestProxy = new MockSessionRequest(
                new MockHeaderRequest(
                        new FakeHttpRequest(siteName, "/").request(),
                        "referer",
                        "https://" + siteName + "/fakeRefer")
                        .request());
        requestProxy.setAttribute(WebKeys.CMS_USER, user);
        requestProxy.getSession().setAttribute(WebKeys.CMS_USER, user);
        requestProxy.setAttribute(com.liferay.portal.util.WebKeys.USER_ID,
                UtilMethods.extractUserIdOrNull(user));

        return requestProxy;
    }

}
