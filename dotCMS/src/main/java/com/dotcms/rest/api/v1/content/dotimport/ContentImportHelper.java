package com.dotcms.rest.api.v1.content.dotimport;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.error.JobProcessorNotFoundException;
import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.processor.DefaultProgressTracker;
import com.dotcms.jobs.business.processor.impl.ImportContentletsProcessor;
import com.dotmarketing.util.UUIDGenerator;
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
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.api.v1.job.JobResponseUtil;
import com.dotcms.rest.api.v1.job.JobStatusResponse;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
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
import java.util.Comparator;
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
     * @throws DotDataException        If the requested site can't be looked up.
     */
    private Map<String, Object> createJobParameters(
            final String command,
            final ContentImportParams params,
            final User user,
            final HttpServletRequest request) throws JsonProcessingException, DotDataException {

        final Map<String, Object> jobParameters = new HashMap<>();

        // Add required parameters
        jobParameters.put("cmd", command);
        jobParameters.put("userId", user.getUserId());
        jobParameters.put("contentType", params.getForm().getContentType());
        jobParameters.put("workflowActionId", params.getForm().getWorkflowActionId());

        // Add optional parameters
        addOptionalParameters(params, jobParameters);

        // Add site information: the form's site when it names one (#37896), else the session's
        final String requestedSite = params.getForm().getSite();
        if (UtilMethods.isSet(requestedSite)) {
            addRequestedSite(requestedSite, user, jobParameters);
        } else {
            addSiteInformation(request, jobParameters);
        }

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
     * Adds the site the form names to the job parameters, in place of the session's site (#37896).
     * <p>
     * The session's site is shared by every tab of the user's session, so a tab browsing another
     * site would otherwise resolve the import's in-row references (File and Image values) against
     * the wrong site. Row placement is not affected: rows still go where their CSV column says.
     *
     * @param site          the site's identifier or hostname
     * @param user          the user running the import; must be able to read the site
     * @param jobParameters the job parameters to add the site to
     * @throws DotDataException    if the site can't be looked up
     * @throws ForbiddenException  if the user can't read the site (answered as 403)
     * @throws ValidationException if no site matches (answered as 400)
     */
    private void addRequestedSite(final String site, final User user,
            final Map<String, Object> jobParameters) throws DotDataException {
        final Host host;
        try {
            host = APILocator.getHostAPI().findByIdOrKey(site, user, false)
                    .orElseThrow(() -> new ValidationException(
                            String.format("Site [%s] does not exist", site)));
        } catch (final DotSecurityException e) {
            throw new ForbiddenException(e);
        }
        jobParameters.put("siteName", host.getHostname());
        jobParameters.put("siteIdentifier", host.getIdentifier());
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
     * Adds the CSV to the job's parameters: either the uploaded file, stored as a new temp file, or
     * the temp file the caller names by id, such as the one its preview stored (#37896). A named
     * temp file must exist and must have been uploaded by this same user. The temp file API on its
     * own also accepts a matching session or browser fingerprint, which two users behind the same
     * browser or proxy can share; matching the user only keeps one user from importing another
     * user's CSV by its id.
     *
     * @param params        The content import parameters.
     * @param jobParameters The map of job parameters.
     * @param request       The HTTP request containing the uploaded file.
     * @throws DotDataException    If there is an error processing the file upload.
     * @throws ValidationException If the named temp file doesn't exist or can't be used (400).
     */
    private void processFileUpload(
            final ContentImportParams params,
            final Map<String, Object> jobParameters,
            final HttpServletRequest request) throws DotDataException {

        try {
            final DotTempFile tempFile;
            final String fileName;
            if (UtilMethods.isSet(params.getTempFileId())) {
                tempFile = APILocator.getTempFileAPI()
                        .getTempFile(List.of(String.valueOf(jobParameters.get("userId"))),
                                params.getTempFileId())
                        .orElseThrow(() -> new ValidationException(
                                "The temporary file doesn't exist or can't be used: "
                                        + params.getTempFileId()));
                fileName = tempFile.fileName;
            } else {
                fileName = params.getContentDisposition().getFileName();
                tempFile = APILocator.getTempFileAPI().createTempFile(
                        fileName, request, params.getFileInputStream());
            }
            jobParameters.put("tempFileId", tempFile.id);
            // Lets Content Drive name a running import after a reload (#37896).
            jobParameters.put("fileName", fileName);
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
     * Runs a content import preview in the request, the way Content Search's preview does (#37896):
     * the same parameters, temp file and validation as a queued preview job, then the processor
     * itself, synchronously. Nothing is queued and nothing is written.
     *
     * @param params         the CSV file and the import form
     * @param initDataObject the initialized request data, with the user
     * @param request        the HTTP servlet request
     * @return 200 with the finished preview job, or 400 with a failed job naming the error
     * @throws DotDataException if the import data can't be read
     * @throws IOException      if the file can't be read
     */
    Response handlePreview(final ContentImportParams params, final InitDataObject initDataObject,
            final HttpServletRequest request) throws DotDataException, IOException {

        final User user = initDataObject.getUser();
        try {
            params.checkValid();
            final Map<String, Object> jobParameters =
                    createJobParameters(CMD_PREVIEW, params, user, request);
            processFileUpload(params, jobParameters, request);

            final ImportContentletsProcessor processor = new ImportContentletsProcessor();
            processor.validate(jobParameters);

            final Job job = Job.builder()
                    .id(UUIDGenerator.generateUuid())
                    .queueName(IMPORT_CONTENTLETS_QUEUE_NAME)
                    .state(JobState.RUNNING)
                    .parameters(jobParameters)
                    .progressTracker(new DefaultProgressTracker())
                    .build();
            processor.process(job);

            final Job finished = job.withState(JobState.SUCCESS)
                    .withResult(JobResult.builder()
                            .metadata(processor.getResultMetadata(job))
                            .build());
            return Response.ok(new ResponseEntityView<>(view(finished))).build();
        } catch (final JobProcessingException e) {
            // A file the processor can't read is the user's to fix, so it is answered like a
            // validation error: a failed job that names it.
            return responseForValidationException(CMD_PREVIEW, params, user, request, e);
        } catch (final Exception e) {
            if (isValidationException(e)) {
                return responseForValidationException(CMD_PREVIEW, params, user, request, e);
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
     * Lists the content types a user can import content into, as Content Search's import does:
     * every type the user has WRITE permission on, except Host, sorted by name. When a search is
     * given, only the types whose name or variable contains it, ignoring case, are kept.
     * <p>
     * Permissions are resolved for the whole list in one round-trip, rather than once per type.
     *
     * @param user   the user the import would run as
     * @param filter text the name or variable must contain, or {@code null} for every type
     * @return the writable content types, never {@code null}
     * @throws DotDataException     if the content types or their permissions can't be read
     * @throws DotSecurityException if the user can't list content types
     */
    List<ImportContentTypeView> getWritableContentTypes(final User user, final String filter)
            throws DotDataException, DotSecurityException {
        final String search = UtilMethods.isSet(filter) ? filter.trim().toLowerCase() : null;
        final List<ContentType> types = APILocator.getContentTypeAPI(user, false).findAll();
        return APILocator.getPermissionAPI()
                .filterCollection(types, PermissionAPI.PERMISSION_WRITE, user, false)
                .stream()
                .filter(type -> !Host.HOST_VELOCITY_VAR_NAME.equalsIgnoreCase(type.variable()))
                .filter(type -> search == null
                        || type.name().toLowerCase().contains(search)
                        || type.variable().toLowerCase().contains(search))
                .sorted(Comparator.comparing(ContentType::name, String.CASE_INSENSITIVE_ORDER))
                .map(type -> new ImportContentTypeView(type.id(), type.variable(), type.name(),
                        type.baseType().name(), type.icon()))
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