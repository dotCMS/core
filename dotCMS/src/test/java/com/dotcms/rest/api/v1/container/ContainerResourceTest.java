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
import com.dotcms.rest.api.v1.page.PageResource;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.util.PaginationUtil;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.business.web.VariantWebAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
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
    private static final String OTHER_INODE = "c0ffee00-0000-4000-8000-000000000004";
    private static final String CONTENT_TITLE = "Quarterly Results Draft";
    private static final long REQUEST_LANGUAGE_ID = 1L;

    private final WebResource webResource = mock(WebResource.class);
    private final ShortyIdAPI shortyAPI = mock(ShortyIdAPI.class);
    private final ContentletAPI contentletAPI = mock(ContentletAPI.class);
    private final VelocityUtil velocityUtil = mock(VelocityUtil.class);
    private final VariantWebAPI variantWebAPI = mock(VariantWebAPI.class);
    private final LanguageWebAPI languageWebAPI = mock(LanguageWebAPI.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final User user = mock(User.class);
    private final User systemUser = mock(User.class);
    private final Contentlet contentlet = mock(Contentlet.class);

    private MockedStatic<WebAPILocator> webApiLocator;
    private MockedStatic<APILocator> apiLocator;
    private ContainerResource resource;

    @BeforeEach
    void setUp() {
        final InitDataObject initData = mock(InitDataObject.class);
        when(initData.getUser()).thenReturn(user);
        when(webResource.init(request, response, true)).thenReturn(initData);
        when(contentlet.getIdentifier()).thenReturn(IDENTIFIER);
        when(contentlet.getInode()).thenReturn(INODE);
        givenRequestLanguage(REQUEST_LANGUAGE_ID);

        // Rendering resolves the current site through WebAPILocator; fail fast there instead of
        // reaching for a database the unit environment does not have.
        webApiLocator = mockStatic(WebAPILocator.class);
        webApiLocator.when(WebAPILocator::getHostWebAPI)
                .thenThrow(new AssertionError("the contentlet must not be rendered"));
        webApiLocator.when(WebAPILocator::getLanguageWebAPI).thenReturn(languageWebAPI);
        webApiLocator.when(WebAPILocator::getVariantWebAPI).thenReturn(variantWebAPI);
        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::systemUser).thenReturn(systemUser);

        resource = new ContainerResource(webResource, mock(PaginationUtil.class), mock(FormAPI.class),
                mock(ContainerAPI.class), mock(VersionableAPI.class), velocityUtil, shortyAPI,
                contentletAPI, mock(ContainerDependenciesHelper.class));
    }

    @AfterEach
    void tearDown() {
        webApiLocator.close();
        apiLocator.close();
    }

    /**
     * Given a contentlet addressed by its identifier, and a caller without READ permission on it
     * When the contentlet is rendered into a container
     * Then the request is forbidden and nothing is rendered
     */
    @Test
    void containerContent_byIdentifier_withoutReadPermission_isForbidden() throws Exception {
        givenIdentifierShorty();
        givenServedVersion(versionInfo(LIVE_INODE));
        // Any lookup made on behalf of the caller is denied
        when(contentletAPI.find(anyString(), eq(user), anyBoolean()))
                .thenThrow(new DotSecurityException("no READ on " + CONTENT_TITLE));

        assertThrows(ForbiddenException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, IDENTIFIER, null));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a contentlet addressed by its inode, and a caller without READ permission on it
     * When the contentlet is rendered into a container
     * Then the request is forbidden and nothing is rendered
     */
    @Test
    void containerContent_byInode_withoutReadPermission_isForbidden() throws Exception {
        givenInodeShorty();
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
        givenInodeShorty();
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
     * Given an identifier with no version for the request's language and variant, nor any fallback
     * When it is rendered into a container
     * Then the request answers not found and nothing is rendered
     */
    @Test
    void containerContent_byIdentifier_withoutAnyVersion_isNotFound() throws Exception {
        givenIdentifierShorty();
        when(variantWebAPI.getContentletVersionInfoByFallback(anyLong(), eq(IDENTIFIER), any(), eq(systemUser)))
                .thenThrow(new ResourceNotFoundException("no version"));

        assertThrows(ResourceNotFoundException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, IDENTIFIER, null));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a caller served live content in a language other than the default one
     * When a contentlet is rendered into a container by identifier
     * Then the version resolved for that language and mode, as rendering resolves it, is the one
     * read on behalf of the caller, and a caller without READ on it is forbidden rather than told
     * the content does not exist
     */
    @Test
    void containerContent_byIdentifier_readsTheVersionResolvedForTheRequest() throws Exception {
        givenIdentifierShorty();
        givenRequestLanguage(2L);
        when(variantWebAPI.getContentletVersionInfoByFallback(2L, IDENTIFIER, PageMode.LIVE, systemUser))
                .thenReturn(versionInfo(LIVE_INODE));
        when(contentletAPI.find(LIVE_INODE, user, true))
                .thenThrow(new DotSecurityException("no READ on " + CONTENT_TITLE));

        assertThrows(ForbiddenException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, IDENTIFIER, null));

        verify(contentletAPI).find(LIVE_INODE, user, true);
        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a caller served live content, and a contentlet with no live version
     * When it is rendered into a container by identifier
     * Then the request answers not found and nothing is rendered
     */
    @Test
    void containerContent_byIdentifier_withoutLiveVersion_isNotFound() throws Exception {
        givenIdentifierShorty();
        givenServedVersion(versionInfo(null));

        assertThrows(ResourceNotFoundException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, IDENTIFIER, null));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a contentlet whose resolved version is archived
     * When it is rendered into a container by identifier
     * Then the request answers not found and the archived version is never read
     */
    @Test
    void containerContent_byIdentifier_archived_isNotFound() throws Exception {
        givenIdentifierShorty();
        final ContentletVersionInfo archived = versionInfo(LIVE_INODE);
        archived.setDeleted(true);
        givenServedVersion(archived);

        assertThrows(ResourceNotFoundException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, IDENTIFIER, null));

        verify(contentletAPI, never()).find(anyString(), any(), anyBoolean());
    }

    /**
     * Given an inode that does not belong to any contentlet
     * When it is rendered into a container
     * Then the request answers not found and nothing is rendered
     */
    @Test
    void containerContent_byUnknownInode_isNotFound() throws Exception {
        givenInodeShorty();

        assertThrows(ResourceNotFoundException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, INODE, null));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a back-end caller, and a contentlet whose working and live versions differ
     * When it is rendered into a container by identifier
     * Then the working version is the one read as the caller
     */
    @Test
    void containerContent_byIdentifier_backendCaller_readsWorkingVersion() throws Exception {
        givenIdentifierShorty();
        givenBackendCaller();
        when(variantWebAPI.getContentletVersionInfoByFallback(REQUEST_LANGUAGE_ID, IDENTIFIER,
                PageMode.EDIT_MODE, systemUser)).thenReturn(versionInfo(LIVE_INODE));
        when(contentletAPI.find(INODE, user, true))
                .thenThrow(new DotSecurityException("no READ on " + CONTENT_TITLE));

        assertThrows(ForbiddenException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, IDENTIFIER, null));

        verify(contentletAPI, never()).find(eq(LIVE_INODE), any(), anyBoolean());
    }

    /**
     * Given a back-end caller whose request renders live content (an XHR request), and a contentlet
     * whose working and live versions differ
     * When it is rendered into a container by identifier
     * Then the live version, the one rendered, is the one read as the caller
     */
    @Test
    void containerContent_byIdentifier_backendCallerServedLive_readsLiveVersion() throws Exception {
        givenIdentifierShorty();
        givenBackendCaller();
        when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");
        when(variantWebAPI.getContentletVersionInfoByFallback(REQUEST_LANGUAGE_ID, IDENTIFIER,
                PageMode.LIVE, systemUser)).thenReturn(versionInfo(LIVE_INODE));
        when(contentletAPI.find(LIVE_INODE, user, true))
                .thenThrow(new DotSecurityException("no READ on " + CONTENT_TITLE));

        assertThrows(ForbiddenException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, IDENTIFIER, null));

        verify(contentletAPI, never()).find(eq(INODE), any(), anyBoolean());
    }

    /**
     * Given a caller served live content while a Time Machine date is set, and a contentlet whose
     * working version the caller cannot read
     * When it is rendered into a container by identifier
     * Then the request is forbidden and nothing is rendered, since the Time Machine can render the
     * working version
     */
    @Test
    void containerContent_byIdentifier_timeMachine_requiresReadOnWorkingVersion() throws Exception {
        givenIdentifierShorty();
        when(request.getAttribute(PageResource.TM_DATE)).thenReturn("4102444800000");
        givenServedVersion(versionInfo(LIVE_INODE));
        when(contentletAPI.find(LIVE_INODE, user, true)).thenReturn(contentlet);
        when(contentletAPI.find(INODE, user, true))
                .thenThrow(new DotSecurityException("no READ on " + CONTENT_TITLE));

        assertThrows(ForbiddenException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, IDENTIFIER, null));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a caller served live content while a Time Machine date is set, and a working version
     * that rendering resolves in preview mode from another variant than the live one served
     * When the contentlet is rendered into a container by identifier
     * Then that working version is the one read as the caller, and a caller without READ on it is
     * forbidden
     */
    @Test
    void containerContent_byIdentifier_timeMachine_readsTheWorkingVersionRenderingResolves() throws Exception {
        givenIdentifierShorty();
        when(request.getAttribute(PageResource.TM_DATE)).thenReturn("4102444800000");
        when(variantWebAPI.getContentletVersionInfoByFallback(REQUEST_LANGUAGE_ID, IDENTIFIER,
                PageMode.LIVE, systemUser)).thenReturn(versionInfo(LIVE_INODE));
        final ContentletVersionInfo otherVariant = versionInfo(null);
        otherVariant.setVariant("experiment-variant");
        otherVariant.setWorkingInode(OTHER_INODE);
        when(variantWebAPI.getContentletVersionInfoByFallback(REQUEST_LANGUAGE_ID, IDENTIFIER,
                PageMode.PREVIEW_MODE, systemUser)).thenReturn(otherVariant);
        when(contentletAPI.find(LIVE_INODE, user, true)).thenReturn(contentlet);
        when(contentletAPI.find(OTHER_INODE, user, true))
                .thenThrow(new DotSecurityException("no READ on " + CONTENT_TITLE));

        assertThrows(ForbiddenException.class,
                () -> resource.containerContent(request, response, CONTAINER_ID, IDENTIFIER, null));

        verify(contentletAPI).find(OTHER_INODE, user, true);
        verify(velocityUtil, never()).merge(anyString(), any());
    }

    private void givenIdentifierShorty() {
        when(shortyAPI.getShorty(IDENTIFIER)).thenReturn(Optional.of(
                new ShortyId(IDENTIFIER, IDENTIFIER, ShortType.IDENTIFIER, ShortType.CONTENTLET)));
    }

    private void givenInodeShorty() {
        when(shortyAPI.getShorty(INODE)).thenReturn(Optional.of(
                new ShortyId(INODE, INODE, ShortType.INODE, ShortType.CONTENTLET)));
    }

    private void givenBackendCaller() {
        when(user.isBackendUser()).thenReturn(true);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        // What PageMode.setPageMode stores on a real request for a back-end user
        when(request.getAttribute(com.dotmarketing.util.WebKeys.PAGE_MODE_PARAMETER))
                .thenReturn(PageMode.EDIT_MODE);
    }

    private void givenRequestLanguage(final long languageId) {
        final Language language = mock(Language.class);
        when(language.getId()).thenReturn(languageId);
        when(languageWebAPI.getLanguage(request)).thenReturn(language);
    }

    private void givenServedVersion(final ContentletVersionInfo info) {
        when(variantWebAPI.getContentletVersionInfoByFallback(anyLong(), eq(IDENTIFIER), any(), eq(systemUser)))
                .thenReturn(info);
    }

    private static ContentletVersionInfo versionInfo(final String liveInode) {
        final ContentletVersionInfo info = new ContentletVersionInfo();
        info.setIdentifier(IDENTIFIER);
        info.setLang(REQUEST_LANGUAGE_ID);
        info.setVariant(VariantAPI.DEFAULT_VARIANT.name());
        info.setWorkingInode(INODE);
        info.setLiveInode(liveInode);
        return info;
    }
}
