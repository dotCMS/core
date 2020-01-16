package com.dotmarketing.util;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HostUtilTest extends IntegrationTestBase {

    private static HostAPI hostAPI;
    private static UserAPI userAPI;

    private static User systemUser;
    private static User testUser;
    private static Host defaultHost;
    private static String defaultHostId;
    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        hostAPI = APILocator.getHostAPI();
        userAPI = APILocator.getUserAPI();

        //Setting the test user
        systemUser = userAPI.getSystemUser();
        testUser = userAPI.loadByUserByEmail("admin@dotcms.com", systemUser, false);
        defaultHost = hostAPI.findDefaultHost( systemUser, false );
        defaultHostId=defaultHost.getIdentifier();
    }

    /**
     * Test tryToFindCurrentHost without thread local request
     * @throws Exception
     */
    @Test
    public void tryToFindCurrentHost_no_request_on_threadlocal_expected_nopresent_Test () throws Exception {

        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        final Optional<Host> hostOpt = HostUtil.tryToFindCurrentHost(testUser);
        assertNotNull( hostOpt );
        assertFalse( hostOpt.isPresent() );
    }

    /**
     * Test tryToFindCurrentHost with thread local request, default host expected
     * @throws Exception
     */
    @Test
    public void tryToFindCurrentHost_with_request_on_threadlocal_expected_default_host_Test () throws Exception {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        final Optional<Host> hostOpt = HostUtil.tryToFindCurrentHost(testUser);
        assertNotNull( hostOpt );
        assertTrue( hostOpt.isPresent() );
        assertEquals("Should return the default one", hostOpt.get().getIdentifier(), defaultHost.getIdentifier());
    }

    /**
     * Test tryToFindCurrentHost with thread local request, with custom host, expected the custom.
     * @throws Exception
     */
    @Test
    public void tryToFindCurrentHost_with_request_on_threadlocal_expected_global_host_Test () throws Exception {

        // 1) create a new site
        // 2) mock everything
        // 3) set the host on the session
        final Host host = new SiteDataGen().name("custom" + System.currentTimeMillis() + ".dotcms.com").nextPersisted(true);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession        session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID)).thenReturn(host.getIdentifier());
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        final Optional<Host> hostOpt = HostUtil.tryToFindCurrentHost(testUser);
        assertNotNull( hostOpt );
        assertTrue( hostOpt.isPresent() );
        assertEquals("Should return the default one", hostOpt.get().getIdentifier(), host.getIdentifier());
    }
}
