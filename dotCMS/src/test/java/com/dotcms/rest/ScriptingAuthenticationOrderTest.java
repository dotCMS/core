package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.rendering.js.JsResource;
import com.dotcms.rendering.util.ScriptingUtil;
import com.dotcms.rest.api.v1.vtl.VTLResource;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.exception.DoesNotExistException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/** Scripting endpoints must authenticate before revealing whether a submitted identifier exists. */
public class ScriptingAuthenticationOrderTest extends UnitTestBase {

    /** Anonymous writes return the same denial for existing and missing identifiers, without a lookup. */
    @Test
    public void deniedScriptingWritesDoNotValidateIdentifiers() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final UriInfo uri = mock(UriInfo.class);
        final ScriptingUtil scripting = mock(ScriptingUtil.class);
        final Map<String, Object> missing = Map.of("identifier", "missing-identifier");
        doThrow(new DoesNotExistException("Identifier not found"))
                .when(scripting).validateBodyMap(eq(missing), any());

        try (MockedStatic<AnonymousAccess> anonymous = mockStatic(AnonymousAccess.class);
                MockedStatic<ScriptingUtil> utilities = mockStatic(ScriptingUtil.class);
                MockedConstruction<WebResource> resources = mockConstruction(WebResource.class,
                        (resource, context) -> when(resource.init(any(WebResource.InitBuilder.class)))
                                .thenThrow(new SecurityException("Access denied", Response.Status.UNAUTHORIZED)))) {
            anonymous.when(AnonymousAccess::systemSetting).thenReturn(AnonymousAccess.NONE);
            utilities.when(ScriptingUtil::getInstance).thenReturn(scripting);
            final List<Function<Map<String, Object>, Response>> operations = List.of(
                    body -> new JsResource().put(request, response, uri, "folder", body),
                    body -> new JsResource().patch(request, response, uri, "folder", body),
                    body -> new JsResource().delete(request, response, uri, "folder", body),
                    body -> new VTLResource().put(request, response, uri, "folder", body),
                    body -> new VTLResource().patch(request, response, uri, "folder", body),
                    body -> new VTLResource().delete(request, response, uri, "folder", body));
            for (final Function<Map<String, Object>, Response> operation : operations) {
                assertEquals(401, operation.apply(Map.of("identifier", "existing-identifier")).getStatus());
                assertEquals(401, operation.apply(missing).getStatus());
            }
            verifyNoInteractions(scripting);
        }
    }
}
