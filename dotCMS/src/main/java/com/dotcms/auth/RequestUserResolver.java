package com.dotcms.auth;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.ejb.UserLocalManagerUtil;
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
     * Resolves the user of a request.
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
        User user = eligible((User) request.getAttribute(WebKeys.USER), request, null);
        if (user == null) {
            user = resolveSessionUser(request.getSession(false));
        }
        if (user == null) {
            final String userId = resolveRequestUserId(request);
            if (userId != null) {
                user = eligible(Try.of(() -> UserLocalManagerUtil.getUserById(userId)).getOrNull(), request, null);
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
     * Resolves the user held by a session.
     *
     * @param session the session, may be null
     * @return the user, or null when there is none or the one found is not eligible
     */
    public static User resolveSessionUser(final HttpSession session) {
        if (scopeApplies()) {
            return systemUser();
        }
        if (session == null) {
            return null;
        }
        User user = eligible((User) session.getAttribute(WebKeys.USER), null, session);
        if (user == null) {
            final String userId = resolveSessionUserId(session);
            if (userId == null) {
                return null;
            }
            user = eligible(Try.of(() -> UserLocalManagerUtil.getUserById(userId)).getOrNull(), null, session);
            if (user != null) {
                session.setAttribute(WebKeys.USER, user);
            }
        }
        return user;
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
        return session == null ? null : eligibleId((String) session.getAttribute(WebKeys.USER_ID), null, session);
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
