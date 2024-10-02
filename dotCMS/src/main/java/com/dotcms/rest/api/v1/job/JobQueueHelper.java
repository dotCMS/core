package com.dotcms.rest.api.v1.job;

import com.dotcms.jobs.business.api.JobProcessorScanner;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.jobs.business.queue.error.JobQueueDataException;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

@ApplicationScoped
public class JobQueueHelper {

    JobQueueManagerAPI jobQueueManagerAPI;

    JobProcessorScanner scanner;

    public JobQueueHelper() {
        //default constructor Mandatory for CDI
    }

    @PostConstruct
    public void onInit() {

        if(!jobQueueManagerAPI.isStarted()){
            jobQueueManagerAPI.start();
            Logger.info(this.getClass(), "JobQueueManagerAPI started");
        }
        final List<Class<? extends JobProcessor>> processors = scanner.discoverJobProcessors();
        processors.forEach(processor -> {
           try {
               final Constructor<? extends JobProcessor> declaredConstructor = processor.getDeclaredConstructor();
               final JobProcessor jobProcessor = declaredConstructor.newInstance();
               //registering the processor with the jobQueueManagerAPI
               // lower case it to avoid case
               if(processor.isAnnotationPresent(Queue.class)){
                   final Queue queue = processor.getAnnotation(Queue.class);
                   jobQueueManagerAPI.registerProcessor(queue.value(), jobProcessor);
               } else {
                  jobQueueManagerAPI.registerProcessor(processor.getName(), jobProcessor);
               }
           }catch (Exception e){
               Logger.error(this.getClass(), "Unable to register JobProcessor ", e);
           }
        });
    }

    @PreDestroy
    public void onDestroy() {
       if(jobQueueManagerAPI.isStarted()){
           try {
               jobQueueManagerAPI.close();
                Logger.info(this.getClass(), "JobQueueManagerAPI successfully closed");
           } catch (Exception e) {
               Logger.error(this.getClass(), e.getMessage(), e);
           }
       }
    }

    @Inject
    public JobQueueHelper(JobQueueManagerAPI jobQueueManagerAPI, JobProcessorScanner scanner) {
        this.jobQueueManagerAPI = jobQueueManagerAPI;
        this.scanner = scanner;
    }

    /**
     * creates a job
     * @param queueName
     * @param form
     * @return jobId
     * @throws JsonProcessingException
     * @throws DotDataException
     */
    String createJob(String queueName, JobParams form, HttpServletRequest request)
            throws JsonProcessingException, DotDataException {

        final HashMap <String, Object>in = new HashMap<>(form.getParams());
        handleUploadIfPresent(form, in, request);
        return jobQueueManagerAPI.createJob(queueName, Map.copyOf(in));
    }

    /**
     * gets a job
     * @param jobId
     * @return
     * @throws DotDataException
     */
    Job getJob(String jobId) throws DotDataException{
        return jobQueueManagerAPI.getJob(jobId);
    }

    /**
     * cancels a job
     * @param jobId
     * @throws DotDataException
     */
    void cancelJob(String jobId) throws DotDataException{
        jobQueueManagerAPI.cancelJob(jobId);
    }

    /**
     * watches a job
     * @param jobId
     * @param watcher
     */
    void watchJob(String jobId, Consumer<Job> watcher){
        jobQueueManagerAPI.watchJob(jobId, watcher);
    }

    /**
     * Retrieves a list of jobs.
     * @param page
     * @param pageSize
     * @return JobPaginatedResult
     * @throws DotDataException
     */
    JobPaginatedResult getJobs(int page, int pageSize) throws DotDataException{
        return jobQueueManagerAPI.getJobs(page, pageSize);
    }

    /**
     * Retrieves a list of jobs.
     * @param page
     * @param pageSize
     * @return JobPaginatedResult
     * @throws DotDataException
     */
    JobPaginatedResult getActiveJobs(String queueName, int page, int pageSize)
            throws JobQueueDataException {
        return jobQueueManagerAPI.getActiveJobs( queueName, page, pageSize);
    }

    /**
     * Retrieves a list of completed jobs for a specific queue within a date range.
     * @param queueName The name of the queue
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return A result object containing the list of completed jobs and pagination information.
     * @throws JobQueueDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getFailedJobs(String queueName, int page, int pageSize)
            throws JobQueueDataException {
        return jobQueueManagerAPI.getFailedJobs( queueName, page, pageSize);
    }

    /**
     * Retrieves a list of active jobs for a specific queue.
     * @return JobPaginatedResult
     */
    Set<String> getQueueNames(){
        return jobQueueManagerAPI.getQueueNames().keySet();
    }

    /**
     * if a file is uploaded, move it to temp location and update params
     * @param form
     * @param params
     * @param request
     */
    private void handleUploadIfPresent(final JobParams form, Map<String, Object> params, HttpServletRequest request) {
        final InputStream fileInputStream = form.getFileInputStream();
        final FormDataContentDisposition contentDisposition = form.getContentDisposition();
        if (null != fileInputStream && null != contentDisposition) {
                final TempFileAPI tempFileAPI = APILocator.getTempFileAPI();
                try {
                    final DotTempFile tempFile = tempFileAPI.createTempFile(contentDisposition.getFileName(), request, fileInputStream);
                    final String path = tempFile.file.getPath();
                    Logger.info(this.getClass(), "File uploaded to temp location: " + path);
                    params.put("filePath", path);
                } catch (Exception e) {
                    Logger.error(this.getClass(), "Error saving temp file", e);
                }
        }
    }
}
