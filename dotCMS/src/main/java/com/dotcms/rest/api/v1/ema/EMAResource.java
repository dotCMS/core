package com.dotcms.rest.api.v1.ema;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;


/**
 * Resource API that deals with secrets and their usage on third-party apps integrations.
 * @author jsanca
 */
@Tag(name = "Apps")
@Path("/v1/ema")
public class EMAResource {

    private static final String EMA_APP_KEY = "dotema-config-v2";

    private final WebResource webResource;
    private AppsAPI appsAPI;

    @VisibleForTesting
    public EMAResource(final WebResource webResource,
            final AppsAPI appsAPI) {
        this.webResource = webResource;
        this.appsAPI = appsAPI;
    }

    public EMAResource() {
        this(new WebResource(), APILocator.getAppsAPI());
    }


    /**
     * Returns the ema config for the current site
     * @param request
     * @param response
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getDetails(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response
    ) throws DotDataException, DotSecurityException {

        final Host site  = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();

        Logger.debug(this, ()-> "Getting EMA config for site: " + site.getHostname());

        final Optional<AppDescriptor> appDescriptorOptional = appsAPI
                .getAppDescriptor(EMA_APP_KEY, APILocator.systemUser()); // we use the system b/c we don't want to check permissions, but only have access to this app and should be backend
        if (appDescriptorOptional.isPresent()) {

            final Optional<AppSecrets> optionalAppSecrets = appsAPI
                    .getSecrets(EMA_APP_KEY, false, site, APILocator.systemUser());

            if (optionalAppSecrets.isPresent()) {

                final AppSecrets appSecrets = optionalAppSecrets.get();
                final Secret configSecret = appSecrets.getSecrets().get("configuration");
                final String configJson   = configSecret.getString();

                return Response.ok(new ResponseEntityView<>(new JSONObject(configJson))).build();
            }
        }

        throw new DoesNotExistException(String.format(
                "No configuration was found for EMA on the current site `%s`. ", site.getHostname()));
    }
}
