package com.dotcms.saml;

import com.dotcms.saml.service.external.IdentityProviderConfiguration;
import com.dotcms.saml.service.external.SamlConfigurationService;
import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.service.external.SamlName;
import com.dotmarketing.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This service will retrieve information form the idp config, but also if the value is not set will provide the default value for the saml name.
 * It has different implementation for several kinds of values
 * @author jsanca
 */
public class SamlConfigurationServiceImpl implements SamlConfigurationService {

    private static final String UNABLE_TO_READ_FILE = "File does not exist or unable to read : ";

    private AtomicBoolean init = new AtomicBoolean(false);
    /**
     * To set into the init map context, the absolute path of the default properties for SAML.
     */
    public final static String DOT_SAML_DEFAULT_PROPERTIES_CONTEXT_MAP_KEY = "dotSamlDefaultPropertiesContextMapKey";


    @Override
    public void initService(final Map<String, Object> contextMap) {

        if (!this.init.get()) {

            this.internalInit(contextMap);
        }
    }

    private synchronized void internalInit(final Map<String, Object> contextMap) {

        final String dotSamlDefaultPropertiesValue = (String)contextMap.get(DOT_SAML_DEFAULT_PROPERTIES_CONTEXT_MAP_KEY);

        if (null == dotSamlDefaultPropertiesValue) {

            Logger.warn(this, DOT_SAML_DEFAULT_PROPERTIES_CONTEXT_MAP_KEY + " must be set on the argument context map");
        } else {

            final File dotSamlDefaultPropertiesFile = new File(dotSamlDefaultPropertiesValue);

            if (!dotSamlDefaultPropertiesFile.exists() || !dotSamlDefaultPropertiesFile.canRead()) {

                Logger.warn(this, "The " + dotSamlDefaultPropertiesValue + " does not exists or can not read");
            } else {

                final Properties properties = new Properties();

                try (InputStream input = Files.newInputStream(dotSamlDefaultPropertiesFile.toPath())) {

                    properties.load(input);

                } catch (IOException ex) {
                    // Since this is optional, it is valid to not have the file.
                    // Log and go on.
                    Logger.warn(this, UNABLE_TO_READ_FILE + dotSamlDefaultPropertiesValue);

                }

                this.updateDefaultParameters(properties);
                this.init.set(true);
            }
        }
    }

    private void updateDefaultParameters(final Properties properties) {

        if (properties != null) {

            properties.forEach((key, value) -> {

                updateDefaultParameter(SamlName.findProperty((String) key), (String) value);
            });
        }
    }

