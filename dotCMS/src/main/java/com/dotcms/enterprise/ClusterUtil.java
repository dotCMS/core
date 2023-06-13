/**
 * 
 */
package com.dotcms.enterprise;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.enterprise.license.DotLicenseRepoEntry;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.node.DiscoveryNode;
import com.dotcms.rest.ClusterResource;
import com.dotmarketing.business.*;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransport.CacheTransportInfo;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.*;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.util.FileUtil;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;

/**
 * Class to handle all logic for clusters. Should be called by REST API and
 * Server Action.
 * 
 * @author Oscar Arrieta.
 *
 */
public class ClusterUtil {

	/**
	 * Returns the basic information of the current server (local node) in JSON
	 * format.
	 * 
	 * @return JSONObject - The information of the local node.
	 * @throws DotStateException
	 * @throws DotDataException
	 *             An error occurred when retrieving the information from the
	 *             database.
	 * @throws DotSecurityException
	 *             The current user does not have permission to access this
	 *             functionality.
	 * @throws JSONException
	 *             An invalid value has been added to the JSON object.
	 */
	public static JSONObject getNodeInfo() throws DotStateException,
			DotDataException, DotSecurityException, JSONException, DotCacheException {
		JSONObject jsonNodeStatusObject = new JSONObject();
		ServerAPI serverAPI = APILocator.getServerAPI();
		Server server = serverAPI.getServer(APILocator.getServerAPI()
				.readServerId());
		// In case the server is not in the List. This should NOT happen, but
		// just in case.
		if (server == null) {
			return null;
		}

		// JGroups Cache
		CacheTransport cacheTransport = CacheLocator.getCacheAdministrator().getImplementationObject().getTransport();
		CacheTransportInfo cacheTransportInfo = cacheTransport.getInfo();

		List<Server> servers = serverAPI.getAliveServers();
		Long dateInMillisLong = new Date().getTime();
		String dateInMillis = dateInMillisLong.toString();
		String myServerId = serverAPI.readServerId();
		Map<String, Boolean> members = ((ChainableCacheAdministratorImpl) CacheLocator
				.getCacheAdministrator().getImplementationObject())
				.validateCacheInCluster(dateInMillis, servers.size() - 1, 1);
		jsonNodeStatusObject.put("serverId", server.getServerId());
		jsonNodeStatusObject.put("displayServerId",
				LicenseUtil.getDisplayServerId());
		jsonNodeStatusObject.put("licenseId",
				LicenseUtil.getDisplaySerial());
		jsonNodeStatusObject.put("ipAddress", server.getIpAddress());
		jsonNodeStatusObject.put("host", server.getHost());
		jsonNodeStatusObject.put("friendlyName", server.getName());
		if (UtilMethods.isSet(server.getLastHeartBeat())) {
			jsonNodeStatusObject.put("contacted",
					DateUtil.prettyDateSince(server.getLastHeartBeat()));
			jsonNodeStatusObject.put("contactedSeconds", ((new Date())
					.getTime() - server.getLastHeartBeat().getTime()) / 1000);
		}
		Boolean hasHeartBeat = false;
		if (serverAPI.getAliveServers() != null
				&& Arrays.asList(serverAPI.getAliveServersIds()).contains(
						server.getServerId())) {
			hasHeartBeat = true;
		}

		jsonNodeStatusObject.put("heartbeat", hasHeartBeat.toString());
		Boolean cacheStatus = false;
		if (cacheTransportInfo != null) {
			// General cache status.
			jsonNodeStatusObject.put("cacheClusterName",
					cacheTransportInfo.getClusterName());
			jsonNodeStatusObject.put("cacheOpen",
					Boolean.toString(cacheTransportInfo.isOpen()));
			jsonNodeStatusObject.put("cacheNumberOfNodes",
					Integer.toString(cacheTransportInfo.getNumberOfNodes()));
			jsonNodeStatusObject.put("cacheAddress",
					cacheTransportInfo.getAddress());
			jsonNodeStatusObject.put(
					"cacheReceivedBytes",
					Long.toString(cacheTransportInfo.getReceivedBytes()) + "/"
							+ Long.toString(cacheTransportInfo.getSentBytes()) + " bytes");
			jsonNodeStatusObject.put("cacheReceivedMessages",
					Long.toString(cacheTransportInfo.getReceivedMessages()));
			jsonNodeStatusObject.put("cacheSentBytes",
					Long.toString(cacheTransportInfo.getSentBytes()));
			jsonNodeStatusObject.put("cacheSentMessages",
					Long.toString(cacheTransportInfo.getSentMessages()));
			jsonNodeStatusObject.put("cachePort", server.getCachePort());
			// 1. If servers is just one we assume the cache is OK.
			// 2. If Server is local and we are connected to at least one other
			// server is OK.
			// 3. If servers are more than 1 and is in members array is OK.
			if (servers.size() == 1
					|| (server.getServerId().equals(myServerId) && (members != null && !members
							.isEmpty()))
					|| (members != null && members.containsKey(server
							.getServerId()))
					|| cacheTransport.getInfo().getClusterName().equals("NullTransport")) {
				cacheStatus = true;
			}
			// If we have more than 1 server in the cluster and also the result
			// of the cache status is less, means we have server that didn't
			// respond.
			if (servers.size() > 1 && members != null && (members.size() < servers.size() - 1)) {
				// Cache has member failing so it is not OK.
				cacheStatus = false;
				// String to concat Server IDs that didn't respond.
				String serversNotResponded = "";
				// Iterates to see which server is not present.
				for (Server serverIter : servers) {
					if (!serverIter.getServerId().equals(myServerId)
							&& !members.containsKey(serverIter.getServerId())) {
						Server serverDown = APILocator.getServerAPI()
								.getServer(serverIter.getServerId());
						if (!serversNotResponded.isEmpty()) {
							serversNotResponded = serversNotResponded
									.concat(", ");
						}
						serversNotResponded = serversNotResponded
								.concat(serverDown.getName()
										+ "-"
										+ serverDown.getServerId().substring(0,
												8));
					}
				}
				jsonNodeStatusObject.put("cacheServersNotResponded",
						serversNotResponded);
			}
			jsonNodeStatusObject.put("cacheStatus", cacheStatus ? "green"
					: "red");
		} else {
			jsonNodeStatusObject.put("cacheStatus", "green");
			jsonNodeStatusObject.put("cacheClusterName", "");
			jsonNodeStatusObject.put("cacheOpen", "");
			jsonNodeStatusObject.put("cacheNumberOfNodes", "");
			jsonNodeStatusObject.put("cacheAddress", "");
			jsonNodeStatusObject.put("cacheReceivedBytes", "");
			jsonNodeStatusObject.put("cacheReceivedMessages", "");
			jsonNodeStatusObject.put("cacheSentBytes", "");
			jsonNodeStatusObject.put("cacheSentMessages", "");
			jsonNodeStatusObject.put("cachePort", "");
		}
		Boolean esStatus = false;
		AdminClient esClient = null;
		try {
			Client client = new ESClient().getClientInCluster();
			if (client != null) {
				esClient = client.admin();
			}
		} catch (Exception e) {
			Logger.error(ClusterResource.class, "Error getting ES Client", e);
		}
		// ES status for the given node.
		if (esClient != null) {
			NodesInfoRequest nodesReq = new NodesInfoRequest();
			ActionFuture<NodesInfoResponse> afNodesRes = esClient.cluster()
					.nodesInfo(nodesReq);
			NodesInfoResponse nodesRes = afNodesRes.actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
			List<NodeInfo> esNodes = nodesRes.getNodes();
			for (NodeInfo nodeInfo : esNodes) {
				DiscoveryNode node = nodeInfo.getNode();
				if (node.getName().equals(server.getServerId())) {
					esStatus = true;
					break;
				}
			}
			// ES general status.
			ClusterHealthRequest clusterReq = new ClusterHealthRequest();
			ActionFuture<ClusterHealthResponse> afClusterRes = esClient
					.cluster().health(clusterReq);
			ClusterHealthResponse clusterRes = afClusterRes.actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
			jsonNodeStatusObject.put("esClusterName",
					clusterRes.getClusterName());
			jsonNodeStatusObject.put("esNumberOfNodes",
					clusterRes.getNumberOfNodes());
			jsonNodeStatusObject.put("esActiveShards",
					clusterRes.getActiveShards());
			jsonNodeStatusObject.put("esActivePrimaryShards",
					clusterRes.getActivePrimaryShards());
			jsonNodeStatusObject.put("esUnasignedPrimaryShards",
					clusterRes.getUnassignedShards());
			jsonNodeStatusObject.put("esPort", server.getEsTransportTcpPort());
			jsonNodeStatusObject.put("esStatus", esStatus ? "green" : "red");
		} else {
			jsonNodeStatusObject.put("esClusterName", "");
			jsonNodeStatusObject.put("esNumberOfNodes", "");
			jsonNodeStatusObject.put("esActiveShards", "");
			jsonNodeStatusObject.put("esActivePrimaryShards", "");
			jsonNodeStatusObject.put("esUnasignedPrimaryShards", "");
			jsonNodeStatusObject.put("esPort", "");
			jsonNodeStatusObject.put("esStatus", "red");
		}
		// Asset folder.
		String serverFilePath = Config.getStringProperty("ASSET_REAL_PATH",
				FileUtil.getRealPath(Config.getStringProperty("ASSET_PATH", "/assets")));
		File assetPath = new File(serverFilePath);
		boolean canWriteCanRead = false;
		jsonNodeStatusObject.put("assetsCanRead",
				Boolean.toString(assetPath.canRead()) + "/");
		jsonNodeStatusObject.put("assetsCanWrite",
				Boolean.toString(assetPath.canWrite()));
		jsonNodeStatusObject.put("assetsPath", serverFilePath);
		if (assetPath.canRead() && assetPath.canWrite()) {
			canWriteCanRead = true;
			File testFile = new File(assetPath, "server" + File.separator + myServerId + File.separator +  myServerId
					+ "-" + dateInMillis + ".txt");
			try {
			    testFile.mkdirs();
			    testFile.delete();
				testFile.createNewFile();
				jsonNodeStatusObject.put("assetsTestPath", testFile.getPath());

			} catch (IOException e) {
				canWriteCanRead = false;
			}
		}
		jsonNodeStatusObject.put("assetsStatus", canWriteCanRead ? "green"
				: "red");
		// Has license?
		Boolean hasLicense = false;
		try {
			for (DotLicenseRepoEntry lic : LicenseUtil.getLicenseRepoList()) {
				if (UtilMethods.isSet(lic.serverId)
						&& lic.serverId.equals(server.getServerId())) {
					hasLicense = true;
				}
			}
		} catch (IOException e) {
			Logger.error(ClusterResource.class,
					"Error reading License from Repo: " + e.getStackTrace());
		}
		jsonNodeStatusObject.put("hasLicense", hasLicense);
		jsonNodeStatusObject.put("status", esStatus && cacheStatus
				&& canWriteCanRead ? "green" : "red");
		return jsonNodeStatusObject;
	}

