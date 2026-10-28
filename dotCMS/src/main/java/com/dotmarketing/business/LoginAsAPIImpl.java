package com.dotmarketing.business;

import com.google.common.annotations.VisibleForTesting;
import com.dotcms.system.AppContext;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

/**
 * Default implementation of {@link LoginAsAPI}
 */
public class LoginAsAPIImpl implements LoginAsAPI {

    private UserAPI userAPI;

    private LoginAsAPIImpl() {
        this(APILocator.getUserAPI());
    }

    /**
     * Creates the API with the given {@link UserAPI}, so tests can pass a mock.
     *
     * @param userAPI The API used to load the principal user.
     */
    @VisibleForTesting
    LoginAsAPIImpl(UserAPI userAPI) {
        this.userAPI = userAPI;
    }

    private static class SingletonHolder {
        private static final LoginAsAPIImpl INSTANCE = new LoginAsAPIImpl();
    }

    public static LoginAsAPIImpl getInstance() {

        return LoginAsAPIImpl.SingletonHolder.INSTANCE;
    }

    /**
     * Returns the user who started the active Login As, read from the {@link LoginAsSession} that
     * the Login As endpoint stored in the context.
     *
     * @param appContext The context, usually the HTTP session.
     * @return The principal user while a Login As is active, otherwise {@code null}.
     * @throws LoginAsRuntimeException if the principal user cannot be loaded.
     */
    public User getPrincipalUser(AppContext appContext) {
        try {
            final String principalUserId = getPrincipalUserId(appContext);
            return principalUserId != null ? userAPI.loadUserById(principalUserId) : null;
        }catch(DotSecurityException|DotDataException e){
            throw new LoginAsRuntimeException(e);
        }
    }

    /**
     * @param appContext The context, usually the HTTP session.
     * @return {@code true} while a Login As is active in the context.
     */
    public boolean isLoginAsUser(AppContext appContext){
        return getPrincipalUserId(appContext) != null;
    }

    /**
     * Returns the ID of the user who started the active Login As. A Login As is active when the
     * context holds a {@link LoginAsSession} whose impersonated user is the context's current user.
     *
     * @param appContext The context, usually the HTTP session.
     * @return The ID of the user who started the Login As, or {@code null} when none is active.
     */
    @Override
    public String getPrincipalUserId(final AppContext appContext) {
        final Object loginAsSession = appContext.getAttribute(LoginAsSession.SESSION_KEY);
        if (loginAsSession instanceof LoginAsSession) {
            final LoginAsSession active = (LoginAsSession) loginAsSession;
            final Object currentUserId = appContext.getAttribute(WebKeys.USER_ID);
            if (active.getLoginAsUserId().equals(currentUserId)) {
                return active.getPrincipalUserId();
            }
        }
        return null;
    }
}
