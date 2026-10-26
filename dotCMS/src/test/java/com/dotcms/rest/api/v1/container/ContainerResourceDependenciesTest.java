package com.dotcms.rest.api.v1.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.dotcms.util.PaginationUtil;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@code GET /v1/containers/dependencies} on {@link ContainerResource}: the
 * endpoint that lists the templates that use a container.
 */
class ContainerResourceDependenciesTest {

    private static final String INODE = "0123456789abcdef";

    private WebResource webResource;
    private ContainerAPI containerAPI;
    private ShortyIdAPI shortyAPI;
    private ContainerDependenciesHelper helper;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private User user;
    private ContainerResource resource;
    private MockedStatic<WebAPILocator> webAPILocator;

    @BeforeEach
    void setUp() {
        webResource = mock(WebResource.class);
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

        resource = new ContainerResource(webResource, mock(PaginationUtil.class), mock(FormAPI.class),
                containerAPI, mock(VersionableAPI.class), mock(VelocityUtil.class), shortyAPI,
                mock(ContentletAPI.class), helper);
    }

    @AfterEach
    void tearDown() {
        webAPILocator.close();
    }

    /**
     * Given a request with no container id (null or blank), when the dependencies are requested,
     * then an {@link IllegalArgumentException} is raised and no lookup runs.
     */
    @Test
    void rejectsAMissingContainerId() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> resource.getContainerDependencies(request, response, null));
        assertThrows(IllegalArgumentException.class,
                () -> resource.getContainerDependencies(request, response, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> resource.getContainerDependencies(request, response, List.of("  ", "")));

        verify(helper, never()).dependenciesOf(any(), any());
    }

    /**
     * Given more container ids than the endpoint accepts, when the dependencies are requested,
     * then an {@link IllegalArgumentException} is raised and no lookup runs.
     */
    @Test
    void rejectsMoreContainerIdsThanTheLimit() throws Exception {
        final List<String> tooMany = IntStream.rangeClosed(1, ContainerResource.MAX_DEPENDENCY_LOOKUP + 1)
                .mapToObj(i -> "id-" + i).collect(Collectors.toList());

        assertThrows(IllegalArgumentException.class,
                () -> resource.getContainerDependencies(request, response, tooMany));

        verify(helper, never()).dependenciesOf(any(), any());
    }

    /**
     * Given an inode that resolves to a container the user can read, when the dependencies are
     * requested, then the response is 200 and its entity is a list with the dependencies view of
     * that container.
     */
    @Test
    void returnsTheDependenciesOfTheWorkingContainer() throws Exception {
        final Container container = resolvable(INODE, "container-id");
        final ContainerDependenciesView view = new ContainerDependenciesView(INODE, "container-id", "Main", List.of());
        when(helper.dependenciesOf(List.of(container), user)).thenReturn(List.of(view));

        final Response result = resource.getContainerDependencies(request, response, List.of(INODE));

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertEquals(List.of(view), ((ResponseEntityView<?>) result.getEntity()).getEntity());
    }

    /**
     * Given two containers and a repeated id, when the dependencies are requested, then the
     * containers are looked up together, once each, in the order asked.
     */
    @Test
    void looksUpSeveralContainersTogetherInTheOrderAsked() throws Exception {
        final String otherInode = "fedcba9876543210";
        final Container first = resolvable(INODE, "first-id");
        final Container second = resolvable(otherInode, "second-id");
        final List<ContainerDependenciesView> views = List.of(
                new ContainerDependenciesView(INODE, "first-id", "First", List.of()),
                new ContainerDependenciesView(otherInode, "second-id", "Second", List.of()));
        when(helper.dependenciesOf(List.of(second, first), user)).thenReturn(views);

        final Response result = resource.getContainerDependencies(request, response,
                List.of(otherInode, INODE, otherInode));

        assertEquals(views, ((ResponseEntityView<?>) result.getEntity()).getEntity());
        verify(helper, times(1)).dependenciesOf(any(), any());
    }

    private Container resolvable(final String inode, final String identifier) throws Exception {
        final Container container = new Container();
        container.setInode(inode);
        container.setIdentifier(identifier);
        when(shortyAPI.getShorty(inode)).thenReturn(Optional.of(new ShortyId(inode, inode, ShortType.INODE, ShortType.INODE)));
        when(containerAPI.find(inode, user, false)).thenReturn(container);
        return container;
    }

    /**
     * Given an inode that resolves to a container without identifier, when the dependencies are
     * requested, then the container is reported as not found and no lookup runs.
     */
    @Test
    void reportsAContainerWithoutIdentifierAsNotFound() throws Exception {
        when(shortyAPI.getShorty(INODE)).thenReturn(Optional.of(new ShortyId(INODE, INODE, ShortType.INODE, ShortType.INODE)));
        when(containerAPI.find(INODE, user, false)).thenReturn(new Container());

        assertThrows(DoesNotExistException.class,
                () -> resource.getContainerDependencies(request, response, List.of(INODE)));

        verify(helper, never()).dependenciesOf(any(), any());
    }

    /**
     * Given an id that matches no container, when the dependencies are requested, then the
     * not-found exception of the sibling lookups is raised and no lookup runs.
     */
    @Test
    void reportsAnUnknownContainerAsNotFound() throws Exception {
        when(shortyAPI.getShorty("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> resource.getContainerDependencies(request, response, List.of("unknown")));

        verify(helper, never()).dependenciesOf(any(), any());
    }
}
