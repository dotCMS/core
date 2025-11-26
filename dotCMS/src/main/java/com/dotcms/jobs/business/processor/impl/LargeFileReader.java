package com.dotcms.jobs.business.processor.impl;

import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.jobs.business.util.JobUtil;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.Dependent;

/**
 * This class reads a large file and prints the content to the log.
 * It is here for the sole purpose of demonstrating the job queue system.
 */
@Queue("demo")
@Dependent
public class LargeFileReader implements JobProcessor, Cancellable {

    public static final int LOG_EVERY_LINES = 1;
    public static final int DEFAULT_MAX_LINES = 2000;
    private boolean working = true;

    @Override
    public void process(Job job) {
        // Retrieve job parameters
        working = true;
        Logger.info(this.getClass(), "Processing job: " + job.id());
        Map<String, Object> params = job.parameters();

        Optional<DotTempFile> tempFile = JobUtil.retrieveTempFile(job);
        if (tempFile.isEmpty()) {
            Logger.error(this.getClass(), "Unable to retrieve the temporary file. Quitting the job.");
            throw new DotRuntimeException("Unable to retrieve the temporary file.");
        }

        final int nLines = linesParam(params).orElse(LOG_EVERY_LINES);
        final int maxLines = maxLinesParam(params).orElse(DEFAULT_MAX_LINES);
        final DotTempFile dotTempFile = tempFile.get();

        doReadLargeFile(dotTempFile, nLines, maxLines, job);

        if (!working) {
            Logger.info(this.getClass(), "Job cancelled: " + job.id());
            // Adding some delay to simulate some cancellation processing, this demo is too fast
            delay(3000);
        }
    }

    /**
     * Process the job
     * @param dotTempFile temporary file
     * @param nLines number of lines to read and print
     */
    private void doReadLargeFile(DotTempFile dotTempFile, int nLines, int maxLines ,final Job job) {
        final Long totalCount = countLines(dotTempFile);
        if (totalCount == null || totalCount == 0 ) {
            Logger.error(this.getClass(), "No lines in the file or unable to count lines: " + dotTempFile.file.getName());
            return;
        }
        Logger.info(this.getClass(), "Total lines in the file: " + totalCount);
        final Optional<ProgressTracker> progressTracker = job.progressTracker();
        try (BufferedReader reader = new BufferedReader(new FileReader(dotTempFile.file))) {

                String line;
                int lineCount = 0;
                int readCount = 0;

                Logger.info(this.getClass(),
                        "Starting to read the file: " + dotTempFile.file.getName());

                while (working && (line = reader.readLine()) != null) {
                    lineCount++;
                    readCount++;

                    // Print the line when the counter reaches nLines
                    if (lineCount == nLines) {
                        lineCount = 0; // Reset the counter
                        Logger.debug(this.getClass(), line);
                        delay(1000);
                    }
                    final float progressPercentage = ((float) readCount / totalCount);
                    progressTracker.ifPresent(tracker -> tracker.updateProgress(progressPercentage));
                    if (readCount >= maxLines) {
                        Logger.info(this.getClass(), "Max lines reached. Stopping the job.");
                        break;
                    }
                }

                Logger.info(this.getClass(), "Reading completed. Total lines read: " + readCount);
            } catch (Exception e) {
                Logger.error(this.getClass(),
                        "Unexpected error during processing: " + e.getMessage());
            }
    }

    /**
     * Count the number of lines in the file
     * @param dotTempFile temporary file
     * @return the number of lines in the file
     */
    private Long countLines(DotTempFile dotTempFile) {
        long totalCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(dotTempFile.file))) {
            totalCount = reader.lines().count();
            if (totalCount == 0) {
                Logger.info(this.getClass(), "No lines in the file: " + dotTempFile.file.getName());
            }
        } catch (Exception e) {
            Logger.error(this.getClass(), "Unexpected error during processing: " + e.getMessage());
        }
        return totalCount;
    }

    private void delay(final long millis) {
        Try.of(()->{
            Thread.sleep(millis);
            return null;
        }).onFailure(e->Logger.error(this.getClass(), "Error during delay", e));
    }

    /**
     * Retrieve the maximum number of lines to read from the parameters
     *
     * @param params input parameters
     * @return the maximum number of lines to read
     */
    Optional<Integer> maxLinesParam(Map<String, Object> params) {
        final Object maxLinesRaw = params.get("maxLines");
        if (!(maxLinesRaw instanceof String)) {
            return Optional.empty();
        }

        int maxLines;
        try {
            maxLines = Integer.parseInt((String) maxLinesRaw);
        } catch (NumberFormatException e) {
            Logger.error(this.getClass(), "Parameter 'maxLines' must be a valid integer.", e);
            return Optional.empty();
        }

        // Validate required parameters
        if (maxLines <= 0) {
            Logger.error(this.getClass(), "Parameters 'maxLines' is required.");
            return Optional.empty();
        }

        return Optional.of(maxLines);
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
            Logger.error(this.getClass(), "Parameters 'nLines' is required.");
            return Optional.empty();
        }

        return Optional.of(nLines);
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