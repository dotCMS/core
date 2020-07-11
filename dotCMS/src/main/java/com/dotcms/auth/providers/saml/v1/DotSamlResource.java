package com.dotcms.auth.providers.saml.v1;

import com.dotcms.rest.annotation.NoCache;
import com.dotcms.saml.Attributes;
import com.dotcms.saml.DotSamlConstants;
import com.dotcms.saml.DotSamlProxyFactory;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.IdentityProviderConfigurationFactory;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Path("/v1/dotsaml")
public class DotSamlResource implements Serializable {

	private static final long serialVersionUID = 8015545653539491684L;

	private final SAMLHelper           				   samlHelper;
	private final SamlAuthenticationService            samlAuthenticationService;
	private final IdentityProviderConfigurationFactory identityProviderConfigurationFactory;

	public static final List<String> dotsamlPathSegments = new ArrayList<String>() {
		{
			add("login");
			add("logout");
			add("metadata");
		}
	};


	public DotSamlResource() {

		this.samlAuthenticationService            = DotSamlProxyFactory.getInstance().samlAuthenticationService();
		this.identityProviderConfigurationFactory = DotSamlProxyFactory.getInstance().identityProviderConfigurationFactory();
		this.samlHelper                           = new SAMLHelper(this.samlAuthenticationService);
	}

	// Login configuration by id
	@GET
	@Path( "/login/{idpConfigId}" )
	@JSONP
	@NoCache
	@Produces( { MediaType.APPLICATION_JSON, "application/javascript" } )
	public Response doLogin(@PathParam( "idpConfigId" ) final String idpConfigId,
						  @Context final HttpServletRequest httpServletRequest,
						  @Context final HttpServletResponse httpServletResponse) {

		if (DotSamlProxyFactory.getInstance().isAnyHostConfiguredAsSAML()) {

			final IdentityProviderConfiguration identityProviderConfiguration =
					this.identityProviderConfigurationFactory.findIdentityProviderConfigurationById(idpConfigId);

			// If idpConfig is null, means this site does not need SAML processing
			if (identityProviderConfiguration != null && identityProviderConfiguration.isEnabled()) {

				Logger.debug(this, () -> "Processing saml login request for idpConfig id: " + idpConfigId);
				this.samlHelper.doRequestLoginSecurityLog(httpServletRequest, identityProviderConfiguration);

				// This will redirect the user to the IdP Login Page.
				this.samlAuthenticationService.authentication(httpServletRequest,
						httpServletResponse, identityProviderConfiguration);

				return Response.ok().build();
			}
		}

		final String message = "No idpConfig for idpConfigId: " + idpConfigId + ". At " + httpServletRequest.getRequestURI();
		Logger.debug( this, ()-> message);
		throw new SamlException(message);
	}

