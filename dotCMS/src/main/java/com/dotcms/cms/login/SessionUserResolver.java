package com.dotcms.cms.login;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LoginAsSession;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import io.vavr.Lazy;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.servlet.http.HttpSession;

/**
 * Resolves the {@link User} an HTTP session belongs to, checking the user's current status on
 * every call.
 * <p>
 * A session identifies its user through the {@link WebKeys#USER_ID} attribute set at login, and
 * keeps a copy of the {@link User} object under {@link WebKeys#USER} after the first lookup. That
 * copy is a snapshot: it does not change when the user record is edited later. This resolver
 * always reads the current record through {@link UserAPI#loadUserById(String)}, which is backed
 * by the user cache and therefore cheap, and only yields a user that still exists and is active.
 * A user that was removed or made inactive after login resolves to no user, and the stale copy is
 * dropped from the session, so every code path that asks the session for its user (REST, the
 * back-end UI, DWR, portlets) sees the change on the next request, on every node. The one
 * exception is the {@link #ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN} switch (off by default): when it is
 * on, inactive accounts may log in, so an inactive user is kept usable here as well.
 */
public class SessionUserResolver {

    /**
     * Existing switch of the legacy login that lets inactive accounts log in. When it is on, an
     * inactive user is also kept usable in its session, for consistency with that login.
     */
    static final String ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN = "ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN";

    private static final Lazy<SessionUserResolver> INSTANCE =
            Lazy.of(() -> new SessionUserResolver(APILocator::getUserAPI));

    private final Supplier<UserAPI> userAPI;

    /**
     * Creates a resolver that checks users through the given {@link UserAPI}, for tests. The
     * rest of the code uses the shared instance from {@link #getInstance()}.
     *
     * @param userAPI the API used to read the current user record
     */
    @VisibleForTesting
    SessionUserResolver(final UserAPI userAPI) {
        this(() -> userAPI);
    }

    /**
     * Creates a resolver that asks {@code userAPI} for the {@link UserAPI} on every call, so the
     * shared instance follows the API locator instead of keeping the first API it was given.
     *
     * @param userAPI supplies the API used to read the current user record
     */
    private SessionUserResolver(final Supplier<UserAPI> userAPI) {
        this.userAPI = userAPI;
    }

    /**
     * Returns the shared resolver, built on the {@link UserAPI} from the {@link APILocator}.
     *
     * @return the shared {@link SessionUserResolver}
     */
    public static SessionUserResolver getInstance() {
        return INSTANCE.get();
    }

    /**
     * Resolves the user the session belongs to, if the session has one and that user currently
     * exists and is active.
     * <p>
     * The user id is taken from {@link WebKeys#USER_ID}, or from the {@link User} copy stored under
     * {@link WebKeys#USER} when a caller stored only the object. The current record is then read
     * through the user API:
     * <ul>
     *   <li>found and active: it is returned and, when the record changed, stored under
     *   {@link WebKeys#USER} in place of the older copy, so later readers of that attribute see
     *   the current data too;</li>
     *   <li>not found, or not active: the session is signed out (the user id, the cached copy and
     *   the Login As attributes are removed) and the result is empty; the person logs in again
     *   when the account is usable again;</li>
     *   <li>the lookup fails for another reason: the result is empty for this call and the cached
     *   copy is removed, while the user id stays, so the next request checks again.</li>
     * </ul>
     * A Login As session holds the impersonated user as its user and the administrator behind it
     * under {@link WebKeys#PRINCIPAL_USER_ID}; both must exist and be active for the session to
     * yield a user.
     *
     * @param session the HTTP session, may be {@code null}
     * @return the current {@link User} record, or empty when the session has no usable user
     */
    public Optional<User> resolve(final HttpSession session) {
        if (null == session) {
            return Optional.empty();
        }
        final Object cached = session.getAttribute(WebKeys.USER);
        final User cachedUser = cached instanceof User ? (User) cached : null;
        final String userId = resolveUserId(session, cachedUser);
        if (!UtilMethods.isSet(userId)) {
            return Optional.empty();
        }

        final User currentUser;
        try {
            currentUser = this.userAPI.get().loadUserById(userId);
        } catch (final NoSuchUserException e) {
            Logger.debug(this, () -> "The session user no longer exists; the session is signed out");
            signOut(session);
            return Optional.empty();
        } catch (final DotDataException | DotSecurityException | DotRuntimeException e) {
            Logger.error(this, "Could not read the status of the session user '" + userId
                    + "', treating the session as having no user for this request: "
                    + e.getMessage(), e);
            session.removeAttribute(WebKeys.USER);
            return Optional.empty();
        }

        if (!isUsable(currentUser)) {
            Logger.debug(this, () -> "The session user is not active; the session is signed out");
            signOut(session);
            return Optional.empty();
        }
        if (!principalIsUsable(session, userId)) {
            Logger.debug(this, () -> "The user behind a Login As session is not usable; the session is signed out");
            signOut(session);
            return Optional.empty();
        }
        if (cachedUser != currentUser && !sameRecord(cachedUser, currentUser)) {
            session.setAttribute(WebKeys.USER, currentUser);
        }
        return Optional.of(currentUser);
    }

