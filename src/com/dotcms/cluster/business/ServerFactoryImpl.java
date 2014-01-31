package com.dotcms.cluster.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.cluster.bean.Server;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;


public class ServerFactoryImpl extends ServerFactory {

	private DotConnect dc;
	private String TIMESTAMPSQL = "NOW()";
	private static final String HEARTBEAT_TIMEOUT_DEFAULT_VALUE = "300";

	public ServerFactoryImpl() {
		dc = new DotConnect();

		if (DbConnectionFactory.isMsSql()) {
            TIMESTAMPSQL = "GETDATE()";
        } else if (DbConnectionFactory.isOracle()) {
            TIMESTAMPSQL = "CAST(SYSTIMESTAMP AS TIMESTAMP)";
        }
	}

	public void saveServer(Server server) throws DotDataException {
		if(!UtilMethods.isSet(server.getServerId())) {
			server.setServerId(UUID.randomUUID().toString());
		}
		server.setClusterId(ClusterFactory.getClusterId());
		dc.setSQL("insert into cluster_server(server_id, cluster_id, name, ip_address) values(?,?,?,?)");
		dc.addParam(server.getServerId());
		dc.addParam(server.getClusterId());
		dc.addParam(server.getName());
		dc.addParam(server.getIpAddress());
		dc.loadResult();
	}

	public Server getServer(String serverId) {
		dc.setSQL("select * from cluster_server where server_id = ?");
		dc.addParam(serverId);
		Server server = null;

		try {
			List<Map<String, Object>> results = dc.loadObjectResults();

			if(results!=null && !results.isEmpty()) {
				Map<String, Object> row = results.get(0);
				server = new Server();
				server.setServerId((String)row.get("server_id"));
				server.setClusterId((String)row.get("cluster_id"));
				server.setIpAddress((String)row.get("ip_address"));
				server.setName((String)row.get("name"));
				server.setHost((String)row.get("host"));
				server.setCachePort((Integer)row.get("cache_port"));
				server.setEsTransportTcpPort((Integer)row.get("es_transport_tcp_port"));
				server.setEsHttpPort((Integer)row.get("es_http_port"));

			}
		} catch (DotDataException e) {
			Logger.error(ServerFactoryImpl.class, "Could not get Server with id:" + serverId, e);
		}

		return server;

	}


	public void createServerUptime(String serverId) throws DotDataException {
		dc.setSQL("insert into cluster_server_uptime(id, server_id, startup) values(?,?, " + TIMESTAMPSQL + ")");
		String serverUptimeId = UUID.randomUUID().toString();
		dc.addParam(serverUptimeId);
		dc.addParam(serverId);
		dc.loadResult();
	}

	public void updateHeartbeat(String serverId) throws DotDataException {

		dc = new DotConnect();
		String id = null;

		if (DbConnectionFactory.isMsSql()) {
			dc.setSQL("SELECT TOP 1 id FROM cluster_server_uptime where server_id = ? ORDER BY startup DESC");
		} else if (DbConnectionFactory.isOracle()) {
			dc.setSQL("select id from (select id from cluster_server_uptime where server_id = order by startup desc ) where rownum = 1");
		}
		else{
			dc.setSQL("select id from cluster_server_uptime where server_id = ? order by startup desc limit 1");

		}

		dc.addParam(serverId);

		List<Map<String, Object>> results = dc.loadObjectResults();

		if(results!=null && !results.isEmpty()) {
			Map<String, Object> row = results.get(0);
			id = row.get("id").toString();
		}

		dc.setSQL("update cluster_server_uptime set heartbeat = "+ TIMESTAMPSQL +" where server_id = ? and id = ?");
		dc.addParam(serverId);
		dc.addParam(id);
		dc.loadResult();
	}

	public List<Server> getAllServers() throws DotDataException {
		List<Server> servers = new ArrayList<Server>();

		dc.setSQL("select server_id, cluster_id, name, ip_address, host, cache_port, es_transport_tcp_port, es_network_port, es_http_port, "
				+ "(select max(heartbeat) as last_heartbeat from cluster_server_uptime where server_id = s.server_id) as last_heartbeat"
				+ " from cluster_server s");
		List<Map<String, Object>> results = dc.loadObjectResults();


		for (Map<String, Object> row : results) {
			Server server = new Server();
			server.setServerId((String)row.get("server_id"));
			server.setClusterId((String)row.get("cluster_id"));
			server.setIpAddress((String)row.get("ip_address"));
			server.setHost((String)row.get("host"));
			server.setName((String)row.get("name"));
			server.setCachePort((Integer)row.get("cache_port"));
			server.setEsTransportTcpPort((Integer)row.get("es_transport_tcp_port"));
			server.setEsHttpPort((Integer)row.get("es_http_port"));
			server.setLastHeartBeat((Date)row.get("last_heartbeat"));
			servers.add(server);
		}

		return servers;
	}

