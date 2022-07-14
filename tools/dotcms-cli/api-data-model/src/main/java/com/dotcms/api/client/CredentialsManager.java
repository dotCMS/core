package com.dotcms.api.client;

import com.dotcms.model.config.CredentialsBean;
import com.dotcms.model.config.ServiceBean;
import java.io.IOException;
import java.util.Map;

public interface CredentialsManager {

    void persist(String service, CredentialsBean credentials) throws IOException;

    Map<String, ServiceBean> services();

    void activate(String profileName);

}
