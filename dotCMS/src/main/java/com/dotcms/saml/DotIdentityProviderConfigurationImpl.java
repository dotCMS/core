package com.dotcms.saml;

import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation to retrieve the configuration from apps
 * @author jsanca
 */
public class DotIdentityProviderConfigurationImpl implements IdentityProviderConfiguration {

    private static final Set<String> SIGNATURE_VALIDATION_TYPES = Set.of(
            DotSamlConstants.RESPONSE, DotSamlConstants.ASSERTION, DotSamlConstants.RESPONSE_AND_ASSERTION);

    /**
     * Extra parameters that used to switch off parts of the signature verification. Stored values are
     * ignored, so the SAML bundle always uses its default (true).
     */
    private static final Set<String> IGNORED_PROPERTIES = Set.of(
            SamlName.DOT_SAML_VERIFY_SIGNATURE_CREDENTIALS.getPropertyName(),
            SamlName.DOT_SAML_VERIFY_SIGNATURE_PROFILE.getPropertyName());

    private final AppsAPI    appsAPI;
    private final Host       host;
    private final AppSecrets appSecrets;

    public DotIdentityProviderConfigurationImpl(final AppsAPI appsAPI, final Host host) throws DotSecurityException, DotDataException {

        this.appsAPI    = appsAPI;
        this.host       = host;
        this.appSecrets = this.appsAPI.getSecrets(DotSamlProxyFactory.SAML_APP_CONFIG_KEY,
                true, host, APILocator.systemUser()).get();

        this.warnAboutOverriddenSettings();
    }

    /**
     * Returns the signature validation type when it is "response", "assertion" or "responseandassertion"
     * (ignoring case and surrounding blanks). Anything else, including blank and "none", fails closed to
     * "responseandassertion": signatures can not be switched off through this setting.
     *
     * @param configured the stored value, may be null
     * @return one of the supported validation types
     */
    public static String resolveSignatureValidationType(final String configured) {

        final String normalized = null == configured ? "" : configured.trim().toLowerCase(Locale.ROOT);
        return SIGNATURE_VALIDATION_TYPES.contains(normalized) ? normalized : DotSamlConstants.RESPONSE_AND_ASSERTION;
    }

    private void warnAboutOverriddenSettings() {

        final Optional<Secret> validationType =
                this.findSecret(SamlName.DOT_SAML_SIGNATURE_VALIDATION_TYPE.getPropertyName());
        final String configured = validationType.map(Secret::getString).orElse(null);
        if (null == configured || !SIGNATURE_VALIDATION_TYPES.contains(configured.trim().toLowerCase(Locale.ROOT))) {

            Logger.warn(this, "SAML configuration for site '" + this.host.getHostname() + "' has an unsupported "
                    + "signature validation type '" + configured + "'. A signed response and a signed assertion "
                    + "are required until a supported Validation Type is saved.");
        }

        for (final String ignoredProperty : IGNORED_PROPERTIES) {

            if (this.findSecret(ignoredProperty).isPresent()) {

                Logger.warn(this, "SAML configuration for site '" + this.host.getHostname() + "' sets '"
                        + ignoredProperty + "', which is ignored: SAML signatures are always verified.");
            }
        }
    }

    private Optional<Secret> findSecret (final String key) {

        return this.appSecrets.getSecrets().containsKey(key)?
                Optional.ofNullable(this.appSecrets.getSecrets().get(key)) : Optional.empty();
    }

    @Override
    public boolean isEnabled() {

        final String enableKey           = SamlName.DOT_SAML_ENABLE.getPropertyName();
        final Optional<Secret> secretOpt = this.findSecret(enableKey);
        return secretOpt.isPresent()? secretOpt.get().getBoolean(): false;
    }

    @Override
    public String getSpIssuerURL() {

        final String sPIssuerURLKey      = SamlName.DOT_SAML_SERVICE_PROVIDER_ISSUER_URL.getPropertyName();
        final Optional<Secret> secretOpt = this.findSecret(sPIssuerURLKey);
        return secretOpt.isPresent()? secretOpt.get().getString(): null;
    }

    @Override
    public String getIdpName() {

        final String sPIssuerURLKey      = SamlName.DOT_SAML_IDENTITY_PROVIDER_NAME.getPropertyName();
        final Optional<Secret> secretOpt = this.findSecret(sPIssuerURLKey);
        return secretOpt.isPresent()? secretOpt.get().getString(): null;
    }

    @Override
    public String getId() {

        return this.host.getIdentifier();
    }

    @Override
    public String getSpEndpointHostname() {

        final String sPEndpointHostnameKey = SamlName.DOT_SAML_SERVICE_PROVIDER_HOST_NAME.getPropertyName();
        final Optional<Secret> secretOpt   = this.findSecret(sPEndpointHostnameKey);
        return secretOpt.isPresent()? secretOpt.get().getString(): null;
    }

    @Override
    public String getSignatureValidationType() {

        final String signatureValidationTypeKey = SamlName.DOT_SAML_SIGNATURE_VALIDATION_TYPE.getPropertyName();
        final Optional<Secret> secretOpt        = this.findSecret(signatureValidationTypeKey);
        return resolveSignatureValidationType(secretOpt.isPresent()? secretOpt.get().getString(): null);
    }

    @Override
    public char[] getIdPMetadataFile() {

        final String idPMetadataFileKey = SamlName.DOT_SAML_IDENTITY_PROVIDER_METADATA_FILE.getPropertyName();
        final Optional<Secret> secretOpt        = this.findSecret(idPMetadataFileKey);
        return secretOpt.isPresent()? secretOpt.get().getValue(): null;
        //return new File("/Users/jsanca/Documents/idp-metadata-example.xml").toPath();
    }

    @Override
    public char[] getPublicCert() {

        final String privateKey = SamlName.DOT_SAML_PUBLIC_CERT_FILE.getPropertyName();
        final Optional<Secret> secretOpt        = this.findSecret(privateKey);
        return secretOpt.isPresent()? secretOpt.get().getValue(): null;

        //return new File("/Users/jsanca/Documents/mysaml.crt");
    }

    @Override
    public char[] getPrivateKey() {
        final String publicCertKey = SamlName.DOT_SAML_PRIVATE_KEY_FILE.getPropertyName();
        final Optional<Secret> secretOpt        = this.findSecret(publicCertKey);
        return secretOpt.isPresent()? secretOpt.get().getValue(): null;
        //return new File("/Users/jsanca/Documents/mysaml.key");
    }

    @Override
    public Object getOptionalProperty(final String propertyKey) {

        if (IGNORED_PROPERTIES.contains(propertyKey)) {
            return null;
        }

        final Optional<Secret> secretOpt = this.findSecret(propertyKey);
        return secretOpt.isPresent()? secretOpt.get().getString(): null;
    }


    @Override
    public boolean containsOptionalProperty(final String propertyKey) {

        return !IGNORED_PROPERTIES.contains(propertyKey) && this.findSecret(propertyKey).isPresent();
    }

    @Override
    public void destroy() {

        this.appSecrets.destroy();
    }
}
