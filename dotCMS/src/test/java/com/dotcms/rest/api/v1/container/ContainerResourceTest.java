package com.dotcms.rest.api.v1.container;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.util.PaginationUtil;
import com.dotcms.variant.VariantAPI;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link ContainerResource#containerContent(HttpServletRequest, HttpServletResponse, String, String, String)}:
 * the contentlet to render must be resolved with the caller's READ permission, whichever id shape is
 * used, and a denied request must answer with a generic message.
 */
class ContainerResourceTest {

    private static final String CONTAINER_ID = "SYSTEM_CONTAINER";
    private static final String IDENTIFIER = "c0ffee00-0000-4000-8000-000000000001";
    private static final String INODE = "c0ffee00-0000-4000-8000-000000000002";
    private static final String LIVE_INODE = "c0ffee00-0000-4000-8000-000000000003";
    private static final String CONTENT_TITLE = "Quarterly Results Draft";

    private final WebResource webResource = mock(WebResource.class);
    private final ShortyIdAPI shortyAPI = mock(ShortyIdAPI.class);
    private final ContentletAPI contentletAPI = mock(ContentletAPI.class);
    private final VelocityUtil velocityUtil = mock(VelocityUtil.class);
    private final VersionableAPI versionableAPI = mock(VersionableAPI.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final User user = mock(User.class);
    private final Contentlet contentlet = mock(Contentlet.class);

    private ContainerResource resource;

    @BeforeEach
    void setUp() {
        final InitDataObject initData = mock(InitDataObject.class);
        when(initData.getUser()).thenReturn(user);
        when(webResource.init(request, response, true)).thenReturn(initData);
        when(contentlet.getIdentifier()).thenReturn(IDENTIFIER);
        when(contentlet.getInode()).thenReturn(INODE);

        resource = new ContainerResource(webResource, mock(PaginationUtil.class), mock(FormAPI.class),
                mock(ContainerAPI.class), versionableAPI, velocityUtil, shortyAPI,
                contentletAPI);
    }

    /**
     * Given a contentlet addressed by its identifier, and a caller without READ permission on it
     * When the contentlet is rendered into a container
     * Then the request is forbidden and nothing is rendered
     */
    @Test
    void containerContent_byIdentifier_withoutReadPermission_isForbidden() throws Exception {
        givenIdentifierShorty();
        when(versionableAPI.findContentletVersionInfos(IDENTIFIER))
                .thenReturn(List.of(versionInfo(LIVE_INODE)));
        // Any lookup made on behalf of the caller is denied
        when(contentletAPI.find(anyString(), eq(user), anyBoolean()))
                .thenThrow(new DotSecurityException("no READ on " + CONTENT_TITLE));
        when(contentletAPI.findContentletByIdentifier(eq(IDENTIFIER), anyBoolean(), anyLong(),
                any(), eq(user), anyBoolean()))
                .thenThrow(new DotSecurityException("no READ on " + CONTENT_TITLE));

        // Rendering resolves the current site through WebAPILocator; fail fast here instead of
        // reaching for a database the unit environment does not have.
        try (MockedStatic<WebAPILocator> webApiLocator = mockStatic(WebAPILocator.class)) {
            webApiLocator.when(WebAPILocator::getHostWebAPI)
                    .thenThrow(new AssertionError("the contentlet must not be rendered"));

            assertThrows(ForbiddenException.class,
                    () -> resource.containerContent(request, response, CONTAINER_ID, IDENTIFIER, null));
        }

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a contentlet addressed by its inode, and a caller without READ permission on it
     * When the contentlet is rendered into a container
     * Then the request is forbidden and nothing is rendered
     */
    @Test
    void containerContent_byInode_withoutReadPermission_isForbidden() throws Exception {
        when(shortyAPI.getShorty(INODE)).thenReturn(Optional.of(
                new ShortyId(INODE, INODE, ShortType.INODE, ShortType.CONTENTLET)));
        when(contentletAPI.find(eq(INODE), eq(user), anyBoolean()))
                .thenThrow(new DotSecurityException("no READ on " + CONTENT_TITLE));

        assertThrows(ForbiddenException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, INODE, null));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a caller without READ permission on the requested contentlet
     * When the request is forbidden
     * Then the error message does not include the contentlet's title, identifier or inode
     */
    @Test
    void containerContent_forbidden_messageDoesNotIncludeContentDetails() throws Exception {
        when(shortyAPI.getShorty(INODE)).thenReturn(Optional.of(
                new ShortyId(INODE, INODE, ShortType.INODE, ShortType.CONTENTLET)));
        when(contentletAPI.find(eq(INODE), eq(user), anyBoolean()))
                .thenThrow(new DotSecurityException("Contentlet [name: " + CONTENT_TITLE
                        + ", identifier: " + IDENTIFIER + ", inode: " + INODE + "]"));

        final ForbiddenException forbidden = assertThrows(ForbiddenException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, INODE, null));

        // What the client receives: the response body and the error-message header
        final String message = forbidden.getResponse().getEntity() + " "
                + forbidden.getResponse().getHeaderString("error-message");
        assertFalse(message.contains(CONTENT_TITLE), "message must not include the title: " + message);
        assertFalse(message.contains(IDENTIFIER), "message must not include the identifier: " + message);
        assertFalse(message.contains(INODE), "message must not include the inode: " + message);
    }

    /**
     * Given an identifier that has no contentlet version at all
     * When it is rendered into a container
     * Then the request answers not found and nothing is rendered
     */
    @Test
    void containerContent_byIdentifier_withoutAnyVersion_isNotFound() throws Exception {
        givenIdentifierShorty();
        when(versionableAPI.findContentletVersionInfos(IDENTIFIER)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, IDENTIFIER, null));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a caller served live content, and a contentlet whose first version found has no live
     * version while another one is live
     * When it is rendered into a container by identifier
     * Then that live version is the one read on behalf of the caller, and a caller without READ on
     * it is forbidden
     */
    @Test
    void containerContent_byIdentifier_liveVersionInAnotherLanguage_isReadAsTheCaller() throws Exception {
        givenIdentifierShorty();
        when(versionableAPI.findContentletVersionInfos(IDENTIFIER))
                .thenReturn(List.of(versionInfo(null), versionInfo(LIVE_INODE)));
        when(contentletAPI.find(LIVE_INODE, user, true))
                .thenThrow(new DotSecurityException("no READ on " + CONTENT_TITLE));

        assertThrows(ForbiddenException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, IDENTIFIER, null));

        verify(contentletAPI).find(LIVE_INODE, user, true);
        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a caller served live content, and a contentlet with no live version in any language
     * When it is rendered into a container by identifier
     * Then the request answers not found and nothing is rendered
     */
    @Test
    void containerContent_byIdentifier_withoutLiveVersion_isNotFound() throws Exception {
        givenIdentifierShorty();
        when(versionableAPI.findContentletVersionInfos(IDENTIFIER)).thenReturn(List.of(versionInfo(null)));

        assertThrows(ResourceNotFoundException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, IDENTIFIER, null));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given an inode that does not belong to any contentlet
     * When it is rendered into a container
     * Then the request answers not found and nothing is rendered
     */
    @Test
    void containerContent_byUnknownInode_isNotFound() throws Exception {
        when(shortyAPI.getShorty(INODE)).thenReturn(Optional.of(
                new ShortyId(INODE, INODE, ShortType.INODE, ShortType.CONTENTLET)));

        try (MockedStatic<WebAPILocator> webApiLocator = mockStatic(WebAPILocator.class)) {
            webApiLocator.when(WebAPILocator::getHostWebAPI)
                    .thenThrow(new AssertionError("the contentlet must not be rendered"));

            assertThrows(ResourceNotFoundException.class,
                    () -> resource.containerContent(request, response, CONTAINER_ID, INODE, null));
        }

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    private void givenIdentifierShorty() {
        when(shortyAPI.getShorty(IDENTIFIER)).thenReturn(Optional.of(
                new ShortyId(IDENTIFIER, IDENTIFIER, ShortType.IDENTIFIER, ShortType.CONTENTLET)));
    }

    private static ContentletVersionInfo versionInfo(final String liveInode) {
        final ContentletVersionInfo info = new ContentletVersionInfo();
        info.setIdentifier(IDENTIFIER);
        info.setVariant(VariantAPI.DEFAULT_VARIANT.name());
        info.setWorkingInode(INODE);
        info.setLiveInode(liveInode);
        return info;
    }
}
