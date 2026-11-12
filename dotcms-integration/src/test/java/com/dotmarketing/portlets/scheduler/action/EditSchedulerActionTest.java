package com.dotmarketing.portlets.scheduler.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.scheduler.struts.SchedulerForm;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.job.ContentImportThread;
import com.liferay.portal.model.User;
import com.liferay.portlet.PortletSessionImpl;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpSession;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for how {@link EditSchedulerAction} saves a job.
 */
public class EditSchedulerActionTest extends IntegrationTestBase {

    private static final String JOB_GROUP = "User Job";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link EditSchedulerAction#_saveScheduler}
     * Given: a job saved with a {@link ScheduledTask#SCHEDULED_BY_USER_ID} parameter naming another user
     * Expected: the job records the user who saved it, and the other parameters are kept
     */
    @Test
    public void test_saveScheduler_shouldRecordTheSavingUser() throws Exception {
        final User user = new UserDataGen().nextPersisted();
        final String jobName = "content-import-" + UUID.randomUUID();
        final SchedulerForm form = newForm(jobName, "another-user");

        try {
            EditSchedulerAction._saveScheduler(newRequest(), null, null, form, user);

            final List<ScheduledTask> saved = QuartzUtils.getStandardScheduledTask(jobName, JOB_GROUP);
            assertFalse(saved.isEmpty());
            final Map<String, Object> properties = saved.get(0).getProperties();
            assertEquals(user.getUserId(), properties.get(ScheduledTask.SCHEDULED_BY_USER_ID));
            assertEquals("SomeType", properties.get("structure"));
        } finally {
            QuartzUtils.removeJob(jobName, JOB_GROUP);
        }
    }

    /**
     * Method to test: {@link EditSchedulerAction#_saveScheduler}
     * Given: an existing job saved again with no user
     * Expected: the save is refused and the job keeps the user it runs as
     */
    @Test
    public void test_saveScheduler_withoutUser_shouldKeepTheJobAsItWas() throws Exception {
        final User user = new UserDataGen().nextPersisted();
        final String jobName = "content-import-" + UUID.randomUUID();

        try {
            EditSchedulerAction._saveScheduler(newRequest(), null, null, newForm(jobName, null), user);

            assertThrows(DotRuntimeException.class, () ->
                    EditSchedulerAction._saveScheduler(newRequest(), null, null, newForm(jobName, null), null));

            final List<ScheduledTask> saved = QuartzUtils.getStandardScheduledTask(jobName, JOB_GROUP);
            assertFalse(saved.isEmpty());
            assertEquals(user.getUserId(), saved.get(0).getProperties().get(ScheduledTask.SCHEDULED_BY_USER_ID));
        } finally {
            QuartzUtils.removeJob(jobName, JOB_GROUP);
        }
    }

    private static SchedulerForm newForm(final String jobName, final String scheduledBy) {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("structure", "SomeType");
        if (scheduledBy != null) {
            parameters.put(ScheduledTask.SCHEDULED_BY_USER_ID, scheduledBy);
        }
        final SchedulerForm form = new SchedulerForm();
        form.setJobName(jobName);
        form.setJobGroup(JOB_GROUP);
        form.setJavaClass(ContentImportThread.class.getName());
        form.setMap(parameters);
        return form;
    }

    private static ActionRequest newRequest() {
        final ActionRequest request = mock(ActionRequest.class);
        when(request.getParameter("cronExpression")).thenReturn("0 0 1 1 1 ? 2099");
        when(request.getParameterNames()).thenReturn(Collections.emptyEnumeration());
        final PortletSessionImpl session = mock(PortletSessionImpl.class);
        when(session.getSession()).thenReturn(mock(HttpSession.class));
        when(request.getPortletSession()).thenReturn(session);
        return request;
    }
}
