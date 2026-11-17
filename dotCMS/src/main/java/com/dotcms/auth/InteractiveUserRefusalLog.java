package com.dotcms.auth;

import com.dotmarketing.util.SecurityLogger;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.security.NoSuchAlgorithmException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

/**
 * Writes the security-log entry for an identity that was refused because it cannot be used interactively.
 * <p>
 * With a session there is at most one entry per session: the sessions already reported are remembered in a
 * bounded cache held here, outside the session, keyed by a hash of the session id, so the session itself is
 * never modified. With no session there is one entry per refused request. The entry never contains the
 * session id or a credential.
 */
public final class InteractiveUserRefusalLog {

    /** Text of the entry; contains neither a session identifier nor a credential. */
    static final String MESSAGE = "user not eligible for interactive use";

    private static final int MAX_TRACKED_SESSIONS = 10_000;

    private static final Cache<String, Boolean> REPORTED = Caffeine.newBuilder()
            .maximumSize(MAX_TRACKED_SESSIONS)
            .expireAfterAccess(Duration.ofHours(1))
            .executor(Runnable::run)
            .build();

    /** Set while an entry is being written: the logger reads the thread-bound request, which must not log again. */
    private static final ThreadLocal<Boolean> WRITING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private InteractiveUserRefusalLog() {
    }

    /**
     * Records a refusal. Call it at the point where an identity is refused.
     *
     * @param request the request, may be null
     * @param session the session the identity came from or was written to, may be null; when null the
     *                session of the request, if any, is used
     */
    public static void record(final HttpServletRequest request, final HttpSession session) {
        if (WRITING.get()) {
            return;
        }
        final HttpSession effective = session != null ? session
                : request != null ? request.getSession(false) : null;
        final String id = effective == null ? null : effective.getId();
        if (id == null || REPORTED.asMap().putIfAbsent(hash(id), Boolean.TRUE) == null) {
            WRITING.set(Boolean.TRUE);
            try {
                SecurityLogger.logWarn(InteractiveUserRefusalLog.class, MESSAGE);
            } finally {
                WRITING.remove();
            }
        }
    }

    private static String hash(final String sessionId) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sessionId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static int maxTrackedSessions() {
        return MAX_TRACKED_SESSIONS;
    }

    static int trackedSessions() {
        REPORTED.cleanUp();
        return (int) REPORTED.estimatedSize();
    }

    static void resetForTests() {
        REPORTED.invalidateAll();
    }
}
