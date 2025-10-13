package com.dotcms.ai.rest;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.WebResource;
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsUtil;
import com.dotcms.security.apps.ParamDescriptor;
import com.dotcms.security.apps.Secret;
import com.dotcms.util.ClasspathResourceLoader;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.StringUtils;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Optional;

@Path("/v1/ai/configuration")
@Tag(name = "AI", description = "AI to support configuration endpoints")
public class ConfigurationResource {

    public static final String ADVANCE_PROVIDER_SETTINGS_KEY = "advanceProviderSettings";
    public static final String DOT_AI_APP_KEY = "dotAI";
    public static final String CONFIG_JSON_PATH = "/dot-ai/dot-ai-vendors-models-default-template-config.json";
    private final WebResource    webResource;

    public ConfigurationResource() {
        this(new WebResource());
    }

    public ConfigurationResource(WebResource webResource) {
        this.webResource = webResource;
    }

    /**
     * This method loads the default configuration from the class path to the site AI config, if the field
     * advanceProviderSettings is empty
     *
     * @param request the HttpServletRequest object.
     * @param response the HttpServletResponse object.
     * @param siteId site id to make the change
     * @return a Response object containing a map with "type" as key and "embeddings" as value.
     */
    @GET
    @JSONP
    @Path("/_loadcustomproviders/{siteId}")
    @Produces(MediaType.APPLICATION_JSON)
    public final ResponseEntityStringView loadCustomProviders(@Context final HttpServletRequest request,
                                                              @Context final HttpServletResponse response,
                                                              @PathParam("siteId") @Parameter(
                                                      required = true,
                                                      description = "Identifier of site to populate the advanceProviderSettings.\n\n",
                                                      schema = @Schema(type = "string")
                                              ) final String siteId) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        final PageMode pageMode = PageMode.get(request);
        final Host site = APILocator.getHostAPI().find(siteId, user, pageMode.respectAnonPerms);

        if (null == site) {

            final String msg = "Site : " + siteId + " does not exist";
            Logger.error(this, msg);
            throw new DoesNotExistException(msg);
        }

        final Optional<AppSecrets> optionalAppSecrets =  APILocator.getAppsAPI().getSecrets(DOT_AI_APP_KEY,site, user);
        if (!optionalAppSecrets.isPresent()) {

            final String msg = "Site : " + siteId + " does not have dot AI configuration";
            Logger.error(this, msg);
            throw new DoesNotExistException(msg);
        }

        // 1) check if the app tied to the app has the advanceProviderSettings empty
        final String dotAISecretValue = optionalAppSecrets.get().getSecrets().get(ADVANCE_PROVIDER_SETTINGS_KEY).getString();
        if (StringUtils.isNotSet(dotAISecretValue)) {

            final AppDescriptor appDescriptor = APILocator.getAppsAPI().getAppDescriptor(DOT_AI_APP_KEY, user).get();
            final java.util.Map<String, ParamDescriptor> paramDescriptors = appDescriptor.getParams();
            final ParamDescriptor paramDescriptor = paramDescriptors.get(ADVANCE_PROVIDER_SETTINGS_KEY);
            // 2) if empty read from the classpath the dot-ai-vendors-models-default-template-config.json
            final String defaultVendorConfigurationJson = ClasspathResourceLoader.readTextOrThrow(CONFIG_JSON_PATH);
            final java.util.Optional<Secret> secretOpt =
                    AppsUtil.paramSecret(
                            DOT_AI_APP_KEY,
                            ADVANCE_PROVIDER_SETTINGS_KEY,
                            defaultVendorConfigurationJson.toCharArray(),
                            paramDescriptor
                    );

            if (secretOpt.isPresent()) {
                APILocator.getAppsAPI().saveSecret(
                        DOT_AI_APP_KEY,
                        new io.vavr.Tuple2<>(ADVANCE_PROVIDER_SETTINGS_KEY, secretOpt.get()),
                        site,
                        user
                );
                Logger.debug(this, String.format("Successfully created and saved secret for %s", ADVANCE_PROVIDER_SETTINGS_KEY));
            } else {
                Logger.warn(this, String.format("Failed to create secret for %s", ADVANCE_PROVIDER_SETTINGS_KEY));
            }

            // 3) override the field for it
            return new ResponseEntityStringView("Success");
        }

        return new ResponseEntityStringView("A configuration already exists. To apply changes, you must first clear the 'Custom AI Provider Settings' field.");
    }
}
