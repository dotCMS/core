package com.dotcms.rest.api.v1.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.api.v1.asset.bulkduplicate.FolderBulkDuplicateResource;
import com.dotcms.rest.api.v1.asset.bulkdelete.FolderBulkDeleteResource;
import com.dotcms.rest.api.v1.asset.bulkupload.BulkUploadResource;
import com.dotcms.rest.api.v1.maintenance.MaintenanceResource;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * #37883: the generic job endpoint {@code POST /api/v1/jobs/{queueName}} (and {@code /upload}) must
 * not create jobs on queues whose entry is a dedicated endpoint. It answers {@code 403} naming that
 * endpoint, and creates nothing. Queues fed by the generic endpoint behave as before.
 * <p>
 * Every refusal is requested as a freshly created back-end user with no admin role and no portlet,
 * the widest gap: the dedicated maintenance endpoints require an administrator and the Maintenance
 * portlet. The route each message must name is read from the real {@code @Path} annotations of the
 * dedicated resources, not from string literals, so a renamed route keeps the message honest.
 */
@EnableWeld
public class JobQueueDomainQueuesBypassIntegrationTest extends Junit5WeldBaseTest {

    @Inject
    JobQueueHelper helper;

    @Inject
    SSEMonitorUtil sseMonitorUtil;

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

