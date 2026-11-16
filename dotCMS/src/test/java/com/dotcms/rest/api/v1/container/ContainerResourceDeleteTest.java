package com.dotcms.rest.api.v1.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.BulkResultView;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.util.PaginationUtil;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the delete endpoints of {@link ContainerResource}: a container that a template
 * still uses must not be deleted, neither one by one nor in bulk.
 */
class ContainerResourceDeleteTest {

    private static final String FREE_INODE = "free-inode-0000000";
    private static final String USED_INODE = "used-inode-0000000";

    private ContainerAPI containerAPI;
    private ShortyIdAPI shortyAPI;
    private ContainerDependenciesHelper helper;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private User user;
    private Container freeContainer;
    private Container usedContainer;
    private ContainerResource resource;
    private MockedStatic<WebAPILocator> webAPILocator;
    private MockedStatic<ActivityLogger> activityLogger;
    private MockedStatic<LanguageUtil> languageUtil;

    @BeforeEach
    void setUp() throws Exception {
        final WebResource webResource = mock(WebResource.class);
        containerAPI = mock(ContainerAPI.class);
        shortyAPI = mock(ShortyIdAPI.class);
        helper = mock(ContainerDependenciesHelper.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        user = mock(User.class);

        final InitDataObject initData = mock(InitDataObject.class);
        when(initData.getUser()).thenReturn(user);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initData);

        final HostWebAPI hostWebAPI = mock(HostWebAPI.class);
        when(hostWebAPI.getHost(request)).thenReturn(mock(Host.class));
        webAPILocator = mockStatic(WebAPILocator.class);
        webAPILocator.when(WebAPILocator::getHostWebAPI).thenReturn(hostWebAPI);
        // the activity log reads its settings from the database, which a unit test does not have
        activityLogger = mockStatic(ActivityLogger.class);
        // the message comes from Language.properties, which needs the company and the database
        languageUtil = mockStatic(LanguageUtil.class);
        languageUtil.when(() -> LanguageUtil.get(eq(user), eq("message.containers.delete.in_use"), any()))
                .thenAnswer(invocation -> "in use by " + invocation.getArgument(2) + " template(s)");
        // an AJAX request makes PageMode.get return its default mode without touching the session
        when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");

        freeContainer = container(FREE_INODE, "free-id");
        usedContainer = container(USED_INODE, "used-id");
        resolves(freeContainer);
        resolves(usedContainer);
        when(helper.countTemplatesUsing(any())).thenAnswer(invocation -> {
            final Collection<Container> asked = invocation.getArgument(0);
            return asked.stream().collect(Collectors.toMap(Container::getIdentifier,
                    container -> container.equals(usedContainer) ? 2 : 0));
        });
        when(containerAPI.delete(any(Container.class), any(User.class), anyBoolean())).thenReturn(true);

        resource = new ContainerResource(webResource, mock(PaginationUtil.class), mock(FormAPI.class),
                containerAPI, mock(VersionableAPI.class), mock(VelocityUtil.class), shortyAPI,
                mock(ContentletAPI.class), helper);
    }

    @AfterEach
    void tearDown() {
        webAPILocator.close();
        activityLogger.close();
        languageUtil.close();
    }

    /**
     * Given a container that no template uses, when it is deleted, then the delete runs and the
     * response is true.
     */
    @Test
    void deletesAContainerNoTemplateUses() throws Exception {
        final Response result = resource.delete(request, response, FREE_INODE);

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertEquals(true, ((ResponseEntityView<?>) result.getEntity()).getEntity());
        verify(containerAPI).delete(freeContainer, user, PageMode.get(request).respectAnonPerms);
    }

    /**
     * Given a container that templates use, when it is deleted, then a conflict is raised with the
     * message of {@code Language.properties} and nothing is deleted.
     */
    @Test
    void refusesToDeleteAContainerATemplateUses() throws Exception {
        final ConflictException conflict = assertThrows(ConflictException.class,
                () -> resource.delete(request, response, USED_INODE));

        assertEquals("in use by 2 template(s)", conflict.getMessage());
        verify(containerAPI, never()).delete(any(Container.class), any(User.class), anyBoolean());
    }

