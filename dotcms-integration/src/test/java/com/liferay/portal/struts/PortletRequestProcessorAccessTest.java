package com.liferay.portal.struts;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.config.ForwardConfig;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the wiring between {@link PortletRequestProcessor#processRoles} and the
 * access decision: that the processor asks about the path of the mapping it is about to run, that
 * a request which is not allowed is stopped, and that the access denied page is only forwarded to
 * in the render phase — the action phase has no render response to write through, and is followed
 * by a render of the same portlet that shows the page.
 */
public class PortletRequestProcessorAccessTest {

    private static final String RESTRICTED_PATH = "/ext/scheduler/view_schedulers";
    private static final String UNRESTRICTED_PATH = "/ext/contentlet/edit_contentlet";

    private static User adminUser;
    private static User contentPortletUser;
    private static Layout layout;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        adminUser = TestUserUtils.getAdminUser();

        final RoleAPI roleAPI = APILocator.getRoleAPI();
        contentPortletUser = new UserDataGen()
                .emailAddress("processor-access" + System.nanoTime() + "@dotcms.com")
                .roles(roleAPI.loadBackEndUserRole())
                .nextPersisted();
        layout = new LayoutDataGen()
                .name("processor-access-" + System.nanoTime())
                .portletIds(PortletID.CONTENT.toString())
                .nextPersisted();
        roleAPI.addLayoutToRole(layout, roleAPI.getUserRole(contentPortletUser));
    }

    /**
     * Deletes the test user and layout; a failure on one does not strand the other.
     */
    @AfterClass
    public static void cleanUp() {
        try {
            APILocator.getUserAPI().delete(contentPortletUser, TestUserUtils.getAdminUser(),
                    APILocator.systemUser(), false);
        } catch (final Exception e) {
            Logger.warn(PortletRequestProcessorAccessTest.class,
                    "Unable to delete test user: " + e.getMessage());
        }
        try {
            APILocator.getLayoutAPI()
                    .removeLayout(APILocator.getLayoutAPI().findLayout(layout.getId()));
        } catch (final Exception e) {
            Logger.warn(PortletRequestProcessorAccessTest.class,
                    "Unable to delete test layout: " + e.getMessage());
        }
    }

    /**
     * Method to test: {@link PortletRequestProcessor#processRoles(HttpServletRequest,
     * HttpServletResponse, ActionMapping)}
     * Given: a render request for a restricted path, from a user who does not hold the portlet
     * that owns it
     * Expected: the request is stopped and forwarded to the access denied page
     */
    @Test
    public void processRoles_restrictedRenderRequest_isStoppedAndForwarded() throws Exception {
        final PortletRequestProcessor processor = processor();
        final HttpServletRequest request = request(contentPortletUser, mock(RenderResponse.class));

        assertFalse(processor.processRoles(request, mock(HttpServletResponse.class),
                mapping(RESTRICTED_PATH)));
        verify(processor).processForwardConfig(any(), any(), any(ForwardConfig.class));
    }

    /**
     * Method to test: {@link PortletRequestProcessor#processRoles(HttpServletRequest,
     * HttpServletResponse, ActionMapping)}
     * Given: an action request for a restricted path, where the portlet response is an action
     * response rather than a render response
     * Expected: the request is stopped without forwarding, since the forward writes through a
     * render response the action phase does not have
     */
    @Test
    public void processRoles_restrictedActionRequest_isStoppedWithoutForwarding() throws Exception {
        final PortletRequestProcessor processor = processor();
        final HttpServletRequest request = request(contentPortletUser, mock(ActionResponse.class));

        assertFalse(processor.processRoles(request, mock(HttpServletResponse.class),
                mapping(RESTRICTED_PATH)));
        verify(processor, never()).processForwardConfig(any(), any(), any(ForwardConfig.class));
    }

    /**
     * Method to test: {@link PortletRequestProcessor#processRoles(HttpServletRequest,
     * HttpServletResponse, ActionMapping)}
     * Given: a request for a path outside the restricted directories
     * Expected: the request continues, whoever asks
     */
    @Test
    public void processRoles_unrestrictedPath_continues() throws Exception {
        final PortletRequestProcessor processor = processor();
        final HttpServletRequest request = request(contentPortletUser, mock(RenderResponse.class));

        assertTrue(processor.processRoles(request, mock(HttpServletResponse.class),
                mapping(UNRESTRICTED_PATH)));
        verify(processor, never()).processForwardConfig(any(), any(), any(ForwardConfig.class));
    }

    /**
     * Method to test: {@link PortletRequestProcessor#processRoles(HttpServletRequest,
     * HttpServletResponse, ActionMapping)}
     * Given: a render request for a restricted path from a CMS administrator
     * Expected: the request continues
     */
    @Test
    public void processRoles_restrictedPathAsAdministrator_continues() throws Exception {
        final PortletRequestProcessor processor = processor();
        final HttpServletRequest request = request(adminUser, mock(RenderResponse.class));

        assertTrue(processor.processRoles(request, mock(HttpServletResponse.class),
                mapping(RESTRICTED_PATH)));
    }

    /**
     * Builds a processor whose real {@code processRoles} runs while the forward it may emit is
     * recorded instead of executed, since emitting it needs the Struts module the processor is
     * normally initialised with.
     *
     * @return the processor under test
     * @throws Exception if the forward cannot be stubbed
     */
    private static PortletRequestProcessor processor() throws Exception {
        final PortletRequestProcessor processor =
                mock(PortletRequestProcessor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        doNothing().when(processor).processForwardConfig(any(), any(), any(ForwardConfig.class));
        return processor;
    }

    /**
     * Builds a mapping that answers the given action path, as Struts resolves it from
     * {@code struts-config.xml} before the processor runs.
     *
     * @param path the struts action path
     * @return the mapping under test
     */
    private static ActionMapping mapping(final String path) {
        final ActionMapping mapping = mock(ActionMapping.class);
        when(mapping.getPath()).thenReturn(path);
        when(mapping.findForward(any())).thenReturn(mock(ActionForward.class));
        return mapping;
    }

    /**
     * Builds a request carrying the user and the portlet response that tells the two phases apart.
     *
     * @param user            the {@link User} the request belongs to
     * @param portletResponse the portlet response stored for the phase under test
     * @return the request under test
     */
    private static HttpServletRequest request(final User user, final Object portletResponse) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        when(request.getAttribute(WebKeys.JAVAX_PORTLET_RESPONSE)).thenReturn(portletResponse);
        return request;
    }
}
