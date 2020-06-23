package com.dotcms.saml;

import com.dotcms.saml.service.external.IdentityProviderConfiguration;
import com.dotcms.saml.service.external.SamlName;
import com.dotcms.security.apps.AppSecrets;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

/**
 * Default implementation to retrieve the configuration from apps
 * @author jsanca
 */
public class DotIdentityProviderConfigurationImpl implements IdentityProviderConfiguration {

    private final AppSecrets appSecrets;

    public DotIdentityProviderConfigurationImpl(final AppSecrets appSecrets) {

        this.appSecrets = appSecrets;
    }

    @Override
    public boolean isEnabled() {

        final String enableKey = SamlName.DOT_SAML_ENABLE.getPropertyName();
        return this.appSecrets.getSecrets().containsKey(enableKey)?
                this.appSecrets.getSecrets().get(enableKey).getBoolean(): false;
    }

    @Override
    public String getSpIssuerURL() {

        final String enableKey = SamlName.DOT_SAML_ENABLE.getPropertyName();
        return this.appSecrets.getSecrets().containsKey(enableKey)?
                this.appSecrets.getSecrets().get(enableKey).g(): false;
    }

    @Override
    public String getIdpName() {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getSpEndpointHostname() {
        return null;
    }

    @Override
    public String getSignatureValidationType() {
        return null;
    }

    @Override
    public Path getIdPMetadataFile() {
        return null;
    }

    @Override
    public File getPublicCert() {
        return null;
    }

    @Override
    public File getPrivateKey() {
        return null;
    }

    @Override
    public Map<String, Object> getOptionalProperties() {
        return null;
    }
}
