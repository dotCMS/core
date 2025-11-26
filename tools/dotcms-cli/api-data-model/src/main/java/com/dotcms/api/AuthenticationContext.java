package com.dotcms.api;

import java.io.IOException;
import java.util.Optional;

/**
 * Context that keeps the logged-in user info
 */
public interface AuthenticationContext {

    /**
     * Current logged-in user Empty Optional if none
     * @return
     */
    Optional<String> getUser() ;

    /**
     * Current logged in Token or Empty if none
     * @return
     */
    Optional<char[]> getToken() throws IOException;

    /**
     * login attempts to get a token and it
     * @param user
     * @param password
     */
    void login(String user, char[] password) throws IOException;


    void login(char[] token) throws IOException;


    /**
     * Reset eliminates the current logged-in user info
     */
    void reset();

}
