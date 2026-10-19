package com.dotcms.auth.providers.saml.v1;

import com.dotcms.auth.AuthAccessDeniedUtil;
import com.dotcms.filters.interceptor.saml.SamlWebUtils;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
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
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.RequestDispatcher;
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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * This endpoint handles the interaction between the IDP callbacks and dotCMS.
 * - doLogin makes the AuthRequest in order to send the user to the IDP third party login screen.
 * - processLogin handles the callback from the IDP when the user gets successfully login, it will retrieve the user information from the assertion and based on that proceed to get login into dotcms
 * - metadata renders the XML metadata.
 * @author jsanca
 */
@SwaggerCompliant(value = "Core authentication and user management APIs", batch = 1)
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
	@Operation(
		summary = "Initiate SAML login",
		description = "Initiates a SAML authentication request by redirecting the user to the Identity Provider (IDP) login screen. Requires IDP metadata to determine the SSO login endpoint."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "SAML authentication request initiated successfully (no body)"),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid IDP configuration ID",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "IDP configuration not found or not enabled",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error during SAML authentication initiation",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path( "/login/{idpConfigId}" )
	@JSONP
	@NoCache
	@Produces( { MediaType.APPLICATION_JSON } )
	public Response doLogin(@Parameter(description = "Identity Provider configuration ID (typically host ID)", required = true) @PathParam( "idpConfigId" ) final String idpConfigId,
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
	@Operation(
		summary = "Process SAML login callback",
		description = "Handles the callback from the Identity Provider after successful authentication. Extracts user information from the SAML assertion and creates/logs in the user to dotCMS.",
		requestBody = @RequestBody(description = "SAML assertion data from Identity Provider", required = true,
					content = {@Content(mediaType = "application/xml"), 
							  @Content(mediaType = "application/x-www-form-urlencoded")})
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "SAML login processed successfully - user logged in",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid SAML assertion or missing data",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - SAML assertion validation failed",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - the user signed in through SSO but lacks the back-end role required for the destination",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "404", 
					description = "IDP configuration not found or not enabled",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error during SAML login processing",
					content = @Content(mediaType = "text/html"))
	})
	@POST
	@Path("/login/{idpConfigId}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
	@Produces( { MediaType.APPLICATION_XML, "text/html" } )
	@NoCache
	public void processLogin(@Parameter(description = "Identity Provider configuration ID (typically host ID)", required = true) @PathParam("idpConfigId") final String idpConfigId,
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

					final User authorizedUser = reloadUser(user);
					final LoginDestination destination = this.resolveLoginPath(
							httpServletRequest, session, identityProviderConfiguration, authorizedUser);
					if (destination.isDenied()) {
						AuthAccessDeniedUtil.sendNoAccessPage(httpServletResponse, authorizedUser);
						return;
					}

					Logger.debug(this, ()-> "Doing login to the user " + (authorizedUser != null? authorizedUser.getEmailAddress() : "unknown"));
					this.samlHelper.doLogin(httpServletRequest, httpServletResponse,
							identityProviderConfiguration, authorizedUser, APILocator.getLoginServiceAPI());

					Logger.debug(this, ()-> "Final, LoginPath: " + destination.path());
					RedirectUtil.sendRedirectHTML(httpServletResponse, destination.path());
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

	private static User reloadUser(final User user) {
		if (user == null) {
			return null;
		}
		return Try.of(() -> APILocator.getUserAPI()
				.loadUserById(user.getUserId(), APILocator.systemUser(), false))
				.getOrElse(user);
	}

	/**
	 * Decides where the SAML callback sends the user after login, or whether the login is denied.
	 *
	 * The destination is the first usable value of: the {@code RelayState} the IdP posted back, the
	 * destination saved in the session ({@link WebKeys#REDIRECT_AFTER_LOGIN}), the app's
	 * {@code redirect.after.login} setting, and finally {@code /dotAdmin/}. The RelayState and the session
	 * value can carry a value a visitor controls, so they are only used when
	 * {@link RedirectUtil#toAllowedRedirect(String, Collection, Collection)} accepts them as a destination on this site
	 * (the callback host, or the hostname or an alias of the site being logged in to), or on a host explicitly
	 * listed in the app's {@code auth.relaystate.allowed.hosts} (followed as an absolute URL);
	 * a rejected value is recorded in the security log (without the value) and the next source is tried.
	 * The admin-configured {@code redirect.after.login} is trusted and used as is.
	 *
	 * When no source expresses an intent (IdP-initiated logins, or every candidate was rejected), a user without
	 * back-end access lands on the site home ({@code /}), whatever {@code enableFrontend} says. A known
	 * destination in the back-end (from the RelayState, the session or {@code redirect.after.login}) is denied to
	 * users without back-end access; front-end destinations are never denied here.
	 *
	 * @param request                       the IdP callback request
	 * @param session                       the session of the callback request
	 * @param identityProviderConfiguration the site's SAML configuration
	 * @param authorizedUser                the user resolved from the assertion
	 * @return where to redirect the user, or a denial
	 */
	@VisibleForTesting
	LoginDestination resolveLoginPath(final HttpServletRequest request,
									  final HttpSession session,
									  final IdentityProviderConfiguration identityProviderConfiguration,
									  final User authorizedUser) {

		final Collection<String> allowedHosts  = this.allowedRedirectHosts(request, identityProviderConfiguration);
		final Collection<String> externalHosts = SamlWebUtils.configuredAllowedHosts(identityProviderConfiguration);
		boolean loginIntentUnknown = false;

		String loginPath = this.acceptedRedirect(request.getParameter("RelayState"), allowedHosts, externalHosts, "RelayState");
		if (null == loginPath) {

			final Object sessionRedirect = session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
			if (null != sessionRedirect) {
				session.removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
				loginPath = this.acceptedRedirect(sessionRedirect.toString(), allowedHosts, externalHosts, "session REDIRECT_AFTER_LOGIN");
			}
		}

		if (null == loginPath) {
			if (identityProviderConfiguration.containsOptionalProperty(REDIRECT_AFTER_LOGIN_CONFIG)) {
				loginPath = identityProviderConfiguration.getOptionalProperty(REDIRECT_AFTER_LOGIN_CONFIG).toString();
			} else {
				// No source tells us whether this was a front-end or back-end login
				// (typically an IdP-initiated login, which carries no RelayState).
				loginPath = DotSamlConstants.DEFAULT_LOGIN_PATH;
				loginIntentUnknown = true;
			}
		}

		// The session may keep the query string of the original request. Append it only when the
		// destination does not already carry a query, so it can never appear twice.
		final Object queryString = session.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING);
		if (null != queryString && !loginPath.equals(DotSamlConstants.DEFAULT_LOGIN_PATH)
				&& !loginPath.contains("?")) {
			loginPath = loginPath + "?" + queryString;
		}

		final boolean hasBackEndAccess = AuthAccessDeniedUtil.hasRequiredRole(authorizedUser, false);
		// No usable destination (IdP-initiated login, or a rejected RelayState / session value): the path
		// defaulted to /dotAdmin/ above without expressing any real intent. Route by what the user may actually
		// do: users without back-end access land on the site home, a safe page they can use, instead of a 403
		// for a back-end page they never asked for. This does not depend on enableFrontend, so sites that never
		// set it (most existing ones) behave the same way. Page permissions still apply on "/".
		if (loginIntentUnknown && !hasBackEndAccess) {
			loginPath = "/";
		}
		// Only the back-end is gated here: a front-end SAML login must never be denied,
		// since its post-login redirect is a content URL rather than a back-end path.
		if (isBackEndLogin(loginPath) && !hasBackEndAccess) {
			return LoginDestination.deny();
		}

		return LoginDestination.redirectTo(loginPath);
	}

	/**
	 * Returns the hosts a post-login redirect may point to: the callback host plus the hostname and aliases of
	 * the site whose SAML configuration is in use (its id is the site identifier).
	 *
	 * @param request                       the IdP callback request
	 * @param identityProviderConfiguration the site's SAML configuration
	 * @return the allowed hosts
	 */
	@VisibleForTesting
	Collection<String> allowedRedirectHosts(final HttpServletRequest request,
											final IdentityProviderConfiguration identityProviderConfiguration) {
		return SamlWebUtils.siteHosts(request.getServerName(), identityProviderConfiguration.getId());
	}

	/**
	 * Returns the candidate as a safe destination (on this site, or on a host listed in
	 * {@code auth.relaystate.allowed.hosts}), or null when it is absent or rejected.
	 * Rejections are security-logged with the source, the reason and the length only, never the value.
	 */
	private String acceptedRedirect(final String candidate, final Collection<String> allowedHosts,
									final Collection<String> externalHosts, final String source) {

		if (!UtilMethods.isSet(candidate)) {
			return null;
		}
		final Optional<String> accepted = RedirectUtil.toAllowedRedirect(candidate, allowedHosts, externalHosts);
		if (accepted.isEmpty()) {
			final String reason = RedirectUtil.rejectionReason(candidate, allowedHosts, externalHosts)
					.map(Enum::name).orElse("UNKNOWN");
			SecurityLogger.logInfo(DotSamlResource.class, () -> "SAML login: ignored an unsafe post-login "
					+ "destination from " + source + " (reason " + reason + ", length " + candidate.length()
					+ "); it is neither on this site nor on an allowed host. Falling back to the next destination.");
			return null;
		}
		Logger.debug(this, () -> "SAML login: destination from " + source + ": " + accepted.get());
		return accepted.get();
	}

	/**
	 * Determines whether the post-login redirect target points at the dotCMS back-end. Only
	 * back-end logins are subject to the no-access role gate; front-end logins are never denied
	 * here. Only the path decides: the value is cut at the first {@code ?} or {@code #}, as
	 * {@link RedirectUtil} does, so a query or fragment can never change the answer. The path is
	 * percent-decoded once and its dot segments are then resolved, as a browser resolves them (a
	 * {@code ..} at the root stays at the root). So {@code /members/../dotAdmin/},
	 * {@code /members/%2e%2e/dotAdmin/}, {@code /members/..%2f..%2fdotAdmin/} and
	 * {@code /dotAdmin?/../x} count as back-end, and {@code /dotAdmin/../members/} does not. Letter
	 * case is ignored, so {@code /DotAdmin/} counts as back-end too. A path with a broken
	 * percent-escape counts as back-end (fail closed), so the role check still runs. This matters for
	 * the trusted {@code redirect.after.login}, which is not validated. Reuses
	 * {@link SamlWebUtils#isBackEndLoginPageIgnoreCase(String)} so the back-end path set stays in one place.
	 *
	 * @param loginPath the post-login destination
	 * @return true when the destination is a back-end path
	 */
	@VisibleForTesting
	boolean isBackEndLogin(final String loginPath) {

		if (!UtilMethods.isSet(loginPath)) {
			return false;
		}
		final String rawPath = loginPath.split("[?#]", 2)[0];
		final String decodedPath = RedirectUtil.percentDecode(rawPath);
		if (null == decodedPath) {
			return true;
		}
		final String path = resolveDotSegments(decodedPath);
		return UtilMethods.isSet(path) && this.samlWebUtils.isBackEndLoginPageIgnoreCase(path);
	}

	/**
	 * Resolves the {@code .} and {@code ..} segments of an already decoded path the way a browser does: a
	 * {@code ..} at the root is dropped rather than rejected.
	 *
	 * @param path the decoded path, may be null
	 * @return the resolved path, or null when the path is null
	 */
	private static String resolveDotSegments(final String path) {

		if (null == path || !path.startsWith("/")) {
			return path;
		}
		final String[] segments = path.substring(1).split("/", -1);
		final Deque<String> kept = new ArrayDeque<>();
		boolean trailingSlash = false;
		for (int i = 0; i < segments.length; i++) {
			final boolean last = i == segments.length - 1;
			if (".".equals(segments[i])) {
				trailingSlash = last;
			} else if ("..".equals(segments[i])) {
				kept.pollLast();
				trailingSlash = last;
			} else {
				kept.addLast(segments[i]);
				trailingSlash = false;
			}
		}
		final String resolved = "/" + String.join("/", kept);
		return trailingSlash && !kept.isEmpty() ? resolved + "/" : resolved;
	}

	/**
	 * The outcome of {@link #resolveLoginPath}: a destination to redirect to, or a denial.
	 *
	 * @param path the destination to redirect to; null when the login is denied
	 */
	record LoginDestination(String path) {

		private static final LoginDestination DENIED = new LoginDestination(null);

		/** A redirect to the given destination. */
		static LoginDestination redirectTo(final String path) {
			return new LoginDestination(path);
		}

		/** A denied login: the caller shows the 403 "no access" page. */
		static LoginDestination deny() {
			return DENIED;
		}

		/** True when the login is denied. */
		boolean isDenied() {
			return null == this.path;
		}
	}


	
	
	
	
	/**
	 * Renders the XML metadata.
	 * @param idpConfigId          {@link String} identifier config (here the host id)
	 * @param httpServletRequest   {@link HttpServletRequest}
	 * @param httpServletResponse   {@link HttpServletResponse}
	 * @throws IOException
	 */
	@Operation(
		summary = "Get SAML metadata",
		description = "Renders the XML metadata for the SAML Service Provider configuration. This endpoint is only accessible by administrators and provides the metadata required for IDP configuration."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "SAML metadata rendered successfully",
					content = @Content(mediaType = "application/xml")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - admin access required",
					content = @Content(mediaType = "application/xml")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - user is not an administrator",
					content = @Content(mediaType = "application/xml")),
		@ApiResponse(responseCode = "404", 
					description = "IDP configuration not found or not enabled",
					content = @Content(mediaType = "application/xml")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error rendering metadata",
					content = @Content(mediaType = "application/xml"))
	})
	@GET
	@Path( "/metadata/{idpConfigId}" )
	@JSONP
	@NoCache
	@Produces( { MediaType.APPLICATION_XML, "application/xml" } )
	public void metadata( @Parameter(description = "Identity Provider configuration ID (typically host ID)", required = true) @PathParam( "idpConfigId" ) final String idpConfigId,
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

	@Operation(
		summary = "Process SAML logout (POST)",
		description = "Processes a SAML logout request via POST method. Handles logout callbacks from the Identity Provider and redirects to the configured logout endpoint."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "SAML logout processed successfully",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "404", 
					description = "IDP configuration not found or not enabled",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error during logout processing",
					content = @Content(mediaType = "text/html"))
	})
	@POST
	@Path("/logout/{idpConfigId}")
	@NoCache
	@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML})
	// Login configuration by id
	public void logoutPost(@Parameter(description = "Identity Provider configuration ID (typically host ID)", required = true) @PathParam("idpConfigId") final String idpConfigId,
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
					// The redirect commits the response; return so we don't fall through to the
					// unconditional DoesNotExistException throw below (which would write a second response).
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

	@Operation(
		summary = "Process SAML logout (GET)",
		description = "Processes a SAML logout request via GET method. Initiates logout flow and redirects to the configured logout endpoint or builds a logout URL based on the request."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "SAML logout processed successfully",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "404", 
					description = "IDP configuration not found or not enabled",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error during logout processing",
					content = @Content(mediaType = "text/html"))
	})
	@GET
	@Path("/logout/{idpConfigId}")
	@NoCache
	@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML})
	// Login configuration by id
	public void logoutGet(@Parameter(description = "Identity Provider configuration ID (typically host ID)", required = true) @PathParam("idpConfigId") final String idpConfigId,
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
					// The redirect commits the response; return so we don't fall through to the
					// unconditional DoesNotExistException throw below (which would write a second response).
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

	/*
	 * Builds the base url from the initiating Servlet Request.
	 */
	private String buildBaseUrlFromRequest(final HttpServletRequest httpServletRequest) {

		final String uri = httpServletRequest.getScheme() + "://" + httpServletRequest.getServerName() + ":"
				+ httpServletRequest.getServerPort() + "/dotAdmin/show-logout";

		return uri;
	}
}
