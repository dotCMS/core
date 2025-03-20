package com.dotcms.rest.api.v1.content.dotimport;

import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.error.JobProcessorNotFoundException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * Helper class for managing content import operations in the dotCMS application.
 * <p>
 * This class provides methods to create and manage jobs for importing content from external
 * sources, such as CSV files, into the system. It handles the validation of import parameters,
 * processes file uploads, and constructs the necessary job parameters to enqueue content import
 * tasks in the job queue.
 */
@ApplicationScoped
public class ContentImportHelper {

    private final JobQueueManagerAPI jobQueueManagerAPI;
    private static final String IMPORT_CONTENTLETS_QUEUE_NAME = "importContentlets";

    /**
     * Constructor for dependency injection.
     *
     * @param jobQueueManagerAPI The API for managing job queues.
     */
    @Inject
    public ContentImportHelper(final JobQueueManagerAPI jobQueueManagerAPI) {
        this.jobQueueManagerAPI = jobQueueManagerAPI;
    }

    /**
     * Default constructor required for CDI.
     */
    public ContentImportHelper() {
        this.jobQueueManagerAPI = null;
    }

    /**
     * Creates a content import job with the provided parameters and submits it to the job queue.
     *
     * @param command   The command indicating the type of operation (e.g., "preview" or "import").
     * @param queueName The name of the queue to which the job should be submitted.
     * @param params    The content import parameters containing the details of the import
     *                  operation.
     * @param user      The user initiating the import.
     * @param request   The HTTP request associated with the import operation.
     * @return The ID of the created job.
     * @throws DotDataException        If there is an error creating the job.
     * @throws JsonProcessingException If there is an error processing JSON data.
     */
    public String createJob(
            final String command,
            final String queueName,
            final ContentImportParams params,
            final User user,
            final HttpServletRequest request) throws DotDataException, JsonProcessingException {

        params.checkValid();
        final Map<String, Object> jobParameters = createJobParameters(command, params, user,
                request);
        processFileUpload(params, jobParameters, request);

        return jobQueueManagerAPI.createJob(queueName, jobParameters);
    }

    /**
     * gets a job
     *
     * @param jobId The ID of the job
     * @return Job
     * @throws DotDataException if there's an error fetching the job
     */
    Job getJob(final String jobId) throws DotDataException {
        return jobQueueManagerAPI.getJob(jobId);
    }

    /**
     * Retrieves a list of jobs.
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return A result object containing the list of jobs and pagination information.
     */
    JobPaginatedResult getJobs(int page, int pageSize) {
        try {
            return jobQueueManagerAPI.getJobs(IMPORT_CONTENTLETS_QUEUE_NAME, page, pageSize);
        } catch (DotDataException e) {
            Logger.error(this.getClass(), "Error fetching content import jobs", e);
        }
        return JobPaginatedResult.builder().build();
    }

    /**
     * Retrieves a list of active content import jobs.
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return JobPaginatedResult
     */
    JobPaginatedResult getActiveJobs(int page, int pageSize) {
        try {
            return jobQueueManagerAPI.getActiveJobs(IMPORT_CONTENTLETS_QUEUE_NAME, page, pageSize);
        } catch (DotDataException e) {
            Logger.error(this.getClass(), "Error fetching active content import jobs", e);
        }
        return JobPaginatedResult.builder().build();
    }


    /**
     * Retrieves a list of completed content import jobs.
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return JobPaginatedResult
     */
    JobPaginatedResult getCompletedJobs(int page, int pageSize) {
        try {
            return jobQueueManagerAPI.getCompletedJobs(IMPORT_CONTENTLETS_QUEUE_NAME, page,
                    pageSize);
        } catch (DotDataException e) {
            Logger.error(this.getClass(), "Error fetching active content import jobs", e);
        }
        return JobPaginatedResult.builder().build();
    }

    /**
     * Retrieves a list of completed content import jobs.
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return JobPaginatedResult
     */
    JobPaginatedResult getCanceledJobs(int page, int pageSize) {
        try {
            return jobQueueManagerAPI.getCanceledJobs(IMPORT_CONTENTLETS_QUEUE_NAME, page,
                    pageSize);
        } catch (DotDataException e) {
            Logger.error(this.getClass(), "Error fetching canceled content import jobs", e);
        }
        return JobPaginatedResult.builder().build();
    }

    /**
     * Retrieves a list of failed content import jobs.
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return JobPaginatedResult
     */
    JobPaginatedResult getFailedJobs(int page, int pageSize) {
        try {
            return jobQueueManagerAPI.getFailedJobs(IMPORT_CONTENTLETS_QUEUE_NAME, page, pageSize);
        } catch (DotDataException e) {
            Logger.error(this.getClass(), "Error fetching failed content import jobs", e);
        }
        return JobPaginatedResult.builder().build();
    }

