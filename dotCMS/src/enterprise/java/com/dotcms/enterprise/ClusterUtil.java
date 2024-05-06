/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

/**
 * 
 */
package com.dotcms.enterprise;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.RequestOptions;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.content.elasticsearch.util.DotRestHighLevelClientProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.NullTransport;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.liferay.util.FileUtil;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;
import org.elasticsearch.common.unit.TimeValue;

/**
 * Class to handle all logic for clusters. Should be called by REST API and Server Action.
 * 
 * @author Oscar Arrieta.
 *
 */
class ClusterUtil {

    /**
     * Returns the basic information of the current server (local node) in JSON format.
     * 
     * @return JSONObject - The information of the local node.
     * @throws DotStateException
     * @throws DotDataException An error occurred when retrieving the information from the database.
     * @throws DotSecurityException The current user does not have permission to access this
     *         functionality.
     * @throws JSONException An invalid value has been added to the JSON object.
     */
    public static Map<String, Serializable> getNodeInfo() {
        final HashMap<String, Serializable> dotCMSNodeStatus = new HashMap<>();
        final ServerAPI serverAPI = APILocator.getServerAPI();
        final Server server =  Try.of(()-> serverAPI.getServer(APILocator.getServerAPI().readServerId())).getOrElseThrow(e-> new DotRuntimeException(e));


        // Cache
        final CacheTransport cacheTransport = Try.of(()-> CacheLocator.getCacheAdministrator().getTransport()).getOrElse(new NullTransport());
        final String cacheTransportStr = cacheTransport.getClass().getSimpleName().replace("CacheTransport", "");
        dotCMSNodeStatus.put("cacheTransportClass", cacheTransportStr);

        dotCMSNodeStatus.putAll(cacheTransport.getInfo().asMap());

        final List<Server> serversFromDb = Try.of(()-> serverAPI.getAliveServers()).getOrElseThrow(e-> new DotRuntimeException(e));
        final Long dateInMillisLong = new Date().getTime();
        final String dateInMillis = dateInMillisLong.toString();
        final String myServerId = serverAPI.readServerId();

        dotCMSNodeStatus.put("serverId", myServerId);
        dotCMSNodeStatus.put("displayServerId", LicenseUtil.getDisplayServerId());
        dotCMSNodeStatus.put("licenseId", LicenseUtil.getDisplaySerial());
        dotCMSNodeStatus.put("ipAddress", server.getIpAddress());
        dotCMSNodeStatus.put("host", server.getHost());
        dotCMSNodeStatus.put("friendlyName", server.getName());
        dotCMSNodeStatus.put("startup", server.getName());
        long jvmUpTime = ManagementFactory.getRuntimeMXBean().getUptime();
        
       
        dotCMSNodeStatus.put("uptime", DateUtil.prettyDateSince( Date.from( Instant.now().minusMillis(jvmUpTime))));
        
        if (UtilMethods.isSet(server.getLastHeartBeat())) {
            dotCMSNodeStatus.put("contacted", DateUtil.prettyDateSince(server.getLastHeartBeat()));
            dotCMSNodeStatus.put("contactedSeconds",
                            ((new Date()).getTime() - server.getLastHeartBeat().getTime()) / 1000);
        }
        
        Boolean hasHeartBeat = serversFromDb.stream().anyMatch(s->server.getServerId().equals(s.getServerId()));

        dotCMSNodeStatus.put("heartbeat", hasHeartBeat.toString());
        Boolean cacheStatus = true;

            


        ClusterHealthRequest request = new ClusterHealthRequest();
        request.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        ClusterHealthResponse response = Sneaky.sneak(() -> DotRestHighLevelClientProvider.getInstance().getClient()
                        .cluster().health(request, RequestOptions.DEFAULT));

        dotCMSNodeStatus.put("esClusterName", response.getClusterName());
        dotCMSNodeStatus.put("esTimedout", response.isTimedOut());
        dotCMSNodeStatus.put("esNumberOfNodes", response.getNumberOfNodes());
        dotCMSNodeStatus.put("esNumberOfDataNodes", response.getNumberOfDataNodes());
        dotCMSNodeStatus.put("esActivePrimaryShards", response.getActivePrimaryShards());
        dotCMSNodeStatus.put("esActiveShards", response.getActiveShards());
        dotCMSNodeStatus.put("esRelocatingShards", response.getRelocatingShards());
        dotCMSNodeStatus.put("esInitializingShards", response.getInitializingShards());
        dotCMSNodeStatus.put("esUnassignedShards", response.getUnassignedShards());
        dotCMSNodeStatus.put("esDelayedUnasignedShards", response.getDelayedUnassignedShards());
        dotCMSNodeStatus.put("esNumberOfPendingTasks", response.getNumberOfPendingTasks());
        dotCMSNodeStatus.put("esNumberOfInFlightFetch", response.getNumberOfInFlightFetch());
        dotCMSNodeStatus.put("esTaskMaxWaitingInQueueMillis", String.valueOf(response.getTaskMaxWaitingTime()));
        dotCMSNodeStatus.put("esActiveShardsPercentAsNumber", response.getActiveShardsPercent());

        final String esStatus = response.getStatus().toString().toLowerCase();
        dotCMSNodeStatus.put("esStatus", esStatus);

        // Asset folder.
        String serverFilePath = Config.getStringProperty("ASSET_REAL_PATH",
                        FileUtil.getRealPath(Config.getStringProperty("ASSET_PATH", "/assets")));
        File assetPath = new File(serverFilePath);
        boolean canWriteCanRead = false;
        dotCMSNodeStatus.put("assetsCanRead", Boolean.toString(assetPath.canRead()) + "/");
        dotCMSNodeStatus.put("assetsCanWrite", Boolean.toString(assetPath.canWrite()));
        dotCMSNodeStatus.put("assetsPath", serverFilePath);
        if (assetPath.canRead() && assetPath.canWrite()) {
            canWriteCanRead = true;
            File testFile = new File(assetPath, "server" + File.separator + myServerId + File.separator + myServerId
                            + "-" + dateInMillis + ".txt");
            try {
                testFile.mkdirs();
                testFile.delete();
                testFile.createNewFile();
                dotCMSNodeStatus.put("assetsTestPath", testFile.getPath());

            } catch (IOException e) {
                canWriteCanRead = false;
            }
        }
        dotCMSNodeStatus.put("assetsStatus", canWriteCanRead ? "green" : "red");

        dotCMSNodeStatus.put("status", cacheStatus && canWriteCanRead ? "green" : "red");
        return dotCMSNodeStatus;
    }

    
    
