package com.dotcms.auth;

import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wraps a request and its session so the user keys never hold an account that cannot be used
 * interactively (see {@link InteractiveUserPolicy}).
 * <p>
 * On the wrapped objects, writing such an account to the {@code USER} or {@code USER_ID} key is dropped,
 * reading a value already stored there answers {@code null}, and the key is left out of the attribute names.
 * Everything else passes through unchanged.
 */
public final class InteractiveIdentityWrapper {

    private InteractiveIdentityWrapper() {
    }

    /**
     * @param request the request to wrap, null is returned as is
     * @return a request whose session is wrapped too
     */
    public static HttpServletRequest wrap(final HttpServletRequest request) {
        if (request == null || request instanceof IdentityRequest) {
            return request;
        }
        return new IdentityRequest(request);
    }

    /**
     * @param session the session to wrap, null is returned as is
     * @return the wrapped session
     */
    public static HttpSession wrap(final HttpSession session) {
        if (session == null || Proxy.isProxyClass(session.getClass())
                && Proxy.getInvocationHandler(session) instanceof IdentitySessionHandler) {
            return session;
        }
        return (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class}, new IdentitySessionHandler(session));
    }

    /** Whether the name is one of the two keys that carry the identity of a request or session. */
    private static boolean isUserKey(final Object name) {
        return WebKeys.USER.equals(name) || WebKeys.USER_ID.equals(name);
    }

    /** Whether the value is an account (or account id) that cannot be used as an interactive identity. */
    private static boolean isRefused(final Object value) {
        if (value instanceof User) {
            return !InteractiveUserPolicy.isEligible((User) value);
        }
        if (value instanceof String) {
            return !InteractiveUserPolicy.isEligibleUserId((String) value);
        }
        return false;
    }

    /**
     * The rule the whole wrapper applies: a value is dropped on write, hidden on read and left out of the
     * attribute names when it is stored under an identity key and names an account that cannot be used
     * interactively.
     */
    private static boolean hidden(final String name, final Object value) {
        return isUserKey(name) && isRefused(value);
    }

    private static final class IdentityRequest extends HttpServletRequestWrapper {

        /** The container session last seen and the wrapper made for it, so one session has one wrapper. */
        private HttpSession target;
        private HttpSession wrapped;

        IdentityRequest(final HttpServletRequest request) {
            super(request);
        }

        /**
         * Returns the same wrapper for the same container session, and a new one when the container hands
         * out a different session (after an invalidation), so the session keeps its identity for
         * equality, locking and use as a map key.
         */
        private HttpSession wrapOnce(final HttpSession real) {
            if (real == null) {
                return null;
            }
            if (real != target) {
                target = real;
                wrapped = wrap(real);
            }
            return wrapped;
        }

        @Override
        public void setAttribute(final String name, final Object value) {
            if (hidden(name, value)) {
                InteractiveUserRefusalLog.record(this, null);
                return;
            }
            super.setAttribute(name, value);
        }

        @Override
        public Object getAttribute(final String name) {
            final Object value = super.getAttribute(name);
            if (hidden(name, value)) {
                InteractiveUserRefusalLog.record(this, null);
                return null;
            }
            return value;
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            final List<String> names = Collections.list(super.getAttributeNames()).stream()
                    .filter(n -> !hidden(n, super.getAttribute(n))).collect(Collectors.toList());
            return Collections.enumeration(names);
        }

        @Override
        public HttpSession getSession() {
            return wrapOnce(super.getSession());
        }

        @Override
        public HttpSession getSession(final boolean create) {
            return wrapOnce(super.getSession(create));
        }
    }

    private static final class IdentitySessionHandler implements InvocationHandler {

        private final HttpSession session;

        IdentitySessionHandler(final HttpSession session) {
            this.session = session;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final String name = method.getName();
            if ("equals".equals(name) && args != null && args.length == 1) {
                return proxy == args[0];
            }
            if ("hashCode".equals(name) && args == null) {
                return System.identityHashCode(proxy);
            }
            try {
                if (("setAttribute".equals(name) || "putValue".equals(name)) && args.length == 2
                        && hidden((String) args[0], args[1])) {
                    InteractiveUserRefusalLog.record(null, session);
                    return null;
                }
                if (("getAttribute".equals(name) || "getValue".equals(name)) && args.length == 1) {
                    final Object value = method.invoke(session, args);
                    if (hidden((String) args[0], value)) {
                        InteractiveUserRefusalLog.record(null, session);
                        return null;
                    }
                    return value;
                }
                if ("getAttributeNames".equals(name) && args == null) {
                    return Collections.enumeration(Collections.list(session.getAttributeNames()).stream()
                            .filter(n -> !hidden(n, session.getAttribute(n))).collect(Collectors.toList()));
                }
                if ("getValueNames".equals(name) && args == null) {
                    return Arrays.stream(session.getValueNames())
                            .filter(n -> !hidden(n, session.getValue(n))).toArray(String[]::new);
                }
                return method.invoke(session, args);
            } catch (final InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
