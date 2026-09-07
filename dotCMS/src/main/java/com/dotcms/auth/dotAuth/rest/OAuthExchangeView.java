package com.dotcms.auth.dotAuth.rest;

import java.util.List;

/**
 * Response body of {@code POST /v1/dotauth/oauth/exchange}. Carries an opaque
 * dotAuth session-ref that the SPA sends as {@code Authorization: Bearer
 * <sessionRef>} on subsequent content-API calls, the absolute expiry of that
 * session, and a minimal identity summary (including applied role keys) for
 * the UI to render without a second round-trip.
 */
public class OAuthExchangeView {

    private final String sessionRef;
    private final String expiresAt;        // ISO-8601 UTC (e.g. "2026-04-30T14:22:11Z")
    private final int    expirationDays;   // mirror of expiresAt for existing SPA code paths
    private final UserSummary user;

    public OAuthExchangeView(final String sessionRef,
                             final String expiresAt,
                             final int expirationDays,
                             final UserSummary user) {
        this.sessionRef     = sessionRef;
        this.expiresAt      = expiresAt;
        this.expirationDays = expirationDays;
        this.user           = user;
    }

    public String getSessionRef()     { return sessionRef; }
    public String getExpiresAt()      { return expiresAt; }
    public int    getExpirationDays() { return expirationDays; }
    public UserSummary getUser()      { return user; }

    /** Minimal identity summary returned to the SPA after a successful exchange. */
    public static final class UserSummary {
        private final String userId;
        private final String email;
        private final String firstName;
        private final String lastName;
        private final String fullName;
        private final List<String> roles;

        public UserSummary(final String userId,
                           final String email,
                           final String firstName,
                           final String lastName,
                           final String fullName,
                           final List<String> roles) {
            this.userId    = userId;
            this.email     = email;
            this.firstName = firstName;
            this.lastName  = lastName;
            this.fullName  = fullName;
            this.roles     = roles;
        }

        public String getUserId()    { return userId; }
        public String getEmail()     { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName()  { return lastName; }
        public String getFullName()  { return fullName; }
        public List<String> getRoles() { return roles; }
    }
}
