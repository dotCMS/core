package com.dotcms.jobs.business.processor.impl;

import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This class reads a large file and prints the content to the log.
 * It is here for the sole purpose of demonstrating the job queue system.
 */
@Queue("FileReader")
public class LargeFileReader implements JobProcessor, Cancellable {

    private boolean working = true;

    @Override
    public void process(Job job) {
        // Retrieve job parameters
        working = true;

        Logger.info(this.getClass(), "Processing job: " + job.id());
        Map<String, Object> params = job.parameters();

        final Optional<Integer> linesParam = linesParam(params);
        if (linesParam.isEmpty()) {
            Logger.error(this.getClass(),
                    "Unable to retrieve the number of lines to read. Quitting the job.");
            return;
        }

        Optional<DotTempFile> tempFile = tempFile(params);
        if (tempFile.isEmpty()) {
            Logger.error(this.getClass(), "Unable to retrieve the temporary. Quitting the job.");
            return;
        }

        final int nLines = linesParam.get();
        final DotTempFile dotTempFile = tempFile.get();

        doReadLargeFile(dotTempFile, nLines);
    }

    /**
     * Process the job
     * @param dotTempFile temporary file
     * @param nLines number of lines to read and print
     */
    private void doReadLargeFile(DotTempFile dotTempFile, int nLines) {
        try (BufferedReader reader = new BufferedReader(new FileReader(dotTempFile.file))) {
            String line;
            int lineCount = 0;
            int totalLines = 0;

            Logger.info(this.getClass(),
                    "Starting to read the file: " + dotTempFile.file.getName());

            while (working && (line = reader.readLine()) != null) {
                lineCount++;
                totalLines++;

                // Print the line when the counter reaches nLines
                if (lineCount == nLines) {
                    Logger.info(this.getClass(), "Line " + totalLines + ": " + line);
                    lineCount = 0; // Reset the counter
                }
            }

            Logger.info(this.getClass(), "Reading completed. Total lines read: " + totalLines);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Unexpected error during processing: " + e.getMessage());
        }
    }

    /**
     * Retrieve the number of lines to read from the parameters
     *
     * @param params input parameters
     * @return the number of lines to read
     */
    Optional<Integer> linesParam(Map<String, Object> params) {

        final Object nLinesRaw = params.get("nLines");
        if (!(nLinesRaw instanceof String)) {
            Logger.error(this.getClass(), "Parameter 'nLines' is required and must be a string.");
            return Optional.empty();
        }

        int nLines;
        try {
            nLines = Integer.parseInt((String) nLinesRaw);
        } catch (NumberFormatException e) {
            Logger.error(this.getClass(), "Parameter 'nLines' must be a valid integer.", e);
            return Optional.empty();
        }

        // Validate required parameters
        if (nLines <= 0) {
            Logger.error(this.getClass(), "Parameters 'tempFileId' are required.");
            return Optional.empty();
        }

        return Optional.of(nLines);
    }

    /**
     * Retrieve the temporary file from the parameters
     *
     * @param params input parameters
     * @return the temporary file
     */
    Optional<DotTempFile> tempFile(Map<String, Object> params) {
        // Extract parameters
        String tempFileId = (String) params.get("tempFileId");

        final Object requestFingerPrintRaw = params.get("requestFingerPrint");
        if (!(requestFingerPrintRaw instanceof String)) {
            Logger.error(this.getClass(),
                    "Parameter 'requestFingerPrint' is required and must be a string.");
            return Optional.empty();
        }
        final String requestFingerPrint = (String) requestFingerPrintRaw;

        // Retrieve the temporary file
        final TempFileAPI tempFileAPI = APILocator.getTempFileAPI();
        final Optional<DotTempFile> tempFile = tempFileAPI.getTempFile(List.of(requestFingerPrint),
                tempFileId);
        if (tempFile.isEmpty()) {
            Logger.error(this.getClass(), "Temporary file not found: " + tempFileId);
            return Optional.empty();
        }

        return tempFile;
    }

    /**
     * Provide metadata for the job result.
     * @param job The job for which to provide metadata.
     * @return The metadata for the job result.
     */
    @Override
    public Map<String, Object> getResultMetadata(Job job) {
        return Map.of();
    }

    /**
     * Cancel the job
     * @param job
     * @throws JobCancellationException
     */
    @Override
    public void cancel(Job job) throws JobCancellationException {
        Logger.info(this.getClass(), "Job cancelled: " + job.id());
        working = false;
    }


}