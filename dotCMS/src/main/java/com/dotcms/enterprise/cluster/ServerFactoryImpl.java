package com.dotcms.enterprise.cluster;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.business.ServerFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import io.vavr.Tuple;
import io.vavr.Tuple2;



public class ServerFactoryImpl extends ServerFactory {



    public ServerFactoryImpl() {


    }


    final static String SELECT_SERVER_SQL = "select cluster_server.*, sitelic.id as license_id, sitelic.lastping as lastping from cluster_server left join sitelic on server_id = serverid ";

    
    private int heartbeatTimeout(){
    	// 30m default
    	return Config.getIntProperty("HEARTBEAT_TIMEOUT", 1800);
    }
    
    
    
    @WrapInTransaction
    public void saveServer(Server server) throws DotDataException {
    	
    	
    	// delete then insert, then update (ghetto upsert!)
    	removeServerFromClusterTable(server.serverId);
    	
        DotConnect dc = new DotConnect();


        dc.setSQL("insert into cluster_server(server_id, cluster_id, name, ip_address, key_) values(?,?,?,?,?)");
        dc.addParam(server.getServerId());
        dc.addParam(server.getClusterId());
        dc.addParam(server.getName());
        dc.addParam(server.getIpAddress());
        dc.addParam("n/a");
        dc.loadResult();
        updateServer(server);
    }
    
    protected Server readServerResult(Map<String, Object> row) {
    	
    	
		Server.Builder serverBuilder =  Server.builder();


		serverBuilder.withServerId((String)row.get("server_id"));

		serverBuilder.withClusterId((String)row.get("cluster_id"));
		serverBuilder.withIpAddress((String)row.get("ip_address"));
		serverBuilder.withName((String)row.get("name"));
		serverBuilder.withHost((String)row.get("host"));

		serverBuilder.withLicenseSerial((String)row.get("license_id"));

        if ( row.get( "cache_port" ) != null ) {
        	serverBuilder.withCachePort( ((Number) row.get( "cache_port" )).intValue() );
        }
        if ( row.get( "es_transport_tcp_port" ) != null ) {
        	serverBuilder.withEsTransportTcpPort( ((Number) row.get( "es_transport_tcp_port" )).intValue() );
        }
        if ( row.get( "es_http_port" ) != null ) {
        	serverBuilder.withEsHttpPort( ((Number) row.get( "es_http_port" )).intValue() );
        }
        if ( row.get( "es_network_port" ) != null ) {
        	serverBuilder.withEsNetworkPort(((Number) row.get( "es_network_port" )).intValue() );
        }


        Date lastPing = row.get("lastping") !=null  ? ((Date)row.get("lastping")) : new Date(0) ;
        
        serverBuilder.withLastHeartBeat(lastPing.getTime());


        return serverBuilder.build();
    }

    public Server getServer(String serverId) throws DotDataException {

        Logger.debug(ServerFactoryImpl.class, "Getting server from data base " + serverId);

        DotConnect dc = new DotConnect();
        dc.setSQL( SELECT_SERVER_SQL + " where server_id = ? ");
        dc.addParam(serverId);
        Server server = null;

        try {
            List<Map<String, Object>> results = dc.loadObjectResults();

            if(results!=null && !results.isEmpty()) {
                Map<String, Object> row = results.get(0);
                server = readServerResult(row);
            }
        } catch (DotDataException e) {
            Logger.error(ServerFactoryImpl.class, "Could not get Server with id:" + serverId, e);
        }

        return server;
    }




    @CloseDBIfOpened
    public List<Server> getAllServers() throws DotDataException {
        List<Server> servers = new ArrayList<Server>();
        DotConnect dc = new DotConnect();

        dc.setSQL(SELECT_SERVER_SQL +  " where server_id is not null order by server_id");


        List<Map<String, Object>> results = dc.loadObjectResults();

        for (Map<String, Object> row : results) {
            Server server = readServerResult(row);
            servers.add(server);
        }

        return servers;
    }

    

  private Tuple2<Instant, List<String>> reindexingServers = null;

