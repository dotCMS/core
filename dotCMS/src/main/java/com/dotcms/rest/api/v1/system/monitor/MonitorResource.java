package com.dotcms.rest.api.v1.system.monitor;

import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.ResponseBuilder;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.annotation.NoCache;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;

/**
 * 
 * The Monitor provides a summary of the system status.
 * It will provide information such as:
 * - Database Health
 * - Elastic Search Index Health
 * - Cache Health
 * - Local file system
 * - Share assets
 *
 * Most of the validation are made by on a timeout, you can use these configuration
 * settings in order to set your own (1 second is the default, 1000millis)
 *
 * - SYSTEM_STATUS_API_LOCAL_FS_TIMEOUT: Local file system timeout
 * - SYSTEM_STATUS_API_CACHE_TIMEOUT: Cache timeout
 * - SYSTEM_STATUS_API_ASSET_FS_TIMEOUT: Share assets timeout
 * - SYSTEM_STATUS_API_INDEX_TIMEOUT: Elastic Search Index timeout
 * - SYSTEM_STATUS_API_DB_TIMEOUT: Database timeout
 *
 * These are the response codes
 * - 200 all good
 * - 403 forbidden (if your ip is not allowed)
 * - 503 some service unavailable
 * - 507 insufficient storage
 *
 * Finally you can get a complete response by using as query string, ?extended
 */
@Path("/v1/system-status")
public class MonitorResource {

    public  static final String EXTENDED = "extended";
    private static final int   INSUFFICIENT_STORAGE = 507;
    private static final int   SERVICE_UNAVAILABLE  = 503;
    private static final int   FORBIDDEN            = 403;
    private final MonitorHelper monitorHelper = new MonitorHelper();

    @NoCache
    @GET
    @JSONP
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMonitorStats(final @Context HttpServletRequest request) throws Throwable {

        // force authentication
        final boolean extendedFormat = (request.getQueryString() != null && EXTENDED.equals(request.getQueryString()));
        final Boolean accessAllowed  = this.monitorHelper.getAccessAllowed(request);
        ResponseBuilder builder = null;

        if (accessAllowed) {

            final MonitorResultView monitorResultView =
                    this.monitorHelper.getMonitorStats(extendedFormat);
            if (extendedFormat) {

                builder = Response.ok(monitorResultView);
            } else {
                if (monitorResultView.isDotCMSHealthy()) {

                    builder = Response.ok(StringPool.BLANK);
                } else if (!monitorResultView.getSubSystemView().isIndexWorkingHealthy() &&
                        monitorResultView.getSubSystemView().isDbSelectHealthy() &&
                        monitorResultView.getSubSystemView().isIndexLiveHealthy() &&
                        monitorResultView.getSubSystemView().isCacheHealthy() &&
                        monitorResultView.getSubSystemView().isLocalFSHealthy() &&
                        monitorResultView.getSubSystemView().isAssetFSHealthy()) {

                    builder = Response.status(INSUFFICIENT_STORAGE).entity(StringPool.BLANK).type(MediaType.APPLICATION_JSON);
                } else {
                    builder = Response.status(SERVICE_UNAVAILABLE).entity(StringPool.BLANK).type(MediaType.APPLICATION_JSON);
                }
            }
        } else {
            builder = Response.status(FORBIDDEN).entity(StringPool.BLANK).type(MediaType.APPLICATION_JSON); // Access is forbidden because IP is not in any range in ACL list
        }

        // todo: create an AccessControl annotation with a key and value.
        builder.header("Access-Control-Expose-Headers", "Authorization");
        builder.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization");
        return builder.build();
    }


}