	public List<Server> getAliveServers() throws DotDataException {
		return getAliveServers(new ArrayList<String>());
	}

	public List<Server> getAliveServers(List<String> toExclude) throws DotDataException {
		if (DbConnectionFactory.isMsSql()) {
			dc.setSQL("select DISTINCT s.server_id from cluster_server s join cluster_server_uptime sut on s.server_id = sut.server_id "
					+ "where DATEDIFF(SECOND, heartbeat, GETDATE()) < " + Config.getStringProperty("HEARTBEAT_TIMEOUT", HEARTBEAT_TIMEOUT_DEFAULT_VALUE));
		} else if (DbConnectionFactory.isMySql() || DbConnectionFactory.isH2()) {
			dc.setSQL("select DISTINCT s.server_id from cluster_server s join cluster_server_uptime sut on s.server_id = sut.server_id "
					+ "where TIMESTAMPDIFF(SECOND, heartbeat, now()) < " + Config.getStringProperty("HEARTBEAT_TIMEOUT", HEARTBEAT_TIMEOUT_DEFAULT_VALUE));
		} else if(DbConnectionFactory.isPostgres()) {
			dc.setSQL("select DISTINCT s.server_id from cluster_server s join cluster_server_uptime sut on s.server_id = sut.server_id "
					+ "where EXTRACT(EPOCH from now()-heartbeat) < " + Config.getStringProperty("HEARTBEAT_TIMEOUT", HEARTBEAT_TIMEOUT_DEFAULT_VALUE));
		} else if (DbConnectionFactory.isOracle()) {
			dc.setSQL("select DISTINCT s.server_id from cluster_server s join cluster_server_uptime sut on s.server_id = sut.server_id "
					+ "where (extract( second from (sysdate-heartbeat) ) "
					+ " + extract( minute from (sysdate-heartbeat) ) * 60 "
					+ " + extract( hour from (sysdate-heartbeat) ) * 60 * 60 "
					+ " + extract( day from (sysdate-heartbeat) ) * 60*60* 24) < " + Config.getStringProperty("HEARTBEAT_TIMEOUT", HEARTBEAT_TIMEOUT_DEFAULT_VALUE));
		}

		List<Server> aliveServers = new ArrayList<Server>();

		List<Map<String, Object>> results = dc.loadObjectResults();

		for (Map<String, Object> row : results) {
			String serverId = (String)row.get("server_id");

			boolean excluding = false;
			for (String toExcludeId : toExclude) {
				if(serverId.equals(toExcludeId)) {
					excluding = true;
					break;
				}
			}

			if(excluding) continue;

			aliveServers.add(getServer(serverId));
		}


		return aliveServers;
	}

	public String[] getAliveServersIds() throws DotDataException {
		List<Server> servers = getAliveServers();
		String[] serversIds = new String[servers.size()];

		int i=0;
		for (Server server : servers) {
			serversIds[i] = server.getServerId();
			i ++;
		}

		return serversIds;
	}

	public void updateServer(Server server) throws DotDataException {
		dc.setSQL("update cluster_server set cluster_id = ?, name=?, ip_address = ?, host = ?, cache_port = ?, es_transport_tcp_port = ?, es_http_port = ? where server_id = ?");
		dc.addParam(server.getClusterId());
		dc.addParam(server.getName());
		dc.addParam(server.getIpAddress());
		dc.addParam(server.getHost());
		dc.addParam(server.getCachePort());
		dc.addParam(server.getEsTransportTcpPort());
		dc.addParam(server.getEsHttpPort());
		dc.addParam(server.getServerId());
		dc.loadResult();
	}

	public void updateServerName(String serverId, String name) throws DotDataException {
		dc.setSQL("update cluster_server set name=? where server_id = ?");
		dc.addParam(name);
		dc.addParam(serverId);
		dc.loadResult();

	}

}
