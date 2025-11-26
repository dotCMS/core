package com.dotcms.rest.api.v1.system;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.AccessControlAllowOrigin;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.Map;

/**
 * This Jersey end-point provides access to configuration parameters that are
 * useful to the dotCMS Angular UI. System properties set through dotCMS
 * configuration files, and the menu items that logged in users can see in their
 * navigation bar, are just a couple of configuration properties that this
 * end-point can provide.
 * <p>
 * The number of configuration properties my vary depending on whether the user
 * is logged in or not before calling this end-point. For example, the list of
 * navigation menu items <b>will be returned as an empty list</b>
 * <p>
 * This is a public endpoint and requires no authentiction.
 *
 * @author Jose Castro
 * @version 3.7
 * @since Jul 22, 2016
 */
@Tag(name = "System Configuration")
@Path("/v1/appconfiguration")
@SuppressWarnings("serial")
public class AppContextInitResource implements Serializable {

    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String KUBE_PROBE_PREFIX = "kube-probe";

    private static final String CONFIG = "config";

    private final AppConfigurationHelper helper;

    /**
     * Default constructor.
     */
    public AppContextInitResource() {
        this(AppConfigurationHelper.getInstance());
    }

    @VisibleForTesting
    public AppContextInitResource(AppConfigurationHelper helper) {
        this.helper = helper;
    }

    /**
     * Returns the list of system properties that are useful to the UI layer.
     *
     * @param request - The {@link HttpServletRequest} object.
     * @return The JSON representation of configuration parameters.
     */
    @GET
    @JSONP
    @NoCache
    @AccessControlAllowOrigin
    @InitRequestRequired
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response list(@Context final HttpServletRequest request) {
        try {
            // Return all configuration parameters in one response
            final Object configData = this.helper.getConfigurationData(request);

            // If the request is from a Kubernetes probe, we return a 200 OK and skip the body.
            if (isKubernetesProbe(request)) {
                return Response.ok().build();
            }

            final Map<String, Object> configMap = Map.of(CONFIG, configData);

            return Response.ok(new ResponseEntityView(configMap)).build();
        } catch (Exception e) {
            // In case of unknown error, so we report it as a 500
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Checks if the incoming request is from a Kubernetes probe based on the User-Agent header.
     * <p>
     * If the request is from a Kubernetes probe (User-Agent starts with "kube-probe"),
     * the endpoint responds with a 200 OK and skips the body to reduce unnecessary payloads.
     * <p>
     * <b>Temporary Solution:</b> This logic is a temporary measure to efficiently handle
     * Kubernetes probes (e.g., liveness and readiness) without introducing additional endpoints
     * or modifying the deployment configuration. These probes do not require the complete
     * configuration payload returned by this endpoint, and a lightweight response is sufficient.
     * <p>
     * <b>Note:</b> Once a dedicated endpoint for probes is implemented, this logic should be reviewed
     * and potentially removed.
     *
     * @param request The incoming {@link HttpServletRequest}.
     * @return {@code true} if the request is from a Kubernetes probe; {@code false} otherwise.
     */
    private boolean isKubernetesProbe(final HttpServletRequest request) {
        final String userAgent = request.getHeader(USER_AGENT_HEADER);
        return userAgent != null && userAgent.startsWith(KUBE_PROBE_PREFIX);
    }
}
