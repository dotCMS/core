package com.dotcms.rest.api.v1.contentImport;

import com.dotcms.jobs.business.api.JobProcessorScanner;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.portal.model.User;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ContentImportHelper {

    private static final String CMD_PREVIEW = "preview";
    private static final String CMD_PUBLISH = "publish";

    JobQueueManagerAPI jobQueueManagerAPI;
    JobProcessorScanner scanner;

    @Inject
    public ContentImportHelper(
            JobQueueManagerAPI jobQueueManagerAPI,
            JobProcessorScanner scanner) {
        this.jobQueueManagerAPI = jobQueueManagerAPI;
        this.scanner = scanner;
    }

    public ContentImportHelper() {
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

    /**
     * Creates a content import job with the provided parameters
     *
     * @param preview Whether this is a preview job
     * @param queueName The name of the queue to submit the job to
     * @param params The import parameters
     * @param user The user initiating the import
     * @param request The HTTP request
     * @return The ID of the created job
     */
    public String createJob(
            final boolean preview, 
            final String queueName, 
            final com.dotcms.rest.api.v1.contentImport.ContentImportParams params,
            final User user,
            final HttpServletRequest request) throws DotDataException, JsonProcessingException {

        params.getForm().checkValid();

        final Map<String, Object> jobParameters = createJobParameters(preview, params, user, request);
        processFileUpload(params, jobParameters, request);

        return jobQueueManagerAPI.createJob(queueName, jobParameters);
    }

    /**
     * Creates the job parameters map from the provided inputs
     */
    private Map<String, Object> createJobParameters(
            final boolean preview,
            final com.dotcms.rest.api.v1.contentImport.ContentImportParams params,
            final User user,
            final HttpServletRequest request) throws JsonProcessingException, DotDataException {

        final Map<String, Object> jobParameters = new HashMap<>();
        
        // Add required parameters
        jobParameters.put("cmd", preview ? CMD_PREVIEW : CMD_PUBLISH);
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
     * Adds optional parameters to the job parameters map if they are present
     */
    private void addOptionalParameters(
            final com.dotcms.rest.api.v1.contentImport.ContentImportParams params,
            final Map<String, Object> jobParameters) throws JsonProcessingException, DotDataException {
        
        final com.dotcms.rest.api.v1.contentImport.ContentImportForm form = params.getForm();

        if (form.getLanguage() != null && !form.getLanguage().isEmpty()) {
            jobParameters.put("language", form.getLanguage());
        }
        if (form.getFields() != null && !form.getFields().isEmpty()) {
            jobParameters.put("fields", form.getFields());
        }
    }

    /**
     * Adds current site information to the job parameters
     */
    private void addSiteInformation(
            final HttpServletRequest request, 
            final Map<String, Object> jobParameters){
        
        final var currentHost = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        jobParameters.put("siteName", currentHost.getHostname());
        jobParameters.put("siteIdentifier", currentHost.getIdentifier());
    }

    /**
     * Processes the file upload and adds the necessary parameters to the job
     */
    private void processFileUpload(
            final com.dotcms.rest.api.v1.contentImport.ContentImportParams params,
            final Map<String, Object> jobParameters,
            final HttpServletRequest request) throws DotDataException {

        validateFileUpload(params);

        try {
            final DotTempFile tempFile = APILocator.getTempFileAPI().createTempFile(
                    params.getContentDisposition().getFileName(),
                    request,
                    params.getFileInputStream()
            );
            jobParameters.put("tempFileId", tempFile.id);
            jobParameters.put("requestFingerPrint", APILocator.getTempFileAPI().getRequestFingerprint(request));
        } catch (DotSecurityException e) {
            Logger.error(this, "Error handling file upload", e);
            throw new DotDataException("Error processing file upload: " + e.getMessage());
        }
    }

    /**
     * Validates that the file upload parameters are present
     */
    private void validateFileUpload(final com.dotcms.rest.api.v1.contentImport.ContentImportParams params) throws DotDataException {
        if (params.getFileInputStream() == null || params.getContentDisposition() == null) {
            throw new DotDataException("CSV file is required");
        }
    }
}