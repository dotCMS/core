package com.dotcms.graphql;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.util.Config;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class DotGraphQLContextBuilderTest {

    /**
     * When authentication is refused, the builder sends the exception's status (401 here) so the
     * GraphQL servlet cannot turn it into a 400, with a generic body that does not name the
     * setting, and still rethrows the exception.
     */
    @Test
    void build_accessRefused_sendsStatusAndRethrows() throws Exception {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final SecurityException refused = new SecurityException("Invalid User", Response.Status.UNAUTHORIZED);

        try (MockedStatic<Config> config = mockStatic(Config.class);
             MockedConstruction<WebResource.InitBuilder> builders = refusingInitBuilder(refused)) {
            assertSame(refused, assertThrows(SecurityException.class,
                    () -> new DotGraphQLContextBuilder().build(mock(HttpServletRequest.class), response)));
        }
        // the body stays generic; the detailed reason only goes to the debug log
        verify(response).sendError(401, "Unauthorized");
    }

    /**
     * When the response is already committed, the builder does not try to send an error, which
     * would throw IllegalStateException, and rethrows the original exception.
     */
    @Test
    void build_accessRefusedOnCommittedResponse_onlyRethrows() throws Exception {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.isCommitted()).thenReturn(true);
        final SecurityException refused = new SecurityException("Invalid User", Response.Status.UNAUTHORIZED);

        try (MockedStatic<Config> config = mockStatic(Config.class);
             MockedConstruction<WebResource.InitBuilder> builders = refusingInitBuilder(refused)) {
            assertSame(refused, assertThrows(SecurityException.class,
                    () -> new DotGraphQLContextBuilder().build(mock(HttpServletRequest.class), response)));
        }
        verify(response, never()).sendError(anyInt(), anyString());
    }

    /**
     * Replaces every {@link WebResource.InitBuilder} created in scope with a mock whose builder
     * methods return itself and whose {@code init()} throws the given exception.
     */
    private static MockedConstruction<WebResource.InitBuilder> refusingInitBuilder(final SecurityException refused) {
        return mockConstruction(WebResource.InitBuilder.class, withSettings().defaultAnswer(RETURNS_SELF),
                (builder, context) -> when(builder.init()).thenThrow(refused));
    }
}