    /**
     * Given a bulk request with a free container and one that templates use, when it is
     * deleted, then the free one is deleted and the other is reported as a failure.
     */
    @Test
    void bulkDeleteReportsTheContainersInUseAsFailures() throws Exception {
        final Response result = resource.bulkDelete(request, response, List.of(FREE_INODE, USED_INODE));

        final BulkResultView view = (BulkResultView) ((ResponseEntityView<?>) result.getEntity()).getEntity();
        assertEquals(Long.valueOf(1L), view.getSuccessCount());
        assertEquals(1, view.getFails().size());
        assertEquals(USED_INODE, view.getFails().get(0).getElement());
        assertEquals("in use by 2 template(s)", view.getFails().get(0).getErrorMessage());
        verify(containerAPI).delete(freeContainer, user, PageMode.get(request).respectAnonPerms);
        verify(containerAPI, never()).delete(usedContainer, user, PageMode.get(request).respectAnonPerms);
    }

    /**
     * Given a bulk request with several containers, when it is deleted, then the templates are
     * searched once for all of them, not once per container.
     */
    @Test
    void bulkDeleteSearchesTheTemplatesOnce() throws Exception {
        resource.bulkDelete(request, response, List.of(FREE_INODE, USED_INODE));

        verify(helper, times(1)).countTemplatesUsing(any());
    }

    /**
     * Given a user without WRITE permission on a container that templates use, when it is deleted,
     * then the permission error is raised, not the conflict, and the templates are not even searched.
     */
    @Test
    void refusesAUserWithoutWritePermissionBeforeLookingAtTemplates() throws Exception {
        doThrow(new DotSecurityException("no write")).when(helper)
                .requireWrite(eq(usedContainer), eq(user), anyBoolean());

        assertThrows(DotSecurityException.class, () -> resource.delete(request, response, USED_INODE));

        verify(helper, never()).countTemplatesUsing(any());
        verify(containerAPI, never()).delete(any(Container.class), any(User.class), anyBoolean());
    }

    /**
     * Given a bulk request where the user cannot write the container that templates use, when it is
     * deleted, then that container is reported with the permission error, not the in-use message.
     */
    @Test
    void bulkDeleteReportsAMissingWritePermissionInsteadOfTheUsage() throws Exception {
        doThrow(new DotSecurityException("no write")).when(helper)
                .requireWrite(eq(usedContainer), eq(user), anyBoolean());

        final Response result = resource.bulkDelete(request, response, List.of(FREE_INODE, USED_INODE));

        final BulkResultView view = (BulkResultView) ((ResponseEntityView<?>) result.getEntity()).getEntity();
        assertEquals(Long.valueOf(1L), view.getSuccessCount());
        assertEquals("no write", view.getFails().get(0).getErrorMessage());
    }

    /**
     * Given the System Container, when it is deleted, then the template scan is skipped and the
     * API's own refusal applies.
     */
    @Test
    void doesNotScanTemplatesForTheSystemContainer() throws Exception {
        final Container system = container("system-inode-00000", Container.SYSTEM_CONTAINER);
        resolves(system);

        resource.delete(request, response, "system-inode-00000");

        verify(helper, never()).countTemplatesUsing(any());
    }

    private void resolves(final Container container) throws Exception {
        final String inode = container.getInode();
        when(shortyAPI.getShorty(inode)).thenReturn(
                Optional.of(new ShortyId(inode, inode, ShortType.INODE, ShortType.INODE)));
        when(containerAPI.find(inode, user, false)).thenReturn(container);
    }

    private static Container container(final String inode, final String identifier) {
        final Container container = new Container();
        container.setInode(inode);
        container.setIdentifier(identifier);
        return container;
    }
}
