package com.dotcms.jobs.business.processor.impl;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

@Queue("FileReader")
public class FileReaderJob implements JobProcessor, Cancellable {

    boolean working = true;

    @Override
    public void process(Job job) {
        // Retrieve job parameters
        working = true;
        Logger.info(this.getClass(), "Processing job: " + job.id());
        Map<String, Object> params = job.parameters();
        String tempFileId = (String) params.get("tempFileId");
        final Object nLinesRaw = params.get("nLines");
        if(!(nLinesRaw instanceof String)) {
            Logger.error(this.getClass(), "Parameter 'nLines' is required.");
            return;
        }

        final Object requestFingerPrintRaw = params.get("requestFingerPrint");
        if(!(requestFingerPrintRaw instanceof String)) {
            Logger.error(this.getClass(), "Parameter 'requestFingerPrint' is required.");
            return;
        }
        final String requestFingerPrint = (String) requestFingerPrintRaw;

        int nLines = Integer.parseInt((String) nLinesRaw);
        // Validate required parameters
        if (tempFileId == null || nLines <= 0) {
            Logger.error(this.getClass(), "Parameters 'tempFileId' and 'nLines' (greater than zero) are required.");
            return;
        }


        final TempFileAPI tempFileAPI = APILocator.getTempFileAPI();
        final Optional<DotTempFile> tempFile = tempFileAPI.getTempFile(List.of(requestFingerPrint), tempFileId);
        if (tempFile.isEmpty()) {
            Logger.error(this.getClass(), "Temporary file not found: " + tempFileId);
            return;
        }
        final DotTempFile dotTempFile = tempFile.get();
        try (BufferedReader reader = new BufferedReader(new FileReader(dotTempFile.file))) {
            String line;
            int lineCount = 0;
            int totalLines = 0;

            Logger.info(this.getClass(), "Starting to read the file: " + dotTempFile.file.getName());

            while (working && (line = reader.readLine()) != null) {
                lineCount++;
                totalLines++;

                // Print the line when the counter reaches nLines
                if (lineCount == nLines) {
                    Logger.info(this.getClass(), "Line " + totalLines + ": " + line);
                    lineCount = 0; // Reset the counter
                }
                Thread.sleep(1000); // Simulate processing time
            }

            Logger.info(this.getClass(), "Reading completed. Total lines read: " + totalLines);

        } catch (IOException e) {
            Logger.error(this.getClass(), "Error reading the file: " + e.getMessage());
        } catch (Exception e) {
            Logger.error(this.getClass(), "Unexpected error during processing: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getResultMetadata(Job job) {
        return Map.of();
    }

    @Override
    public void cancel(Job job) throws JobCancellationException {
        Logger.info(this.getClass(), "Job cancelled: " + job.id());

        working = false;
    }



}
