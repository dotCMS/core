package com.dotcms.api.client;

import java.util.Optional;

public interface ParamAuthentication {
     void setToken(String token);

    Optional<String> getToken();

}
