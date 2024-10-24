package com.dotcms.jobs.business.processor.impl;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.ExponentialBackoffRetryPolicy;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.NoRetryPolicy;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.jobs.business.util.JobUtil;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.action.ImportAuditUtil;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.ImportUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.hash.Hashing;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;
import javax.servlet.http.HttpServletRequest;

/**
 * Processor implementation for handling content import operations in dotCMS. This class provides
 * functionality to import content from CSV files, with support for both preview and publish
 * operations, as well as multilingual content handling.
 *
 * <p>The processor implements both {@link JobProcessor} and {@link Cancellable} interfaces to
 * provide job processing and cancellation capabilities. It's annotated with {@link Queue} to
 * specify the queue name and {@link ExponentialBackoffRetryPolicy} to define retry behavior.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Support for both preview and publish operations</li>
 *   <li>Multilingual content import capabilities</li>
 *   <li>Progress tracking during import</li>
 *   <li>Cancellation support</li>
 *   <li>Validation of import parameters and content</li>
 * </ul>
 *
 * @see JobProcessor
 * @see Cancellable
 * @see Queue
 * @see ExponentialBackoffRetryPolicy
 */
@Queue("importContentlets")
@NoRetryPolicy
public class ImportContentletsProcessor implements JobProcessor, Cancellable {

    private static final String PARAMETER_LANGUAGE = "language";
    private static final String PARAMETER_FIELDS = "fields";
    private static final String PARAMETER_USER_ID = "userId";
    private static final String PARAMETER_SITE_IDENTIFIER = "siteIdentifier";
    private static final String PARAMETER_SITE_NAME = "siteName";
    private static final String PARAMETER_CONTENT_TYPE = "contentType";
    private static final String PARAMETER_WORKFLOW_ACTION_ID = "workflowActionId";
    private static final String PARAMETER_CMD = Constants.CMD;
    private static final String CMD_PREVIEW = com.dotmarketing.util.Constants.PREVIEW;
    private static final String CMD_PUBLISH = com.dotmarketing.util.Constants.PUBLISH;

    private static final String LANGUAGE_CODE_HEADER = "languageCode";
    private static final String COUNTRY_CODE_HEADER = "countryCode";