    public  void updateDefaultParameter(final SamlName property, final String value) {

        if (property == null) {

            Logger.warn(this, "Couldn't do the 'updateDefaultParameter' because property is null");
            return;
        }

        if (value == null) {
            Logger.warn(this, "Couldn't do the 'updateDefaultParameter' because property value for: " + property + " is null");
            return;
        }

        Logger.info(DotsamlDefaultPropertiesService.class, "Updating default property '"
                + property.getPropertyName() + "' to '" + value + "'");

        switch (property) {

            case DOTCMS_SAML_ASSERTION_RESOLVER_HANDLER_CLASS_NAME:
                defaultParams.setDotcmsSamlAssertionResolverHandlerClassName(value);
                break;
            case DOTCMS_SAML_AUTHN_COMPARISON_TYPE:
                defaultParams.setDotcmsSamlAuthnComparisonType(value);
                break;
            case DOTCMS_SAML_AUTHN_CONTEXT_CLASS_REF:
                defaultParams.setDotcmsSamlAuthnContextClassRef(value);
                break;
            case DOTCMS_SAML_BINDING_TYPE:
                defaultParams.setDotcmsSamlBindingType(value);
                break;
            case DOTCMS_SAML_BUILD_ROLES:
                defaultParams.setDotcmsSamlBuildRoles(value);
                break;
            case DOTCMS_SAML_FORCE_AUTHN:
                defaultParams.setDotcmsSamlForceAuthn(Boolean.parseBoolean(value));
                break;
            case DOTCMS_SAML_IDENTITY_PROVIDER_DESTINATION_SLO_URL:
                defaultParams.setDotcmsSamlIdentityProviderDestinationSloUrl(value);
                break;
            case DOTCMS_SAML_IDENTITY_PROVIDER_DESTINATION_SSO_URL:
                defaultParams.setDotcmsSamlIdentityProviderDestinationSsoUrl(value);
                break;
            case DOTCMS_SAML_INCLUDE_ROLES_PATTERN:
                defaultParams.setDotcmsSamlIncludeRolesPattern(value);
                break;
            case DOTCMS_SAML_IS_ASSERTION_ENCRYPTED:
                defaultParams.setDotcmsSamlIsAssertionEncrypted(Boolean.parseBoolean(value));
                break;
            case DOTCMS_SAML_IS_LOGOUT_NEED:
                defaultParams.setDotcmsSamlIsLogoutNeeded(Boolean.parseBoolean(value));
                break;
            case DOTCMS_SAML_NAME_ID_POLICY_FORMAT:
                defaultParams.setDotcmsSamlNameIdPolicyFormat(value);
                break;
            case DOTCMS_SAML_OPTIONAL_USER_ROLE:
                defaultParams.setDotcmsSamlOptionalUserRole(value);
                break;
            case DOTCMS_SAML_POLICY_ALLOW_CREATE:
                defaultParams.setDotcmsSamlPolicyAllowCreate(Boolean.parseBoolean(value));
                break;
            case DOTCMS_SAML_PROTOCOL_BINDING:
                defaultParams.setDotcmsSamlProtocolBinding(value);
                break;
            case DOTCMS_SAML_USE_ENCRYPTED_DESCRIPTOR:
                defaultParams.setDotcmsSamlUseEncryptedDescriptor(Boolean.parseBoolean(value));
                break;
            case DOT_SAML_ACCESS_FILTER_VALUES:
                defaultParams.setDotSamlAccessFilterValues(value);
                break;
            case DOT_SAML_CLOCK_SKEW:
                try {
                    defaultParams.setDotSamlClockSkew(Integer.parseInt(value));
                } catch (Exception ex) {
                    Logger.warn(DotsamlDefaultPropertiesService.class,
                            INTEGER_PARSE_ERROR + property.getPropertyName() + ":" + value);
                }
                break;
            case DOT_SAML_EMAIL_ATTRIBUTE:
                defaultParams.setDotSamlEmailAttribute(value);
                break;
            case DOT_SAML_EMAIL_ATTRIBUTE_ALLOW_NULL:
                defaultParams.setDotSamlEmailAttributeNullValue(Boolean.parseBoolean(value));
                break;
            case DOT_SAML_FIRSTNAME_ATTRIBUTE:
                defaultParams.setDotSamlFirstnameAttribute(value);
                break;
            case DOT_SAML_FIRSTNAME_ATTRIBUTE_NULL_VALUE:
                defaultParams.setDotSamlFirstnameAttributeNullValue(value);
                break;
            case DOT_SAML_IDP_METADATA_PARSER_CLASS_NAME:
                defaultParams.setDotSamlIdpMetadataParserClassName(value);
                break;
            case DOT_SAML_IDP_METADATA_PROTOCOL:
                defaultParams.setDotSamlIdpMetadataProtocol(value);
                break;
            case DOT_SAML_ID_PROVIDER_CUSTOM_CREDENTIAL_PROVIDER_CLASSNAME:
                defaultParams.setDotSamlIdProviderCustomCredentialProviderClassname(value);
                break;
            case DOT_SAML_INCLUDE_PATH_VALUES:
                defaultParams.setDotSamlIncludePathValues(value);
                break;
            case DOT_SAML_LASTNAME_ATTRIBUTE:
                defaultParams.setDotSamlLastnameAttribute(value);
                break;
            case DOT_SAML_LASTNAME_ATTRIBUTE_NULL_VALUE:
                defaultParams.setDotSamlLastnameAttributeNullValue(value);
                break;
            case DOT_SAML_LOGOUT_PATH_VALUES:
                defaultParams.setDotSamlLogoutPathValues(value);
                break;
            case DOT_SAML_LOGOUT_SERVICE_ENDPOINT_URL:
                defaultParams.setDotSamlLogoutServiceEndpointUrl(value);
                break;
            case DOT_SAML_MESSAGE_LIFE_TIME:
                try {
                    defaultParams.setDotSamlMessageLifeTime(Integer.parseInt(value));
                } catch (Exception ex) {
                    Logger.warn(DotsamlDefaultPropertiesService.class,
                            INTEGER_PARSE_ERROR + property.getPropertyName() + ":" + value);
                }
                break;
            case DOT_SAML_REMOVE_ROLES_PREFIX:
                defaultParams.setDotSamlRemoveRolesPrefix(value);
                break;
            case DOT_SAML_ROLES_ATTRIBUTE:
                defaultParams.setDotSamlRolesAttribute(value);
                break;
            case DOT_SAML_SERVICE_PROVIDER_CUSTOM_CREDENTIAL_PROVIDER_CLASSNAME:
                defaultParams.setDotSamlServiceProviderCustomCredentialProviderClassname(value);
                break;
            case DOT_SAML_VERIFY_SIGNATURE_CREDENTIALS:
                defaultParams.setDotSamlVerifySignatureCredentials(Boolean.parseBoolean(value));
                break;
            case DOT_SAML_VERIFY_SIGNATURE_PROFILE:
                defaultParams.setDotSamlVerifySignatureProfile(Boolean.parseBoolean(value));
                break;
            case DOTCMS_SAML_CLEAR_LOCATION_QUERY_PARAMS:
                defaultParams.setDotcmsSamlClearLocationQueryParams(Boolean.parseBoolean(value));
                break;
            case DOTCMS_SAML_LOGIN_UPDATE_EMAIL:
                defaultParams.setDotcmsSamlLoginEmailUpdate(Boolean.parseBoolean(value));
                break;
            case DOT_SAML_ALLOW_USER_SYNCHRONIZATION:
                defaultParams.setAllowUserSynchronization(Boolean.parseBoolean(value));
                break;
            default:
                Logger.warn(DotsamlDefaultPropertiesService.class,
                        NOT_FOUND_ERROR + property.getPropertyName() + ":" + value);
                break;
        }
    }

