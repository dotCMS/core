package com.dotcms.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.util.Logger;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/** Credential-bearing paths must not be copied into authentication or authorization logs. */
public class WebResourceInitLoggingTest extends UnitTestBase {

    /** Denials are rethrown without logging reusable credentials from a legacy path. */
    @Test
    public void deniedInitializationDoesNotLogCredentialPath() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/content/user/actor/password/reusable-api-jwt");
        final WebResource resource = mock(WebResource.class);
        final SecurityException denied = new SecurityException("Access denied", Response.Status.UNAUTHORIZED);
        when(resource.init(any(WebResource.InitBuilder.class))).thenThrow(denied);

        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            assertSame(denied, assertThrows(SecurityException.class,
                    () -> new WebResource.InitBuilder(resource)
                            .requestAndResponse(request, mock(HttpServletResponse.class))
                            .requiredAnonAccess(AnonymousAccess.READ).init()));

            final ArgumentCaptor<String> warning = ArgumentCaptor.forClass(String.class);
            logger.verify(() -> Logger.warnAndDebug(eq(WebResource.class), warning.capture(), same(denied)));
            assertFalse(warning.getValue().contains("reusable-api-jwt"));

            @SuppressWarnings("unchecked")
            final ArgumentCaptor<Supplier<String>> debug =
                    ArgumentCaptor.forClass((Class<Supplier<String>>) (Class<?>) Supplier.class);
            logger.verify(() -> Logger.debug(eq(WebResource.InitBuilder.class), debug.capture()));
            assertFalse(debug.getValue().get().contains("reusable-api-jwt"));
        }
    }
}
