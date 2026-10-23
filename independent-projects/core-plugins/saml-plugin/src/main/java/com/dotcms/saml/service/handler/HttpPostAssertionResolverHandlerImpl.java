package com.dotcms.saml.service.handler;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.MessageObserver;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlName;
import com.dotcms.saml.service.impl.SamlResponseValidator;
import com.dotcms.saml.service.internal.SamlCoreService;
import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.utils.InstanceUtil;
import com.dotcms.saml.utils.SamlUtils;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import org.apache.commons.lang.StringUtils;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.handler.MessageHandler;
import org.opensaml.messaging.handler.impl.BasicMessageHandlerChain;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.binding.security.impl.MessageLifetimeSecurityHandler;
import org.opensaml.saml.common.messaging.context.SAMLMessageInfoContext;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPPostDecoder;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the http post
 *
 * @author jsanca
 */

public class HttpPostAssertionResolverHandlerImpl implements AssertionResolverHandler {

	private static final long serialVersionUID = 3479922364325870009L;

	// This is the key to get the saml response from the request.
	private static final String SAML_RESPONSE_KEY = "SAMLResponse";

	private final MessageObserver messageObserver;
	private final SamlCoreService samlCoreService;
	private final SamlConfigurationService samlConfigurationService;
	private final SamlResponseValidator samlResponseValidator;

	public HttpPostAssertionResolverHandlerImpl(final MessageObserver messageObserver,
												final SamlCoreService samlCoreService,
												final SamlConfigurationService samlConfigurationService) {

		this(messageObserver, samlCoreService, samlConfigurationService,
				InstanceUtil.getInstance(SamlResponseValidator.class));
	}

	public HttpPostAssertionResolverHandlerImpl(final MessageObserver messageObserver,
												final SamlCoreService samlCoreService,
												final SamlConfigurationService samlConfigurationService,
												final SamlResponseValidator samlResponseValidator) {

		this.messageObserver = messageObserver;
		this.samlCoreService = samlCoreService;
		this.samlConfigurationService = samlConfigurationService;
		this.samlResponseValidator = samlResponseValidator;
	}

	@Override
	public boolean isValidSamlRequest(final HttpServletRequest request, final HttpServletResponse response,
									  final IdentityProviderConfiguration identityProviderConfiguration) {

		return StringUtils.isNotBlank(request.getParameter(SAML_RESPONSE_KEY));
	}

	@Override
	public Assertion resolveAssertion(final HttpServletRequest request, final HttpServletResponse response,
									  final IdentityProviderConfiguration identityProviderConfiguration) {

		Assertion assertion     = null;
		HTTPPostDecoder decoder = new HTTPPostDecoder();
		Response samlResponse   = null;
		MessageContext<SAMLObject> messageContext = null;

		this.messageObserver.updateDebug(this.getClass().getName(),
				"Resolving SAML Artifact with AssertionResolverHandler implementation: " + this.getClass().getName());

		try {

			this.messageObserver.updateDebug(this.getClass().getName(),
					"Decoding the Post message: " + request.getParameter(SAML_RESPONSE_KEY));

			decoder.setHttpServletRequest(request);
			decoder.setParserPool(XMLObjectProviderRegistrySupport.getParserPool());

			decoder.initialize();
			decoder.decode();

			messageContext = decoder.getMessageContext();
			if (!(messageContext.getMessage() instanceof Response)) {

				this.messageObserver.updateError(this.getClass().getName(), "The SAML message posted for IdP '"
						+ identityProviderConfiguration.getIdpName() + "' is not a Response");
				throw new SamlException("The SAML message posted to the assertion consumer URL is not a Response");
			}

			samlResponse = (Response) messageContext.getMessage();

			this.messageObserver.updateDebug(this.getClass().getName(),"Post message context decoded:");
			this.messageObserver.updateDebug(this.getClass().getName(),"\n\n" + SamlUtils.toXMLObjectString(samlResponse));

		} catch (ComponentInitializationException | MessageDecodingException e) {

			this.messageObserver.updateError(this.getClass().getName(),
					"Error decoding inbound message context for IdP '" + identityProviderConfiguration.getIdpName() + "'", e);
			throw new SamlException(e.getMessage(), e);
		} finally {

			decoder.destroy();
		}

		if (null == this.samlResponseValidator) {

			// fail closed: never resolve an assertion without the profile checks
			throw new SamlException("The SAML Response validator is not available");
		}

		this.validateMessageLifetime(messageContext, identityProviderConfiguration);

		assertion = this.samlCoreService.getAssertion(samlResponse, identityProviderConfiguration);

		this.messageObserver.updateDebug(this.getClass().getName(), "Decrypted Assertion:");
		this.messageObserver.updateDebug(this.getClass().getName(), "\n\n" + SamlUtils.toXMLObjectString(assertion));

		// Verify Signatures.
		this.samlCoreService.verifyResponseSignature(samlResponse, identityProviderConfiguration);
		this.samlCoreService.verifyAssertionSignature(assertion,   identityProviderConfiguration);

		this.verifyStatus(samlResponse);

		// Destination, Issuer, Audience, Conditions, SubjectConfirmation, InResponseTo and replay.
		this.samlResponseValidator.validate(samlResponse, assertion, request, response, identityProviderConfiguration);

		return assertion;
	}

