package com.dotcms.saml.service.impl;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlName;
import com.dotcms.saml.service.internal.CredentialProvider;
import com.dotcms.saml.service.internal.CredentialService;
import com.dotcms.saml.utils.InstanceUtil;
import com.dotcms.saml.service.external.SamlConstants;
import com.dotmarketing.util.Logger;
import org.apache.commons.lang.StringUtils;

import java.util.Locale;

public class CredentialServiceImpl implements CredentialService {

	private final SamlConfigurationService samlConfigurationService;

	public CredentialServiceImpl(final SamlConfigurationService samlConfigurationService) {

		this.samlConfigurationService = samlConfigurationService;
	}

	/**
	 * In case you need a custom credentials for the ID Provider (dotCMS)
	 * overrides the implementation class on the configuration. By default it
	 * uses the Idp metadata credentials info, from the XML to figure out this
	 * info.
	 *
	 * @param identityProviderConfiguration IdentityProviderConfiguration
	 * @return CredentialProvider
	 */
	@Override
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public CredentialProvider getIdProviderCustomCredentialProvider(final IdentityProviderConfiguration identityProviderConfiguration) {

		final String className = this.samlConfigurationService.getConfigAsString(identityProviderConfiguration,
				SamlName.DOT_SAML_ID_PROVIDER_CUSTOM_CREDENTIAL_PROVIDER_CLASSNAME);
		final Class clazz      = InstanceUtil.getClass(className);

		return null != clazz? (CredentialProvider) InstanceUtil.newInstance(clazz) : null;
	}

	/**
	 * In case you need custom credentials for the Service Provider (DotCMS)
	 * overwrites the implementation class on the configuration. By default it
	 * uses a Trust Storage to get the keys and creates the credential.
	 * 
	 * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
	 * @return CredentialProvider
	 */
	@Override
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public  CredentialProvider getServiceProviderCustomCredentialProvider(final IdentityProviderConfiguration identityProviderConfiguration) {

		final String className = this.samlConfigurationService.getConfigAsString(identityProviderConfiguration,
				SamlName.DOT_SAML_SERVICE_PROVIDER_CUSTOM_CREDENTIAL_PROVIDER_CLASSNAME);
		final Class clazz      = InstanceUtil.getClass(className);

		return null != clazz? (CredentialProvider) InstanceUtil.newInstance(clazz) : null;
	}

	/**
	 * True when the assertion must carry a valid signature: validation type "assertion" or
	 * "responseandassertion", or any value that is not recognized (see {@link #resolveSignatureValidationType}).
	 *
	 * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
	 * @return boolean
	 */
	@Override
	public boolean isVerifyAssertionSignatureNeeded(final IdentityProviderConfiguration identityProviderConfiguration) {

		final String validationType = resolveSignatureValidationType(identityProviderConfiguration);
		return SamlConstants.RESPONSE_AND_ASSERTION.equals(validationType) || SamlConstants.ASSERTION.equals(validationType);
	}

	/**
	 * True when the response must carry a valid signature: validation type "response" or
	 * "responseandassertion", or any value that is not recognized (see {@link #resolveSignatureValidationType}).
	 *
	 * @param identityProviderConfiguration identityProviderConfiguration
	 * @return boolean
	 */
	@Override
	public boolean isVerifyResponseSignatureNeeded(final IdentityProviderConfiguration identityProviderConfiguration) {

		final String validationType = resolveSignatureValidationType(identityProviderConfiguration);
		return SamlConstants.RESPONSE_AND_ASSERTION.equals(validationType) || SamlConstants.RESPONSE.equals(validationType);
	}

	/**
	 * Always true. Cryptographic verification of a signature cannot be turned off; a configured
	 * "false" value is ignored and logged.
	 *
	 * @param identityProviderConfiguration IdentityProviderConfiguration
	 * @return boolean
	 */
	@Override
	public boolean isVerifySignatureCredentialsNeeded(final IdentityProviderConfiguration identityProviderConfiguration) {

		warnIfDisabled(identityProviderConfiguration, SamlName.DOT_SAML_VERIFY_SIGNATURE_CREDENTIALS);
		return true;
	}

	/**
	 * Always true. The signature profile check binds a signature to the element it covers; a configured
	 * "false" value is ignored and logged.
	 *
	 * @param identityProviderConfiguration IdentityProviderConfiguration
	 * @return boolean
	 */
	@Override
	public boolean isVerifySignatureProfileNeeded(final IdentityProviderConfiguration identityProviderConfiguration) {

		warnIfDisabled(identityProviderConfiguration, SamlName.DOT_SAML_VERIFY_SIGNATURE_PROFILE);
		return true;
	}

	/**
	 * Returns the configured signature validation type when it is one of "response", "assertion" or
	 * "responseandassertion". Anything else (blank, "none", a typo) fails closed to "responseandassertion",
	 * so an unrecognized value can never turn signature verification off.
	 *
	 * @param identityProviderConfiguration IdentityProviderConfiguration
	 * @return String one of the {@link SamlConstants} validation types
	 */
	public static String resolveSignatureValidationType(final IdentityProviderConfiguration identityProviderConfiguration) {

		final String configured = identityProviderConfiguration.getSignatureValidationType();
		final String normalized = null == configured ? StringUtils.EMPTY : configured.trim().toLowerCase(Locale.ROOT);
		switch (normalized) {
			case SamlConstants.RESPONSE:
			case SamlConstants.ASSERTION:
			case SamlConstants.RESPONSE_AND_ASSERTION:
				return normalized;
			default:
				Logger.warn(CredentialServiceImpl.class, "Unsupported signature validation type '" + configured
						+ "' for IdP '" + identityProviderConfiguration.getIdpName()
						+ "'. Requiring a signed response and a signed assertion.");
				return SamlConstants.RESPONSE_AND_ASSERTION;
		}
	}

	private void warnIfDisabled(final IdentityProviderConfiguration identityProviderConfiguration, final SamlName samlName) {

		final Boolean configured = this.samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, samlName);
		if (Boolean.FALSE.equals(configured)) {

			Logger.warn(CredentialServiceImpl.class, "Ignoring '" + samlName.getPropertyName() + "=false' for IdP '"
					+ identityProviderConfiguration.getIdpName() + "': SAML signatures are always verified.");
		}
	}
}
