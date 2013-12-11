package com.dotcms.cluster.business;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotGuavaCacheAdministratorImpl;
import com.dotmarketing.cache.H2CacheLoader;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.servlet.MainServlet;

public class ClusterFactory {

	public static void generateClusterId() throws DotDataException {

		String clusterId = getClusterId();

		if(!UtilMethods.isSet(clusterId)) {
			DotConnect dc = new DotConnect();
			dc.setSQL("insert into cluster values (?)");
			clusterId = UUID.randomUUID().toString();
			dc.addParam(clusterId);
			dc.loadResult();
		}
	}

	public static String getClusterId() {
		DotConnect dc = new DotConnect();
		dc.setSQL("select cluster_id from cluster");
		String clusterId = null;

		try {
			List<Map<String,Object>> results = dc.loadObjectResults();
			if(!results.isEmpty()) {
				clusterId = (String) results.get(0).get("cluster_id");
			}

		} catch (DotDataException e) {
			Logger.error(ClusterFactory.class, "Could not get Cluster ID", e);
		}

		return clusterId;
	}

	public static String getNextAvailablePort(String serverId, ServerPort port) {
		DotConnect dc = new DotConnect();
		dc.setSQL("select max(" + port.getTableName()+ ") as port from server where ip_address = (select s.ip_address from server s where s.server_id = ?) "
				+ "or ('localhost' = (select s.ip_address from server s where s.server_id = '2fc4ff9f-205e-4b42-a65b-abe1f8a92f9f') and ip_address = '127.0.0.1') "
				+ "or('127.0.0.1' = (select s.ip_address from server s where s.server_id = '2fc4ff9f-205e-4b42-a65b-abe1f8a92f9f') and ip_address = 'localhost') ");

		dc.addParam(serverId);
		Integer maxPort = null;
		String freePort = Config.getStringProperty(port.getPropertyName(), port.getDefaultValue());

		try {
			List<Map<String,Object>> results = dc.loadObjectResults();
			if(!results.isEmpty()) {
				maxPort = (Integer) results.get(0).get("port");
				freePort = UtilMethods.isSet(maxPort)?Integer.toString(maxPort+1):freePort;
			}

		} catch (DotDataException e) {
			Logger.error(ClusterFactory.class, "Could not get Available server port", e);
		}

		return freePort.toString();
	}

	public static void addNodeToCluster(String serverId) throws Exception {
		addNodeToCluster(null, serverId);
	}

	public static void addNodeToCluster(Map<String,String> properties, String serverId) throws Exception {

		if(properties==null) {
			properties = new HashMap<String, String>();
		}

		ServerAPI serverAPI = APILocator.getServerAPI();
		Server currentServer = serverAPI.getServer(serverId);
		InetAddress addr = InetAddress.getLocalHost();
		String address = addr.getHostAddress();
		currentServer.setIpAddress(address);

		addNodeToCacheCluster(properties, currentServer);

//		properties.put("ES_NODE_LOCAL", "false");
		addNodeToESCluster(properties);



	}

	private static void addNodeToCacheCluster(Map<String, String> cacheProperties, Server localServer) throws Exception {
		((DotGuavaCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).setCluster(cacheProperties, localServer);
		((DotGuavaCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).testCluster();
		Config.setProperty("DIST_INDEXATION_ENABLED", true);

    	try {
			H2CacheLoader.getInstance().moveh2dbDir();
		} catch (Exception e) {
			Logger.error(MainServlet.class, "Error sending H2DB Dir to trash", e);
		}

	}

	private static void addNodeToESCluster(Map<String, String> esProperties) throws Exception {
		ESClient esClient = new ESClient();
		esClient.setClusterNode(esProperties);

	}

}
