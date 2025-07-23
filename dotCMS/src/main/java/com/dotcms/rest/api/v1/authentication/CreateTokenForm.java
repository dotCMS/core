package com.dotcms.rest.api.v1.authentication;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.ValidationException;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;

@JsonDeserialize(builder = CreateTokenForm.Builder.class)
public class CreateTokenForm extends Validated {

    @NotNull
    @Length(min = 2, max = 100)
    public final String user;

    @NotNull
    public final String password;

    public final int expirationDays;
    
    public final String label;

    private CreateTokenForm(Builder builder) {
        user = builder.user;
        password = builder.password;
        expirationDays = builder.expirationDays;
        label=builder.label;
        try {
        	checkValid();
        }catch(ValidationException ve){
        	HttpServletRequestThreadLocal threadLocal = HttpServletRequestThreadLocal.INSTANCE;
        	SecurityLogger.logInfo(this.getClass(),"An invalid attempt to login as " + (null != user?user.toLowerCase():"")
                    + " has been made from IP: " + (null != threadLocal.getRequest()?threadLocal.getRequest().getRemoteAddr():"0.0.0.0"));
        	throw ve;
        }
    }

    public static final class Builder {
        @JsonProperty(required = true) private String user; // not present on create
        @JsonProperty(required = true) private String password;
        @JsonProperty private int expirationDays = -1;
        @JsonProperty private String label=null;
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder expirationDays(int expirationDays) {
            this.expirationDays = expirationDays;
            return this;
        }
        public Builder label(String label) {
            this.label = UtilMethods.isSet(label) ? label : null;
            return this;
        }
        
        

        public CreateTokenForm build() {
            return new CreateTokenForm(this);
        }
    }
}

