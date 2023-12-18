package com.dotcms.api.client.model;

import com.dotcms.model.config.ServiceBean;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ServiceManager {

    ServiceManager persist(ServiceBean service) throws IOException;

    List<ServiceBean> services() throws IOException;

    default Optional<ServiceBean> selected() throws IOException {
        final List<ServiceBean> beans = services().stream().filter(ServiceBean::active)
                .collect(Collectors.toList());
        final int size = beans.size();
        if(size > 1){
            throw new IllegalStateException(
                   String.format("There can only be one active service (%d) were found, check your configuration.", size)
            );
        }
        return !beans.isEmpty() ? Optional.of(beans.get(0)) : Optional.empty();
    }

    ServiceManager removeAll() throws IOException;

}