	@POST
	@Path("/login/{idpConfigId}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
	@NoCache
	public void processLogin(@PathParam("idpConfigId") final String idpConfigId,
					  @Context final HttpServletRequest httpServletRequest,
					  @Context final HttpServletResponse httpServletResponse) throws IOException {

		if (DotSamlProxyFactory.getInstance().isAnyHostConfiguredAsSAML()) {

			final IdentityProviderConfiguration identityProviderConfiguration =
					this.identityProviderConfigurationFactory.findIdentityProviderConfigurationById(idpConfigId);
			// If idpConfig is null, means this site does not need SAML processing
			if (identityProviderConfiguration != null && identityProviderConfiguration.isEnabled()) {

				Logger.debug(this, () -> "Processing saml login request for idpConfig id: " + idpConfigId);
				this.samlHelper.doRequestLoginSecurityLog(httpServletRequest, identityProviderConfiguration);

				final HttpSession session = httpServletRequest.getSession();
				if (null == session) {

					throw new SamlException("No session has been created.");
				}

				// Extracts data from the assertion - if it can't process a DotSamlException is thrown
				final Attributes attributes = this.samlAuthenticationService.resolveAttributes(httpServletRequest,
						httpServletResponse, identityProviderConfiguration);

				if (null == attributes) {

					throw new SamlException("User cannot be extracted from Assertion!");
				}
				// Creates the user object and adds a user if it doesn't already exist
				final User user = this.samlHelper.resolveUser(attributes, identityProviderConfiguration);
				if (null == user) {

					throw new SamlException("User cannot be extracted from Assertion!");
				}

				Logger.debug(this, ()-> "Resolved user: " + user);

				final String samlSessionIndex = attributes.getSessionIndex();
				if (null != samlSessionIndex) {

					Logger.debug(this, ()-> "SAMLSessionIndex: " + samlSessionIndex);
					// Session Attributes used to build logout request
					final String sessionIndexKey = identityProviderConfiguration.getId() + DotSamlConstants.SAML_SESSION_INDEX;
					final String samlNameIdKey   = identityProviderConfiguration.getId() + DotSamlConstants.SAML_NAME_ID;
					session.setAttribute(sessionIndexKey, samlSessionIndex);
					session.setAttribute(samlNameIdKey,  attributes.getNameID());
					Logger.debug(this, ()->"Session index with key: " + sessionIndexKey + " and value: " + session.getAttribute(sessionIndexKey) + " is already set.");
					Logger.debug(this, ()->"NameID with key: " + samlNameIdKey + " and value: " + session.getAttribute(samlNameIdKey) + " is already set.");
				}

				// Add session based user ID to be used on the redirect.
				session.setAttribute(identityProviderConfiguration.getId() + DotSamlConstants.SAML_USER_ID, user.getUserId());
				session.setAttribute(com.liferay.portal.util.WebKeys.USER,    user);
				session.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
				session.setAttribute(WebKeys.CMS_USER, user);

				String loginPath = (String) session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
				if (null == loginPath) {
					// At this stage we cannot determine whether this was a front
					// end or back end request since we cannot determine
					// original request.
					//
					// REDIRECT_AFTER_LOGIN should have already been set in relay
					// request to IdP. 'autoLogin' will check the ORIGINAL_REQUEST
					// session attribute.
					loginPath = DotSamlConstants.DEFAULT_LOGIN_PATH;
				} else {

					session.removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
				}

				httpServletResponse.sendRedirect(loginPath);
			}
		}

		final String message = "No idpConfig for idpConfigId: " + idpConfigId + ". At " + httpServletRequest.getRequestURI();
		Logger.debug( this, ()-> message);
		throw new SamlException(message);
	}

	// Gets metadata configuration by id
	@GET
	@Path( "/metadata/{idpConfigId}" )
	@JSONP
	@NoCache
	@Produces( { MediaType.APPLICATION_JSON, "application/javascript" } )
	public void metadata( @PathParam( "idpConfigId" ) final String idpConfigId,
						  @Context final HttpServletRequest httpServletRequest,
						  @Context final HttpServletResponse httpServletResponse ) throws IOException {

		boolean noConfig = true;

		if (DotSamlProxyFactory.getInstance().isAnyHostConfiguredAsSAML()) {

			final IdentityProviderConfiguration identityProviderConfiguration =
					this.identityProviderConfigurationFactory.findIdentityProviderConfigurationById(idpConfigId);

			// If idpConfig is null, means this site does not need SAML processing
			if (identityProviderConfiguration != null && identityProviderConfiguration.isEnabled()) {

				Logger.debug(this, () -> "Processing saml login request for idpConfig id: " + idpConfigId);
				this.samlAuthenticationService.renderMetadataXML(httpServletResponse.getWriter(), identityProviderConfiguration);
				noConfig = false;
			}
		}

		if (noConfig) {

			final String message = "No idpConfig for idpConfigId: " + idpConfigId + ". At " + httpServletRequest.getRequestURI();
			Logger.debug(this, () -> message);
			throw new DoesNotExistException(message);
		}
	}
}

