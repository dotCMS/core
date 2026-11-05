package com.dotcms.saml.service.handler;

import com.dotcms.saml.DotSamlConstants;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.MessageObserver;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlName;
import com.dotcms.saml.service.internal.SamlCoreService;
import org.apache.velocity.app.VelocityEngine;

import java.io.Serializable;

/**
 * A factory for the {@link LogoutHandler}
 * 
 * @author jsanca
 */
public class LogoutResolverHandlerFactory implements Serializable {

	private final SamlConfigurationService samlConfigurationService;
	private final SamlCoreService          samlCoreService;
	private final VelocityEngine           velocityEngine;
	private final MessageObserver          messageObserver;


	public LogoutResolverHandlerFactory(final SamlConfigurationService samlConfigurationService,
                                        final SamlCoreService samlCoreService,
                                        final VelocityEngine           velocityEngine,
                                        final MessageObserver          messageObserver) {

		this.samlConfigurationService = samlConfigurationService;
		this.samlCoreService          = samlCoreService;
		this.velocityEngine           = velocityEngine;
		this.messageObserver		  = messageObserver;
	}

	/**
	 * Get the resolver assertion depending on the site.
	 *
	 * @param identityProviderConfiguration
	 *            {@link IdentityProviderConfiguration}
	 * @return
	 */
	public LogoutHandler getLogoutHandlerForSite(final IdentityProviderConfiguration identityProviderConfiguration) {

		final String logoutProtocolBinding = identityProviderConfiguration.containsOptionalProperty("logout.protocol.binding")?
				identityProviderConfiguration.getOptionalProperty("logout.protocol.binding").toString(): DotSamlConstants.DOTCMS_SAML_AUTHN_PROTOCOL_BINDING_REDIRECT;

		switch (logoutProtocolBinding) {

			case DotSamlConstants.DOTCMS_SAML_AUTHN_PROTOCOL_BINDING_REDIRECT:
				return new HttpRedirectLogoutHandler(this.samlCoreService, this.messageObserver, this.samlConfigurationService);
			case DotSamlConstants.DOTCMS_SAML_AUTHN_PROTOCOL_BINDING_POST:
				return new HttpPOSTLogoutHandler(this.samlCoreService, this.velocityEngine, this.messageObserver);
			case "Http-Okta":
				return new HttpOktaLogoutHandler(this.samlCoreService, this.velocityEngine, this.messageObserver);
		}

		return new HttpRedirectLogoutHandler(this.samlCoreService, this.messageObserver, this.samlConfigurationService);
	}
}
