package com.dotcms.cluster.business;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.cluster.bean.Server;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;


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
		server.setServerId(UUID.randomUUID().toString());
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
				server.setServerId(row.get("server_id").toString());
				server.setClusterId(row.get("cluster_id").toString());
				server.setIpAddress(row.get("ip_address").toString());

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
			dc.setSQL("SELECT TOP 1 id FROM server_uptime ORDER BY startup DESC");
		} else if (DbConnectionFactory.isMySql()
				|| DbConnectionFactory.isPostgres()) {
			dc.setSQL("select id from server_uptime order by startup desc limit 1");
		} else if (DbConnectionFactory.isOracle()) {
			dc.setSQL("select id from (select id from server_uptime order by startup desc ) where rownum = 1");
		}

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

}
