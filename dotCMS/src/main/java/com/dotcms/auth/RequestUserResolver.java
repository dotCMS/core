package com.dotcms.auth;

import com.dotcms.cms.login.SessionUserResolver;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Single place that resolves the user of a request or session.
 * <p>
 * While a {@link SystemUserScope} is open on the thread the answer is the built-in system user and the
 * request is not read. Otherwise the user comes from the request attribute, the session, the stored user id
 * or the {@code Authorization} header, and an account that is not eligible for interactive use (see
 * {@link InteractiveUserPolicy}) is skipped. Refusing never writes anything to the request or the session.
 */
public final class RequestUserResolver {

    private RequestUserResolver() {
    }

    /**
     * Resolves the user of a request, in the order {@code PortalUtil} always used: the user already
     * resolved for the request, the user of its session, the user id set on the request, the
     * {@code Authorization} header. Users taken from the session or from an id must currently exist and be
     * active (see {@link SessionUserResolver}). The result is stored on the request.
     *
     * @param request the request
     * @return the user, or null when there is none or the only one found is not eligible
     */
    public static User resolveRequestUser(final HttpServletRequest request) {
        if (scopeApplies()) {
            return systemUser();
        }
        if (request == null) {
            return null;
        }
        final Object resolved = request.getAttribute(WebKeys.USER);
        if (resolved instanceof User) {
            final User allowed = eligible((User) resolved, request, null);
            if (allowed != null) {
                request.setAttribute(WebKeys.USER_ID, allowed.getUserId());
                return allowed;
            }
        }
        final HttpSession session = request.getSession(false);
        // read before the session user is resolved: a refused session loses its user id
        final String sessionUserId = resolveSessionUserId(session);
        User user = resolveSessionUser(session);
        if (user == null) {
            // a user id set on the request itself, e.g. by a login filter; the session's own id was
            // already checked above and is not tried again, even as a copy on the request
            final Object userId = request.getAttribute(WebKeys.USER_ID);
            if (userId instanceof String && ((String) userId).equalsIgnoreCase(sessionUserId)) {
                request.removeAttribute(WebKeys.USER_ID);
            } else if (userId instanceof String && eligibleId((String) userId, request, session) != null) {
                user = eligible(SessionUserResolver.getInstance().resolveActive((String) userId).orElse(null),
                        request, session);
            }
        }
        if (user == null) {
            user = eligible(PortalUtil.getUserFromAuthHeader(request), request, null);
        }
        if (user != null) {
            request.setAttribute(WebKeys.USER, user);
            request.setAttribute(WebKeys.USER_ID, user.getUserId());
        }
        return user;
    }

    /**
     * Resolves the user held by a session. An account that cannot be used interactively is refused before
     * the session resolver sees it, because resolving it would write it back to the session.
     *
     * @param session the session, may be null
     * @return the user, or null when there is none or the one found is not eligible
     */
    public static User resolveSessionUser(final HttpSession session) {
        if (scopeApplies()) {
            return systemUser();
        }
        if (session == null || principalRefused(session)) {
            return null;
        }
        final Object cached = session.getAttribute(WebKeys.USER);
        final Object storedId = session.getAttribute(WebKeys.USER_ID);
        if (cached instanceof User) {
            if (eligible((User) cached, null, session) == null) {
                return null;
            }
        } else if (storedId instanceof String && eligibleId((String) storedId, null, session) == null) {
            return null;
        }
        return eligible(SessionUserResolver.getInstance().resolve(session).orElse(null), null, session);
    }

    /**
     * Resolves the user id of a request.
     *
     * @param request the request
     * @return the id, or null when there is none or the one found is not eligible
     */
    public static String resolveRequestUserId(final HttpServletRequest request) {
        if (scopeApplies()) {
            return UserAPI.SYSTEM_USER_ID;
        }
        if (request == null) {
            return null;
        }
        String userId = eligibleId((String) request.getAttribute(WebKeys.USER_ID), request, null);
        if (userId == null) {
            userId = resolveSessionUserId(request.getSession(false));
        }
        if (userId != null) {
            request.setAttribute(WebKeys.USER_ID, userId);
        }
        return userId;
    }

    /**
     * Resolves the user id held by a session.
     *
     * @param session the session, may be null
     * @return the id, or null when there is none or the one found is not eligible
     */
    public static String resolveSessionUserId(final HttpSession session) {
        if (scopeApplies()) {
            return UserAPI.SYSTEM_USER_ID;
        }
        if (session == null || principalRefused(session)) {
            return null;
        }
        return eligibleId((String) session.getAttribute(WebKeys.USER_ID), null, session);
    }

    /**
     * Whether a Login As session was started by an account that cannot be used interactively. Such a session
     * is refused even though the user it acts as is a regular one, so a session opened by the built-in account
     * before this rule existed does not outlive it.
     *
     * @param session the session
     * @return true when the session's principal is not eligible
     */
    private static boolean principalRefused(final HttpSession session) {
        final Object principalId = session.getAttribute(WebKeys.PRINCIPAL_USER_ID);
        return principalId instanceof String && eligibleId((String) principalId, null, session) == null;
    }

    /** A scope only counts while the rule is on; with the rollback switch off it is inert. */
    private static boolean scopeApplies() {
        return SystemUserScope.isActive() && InteractiveUserPolicy.isRuleOn();
    }

    private static User systemUser() {
        return Try.of(() -> APILocator.systemUser())
                .onFailure(e -> Logger.error(RequestUserResolver.class,
                        "Unable to load the system user: " + e.getMessage(), e))
                .getOrNull();
    }

    private static User eligible(final User user, final HttpServletRequest request, final HttpSession session) {
        return InteractiveUserPolicy.refuseIfNotEligible(user, request, session);
    }

    private static String eligibleId(final String userId, final HttpServletRequest request,
            final HttpSession session) {
        return InteractiveUserPolicy.refuseIfNotEligibleId(userId, request, session);
    }
}