    @Override
    public String getConfigAsString(final IdentityProviderConfiguration identityProviderConfiguration, final SamlName samlName) {

        try {

            final String value = identityProviderConfiguration.getOptionalProperties().containsKey(samlName.getPropertyName())?
                    (String) identityProviderConfiguration.getOptionalProperties().get(samlName.getPropertyName()):
                    this.getDefaultStringParameter(samlName);

            Logger.debug(this,
                    ()-> "Found " + samlName.getPropertyName() + " : " + ((value == null) ? "null" : value));

            return value;
        } catch (Exception e) {

            Logger.warn(this, ()-> "Cast exception on " + samlName.getPropertyName()
                    + " property. idpConfigId: " + identityProviderConfiguration.getId());
        }

        return null;
    }

    @Override
    public Boolean getConfigAsBoolean(final IdentityProviderConfiguration identityProviderConfiguration, final SamlName samlName) {
        return null;
    }

    @Override
    public String[] getConfigAsArrayString(final IdentityProviderConfiguration identityProviderConfiguration, final SamlName samlName) {
        return new String[0];
    }

    @Override
    public Integer getConfigAsInteger(final IdentityProviderConfiguration identityProviderConfiguration, final SamlName samlName) {
        return null;
    }



    ////////
    public  boolean getDefaultBooleanParameter(final SamlName samlName) {

        if (samlName == null) {

            throw new SamlException("The 'getDefaultBooleanParameter' property is null");
        }

        switch (samlName) {

            case DOTCMS_SAML_FORCE_AUTHN:
                return defaultParams.isDotcmsSamlForceAuthn();
            case DOTCMS_SAML_IS_ASSERTION_ENCRYPTED:
                return defaultParams.isDotcmsSamlIsAssertionEncrypted();
            case DOTCMS_SAML_IS_LOGOUT_NEED:
                return defaultParams.isDotcmsSamlIsLogoutNeeded();
            case DOTCMS_SAML_POLICY_ALLOW_CREATE:
                return defaultParams.isDotcmsSamlPolicyAllowCreate();
            case DOTCMS_SAML_USE_ENCRYPTED_DESCRIPTOR:
                return defaultParams.isDotcmsSamlUseEncryptedDescriptor();
            case DOT_SAML_EMAIL_ATTRIBUTE_ALLOW_NULL:
                return defaultParams.isDotSamlEmailAttributeNullValue();
            case DOT_SAML_VERIFY_SIGNATURE_CREDENTIALS:
                return defaultParams.isDotSamlVerifySignatureCredentials();
            case DOT_SAML_VERIFY_SIGNATURE_PROFILE:
                return defaultParams.isDotSamlVerifySignatureProfile();
            case DOTCMS_SAML_CLEAR_LOCATION_QUERY_PARAMS:
                return defaultParams.isDotcmsSamlClearLocationQueryParams();
            case DOTCMS_SAML_LOGIN_UPDATE_EMAIL:
                return defaultParams.isDotcmsSamlLoginEmailUpdate();
            case DOT_SAML_ALLOW_USER_SYNCHRONIZATION:
                return defaultParams.isAllowUserSynchronization();
            default:
                break;
        }

        throw new DotSamlException(NOT_FOUND_ERROR + samlName.getPropertyName());
    }
}
