package com.dotcms.auth.providers.saml.v1;

import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.saml.DotSamlFactory;
import com.dotcms.saml.service.external.IdentityProviderConfiguration;
import com.dotcms.saml.service.external.IdentityProviderConfigurationFactory;
import com.dotcms.saml.service.external.SamlAuthenticationService;
import com.dotcms.saml.service.external.SamlException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

@Path( "/v1/dotsaml" )
public class DotSamlResource implements Serializable {

	private static final long serialVersionUID = 8015545653539491684L;
	private final HostWebAPI hostWebAPI;
	private final SamlAuthenticationService samlAuthenticationService;
	private final IdentityProviderConfigurationFactory identityProviderConfigurationFactory;


	public DotSamlResource() {

		this.hostWebAPI  = WebAPILocator.getHostWebAPI();
		this.samlAuthenticationService = DotSamlFactory.getInstance().samlAuthenticationService();
		this.identityProviderConfigurationFactory = DotSamlFactory.getInstance().IdentityProviderConfigurationFactory();
	}

	@GET
	@Path( "/login/{idpConfigId}" )
	@JSONP
	@NoCache
	@Produces( { MediaType.APPLICATION_JSON, "application/javascript" } )
	// Login configuration by id
	public Response login(@PathParam( "idpConfigId" ) final String idpConfigId,
						  @Context final HttpServletRequest httpServletRequest,
						  @Context final HttpServletResponse httpServletResponse) {

		if (DotSamlFactory.getInstance().isAnyHostConfiguredAsSAML()) {

			final IdentityProviderConfiguration identityProviderConfiguration =
					this.identityProviderConfigurationFactory.findIdentityProviderConfigurationById(idpConfigId);

			// If idpConfig is null, means this site does not need SAML processing
			if (identityProviderConfiguration != null && identityProviderConfiguration.isEnabled()) {

				Logger.debug(this, () -> "Processing saml login request for idpConfig id: " + idpConfigId);
				this.doRequestLoginSecurityLog(httpServletRequest, identityProviderConfiguration);

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

	@GET
	@Path( "/metadata/{idpConfigId}" )
	@JSONP
	@NoCache
	@Produces( { MediaType.APPLICATION_JSON, "application/javascript" } )
	// Gets metadata configuration by id
	public void metadata( @PathParam( "idpConfigId" ) final String idpConfigId,
						  @Context final HttpServletRequest httpServletRequest,
						  @Context final HttpServletResponse httpServletResponse ) throws IOException {

		boolean noConfig = true;

		if (DotSamlFactory.getInstance().isAnyHostConfiguredAsSAML()) {

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
			throw new SamlException(message);
		}
	}


	public void doRequestLoginSecurityLog(final HttpServletRequest request,
										  final IdentityProviderConfiguration identityProviderConfiguration) {

		try {

			final Host host  = this.hostWebAPI.getCurrentHost(request);
			final String env = this.isFrontEndLoginPage(request.getRequestURI()) ? "frontend" : "backend";
			final String log = new Date() + ": SAML login request for Site '" + host.getHostname() + "' with IdP ID: "
					+ identityProviderConfiguration.getId() + " (" + env + ") from " + request.getRemoteAddr();

			// “$TIMEDATE: SAML login request for $host (frontend|backend)from
			// $REQUEST_ADDR”
			SecurityLogger.logInfo(SecurityLogger.class, this.getClass() + " - " + log);
			Logger.debug(this, ()-> log);
		} catch (Exception e) {

			Logger.error(this, e.getMessage(), e);
		}
	}

	protected boolean isFrontEndLoginPage(final String uri) {

		return uri.startsWith("/dotCMS/login") || uri.startsWith("/application/login");
	}
}
