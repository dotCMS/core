package com.dotcms.auth.providers.saml.v1;

import com.dotcms.filters.interceptor.saml.SamlWebUtils;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.saml.Attributes;
import com.dotcms.saml.DotSamlConstants;
import com.dotcms.saml.DotSamlException;
import com.dotcms.saml.DotSamlProxyFactory;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.IdentityProviderConfigurationFactory;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlName;
import com.dotcms.util.RedirectUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * This endpoint handles the interaction between the IDP callbacks and dotCMS.
 * - doLogin makes the AuthRequest in order to send the user to the IDP third party login screen.
 * - processLogin handles the callback from the IDP when the user gets successfully login, it will retrieve the user information from the assertion and based on that proceed to get login into dotcms
 * - metadata renders the XML metadata.
 * @author jsanca
 */
@Tag(name = "SAML Authentication")
@Path("/v1/dotsaml")
public class DotSamlResource implements Serializable {

	private static final long serialVersionUID = 8015545653539491684L;
	public static final String REDIRECT_AFTER_LOGIN_CONFIG = "redirect.after.login";

	private final SamlConfigurationService             samlConfigurationService;
	private final SAMLHelper           				   samlHelper;
	private final SamlAuthenticationService            samlAuthenticationService;
	private final IdentityProviderConfigurationFactory identityProviderConfigurationFactory;
	private final WebResource						   webResource;

	private final SamlWebUtils samlWebUtils = new SamlWebUtils();

	public static final List<String> dotsamlPathSegments = Arrays.asList("login", "logout", "metadata");


	public DotSamlResource() {

		this.samlConfigurationService			  = DotSamlProxyFactory.getInstance().samlConfigurationService();
		this.samlAuthenticationService            = DotSamlProxyFactory.getInstance().samlAuthenticationService();
		this.identityProviderConfigurationFactory = DotSamlProxyFactory.getInstance().identityProviderConfigurationFactory();
		this.samlHelper                           = new SAMLHelper(this.samlAuthenticationService, APILocator.getCompanyAPI());
		this.webResource						  = new WebResource();
	}

	@VisibleForTesting
	protected DotSamlResource(final SamlConfigurationService           samlConfigurationService,
							final SAMLHelper           				   samlHelper,
							final SamlAuthenticationService            samlAuthenticationService,
							final IdentityProviderConfigurationFactory identityProviderConfigurationFactory,
							final WebResource						   webResource) {

		this.samlConfigurationService			  = samlConfigurationService;
		this.samlAuthenticationService            = samlAuthenticationService;
		this.identityProviderConfigurationFactory = identityProviderConfigurationFactory;
		this.samlHelper                           = samlHelper;
		this.webResource						  = webResource;
	}

	/**
	 * doLogin makes the AuthRequest in order to send the user to the IDP third party login screen.
	 * It will needs the IDP metadata in order to now the SSO Login endpoint
	 * @param idpConfigId			{@link String} identifier config (here the host id)
	 * @param httpServletRequest    {@link HttpServletRequest}
	 * @param httpServletResponse   {@link HttpServletResponse}
	 * @return Response
	 */
	@GET
	@Path( "/login/{idpConfigId}" )
	@JSONP
	@NoCache
	@Produces( { MediaType.APPLICATION_JSON, "application/javascript" } )
	public Response doLogin(@PathParam( "idpConfigId" ) final String idpConfigId,
							@Context final HttpServletRequest httpServletRequest,
							@Context final HttpServletResponse httpServletResponse) {

		IdentityProviderConfiguration identityProviderConfiguration = null;

		try {
			if (DotSamlProxyFactory.getInstance().isAnyHostConfiguredAsSAML()) {

				identityProviderConfiguration =
						this.identityProviderConfigurationFactory.findIdentityProviderConfigurationById(idpConfigId);

				// If idpConfig is null, means this site does not need SAML processing
				if (identityProviderConfiguration != null && identityProviderConfiguration.isEnabled()) {

					Logger.debug(this, () -> "Processing saml login request for idpConfig id: " + idpConfigId);
					this.samlHelper.doRequestLoginSecurityLog(httpServletRequest, identityProviderConfiguration);

					final String relayState = this.samlWebUtils.getRelayState(httpServletRequest, httpServletResponse, identityProviderConfiguration, idpConfigId);
					// This will redirect the user to the IdP Login Page.
					this.samlAuthenticationService.authentication(httpServletRequest,
							httpServletResponse, identityProviderConfiguration, relayState);

					return Response.ok().build();
				}
			}
		} finally {

			if (null != identityProviderConfiguration) {
				identityProviderConfiguration.destroy();
			}
		}

		final String message = "No idpConfig for idpConfigId: " + idpConfigId + ". At " + httpServletRequest.getRequestURI();
		Logger.debug( this, ()-> message);
		throw new DotSamlException(message);
	}

