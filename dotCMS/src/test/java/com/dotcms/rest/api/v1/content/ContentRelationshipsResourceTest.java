package com.dotcms.rest.api.v1.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link ContentRelationshipsResource}.
 */
class ContentRelationshipsResourceTest {

    private static final String PARAMS = "id/content-id";
    private static final String INTERNAL_TEXT = "internal detail of the failure";

    private ContentRelationshipsHelper helper;
    private WebResource webResource;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private User user;
    private List<Contentlet> contents;

    @BeforeEach
    void setUp() {
        helper = mock(ContentRelationshipsHelper.class);
        webResource = mock(WebResource.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        user = mock(User.class);
        contents = List.of(mock(Contentlet.class));

        final InitDataObject initData = mock(InitDataObject.class);
        final Map<String, String> paramsMap = Map.of("id", "content-id");
        when(webResource.init(PARAMS, request, response, false, null)).thenReturn(initData);
        when(initData.getUser()).thenReturn(user);
        when(initData.getParamsMap()).thenReturn(paramsMap);
        when(helper.getRelatedContent(request, user, paramsMap)).thenReturn(contents);
    }

    @Test
    void getContent_serializesRelatedContentForTheRequestingUser() throws Exception {
        // Given a request whose initialization resolves a non-admin user
        final String serialized = "[{\"identifier\":\"content-id\"}]";
        when(helper.contentsAsJson(contents, user)).thenReturn(serialized);

        // When the content relationships are requested
        final Response result = new ContentRelationshipsResource(helper, webResource)
                .getContent(request, response, PARAMS);

        // Then the related content is serialized for that same user and the response is 200
        verify(helper).contentsAsJson(contents, user);
        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertEquals(serialized, result.getEntity());
    }

    /**
     * The failures the content can hit while it is converted, each carrying internal text.
     */
    static Stream<Exception> conversionFailures() {
        return Stream.of(new DotDataException(INTERNAL_TEXT), new JSONException(INTERNAL_TEXT));
    }

    @ParameterizedTest
    @MethodSource("conversionFailures")
    void getContent_answersAFixedMessageWhenContentCannotBeLoaded(final Exception failure)
            throws Exception {
        // Given the related content cannot be converted, with a failure carrying internal text
        when(helper.contentsAsJson(contents, user)).thenThrow(failure);

        // When the content relationships are requested
        final Response result = new ContentRelationshipsResource(helper, webResource)
                .getContent(request, response, PARAMS);

        // Then the answer is a 400 whose body and headers do not carry that text
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatus());
        assertFalse(String.valueOf(result.getEntity()).contains(INTERNAL_TEXT),
                "The response body must not carry the failure's own text");
        assertFalse(result.getHeaders().toString().contains(INTERNAL_TEXT),
                "The response headers must not carry the failure's own text");
    }
}
