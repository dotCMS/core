package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.UnitTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.List;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link ContainerWebAPI}
 */
public class ContainerWebAPITest extends UnitTestBase {

    private PermissionAPI permissionAPI;
    private ContainerAPI containerAPI;
    private UserAPI userAPI;
    private UserWebAPI userWebAPI;
    private HttpServletRequest mockHttpServletRequest;

    private ContainerWebAPI containerWebAPI;

    private ViewContext initData;

    private HttpSession mockHttpSession;

    @Before
    public void before(){
        permissionAPI = mock(PermissionAPI.class);
        containerAPI = mock(ContainerAPI.class);;
        userAPI = mock(UserAPI.class);
        userWebAPI = mock(UserWebAPI.class);
        containerWebAPI = new ContainerWebAPI(permissionAPI, containerAPI, userAPI, userWebAPI);
        initData = mock(ViewContext.class);

        mockHttpServletRequest = mock(HttpServletRequest.class);
        mockHttpSession = mock(HttpSession.class);
        when(mockHttpServletRequest.getSession()).thenReturn(mockHttpSession);

        when(initData.getRequest()).thenReturn(mockHttpServletRequest);
        when(initData.getVelocityContext()).thenReturn(mock(Context.class));
    }

    @Test
    public void doesUserHasPermissionToAddContentWithNullParameter() throws DotDataException {
        assertFalse(containerWebAPI.doesUserHasPermissionToAddContent(null));
    }

    @Test
    public void doesUserHasPermissionToAddContent() throws DotDataException, DotSecurityException {
        String containerInode = "12345";
        User systemUser = mock(User.class);
        User backUser = mock(User.class);
        Container container = mock(Container.class);
        List<ContentType> contentTypeList = list(mock(ContentType.class));

        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(containerAPI.find(containerInode, systemUser, false)).thenReturn(container);
        when(userWebAPI.getUser(mockHttpServletRequest)).thenReturn(backUser);
        when(containerAPI.getContentTypesInContainer(backUser, container)).thenReturn(contentTypeList);

        containerWebAPI.init(initData);

        assertTrue(containerWebAPI.doesUserHasPermissionToAddContent(containerInode));
    }

    @Test
    public void doesUserHasPermissionToAddContentEmptyList() throws DotDataException, DotSecurityException {
        String containerInode = "12345";
        User systemUser = mock(User.class);
        User backUser = mock(User.class);
        Container container = mock(Container.class);
        List<ContentType> contentTypeList = list();

        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(containerAPI.find(containerInode, systemUser, false)).thenReturn(container);
        when(userWebAPI.getUser(mockHttpServletRequest)).thenReturn(backUser);
        when(containerAPI.getContentTypesInContainer(backUser, container)).thenReturn(contentTypeList);

        containerWebAPI.init(initData);

        assertFalse(containerWebAPI.doesUserHasPermissionToAddContent(containerInode));
    }

    @Test
    public void doesUserHasPermissionToAddContentNullList() throws DotDataException, DotSecurityException {
        String containerInode = "12345";
        User systemUser = mock(User.class);
        User backUser = mock(User.class);
        Container container = mock(Container.class);
        List<ContentType> contentTypeList = null;

        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(containerAPI.find(containerInode, systemUser, false)).thenReturn(container);
        when(userWebAPI.getUser(mockHttpServletRequest)).thenReturn(backUser);
        when(containerAPI.getContentTypesInContainer(backUser, container)).thenReturn(contentTypeList);

        containerWebAPI.init(initData);

        assertFalse(containerWebAPI.doesUserHasPermissionToAddContent(containerInode));
    }

    @Test
    public void doesUserHasPermissionToAddContentThrowDotDataExceptionGettingSystemUser() throws DotSecurityException {
        String containerInode = "12345";

        DotDataException ex = mock(DotDataException.class);
        User backUser = mock(User.class);
        User systemUser = mock(User.class);

        when(userWebAPI.getUser(mockHttpServletRequest)).thenReturn(backUser);
        try {
            when(userAPI.getSystemUser()).thenThrow(ex);
        } catch (DotDataException e) {

        }

        containerWebAPI.init(initData);

        try {
            containerWebAPI.doesUserHasPermissionToAddContent(containerInode);
            assertTrue(false);
        } catch(DotDataException e) {
            assertTrue(true);
        }
    }

    @Test
    public void doesUserHasPermissionToAddContentThrowDotDataExceptionFindingContainer() throws DotSecurityException {
        String containerInode = "12345";

        DotDataException ex = mock(DotDataException.class);
        User backUser = mock(User.class);
        User systemUser = mock(User.class);

        when(userWebAPI.getUser(mockHttpServletRequest)).thenReturn(backUser);

        try {
            when(userAPI.getSystemUser()).thenReturn(systemUser);
            when(containerAPI.find(containerInode, systemUser, false)).thenThrow(ex);
        } catch(DotDataException e) {

        }

        containerWebAPI.init(initData);

        try {
            containerWebAPI.doesUserHasPermissionToAddContent(containerInode);
            assertTrue(false);
        } catch(DotDataException e) {
            assertTrue(true);
        }
    }

    @Test
    public void doesUserHasPermissionToAddContentThrowDotSecurityExceptionFindingContainer() throws DotDataException {
        String containerInode = "12345";

        DotSecurityException ex = mock(DotSecurityException.class);
        User backUser = mock(User.class);
        User systemUser = mock(User.class);

        when(userWebAPI.getLoggedInUser(mockHttpSession)).thenReturn(backUser);
        when(userAPI.getSystemUser()).thenReturn(systemUser);

        try {
            when(containerAPI.find(containerInode, systemUser, false)).thenThrow(ex);
        } catch(DotSecurityException e) {

        }

        containerWebAPI.init(initData);

        try {
            containerWebAPI.doesUserHasPermissionToAddContent(containerInode);
            assertTrue(false);
        } catch(DotRuntimeException e) {
            assertEquals(ex, e.getCause());
        }
    }
}
