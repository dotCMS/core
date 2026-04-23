package com.dotcms.auth.dotAuth.rest;

import com.dotcms.auth.dotAuth.session.DotAuthSession;
import com.dotcms.auth.dotAuth.session.DotAuthSessionCache;
import com.dotcms.auth.dotAuth.session.DotAuthSessionCacheImpl;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.ContainerRequest;

public final class DotAuthSessionCredentialProcessorImpl implements DotAuthSessionCredentialProcessor {

    private static final long serialVersionUID = 1L;

    private static final String BEARER = "Bearer ";

    private final DotAuthSessionCache sessionCache;

    private static final class SingletonHolder {
        private static final DotAuthSessionCredentialProcessorImpl INSTANCE =
                new DotAuthSessionCredentialProcessorImpl(DotAuthSessionCacheImpl.getInstance());
    }

    public static DotAuthSessionCredentialProcessorImpl getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @VisibleForTesting
    DotAuthSessionCredentialProcessorImpl(final DotAuthSessionCache sessionCache) {
        this.sessionCache = sessionCache;
    }

    @Override
    public User processAuthHeaderFromSessionRef(final HttpServletRequest request) {
        final String header = request.getHeader(ContainerRequest.AUTHORIZATION);
        if (StringUtils.isEmpty(header) || !header.startsWith(BEARER)) {
            return null;
        }
        final String candidate = header.substring(BEARER.length()).trim();
        if (!candidate.startsWith(DotAuthSessionCache.SESSION_REF_PREFIX)) {
            return null;
        }

        final Optional<DotAuthSession> sessionOpt = sessionCache.get(candidate);
        if (sessionOpt.isEmpty()) {
            // Prefix matched, so the caller meant a dotAuth session-ref. Don't fall through
            // to the JWT processor with a credential we already know won't parse as a JWT.
            throw new SecurityException("Invalid or expired session", Response.Status.UNAUTHORIZED);
        }

        final String userId = sessionOpt.get().getUserId();
        try {
            final User user = APILocator.getUserAPI().loadUserById(userId);
            if (user == null || !user.isActive()) {
                sessionCache.invalidate(candidate);
                throw new SecurityException("User for session is no longer active",
                        Response.Status.UNAUTHORIZED);
            }
            request.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
            request.setAttribute(com.liferay.portal.util.WebKeys.USER, user);
            return user;
        } catch (final SecurityException se) {
            throw se;
        } catch (final Exception e) {
            Logger.warn(DotAuthSessionCredentialProcessorImpl.class,
                    "Failed resolving user for dotAuth session-ref: " + e.getMessage());
            throw new SecurityException("Invalid session", Response.Status.UNAUTHORIZED);
        }
    }
}
