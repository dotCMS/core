package com.dotcms.rest.api.v1.authentication;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.exception.ValidationException;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.repackage.org.hibernate.validator.constraints.Length;
import com.dotcms.rest.api.Validated;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.util.SecurityLogger;

@JsonDeserialize(builder = AuthenticationForm.Builder.class)
public class AuthenticationForm extends Validated {

    @NotNull
    @Length(min = 2, max = 100)
    private final String userId;

    @NotNull
    private final String password;

    private  final boolean rememberMe;

    private final String language;

    private final String country;

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public String getCountry() {
        return country;
    }

    public String getLanguage() {
        return language;
    }

    private AuthenticationForm(Builder builder) {
        userId = builder.userId;
        password = builder.password;
        rememberMe = builder.rememberMe;
        language = builder.language;
        country  = builder.country;
        try {
        	checkValid();
        }catch(ValidationException ve){
        	HttpServletRequestThreadLocal threadLocal = HttpServletRequestThreadLocal.INSTANCE;
        	SecurityLogger.logInfo(this.getClass(),"An invalid attempt to login as " + (null != userId?userId.toLowerCase():"") + " has been made from IP: " +
                    HttpRequestDataUtil.getRemoteAddress(threadLocal.getRequest()));
        	throw ve;
        }
    }

    public static final class Builder {
        @JsonProperty(required = true) private String userId; // not present on create
        @JsonProperty(required = true) private String password;
        @JsonProperty private boolean rememberMe;
        @JsonProperty private String language;
        @JsonProperty private String country;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder rememberMe(boolean rememberMe) {
            this.rememberMe = rememberMe;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public AuthenticationForm build() {
            return new AuthenticationForm(this);
        }
    }
}

