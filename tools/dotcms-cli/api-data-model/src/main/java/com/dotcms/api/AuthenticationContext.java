package com.dotcms.api;

import java.util.Optional;

/**
 * Context that keeps the loged in user info
 */
public interface AuthenticationContext {

    /**
     * Current logged in user Empty Optional if none
     * @return
     */
    Optional<String> getUser() ;

    /**
     * Current logged in Token or Empty if none
     * @return
     */
    Optional<char[]> getToken();

    /**
     * login attempts to get a token and it
     * @param user
     * @param password
     */
    void login(String user, char[] password);

    /**
     * Reset eliminates the current logged-in user info
     */
    void reset();

}