    /**
     * @param server
     * @return JSONObject with default fields filled. Intended to use when the
     *         Communications or Process fail.
     * @throws JSONException
     * @throws DotDataException
     */
    public static HashMap<String,Serializable> createFailedJson(final Server server) {
        if (server == null) {
            return null;
        }
        HashMap<String,Serializable> badMap = new HashMap<>();
        // Setting info that we know.
        badMap.put("serverId", server.getServerId());
        badMap.put("displayServerId", StringUtils.shortify(server.getServerId(),8));
        badMap.put("licenseId", StringUtils.shortify(server.getLicenseSerial(),8));
        badMap.put("ipAddress", server.getIpAddress());
        badMap.put("host", server.getHost());
        badMap.put("friendlyName", server.getName());
        // Setting if the node has heart beat.

        final List<Server> servers =  Try.of(()-> APILocator.getServerAPI().getAliveServers()).getOrElseThrow(e-> new DotRuntimeException(e));

        Boolean hasHeartBeat= (servers != null && servers.stream().anyMatch(s->s.getServerId().equals(server.getServerId()))) ;
        badMap.put("heartbeat", hasHeartBeat.toString());
        // Leaving blank the rest of the information.
        badMap.put("cacheClusterName", "");
        badMap.put("cacheOpen", "");
        badMap.put("cacheNumberOfNodes", "");
        badMap.put("cacheAddress", "");
        badMap.put("cacheReceivedBytes", "");
        badMap.put("cacheReceivedMessages", "");
        badMap.put("cacheSentBytes", "");
        badMap.put("cacheSentMessages", "");
        badMap.put("esClusterName", "");
        badMap.put("esTimedout", "");
        badMap.put("esNumberOfNodes", "");
        badMap.put("esNumberOfDataNodes", "");
        badMap.put("esActivePrimaryShards", "");
        badMap.put("esActiveShards", "");
        badMap.put("esRelocatingShards", "");
        badMap.put("esInitializingShards", "");
        badMap.put("esUnasignedShards", "");
        badMap.put("esDelayedUnasignedShards", "");
        badMap.put("esNumberOfPendingTasks", "");
        badMap.put("esNumberOfInFlightFetch", "");
        badMap.put("esTaskMaxWaitingInQueueMillis", "");
        badMap.put("esActiveShardsPercentAsNumber", "");
        badMap.put("cachePort", "");
        badMap.put("esPort", "");
        badMap.put("assetsCanRead", "");
        badMap.put("assetsCanWrite", "");
        badMap.put("assetsPath", "");
        badMap.put("hasLicense", "");
        // Setting red status for everything.
        badMap.put("assetsStatus", "red");
        badMap.put("esStatus", "red");
        badMap.put("status", "red");
        badMap.put("cacheStatus", "red");
        if (UtilMethods.isSet(server.getLastHeartBeat())) {
            badMap.put("contacted",
                    DateUtil.prettyDateSince(server.getLastHeartBeat()));
            badMap.put("contactedSeconds", ((new Date())
                    .getTime() - server.getLastHeartBeat().getTime()) / 1000);
        }
        return badMap;
    }
    
    
    
    
    

}
