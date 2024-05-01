package com.dotcms.api.client.model;

import java.util.Optional;


/***
 * This interface is used to pass the token from the CLI to the API client.
 * If the token is present here we use directly instead of trying to load it from the service manager.
 */
public interface AuthenticationParam {
     void setToken(char[] token);

    Optional<char[] > getToken();

    void reset();

}
