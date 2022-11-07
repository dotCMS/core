package com.dotcms.api.client;

import com.dotcms.model.config.ServiceBean;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface ServiceManager {

    ServiceManager persist(ServiceBean service) throws IOException;

    List<ServiceBean> services();

    default Optional<ServiceBean> selected(){
        return services().stream().filter(ServiceBean::active)
                .findFirst();
    }

    ServiceManager removeAll();

}
