package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

public class Task01311CreateClusterConfigModel implements StartupTask {

	private void createPushedAssetsTable(DotConnect dc) throws SQLException, DotDataException {
		if(DbConnectionFactory.isMsSql()) {
			dc.executeStatement("CREATE TABLE server(server_id varchar(36) NOT NULL, cluster_id varchar(36) NOT NULL,ip_address varchar(39) NOT NULL, host varchar(36), cache_port numeric(5,0), es_transport_tcp_port numeric(5,0), es_network_port numeric(5,0), es_http_port numeric(5,0) );");
			dc.executeStatement("CREATE TABLE server_uptime(id varchar(36) NOT NULL, server_id varchar(36) NOT NULL, startup datetime null, heartbeat datetime null );");
			dc.executeStatement("CREATE TABLE cluster(cluster_id varchar(36) );");
		}else if(DbConnectionFactory.isOracle()) {
			dc.executeStatement("CREATE TABLE server(server_id varchar2(36) NOT NULL, cluster_id varcha2r(36) NOT NULL,ip_address varchar2(39) NOT NULL, host varchar2(36), cache_port number(5,0), es_transport_tcp_port number(5,0), es_network_port number(5,0), es_http_port number(5,0) );");
			dc.executeStatement("CREATE TABLE server_uptime(id varchar2(36) NOT NULL,server_id varchar2(36) NOT NULL, startup TIMESTAMP, heartbeat TIMESTAMP);");
			dc.executeStatement("CREATE TABLE cluster(cluster_id varchar2(36) );");
		}else if(DbConnectionFactory.isMySql()) {
			dc.executeStatement("CREATE TABLE server(server_id varchar(36) NOT NULL, cluster_id varchar(36) NOT NULL,ip_address varchar(39) NOT NULL, host varchar(36), cache_port bigint, es_transport_tcp_port bigint, es_network_port bigint, es_http_port bigint );");
			dc.executeStatement("CREATE TABLE server_uptime(id varchar(36) NOT NULL,server_id varchar(36) NOT NULL, startup datetime, heartbeat datetime;");
			dc.executeStatement("CREATE TABLE cluster(cluster_id varchar(36) );");
		}else if(DbConnectionFactory.isPostgres()) {
			dc.executeStatement("CREATE TABLE server(server_id varchar(36) NOT NULL, cluster_id varchar(36) NOT NULL,ip_address varchar(39) NOT NULL, host varchar(36), cache_port numeric(5,0), es_transport_tcp_port numeric(5,0), es_network_port numeric(5,0), es_http_port numeric(5,0) );");
			dc.executeStatement("CREATE TABLE server_uptime(id varchar(36) NOT NULL,server_id varchar(36) NOT NULL, startup timestamp without time zone null, heartbeat timestamp without time zone null);");
			dc.executeStatement("CREATE TABLE cluster(cluster_id varchar(36) );");
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
		} catch (SQLException e) {
			throw new DotRuntimeException(e.getMessage(),e);
		}

	}

	@Override
	public boolean forceRun() {
		return true;
	}

}
