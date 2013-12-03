package com.dotcms.cluster.business;

import java.io.IOException;
import java.util.ArrayList;
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
		dc.setSQL("insert into server(server_id, cluster_id, ip_address) values(?,?,?)");
		dc.addParam(server.getServerId());
		dc.addParam(server.getClusterId());
		dc.addParam(server.getIpAddress());
		dc.loadResult();
	}

	public Server getServer(String serverId) {
		dc.setSQL("select * from server where server_id = ?");
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
				server.setHost((String)row.get("host"));
				server.setCachePort((Long)row.get("cache_port"));

			}
		} catch (DotDataException e) {
			Logger.error(ServerFactoryImpl.class, "Could not get Server with id:" + serverId, e);
		}

		return server;

	}


	public void createServerUptime(String serverId) throws DotDataException {
		dc.setSQL("insert into server_uptime(id, server_id, startup) values(?,?, " + TIMESTAMPSQL + ")");
		String serverUptimeId = UUID.randomUUID().toString();
		dc.addParam(serverUptimeId);
		dc.addParam(serverId);
		dc.loadResult();
	}

	public void updateHeartbeat(String serverId) throws DotDataException {

		String id = null;

		if (DbConnectionFactory.isMsSql()) {
			dc.setSQL("SELECT TOP 1 id FROM server_uptime where server_id = ? ORDER BY startup DESC");
		} else if (DbConnectionFactory.isMySql()
				|| DbConnectionFactory.isPostgres()) {
			dc.setSQL("select id from server_uptime where server_id = ? order by startup desc limit 1");
		} else if (DbConnectionFactory.isOracle()) {
			dc.setSQL("select id from (select id from server_uptime where server_id = order by startup desc ) where rownum = 1");
		}

		dc.addParam(serverId);

		List<Map<String, Object>> results = dc.loadObjectResults();

		if(results!=null && !results.isEmpty()) {
			Map<String, Object> row = results.get(0);
			id = row.get("id").toString();
		}

		dc.setSQL("update server_uptime set heartbeat = "+ TIMESTAMPSQL +" where server_id = ? and id = ?");
		dc.addParam(serverId);
		dc.addParam(id);
		dc.loadResult();
	}

	public List<Server> getAliveServers() throws DotDataException {
		if (DbConnectionFactory.isMsSql()) {
			dc.setSQL("select DISTINCT s.server_id from server s join server_uptime sut on s.server_id = sut.server_id "
					+ "where DATEDIFF(SECOND, heartbeat, GETDATE()) < " + Config.getStringProperty("HEARTBEAT_TIMEOUT", "60"));
		} else if (DbConnectionFactory.isMySql()) {
			dc.setSQL("select DISTINCT s.server_id from server s join server_uptime sut on s.server_id = sut.server_id "
					+ "where TIMESTAMPDIFF(SECOND, heartbeat, now()) < " + Config.getStringProperty("HEARTBEAT_TIMEOUT", "60"));
		} else if(DbConnectionFactory.isPostgres()) {
			dc.setSQL("select DISTINCT s.server_id from server s join server_uptime sut on s.server_id = sut.server_id "
					+ "where EXTRACT(EPOCH from now()-heartbeat) < " + Config.getStringProperty("HEARTBEAT_TIMEOUT", "60"));
		} else if (DbConnectionFactory.isOracle()) {
			dc.setSQL("select DISTINCT s.server_id from server s join server_uptime sut on s.server_id = sut.server_id "
					+ "where (extract( second from (sysdate-heartbeat) ) "
					+ " + extract( minute from (sysdate-heartbeat) ) * 60 "
					+ " + extract( hour from (sysdate-heartbeat) ) * 60 * 60 "
					+ " + extract( day from (sysdate-heartbeat) ) * 60*60* 24) < " + Config.getStringProperty("HEARTBEAT_TIMEOUT", "60"));
		}

		List<Server> aliveServers = new ArrayList<Server>();

		try {
			List<Map<String, Object>> results = dc.loadObjectResults();

			for (Map<String, Object> row : results) {
				Server server = new Server();
				server.setServerId(row.get("server_id").toString());
				server.setClusterId(row.get("cluster_id").toString());
				server.setIpAddress(row.get("ip_address").toString());
				server.setCachePort((Long)row.get("cache_port"));
				aliveServers.add(server);
			}

		} catch (DotDataException e) {
			Logger.error(ServerFactoryImpl.class, "Could not get alive Servers Ids", e);
		}

		return aliveServers;
	}

	public void updateServer(Server server) throws DotDataException {
		dc.setSQL("update server set cluster_id = ?, ip_address = ?, host = ?, cache_port = ? where server_id = ?");
		dc.addParam(server.getClusterId());
		dc.addParam(server.getIpAddress());
		dc.addParam(server.getHost());
		dc.addParam(server.getCachePort());
		dc.addParam(server.getServerId());
		dc.loadResult();
	}

}
