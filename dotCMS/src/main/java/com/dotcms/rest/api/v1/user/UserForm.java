package com.dotcms.rest.api.v1.user;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotBlank;
import com.dotcms.rest.api.Validated;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the information to create/update an User
 */
@JsonDeserialize(builder = UserForm.Builder.class)
public final class UserForm extends Validated implements LanguageSupport  {

    private String userId;
    private final boolean active;
    @NotNull
    @NotBlank
    private final String firstName;
    private final String middleName;
    @NotNull
    @NotBlank
    private final String lastName;
    private final String nickName;
    @NotNull
    @NotBlank
    private final String email;
    private final boolean male;
    private final String  birthday;
    private final String    languageId;
    private final String  timeZoneId;
    private final char[] password;

    private final Map<String, Object> additionalInfo;

    private final List<String> roles;

    private UserForm(UserForm.Builder builder) {

        this.active = builder.active;
        this.firstName = builder.firstName;
        this.middleName = builder.middleName;
        this.lastName = builder.lastName;
        this.nickName = builder.nickName;
        this.email = builder.email;
        this.male = builder.male;
        this.birthday = builder.birthday;
        this.languageId = builder.languageId;
        this.timeZoneId = builder.timeZoneId;
        this.password = builder.password;
        this.additionalInfo = builder.additionalInfo;
        this.roles = UtilMethods.isSet(builder.roles)?builder.roles: Collections.emptyList();
        this.userId = builder.userId;

        checkValid();
        if (!UtilMethods.isSet(this.password)) {
            throw new IllegalArgumentException("Password can not be null");
        }
    }

    public String getUserId() {
        return userId;
    }

    public boolean isActive() {
        return active;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getNickName() {
        return nickName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isMale() {
        return male;
    }

    public String getBirthday() {
        return birthday;
    }

    public String getLanguageId() {
        return languageId;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public char[] getPassword() {
        return password;
    }

    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    public List<String> getRoles() {
        return roles;
    }

    public static final class Builder {
        @JsonProperty private String userId;
        @JsonProperty private boolean active;
        @JsonProperty private String firstName;
        @JsonProperty private String middleName;
        @JsonProperty private String lastName;
        @JsonProperty private String nickName;
        @JsonProperty private String email;
        @JsonProperty private boolean male;
        @JsonProperty private String  birthday;
        @JsonProperty private String    languageId ="en-US";
        @JsonProperty private String    timeZoneId;
        @JsonProperty private char[]    password;
        @JsonProperty private Map<String, Object>    additionalInfo;

        @JsonProperty private List<String> roles;
        public Builder() {
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        public Builder roles(List<String> roles) {
            this.roles = roles;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder middleName(String middleName) {
            this.middleName = middleName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder nickName(String nickName) {
            this.nickName = nickName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder male(boolean male) {
            this.male = male;
            return this;
        }

        public Builder birthday(String birthday) {
            this.birthday = birthday;
            return this;
        }

        public Builder languageId(String languageId) {
            this.languageId = languageId;
            return this;
        }

        public Builder timeZoneId(String timeZoneId) {
            this.timeZoneId = timeZoneId;
            return this;
        }

        public Builder password(char[] password) {
            this.password = password;
            return this;
        }

        public Builder additionalInfo(Map<String, Object>    additionalInfo) {
            this.additionalInfo = additionalInfo;
            return this;
        }

        public UserForm build() {
            return new UserForm(this);
        }
    }
}

