package com.dotcms.security.secret;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ServiceDescriptor {

    private String serviceKey;

    private String name;

    private String description;

    private String iconUrl;

    private Map<String,Param> params;

    public ServiceDescriptor(final String serviceKey, final String name, final String description, final String iconUrl) {
        this.serviceKey = serviceKey;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
    }

    public ServiceDescriptor() {
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public Map<String, Param> getParams() {
        if(null == params){
            params = new HashMap<>();
        }
        return params;
    }

    public void addParam(final String name, final String value, final boolean hidden,
            final Type type, final String label, final String hint) {
        getParams().put(name, Param.newParam(value, hidden, type, label, hint));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ServiceDescriptor that = (ServiceDescriptor) o;
        return serviceKey.equals(that.serviceKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceKey);
    }
}
