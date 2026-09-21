package com.dotcms.jobs.business.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.VisibilityRoles;
import com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.DefaultProgressTracker;
import com.dotcms.rest.api.v1.asset.bulkdelete.FolderBulkDeleteHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the per-folder "entering" / "left" a delete announcements (#37063,
 * FR-035a, FR-035b, C-012).
 * <p>
 * <b>Corrected before being written</b> — checking the frontend half (PR dotCMS/core#37612)
 * directly found the original plan wrong about reusing the existing {@code DELETE_FOLDER} event
 * for "left": the frontend subscribes to its own {@code FOLDER_DELETE_FINISHED}, expected on a
 * failed per-path delete too ("success or failure alike", its own fixture comment), which
 * {@code DELETE_FOLDER}'s success-only push cannot provide. See research.md R4 and plan.md PO-3
 * for the full trace. Both {@code FOLDER_DELETE_STARTED} and {@code FOLDER_DELETE_FINISHED} are
 * therefore new pushes from {@link FolderBulkDeleteProcessor} itself.
 * <p>
 * <b>Drives {@link FolderBulkDeleteProcessor} with a captured {@link SystemEventsAPI}</b>, the
 * same convention {@code FolderBulkDeleteNotificationIT} uses for {@code NotificationAPI} — found
 * necessary the hard way, not chosen up front: an earlier version of this file read announcements
 * back through the real queue ({@code SystemEventsAPI#getEventsSince}), which uncovered a genuine,
 * pre-existing defect unrelated to this feature — {@code ExcludeOwnerVerifierBean} has no Jackson
 * creator, so any {@code Visibility.EXCLUDE_OWNER} event (including the existing shipped
 * {@code DELETE_FOLDER}) is silently unreadable through that path, the failure swallowed by
 * {@code SystemEventsFactory#convertBatchSkippingUnreadableRows}. That defect is tracked
 * separately and does not block this feature; capturing the call directly is both the fix for this
 * test and the pattern {@code FolderBulkDeleteCompletionListener}'s own tests already established.
 */
@EnableWeld
public class FolderBulkDeleteAnnouncementIT extends Junit5WeldBaseTest {

    private static User admin;
    private static Host site;
    private static FolderAPI folderAPI;

    @BeforeAll
    public static void prepare() throws Exception {
        com.dotcms.util.IntegrationTestInitService.getInstance().init();
        admin = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
        folderAPI = APILocator.getFolderAPI();
    }

    private Folder folder() {
        return new FolderDataGen().site(site).nextPersisted();
    }

    private String pathOf(final Folder folder) {
        return String.format("//%s/%s/", site.getHostname(), folder.getName());
    }

    private Job jobForPaths(final List<String> paths, final User submittingUser) {
        final List<Map<String, Object>> pathParams = new ArrayList<>();
        for (final String path : paths) {
            final Map<String, Object> p = new HashMap<>();
            p.put("path", path);
            pathParams.add(p);
        }

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", submittingUser.getUserId());
        parameters.put("paths", pathParams);

        return Job.builder()
                .id(UUID.randomUUID().toString())
                .queueName(FolderBulkDeleteHelper.QUEUE_NAME)
                .state(JobState.RUNNING)
                .parameters(parameters)
                .progressTracker(new DefaultProgressTracker())
                .build();
    }

    /** Grants a role a combined permission bitmask on a permissionable, admin-issued. */
    private void grantPermission(final Permissionable permissionable, final String roleId,
            final int permissionBitmask) throws Exception {
        final Permission permission = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                permissionable.getPermissionId(), roleId, permissionBitmask, true);
        APILocator.getPermissionAPI().save(permission, permissionable, admin, false);
    }

    /**
     * Captures every {@code pushAsync} call the processor makes, in call order — unlike
     * {@code FolderBulkDeleteNotificationIT}'s single-shot capture, one run can push several
     * announcements (one pair per top-level folder), so this keeps all of them.
     */
    private static class CapturingSystemEvents implements InvocationHandler {

        private final List<Object[]> calls = new ArrayList<>();

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) {
            if ("pushAsync".equals(method.getName()) && args != null && args.length == 2) {
                calls.add(new Object[]{args[0], args[1]});
            }
            return null;
        }

        SystemEventsAPI asApi() {
            return (SystemEventsAPI) Proxy.newProxyInstance(
                    SystemEventsAPI.class.getClassLoader(),
                    new Class<?>[]{SystemEventsAPI.class}, this);
        }

        /** The index of the first push of this type for this path, or -1 — used for ordering. */
        int indexOf(final SystemEventType type, final String path) {
            for (int i = 0; i < calls.size(); i++) {
                if (calls.get(i)[0] == type && path.equals(pathOf(calls.get(i)))) {
                    return i;
                }
            }
            return -1;
        }

        Payload payloadFor(final SystemEventType type, final String path) {
            final int index = indexOf(type, path);
            if (index < 0) {
                throw new AssertionError("no " + type + " pushed for " + path + " among "
                        + calls.size() + " captured push(es)");
            }
            return (Payload) calls.get(index)[1];
        }

        private static String pathOf(final Object[] call) {
            final Payload payload = (Payload) call[1];
            final Object data = payload.getData();
            return data instanceof Map ? String.valueOf(((Map<?, ?>) data).get("path")) : null;
        }
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: A folder the submitter may fully delete
     * ExpectedResult: Both {@code FOLDER_DELETE_STARTED} and {@code FOLDER_DELETE_FINISHED} are
     * pushed, each carrying {@code {jobId, path}}, and "entering" is announced strictly before
     * "left" (FR-035a's own ordering requirement, C-012)
     */
    @Test
    public void test_process_successfulDelete_announcesEnteringThenLeaving_withJobIdAndPath()
            throws Exception {

        final Folder folder = folder();
        final String path = pathOf(folder);
        final Job job = jobForPaths(List.of(path), admin);

        final CapturingSystemEvents captured = new CapturingSystemEvents();
        new FolderBulkDeleteProcessor(captured.asApi()).process(job);

        final Payload started = captured.payloadFor(SystemEventType.FOLDER_DELETE_STARTED, path);
        final Payload finished = captured.payloadFor(SystemEventType.FOLDER_DELETE_FINISHED, path);

        assertEquals(job.id(), ((Map<?, ?>) started.getData()).get("jobId"));
        assertEquals(job.id(), ((Map<?, ?>) finished.getData()).get("jobId"));
        assertTrue(captured.indexOf(SystemEventType.FOLDER_DELETE_STARTED, path)
                        < captured.indexOf(SystemEventType.FOLDER_DELETE_FINISHED, path),
                "the folder must be announced as entering the delete before it is announced as "
                        + "having left it");
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: A folder the submitting user may only read, not delete — recorded
     * FAILED/PERMISSION_DENIED by the run (US2's own scenario, reused here)
     * ExpectedResult: {@code FOLDER_DELETE_FINISHED} is still announced — the whole reason it
     * cannot simply reuse the existing (success-only) {@code DELETE_FOLDER} push. An author working
     * inside this folder must learn it is no longer busy whether or not the delete actually
     * succeeded.
     */
    @Test
    public void test_process_failedDelete_stillAnnouncesLeaving() throws Exception {

        final User limitedUser = new UserDataGen().nextPersisted();
        final String roleId = APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId();
        grantPermission(site, roleId, PermissionAPI.PERMISSION_READ);

        final Folder readOnlyFolder = folder();
        grantPermission(readOnlyFolder, roleId, PermissionAPI.PERMISSION_READ);
        final String path = pathOf(readOnlyFolder);

        final Job job = jobForPaths(List.of(path), limitedUser);

        final CapturingSystemEvents captured = new CapturingSystemEvents();
        new FolderBulkDeleteProcessor(captured.asApi()).process(job);

        assertTrue(captured.indexOf(SystemEventType.FOLDER_DELETE_FINISHED, path) >= 0,
                "a folder whose delete failed must still be announced as no longer busy — "
                        + "otherwise every author who saw the 'entering' announcement is left "
                        + "believing it still is");
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: A folder with a specific role granted {@code PERMISSION_READ}
     * ExpectedResult: The announcement is scoped to exactly that audience —
     * {@link Visibility#EXCLUDE_OWNER}, naming the submitter as the excluded owner and the granted
     * role among the roles it is delivered to (FR-035a, FR-035b: filtered by rights, not by
     * back-end role)
     */
    @Test
    public void test_process_announcementScoped_excludesSubmitter_includesReaderRole()
            throws Exception {

        final User reader = new UserDataGen().nextPersisted();
        final String readerRoleId = APILocator.getRoleAPI().loadRoleByKey(reader.getUserId()).getId();

        final Folder folder = folder();
        grantPermission(folder, readerRoleId, PermissionAPI.PERMISSION_READ);
        final String path = pathOf(folder);

        final Job job = jobForPaths(List.of(path), admin);

        final CapturingSystemEvents captured = new CapturingSystemEvents();
        new FolderBulkDeleteProcessor(captured.asApi()).process(job);

        final Payload payload = captured.payloadFor(SystemEventType.FOLDER_DELETE_STARTED, path);
        assertEquals(Visibility.EXCLUDE_OWNER, payload.getVisibility(),
                "the submitter must be excluded from their own announcement, the same pattern "
                        + "DELETE_FOLDER already uses");

        final ExcludeOwnerVerifierBean excludeBean =
                (ExcludeOwnerVerifierBean) payload.getVisibilityValue();
        assertEquals(admin.getUserId(), excludeBean.getUserId(),
                "excluded owner must be the submitter who is doing the deleting");

        final VisibilityRoles roles = (VisibilityRoles) excludeBean.getVisibilityValue();
        assertTrue(roles.getRolesId().contains(readerRoleId),
                "a role granted PERMISSION_READ on the folder must be part of the announcement's "
                        + "audience (FR-035b)");
    }
}
