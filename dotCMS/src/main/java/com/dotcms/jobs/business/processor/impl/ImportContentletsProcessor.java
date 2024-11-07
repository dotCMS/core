package com.dotcms.jobs.business.processor.impl;

import com.dotcms.contenttype.exception.NotFoundInDbException;
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
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.action.ImportAuditUtil;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.ImportUtil;
import com.dotmarketing.util.Logger;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;

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

        final var fileToImport = tempFile.get().file;
        final long totalLines = totalLines(job, fileToImport);

        // Create a progress callback function
        final var progressTracker = job.progressTracker().orElseThrow(
                () -> new JobProcessingException(job.id(), "Progress tracker not found")
        );
        final LongConsumer progressCallback = processedLines -> {
            float progressPercentage = (float) processedLines / totalLines;
            // This ensures the progress is between 0.0 and 1.0
            progressTracker.updateProgress(Math.min(1.0f, Math.max(0.0f, progressPercentage)));
        };

        // Handle the import operation based on the command, by default any command that is not
        // "publish" is considered preview.
        final boolean isPublish = CMD_PUBLISH.equals(command);
        handleImport(!isPublish, job, fileToImport, user, progressCallback);

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
     * Handles the content import. Depending on the preview flag, this method will either analyze
     * the content for potential issues or perform the actual import operation.
     *
     * @param preview          Flag indicating whether the operation is a preview or publish
     * @param job              The import job configuration
     * @param fileToImport     The CSV file to be imported
     * @param user             The user performing the import
     * @param progressCallback Callback for tracking import progress
     */
    private void handleImport(final boolean preview, final Job job, final File fileToImport,
            final User user, final LongConsumer progressCallback) {

        if (!preview) {
            AdminLogger.log(
                    ImportContentletsProcessor.class, "process",
                    "Importing Contentlets", user
            );
        }

        try (Reader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(fileToImport),
                        Charset.defaultCharset()))) {

            CsvReader csvReader = createCsvReader(reader);

            final var importResults = processImport(preview, job, user, csvReader,
                    progressCallback);
            resultMetadata = new HashMap<>(importResults);
        } catch (Exception e) {

            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException he) {
                Logger.error(this, he.getMessage(), he);
            }

            final var errorMessage = String.format("An error occurred when %s the CSV file.",
                    preview ? "analyzing" : "importing");
            Logger.error(this, errorMessage, e);
            throw new JobProcessingException(job.id(), errorMessage, e);
        } finally {
            final var importId = jobIdToLong(job.id());
            ImportAuditUtil.cancelledImports.remove(importId);
        }
    }

    /**
     * Executes the content import for a preview or publish operation. This method processes the
     * CSV file and imports the content into dotCMS or reviews the content for potential issues.
     *
     * @param preview                  - Flag indicating whether the operation is a preview or publish
     * @param job                      - The {@link Job} being processed.
     * @param user                     - The {@link User} performing this action.
     * @param csvReader                - The actual data contained in the CSV file.
     * @param progressCallback         - The callback function to update the progress of the job.
     * @return The status of the content import performed by dotCMS. This provides information
     * regarding inconsistencies, errors, warnings and/or precautions to the user.
     * @throws DotDataException An error occurred when importing the CSV file.
     */
    private Map<String, List<String>> processImport(final boolean preview, final Job job,
            final User user, final CsvReader csvReader, final LongConsumer progressCallback)
            throws DotDataException, IOException, DotSecurityException {

        final var currentSiteId = getSiteIdentifier(job);
        final var currentSiteName = getSiteName(job);
        final var contentType = findContentType(job);
        final var fields = getFields(job);
        final var language = findLanguage(job);
        final var workflowActionId = getWorkflowActionId(job);
        final var httpReq = JobUtil.generateMockRequest(user, currentSiteName);
        final var importId = jobIdToLong(job.id());

        // Read headers and process language columns for multilingual imports
        CsvHeaderInfo headerInfo = readHeaders(job, language == null, csvReader);

        Logger.info(this, String.format("-------- Starting Content Import %s -------- ",
                preview ? "Preview" : "Process"));
        Logger.info(this, String.format("-> Content Type: %s", contentType.variable()));

        return ImportUtil.importFile(importId, currentSiteId, contentType.id(), fields, preview,
                language == null, user, language == null ? -1 : language.getId(),
                headerInfo.headers, csvReader, headerInfo.languageCodeColumn,
                headerInfo.countryCodeColumn, workflowActionId, httpReq, progressCallback);
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
     * Retrieves the language from the job parameters.
     *
     * @param job The job containing the parameters
     * @return The language ID or ISO code
     */
    private String getLanguage(final Job job) {

        if (!job.parameters().containsKey(PARAMETER_LANGUAGE)
                || job.parameters().get(PARAMETER_LANGUAGE) == null) {
            return null;
        }

        return (String) job.parameters().get(PARAMETER_LANGUAGE);
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
        if (fields instanceof List) {
            return ((List<String>) fields).toArray(new String[0]);
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

        // Validating the language (will throw an exception if it doesn't)
        final Language language = findLanguage(job);

        if (getContentType(job) != null && getContentType(job).isEmpty()) {
            final var errorMessage = "A Content Type id or variable is required";
            Logger.error(this.getClass(), errorMessage);
            throw new JobValidationException(job.id(), errorMessage);
        } else if (getWorkflowActionId(job) != null && getWorkflowActionId(job).isEmpty()) {
            final var errorMessage = "A Workflow Action id is required";
            Logger.error(this.getClass(), errorMessage);
            throw new JobValidationException(job.id(), errorMessage);
        } else if (language == null && getFields(job).length == 0) {
            final var errorMessage =
                    "A key identifying the different Language versions of the same "
                            + "content must be defined when importing multilingual files.";
            Logger.error(this, errorMessage);
            throw new JobValidationException(job.id(), errorMessage);
        }

        try {

            // Make sure the content type exist (will throw an exception if it doesn't)
            final var contentTypeFound = findContentType(job);

            // Security measure to prevent invalid attempts to import a host.
            final ContentType hostContentType = APILocator.getContentTypeAPI(
                    APILocator.systemUser()).find(Host.HOST_VELOCITY_VAR_NAME);
            final boolean isHost = (hostContentType.id().equals(contentTypeFound.id()));
            if (isHost) {
                final var errorMessage = "Invalid attempt to import a host.";
                Logger.error(this, errorMessage);
                throw new JobValidationException(job.id(), errorMessage);
            }
        } catch (DotSecurityException | DotDataException e) {
            throw new JobProcessingException(job.id(), "Error validating content type", e);
        }
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
     * Reads and processes headers from the CSV file. Handles both single and multilingual content
     * imports.
     *
     * @param job            The current import job
     * @param isMultilingual Flag indicating whether the import is multilingual
     * @param csvReader      The CSV reader containing the file data
     * @return CsvHeaderInfo containing processed header information
     * @throws IOException if an error occurs reading the CSV file
     */
    private CsvHeaderInfo readHeaders(final Job job, boolean isMultilingual, CsvReader csvReader)
            throws IOException {

        if (isMultilingual) {
            return processMultilingualHeaders(job, csvReader);
        }

        return new CsvHeaderInfo(null, -1, -1);
    }

    /**
     * Locates language-related columns in CSV headers.
     *
     * @param job     The current import job
     * @param headers Array of CSV header strings
     * @return CsvHeaderInfo containing the positions of language and country code columns
     */
    private CsvHeaderInfo findLanguageColumnsInHeaders(Job job, String[] headers) {

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
     * Processes headers for multilingual content imports.
     *
     * @param job       The current import job
     * @param csvReader The CSV reader to process headers from
     * @return CsvHeaderInfo containing processed multilingual header information
     * @throws IOException if an error occurs reading the CSV file
     */
    private CsvHeaderInfo processMultilingualHeaders(final Job job, final CsvReader csvReader)
            throws IOException {

        if (!csvReader.readHeaders()) {
            final var errorMessage = "An error occurred when attempting to read the CSV file headers.";
            Logger.error(this, errorMessage);
            throw new JobProcessingException(job.id(), errorMessage);
        }

        String[] headers = csvReader.getHeaders();
        return findLanguageColumnsInHeaders(job, headers);
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
     * Retrieves the existing content type based on an id or variable.
     *
     * @param job The current import job.
     * @return The existing content type if found, otherwise fails with an exception.
     * @throws DotSecurityException If there are security restrictions preventing the evaluation.
     */
    private ContentType findContentType(final Job job)
            throws DotSecurityException {

        final var contentTypeIdOrVar = getContentType(job);
        final User user;

        // Retrieving the user requesting the import
        try {
            user = getUser(job);
        } catch (DotDataException e) {
            final var errorMessage = "Error retrieving user.";
            Logger.error(this.getClass(), errorMessage);
            throw new JobProcessingException(job.id(), errorMessage, e);
        }

        try {
            return APILocator.getContentTypeAPI(user, true)
                    .find(contentTypeIdOrVar);
        } catch (NotFoundInDbException e) {
            final var errorMessage = String.format(
                    "Content Type [%s] not found.", contentTypeIdOrVar
            );
            Logger.error(this.getClass(), errorMessage);
            throw new JobValidationException(job.id(), errorMessage);
        } catch (DotDataException e) {
            final var errorMessage = String.format(
                    "Error finding Content Type [%s].", contentTypeIdOrVar
            );
            Logger.error(this.getClass(), errorMessage);
            throw new JobProcessingException(job.id(), errorMessage, e);
        }
    }

    /**
     * Retrieves the existing language based on an id or ISO code.
     *
     * @param job The current import job.
     * @return The existing language if found, otherwise fails with an exception.
     */
    private Language findLanguage(final Job job) {

        // Read the language from the job parameters
        final var languageIsoOrId = getLanguage(job);
        if (languageIsoOrId == null || languageIsoOrId.equals("-1")) {
            return null;
        }

        // Retrieve the language based on the provided ISO code or ID
        final Language language = APILocator.getLanguageAPI().getLanguage(languageIsoOrId);
        if (language != null && language.getId() > 0) {
            return language;
        }

        final var errorMessage = String.format(
                "Language [%s] not found.", languageIsoOrId
        );
        Logger.error(this.getClass(), errorMessage);
        throw new JobValidationException(job.id(), errorMessage);
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