    /**
     * Returns the current record of the user with the given id when that user exists and is
     * active, for callers that hold a user id without a session (for example a user id set as a
     * request attribute by a login filter). Unlike {@link #resolve(HttpSession)} there is nothing
     * to sign out or to drop when the user is missing, inactive or cannot be read: the id lives
     * on the request alone, and the request simply goes on without a user.
     *
     * @param userId the user id to check
     * @return the current {@link User} record, or empty when the user is blank, missing, inactive
     *         or cannot be read
     */
    public Optional<User> resolveActive(final String userId) {
        if (!UtilMethods.isSet(userId)) {
            return Optional.empty();
        }
        try {
            final User currentUser = this.userAPI.get().loadUserById(userId);
            return isUsable(currentUser) ? Optional.of(currentUser) : Optional.empty();
        } catch (final NoSuchUserException e) {
            return Optional.empty();
        } catch (final DotDataException | DotSecurityException | DotRuntimeException e) {
            Logger.error(this, "Could not read the status of the user '" + userId
                    + "', treating the request as having no user: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Removes what makes the session an authenticated one: the user id, the cached user copy and
     * the Login As attributes. The session object itself stays, as code handling the current
     * request may still hold it.
     */
    private static void signOut(final HttpSession session) {
        session.removeAttribute(WebKeys.USER);
        session.removeAttribute(WebKeys.USER_ID);
        session.removeAttribute(WebKeys.PRINCIPAL_USER_ID);
        session.removeAttribute(LoginAsSession.SESSION_KEY);
    }

    /**
     * Tells whether the copy kept in the session already describes the current record (same id,
     * same modification date, same status), in which case storing it again is a wasted session
     * write, which matters with a session store that persists every attribute change.
     */
    private static boolean sameRecord(final User cachedUser, final User currentUser) {
        return null != cachedUser
                && null != cachedUser.getUserId()
                && cachedUser.getUserId().equalsIgnoreCase(currentUser.getUserId())
                && Objects.equals(cachedUser.getModificationDate(), currentUser.getModificationDate())
                && cachedUser.isActive() == currentUser.isActive();
    }

    /**
     * In a Login As session, the administrator behind the impersonated user must also exist and
     * be active. Any other session has no principal, or the principal is the session user itself,
     * and passes.
     */
    private boolean principalIsUsable(final HttpSession session, final String userId) {
        final Object principalId = session.getAttribute(WebKeys.PRINCIPAL_USER_ID);
        if (!(principalId instanceof String) || !UtilMethods.isSet((String) principalId)
                || ((String) principalId).equalsIgnoreCase(userId)) {
            return true;
        }
        return resolveActive((String) principalId).isPresent();
    }

    /**
     * A user record can keep a session or request when it exists and is active, or, when the
     * {@link #ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN} switch is on, when it merely exists.
     */
    private static boolean isUsable(final User currentUser) {
        return null != currentUser
                && (currentUser.isActive()
                        || Config.getBooleanProperty(ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN, false));
    }

    /**
     * Picks the user id the session is bound to: the {@link WebKeys#USER_ID} attribute when set,
     * otherwise the id of the {@link User} copy stored in the session, if any.
     */
    private static String resolveUserId(final HttpSession session, final User cachedUser) {
        final Object userId = session.getAttribute(WebKeys.USER_ID);
        if (userId instanceof String && UtilMethods.isSet((String) userId)) {
            return (String) userId;
        }
        return null != cachedUser ? cachedUser.getUserId() : null;
    }

}
