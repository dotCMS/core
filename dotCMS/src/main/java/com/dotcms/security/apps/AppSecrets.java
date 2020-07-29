package com.dotcms.security.apps;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AppSecrets that = (AppSecrets) o;
        return key.equals(that.key) && this.secrets.equals(that.secrets); //areEqual(this.secrets, that.secrets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, secrets);
    }

    public static class Builder {

        private final Map<String,Secret> secretMap = new HashMap<>();
        private String key;

        public AppSecrets build(){
            return new AppSecrets(key, ImmutableMap.copyOf(secretMap));
        }

        public Builder withKey(final String key){
            this.key = key;
            return this;
        }

        public Builder withHiddenSecret(final String name, final String value) {
            secretMap.put(
                    name, Secret.newSecret(value.toCharArray(), Type.STRING, true)
            );
            return this;
        }

        public Builder withHiddenSecret(final String name, final boolean value){
            secretMap.put(
                    name, Secret.newSecret(String.valueOf(value).toCharArray(), Type.BOOL, true)
            );
            return this;
        }

        public Builder withSecret(final String name, final String value){
            secretMap.put(
                    name, Secret.newSecret(value.toCharArray(), Type.STRING, false)
            );
            return this;
        }

        public Builder withSecret(final String name, final boolean value){
            secretMap.put(
                    name, Secret.newSecret(String.valueOf(value).toCharArray(), Type.STRING, false)
            );
            return this;
        }

        public Builder withSecret(final String name, final Secret secret){
            secretMap.put(name, secret);
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

}
