package com.dotcms.rest.api.v1.company;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.liferay.portal.model.Company;

/**
 * Authentication type for company login configuration.
 *
 * @author hassandotcms
 */
public enum AuthType {

    EMAIL_ADDRESS(Company.AUTH_TYPE_EA),
    USER_ID(Company.AUTH_TYPE_ID);

    private final String value;

    AuthType(final String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AuthType fromString(final String value) {
        for (final AuthType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Invalid authType: '" + value + "'. Must be one of: emailAddress, userId");
    }
}
