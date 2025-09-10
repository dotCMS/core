package com.dotcms.rest.api.v1.content.dotimport;

import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.error.JobProcessorNotFoundException;
import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.job.JobView;
import com.dotcms.jobs.business.job.JobViewPaginatedResult;
import com.dotcms.jobs.business.util.JobUtil;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityJobStatusView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.job.JobResponseUtil;
import com.dotcms.rest.api.v1.job.JobStatusResponse;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.importer.model.AbstractImportResult.OperationType;
import com.dotmarketing.util.importer.model.AbstractValidationMessage.ValidationMessageType;
import com.dotmarketing.util.importer.model.ImportResult;
import com.dotmarketing.util.importer.model.ValidationMessage;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;

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

    private static final String VALIDATION_ERROR_CODE = "JOB_CREATION_VALIDATION_ERROR";

    // Constants for commands
    static final String CMD_PUBLISH = Constants.PUBLISH;
    static final String CMD_PREVIEW = Constants.PREVIEW;

    private static final Set<Class<? extends Throwable>> VALIDATION_EXCEPTION_TYPES = Set.of(
            JobValidationException.class,
            ValidationException.class,
            com.dotcms.rest.exception.ValidationException.class,
            ValueInstantiationException.class,
            JsonEOFException.class,
            JsonParseException.class
    );

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
     * Creates a map of job parameters for error handling during content import.
     *
     * @param command The command indicating the type of operation (e.g., "preview" or "import").
     * @param params  The content import parameters containing the details of the import operation.
     * @param user    The user initiating the import.
     * @param request The HTTP request associated with the import operation.
     * @return A map containing the job parameters for error handling.
     */
    private Map<String, Object> createJobParametersOnError(
            final String command,
            final ContentImportParams params,
            final User user,
            final HttpServletRequest request) {

        final var jsonForm = params.getJsonForm();

        final Map<String, Object> jobParameters;
        if (null != jsonForm) {
            jobParameters = Try.of(() -> JsonUtil.getJsonFromString(jsonForm))
                    .getOrElse(new HashMap<>());
        } else {
            jobParameters = new HashMap<>();
        }

        jobParameters.put("cmd", command);
        jobParameters.put("userId", user.getUserId());

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

    /**
     * Handles the creation of a content import job. This method processes the request parameters,
     * creates the job, and returns an appropriate response. If validation fails, it returns a
     * BAD_REQUEST response with error details.
     *
     * @param command        The command indicating the type of operation (e.g., "preview" or
     *                       "import")
     * @param params         The content import parameters containing file and form data
     * @param initDataObject Object containing initialized user and request data
     * @param request        The HTTP servlet request
     * @return A Response object containing either:
     *         - 200 OK with job status for successful creation
     *         - 400 BAD_REQUEST with validation error details
     * @throws DotDataException        If there is an error accessing the data layer
     * @throws JsonProcessingException If there is an error processing JSON data
     */
    Response handleJobCreation(final String command, final ContentImportParams params,
            final InitDataObject initDataObject, final HttpServletRequest request)
            throws DotDataException, IOException {

        Logger.debug(this, () -> String.format(
                " user %s is importing content in preview mode: %s",
                initDataObject.getUser().getUserId(), params)
        );

        try {

            // Create the content import job in preview mode
            final String jobId = createJob(
                    command, IMPORT_CONTENTLETS_QUEUE_NAME, params,
                    initDataObject.getUser(), request
            );

            final var jobStatusResponse = buildJobStatusResponse(jobId, request);
            return Response.ok(new ResponseEntityJobStatusView(jobStatusResponse)).build();
        } catch (Exception e) {
            if (isValidationException(e)) {
                return responseForValidationException(
                        command, params, initDataObject.getUser(), request, e
                );
            }

            throw e;
        }
    }

    /**
     * Creates an error response for validation exceptions during content import. This method builds
     * a job object with error metadata and returns a BAD_REQUEST response.
     *
     * @param command   The command being executed (e.g. preview, publish)
     * @param params    The content import parameters from the request
     * @param user      The user executing the import
     * @param request   The HTTP servlet request
     * @param exception The exception containing the validation error message
     * @return A Response object with BAD_REQUEST status and job error details
     */
    Response responseForValidationException(
            final String command,
            final ContentImportParams params,
            final User user,
            final HttpServletRequest request,
            final Exception exception) {

        final Map<String, Object> jobParameters = createJobParametersOnError(
                command, params, user, request
        );
        // Clean up null job parameters
        jobParameters.entrySet().removeIf(entry -> entry.getValue() == null);

        final var contentType = jobParameters.getOrDefault("contentType", "");
        final String workflowActionId = (String) jobParameters.getOrDefault(
                "workflowActionId", null);

        var operationType = OperationType.PREVIEW;
        if (CMD_PUBLISH.equalsIgnoreCase(command)) {
            operationType = OperationType.PUBLISH;
        }

        // Create a validation messages based on the exception
        final var validationMessages = createValidationMessage(exception);

        final var importResults = ImportResult.builder()
                .type(operationType)
                .contentTypeName((String) contentType)
                .workflowActionId(Optional.ofNullable(workflowActionId))
                .contentTypeVariableName("")
                .error(validationMessages)
                .build();

        final var job = Job.builder()
                .parameters(jobParameters)
                .id("")
                .queueName(IMPORT_CONTENTLETS_QUEUE_NAME)
                .state(JobState.FAILED)
                .result(JobResult.builder()
                        .metadata(JobUtil.transformToMap(importResults))
                        .build())
                .build();

        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(new ResponseEntityView<>(view(job)))
                .build();
    }

    /**
     * Checks if the provided exception is a validation exception.
     *
     * @param e The exception to check
     * @return True if the exception is a validation exception, false otherwise
     */
    private boolean isValidationException(Exception e) {
        return VALIDATION_EXCEPTION_TYPES.stream()
                .anyMatch(exceptionType -> exceptionType.isInstance(e));
    }

    /**
     * Creates a list of validation messages based on the provided exception.
     *
     * @param exception The exception that contains the validation error message
     * @return A list of ValidationMessage objects
     */
    private List<ValidationMessage> createValidationMessage(final Exception exception) {

        if (exception instanceof com.dotcms.rest.exception.ValidationException) {
            return getValidationExceptionMessages(
                    (com.dotcms.rest.exception.ValidationException) exception
            );
        } else if (exception.getCause() instanceof com.dotcms.rest.exception.ValidationException) {
            return getValidationExceptionMessages(
                    (com.dotcms.rest.exception.ValidationException) exception.getCause()
            );
        }

        return Collections.singletonList(ValidationMessage.builder()
                .message(exception.getMessage())
                .code(VALIDATION_ERROR_CODE)
                .type(ValidationMessageType.ERROR)
                .build());
    }

    /**
     * Converts a ValidationException to a list of ValidationMessage objects.
     *
     * @param exception The ValidationException to convert
     * @return A list of ValidationMessage objects
     */
    private List<ValidationMessage> getValidationExceptionMessages(
            com.dotcms.rest.exception.ValidationException exception) {

        final var violations = exception.violations;
        return violations.stream()
                .map(violation -> ValidationMessage.builder()
                        .message(violation.getMessage())
                        .code(VALIDATION_ERROR_CODE)
                        .type(ValidationMessageType.ERROR)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Builds a JobStatusResponse object with the job ID and status URL.
     *
     * @param jobId   The job ID
     * @param request The HttpServletRequest to build the base URL
     * @return A JobStatusResponse object
     */
    JobStatusResponse buildJobStatusResponse(String jobId, HttpServletRequest request) {
        return JobResponseUtil.buildJobStatusResponse(
                jobId, "/api/v1/content/_import/%s", request
        );
    }

    /**
     * Converts a Job object to a JobView object.
     * @param job The Job object to convert.
     * @return The JobView object.
     */
    JobView view(final Job job) {
        return JobView.builder().from(job).build();
    }

    /**
     * Converts a JobPaginatedResult object to a JobViewPaginatedResult object.
     * @param result The JobPaginatedResult object to convert.
     * @return The JobViewPaginatedResult object.
     */
    JobViewPaginatedResult view(final JobPaginatedResult result) {
        return JobViewPaginatedResult.builder()
                .page(result.page())
                .pageSize(result.pageSize())
                .total(result.total())
                .jobs(result.jobs().stream().map(this::view).collect(Collectors.toList()))
                .build();
    }
}