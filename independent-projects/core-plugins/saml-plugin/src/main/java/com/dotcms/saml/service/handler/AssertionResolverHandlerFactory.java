package com.dotcms.saml.service.handler;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.MessageObserver;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlName;
import com.dotcms.saml.utils.InstanceUtil;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A factory for the {@link AssertionResolverHandler}
 * By default we only support POST messages from the IDP, but this factory allows to have more in the future.
 * 
 * @author jsanca
 */

public class AssertionResolverHandlerFactory implements Serializable {

	private static final long serialVersionUID = 2434118681822205248L;

	private final SamlConfigurationService samlConfigurationService;
	private final MessageObserver messageObserver;
	private static final Map<String, AssertionResolverHandler> assertionResolverHandlerInstancesMap = new ConcurrentHashMap<>();

	public AssertionResolverHandlerFactory(final SamlConfigurationService samlConfigurationService,
										   final MessageObserver messageObserver) {
		this.samlConfigurationService = samlConfigurationService;
		this.messageObserver = messageObserver;
	}

	/**
	 * Get the resolver assertion depending on the site.
	 *
	 * @param identityProviderConfiguration
	 *            {@link IdentityProviderConfiguration}
	 * @return AssertionResolverHandler
	 */
	public AssertionResolverHandler getAssertionResolverForSite(final IdentityProviderConfiguration identityProviderConfiguration) {

		String className = null;

		try {

			className = this.samlConfigurationService.getConfigAsString(identityProviderConfiguration,
					SamlName.DOTCMS_SAML_ASSERTION_RESOLVER_HANDLER_CLASS_NAME);
		} catch (Exception exception) {

			this.messageObserver.updateInfo(this.getClass().getName(),
					"Optional property not set: "
							+ SamlName.DOTCMS_SAML_ASSERTION_RESOLVER_HANDLER_CLASS_NAME.getPropertyName()
							+ " for idpConfig: " + identityProviderConfiguration.getId() + " Using default.");
		}

		final AssertionResolverHandler assertionResolverHandler = StringUtils.isBlank(className)?
				this.getDefaultAssertionResolverHandler() : this.getAssertionResolverHandler(className);

		this.messageObserver.updateDebug(this.getClass().getName(),
				"Getting the assertion resolver for the idpConfig: " + identityProviderConfiguration.getId()
				+ ", with the class: " + assertionResolverHandler);

		return assertionResolverHandler;
	}

	private AssertionResolverHandler getDefaultAssertionResolverHandler() {

		return this.getAssertionResolverHandler(HttpPostAssertionResolverHandlerImpl.class.getName());
	}

	private AssertionResolverHandler getAssertionResolverHandler(final String className) {

		if (!assertionResolverHandlerInstancesMap.containsKey(className)) {

			final AssertionResolverHandler assertionResolverHandler =
					(AssertionResolverHandler) InstanceUtil.newInstance(InstanceUtil.getClass(className));

			assertionResolverHandlerInstancesMap.put(className, assertionResolverHandler);
		}

		return assertionResolverHandlerInstancesMap.get(className);
	}

	public void addAssertionResolverHandler (final String className, final AssertionResolverHandler handler) {

		this.assertionResolverHandlerInstancesMap.put(className, handler);
	}
}
