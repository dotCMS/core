package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

public class Task03015CreateClusterConfigModel implements StartupTask {

	private void createPushedAssetsTable(DotConnect dc) throws SQLException, DotDataException {
		if(DbConnectionFactory.isMsSql()) {
			dc.executeStatement("CREATE TABLE dot_cluster(cluster_id varchar(36), PRIMARY KEY (cluster_id) );");
			dc.executeStatement("CREATE TABLE cluster_server(server_id varchar(36) NOT NULL, cluster_id varchar(36) NOT NULL, name varchar(100), ip_address varchar(39) NOT NULL, host varchar(36), cache_port SMALLINT, es_transport_tcp_port SMALLINT, es_network_port SMALLINT, es_http_port SMALLINT, key_ varchar(100), PRIMARY KEY (server_id) );");
			dc.executeStatement("ALTER TABLE cluster_server add constraint fk_cluster_id foreign key (cluster_id) REFERENCES dot_cluster(cluster_id); ");
			dc.executeStatement("CREATE TABLE cluster_server_uptime(id varchar(36) NOT NULL, server_id varchar(36) NOT NULL, startup datetime null, heartbeat datetime null, PRIMARY KEY (id) );");
			dc.executeStatement("ALTER TABLE cluster_server_uptime add constraint fk_cluster_server_id foreign key (server_id) REFERENCES cluster_server(server_id); ");
		}else if(DbConnectionFactory.isOracle()) {
			dc.executeStatement("CREATE TABLE dot_cluster(cluster_id varchar2(36), PRIMARY KEY (cluster_id) )");
            dc.executeStatement("CREATE TABLE cluster_server(server_id varchar2(36) NOT NULL, cluster_id varchar2(36) NOT NULL, name varchar2(100), ip_address varchar2(39) NOT NULL, host varchar2(36), cache_port SMALLINT, es_transport_tcp_port SMALLINT, es_network_port SMALLINT, es_http_port SMALLINT, key_ varchar2(100), PRIMARY KEY (server_id) )");
			dc.executeStatement("ALTER TABLE cluster_server add constraint fk_cluster_id foreign key (cluster_id) REFERENCES dot_cluster(cluster_id)");
			dc.executeStatement("CREATE TABLE cluster_server_uptime(id varchar2(36) NOT NULL,server_id varchar2(36) NOT NULL, startup TIMESTAMP, heartbeat TIMESTAMP, PRIMARY KEY (id))");
			dc.executeStatement("ALTER TABLE cluster_server_uptime add constraint fk_cluster_server_id foreign key (server_id) REFERENCES cluster_server(server_id)");
		}else if(DbConnectionFactory.isMySql()) {
			dc.executeStatement("CREATE TABLE dot_cluster(cluster_id varchar(36), PRIMARY KEY (cluster_id) );");
			dc.executeStatement("CREATE TABLE cluster_server(server_id varchar(36), cluster_id varchar(36) NOT NULL, name varchar(100), ip_address varchar(39) NOT NULL, host varchar(36), cache_port SMALLINT, es_transport_tcp_port SMALLINT, es_network_port SMALLINT, es_http_port SMALLINT, key_ varchar(100), PRIMARY KEY (server_id) );");
			dc.executeStatement("ALTER TABLE cluster_server add constraint fk_cluster_id foreign key (cluster_id) REFERENCES dot_cluster(cluster_id);");
			dc.executeStatement("CREATE TABLE cluster_server_uptime(id varchar(36),server_id varchar(36) NOT NULL, startup datetime, heartbeat datetime, PRIMARY KEY (id)) ;");
			dc.executeStatement("ALTER TABLE cluster_server_uptime add constraint fk_cluster_server_id foreign key (server_id) REFERENCES cluster_server(server_id);");
		}else if(DbConnectionFactory.isPostgres()) {
			dc.executeStatement("CREATE TABLE dot_cluster(cluster_id varchar(36), PRIMARY KEY (cluster_id) );");
			dc.executeStatement("CREATE TABLE cluster_server(server_id varchar(36), cluster_id varchar(36) NOT NULL, name varchar(100), ip_address varchar(39) NOT NULL, host varchar(36), cache_port SMALLINT, es_transport_tcp_port SMALLINT, es_network_port SMALLINT, es_http_port SMALLINT, key_ varchar(100), PRIMARY KEY (server_id));");
			dc.executeStatement("ALTER TABLE cluster_server add constraint fk_cluster_id foreign key (cluster_id) REFERENCES dot_cluster(cluster_id);");
			dc.executeStatement("CREATE TABLE cluster_server_uptime(id varchar(36),server_id varchar(36) references cluster_server(server_id), startup timestamp without time zone null, heartbeat timestamp without time zone null, PRIMARY KEY (id));");
			dc.executeStatement("ALTER TABLE cluster_server_uptime add constraint fk_cluster_server_id foreign key (server_id) REFERENCES cluster_server(server_id);");
		}else if(DbConnectionFactory.isH2()) {
			dc.executeStatement("CREATE TABLE dot_cluster(cluster_id varchar(36), PRIMARY KEY (cluster_id) );");
			dc.executeStatement("CREATE TABLE cluster_server(server_id varchar(36), cluster_id varchar(36) NOT NULL, name varchar(100), ip_address varchar(39) NOT NULL, host varchar(36), cache_port SMALLINT, es_transport_tcp_port SMALLINT, es_network_port SMALLINT, es_http_port SMALLINT, PRIMARY KEY (server_id));");
			dc.executeStatement("ALTER TABLE cluster_server add constraint fk_cluster_id foreign key (cluster_id) REFERENCES dot_cluster(cluster_id);");
			dc.executeStatement("CREATE TABLE cluster_server_uptime(id varchar(36),server_id varchar(36) references cluster_server(server_id), startup timestamp without time zone null, heartbeat timestamp without time zone null, PRIMARY KEY (id));");
			dc.executeStatement("ALTER TABLE cluster_server_uptime add constraint fk_cluster_server_id foreign key (server_id) REFERENCES cluster_server(server_id);");
		}

	}

	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		try {
			DbConnectionFactory.getConnection().setAutoCommit(true);
		} catch (SQLException e) {
			throw new DotDataException(e.getMessage(), e);
		}
		try {
			DotConnect dc=new DotConnect();
			createPushedAssetsTable(dc);
			ClusterFactory.initialize();
		} catch (SQLException e) {
			throw new DotRuntimeException(e.getMessage(),e);
		}

	}

	@Override
	public boolean forceRun() {
		return true;
	}

}