	/**
	 * @param server
	 * @return JSONObject with default fields filled. Intended to use when the
	 *         Communications or Process fail.
	 * @throws JSONException
	 * @throws DotDataException
	 */
	public static JSONObject createFailedJson(Server server)
			throws JSONException, DotDataException {
		if (server == null) {
			return null;
		}
		JSONObject jsonNodeStatusObject = new JSONObject();
		// Setting info that we know.
		jsonNodeStatusObject.put("serverId", server.getServerId());
		jsonNodeStatusObject.put("displayServerId", "N/A");
		jsonNodeStatusObject.put("licenseId", "N/A");
		jsonNodeStatusObject.put("ipAddress", server.getIpAddress());
		jsonNodeStatusObject.put("host", server.getHost());
		jsonNodeStatusObject.put("friendlyName", server.getName());
		// Setting if the node has heart beat.
		Boolean hasHeartBeat = false;
		if (APILocator.getServerAPI().getAliveServers() != null
				&& Arrays
						.asList(APILocator.getServerAPI().getAliveServersIds())
						.contains(server.getServerId())) {
			hasHeartBeat = true;
		}
		jsonNodeStatusObject.put("heartbeat", hasHeartBeat.toString());
		// Leaving blank the rest of the information.
		jsonNodeStatusObject.put("cacheClusterName", "");
		jsonNodeStatusObject.put("cacheOpen", "");
		jsonNodeStatusObject.put("cacheNumberOfNodes", "");
		jsonNodeStatusObject.put("cacheAddress", "");
		jsonNodeStatusObject.put("cacheReceivedBytes", "");
		jsonNodeStatusObject.put("cacheReceivedMessages", "");
		jsonNodeStatusObject.put("cacheSentBytes", "");
		jsonNodeStatusObject.put("cacheSentMessages", "");
		jsonNodeStatusObject.put("esClusterName", "");
		jsonNodeStatusObject.put("esNumberOfNodes", "");
		jsonNodeStatusObject.put("esActiveShards", "");
		jsonNodeStatusObject.put("esActivePrimaryShards", "");
		jsonNodeStatusObject.put("esUnasignedPrimaryShards", "");
		jsonNodeStatusObject.put("cachePort", "");
		jsonNodeStatusObject.put("esPort", "");
		jsonNodeStatusObject.put("assetsCanRead", "");
		jsonNodeStatusObject.put("assetsCanWrite", "");
		jsonNodeStatusObject.put("assetsPath", "");
		jsonNodeStatusObject.put("hasLicense", "");
		// Setting red status for everything.
		jsonNodeStatusObject.put("assetsStatus", "red");
		jsonNodeStatusObject.put("esStatus", "red");
		jsonNodeStatusObject.put("status", "red");
		jsonNodeStatusObject.put("cacheStatus", "red");
		if (UtilMethods.isSet(server.getLastHeartBeat())) {
			jsonNodeStatusObject.put("contacted",
					DateUtil.prettyDateSince(server.getLastHeartBeat()));
			jsonNodeStatusObject.put("contactedSeconds", ((new Date())
					.getTime() - server.getLastHeartBeat().getTime()) / 1000);
		}
		return jsonNodeStatusObject;
	}

}
