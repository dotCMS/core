package com.dotcms.rest.api.v1.job;

import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.rest.exception.BadRequestException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

public class JobResponseUtil {

    /**
     * Job parameters the server sets when files are uploaded with a job request. Job processors
     * use them to open the uploaded files; the job endpoints neither return nor accept them.
     */
    static final Set<String> SERVER_FILE_PARAMETERS =
            Set.of("tempFileId", "requestFingerPrint", "requestFingerprint");

    /**
     * The job parameter that lists the files of a bulk upload. Each entry carries the uploaded
     * file's {@code tempFileId}, which the job endpoints do not return either.
     */
    private static final String STAGED_FILES = "stagedFiles";

    private JobResponseUtil() {
        // Utility class
    }

    /**
     * Returns the job as the job endpoints show it, without the parameters the server sets for
     * uploaded files: {@link #SERVER_FILE_PARAMETERS}, and the {@code tempFileId} of each
     * {@code stagedFiles} entry. Everything else, including the number of staged files, is kept.
     *
     * @param job the job
     * @return the same job without those parameters
     */
    public static Job forResponse(final Job job) {
        final Map<String, Object> parameters = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : job.parameters().entrySet()) {
            if (SERVER_FILE_PARAMETERS.contains(entry.getKey())) {
                continue;
            }
            parameters.put(entry.getKey(), STAGED_FILES.equals(entry.getKey())
                    ? withoutFileIds(entry.getValue())
                    : entry.getValue());
        }
        return parameters.equals(job.parameters()) ? job : job.withParameters(parameters);
    }

    /**
     * Copies the {@code stagedFiles} parameter without the {@code tempFileId} of each entry.
     *
     * @param stagedFiles the parameter's value; anything other than a list is returned as is
     * @return the copy
     */
    private static Object withoutFileIds(final Object stagedFiles) {
        if (!(stagedFiles instanceof List)) {
            return stagedFiles;
        }
        final List<Object> files = new ArrayList<>();
        for (final Object file : (List<?>) stagedFiles) {
            if (file instanceof Map) {
                final Map<Object, Object> copy = new LinkedHashMap<>((Map<?, ?>) file);
                copy.remove("tempFileId");
                files.add(copy);
            } else {
                files.add(file);
            }
        }
        return files;
    }

    /**
     * Returns a page of jobs as the job endpoints show it; see {@link #forResponse(Job)}.
     *
     * @param result the page of jobs
     * @return the same page, each job without the parameters the server sets for uploaded files
     */
    public static JobPaginatedResult forResponse(final JobPaginatedResult result) {
        return JobPaginatedResult.builder().from(result)
                .jobs(result.jobs().stream().map(JobResponseUtil::forResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Refuses job parameters supplied by the caller that only the server may set.
     *
     * @param parameters the parameters of a job request; {@code null} is accepted
     * @throws BadRequestException when the parameters include any of
     *                             {@link #SERVER_FILE_PARAMETERS}
     */
    public static void rejectServerFileParameters(final Map<String, Object> parameters) {
        if (parameters != null
                && parameters.keySet().stream().anyMatch(SERVER_FILE_PARAMETERS::contains)) {
            throw new BadRequestException(String.format(
                    "Job parameters may not include %s; these are set by the server when a file"
                            + " is uploaded with the request",
                    String.join(", ", new TreeSet<>(SERVER_FILE_PARAMETERS))));
        }
    }

    /**
     * Builds a JobStatusResponse object with the job ID and status URL.
     *
     * @param jobId          The job ID
     * @param statusEndpoint The path to the status endpoint (e.g. "/api/v1/jobs/%s/status")
     * @param request        The HttpServletRequest to build the base URL
     * @return A JobStatusResponse object
     */
    public static JobStatusResponse buildJobStatusResponse(final String jobId,
            final String statusEndpoint, final HttpServletRequest request) {

        String statusUrl = buildBaseUrlFromRequest(request) + String.format(statusEndpoint, jobId);
        return JobStatusResponse.builder()
                .jobId(jobId)
                .statusUrl(statusUrl)
                .build();
    }

    /**
     * Builds the base URL from the HttpServletRequest.
     *
     * @param request The HttpServletRequest
     * @return The base URL as a string
     */
    private static String buildBaseUrlFromRequest(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        if (scheme == null || scheme.isBlank()) {
            scheme = "http";
        }

        boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && serverPort == 80) ||
                ("https".equalsIgnoreCase(scheme) && serverPort == 443);

        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);

        if (!isDefaultPort) {
            baseUrl.append(":").append(serverPort);
        }

        return baseUrl.toString();
    }
}