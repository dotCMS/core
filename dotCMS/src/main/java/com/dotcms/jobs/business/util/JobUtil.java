package com.dotcms.jobs.business.util;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.jobs.business.api.events.JobEvent;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.github.jonpeterson.jackson.module.versioning.VersioningModule;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import javax.servlet.http.HttpServletRequest;

/**
 * Utility class for job-related operations.
 */
public class JobUtil {

    /**
     * Jackson mapper configuration and lazy initialized instance.
     */
    private static final Lazy<ObjectMapper> objectMapper = Lazy.of(() -> {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new BlackbirdModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new GuavaModule());
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new VersioningModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    });

    private JobUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Retrieves the temporary file associated with the given job.
     *
     * @param job The job containing the temporary file information in its parameters.
     * @return An Optional containing the DotTempFile if found, or an empty Optional if not found or
     * if an error occurs.
     */
    public static Optional<DotTempFile> retrieveTempFile(final Job job) {

        if (job == null) {
            Logger.error(JobUtil.class, "Job cannot be null");
            return Optional.empty();
        }

        Map<String, Object> params = job.parameters();
        return retrieveTempFile(params);
    }

    /**
     * Retrieves the temporary file associated with the given job parameters.
     *
     * @param params The parameters containing the temporary file information.
     * @return An Optional containing the DotTempFile if found, or an empty Optional if not found or
     * if an error occurs.
     */
    public static Optional<DotTempFile> retrieveTempFile(final Map<String, Object> params) {

        if (params == null) {
            Logger.error(JobUtil.class, "Job parameters cannot be null");
            return Optional.empty();
        }

        // Extract parameters
        String tempFileId = (String) params.get("tempFileId");
        if (tempFileId == null) {
            Logger.error(JobUtil.class, "Parameter 'tempFileId' is required");
            return Optional.empty();
        }

        final Object requestFingerPrintRaw = params.get("requestFingerPrint");
        if (!(requestFingerPrintRaw instanceof String)) {
            Logger.error(JobUtil.class,
                    "Parameter 'requestFingerPrint' is required and must be a string.");
            return Optional.empty();
        }
        final String requestFingerPrint = (String) requestFingerPrintRaw;

        // Retrieve the temporary file
        final TempFileAPI tempFileAPI = APILocator.getTempFileAPI();
        final Optional<DotTempFile> tempFile = tempFileAPI.getTempFile(
                List.of(requestFingerPrint), tempFileId
        );
        if (tempFile.isEmpty()) {
            Logger.error(JobUtil.class, "Temporary file not found: " + tempFileId);
        }

        return tempFile;
    }

    /**
     * Utility method to create or retrieve an HttpServletRequest when needed from job processors.
     * Uses thread-local request if available, otherwise creates a mock request with the specified
     * user and site information.
     *
     * @param user     The user performing the import
     * @param siteName The name of the site for the import
     * @return An HttpServletRequest instance configured for the import operation
     */
    public static HttpServletRequest generateMockRequest(final User user, final String siteName) {

        if (null != HttpServletRequestThreadLocal.INSTANCE.getRequest()) {
            return HttpServletRequestThreadLocal.INSTANCE.getRequest();
        }

        final HttpServletRequest requestProxy = new MockSessionRequest(
                new MockHeaderRequest(
                        new FakeHttpRequest(siteName, "/").request(),
                        "referer",
                        "https://" + siteName + "/fakeRefer")
                        .request());
        requestProxy.setAttribute(WebKeys.CMS_USER, user);
        requestProxy.getSession().setAttribute(WebKeys.CMS_USER, user);
        requestProxy.setAttribute(com.liferay.portal.util.WebKeys.USER_ID,
                UtilMethods.extractUserIdOrNull(user));

        return requestProxy;
    }

    /**
     * Helper method to round the progress to 3 decimal places.
     *
     * @param progress The progress value to round
     * @return The rounded progress value
     */
    public static float roundedProgress(final float progress) {

        // Round the progress to 3 decimal places
        final var roundedProgress = BigDecimal.valueOf(progress)
                .setScale(3, RoundingMode.HALF_UP)
                .floatValue();

        return Math.round(roundedProgress * 1000f) / 1000f;
    }

    /**
     * Helper method to send both local and cluster-wide events for a job state change
     *
     * @param job          The job that triggered the event
     * @param eventFactory Factory function to create the specific event type
     * @param <T>          The type of event being created (must extend JobEvent)
     */
    public static <T extends JobEvent> void sendEvents(
            final Job job,
            final BiFunction<Job, LocalDateTime, T> eventFactory) {

        // Create the event
        final T event = eventFactory.apply(job, LocalDateTime.now());

        // Send the event notifications
        sendEvents(event);
    }

    /**
     * Helper method to send both local and cluster-wide notifications for a job event.
     *
     * @param event The event to send (must implement the JobEvent interface)
     */
    public static <T extends JobEvent> void sendEvents(final T event) {

        // LOCAL event
        APILocator.getLocalSystemEventsAPI().notify(event);

        // CLUSTER WIDE event
        Try.run(() -> APILocator.getSystemEventsAPI()
                        .push(SystemEventType.CLUSTER_WIDE_EVENT, new Payload(event)))
                .onFailure(e -> Logger.error(JobUtil.class, e.getMessage()));
    }

    /**
     * Converts the given object into a Map representation. This method uses the configured
     * ObjectMapper to transform the provided object into a Map with keys as strings and values as
     * objects.
     *
     * @param toTransform The object to be converted into a Map
     * @return A Map containing the key-value pairs representing the structure of the input object
     */
    public static Map<String, Object> transformToMap(final Object toTransform) {
        return objectMapper.get().convertValue(toTransform, Map.class);
    }

    /**
     * Converts the given object into its JSON string representation using the configured
     * ObjectMapper. The ObjectMapper is configured with various modules (JDK8, Guava, JavaTime,
     * Versioning) and has pretty printing enabled.
     *
     * @param toTransform The object to be transformed into a JSON string
     * @return A JSON string representation of the input object
     * @throws JsonProcessingException If the object cannot be serialized to JSON
     */
    public static String transformToString(final Object toTransform)
            throws JsonProcessingException {
        return objectMapper.get().writeValueAsString(toTransform);
    }

}
