package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.util.PortletID;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.MockedConstruction;

/** Body-bound credentials must not be serialized into a path and mistaken for URL credentials. */
public class FormCredentialRoutingTest extends UnitTestBase {

    /** Covers every legacy form caller without executing configuration updates or destructive actions. */
    @Test
    public void formEndpointsUseBodyAuthenticationAndKeepTheirPortletRequirement() throws Exception {
        final Map<Class<?>, Set<String>> endpoints = Map.of(
                CMSConfigResource.class, Set.of("saveCompanyBasicInfo", "saveCompanyLocaleInfo",
                        "saveCompanyAuthTypeInfo", "saveCompanyLogo", "deleteEnvironment", "deleteEndpoint"),
                TestResource.class, Set.of("saveTest"));
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final SecurityException denied = new SecurityException("Stop before updating configuration",
                Response.Status.UNAUTHORIZED);
        int checked = 0;
        for (final Map.Entry<Class<?>, Set<String>> entry : endpoints.entrySet()) {
            for (final Method method : entry.getKey().getDeclaredMethods()) {
                if (!entry.getValue().contains(method.getName())) {
                    continue;
                }
                try (MockedConstruction<WebResource> resources = mockConstruction(WebResource.class,
                        (resource, context) -> when(resource.initWithFormCredentials(eq("body-user"),
                                eq("body-secret/with:separators"), eq(request), eq(response),
                                nullable(String.class))).thenThrow(denied))) {
                    final Object endpoint = entry.getKey().getConstructor().newInstance();
                    final Object[] arguments = new Object[method.getParameterCount()];
                    arguments[0] = request;
                    arguments[1] = response;
                    arguments[2] = "body-user";
                    arguments[3] = "body-secret/with:separators";

                    final InvocationTargetException failure = assertThrows(method.getName(),
                            InvocationTargetException.class, () -> method.invoke(endpoint, arguments));

                    assertSame(method.getName(), denied, failure.getCause());
                    assertEquals(1, resources.constructed().size());
                    verify(resources.constructed().get(0)).initWithFormCredentials("body-user",
                            "body-secret/with:separators", request, response,
                            entry.getKey() == CMSConfigResource.class ? PortletID.CONFIGURATION.toString() : null);
                    checked++;
                }
            }
        }
        assertEquals("All six configuration forms and the example form must be covered", 7, checked);
    }
}
