package com.dotmarketing.osgi;

import java.io.Serializable;
import java.util.Hashtable;

/**
 * Encapsulates an OSGI service bean, with the service instance and the properties
 * @author jsanca
 */
public class ServiceBean<T> implements Serializable {

    private final T service;
    private final Hashtable<String, ?> properties;

    public ServiceBean(final T service,
                       final Hashtable<String, ?> properties) {

        this.service = service;
        this.properties = properties;
    }

    public T getService() {
        return service;
    }

    public Hashtable<String, ?> getProperties() {
        return properties;
    }
} // E:O:F:ServiceBean.
