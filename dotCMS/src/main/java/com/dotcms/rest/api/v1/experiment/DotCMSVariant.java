package com.dotcms.rest.api.v1.experiment;

public class DotCMSVariant {

    private String name;
    private String domain;
    private String key;

    public DotCMSVariant(String name, String domain, String key) {
        this.name = name;
        this.domain = domain;
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public String getDomain() {
        return domain;
    }

    public String getKey() {
        return key;
    }
}
