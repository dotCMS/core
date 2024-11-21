package com.dotcms.rest.api.v1.contentImport;

import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.rest.api.v1.JobQueueManagerHelper;
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
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class ContentImportHelper {

    private JobQueueManagerAPI jobQueueManagerAPI;
    private JobQueueManagerHelper jobQueueManagerHelper;

    @Inject
    public ContentImportHelper(final JobQueueManagerAPI jobQueueManagerAPI, final JobQueueManagerHelper jobQueueManagerHelper) {
        this.jobQueueManagerAPI = jobQueueManagerAPI;
        this.jobQueueManagerHelper = jobQueueManagerHelper;
    }

    public ContentImportHelper() {
        //default constructor Mandatory for CDI
    }

    @PostConstruct
    public void onInit() {
        jobQueueManagerHelper.registerProcessors();
    }

    @PreDestroy
    public void onDestroy() {
        jobQueueManagerHelper.shutdown();
    }

    /**
     * Creates a content import job with the provided parameters
     *
     * @param command Whether this is a preview job
     * @param queueName The name of the queue to submit the job to
     * @param params The import parameters
     * @param user The user initiating the import
     * @param request The HTTP request
     * @return The ID of the created job
     */
    public String createJob(
            final String command,
            final String queueName, 
            final ContentImportParams params,
            final User user,
            final HttpServletRequest request) throws DotDataException, JsonProcessingException {

        params.checkValid();

        final Map<String, Object> jobParameters = createJobParameters(command, params, user, request);
        processFileUpload(params, jobParameters, request);

        return jobQueueManagerAPI.createJob(queueName, jobParameters);
    }

    /**
     * Creates the job parameters map from the provided inputs
     */
    private Map<String, Object> createJobParameters(
            final String command,
            final com.dotcms.rest.api.v1.contentImport.ContentImportParams params,
            final User user,
            final HttpServletRequest request) throws JsonProcessingException {

        final Map<String, Object> jobParameters = new HashMap<>();
        
        // Add required parameters
        jobParameters.put("cmd", command);
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
            final Map<String, Object> jobParameters) throws JsonProcessingException {
        
        final ContentImportForm form = params.getForm();

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
            final ContentImportParams params,
            final Map<String, Object> jobParameters,
            final HttpServletRequest request) throws DotDataException {

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
}