    /**
     * Retrieves a list of abandoned content import jobs.
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return JobPaginatedResult
     */
    JobPaginatedResult getAbandonedJobs(int page, int pageSize) {
        try {
            return jobQueueManagerAPI.getAbandonedJobs(IMPORT_CONTENTLETS_QUEUE_NAME, page,
                    pageSize);
        } catch (DotDataException e) {
            Logger.error(this.getClass(), "Error fetching abandoned content import jobs", e);
        }
        return JobPaginatedResult.builder().build();
    }

    /**
     * Retrieves a list of successful content import jobs.
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return JobPaginatedResult
     */
    JobPaginatedResult getSuccessfulJobs(int page, int pageSize) {
        try {
            return jobQueueManagerAPI.getSuccessfulJobs(IMPORT_CONTENTLETS_QUEUE_NAME, page,
                    pageSize);
        } catch (DotDataException e) {
            Logger.error(this.getClass(), "Error fetching abandoned content import jobs", e);
        }
        return JobPaginatedResult.builder().build();
    }

    /**
     * cancels a job
     *
     * @param jobId The ID of the job
     * @throws DotDataException if there's an error cancelling the job
     */
    void cancelJob(String jobId) throws DotDataException {
        try {
            jobQueueManagerAPI.cancelJob(jobId);
        } catch (JobProcessorNotFoundException e) {
            Logger.error(this.getClass(), "Error cancelling job", e);
            throw new DoesNotExistException(e.getMessage());
        }
    }

    /**
     * Constructs a map of job parameters based on the provided inputs.
     *
     * @param command The command indicating the type of operation.
     * @param params  The content import parameters.
     * @param user    The user initiating the import.
     * @param request The HTTP request associated with the operation.
     * @return A map containing the job parameters.
     * @throws JsonProcessingException If there is an error processing JSON data.
     */
    private Map<String, Object> createJobParameters(
            final String command,
            final ContentImportParams params,
            final User user,
            final HttpServletRequest request) throws JsonProcessingException {

        final Map<String, Object> jobParameters = new HashMap<>();

        // Add required parameters
        jobParameters.put("cmd", command);
        jobParameters.put("userId", user.getUserId());
        jobParameters.put("contentType", params.getForm().getContentType());
        jobParameters.put("workflowActionId", params.getForm().getWorkflowActionId());

        // Add optional parameters
        addOptionalParameters(params, jobParameters);

        // Add site information
        addSiteInformation(request, jobParameters);

        return jobParameters;
    }

    /**
     * Adds optional parameters to the job parameter map if they are present in the form.
     *
     * @param params        The content import parameters.
     * @param jobParameters The map of job parameters to which optional parameters are added.
     * @throws JsonProcessingException If there is an error processing JSON data.
     */
    private void addOptionalParameters(
            final ContentImportParams params,
            final Map<String, Object> jobParameters) throws JsonProcessingException {

        final ContentImportForm form = params.getForm();

        if (form.getLanguage() != null && !form.getLanguage().isEmpty()) {
            jobParameters.put("language", form.getLanguage());
        }
        if (form.getFields() != null && !form.getFields().isEmpty()) {
            jobParameters.put("fields", form.getFields());
        }

        if(null != form.getStopOnError()){
            jobParameters.put("stopOnError", form.getStopOnError());
        }

        if(null != form.getCommitGranularity()){
            jobParameters.put("commitGranularity", form.getCommitGranularity());
        }

    }

    /**
     * Adds the current site information to the job parameters.
     *
     * @param request       The HTTP request associated with the operation.
     * @param jobParameters The map of job parameters to which site information is added.
     */
    private void addSiteInformation(
            final HttpServletRequest request,
            final Map<String, Object> jobParameters) {

        final var currentHost = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        jobParameters.put("siteName", currentHost.getHostname());
        jobParameters.put("siteIdentifier", currentHost.getIdentifier());
    }

    /**
     * Processes the file upload and adds the file-related parameters to the job.
     *
     * @param params        The content import parameters.
     * @param jobParameters The map of job parameters.
     * @param request       The HTTP request containing the uploaded file.
     * @throws DotDataException If there is an error processing the file upload.
     */
    private void processFileUpload(
            final ContentImportParams params,
            final Map<String, Object> jobParameters,
            final HttpServletRequest request) throws DotDataException {

        try {
            final DotTempFile tempFile = APILocator.getTempFileAPI().createTempFile(
                    params.getContentDisposition().getFileName(),
                    request,
                    params.getFileInputStream()
            );
            jobParameters.put("tempFileId", tempFile.id);
            jobParameters.put("requestFingerPrint",
                    APILocator.getTempFileAPI().getRequestFingerprint(request));
        } catch (DotSecurityException e) {
            Logger.error(this, "Error handling file upload", e);
            throw new DotDataException("Error processing file upload: " + e.getMessage());
        }
    }
}