package com.dotmarketing.business.web;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class HostWebAPIImplIntegrationTest extends IntegrationTestBase {

    public static final String HOST_ID_PARAMETER_NAME = "host_id";

    @BeforeClass
    public static void prepare () throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: request host_id parameter is set and the user is a backend user
     * Should: host_id parameter be take over any other source
     */
    @Test
    public void useHostIDRequestAsCurrentHost() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        setAllParametersAndAttributes(request, session);
        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(host.getIdentifier());

        final User user = createBackendUser(host);

        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();

        final Host currentHost = hostWebAPI.getCurrentHost(request, user);

        assertEquals(host.getIdentifier(), currentHost.getIdentifier());
        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
        verify(session).setAttribute(WebKeys.CURRENT_HOST, currentHost);
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: request host_id parameter is set and the user is not a backend user
     * Should: Host parameter should by taken
     */
    @Test
    public void useHostIDRequestAsCurrentHostNotBackendUser() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Host anotherHost = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        setAllParametersAndAttributes(request, session, anotherHost);
        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(host.getIdentifier());

        final User user = createUser(host);

        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();

        final Host currentHost = hostWebAPI.getCurrentHost(request, user);

        assertEquals(anotherHost.getIdentifier(), currentHost.getIdentifier());
        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: request host_id parameter is set and the user is a backend user but don't have permission
     * Should: throw a {@link DotSecurityException}
     */
    @Test
    public void useHostIdAndTheUserDontHavePermission() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        setAllParametersAndAttributes(request, session);
        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(host.getIdentifier());

        final User user = createBackendUser();

        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();

        try {
            hostWebAPI.getCurrentHost(request, user);
            throw new RuntimeException("should throw a DotSecurityException");
        } catch (DotSecurityException e) {

        }

        verify(request, never()).setAttribute(anyString(), any());
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: request Host parameter is set and the user is a backend user
     * Should: Host parameter should be taken
     */
    @Test
    public void useHostNameParameterRequestAsCurrentHost() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        setAllParametersAndAttributes(request, session);
        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(null);
        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(host.getName());

        final User user = createBackendUser(host);

        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();

        final Host currentHost = hostWebAPI.getCurrentHost(request, user);

        assertEquals(host.getIdentifier(), currentHost.getIdentifier());
        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
        verify(session).setAttribute(WebKeys.CURRENT_HOST, currentHost);
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: request Host parameter is set and the user is not a backend user
     * Should: Host parameter should be taken
     */
    @Test
    public void useHostNameParameterRequestAsCurrentHostNotBackEndUser() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        setAllParametersAndAttributes(request, session);
        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(null);
        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(host.getName());

        final User user = createUser(host);

        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();

        final Host currentHost = hostWebAPI.getCurrentHost(request, user);

        assertEquals(host.getIdentifier(), currentHost.getIdentifier());

        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: request Host parameter is set and the user is a backend user but not have permission
     * Should: throw {@link DotSecurityException}
     */
    @Test
    public void useHostNameParameterAndUserNotHavePermission() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        setAllParametersAndAttributes(request, session);
        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(null);
        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(host.getName());

        final User user = createBackendUser();

        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();

        try {
            hostWebAPI.getCurrentHost(request, user);
            throw new RuntimeException("should throw a DotSecurityException");
        } catch (DotSecurityException e) {

        }

        verify(request, never()).setAttribute(anyString(), any());
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: request Host parameter is set and the user is not a backend user
     * Should: return the host
     */
    @Test
    public void useHostNameParameterAndNotBackendUserNotHavePermission() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        setAllParametersAndAttributes(request, session);
        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(null);
        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(host.getName());

        final User user = createUser();

        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();

        final Host currentHost = hostWebAPI.getCurrentHost(request, user);
        assertEquals(host.getIdentifier(), currentHost.getIdentifier());

        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: request Host parameter is set and the user is not a backend user
     * Should: Host parameter should be taken
     */
    @Test
    public void useHostAttributeRequestAsCurrentHost() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        setAllParametersAndAttributes(request, session);
        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(null);
        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(null);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);

        final User user = createUser(host);

        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();

        final Host currentHost = hostWebAPI.getCurrentHost(request, user);
        assertEquals(host.getIdentifier(), currentHost.getIdentifier());

        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: request Host parameter is set and the user is  a backend user and not have permission
     * Should: throw {@link DotSecurityException}
     */
    @Test
    public void useHostAttributeRequestNotPermission() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        setAllParametersAndAttributes(request, session);
        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(null);
        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(null);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);

        final User user = createBackendUser();
        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();

        try {
            hostWebAPI.getCurrentHost(request, user);
            throw new RuntimeException("should throw a DotSecurityException");
        } catch (DotSecurityException e) {

        }

        verify(request, never()).setAttribute(anyString(), any());
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: request Host parameter is set and the user is  not a backend user
     * Should: return the host
     */
    @Test
    public void useHostAttributeRequestNotPermissionAndNotBackendUser() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        setAllParametersAndAttributes(request, session);
        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(null);
        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(null);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);

        final User user = createUser();
        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();
        final Host currentHost = hostWebAPI.getCurrentHost(request, user);

        assertEquals(host.getIdentifier(), currentHost.getIdentifier());

        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: session Host parameter is set and the user is a backend user
     * Should: session Host parameter should be taken
     */
    @Test
    public void useHostAttributeSessionAsCurrentHost() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        setAllParametersAndAttributes(request, session);
        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(null);
        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(null);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(null);
        when(session.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);
        when(session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID)).thenReturn(host.getIdentifier());

        final User user = createBackendUser(host);
        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();
        final Host currentHost = hostWebAPI.getCurrentHost(request, user);

        assertEquals(host.getIdentifier(), currentHost.getIdentifier());
        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
        verify(session).setAttribute(WebKeys.CURRENT_HOST, currentHost);
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: session Host parameter is set and the user is a backend user and not have permission
     * Should: throw {@link DotSecurityException}
     */
    @Test
    public void useHostAttributeSessionNotPermission() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        setAllParametersAndAttributes(request, session);
        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(null);
        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(null);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(null);
        when(session.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);

        final User user = createBackendUser();

        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();

        try {
            hostWebAPI.getCurrentHost(request, user);
            throw new RuntimeException("should throw a DotSecurityException");
        } catch (DotSecurityException e) {

        }

        verify(request, never()).setAttribute(anyString(), any());
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: session Host parameter is set and the user is a not backend user
     * Should: should return default host
     */
    @Test
    public void useHostAttributeSessionNotBackendUser() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        when(session.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), true);
        final Role role = new RoleDataGen().nextPersisted();
        final User user = createUser(role, defaultHost);

        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();
        this.addPermission(role, defaultHost);

        final Host currentHost = hostWebAPI.getCurrentHost(request, user);

        assertEquals(defaultHost.getIdentifier(), currentHost.getIdentifier());

        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: session {@link WebKeys#CMS_SELECTED_HOST_ID} parameter is set and the user is a backend user
     * Should: session {@link WebKeys#CMS_SELECTED_HOST_ID} parameter should be taken
     */
    @Test
    public void useHostIdAttributeSessionAsCurrentHost() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        setAllParametersAndAttributes(request, session);
        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(null);
        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(null);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(null);
        when(session.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(null);
        when(session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID)).thenReturn(host.getIdentifier());

        final User user = createBackendUser(host);

        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();

        final Host currentHost = hostWebAPI.getCurrentHost(request, user);

        assertEquals(host.getIdentifier(), currentHost.getIdentifier());

        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
        verify(session).setAttribute(WebKeys.CURRENT_HOST, currentHost);
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: session {@link WebKeys#CMS_SELECTED_HOST_ID} parameter is set and the user is not backend user
     * Should: session {@link WebKeys#CMS_SELECTED_HOST_ID} parameter should not be taken and return the default host
     */
    @Test
    public void useHostIdAttributeSessionAsCurrentHostNotBackendUser() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        when(session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID)).thenReturn(host.getIdentifier());

        final Role role = new RoleDataGen().nextPersisted();
        final User user = createUser(role, host);

        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();
        final Host defaultHost = hostWebAPI.findDefaultHost(APILocator.systemUser(), true);

        this.addPermission(role, defaultHost);

        final Host currentHost = hostWebAPI.getCurrentHost(request, user);

        assertEquals(defaultHost.getIdentifier(),  currentHost.getIdentifier());

        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: session {@link WebKeys#CMS_SELECTED_HOST_ID} parameter is set and the user is a backend user and not have permission
     * Should: throw a {@link DotSecurityException}
     */
    @Test
    public void useHostIdAttributeSessionNotPermission() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        setAllParametersAndAttributes(request, session);
        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(null);
        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(null);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(null);
        when(session.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(null);
        when(session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID)).thenReturn(host.getIdentifier());

        final User user = createBackendUser();

        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();

        try {
            hostWebAPI.getCurrentHost(request, user);
            throw new RuntimeException("should throw a DotSecurityException");
        } catch (DotSecurityException e) {

        }

        verify(request, never()).setAttribute(anyString(), any());
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: the current host is not set in any scope
     * Should: take the {@link HttpServletRequest#getServerName()}
     */
    @Test
    public void useRequestServerNameAsCurrentHost() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(request.getServerName()).thenReturn(host.getName());

        final User user = createUser(host);
        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();
        final Host currentHost = hostWebAPI.getCurrentHost(request, user);

        assertEquals(host.getIdentifier(), currentHost.getIdentifier());

        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: the current host is not set in any scope and the {@link HttpServletRequest#getServerName()} is not a host
     * Should: take the default host
     */
    @Test
    public void whenRequestServerNameNosExistsAsDefaultHost() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(request.getServerName()).thenReturn("not_exists_host");

        final Role role = new RoleDataGen().nextPersisted();
        final User user = createUser(role, host);
        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();
        final Host defaultHost = hostWebAPI.findDefaultHost(APILocator.systemUser(), true);

        this.addPermission(role, defaultHost);

        final Host currentHost = hostWebAPI.getCurrentHost(request, user);

        assertEquals(defaultHost.getIdentifier(), currentHost.getIdentifier());

        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: There is not session
     * Should: not throw a NullPointerException and return default host
     */
    @Test
    public void whenNoSessionShouldNotThrowNullPointerException() throws DotDataException, DotSecurityException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        final Role role = new RoleDataGen().nextPersisted();
        final User user = createBackendUser(role);
        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();
        final Host defaultHost = hostWebAPI.findDefaultHost(APILocator.systemUser(), true);
        this.addPermission(role, defaultHost);

        final Host currentHost = hostWebAPI.getCurrentHost(request, user);

        assertEquals(defaultHost.getIdentifier(), currentHost.getIdentifier());

        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: the current host is not set in any scope and the {@link HttpServletRequest#getServerName()} is not a host
     *       but the back end user not have permission over the default host
     * Should: throw a {@link DotSecurityException}
     */
    @Test
    public void whenDontHavePermissionInDefaultHost() throws DotDataException, DotSecurityException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(request.getServerName()).thenReturn("not_exists_host");

        final User user = createBackendUser();
        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();

        try {
            hostWebAPI.getCurrentHost(request, user);
        }catch(DotSecurityException e) {

        }

        verify(request, never()).setAttribute(anyString(), any());
        verify(session, never()).setAttribute(anyString(), any());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: the current host is not set in any scope and the {@link HttpServletRequest#getServerName()} is not a host
     *       but the user not have permission over the default host but this is not a backend user
     * Should: return default host
     */
    @Test
    public void whenDontHavePermissionInDefaultHostNotBackendUser() throws DotDataException, DotSecurityException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(request.getServerName()).thenReturn("not_exists_host");

        final User user = createUser();
        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();
        final Host defaultHost = hostWebAPI.findDefaultHost(APILocator.systemUser(), true);

        final Host currentHost = hostWebAPI.getCurrentHost(request, user);
        assertEquals(defaultHost.getIdentifier(), currentHost.getIdentifier());

        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
    }

    /**
     * Method to test: {@link HostWebAPIImpl#getCurrentHost(HttpServletRequest, User)} (HttpServletRequest)}
     * When: session {@link WebKeys#CURRENT_HOST} attribute is set but is not equals to {@link WebKeys#CMS_SELECTED_HOST_ID}
     * Should: session {@link WebKeys#CMS_SELECTED_HOST_ID} attribute should be taken
     */
    @Test
    public void whenBothAreSetInSessionUseHostId() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Host anotherHost = new SiteDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);

        when(session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID)).thenReturn(host.getIdentifier());
        when(session.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(anotherHost);

        final Role role = new RoleDataGen().nextPersisted();
        final User user = createBackendUser(role, host);

        this.addPermission(role, host);

        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();
        final Host currentHost = hostWebAPI.getCurrentHost(request, user);

        assertEquals(host.getIdentifier(), currentHost.getIdentifier());
        verify(request).setAttribute(WebKeys.CURRENT_HOST, currentHost);
        verify(session).setAttribute(WebKeys.CURRENT_HOST, currentHost);
    }

    /**
     * Method to test: {@link HostWebAPIImpl#find(Contentlet, User, boolean )}}
     * When: Contentlet.getHost() returns null
     * Should: fallback to Contentlet.getContentType().host() to fetch the actual host.
     */
    @Test
    public void findHostByContentletWithNullHost() throws DotSecurityException, DotDataException {
        final User user = createBackendUser();
        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();
        final Contentlet contentlet = mock(Contentlet.class);
        final ContentType contentType = mock(ContentType.class);
        when(contentlet.getHost()).thenReturn(null);
        when(contentlet.getContentType()).thenReturn(contentType);
        final Host host = hostWebAPI.findSystemHost(user, false);
        when(contentType.host()).thenReturn(host.getHost());
        assertEquals(host.getHost(), hostWebAPI.find(contentlet, user, false).getHost());
    }

    /**
     * Method to test: {@link HostWebAPIImpl#find(Contentlet, User, boolean )}}
     * When: Contentlet.getHost() returns some id
     * Should: use it to fetch the actual host.
     */
    @Test
    public void findHostByContentlet() throws DotSecurityException, DotDataException {
        final User user = createBackendUser();
        final HostWebAPIImpl hostWebAPI = new HostWebAPIImpl();
        final Contentlet contentlet = mock(Contentlet.class);
        final Host host = hostWebAPI.findSystemHost(user, false);
        when(contentlet.getHost()).thenReturn(host.getHost());
        final ContentType contentType = mock(ContentType.class);
        when(contentlet.getContentType()).thenReturn(contentType);
        when(contentType.host()).thenReturn(host.getHost());
        assertEquals(host.getHost(), hostWebAPI.find(contentlet, user, false).getHost());
    }

    private User createUser() throws DotDataException, DotSecurityException {
        return createUser( null);
    }

    private User createUser(final Host host) throws DotDataException, DotSecurityException {
        return createUser(null, host);
    }

    private User createUser(final Role roleParam, final Host host)
            throws DotDataException, DotSecurityException {
        final Role role = roleParam == null ? new RoleDataGen().nextPersisted() : roleParam
                ;
        final User user = new UserDataGen().roles(role).nextPersisted();

        if (host != null) {
            this.addPermission(role, host);
        }

        return user;
    }

    private User createBackendUser( final Host host)
            throws DotDataException, DotSecurityException {
        return createBackendUser(null, host);
    }

    private User createBackendUser( final Role role)
            throws DotDataException, DotSecurityException {
        return createBackendUser(role, null);
    }

    private User createBackendUser()
            throws DotDataException, DotSecurityException {
        return createBackendUser(null, null);
    }

    private User createBackendUser(final Role role, final Host host)
            throws DotDataException, DotSecurityException {
        final User user = createUser(role, host);
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadBackEndUserRole(), user);
        return user;
    }

    private void addPermission(final Role role, final Host host)
            throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();

        final Permission permission = new Permission();
        permission.setInode(host.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(PermissionAPI.PERMISSION_READ);

        APILocator.getPermissionAPI().save(CollectionsUtils.list(permission), host, systemUser, false);
    }

    private void setAllParametersAndAttributes(final HttpServletRequest request, final HttpSession session) {
        setAllParametersAndAttributes(request, session, null);
    }

    private void setAllParametersAndAttributes(final HttpServletRequest request, final HttpSession session, final Host host) {
        when(request.getSession(false)).thenReturn(session);

        final Host anotherHost = host == null ? new SiteDataGen().nextPersisted() : host;

        when(request.getParameter(HOST_ID_PARAMETER_NAME)).thenReturn(anotherHost.getIdentifier());
        when(request.getParameter(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(anotherHost.getName());
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(anotherHost);
        when(request.getServerName()).thenReturn(anotherHost.getName());

        when(session.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(anotherHost);
        when(session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID)).thenReturn(anotherHost.getIdentifier());
    }
}