	/**
	 * processLogin handles the callback from the IDP when the user gets successfully login, it will retrieve the user information from the assertion and based on that proceed to get login into dotcms
	 * @param idpConfigId           {@link String} identifier config (here the host id)
	 * @param httpServletRequest    {@link HttpServletRequest}
	 * @param httpServletResponse   {@link HttpServletResponse}
	 * @throws IOException
	 */
	@POST
	@Path("/login/{idpConfigId}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
	@Produces( { MediaType.APPLICATION_XML, "text/html" } )
	@NoCache
	public void processLogin(@PathParam("idpConfigId") final String idpConfigId,
							 @Context final HttpServletRequest httpServletRequest,
							 @Context final HttpServletResponse httpServletResponse) throws IOException {

		if (DotSamlProxyFactory.getInstance().isAnyHostConfiguredAsSAML()) {

			final IdentityProviderConfiguration identityProviderConfiguration =
					this.identityProviderConfigurationFactory.findIdentityProviderConfigurationById(idpConfigId);
			try {

				// If idpConfig is null, means this site does not need SAML processing
				if (identityProviderConfiguration != null && identityProviderConfiguration.isEnabled()) {

					Logger.debug(this, () -> "Processing saml login request for idpConfig id: " + idpConfigId);
					this.samlHelper.doRequestLoginSecurityLog(httpServletRequest, identityProviderConfiguration);

					final HttpSession session = httpServletRequest.getSession();
					if (null == session) {

						Logger.debug(this, () -> "No session has been created.");
						throw new DotSamlException("No session has been created.");
					}

					Logger.debug(this, ()-> "SAML: Http Session Id: " + session.getId());

					// Extracts data from the assertion - if it can't process a DotSamlException is thrown
					final Attributes attributes = this.samlAuthenticationService.resolveAttributes(httpServletRequest,
							httpServletResponse, identityProviderConfiguration);

					if (null == attributes) {

						Logger.debug(this, () -> "User cannot be extracted from Assertion!");
						throw new DotSamlException("User cannot be extracted from Assertion!");
					}

					Logger.debug(this, () -> "Retrieving attributes: " + attributes);
					// Creates the user object and adds a user if it doesn't already exist
					final User user = this.samlHelper.resolveUser(attributes, identityProviderConfiguration);
					if (null == user) {

						Logger.debug(this, () -> "User cannot be extracted from Assertion!");
						throw new DotSamlException("User cannot be extracted from Assertion!");
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

					String loginPath = httpServletRequest.getParameter("RelayState");
					Logger.debug(this, "RelayState, LoginPath: " + loginPath);
					if (!UtilMethods.isSet(loginPath)) {

						loginPath = (String) session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
						Logger.debug(this, "REDIRECT_AFTER_LOGIN, LoginPath: " + loginPath);
						if (null == loginPath) {
							if (identityProviderConfiguration.containsOptionalProperty(REDIRECT_AFTER_LOGIN_CONFIG)) {
								loginPath = identityProviderConfiguration.getOptionalProperty(REDIRECT_AFTER_LOGIN_CONFIG).toString();
							} else {
								// At this stage we cannot determine whether this was a front
								// end or back end request since we cannot determine
								// original request.
								//
								// REDIRECT_AFTER_LOGIN should have already been set in relay
								// request to IdP. 'autoLogin' will check the ORIGINAL_REQUEST
								// session attribute.
								loginPath = DotSamlConstants.DEFAULT_LOGIN_PATH;
							}
						} else {

							session.removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
						}
					}

					Logger.debug(this, ()-> "Doing login to the user " + (user != null? user.getEmailAddress() : "unknown"));
					this.samlHelper.doLogin(httpServletRequest, httpServletResponse,
							identityProviderConfiguration, user, APILocator.getLoginServiceAPI());

					Logger.debug(this, "Final, LoginPath: " + loginPath);
					RedirectUtil.sendRedirectHTML(httpServletResponse, loginPath);
					return;
				}
			} finally {
				if (null != identityProviderConfiguration) {
					identityProviderConfiguration.destroy();
				}
			}
		}

		final String message = "No idpConfig for idpConfigId: " + idpConfigId + ". At " + httpServletRequest.getRequestURI();
		Logger.debug( this, ()-> message);
		throw new DotSamlException(message);
	}


	
	
	
	
	/**
	 * Renders the XML metadata.
	 * @param idpConfigId          {@link String} identifier config (here the host id)
	 * @param httpServletRequest   {@link HttpServletRequest}
	 * @param httpServletResponse   {@link HttpServletResponse}
	 * @throws IOException
	 */
	@GET
	@Path( "/metadata/{idpConfigId}" )
	@JSONP
	@NoCache
	@Produces( { MediaType.APPLICATION_XML, "application/xml" } )
	public void metadata( @PathParam( "idpConfigId" ) final String idpConfigId,
						  @Context final HttpServletRequest httpServletRequest,
						  @Context final HttpServletResponse httpServletResponse ) throws IOException {

		if (!new WebResource.InitBuilder(this.webResource).rejectWhenNoUser(true).
				requestAndResponse(httpServletRequest, httpServletResponse)
				.requiredBackendUser(true).init().getUser().isAdmin()) {

			throw new RuntimeException(new DotSecurityException("SAML metadata is only accessible by administrators"));
		}

		if (DotSamlProxyFactory.getInstance().isAnyHostConfiguredAsSAML()) {

			final IdentityProviderConfiguration identityProviderConfiguration =
					this.identityProviderConfigurationFactory.findIdentityProviderConfigurationById(idpConfigId);
			try {
				// If idpConfig is null, means this site does not need SAML processing
				if (identityProviderConfiguration != null && identityProviderConfiguration.isEnabled()) {

					Logger.debug(this, () -> "Processing saml login request for idpConfig id: " + idpConfigId);
					httpServletResponse.setContentType("application/xml");
					this.samlAuthenticationService.renderMetadataXML(httpServletResponse.getWriter(), identityProviderConfiguration);
					return;
				}
			} finally {
				if (null != identityProviderConfiguration) {
					identityProviderConfiguration.destroy();
				}
			}
		}

		final String message = "No idpConfig for idpConfigId: " + idpConfigId + ". At " + httpServletRequest.getRequestURI();
		Logger.debug(this, () -> message);
		throw new DoesNotExistException(message);
	}

	@POST
	@Path("/logout/{idpConfigId}")
	@NoCache
	@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML})
	// Login configuration by id
	public void logoutPost(@PathParam("idpConfigId") final String idpConfigId,
					   @Context final HttpServletRequest httpServletRequest,
					   @Context final HttpServletResponse httpServletResponse) throws IOException, URISyntaxException {

		if (DotSamlProxyFactory.getInstance().isAnyHostConfiguredAsSAML()) {

			final IdentityProviderConfiguration identityProviderConfiguration =
					this.identityProviderConfigurationFactory.findIdentityProviderConfigurationById(idpConfigId);
			try {
				// If idpConfig is null, means this site does not need SAML processing
				if (identityProviderConfiguration != null && identityProviderConfiguration.isEnabled()) {

					Logger.debug(this, () -> "Processing saml logout post request for idpConfig id: " + idpConfigId);
					final String logoutPath = this.samlConfigurationService.getConfigAsString(identityProviderConfiguration,
							SamlName.DOT_SAML_LOGOUT_SERVICE_ENDPOINT_URL,
							()-> "/dotAdmin/#/public/logout");
					RedirectUtil.sendRedirectHTML(httpServletResponse, logoutPath);


				}
			} finally {
				if (null != identityProviderConfiguration) {
					identityProviderConfiguration.destroy();
				}
			}
		}

		final String message = "No idpConfig for idpConfigId: " + idpConfigId + ". At " + httpServletRequest.getRequestURI();
		Logger.debug(this, () -> message);
		throw new DoesNotExistException(message);
	}

