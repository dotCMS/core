package com.dotmarketing.portlets.cmsmaintenance.action;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.cmsmaintenance.struts.CmsMaintenanceForm;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.PortletSessionImpl;
import com.liferay.util.servlet.SessionMessages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the destructive commands of {@link ViewCMSMaintenanceAction}. Two layers
 * are covered: the {@link ViewCMSMaintenanceAction#isAuthorizedAdmin(User)} predicate in isolation,
 * and the {@link ViewCMSMaintenanceAction#processAction} dispatch itself, driven for a non-admin
 * user so the test fails if the admin gate is ever moved out of a destructive command's path.
 */
public class ViewCMSMaintenanceActionTest extends IntegrationTestBase {

    private static final String ERROR_MESSAGE = "error";
    private static final String SUCCESS_MESSAGE = "message";
    /** A date old enough that the drop-old-assets command matches nothing, even if it ever ran. */
    private static final String HARMLESS_DATE = "01/01/1970";

    private static final List<User> createdUsers = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @AfterClass
    public static void cleanUp() {
        createdUsers.forEach(user -> UserDataGen.remove(user, Boolean.TRUE));
        createdUsers.clear();
    }

    private static User newNonAdmin() throws DotDataException {
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
        createdUsers.add(user);
        return user;
    }

    private static User newAdmin() throws DotDataException {
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(),
                        APILocator.getRoleAPI().loadCMSAdminRole())
                .nextPersisted();
        createdUsers.add(user);
        return user;
    }

    @Test
    public void isAuthorizedAdmin_nonAdminUser_isFalse() throws Exception {
        assertFalse(ViewCMSMaintenanceAction.isAuthorizedAdmin(newNonAdmin()));
    }

    @Test
    public void isAuthorizedAdmin_adminUser_isTrue() throws Exception {
        assertTrue(ViewCMSMaintenanceAction.isAuthorizedAdmin(newAdmin()));
    }

    @Test
    public void isAuthorizedAdmin_null_isFalse() {
        assertFalse(ViewCMSMaintenanceAction.isAuthorizedAdmin(null));
    }

    /**
     * Method to test: {@link ViewCMSMaintenanceAction#processAction} with {@code cmd=cache}
     * Given: a back-end user who is not a CMS administrator asks to flush the menu file cache
     * Expected: an error message, no success message, and the flush is never reached
     */
    @Test
    public void processAction_cacheAsNonAdmin_rejectedBeforeFlush() throws Exception {
        final CmsMaintenanceForm form = new CmsMaintenanceForm();
        form.setCacheName(com.dotmarketing.util.WebKeys.Cache.CACHE_MENU_FILES);
        final ActionRequestImpl request = maintenanceRequest(newNonAdmin(), "cache");

        new ViewCMSMaintenanceAction().processAction(null, form, null, request, mock(ActionResponse.class));

        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
    }

    /**
     * Method to test: {@link ViewCMSMaintenanceAction#processAction} with {@code cmd=dropoldassets}
     * Given: a back-end user who is not a CMS administrator asks to drop old asset versions
     * Expected: an error message, no success message, and the drop is never reached (the harmless
     * date would delete nothing even if the gate were bypassed, so a bypass shows up as a success)
     */
    @Test
    public void processAction_dropOldAssetsAsNonAdmin_rejectedBeforeDrop() throws Exception {
        final CmsMaintenanceForm form = new CmsMaintenanceForm();
        form.setRemoveassetsdate(HARMLESS_DATE);
        final ActionRequestImpl request = maintenanceRequest(newNonAdmin(), "dropoldassets");

        new ViewCMSMaintenanceAction().processAction(null, form, null, request, mock(ActionResponse.class));

        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
    }

    /**
     * Method to test: {@link ViewCMSMaintenanceAction#processAction} with {@code cmd=deleteSystemJob}
     * Given: a back-end user who is not a CMS administrator asks to delete a scheduler job
     * Expected: an error message, no success message, and the removal is never reached (the job name
     * does not exist, so a bypass would still be a safe no-op that reports success)
     */
    @Test
    public void processAction_deleteSystemJobAsNonAdmin_rejectedBeforeDelete() throws Exception {
        final ActionRequestImpl request = maintenanceRequest(newNonAdmin(), "deleteSystemJob");
        when(request.getParameter("systemJobName")).thenReturn("no-such-job-" + System.nanoTime());
        when(request.getParameter("systemJobGroup")).thenReturn("no-such-group");

        new ViewCMSMaintenanceAction()
                .processAction(null, new CmsMaintenanceForm(), null, request, mock(ActionResponse.class));

        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
    }

    /**
     * Method to test: {@link ViewCMSMaintenanceAction#processAction} with {@code cmd=cache}
     * Given: a CMS administrator asks to flush the menu file cache
     * Expected: a success message and no error message, proving the gate lets administrators through
     * to the same command the non-admin cases are refused (so those refusals are about the user, not
     * a blanket block)
     */
    @Test
    public void processAction_cacheAsAdmin_reachesFlush() throws Exception {
        final CmsMaintenanceForm form = new CmsMaintenanceForm();
        form.setCacheName(com.dotmarketing.util.WebKeys.Cache.CACHE_MENU_FILES);
        final ActionRequestImpl request = maintenanceRequest(newAdmin(), "cache");

        new ViewCMSMaintenanceAction().processAction(null, form, null, request, mock(ActionResponse.class));

        assertTrue(SessionMessages.contains(request, SUCCESS_MESSAGE));
        assertFalse(SessionMessages.contains(request, ERROR_MESSAGE));
    }

    /**
     * A portlet {@code ActionRequest} that resolves to the given user and keeps, in an in-memory
     * session, the messages the action adds, so {@link SessionMessages} can be read back.
     *
     * @param user the user the action will resolve from the request
     * @param cmd  the value of the {@code cmd} parameter the action dispatches on
     * @return a mock request wired for {@link ViewCMSMaintenanceAction#processAction}
     */
    private static ActionRequestImpl maintenanceRequest(final User user, final String cmd) {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getAttribute(WebKeys.USER)).thenReturn(user);

        final Map<String, Object> sessionAttributes = new HashMap<>();
        final HttpSession httpSession = mock(HttpSession.class);
        when(httpSession.getAttribute(anyString()))
                .thenAnswer(invocation -> sessionAttributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> sessionAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(httpSession).setAttribute(anyString(), any());
        when(httpRequest.getSession()).thenReturn(httpSession);
        when(httpRequest.getSession(false)).thenReturn(httpSession);
        final PortletSessionImpl portletSession = mock(PortletSessionImpl.class);
        when(portletSession.getSession()).thenReturn(httpSession);

        final ActionRequestImpl request = mock(ActionRequestImpl.class);
        when(request.getHttpServletRequest()).thenReturn(httpRequest);
        when(request.getPortletSession()).thenReturn(portletSession);
        when(request.getParameter("cmd")).thenReturn(cmd);
        return request;
    }
}
