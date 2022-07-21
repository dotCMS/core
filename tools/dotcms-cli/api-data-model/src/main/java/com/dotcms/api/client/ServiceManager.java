package com.dotcms.api.client;

import com.dotcms.model.config.ServiceBean;
import java.io.IOException;
import java.util.List;

public interface ServiceManager {

    void persist(ServiceBean service) throws IOException;

    List<ServiceBean> services();

}
