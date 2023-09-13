package com.dotcms.api.client;

import java.util.Optional;

public interface ParamAuthentication {
     void setToken(char[] token);

    Optional<char[] > getToken();

}