    /**
     * Flag to track cancellation requests for the current import operation.
     */
    private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);

    /**
     * Storage for metadata about the import operation results.
     */
    private Map<String, Object> resultMetadata = new HashMap<>();

    /**
     * Processes a content import job. This method serves as the main entry point for the import
     * operation and handles both preview and publish modes.
     *
     * <p>The method performs the following steps:</p>
     * <ol>
     *   <li>Validates the input parameters and retrieves the necessary user information</li>
     *   <li>Retrieves and validates the import file</li>
     *   <li>Sets up progress tracking</li>
     *   <li>Executes either preview or publish operation based on the command</li>
     *   <li>Ensures proper progress updates throughout the process</li>
     * </ol>
     *
     * @param job The job containing import parameters and configuration
     * @throws JobProcessingException if any error occurs during processing
     */
    @Override
    public void process(final Job job) throws JobProcessingException {

        final String command = getCommand(job);

        final User user;
        try {
            user = getUser(job);
        } catch (Exception e) {
            Logger.error(this, "Error retrieving user", e);
            throw new JobProcessingException(job.id(), "Error retrieving user", e);
        }

        Logger.info(this, String.format("Processing import contentlets job [%s], "
                + "with command [%s] and user [%s]", job.id(), command, user.getUserId()));

        // Retrieving the import file
        Optional<DotTempFile> tempFile = JobUtil.retrieveTempFile(job);
        if (tempFile.isEmpty()) {
            Logger.error(this.getClass(), "Unable to retrieve the import file. Quitting the job.");
            throw new JobValidationException(job.id(), "Unable to retrieve the import file.");
        }

        // Validate the job has the required data
        validate(job);

        final var language = getLanguage(job);
        final var fileToImport = tempFile.get().file;
        final long totalLines = totalLines(job, fileToImport);
        final Charset charset = language == -1 ?
                Charset.defaultCharset() : FileUtil.detectEncodeType(fileToImport);

        // Create a progress callback function
        final var progressTracker = job.progressTracker().orElseThrow(
                () -> new JobProcessingException(job.id(), "Progress tracker not found")
        );
        final LongConsumer progressCallback = processedLines -> {
            float progressPercentage = (float) processedLines / totalLines;
            // This ensures the progress is between 0.0 and 1.0
            progressTracker.updateProgress(Math.min(1.0f, Math.max(0.0f, progressPercentage)));
        };

        if (CMD_PREVIEW.equals(command)) {
            handlePreview(job, language, fileToImport, charset, user, progressCallback);
        } else if (CMD_PUBLISH.equals(command)) {
            handlePublish(job, language, fileToImport, charset, user, progressCallback);
        }

        if (!cancellationRequested.get()) {
            // Ensure the progress is at 100% when the job is done
            progressTracker.updateProgress(1.0f);
        }
    }

    /**
     * Handles cancellation requests for the import operation. When called, it marks the operation
     * for cancellation.
     *
     * @param job The job to be cancelled
     * @throws JobCancellationException if any error occurs during cancellation
     */
    @Override
    public void cancel(Job job) throws JobCancellationException {

        Logger.info(this.getClass(), "Job cancellation requested: " + job.id());
        cancellationRequested.set(true);

        final var importId = jobIdToLong(job.id());
        ImportAuditUtil.cancelledImports.put(importId, Calendar.getInstance().getTime());
    }

    /**
     * Retrieves metadata about the import operation results.
     *
     * @param job The job whose metadata is being requested
     * @return A map containing result metadata, or an empty map if no metadata is available
     */
    @Override
    public Map<String, Object> getResultMetadata(Job job) {

        if (resultMetadata.isEmpty()) {
            return Collections.emptyMap();
        }

        return resultMetadata;
    }

    /**
     * Handles the preview phase of content import. This method analyzes the CSV file and provides
     * information about potential issues without actually importing the content.
     *
     * @param job              The import job configuration
     * @param language         The target language for import
     * @param fileToImport     The CSV file to be imported
     * @param charset          The character encoding of the import file
     * @param user             The user performing the import
     * @param progressCallback Callback for tracking import progress
     */
    private void handlePreview(final Job job, long language, final File fileToImport,
            final Charset charset, final User user, final LongConsumer progressCallback) {

        try {
            try (Reader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(fileToImport), charset))) {

                CsvReader csvReader = createCsvReader(reader);
                CsvHeaderInfo headerInfo = processHeadersBasedOnLanguage(job, language, csvReader);

                final var previewResult = generatePreview(job, user,
                        headerInfo.headers, csvReader, headerInfo.languageCodeColumn,
                        headerInfo.countryCodeColumn, progressCallback);
                resultMetadata = new HashMap<>(previewResult);
            }
        } catch (Exception e) {

            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException he) {
                Logger.error(this, he.getMessage(), he);
            }

            final var errorMessage = "An error occurred when analyzing the CSV file.";
            Logger.error(this, errorMessage, e);
            throw new JobProcessingException(job.id(), errorMessage, e);
        }
    }

    /**
     * Handles the publish phase of content import. This method performs the actual content import
     * operation, creating or updating content based on the CSV file.
     *
     * @param job              The import job configuration
     * @param language         The target language for import
     * @param fileToImport     The CSV file to be imported
     * @param charset          The character encoding of the import file
     * @param user             The user performing the import
     * @param progressCallback Callback for tracking import progress
     */
    private void handlePublish(final Job job, long language, final File fileToImport,
            final Charset charset, final User user, final LongConsumer progressCallback) {

        AdminLogger.log(
                ImportContentletsProcessor.class, "process",
                "Importing Contentlets", user
        );

        try {
            try (Reader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(fileToImport), charset))) {

                CsvReader csvReader = createCsvReader(reader);
                CsvHeaderInfo headerInfo = readPublishHeaders(language, csvReader);

                final var importResults = processFile(job, user, headerInfo.headers, csvReader,
                        headerInfo.languageCodeColumn, headerInfo.countryCodeColumn,
                        progressCallback);
                resultMetadata = new HashMap<>(importResults);
            }
        } catch (Exception e) {

            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException he) {
                Logger.error(this, he.getMessage(), he);
            }

            final var errorMessage = "An error occurred when importing the CSV file.";
            Logger.error(this, errorMessage, e);
            throw new JobProcessingException(job.id(), errorMessage, e);
        } finally {
            final var importId = jobIdToLong(job.id());
            ImportAuditUtil.cancelledImports.remove(importId);
        }
    }

    /**
     * Reads and analyzes the content of the CSV import file to determine potential errors,
     * inconsistencies or warnings, and provide the user with useful information regarding the
     * contents of the file.
     *
     * @param job                      - The {@link Job} being processed.
     * @param user                     - The {@link User} performing this action.
     * @param csvHeaders               - The headers that make up the CSV file.
     * @param csvReader                - The actual data contained in the CSV file.
     * @param languageCodeHeaderColumn - The column name containing the language code.
     * @param countryCodeHeaderColumn  - The column name containing the country code.
     * @param progressCallback         - The callback function to update the progress of the job.
     * @throws DotDataException An error occurred when analyzing the CSV file.
     */
    private Map<String, List<String>> generatePreview(final Job job, final User user,
            final String[] csvHeaders, final CsvReader csvReader,
            final int languageCodeHeaderColumn, int countryCodeHeaderColumn,
            final LongConsumer progressCallback) throws DotDataException {

        final var currentSiteId = getSiteIdentifier(job);
        final var currentSiteName = getSiteName(job);
        final var contentType = getContentType(job);
        final var fields = getFields(job);
        final var language = getLanguage(job);
        final var workflowActionId = getWorkflowActionId(job);
        final var httpReq = getRequest(user, currentSiteName);

        Logger.info(this, "-------- Starting Content Import Preview -------- ");
        Logger.info(this, String.format("-> Content Type ID: %s", contentType));

        return ImportUtil.importFile(0L, currentSiteId, contentType, fields, true,
                (language == -1), user, language, csvHeaders, csvReader, languageCodeHeaderColumn,
                countryCodeHeaderColumn, workflowActionId, httpReq, progressCallback);
    }

    /**
     * Executes the content import process after the review process has been run and displayed to
     * the user.
     *
     * @param job                      - The {@link Job} being processed.
     * @param user                     - The {@link User} performing this action.
     * @param csvHeaders               - The headers that make up the CSV file.
     * @param csvReader                - The actual data contained in the CSV file.
     * @param languageCodeHeaderColumn - The column name containing the language code.
     * @param countryCodeHeaderColumn  - The column name containing the country code.
     * @param progressCallback         - The callback function to update the progress of the job.
     * @return The status of the content import performed by dotCMS. This provides information
     * regarding inconsistencies, errors, warnings and/or precautions to the user.
     * @throws DotDataException An error occurred when importing the CSV file.
     */
    private HashMap<String, List<String>> processFile(final Job job, final User user,
            final String[] csvHeaders, final CsvReader csvReader,
            final int languageCodeHeaderColumn, final int countryCodeHeaderColumn,
            final LongConsumer progressCallback) throws DotDataException {

        final var currentSiteId = getSiteIdentifier(job);
        final var currentSiteName = getSiteName(job);
        final var contentType = getContentType(job);
        final var fields = getFields(job);
        final var language = getLanguage(job);
        final var workflowActionId = getWorkflowActionId(job);
        final var httpReq = getRequest(user, currentSiteName);
        final var importId = jobIdToLong(job.id());

        Logger.info(this, "-------- Starting Content Import Process -------- ");
        Logger.info(this, String.format("-> Content Type ID: %s", contentType));

        return ImportUtil.importFile(importId, currentSiteId, contentType, fields, false,
                (language == -1), user, language, csvHeaders, csvReader, languageCodeHeaderColumn,
                countryCodeHeaderColumn, workflowActionId, httpReq, progressCallback);
    }

    /**
     * Retrieve the command from the job parameters
     *
     * @param job input job
     * @return the command from the job parameters, if not found, return the default value "preview"
     */
    private String getCommand(final Job job) {

        if (!job.parameters().containsKey(PARAMETER_CMD)) {
            return CMD_PREVIEW;
        }

        return (String) job.parameters().get(PARAMETER_CMD);
    }

    /**
     * Retrieve the user from the job parameters
     *
     * @param job input job
     * @return the user from the job parameters
     * @throws DotDataException     if an error occurs during the user retrieval
     * @throws DotSecurityException if we don't have the necessary permissions to retrieve the user
     */
    private User getUser(final Job job) throws DotDataException, DotSecurityException {
        final var userId = (String) job.parameters().get(PARAMETER_USER_ID);
        return APILocator.getUserAPI().loadUserById(userId);
    }

    /**
     * Retrieves the site identifier from the job parameters.
     *
     * @param job The job containing the parameters
     * @return The site identifier string, or null if not present in parameters
     */
    private String getSiteIdentifier(final Job job) {
        return (String) job.parameters().get(PARAMETER_SITE_IDENTIFIER);
    }

    /**
     * Retrieves the site name from the job parameters.
     *
     * @param job The job containing the parameters
     * @return The site name string, or null if not present in parameters
     */
    private String getSiteName(final Job job) {
        return (String) job.parameters().get(PARAMETER_SITE_NAME);
    }

    /**
     * Retrieves the content type from the job parameters.
     *
     * @param job The job containing the parameters
     * @return The content type string, or null if not present in parameters
     */
    private String getContentType(final Job job) {
        return (String) job.parameters().get(PARAMETER_CONTENT_TYPE);
    }

    /**
     * Retrieves the workflow action ID from the job parameters.
     *
     * @param job The job containing the parameters
     * @return The workflow action ID string, or null if not present in parameters
     */
    private String getWorkflowActionId(final Job job) {
        return (String) job.parameters().get(PARAMETER_WORKFLOW_ACTION_ID);
    }

    /**
     * Retrieves the language setting from the job parameters. Handles both string and long
     * parameter types.
     *
     * @param job The job containing the parameters
     * @return The language ID as a long, or -1 if not specified
     */
    private long getLanguage(final Job job) {

        if (!job.parameters().containsKey(PARAMETER_LANGUAGE)
                || job.parameters().get(PARAMETER_LANGUAGE) == null) {
            return -1;
        }

        final Object language = job.parameters().get(PARAMETER_LANGUAGE);

        if (language instanceof String) {
            return Long.parseLong((String) language);
        }

        return (long) language;
    }

    /**
     * Retrieves the fields array from the job parameters.
     *
     * @param job The job containing the parameters
     * @return An array of field strings, or an empty array if no fields are specified
     */
    public String[] getFields(final Job job) {

        if (!job.parameters().containsKey(PARAMETER_FIELDS)
                || job.parameters().get(PARAMETER_FIELDS) == null) {
            return new String[0];
        }

        final var fields = job.parameters().get(PARAMETER_FIELDS);
        if (fields instanceof ArrayList) {
            return ((ArrayList<String>) fields).toArray(new String[0]);
        }
        
        return (String[]) fields;
    }

    /**
     * Validates the job parameters and content type. Performs security checks to prevent
     * unauthorized host imports.
     *
     * @param job The job to validate
     * @throws JobValidationException if validation fails
     * @throws JobProcessingException if an error occurs during content type validation
     */
    private void validate(final Job job) {

        if (getContentType(job) != null && getContentType(job).isEmpty()) {
            Logger.error(this.getClass(), "A Content Type is required");
            throw new JobValidationException(job.id(), "A Content Type is required");
        } else if (getWorkflowActionId(job) != null && getWorkflowActionId(job).isEmpty()) {
            Logger.error(this.getClass(), "Workflow action type is required");
            throw new JobValidationException(job.id(), "Workflow action type is required");
        }

        // Security measure to prevent invalid attempts to import a host.
        try {
            final ContentType hostContentType = APILocator.getContentTypeAPI(
                    APILocator.systemUser()).find(Host.HOST_VELOCITY_VAR_NAME
            );
            final boolean isHost = (hostContentType.id().equals(getContentType(job)));
            if (isHost) {
                Logger.error(this, "Invalid attempt to import a host.");
                throw new JobValidationException(job.id(), "Invalid attempt to import a host.");
            }
        } catch (DotSecurityException | DotDataException e) {
            throw new JobProcessingException(job.id(), "Error validating content type", e);
        }
    }

    /**
     * Creates or retrieves an HttpServletRequest for the import operation. Uses thread-local
     * request if available, otherwise creates a mock request with the specified user and site
     * information.
     *
     * @param user     The user performing the import
     * @param siteName The name of the site for the import
     * @return An HttpServletRequest instance configured for the import operation
     */
    private HttpServletRequest getRequest(final User user, final String siteName) {

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

    /**
     * Utility method to convert a job ID to a long value for internal processing. Uses FarmHash for
     * efficient hash generation and distribution.
     *
     * @param jobId The string job identifier
     * @return A long value representing the job ID
     */
    public static long jobIdToLong(final String jobId) {

        // Use FarmHash for good distribution and speed
        long hashValue = Hashing.farmHashFingerprint64()
                .hashString(jobId, StandardCharsets.UTF_8).asLong();

        // Ensure the value is positive (in the upper half of the bigint range)
        return Math.abs(hashValue);
    }

    /**
     * Count the number of lines in the file
     *
     * @param dotTempFile temporary file
     * @return the number of lines in the file
     */
    private Long totalLines(final Job job, final File dotTempFile) {

        long totalCount;
        try (BufferedReader reader = new BufferedReader(new FileReader(dotTempFile))) {
            totalCount = reader.lines().count();
            if (totalCount == 0) {
                Logger.info(this.getClass(),
                        "No lines in CSV import file: " + dotTempFile.getName());
            }
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Error calculating total lines in CSV import file: " + e.getMessage());
            throw new JobProcessingException(job.id(),
                    "Error calculating total lines in CSV import file", e);
        }

        return totalCount;
    }

    /**
     * Reads and processes headers for publishing operation.
     *
     * @param language  The target language for import
     * @param csvreader The CSV reader containing the file data
     * @return CsvHeaderInfo containing processed header information
     * @throws IOException if an error occurs reading the CSV file
     */
    private CsvHeaderInfo readPublishHeaders(long language, CsvReader csvreader)
            throws IOException {
        if (language == -1 && csvreader.readHeaders()) {
            return findLanguageColumnsInHeaders(csvreader.getHeaders());
        }
        return new CsvHeaderInfo(null, -1, -1);
    }

    /**
     * Locates language-related columns in CSV headers.
     *
     * @param headers Array of CSV header strings
     * @return CsvHeaderInfo containing the positions of language and country code columns
     */
    private CsvHeaderInfo findLanguageColumnsInHeaders(String[] headers) {

        int languageCodeColumn = -1;
        int countryCodeColumn = -1;

        for (int column = 0; column < headers.length; ++column) {
            if (headers[column].equals(LANGUAGE_CODE_HEADER)) {
                languageCodeColumn = column;
            }
            if (headers[column].equals(COUNTRY_CODE_HEADER)) {
                countryCodeColumn = column;
            }
            if (languageCodeColumn != -1 && countryCodeColumn != -1) {
                break;
            }
        }

        return new CsvHeaderInfo(headers, languageCodeColumn, countryCodeColumn);
    }

    /**
     * Creates a CSV reader with appropriate configuration for import operations.
     *
     * @param reader The source reader for CSV content
     * @return A configured CsvReader instance
     */
    private CsvReader createCsvReader(final Reader reader) {
        CsvReader csvreader = new CsvReader(reader);
        csvreader.setSafetySwitch(false);
        return csvreader;
    }

    /**
     * Processes CSV headers based on the specified language configuration.
     *
     * @param job       The current import job
     * @param language  The target language for import
     * @param csvReader The CSV reader to process headers from
     * @return CsvHeaderInfo containing processed header information
     * @throws IOException if an error occurs reading the CSV file
     */
    private CsvHeaderInfo processHeadersBasedOnLanguage(final Job job, final long language,
            final CsvReader csvReader) throws IOException {
        if (language != -1) {
            validateLanguage(job, language);
            return new CsvHeaderInfo(null, -1, -1);
        }

        return processMultilingualHeaders(job, csvReader);
    }

    /**
     * Validates the language configuration for import operations.
     *
     * @param job      The current import job
     * @param language The language identifier to validate
     */
    private void validateLanguage(Job job, long language) {
        if (language == 0) {
            final var errorMessage = "Please select a valid Language.";
            Logger.error(this, errorMessage);
            throw new JobValidationException(job.id(), errorMessage);
        }
    }

    /**
     * Processes headers for multilingual content imports.
     *
     * @param job       The current import job
     * @param csvReader The CSV reader to process headers from
     * @return CsvHeaderInfo containing processed multilingual header information
     * @throws IOException if an error occurs reading the CSV file
     */
    private CsvHeaderInfo processMultilingualHeaders(final Job job, final CsvReader csvReader)
            throws IOException {

        if (getFields(job).length == 0) {
            final var errorMessage =
                    "A key identifying the different Language versions of the same "
                            + "content must be defined when importing multilingual files.";
            Logger.error(this, errorMessage);
            throw new JobValidationException(job.id(), errorMessage);
        }

        if (!csvReader.readHeaders()) {
            final var errorMessage = "An error occurred when attempting to read the CSV file headers.";
            Logger.error(this, errorMessage);
            throw new JobProcessingException(job.id(), errorMessage);
        }

        String[] headers = csvReader.getHeaders();
        return findLanguageColumns(job, headers);
    }

    /**
     * Locates language-related columns in CSV headers.
     *
     * @param headers Array of CSV header strings
     * @return CsvHeaderInfo containing the positions of language and country code columns
     */
    private CsvHeaderInfo findLanguageColumns(Job job, String[] headers)
            throws JobProcessingException {

        int languageCodeColumn = -1;
        int countryCodeColumn = -1;

        for (int column = 0; column < headers.length; ++column) {
            if (headers[column].equals(LANGUAGE_CODE_HEADER)) {
                languageCodeColumn = column;
            }
            if (headers[column].equals(COUNTRY_CODE_HEADER)) {
                countryCodeColumn = column;
            }
            if (languageCodeColumn != -1 && countryCodeColumn != -1) {
                break;
            }
        }

        validateLanguageColumns(job, languageCodeColumn, countryCodeColumn);
        return new CsvHeaderInfo(headers, languageCodeColumn, countryCodeColumn);
    }

    /**
     * Performs validation of language columns for multilingual imports.
     *
     * @param job                The current import job
     * @param languageCodeColumn The index of the language code column
     * @param countryCodeColumn  The index of the country code column
     * @throws JobValidationException if the required language columns are not found
     */
    private void validateLanguageColumns(Job job, int languageCodeColumn, int countryCodeColumn)
            throws JobProcessingException {
        if (languageCodeColumn == -1 || countryCodeColumn == -1) {
            final var errorMessage = "languageCode and countryCode fields are mandatory in the CSV "
                    + "file when importing multilingual content.";
            Logger.error(this, errorMessage);
            throw new JobValidationException(job.id(), errorMessage);
        }
    }

    /**
     * Container class for CSV header information, particularly for handling language-related
     * columns in multilingual imports.
     */
    private static class CsvHeaderInfo {

        final String[] headers;
        final int languageCodeColumn;
        final int countryCodeColumn;

        CsvHeaderInfo(String[] headers, int languageCodeColumn, int countryCodeColumn) {
            this.headers = headers;
            this.languageCodeColumn = languageCodeColumn;
            this.countryCodeColumn = countryCodeColumn;
        }
    }

}
