package com.dotcms.api;

import java.util.Optional;

public interface AuthenticationContext {

    Optional<String> getUser() ;

    Optional<char[]> getToken();

    void login(String user, char[] password);

}
