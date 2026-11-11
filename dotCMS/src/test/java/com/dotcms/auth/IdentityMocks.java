package com.dotcms.auth;

import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test helpers: a request and a session whose attributes live in plain maps, so tests can assert what was
 * really stored.
 */
final class IdentityMocks {

    private IdentityMocks() {
    }

    static HttpSession session(final Map<String, Object> attributes) {
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(anyString())).thenAnswer(i -> attributes.get(i.<String>getArgument(0)));
        when(session.getValue(anyString())).thenAnswer(i -> attributes.get(i.<String>getArgument(0)));
        when(session.getAttributeNames())
                .thenAnswer(i -> Collections.enumeration(new HashMap<>(attributes).keySet()));
        doAnswer(i -> attributes.put(i.getArgument(0), i.getArgument(1))).when(session)
                .setAttribute(anyString(), any());
        doAnswer(i -> attributes.put(i.getArgument(0), i.getArgument(1))).when(session)
                .putValue(anyString(), any());
        doAnswer(i -> attributes.remove(i.<String>getArgument(0))).when(session).removeAttribute(anyString());
        return session;
    }

    static HttpServletRequest request(final Map<String, Object> attributes, final HttpSession session) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(anyString())).thenAnswer(i -> attributes.get(i.<String>getArgument(0)));
        doAnswer(i -> attributes.put(i.getArgument(0), i.getArgument(1))).when(request)
                .setAttribute(anyString(), any());
        when(request.getSession()).thenReturn(session);
        when(request.getSession(Mockito.anyBoolean())).thenReturn(session);
        return request;
    }
}
