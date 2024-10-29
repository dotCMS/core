package com.dotcms.rest.api.v1.job;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the JobQueueHelper class
 * Helper add functionality to consume JobQueueManagerAPI
 * Here we test those functionalities, methods that simply call the JobQueueManagerAPI are not tested
 */
@EnableWeld
public class JobQueueHelperIntegrationTest  extends com.dotcms.Junit5WeldBaseTest {

    @Inject
    JobQueueHelper jobQueueHelper;

    @Test
    void testEmptyParams(){
        assertThrows(IllegalArgumentException.class, () -> {
            jobQueueHelper.createJob("any", new JobParams(), mock(HttpServletRequest.class));
        });
    }

    @Test
    void testWithValidParamsButInvalidQueueName(){
        final JobParams jobParams = new JobParams();
        jobParams.setJsonParams("{}");
        assertThrows(DoesNotExistException.class, () -> {
            jobQueueHelper.createJob("nonExisting", jobParams, mock(HttpServletRequest.class));
        });
    }

    public static class DemoJobProcessor implements JobProcessor {

        public DemoJobProcessor() {
            // Do nothing
        }

        @Override
        public void process(Job job) {
            // Do nothing
        }

        @Override
        public Map<String, Object> getResultMetadata(Job job) {
            return Map.of();
        }
    }

    /**
     * Test with valid parameters and queue name
     * Given scenario: create a job with valid parameters and queue name
     * Expected result: the job is created and the queue name is added to the list of queue names
     * @throws DotDataException
     * @throws JsonProcessingException
     */
    @Test
    void testWithValidParamsAndQueueName() throws DotDataException, JsonProcessingException {
        jobQueueHelper.registerProcessor("demoQueue", DemoJobProcessor.class);

        final JobParams jobParams = new JobParams();
        jobParams.setJsonParams("{}");

        final String jobId = jobQueueHelper.createJob("demoQueue", jobParams,
                mock(HttpServletRequest.class));

        Assertions.assertNotNull(jobId);
        final Job job = jobQueueHelper.getJob(jobId);
        Assertions.assertNotNull(job);
        Assertions.assertEquals(jobId, job.id());
        Assertions.assertTrue(jobQueueHelper.getQueueNames().contains("demoQueue".toLowerCase()));
    }

    /**
     * Given scenario: create a job with valid parameters and queue name
     * Expected result: the job is created and it is watchable
     * @throws DotDataException
     * @throws JsonProcessingException
     */
    @Test
    void testIsWatchable() throws DotDataException, JsonProcessingException {
        jobQueueHelper.registerProcessor("testQueue", DemoJobProcessor.class);
        final JobParams jobParams = new JobParams();
        jobParams.setJsonParams("{}");
        final String jobId = jobQueueHelper.createJob("testQueue", jobParams,
                mock(HttpServletRequest.class));
        Assertions.assertNotNull(jobId);
        final Job job = jobQueueHelper.getJob(jobId);
        assertFalse(jobQueueHelper.isNotWatchable(job));
    }


    /**
     * Given scenario: create a job with valid parameters and queue name
     * Expected result: the job is created and the status info is returned
     * @throws DotDataException
     * @throws JsonProcessingException
     */
    @Test
    void testGetStatusInfo() throws DotDataException, JsonProcessingException {
        jobQueueHelper.registerProcessor("testQueue", DemoJobProcessor.class);
        final JobParams jobParams = new JobParams();
        jobParams.setJsonParams("{}");
        final String jobId = jobQueueHelper.createJob("testQueue", jobParams,
                mock(HttpServletRequest.class));
        Assertions.assertNotNull(jobId);
        final Job job = jobQueueHelper.getJob(jobId);
        final Map<String, Object> info = jobQueueHelper.getJobStatusInfo(job);
        Assertions.assertTrue(info.containsKey("state"));
        Assertions.assertTrue(info.containsKey("progress"));
        Assertions.assertTrue(info.containsKey("startedAt"));
        Assertions.assertTrue(info.containsKey("finishedAt"));
    }

    /**
     * Given scenario: call cancel Job with an invalid job id
     * Expected result: we should get a DoesNotExistException
     */
   @Test
    void testCancelNonExistingJob(){
        assertThrows(DoesNotExistException.class, () -> {
            jobQueueHelper.cancelJob("nonExisting" );
        });
    }

    /**
     * Mock the request
     * @return a mocked HttpServletRequest
     */
    HttpServletRequest mockRequest(){
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("param1")).thenReturn("value1");
        when(request.getParameter("param2")).thenReturn("value2");
        when(request.getParameter("param3")).thenReturn("value3");
        when(request.getParameterNames()).thenReturn(java.util.Collections.enumeration(java.util.Arrays.asList("param1", "param2", "param3")));
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("mockSessionId123");
        when(request.getSession()).thenReturn(session);

        User user = mock(User.class);
        when(user.getUserId()).thenReturn("mockUserId123");
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        return request;
    }

    FormDataContentDisposition mockFormDataContentDisposition(){
        FormDataContentDisposition formDataContentDisposition = mock(FormDataContentDisposition.class);
        when(formDataContentDisposition.getFileName()).thenReturn("test.txt");
        return formDataContentDisposition;
    }

    /**
     * Test file upload
     * Given scenario: upload a file
     * Expected result: the file is uploaded and the request fingerprint and temp file id are added to the map
     * @throws IOException
     */
    @Test
    void TestFileUpload() throws IOException {

        final File tempFile = File.createTempFile("test", "test");
        JobParams jobParams = new JobParams();
        jobParams.setJsonParams("{}");
        jobParams.setParams(Map.of());
        jobParams.setFileInputStream(new FileInputStream(tempFile));
        jobParams.setContentDisposition(mockFormDataContentDisposition());

        final Map <String,Object>map = new HashMap<>();
        jobQueueHelper.handleUploadIfPresent(jobParams,map,mockRequest());
        Assertions.assertTrue(map.containsKey("requestFingerPrint"));
        Assertions.assertTrue(map.containsKey("tempFileId"));
    }

}
