package com.dotcms.jobs.business.processor.impl;

import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotmarketing.util.Logger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

@Queue("FileReader")
public class FileReaderJob implements JobProcessor, Cancellable {

    boolean working = true;

    @Override
    public void process(Job job) {
        // Retrieve job parameters
        Logger.info(this.getClass(), "Processing job: " + job.id());
        Map<String, Object> params = job.parameters();
        String filePath = (String) params.get("filePath");
        final Object nLinesRaw = params.get("nLines");
        if(!(nLinesRaw instanceof String)) {
            Logger.error(this.getClass(), "Parameter 'nLines' is required.");
            return;
        }
        int nLines = Integer.parseInt((String) nLinesRaw);
        // Validate required parameters
        if (filePath == null || nLines <= 0) {
            Logger.error(this.getClass(), "Parameters 'filePath' and 'nLines' (greater than zero) are required.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineCount = 0;
            int totalLines = 0;

            Logger.info(this.getClass(), "Starting to read the file: " + filePath);

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