  @Override
  public List<String> getReindexingServers() throws DotDataException {
    
    if (reindexingServers != null && !reindexingServers._2.isEmpty() && Instant.now().isBefore(reindexingServers._1)) {
      return reindexingServers._2;
    }


    final List<String> servers = new ArrayList<>();
    DotConnect dc = new DotConnect();

    dc.setSQL(SELECT_SERVER_SQL + "  where sitelic.id is not null and lastping > ? order by server_id");

    dc.addParam(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)));
    List<Map<String, Object>> results = dc.loadObjectResults();

    for (Map<String, Object> row : results) {
      servers.add((String) row.get("server_id"));
    }
    if(!servers.contains(APILocator.getServerAPI().readServerId())) {
      servers.add(APILocator.getServerAPI().readServerId());
    }
    reindexingServers = Tuple.of(Instant.now().plus(1, ChronoUnit.MINUTES), servers);
    return servers;
  }
    
    
    
    
    @CloseDBIfOpened
    public List<Server> getAliveServers() throws DotDataException {

    	Calendar cal = Calendar.getInstance();
    	cal.add(Calendar.SECOND, -heartbeatTimeout());
        List<Server> servers = new ArrayList<Server>();
        DotConnect dc = new DotConnect();

        dc.setSQL(SELECT_SERVER_SQL +  "  where sitelic.id is not null and lastping > ? order by server_id");

		dc.addParam(cal.getTime());
        List<Map<String, Object>> results = dc.loadObjectResults();


        for (Map<String, Object> row : results) {
            Server server = readServerResult(row);
            servers.add(server);
        }

        return servers;
    }

    @CloseDBIfOpened
    public List<Server> getAliveServers(List<String> toExclude) throws DotDataException {
        List<Server> list = getAliveServers();
    	for (Iterator<Server> i = list.iterator(); i.hasNext();) {
    		Server server = i.next();
    	    if (toExclude.contains(server.getServerId())){
    	        i.remove();
    	    }
    	}

        return list;
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
    @WrapInTransaction
    public void updateServer(Server server) throws DotDataException {
    
            DotConnect dc = new DotConnect();       
            dc.setSQL("update cluster_server set cluster_id = ?, name=?, ip_address = ?, host = ?, cache_port = ?, es_transport_tcp_port = ?, es_http_port = ?, key_ = ?,es_network_port=? where server_id = ?");
            dc.addParam(server.getClusterId());
            dc.addParam(server.getName());
            dc.addParam(server.getIpAddress());
            dc.addParam(server.getHost());
            dc.addParam(server.getCachePort());
            dc.addParam(server.getEsTransportTcpPort());
            dc.addParam(server.getEsHttpPort());
            dc.addParam("n/a");
            dc.addParam(server.getEsNetworkPort());
            dc.addParam(server.getServerId());
            dc.loadResult();
    }
    
    @WrapInTransaction
    public void updateServerName(String serverId, String name) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("update cluster_server set name=? where server_id = ?");
        dc.addParam(name);
        dc.addParam(serverId);
        dc.loadResult();
    }
    
    @WrapInTransaction
    @Override
    public void removeServerFromClusterTable(String serverId) throws DotDataException{

        DotConnect dc = new DotConnect();
        dc.setSQL("delete from cluster_server_uptime");
        dc.addParam(serverId);
        dc.loadResult();

        dc = new DotConnect();
        dc.setSQL("delete from cluster_server where server_id = ?");
        dc.addParam(serverId);
        dc.loadResult();
    }
    

    @CloseDBIfOpened
	@Override
	public List<Server> getInactiveServers() throws DotDataException {
    	Calendar cal = Calendar.getInstance();
    	cal.add(Calendar.SECOND, -heartbeatTimeout());
        List<Server> servers = new ArrayList<Server>();
        DotConnect dc = new DotConnect();

        dc.setSQL(SELECT_SERVER_SQL +  "  where lastping is null or lastping < ? ");

		dc.addParam(cal.getTime());
        List<Map<String, Object>> results = dc.loadObjectResults();


        for (Map<String, Object> row : results) {
            Server server = readServerResult(row);
            servers.add(server);
        }

        return servers;
		
		
		
		
		
	}


}
