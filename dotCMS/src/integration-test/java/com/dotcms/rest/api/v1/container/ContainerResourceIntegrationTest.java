package com.dotcms.rest.api.v1.container;

import com.dotcms.rendering.velocity.services.ContainerLoader;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.PaginationUtil;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.util.PageMode;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Optional;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * test  of {@link ContainerResource}
 */
public class ContainerResourceIntegrationTest {

    private ContainerResource containerResource;

    private final PaginationUtil paginationUtil = mock(PaginationUtil.class);
    private final WebResource webResource = mock(WebResource.class);
    private final FormAPI formAPI = mock(FormAPI.class);
    private final ContainerAPI containerAPI = mock(ContainerAPI.class);
    private final VersionableAPI versionableAPI = mock(VersionableAPI.class);
    private final VelocityUtil velocityUtil = mock(VelocityUtil.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final ShortyIdAPI shortyAPI = mock(ShortyIdAPI.class);
    private final User user = mock(User.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final ChainedContext context = mock(ChainedContext.class);
    private final HttpSession session = mock(HttpSession.class);
    private final Contentlet formContent = mock(Contentlet.class);

    private final String containerId = "1";
    private final String formId = "2";
    private final Container container = mock(Container.class);
    private final ShortyId shortyId = new ShortyId("2", "1234567", ShortType.IDENTIFIER, ShortType.IDENTIFIER);

    @Before
    public void init() throws Exception {
        final InitDataObject initData = mock(InitDataObject.class);

        when(request.getSession()).thenReturn(session);
        when(initData.getUser()).thenReturn(user);
        when(webResource.init(true, request, true)).thenReturn(initData);
        when(velocityUtil.getContext(request, response)).thenReturn(context);
        when(formAPI.getFormContent(formId)).thenReturn(formContent);
        when(shortyAPI.getShorty(containerId)).thenReturn(Optional.of(shortyId));
        when(container.getIdentifier()).thenReturn(containerId);
        when(formContent.getIdentifier()).thenReturn(formId);

        containerResource = new ContainerResource(webResource, paginationUtil, formAPI, containerAPI, versionableAPI,
                velocityUtil, shortyAPI);

        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void test_renderFormIntoContainer() throws DotSecurityException, DotDataException {
        Map contentMap = mock(Map.class);

        when(versionableAPI.findWorkingVersion(shortyId.longId, user, PageMode.EDIT_MODE.respectAnonPerms)).thenReturn(container);
        when(velocityUtil.merge("/EDIT_MODE/1/LEGACY_RELATION_TYPE.container", context)).thenReturn("html");
        when(formContent.getMap()).thenReturn(contentMap);

        Response response = containerResource.containerForm(request, this.response, containerId, formId);

        RestUtilTest.verifySuccessResponse(response);
        Map<String, Object> map = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity();
        assertEquals("html" , map.get("render"));
        assertEquals(contentMap , map.get("content"));

        verify(context).put(ContainerLoader.SHOW_PRE_POST_LOOP, false);
        verify(context).put("contentletList1LEGACY_RELATION_TYPE", Lists.newArrayList(formId));
        verify(context).put(PageMode.EDIT_MODE.name(), Boolean.TRUE);
    }

    @Test
    public void test_renderFormIntoContainer_WhenContainerIDThrowDataSecurityException() throws DotSecurityException, DotDataException {
        DotSecurityException dotSecurityException = new DotSecurityException("");
        when(versionableAPI.findWorkingVersion(shortyId.longId, user, PageMode.EDIT_MODE.respectAnonPerms)).thenThrow(dotSecurityException);

        try {
            containerResource.containerForm(request, this.response, containerId, formId);
            assertTrue(false);
        } catch(DotSecurityException e) {
            assertEquals(dotSecurityException, e);
        }
    }

    @Test
    public void test_renderFormIntoContainer_WhenContentMergeThrowDotParserException() throws DotSecurityException, DotDataException {
        ParseErrorException exception = new ParseErrorException("");

        when(versionableAPI.findWorkingVersion(shortyId.longId, user, PageMode.EDIT_MODE.respectAnonPerms)).thenReturn(container);
        when(velocityUtil.merge("/EDIT_MODE/1/LEGACY_RELATION_TYPE.container", context)).thenThrow(exception);

        try {
            containerResource.containerForm(request, this.response, containerId, formId);
            assertTrue(false);
        } catch(ParseErrorException e) {
            assertEquals(exception, e);
        }
    }

}
