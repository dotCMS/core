package com.dotcms.api.client.model;

import com.dotcms.model.config.ServiceBean;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ServiceManager {

    ServiceManager persist(ServiceBean service) throws IOException;

    List<ServiceBean> services() throws IOException;

    /**
     * Get the selected service
     * @return the selected service or empty if none is selected
     * @throws IOException if an error occurs
     */
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

    /**
     * Get a service by name
     * @param name service name
     * @return the service or empty if not found
     * @throws IOException if an error occurs
     */
    default Optional<ServiceBean> get(final String name) throws IOException {
        return services().stream().filter(serviceBean -> name.equals(serviceBean.name())).findFirst();
    }

    /**
     * Remove all services
     * @return the service manager
     * @throws IOException if an error occurs
     */
    ServiceManager removeAll() throws IOException;

}