	private void verifyStatus(final Response response) {

		final Status status         = response.getStatus();
		final StatusCode statusCode = null != status ? status.getStatusCode() : null;
		final String statusCodeURI  = null != statusCode ? statusCode.getValue() : null;

		if (!StatusCode.SUCCESS.equals(statusCodeURI)) {

			this.messageObserver.updateError(this.getClass().getName(),
					"SAML status code was NOT successful: " + statusCodeURI);
			throw new SamlException("SAML status code was NOT successful: " + statusCodeURI);
		}
	}

	/**
	 * Checks the Response IssueInstant against the configured message lifetime. The Destination and the
	 * assertion validity window are checked by {@link SamlResponseValidator}, against the configured assertion
	 * consumer URL rather than the servlet request URL, which differs behind a TLS-terminating proxy and after the
	 * /dotsaml/login to /api/v1/dotsaml/login rewrite.
	 */
	@SuppressWarnings("unchecked")
	private void validateMessageLifetime(final MessageContext<SAMLObject> context,
			final IdentityProviderConfiguration identityProviderConfiguration) {

		// Just setting it to a value in case of exception.
		long clockSkew = DOT_SAML_CLOCK_SKEW_DEFAULT_VALUE;
		long lifeTime = DOT_SAML_MESSAGE_LIFE_DEFAULT_VALUE;

		try {

			final Integer intClockSkew = this.samlConfigurationService.getConfigAsInteger(identityProviderConfiguration,
					SamlName.DOT_SAML_CLOCK_SKEW);
			if (intClockSkew != null) {

				clockSkew = new Long(intClockSkew).longValue();
			}
		} catch (Exception exception) {

			this.messageObserver.updateInfo(this.getClass().getName(),
					"Optional property not set: " + SamlName.DOT_SAML_CLOCK_SKEW + ". Using default.");
		}

		try {

			final Integer intLifeTime = this.samlConfigurationService.getConfigAsInteger(identityProviderConfiguration,
					SamlName.DOT_SAML_MESSAGE_LIFE_TIME);
			if (intLifeTime != null) {

				lifeTime = new Long(intLifeTime).longValue();
			}

		} catch (Exception exception) {

			this.messageObserver.updateInfo(this.getClass().getName(),
					"Optional property not set: " + SamlName.DOT_SAML_MESSAGE_LIFE_TIME.getPropertyName() + ". Using default.");
		}

		final SAMLMessageInfoContext messageInfoContext = context.getSubcontext(SAMLMessageInfoContext.class, true);
		final MessageLifetimeSecurityHandler lifetimeSecurityHandler = new MessageLifetimeSecurityHandler();
		final BasicMessageHandlerChain<SAMLObject> handlerChain      = new BasicMessageHandlerChain<SAMLObject>();
		final List<MessageHandler<SAMLObject>> handlers              = new ArrayList<>();
		final Response response = (Response) context.getMessage();
		
		messageInfoContext.setMessageIssueInstant(response.getIssueInstant());

		// message lifetime validation.
		lifetimeSecurityHandler.setClockSkew(clockSkew);
		lifetimeSecurityHandler.setMessageLifetime(lifeTime);
		lifetimeSecurityHandler.setRequiredRule(true);

		handlers.add(lifetimeSecurityHandler);
		handlerChain.setHandlers(handlers);

		SamlUtils.invokeMessageHandlerChain(handlerChain, context);
	}

}
