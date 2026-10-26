package com.dotcms.rest.api.v1.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ContentRelationshipsResource}.
 */
class ContentRelationshipsResourceTest {

    @Test
    void getContent_serializesRelatedContentForTheRequestingUser() throws Exception {
        // Given a request whose initialization resolves a non-admin user
        final ContentRelationshipsHelper helper = mock(ContentRelationshipsHelper.class);
        final WebResource webResource = mock(WebResource.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final InitDataObject initData = mock(InitDataObject.class);
        final User user = mock(User.class);
        final Map<String, String> paramsMap = Map.of("id", "content-id");
        final List<Contentlet> contents = List.of(mock(Contentlet.class));
        final String serialized = "[{\"identifier\":\"content-id\"}]";
        when(webResource.init("id/content-id", request, response, false, null))
                .thenReturn(initData);
        when(initData.getUser()).thenReturn(user);
        when(initData.getParamsMap()).thenReturn(paramsMap);
        when(helper.getRelatedContent(request, user, paramsMap)).thenReturn(contents);
        when(helper.contentsAsJson(contents, user)).thenReturn(serialized);

        // When the content relationships are requested
        final Response result = new ContentRelationshipsResource(helper, webResource)
                .getContent(request, response, "id/content-id");

        // Then the related content is serialized for that same user and the response is 200
        verify(helper).contentsAsJson(contents, user);
        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertEquals(serialized, result.getEntity());
    }
}
