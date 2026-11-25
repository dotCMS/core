package com.dotmarketing.business;

import java.io.Serializable;

/**
 * Records an active Login As in the HTTP session: the user who started it and the user being
 * impersonated. The Login As endpoint stores it under {@link #SESSION_KEY} and Logout As removes it;
 * {@link LoginAsAPI} reads the principal user from it.
 * <p>
 * The record holds one level and does not nest. If the impersonated user starts a further Login As,
 * the new record replaces this one, and Logout As returns to that impersonated user, not to the user
 * who started the first Login As.
 */
public final class LoginAsSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Session attribute that holds the {@link LoginAsSession} while a Login As is active. */
    public static final String SESSION_KEY = "LOGIN_AS_SESSION";

    private final String principalUserId;
    private final String loginAsUserId;

    /**
     * @param principalUserId The ID of the user who started the Login As.
     * @param loginAsUserId   The ID of the user being impersonated.
     */
    public LoginAsSession(final String principalUserId, final String loginAsUserId) {
        this.principalUserId = principalUserId;
        this.loginAsUserId = loginAsUserId;
    }

    /** @return The ID of the user who started the Login As. */
    public String getPrincipalUserId() {
        return principalUserId;
    }

    /** @return The ID of the user being impersonated. */
    public String getLoginAsUserId() {
        return loginAsUserId;
    }
}
