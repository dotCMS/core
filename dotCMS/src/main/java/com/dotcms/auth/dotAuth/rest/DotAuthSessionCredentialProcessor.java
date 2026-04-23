package com.dotcms.auth.dotAuth.rest;

import com.liferay.portal.model.User;
import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;

/**
 * Credential processor for dotAuth session-refs. Sibling of
 * {@code JsonWebTokenAuthCredentialProcessor}: it owns {@code Authorization:
 * Bearer} headers whose credential begins with
 * {@link com.dotcms.auth.dotAuth.session.DotAuthSessionCache#SESSION_REF_PREFIX}
 * and leaves everything else for downstream processors to handle.
 */
public interface DotAuthSessionCredentialProcessor extends Serializable {

    /**
     * If the request carries an {@code Authorization: Bearer dsr_…} header, resolve
     * the referenced dotAuth session to its {@link User} and return it. Returns
     * {@code null} when the header is absent, uses a different scheme, or does not
     * carry the dotAuth session-ref prefix — callers should then fall through to the
     * next credential processor in the chain.
     *
     * @throws com.dotcms.rest.exception.SecurityException with 401 when the prefix
     *         matches but the session-ref is unknown or expired. Prefix-match means
     *         the caller explicitly intended a dotAuth session, so we reject
     *         rather than silently falling through to the JWT processor.
     */
    User processAuthHeaderFromSessionRef(HttpServletRequest request);
}
