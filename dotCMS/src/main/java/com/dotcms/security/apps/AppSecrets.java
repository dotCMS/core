package com.dotcms.security.apps;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AppSecrets implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String key;

    private final Map<String,Secret> secrets;

    @JsonCreator
    private AppSecrets(@JsonProperty("key") final String key,
            @JsonProperty("secrets") final Map<String, Secret> secrets) {
        this.key = key;
        this.secrets = null == secrets ? new HashMap<>() : secrets ;
    }

    public String getKey() {
        return key;
    }

    public Map<String, Secret> getSecrets() {
        return secrets;
    }

    public void destroy() {
        for (final Secret secret : secrets.values()) {
            secret.destroy();
        }
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final AppSecrets that = (AppSecrets) object;
        return key.equals(that.key) && this.secrets.equals(that.secrets);
    }

    public static Builder builder(){
        return new Builder();
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, secrets);
    }

    public static class Builder {

        private final Map<String, Secret> secretMap = new HashMap<>();
        private String key;

        public AppSecrets build(){
            return new AppSecrets(key, ImmutableMap.copyOf(secretMap));
        }

        public Builder withKey(final String key){
            this.key = key;
            return this;
        }

        public Builder withSecret(final String name, final Secret secret) {
            secretMap.put(name, secret);
            return this;
        }

        public Builder withHiddenSecret(final String name, final String value) {
            return withSecret(
                    name,
                    Secret.builder()
                            .withValue(value)
                            .withHidden(true)
                            .withType(Type.STRING)
                            .build());
        }

        public Builder withHiddenSecret(final String name, final boolean value){
            return withHiddenSecret(name, String.valueOf(value));
        }

        public Builder withSecret(final String name, final String value){
            return withSecret(
                    name,
                    Secret.builder()
                            .withValue(value)
                            .withHidden(false)
                            .withType(Type.STRING)
                            .build());
        }

        public Builder withSecret(final String name, final boolean value) {
            return withSecret(name, String.valueOf(value));
        }

        public Builder withSecrets(final Map<String, Secret> secrets) {
            secretMap.putAll(secrets);
            return this;
        }

    }

    /**
     * Short hand to
     * new Builder().build();
     * @return
     */
    public static AppSecrets empty(){
      return new Builder().build();
    }

    @Override
    public String toString() {
        final List<String> stringsList = secrets.entrySet().stream()
                .map(entry -> {
                   final String name = entry.getKey();
                   final Secret secret = entry.getValue();
                   return "{ name: " + name + " , type: " + secret.getType() + ", hidden: " + secret.isHidden() + "}";
                }).collect(Collectors.toList());

        return String.format("AppSecrets{key= `%s` secrets=`%s` }",key, stringsList);
    }
}
