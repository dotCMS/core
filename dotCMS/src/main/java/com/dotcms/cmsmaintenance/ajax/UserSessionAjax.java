package com.dotcms.cmsmaintenance.ajax;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.listeners.SessionMonitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

/**
 * UserSessionAjax provides AJAX endpoints for session management including
 * validating user access, invalidating sessions, and retrieving session lists.
 */
public class UserSessionAjax {
    private static final String CSRF_TOKEN_ATTRIBUTE = "csrfToken";
    private static final String CSRF_TOKEN_TIMESTAMP_ATTRIBUTE = "csrfTokenTimestamp";
    private static final Duration TOKEN_EXPIRY_DURATION = Duration.ofMinutes(15);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Validates if the current user has access to the CMS Maintenance Portlet.
     *
     * @return true if the user has access, otherwise throws an exception.
     */
    public boolean validateUser() {
        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user;
        try {
            user = com.liferay.portal.util.PortalUtil.getUser(req);
            if (user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("maintenance", user)) {
                throw new DotSecurityException("User does not have access to the CMS Maintenance Portlet");
            }
            return true;
        } catch (NoSuchUserException | DotDataException | DotSecurityException e) {
            Logger.error(this, "Error validating user: " + e.getMessage(), e);
            throw new DotRuntimeException("Error validating user", e);
        }
    }

    /**
     * Invalidates a session specified by the token.
     *
     * @param token the token of the session to invalidate
     * @throws NoSuchUserException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public void invalidateSession(String token) throws NoSuchUserException, DotDataException, DotSecurityException {
        validateUser();
        SessionMonitor sm = getSessionMonitor();
        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
        HttpSession callingSession = WebContextFactory.get().getSession();

        String csrfToken = getAndValidateCSRF(callingSession);

        boolean sessionInvalidated = false;

        for (String sessionId : sm.getSysUsers().keySet()) {
            HttpSession session = sm.getUserSessions().get(sessionId);
            synchronized (session) {
                if (validateSessionId(session.getId(), csrfToken, token)) {
                    User user = APILocator.getUserAPI().loadUserById(sm.getSysUsers().get(sessionId), APILocator.getUserAPI().getSystemUser(), false);
                    if (!callingSession.getId().equals(sessionId)) {
                        session.invalidate();
                        sessionInvalidated = true;
                        break;
                    } else {
                        throw new IllegalArgumentException("Can't invalidate your own session");
                    }
                }
            }
        }

        if (!sessionInvalidated) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
    }

    /**
     * Invalidates all sessions except for the current user's session.
     *
     * @throws NoSuchUserException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public void invalidateAllSessions() throws NoSuchUserException, DotDataException, DotSecurityException {
        validateUser();
        SessionMonitor sm = getSessionMonitor();
        HttpSession callingSession = WebContextFactory.get().getSession();

        for (String sessionId : sm.getSysUsers().keySet()) {
            HttpSession session = sm.getUserSessions().get(sessionId);
            synchronized (session) {
                if (!callingSession.getId().equals(sessionId)) {
                    session.invalidate();
                }
            }
        }
    }

    /**
     * Retrieves a list of active sessions with their details.
     *
     * @return a list of maps containing session details
     * @throws NoSuchUserException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public List<Map<String, String>> getSessionList() throws NoSuchUserException, DotDataException, DotSecurityException {
        validateUser();
        List<Map<String, String>> sessionList = new ArrayList<>();
        SessionMonitor sm = getSessionMonitor();
        HttpSession callingSession = WebContextFactory.get().getSession();
        String csrfToken = generateAndStoreToken(callingSession);

        for (String sessionId : sm.getSysUsers().keySet()) {
            HttpSession session = sm.getUserSessions().get(sessionId);
            synchronized (session) {
                Map<String, String> sessionInfo = new HashMap<>();
                String obfSession = obfuscateSessionId(sessionId, csrfToken);
                sessionInfo.put("obfSession", obfSession);
                sessionInfo.put("isCurrent", String.valueOf(sessionId.equals(callingSession.getId())));
                User user = APILocator.getUserAPI().loadUserById(sm.getSysUsers().get(sessionId), APILocator.getUserAPI().getSystemUser(), false);
                sessionInfo.put("userId", user.getUserId());
                sessionInfo.put("userEmail", user.getEmailAddress());
                sessionInfo.put("userFullName", user.getFullName());
                sessionInfo.put("address", sm.getSysUsersAddress().get(sessionId));
                Date creationTime = new Date(session.getCreationTime());
                sessionInfo.put("sessionTime", DateUtil.prettyDateSince(creationTime, PublicCompanyFactory.getDefaultCompany().getLocale()));
                sessionList.add(sessionInfo);
            }
        }
        return sessionList;
    }

    /**
     * Generates and stores a CSRF token in the session.
     *
     * @param session the HTTP session
     * @return the generated CSRF token
     */
    private String generateAndStoreToken(HttpSession session) {
        String token = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        session.setAttribute(CSRF_TOKEN_ATTRIBUTE, token);
        session.setAttribute(CSRF_TOKEN_TIMESTAMP_ATTRIBUTE, timestamp);
        return token;
    }

    /**
     * Retrieves and validates the CSRF token from the session.
     *
     * @param session the HTTP session
     * @return the CSRF token
     */
    private String getAndValidateCSRF(HttpSession session) {
        String csrf = (String) session.getAttribute(CSRF_TOKEN_ATTRIBUTE);
        Instant tokenTimestamp = (Instant) session.getAttribute(CSRF_TOKEN_TIMESTAMP_ATTRIBUTE);
        if (csrf == null || tokenTimestamp == null || !isTokenValid(tokenTimestamp)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        return csrf;
    }

    /**
     * Obfuscates a session ID using HMAC with the specified secret key.
     *
     * @param sessionId the session ID to obfuscate
     * @param secretKey the secret key for HMAC
     * @return the obfuscated session ID
     */
    public static String obfuscateSessionId(String sessionId, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), HMAC_ALGORITHM);
            mac.init(key);
            byte[] hash = mac.doFinal(sessionId.getBytes());
            byte[] truncatedHash = new byte[16];
            System.arraycopy(hash, 0, truncatedHash, 0, 16);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(truncatedHash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validates if the provided obfuscated session ID matches the original session ID using the secret key.
     *
     * @param sessionId the original session ID
     * @param secretKey the secret key for HMAC
     * @param obfuscatedId the obfuscated session ID to validate
     * @return true if the session IDs match, false otherwise
     */
    public static boolean validateSessionId(String sessionId, String secretKey, String obfuscatedId) {
        String generatedObfuscatedId = obfuscateSessionId(sessionId, secretKey);
        return generatedObfuscatedId.equals(obfuscatedId);
    }

    /**
     * Checks if the token is still valid based on its timestamp.
     *
     * @param tokenTimestamp the timestamp of the token
     * @return true if the token is valid, false otherwise
     */
    private boolean isTokenValid(Instant tokenTimestamp) {
        return Instant.now().isBefore(tokenTimestamp.plus(TOKEN_EXPIRY_DURATION));
    }

    /**
     * Retrieves the SessionMonitor from the servlet context.
     *
     * @return the SessionMonitor instance
     */
    private SessionMonitor getSessionMonitor() {
        return (SessionMonitor) WebContextFactory.get().getServletContext().getAttribute(WebKeys.USER_SESSIONS);
    }
}