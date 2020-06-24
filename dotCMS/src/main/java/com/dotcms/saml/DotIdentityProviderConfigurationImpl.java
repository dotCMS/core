package com.dotcms.saml;

import com.dotcms.saml.service.external.IdentityProviderConfiguration;
import com.dotcms.saml.service.external.SamlName;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import io.vavr.control.Try;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Default implementation to retrieve the configuration from apps
 * @author jsanca
 */
public class DotIdentityProviderConfigurationImpl implements IdentityProviderConfiguration {

    private final AppsAPI appsAPI;
    private final Host    host;

    public DotIdentityProviderConfigurationImpl(final AppsAPI appsAPI, final Host host) {

        this.appsAPI = appsAPI;
        this.host    = host;

    }

    private final Optional<Secret> findSecret (final String key) {

        final Optional<AppSecrets> appSecretOpt =
                Try.of(()->this.appsAPI.getSecrets(DotSamlConfigurationServiceImpl.DOT_SAML_DEFAULT_PROPERTIES_CONTEXT_MAP_KEY,
                        true, host, APILocator.systemUser())).getOrElseGet(e -> Optional.empty());

        return appSecretOpt.isPresent() && appSecretOpt.get().getSecrets().containsKey(key)?
                Optional.of(appSecretOpt.get().getSecrets().get(key)): Optional.empty();
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
        return secretOpt.isPresent()? secretOpt.get().getString(): null;
    }

    @Override
    public Path getIdPMetadataFile() {

        /*final String idPMetadataFileKey = SamlName.DOT_SAML_IDENTITY_PROVIDER_METADATA_FILE.getPropertyName();
        final Optional<Secret> secretOpt        = this.findSecret(idPMetadataFileKey);
        return secretOpt.isPresent()? secretOpt.get().getString(): null;*/
        return null; // todo: this is not being yet supported
    }

    @Override
    public File getPublicCert() {

        /*final String privateKey = SamlName.DOT_SAML_PUBLIC_CERT_FILE.getPropertyName();
        final Optional<Secret> secretOpt        = this.findSecret(privateKey);
        return secretOpt.isPresent()? secretOpt.get().getString(): null;*/
        return null; // todo: this is not being yet supported
    }

    @Override
    public File getPrivateKey() {
        /*final String publicCertKey = SamlName.DOT_SAML_PRIVATE_KEY_FILE.getPropertyName();
        final Optional<Secret> secretOpt        = this.findSecret(publicCertKey);
        return secretOpt.isPresent()? secretOpt.get().getString(): null;*/
        return null; // todo: this is not being yet supported
    }

    @Override
    public Object getOptionalProperty(final String propertyKey) {

        final Optional<Secret> secretOpt = this.findSecret(propertyKey);
        return secretOpt.isPresent()? secretOpt.get().getString(): null;
    }


    @Override
    public boolean containsOptionalProperty(final String propertyKey) {

        return this.findSecret(propertyKey).isPresent();
    }
}
