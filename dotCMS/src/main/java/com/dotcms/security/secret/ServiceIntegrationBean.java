package com.dotcms.security.secret;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ServiceIntegrationBean implements Serializable {

    private String serviceKey;

    private Map<String,Secret> secrets;

    @JsonCreator
    public ServiceIntegrationBean( @JsonProperty("serviceKey")  final String serviceKey, @JsonProperty("secrets") final Map<String, Secret> secrets) {
        this.serviceKey = serviceKey;
        this.secrets = secrets;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public Map<String, Secret> getSecrets() {
        return secrets;
    }

    public static class Builder {

        private Map<String,Secret> secretMap = new HashMap<>();
        private String serviceKey;

        ServiceIntegrationBean build(){
            return new ServiceIntegrationBean(serviceKey, ImmutableMap.copyOf(secretMap));
        }

        Builder withServiceKey(final String serviceKey){
            this.serviceKey = serviceKey;
            return this;
        }

        Builder withHiddenSecret(final String name, final String value) {
            secretMap.put(
                    name, Secret.newSecret(value.toCharArray(), SecretType.STRING, true)
            );
            return this;
        }

        Builder withHiddenSecret(final String name, final boolean value){
            secretMap.put(
                    name, Secret.newSecret(String.valueOf(value).toCharArray(), SecretType.BOOL, true)
            );
            return this;
        }

        Builder withSecret(final String name, final String value){
            secretMap.put(
                    name, Secret.newSecret(value.toCharArray(), SecretType.STRING, false)
            );
            return this;
        }

        Builder withSecret(final String name, final boolean value){
            secretMap.put(
                    name, Secret.newSecret(String.valueOf(value).toCharArray(), SecretType.STRING, false)
            );
            return this;
        }

    }

}
