package com.dotcms.security.secret;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ServiceDescriptor {

    private String key;

    private String name;

    private String description;

    private String iconUrl;

    private boolean allowExtraParameters;

    private Map<String,Param> params;

    public ServiceDescriptor(final String key, final String name, final String description,
            final String iconUrl, final boolean allowExtraParameters) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.allowExtraParameters = allowExtraParameters;
    }

    public ServiceDescriptor() {
    }

    public String getKey() {
        return key;
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

    public boolean isAllowExtraParameters() {
        return allowExtraParameters;
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
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final ServiceDescriptor that = (ServiceDescriptor) object;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
