package com.dotcms.rest.api.v1.job;

import static com.dotcms.jobs.business.util.JobUtil.roundedProgress;

import com.dotcms.jobs.business.api.JobProcessorScanner;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.error.JobProcessorNotFoundException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.jobs.business.queue.error.JobQueueDataException;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.time.format.DateTimeFormatter;
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

/**
 * Helper class for interacting with the job queue system. This class provides methods for creating, cancelling, and listing jobs.
 */
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
                if(!testInstantiation(processor)){
                     return;
                }
               //registering the processor with the jobQueueManagerAPI
               // lower case it to avoid case
               if(processor.isAnnotationPresent(Queue.class)){
                   final Queue queue = processor.getAnnotation(Queue.class);
                   jobQueueManagerAPI.registerProcessor(queue.value(), processor);
               } else {
                   jobQueueManagerAPI.registerProcessor(processor.getName(), processor);
               }
           }catch (Exception e){
               Logger.error(this.getClass(), "Unable to register JobProcessor ", e);
           }
        });
    }

    /**
     * Test if a processor can be instantiated
     * @param processor The processor to tested
     * @return true if the processor can be instantiated, false otherwise
     */
    private boolean testInstantiation(Class<? extends JobProcessor> processor)  {
      try {
          final Constructor<? extends JobProcessor> declaredConstructor = processor.getDeclaredConstructor();
          declaredConstructor.newInstance();
          return true;
      } catch (Exception e) {
          Logger.error(this.getClass(), String.format(" JobProcessor [%s] can not be instantiated and will be ignored.",processor.getName()), e);
      }
      return false;
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
     * Registers a processor
     * @param queueName The name of the queue
     * @param processor Class of the processor
     */
    @VisibleForTesting
    void registerProcessor(final String queueName, final Class<? extends JobProcessor> processor){
        jobQueueManagerAPI.registerProcessor(queueName, processor);
    }

    /**
     * Creates a job
     *
     * @param queueName The name of the queue
     * @param form      The request form with the job parameters
     * @param user      The user requesting to create the job
     * @param request   The request object
     * @return jobId The ID of the created job
     * @throws JsonProcessingException If there is an error processing the request form parameters
     * @throws DotDataException        If there is an error creating the job
     */
    String createJob(final String queueName, final JobParams form, final User user,
            final HttpServletRequest request) throws JsonProcessingException, DotDataException {

        final HashMap<String, Object> in;

        final var parameters = form.getParams();
        if (parameters != null) {
            in = new HashMap<>(parameters);
        } else {
            in = new HashMap<>();
        }

        handleUploadIfPresent(form, in, request);
        return createJob(queueName, in, user, request);
    }

    /**
     * Creates a job with the given parameters
     *
     * @param queueName  The name of the queue
     * @param parameters The job parameters
     * @param user       The user requesting to create the job
     * @param request    The request object
     * @return jobId The ID of the created job
     * @throws DotDataException If there is an error creating the job
     */
    String createJob(final String queueName, final Map<String, Object> parameters, final User user,
            final HttpServletRequest request) throws DotDataException {

        final HashMap<String, Object> in;
        if (parameters != null) {
            in = new HashMap<>(parameters);
        } else {
            in = new HashMap<>();
        }
        return createJob(queueName, in, user, request);
    }

    /**
     * Creates a job with the given parameters
     *
     * @param queueName  The name of the queue
     * @param parameters The job parameters
     * @param user       The user requesting to create the job
     * @param request    The request object
     * @return jobId The ID of the created job
     * @throws DotDataException If there is an error creating the job
     */
    private String createJob(final String queueName, final HashMap<String, Object> parameters,
            final User user, final HttpServletRequest request) throws DotDataException {

        // Get the current host and include it in the job params
        final var currentHost = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        parameters.put("siteName", currentHost.getHostname());
        parameters.put("siteIdentifier", currentHost.getIdentifier());

        // Including the user id in the job params
        parameters.put("userId", user.getUserId());

        try {
            return jobQueueManagerAPI.createJob(queueName, Map.copyOf(parameters));
        } catch (JobProcessorNotFoundException e) {
            Logger.error(this.getClass(), "Error creating job", e);
            throw new DoesNotExistException(e.getMessage());
        }
    }

    /**
     * gets a job
     * @param jobId The ID of the job
     * @return Job
     * @throws DotDataException if there's an error fetching the job
     */
    Job getJob(String jobId) throws DotDataException {
        return jobQueueManagerAPI.getJob(jobId);
    }

    /**
     * cancels a job
     * @param jobId The ID of the job
     * @throws DotDataException if there's an error cancelling the job
     */
    void cancelJob(String jobId) throws DotDataException {
       try{
           jobQueueManagerAPI.cancelJob(jobId);
        } catch (JobProcessorNotFoundException e) {
            Logger.error(this.getClass(), "Error cancelling job", e);
            throw new DoesNotExistException(e.getMessage());
        }
    }

    /**
     * watches a job
     * @param jobId The ID of the job
     * @param watcher The watcher
     */
    void watchJob(String jobId, Consumer<Job> watcher) {
        // if it does then watch it
        jobQueueManagerAPI.watchJob(jobId, watcher);
    }

    /**
     * Retrieves a list of jobs.
     * @param page    The page number
     * @param pageSize The number of jobs per page
     * @return JobPaginatedResult
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getActiveJobs(String queueName, int page, int pageSize) {
        try {
            return jobQueueManagerAPI.getActiveJobs(queueName, page, pageSize);
        } catch (JobQueueDataException e) {
            Logger.error(this.getClass(), "Error fetching active jobs", e);
        }
        return JobPaginatedResult.builder().build();
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
            return jobQueueManagerAPI.getJobs(page, pageSize);
        } catch (DotDataException e) {
            Logger.error(this.getClass(), "Error fetching jobs", e);
        }
        return JobPaginatedResult.builder().build();
    }

    /**
     * Retrieves a list of active jobs, meaning jobs that are currently being processed.
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return A result object containing the list of active jobs and pagination information.
     */
    JobPaginatedResult getActiveJobs(int page, int pageSize) {
        try {
            return jobQueueManagerAPI.getActiveJobs(page, pageSize);
        } catch (JobQueueDataException e) {
            Logger.error(this.getClass(), "Error fetching active jobs", e);
        }
        return JobPaginatedResult.builder().build();
    }

    /**
     * Retrieves a list of completed jobs
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return A result object containing the list of completed jobs and pagination information.
     */
    JobPaginatedResult getCompletedJobs(int page, int pageSize) {
        try {
            return jobQueueManagerAPI.getCompletedJobs(page, pageSize);
        } catch (JobQueueDataException e) {
            Logger.error(this.getClass(), "Error fetching completed jobs", e);
        }
        return JobPaginatedResult.builder().build();
    }

    /**
     * Retrieves a list of canceled jobs
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return A result object containing the list of canceled jobs and pagination information.
     */
    JobPaginatedResult getCanceledJobs(int page, int pageSize) {
        try {
            return jobQueueManagerAPI.getCanceledJobs(page, pageSize);
        } catch (JobQueueDataException e) {
            Logger.error(this.getClass(), "Error fetching canceled jobs", e);
        }
        return JobPaginatedResult.builder().build();
    }

    /**
     * Retrieves a list of failed jobs
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return A result object containing the list of completed jobs and pagination information.
     */
    JobPaginatedResult getFailedJobs(int page, int pageSize) {
        try {
            return jobQueueManagerAPI.getFailedJobs(page, pageSize);
        } catch (JobQueueDataException e) {
            Logger.error(this.getClass(), "Error fetching failed jobs", e);
        }
        return JobPaginatedResult.builder().build();
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
     * @param form The form
     * @param params The params
     * @param request The request
     */
     void handleUploadIfPresent(final JobParams form, Map<String, Object> params, HttpServletRequest request) {
        final InputStream fileInputStream = form.getFileInputStream();
        final FormDataContentDisposition contentDisposition = form.getContentDisposition();
        if (null != fileInputStream && null != contentDisposition) {
                final TempFileAPI tempFileAPI = APILocator.getTempFileAPI();
                try {
                    final DotTempFile tempFile = tempFileAPI.createTempFile(contentDisposition.getFileName(), request, fileInputStream);
                    params.put("tempFileId", tempFile.id);
                    params.put("requestFingerPrint", tempFileAPI.getRequestFingerprint(request));
                } catch (Exception e) {
                    Logger.error(this.getClass(), "Error saving temp file", e);
                }
        } else {
            Logger.info(this.getClass(), "No file was uploaded.");
        }
    }

    /**
     * Check if a job is NOT watchable
     * @param job The job
     * @return true if the job is watchable, false otherwise
     */
    public boolean isNotWatchable(Job job){
        return JobState.PENDING != job.state() && JobState.RUNNING != job.state()
                && JobState.CANCELLING != job.state();
    }

    /**
     * Get the status info for a job
     * @param job The job
     * @return The status info
     */
    public Map<String, Object> getJobStatusInfo(Job job) {
        final DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return Map.of(
                "state", job.state(),
                "progress", roundedProgress(job.progress()),
                "startedAt", job.startedAt().map(isoFormatter::format).orElse("N/A"),
                "finishedAt", job.completedAt().map(isoFormatter::format).orElse("N/A")
        );
    }

}