	@GET
	@Path("/logout/{idpConfigId}")
	@NoCache
	@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML})
	// Login configuration by id
	public void logoutGet(@PathParam("idpConfigId") final String idpConfigId,
					   @Context final HttpServletRequest httpServletRequest,
					   @Context final HttpServletResponse httpServletResponse) throws IOException, URISyntaxException {

		if (DotSamlProxyFactory.getInstance().isAnyHostConfiguredAsSAML()) {

			final IdentityProviderConfiguration identityProviderConfiguration =
					this.identityProviderConfigurationFactory.findIdentityProviderConfigurationById(idpConfigId);
			try {
				// If idpConfig is null, means this site does not need SAML processing
				if (identityProviderConfiguration != null && identityProviderConfiguration.isEnabled()) {

					Logger.debug(this, () -> "Processing saml logout get request for idpConfig id: " + idpConfigId);
					final String logoutPath = this.samlConfigurationService.getConfigAsString(identityProviderConfiguration,
							SamlName.DOT_SAML_LOGOUT_SERVICE_ENDPOINT_URL,
							()-> this.buildBaseUrlFromRequest(httpServletRequest));

					
					
					RedirectUtil.sendRedirectHTML(httpServletResponse, logoutPath);

				}
			} finally {
				if (null != identityProviderConfiguration) {
					identityProviderConfiguration.destroy();
				}
			}
		}

		final String message = "No idpConfig for idpConfigId: " + idpConfigId + ". At " + httpServletRequest.getRequestURI();
		Logger.debug(this, () -> message);
		throw new DoesNotExistException(message);
	}

	/*
	 * Builds the base url from the initiating Servlet Request.
	 */
	private String buildBaseUrlFromRequest(final HttpServletRequest httpServletRequest) {

		final String uri = httpServletRequest.getScheme() + "://" + httpServletRequest.getServerName() + ":"
				+ httpServletRequest.getServerPort() + "/dotAdmin/show-logout";

		return uri;
	}
}
