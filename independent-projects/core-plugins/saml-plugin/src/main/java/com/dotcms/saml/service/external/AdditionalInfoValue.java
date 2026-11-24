package com.dotcms.saml.service.external;

/**
 * Keeps the aliaskey and the type of the additionalInfo mapping
 * @author jsanca
 */
public class AdditionalInfoValue {

    private final String aliasKey;
    private final AdditionalInformationType type;

    public AdditionalInfoValue(final String aliasKey,
                               final AdditionalInformationType type) {
        this.aliasKey = aliasKey;
        this.type = type;
    }

    public String getAliasKey() {
        return aliasKey;
    }

    public AdditionalInformationType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "AdditionalInfoValue{" +
                "aliasKey='" + aliasKey + '\'' +
                ", type=" + type +
                '}';
    }
}
