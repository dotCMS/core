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
 * A factory for the {@link AuthenticationHandler}
 * By default it uses the Redirect AuthenticationHandler, by configuration can be also used POST or POST_RAW
 * @author jsanca
 */
public class AuthenticationResolverHandlerFactory implements Serializable {

	private final SamlConfigurationService samlConfigurationService;
	private final SamlCoreService          samlCoreService;
	private final VelocityEngine           velocityEngine;
	private final MessageObserver          messageObserver;


	public AuthenticationResolverHandlerFactory(final SamlConfigurationService samlConfigurationService,
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
	 * @param idpConfig
	 *            {@link IdentityProviderConfiguration}
	 * @return
	 */
	public AuthenticationHandler getAuthenticationHandlerForSite(final IdentityProviderConfiguration idpConfig) {

		final String authenticationProtocolBinding = this.samlConfigurationService.getConfigAsString(idpConfig,
				SamlName.DOTCMS_SAML_AUTHN_PROTOCOL_BINDING, ()->DotSamlConstants.DOTCMS_SAML_AUTHN_PROTOCOL_BINDING_REDIRECT);

		this.messageObserver.updateInfo(this.getClass().getName(), "Using authentication handler: " + authenticationProtocolBinding);
		switch (authenticationProtocolBinding) {

			case DotSamlConstants.DOTCMS_SAML_AUTHN_PROTOCOL_BINDING_REDIRECT:
				return new HttpRedirectAuthenticationHandler(this.samlCoreService, this.messageObserver, this.samlConfigurationService);
			case DotSamlConstants.DOTCMS_SAML_AUTHN_PROTOCOL_BINDING_POST:
				return new HttpPOSTAuthenticationHandler(this.samlCoreService, this.velocityEngine, this.messageObserver);
			case "Http-POST-Raw":
				return new HttpPOSTRawAuthenticationHandler(this.samlCoreService, this.messageObserver);
		}

		return new HttpRedirectAuthenticationHandler(this.samlCoreService, this.messageObserver, this.samlConfigurationService);
	}
}
