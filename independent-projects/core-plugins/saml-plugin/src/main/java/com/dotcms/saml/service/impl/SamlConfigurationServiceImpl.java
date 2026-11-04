package com.dotcms.saml.service.impl;

import com.dotcms.saml.DotAbstractSamlConfigurationServiceImpl;
import com.dotcms.saml.SamlName;
import com.dotcms.saml.service.external.BindingType;
import com.dotcms.saml.service.external.SamlConstants;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.NameIDType;

import java.util.HashMap;
import java.util.Map;

/**
 * This implementation only provides the very initial values for the default values on the client implementation
 * @author jsanca
 */
public class SamlConfigurationServiceImpl extends DotAbstractSamlConfigurationServiceImpl {

    @Override
    public Map<String, String> createInitialMap() {
        final Map<String, String> map = new HashMap<>();

        map.put(SamlName.DOT_SAML_ENABLE.getPropertyName(),                        "true");
        map.put(SamlName.DOT_SAML_ACCESS_FILTER_VALUES.getPropertyName(),          "/dotAdmin/show-logout,/dotAdmin/logout");
        map.put(SamlName.DOTCMS_SAML_AUTHN_COMPARISON_TYPE.getPropertyName(),      null);
        map.put(SamlName.DOTCMS_SAML_AUTHN_CONTEXT_CLASS_REF.getPropertyName(),    AuthnContext.PASSWORD_AUTHN_CTX);
        map.put(SamlName.DOTCMS_SAML_BINDING_TYPE.getPropertyName(),               BindingType.REDIRECT.getBinding());
        map.put(SamlName.DOTCMS_SAML_BUILD_ROLES.getPropertyName(),                SamlConstants.DOTCMS_SAML_BUILD_ROLES_ALL_VALUE);
        map.put(SamlName.DOT_SAML_CLOCK_SKEW.getPropertyName(),                    "10000");
        map.put(SamlName.DOT_SAML_EMAIL_ATTRIBUTE.getPropertyName(),               "mail");
        map.put(SamlName.DOT_SAML_EMAIL_ATTRIBUTE_ALLOW_NULL.getPropertyName(),    "true");
        map.put(SamlName.DOT_SAML_FIRSTNAME_ATTRIBUTE.getPropertyName(),           "givenName");
        map.put(SamlName.DOT_SAML_FIRSTNAME_ATTRIBUTE_NULL_VALUE.getPropertyName(), null);
        map.put(SamlName.DOT_SAML_LASTNAME_ATTRIBUTE.getPropertyName(),             "sn");
        map.put(SamlName.DOT_SAML_LASTNAME_ATTRIBUTE_NULL_VALUE.getPropertyName(),  null);
        map.put(SamlName.DOT_SAML_ROLES_ATTRIBUTE.getPropertyName(),                "authorizations");
        map.put(SamlName.DOTCMS_SAML_IS_LOGOUT_NEED.getPropertyName(),              "true");
        map.put(SamlName.DOT_SAML_LOGOUT_SERVICE_ENDPOINT_URL.getPropertyName(),    null);
        map.put(SamlName.DOT_SAML_IDP_METADATA_PARSER_CLASS_NAME.getPropertyName(), null);
        map.put(SamlName.DOT_SAML_IDP_METADATA_PROTOCOL.getPropertyName(),          SamlConstants.DOT_SAML_IDP_METADATA_PROTOCOL_DEFAULT_VALUE);
        map.put(SamlName.DOTCMS_SAML_IS_ASSERTION_ENCRYPTED.getPropertyName(),      "false");
        map.put(SamlName.DOT_SAML_MESSAGE_LIFE_TIME.getPropertyName(),              "20000");
        map.put(SamlName.DOTCMS_SAML_FORCE_AUTHN.getPropertyName(),                 "false");
        map.put(SamlName.DOTCMS_SAML_OPTIONAL_USER_ROLE.getPropertyName(),          null);
        map.put(SamlName.DOTCMS_SAML_POLICY_ALLOW_CREATE.getPropertyName(),         "false");
        map.put(SamlName.DOT_SAML_INCLUDE_PATH_VALUES.getPropertyName(),            SamlConstants.DOT_SAML_INCLUDE_PATH_DEFAULT_VALUES);
        map.put(SamlName.DOT_SAML_LOGOUT_PATH_VALUES.getPropertyName(),             SamlConstants.DOT_SAML_LOGOUT_PATH_DEFAULT_VALUES);
        map.put(SamlName.DOTCMS_SAML_NAME_ID_POLICY_FORMAT.getPropertyName(),       NameIDType.PERSISTENT);
        map.put(SamlName.DOTCMS_SAML_PROTOCOL_BINDING.getPropertyName(),            SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        map.put(SamlName.DOT_SAML_REMOVE_ROLES_PREFIX.getPropertyName(),            StringUtils.EMPTY);
        map.put(SamlName.DOTCMS_SAML_USE_ENCRYPTED_DESCRIPTOR.getPropertyName(),    "false");
        map.put(SamlName.DOT_SAML_VERIFY_SIGNATURE_CREDENTIALS.getPropertyName(),   "true");
        map.put(SamlName.DOT_SAML_VERIFY_SIGNATURE_PROFILE.getPropertyName(),       "true");
        map.put(SamlName.DOTCMS_SAML_CLEAR_LOCATION_QUERY_PARAMS.getPropertyName(), "true");
        map.put(SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL.getPropertyName(),          "true");
        map.put(SamlName.DOT_SAML_ALLOW_USER_SYNCHRONIZATION.getPropertyName(),     "true");
        map.put(SamlName.DOT_SAML_SERVICE_PROVIDER_CUSTOM_CREDENTIAL_PROVIDER_CLASSNAME.getPropertyName(), null);
        map.put(SamlName.DOTCMS_SAML_OPTIONAL_USER_ROLE.getPropertyName(),                                 null);
        map.put(SamlName.DOTCMS_SAML_INCLUDE_ROLES_PATTERN.getPropertyName(),                              null);
        map.put(SamlName.DOTCMS_SAML_ASSERTION_RESOLVER_HANDLER_CLASS_NAME.getPropertyName(),              null);
        map.put(SamlName.DOT_SAML_ID_PROVIDER_CUSTOM_CREDENTIAL_PROVIDER_CLASSNAME.getPropertyName(),      null);
        map.put(SamlName.DOTCMS_SAML_IDENTITY_PROVIDER_DESTINATION_SLO_URL.getPropertyName(),              null);
        map.put(SamlName.DOTCMS_SAML_IDENTITY_PROVIDER_DESTINATION_SSO_URL.getPropertyName(),              null);


        return map;
    }
}
