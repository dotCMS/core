package com.dotcms.filters.interceptor.dotcms;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.util.security.Encryptor;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.CookieUtil;
import io.vavr.control.Try;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Default Auto Login implementation
 *
 * @author jsanca
 */
public class DefaultAutoLoginWebInterceptor implements WebInterceptor {

    private final JsonWebTokenUtils jsonWebTokenUtils;
    private final LoginServiceAPI loginServiceAPI;
    private Encryptor encryptor;

    public DefaultAutoLoginWebInterceptor() {
        this(JsonWebTokenUtils.getInstance(), APILocator.getLoginServiceAPI(),
                EncryptorFactory.getInstance().getEncryptor());
    }

    public DefaultAutoLoginWebInterceptor(final JsonWebTokenUtils jsonWebTokenUtils,
            final LoginServiceAPI loginServiceAPI,
            final Encryptor encryptor) {

        this.jsonWebTokenUtils = jsonWebTokenUtils;
        this.loginServiceAPI = loginServiceAPI;
        this.encryptor = encryptor;
    }

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) {

        if (PortalUtil.getUser(request) != null) {
            return Result.SKIP;
        }

        final Cookie rememberMe = UtilMethods.getCookie(request.getCookies(), CookieKeys.REMEMBER_ME_COOKIE.get());
        if (rememberMe == null) {
            return Result.NEXT;
        }


        final Optional<JWToken> token = APILocator.getApiTokenAPI().fromJwt(rememberMe.getValue(), request.getRemoteAddr());
        Optional<User> user= token.flatMap(JWToken::getActiveUser);

        if(user.isEmpty()) {
            // user is null because token is expired
            CookieUtil.deleteCookie(request, response, CookieKeys.JWT_ACCESS_TOKEN);
            return Result.NEXT;
        }

        // if the token was expiry date is greater than the allowed EXPIRY date, reset it
        // maybe someone updated the configured MAX_AGE_DAYS
        if(token.get().getExpiresDate().after(Date.from(Instant.now().plus(LoginServiceAPI.JWT_TOKEN_MAX_AGE_DAYS.get(), ChronoUnit.DAYS)))) {
            // refresh the token
            this.loginServiceAPI.doRememberMe(request, response, user.get(), true);
        }

        if (this.loginServiceAPI.doCookieLogin(token.get().getSubject(), request, response)) {
            return Result.SKIP;
        }

        // user is null because token is expired
        CookieUtil.deleteCookie(request, response, CookieKeys.JWT_ACCESS_TOKEN);
        return Result.NEXT;


    } // intercept.


} // E:O:F:DefaultAutoLoginWebInterceptor.
