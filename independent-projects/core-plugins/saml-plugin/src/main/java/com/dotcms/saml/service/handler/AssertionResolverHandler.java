package com.dotcms.saml.service.handler;

import com.dotcms.saml.IdentityProviderConfiguration;
import org.opensaml.saml.saml2.core.Assertion;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;

/**
 * This handler is in charge of resolve the user based on a saml callback
 * Means when the user properly gets login into the IDP, the IDP will perform a
 * request (post/get..) this class is in charge or take this request (which is a XML) and
 * run the logic
 * @author jsanca
 */
public interface AssertionResolverHandler extends Serializable {

	// define the tolerance between the IDP and the IP at time level
	int DOT_SAML_CLOCK_SKEW_DEFAULT_VALUE   = 1000;
	// how long time is an assertion valid
	int DOT_SAML_MESSAGE_LIFE_DEFAULT_VALUE = 2000;

	/**
	 * Returns true if it is a valid saml request, just check if the response is there.
	 *
	 * @param request {@link HttpServletRequest}
	 * @param response {@link HttpServletResponse}
	 * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
	 * @return boolean
	 */
	boolean isValidSamlRequest(final HttpServletRequest request,
							   final HttpServletResponse response,
							   final IdentityProviderConfiguration identityProviderConfiguration);

	/**
	 * Resolve the user based on a SAML callback, depending on the
	 * implementation the criteria to check if it is a saml request and how to
	 * handle might be different.
	 *
	 * @param request  {@link HttpServletRequest}
	 * @param response {@link HttpServletResponse}
	 * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
	 * @return User
	 */
	Assertion resolveAssertion(final HttpServletRequest request,
							   final HttpServletResponse response,
							   final IdentityProviderConfiguration identityProviderConfiguration);

}