    @BeforeAll
    static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    private void startQueue() throws Exception {
        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(10, TimeUnit.SECONDS);
        }
    }

    private JobQueueResource resource() {
        return new JobQueueResource(helper, sseMonitorUtil);
    }

    /** A fresh back-end user with no portlet and no admin role, created right before each call. */
    private static User newBackendUserWithoutPortlets() throws Exception {
        final User user = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(
                APILocator.getRoleAPI().loadBackEndUserRole(), user);
        return user;
    }

    private static HttpServletRequest requestAs(final User user) {
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()).request();
        request.setAttribute(WebKeys.USER, user);
        return request;
    }

    private Response createThroughJson(final String queueName,
            final Map<String, Object> parameters) throws Exception {
        startQueue();
        final HttpServletResponse response = new MockHttpResponse().response();
        return resource().createJob(requestAs(newBackendUserWithoutPortlets()), response,
                queueName, parameters);
    }

    private Response createThroughUpload(final String queueName) throws Exception {
        startQueue();
        final HttpServletResponse response = new MockHttpResponse().response();
        return resource().createJob(requestAs(newBackendUserWithoutPortlets()), response,
                queueName, new JobParams());
    }

    private long jobsOn(final String queueName) throws Exception {
        return jobQueueManagerAPI.getJobs(queueName, 1, 1).total();
    }

    /**
     * Reads the route a dedicated resource method is served at from its real annotations:
     * the verb, {@code /api}, the class {@code @Path} and the method {@code @Path}.
     */
    private static String routeOf(final Class<?> resource, final String methodName) {
        final Method method = Arrays.stream(resource.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No method " + methodName + " on " + resource.getSimpleName()));
        assertTrue(method.isAnnotationPresent(POST.class), methodName + " is not a POST");
        final String classPath = resource.getAnnotation(Path.class).value();
        final String methodPath = method.getAnnotation(Path.class).value();
        return "POST /api/" + (classPath + "/" + methodPath).replaceAll("^/+|/+$", "")
                .replaceAll("/{2,}", "/");
    }

    private static Map<String, Object> pathsOver(final int count) {
        final List<String> paths = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            paths.add(String.format("//demo.dotcms.com/qa-bulk-over-max/f%02d/", i));
        }
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("assetPaths", paths);
        return parameters;
    }

    /**
     * Asserts the JSON path refuses the queue with a 403 that names the dedicated route, and that
     * no job was created on it.
     */
    private void assertRefusedThroughJson(final String queueName,
            final Map<String, Object> parameters, final String expectedRoute) throws Exception {
        final long before = jobsOn(queueName);

        final ForbiddenException refusal = assertThrows(ForbiddenException.class,
                () -> createThroughJson(queueName, parameters));

        // the message lives in the 403 response entity: WebApplicationException#getMessage is generic
        final String body = refusal.getResponse().getEntity().toString();
        assertTrue(body.contains(expectedRoute),
                "message must name " + expectedRoute + " but was: " + body);
        assertEquals(before, jobsOn(queueName), "a refused request must not create a job");
    }

    /**
     * Given 51 paths, one more than FOLDER_BULK_DUPLICATE_MAX_PATHS (50).
     * When they are sent to the generic endpoint instead of {@code _bulkduplicate}.
     * Then the request is refused with 403 naming {@code _bulkduplicate}, and no job is created.
     */
    @Test
    void genericEndpointRefusesFolderBulkDuplicate() throws Exception {
        assertRefusedThroughJson("folderBulkDuplicate", pathsOver(51),
                routeOf(FolderBulkDuplicateResource.class, "bulkDuplicate"));
    }

    /**
     * Given 51 paths, one more than FOLDER_BULK_DELETE_MAX_PATHS (50).
     * When they are sent to the generic endpoint instead of {@code _bulkdelete}.
     * Then the request is refused with 403 naming {@code _bulkdelete}, and no job is created.
     */
    @Test
    void genericEndpointRefusesFolderBulkDelete() throws Exception {
        assertRefusedThroughJson("folderBulkDelete", pathsOver(51),
                routeOf(FolderBulkDeleteResource.class, "bulkDelete"));
    }

    /**
     * Given the bulk-upload queue and no files at all.
     * When a job is requested through the generic JSON path.
     * Then the request is refused with 403 naming {@code _bulkupload}: the file and size ceilings
     * live in the dedicated endpoint.
     */
    @Test
    void genericEndpointRefusesAssetBulkUpload() throws Exception {
        assertRefusedThroughJson("assetBulkUpload", new HashMap<>(),
                routeOf(BulkUploadResource.class, "bulkUpload"));
    }

    /**
     * Given a back-end user without admin role and without the Maintenance portlet.
     * When they ask the generic endpoint for a fix-assets job.
     * Then the request is refused with 403 naming {@code /maintenance/assets/_fix}.
     */
    @Test
    void genericEndpointRefusesMaintenanceFixAssets() throws Exception {
        assertRefusedThroughJson("maintenanceFixAssets", new HashMap<>(),
                routeOf(MaintenanceResource.class, "requestFixAssetsJob"));
    }

    /**
     * Given a back-end user without admin role and without the Maintenance portlet.
     * When they ask the generic endpoint for a clean-assets job (which runs as the system user).
     * Then the request is refused with 403 naming {@code /maintenance/assets/_clean}.
     */
    @Test
    void genericEndpointRefusesMaintenanceCleanAssets() throws Exception {
        assertRefusedThroughJson("maintenanceCleanAssets", new HashMap<>(),
                routeOf(MaintenanceResource.class, "requestCleanAssetsJob"));
    }

    /**
     * Given the multipart creation path {@code /{queueName}/upload}.
     * When a dedicated-entry queue is requested through it.
     * Then it is refused the same way as the JSON path, and no job is created.
     */
    @Test
    void uploadPathRefusesDedicatedQueues() throws Exception {
        for (final String queueName : new String[] {"assetBulkUpload", "folderBulkDuplicate"}) {
            final long before = jobsOn(queueName);

            assertThrows(ForbiddenException.class, () -> createThroughUpload(queueName),
                    queueName + " must be refused on /upload");

            assertEquals(before, jobsOn(queueName),
                    queueName + ": a refused /upload request must not create a job");
        }
    }

    /**
     * Given a queue fed by the generic endpoint ({@code failSuccess}).
     * When a job is requested through the generic endpoint.
     * Then it is accepted exactly as before (200) and the queue's job count grows.
     */
    @Test
    void genericQueueStillAcceptsJobs() throws Exception {
        final long before = jobsOn("failSuccess");

        final Response response = createThroughJson("failSuccess", new HashMap<>());

        assertEquals(200, response.getStatus());
        assertTrue(jobsOn("failSuccess") > before);
    }

    /**
     * Given {@code importContentlets}, whose processor validates its own parameters.
     * When a job is requested through the generic endpoint with no import file.
     * Then it answers as before: its own validation decides, and it is never the 403 of this fix.
     */
    @Test
    void importContentletsIsNotRefusedByTheEntryPointGate() throws Exception {
        try {
            final Response response = createThroughJson("importContentlets", new HashMap<>());
            assertNotEquals(403, response.getStatus());
        } catch (final Exception e) {
            assertFalse(e instanceof ForbiddenException,
                    "importContentlets must not be refused by the entry-point gate");
        }
    }
}
